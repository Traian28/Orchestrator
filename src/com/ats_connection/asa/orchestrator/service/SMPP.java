/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ats_connection.asa.orchestrator.service;

import java.util.Map;

import ats.smpp2ws.services.SMPP2WS;

import javax.jws.WebService;

import com.ats_connection.asa.orchestrator.config.ConnectionManager;
import com.ats_connection.asa.orchestrator.config.ServerConfiguration;
import com.ats_connection.asa.orchestrator.config.Subscriber;
import com.ats_connection.asa.orchestrator.core.ManagementService;
import com.ats_connection.asa.orchestrator.core.SessionService;
import com.ats_connection.asa.orchestrator.core.SessionStatus;
import com.ats_connection.asa.orchestrator.helper.LogHelper;
import com.ats_connection.asa.orchestrator.response.WsAckRsp;

// Imports de StateMachine
import com.ats_connection.asa.orchestrator.sme.event.ActivationEvent;
import com.ats_connection.asa.orchestrator.sme.event.Event;
import com.ats_connection.asa.orchestrator.sme.event.EventTag;
import com.ats_connection.asa.orchestrator.sme.helper.Pair;
import com.ats_connection.asa.orchestrator.sme.api.SMEFacade;
import com.ats_connection.asa.orchestrator.sme.helper.SearchCriteria;
import com.ats_connection.asa.orchestrator.sme.Session.SessionData;

/**
 *
 * @author pdiaz
 */
@WebService(serviceName = "SMPP2WS", portName = "SMPP2WSSOAP", endpointInterface = "ats.smpp2ws.services.SMPP2WS", targetNamespace = "http://services.smpp2ws.ats", wsdlLocation = "WEB-INF/wsdl/smpp/SMPP2WS.wsdl")
public class SMPP implements SMPP2WS {
    
    public SMPP() throws Exception {
    	 LogHelper.webLog.info("ASA - SMPP API- Initializing");
    }

    public int smsReceivedInd(String smsOrigin, String smsDestination, String smsMessage, boolean binaryData) {
    	LogHelper.webLog.debug("smsReceivedInd - smsOrigin: " + smsOrigin);
        //busco el parametro sms_act_dest para verificar el destino
        //String destination = ServerConfiguration.getInstance().get("sms_act_dest");
        //LogHelper.webLog.debug("parameters: sms_act_dest " + destination );

    	return processCall(smsOrigin, smsOrigin, "SMS_RECEIVED", "OK");
    }

    public void alertNotification(String sourceAddress) {
    	LogHelper.webLog.debug("alertNotification - sourceAddress: " + sourceAddress);
    	processCall(null, sourceAddress, "ALERT_NOTIF", "OK");
        return; 
    }

    private int processCall(String msisdnFake, String msisdnReal, String operationId, String resultCode) {
        // Busco el subcribers
    	// Primero busco con el msisdnReal y luego con el msisdnFake, si esta
        Subscriber subscriber = null;
        subscriber = ConnectionManager.getInstance().getSubscriber(null, null, null, msisdnReal, null);
        
        if (subscriber == null) {
        	if (msisdnFake != null) {
        		subscriber = ConnectionManager.getInstance().getSubscriber(null, msisdnFake, null, null, null);
        	}
        		
    		if (subscriber == null) {
    			LogHelper.webLog.error("Subscriber doesn't exist");
    			return -1;
    		}
        }
        
        if (subscriber.getBlockedStatus().equals("B")) {
        	LogHelper.webLog.error("Subscriber blocked");
        	return -1;
        }

        if (subscriber.isExpired()) {
        	LogHelper.webLog.error("Subscriber Expired");
        	return -1;
        }
        
        try { 
            //buscamos la sesion en disco
            LogHelper.webLog.debug("Calling getSessionData");
            
            SessionService sessionWS = SessionService.getInstance();
            
            // Chequea Banda horaria
            if (!sessionWS.isNowInBand()) {
                //Fuera de horario
                LogHelper.webLog.error("Operation not processed.  Reason: Out of time band.");
                return -1;
            }
            
            EventTag eventTag = null;
            ActivationEvent evento = null;
		    String stateMachineName = null;
		    String stateMachineVersion = null;

            // Buscamos una sesion para este subscriber
            ManagementService managementWs = ManagementService.getInstance();
            SessionStatus sessionStatus = managementWs.getSessionStatus(subscriber.getMSISDN_t(), null, subscriber.getICCID());
            boolean setIMEI = false;
            
            // Si la sesion no existe
            if (sessionStatus == null) {
            	// Enviamos un evento SMS_START
                LogHelper.webLog.debug("[new session] iccid=" + subscriber.getICCID() + ", msisdnFake=" + subscriber.getMSISDN_t() );
                eventTag = EventTag.getSMSSTART();
			    stateMachineName = ServerConfiguration.getInstance().get("SMSStateMachineName");
			    stateMachineVersion = ServerConfiguration.getInstance().get("SMSStateMachineVersion");
            }
            else {
            	LogHelper.webLog.debug("Session id: "+sessionStatus.sessionId+", state: "+sessionStatus.sesStatus+", error state: "+sessionStatus.errorStatus);
            	
			    stateMachineName = sessionStatus.stateMachineName;
			    stateMachineVersion = sessionStatus.stateMachineVersion;

			    if (sessionStatus.whereIs.equals("M")) {
            		LogHelper.webLog.debug("[session in memory]");
            		WsAckRsp rsp = sessionWS.operationResult(sessionStatus.sessionId, Integer.parseInt(sessionStatus.transactionId), 
            				                                   operationId, resultCode, "Call from SMPP2WS received", null);
            		return rsp.resultCode.intValue();
            	}
            	else {
            		setIMEI = true;
            		
            		if (sessionStatus.finished.equals("T")) {
            			if (SMEFacade.getInstance().getStateTag(sessionStatus.sesStatus, sessionStatus.stateMachineName, sessionStatus.stateMachineVersion).isPersist()) {
            				LogHelper.webLog.debug("Session persisted found, state:" + sessionStatus.sesStatus);
            				// Buscamos alguna configuracion sino usamos el default
							String resumeEvent = ConnectionManager.getInstance().getResumeEvent("SMS_START", sessionStatus.sesStatus);
							if (resumeEvent == null)
								eventTag = EventTag.getResumeSessionActivation();
							else
								eventTag = new EventTag(resumeEvent, Event.Type.ACTIVATION.name(), Event.SubType.resumeActivation.name());
            			}
            			else if (SMEFacade.getInstance().getStateTag(sessionStatus.sesStatus, sessionStatus.stateMachineName, sessionStatus.stateMachineVersion).isFinalError()) {
           					 eventTag = EventTag.getSMSSTART();
            			}
            			else
            				return -1;
            		}
            		else {
            			if (SMEFacade.getInstance().getStateTag(sessionStatus.sesStatus, sessionStatus.stateMachineName, sessionStatus.stateMachineVersion).isUserInfo() ||
            				 SMEFacade.getInstance().getStateTag(sessionStatus.sesStatus, sessionStatus.stateMachineName, sessionStatus.stateMachineVersion).isActivation()) 
            			{
            				eventTag = EventTag.getSMSSTART();
            			}
            			else return -1;
            		}
            	}
            }
        	
            // Dentro del horario, enviamos el evento de Start
            
            evento = new ActivationEvent(eventTag, 0);
            
            // Carga identificador de sesion en el evento
 			if (eventTag.isResumeActivation()) {
 				// Es un resume session
 				evento.setICCID(sessionStatus.iccid);
 				evento.setMSISDN_FAKE(sessionStatus.msisdn_t);
 				evento.setIMSI_FAKE(sessionStatus.imsi_t);
 				evento.setIMEI(sessionStatus.imei);
 				evento.setLOCI(sessionStatus.loci);
 				evento.setIMSI(sessionStatus.imsi_real);
 				evento.setMSISDN(sessionStatus.msisdn_real);
 				evento.setIMSI_ROAMING(sessionStatus.imsi_roaming);
 				evento.setAreaCode(sessionStatus.areaCode);
 				evento.setActivationType(sessionStatus.activationType);
 	    		evento.setVirtualNetwork(sessionStatus.virtualNetwork);
 	    		evento.setApplication(sessionStatus.subApplication);
 	    		evento.setOpc(sessionStatus.opc);
 	    		evento.setOrigin(sessionStatus.origin);
 	    		evento.setUserType(sessionStatus.userType);
 	    		evento.setCardType(sessionStatus.cardType);
 	    		evento.setOpKey(sessionStatus.operatorKey);
 				
 				evento.addArg("OPERATION", sessionStatus.auxiliarData.get("OPERATION"));
 				evento.addArg("serverId",ServerConfiguration.getInstance().get("serverId"));
 				
 				int idxa=0;
 				// BEGIN Agrego parametros auxiliares
 				LogHelper.webLog.debug("Deserializing Session AuxiliarData...");
 				if (sessionStatus.auxiliarData != null) {
 					for (Map.Entry<String, String> elem : sessionStatus.auxiliarData.entrySet()) {
 						evento.setAuxiliarData((String) elem.getKey(),(String)elem.getValue());
 						if (elem.getKey().equals("CARDTYPE")) {
 							evento.setCardType((String) elem.getValue());
 						} else if (elem.getKey().equals("VIRTUALNETWORK")) {
 							evento.setVirtualNetwork(Long.parseLong((String) elem.getValue()));
 						} else if (elem.getKey().equals("APPLICATION")) {
 							evento.setApplication(Long.parseLong((String) elem.getValue()));
 						}
 						idxa++;
 					}
 					LogHelper.webLog.debug("Deserializing Session AuxiliarData finished with " + idxa + "elements.");
 				}
 				// END
 				evento.addArg("ResumedFrom", sessionStatus.sessionId);
 				evento.setAuxiliarData("ResumedFrom", sessionStatus.sessionId);
 				evento.addArg("ORIGIN_ACTIVATION", sessionStatus.originActivation);
 				evento.setAuxiliarData("ORIGIN_ACTIVATION", sessionStatus.originActivation);
 				evento.setAuxiliarData("RETRY", "10");
 			}
 			else {
	            if (setIMEI && sessionStatus.imei != null) evento.setIMEI(sessionStatus.imei);
	            
	            evento.setMSISDN_FAKE(subscriber.getMSISDN_t());
	            evento.setIMSI_FAKE(subscriber.getIMSI_t());
	            evento.setICCID(subscriber.getICCID());
	            evento.setVirtualNetwork(subscriber.getVirtualNetwork());
	            evento.setApplication(subscriber.getSubApplication());
	            evento.setOpc(subscriber.getOpc());
	            evento.setIMSI_ROAMING(subscriber.getImsiRoaming());
	            evento.setOrigin(subscriber.getOrigin());
	            evento.setUserType(subscriber.getUserType());
	            evento.setCardType(subscriber.getCardType());
	            evento.setOpKey(subscriber.getOperatorKey());
	            
	            evento.setAuxiliarData("CARDTYPE",  String.valueOf(subscriber.getCardType()));
	            evento.setAuxiliarData("VIRTUALNETWORK",  String.valueOf(subscriber.getVirtualNetwork()));
	            evento.setAuxiliarData("APPLICATION",  String.valueOf(subscriber.getSubApplication()));
 			}
             
			// Add StateMachine Name and version
			evento.addArg("StateMachineName", stateMachineName);
			evento.addArg("StateMachineVersion",stateMachineVersion);
 			
            Pair<Integer,String> pseResult = sessionWS.postStartEvent(SMEFacade.getInstance(),evento);

            if (pseResult == null) {
                LogHelper.webLog.error("postStartEvent returned null.");
                return -1;
            }
            
            LogHelper.webLog.error("postStartEvent returned (" + pseResult.getLeft() + ", " + pseResult.getRight() + ")" );

            if (pseResult.getLeft().equals(new Integer(0))) {
                SearchCriteria search2 = new SearchCriteria(pseResult.getRight().toString(), null, msisdnFake, null);
                SessionData sd2 = SMEFacade.getInstance().getSessionBySearchCriteria(search2);

                if (sd2 != null) {
                    LogHelper.webLog.debug( "Session created sessionId=" + sd2.getID() );
                    ManagementService.getInstance().persistSessionData(sd2.getID());  // Persist Session
                }
                else {
                    LogHelper.webLog.error("new session not created yet.");
                }
            }
            else {
                LogHelper.webLog.error("postEvent error: " + pseResult.getLeft());
                return -1;
            }
        
        } 
        catch (Exception ex) {
            LogHelper.webLog.error("Exception: " + ex, ex);
            return -1;
        }
        
        return 0;
    }

    public int deliveryReceiptInd(String smsOrigin, String smsDestination, String smsMessage, 
    		                                        String messageId, int messageState, int networkErrorCode) 
    {
    	// Armamos el resultCode como messageState-networkErrorCode
    	StringBuilder resultCode = new StringBuilder();
    	resultCode.append(messageState).append('-').append(networkErrorCode);
    			
    	LogHelper.webLog.debug("deliveryReceiptInd - messageId: "+messageId+", smsOrigin: " + smsOrigin + 
    			", messageState-networkErrorCode: "+resultCode.toString());
    	
    	return processCall(smsOrigin, smsOrigin, "DELIVERY_RECEIPT", resultCode.toString());
    }


}