/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ats_connection.asa.orchestrator.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map; 
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

import com.ats_connection.asa.orchestrator.config.ConnectionManager;
import com.ats_connection.asa.orchestrator.config.ServerConfiguration;
import com.ats_connection.asa.orchestrator.config.Subscriber;
import com.ats_connection.asa.orchestrator.core.Constants;
import com.ats_connection.asa.orchestrator.core.ManagementService;
import com.ats_connection.asa.orchestrator.core.SessionService;
import com.ats_connection.asa.orchestrator.core.SessionStatus;
import com.ats_connection.asa.orchestrator.helper.LogHelper;
import com.ats_connection.asa.orchestrator.response.WsAckRsp;
import com.ats_connection.asa.orchestrator.response.provisioningSIMRsp;

// Imports de la StateMachine
import com.ats_connection.asa.orchestrator.sme.api.SMEFacade;
import com.ats_connection.asa.orchestrator.sme.event.ActivationEvent;
import com.ats_connection.asa.orchestrator.sme.event.Event;
import com.ats_connection.asa.orchestrator.sme.event.EventTag;
import com.ats_connection.asa.orchestrator.sme.exception.MalformedEventException;
import com.ats_connection.asa.orchestrator.sme.helper.Pair;
import com.ats_connection.asa.orchestrator.sme.stma.state.StateTag;

/**
 *
 * @author pdiaz
 *
 */
@WebService()
public class HLR {
	private static List<String> pendingStartHlrRequest = new ArrayList<>();
 
	private static Lock lock = new ReentrantLock();
	private static Condition testCondition = lock.newCondition();
	
	private static final String CONST_UPDATELOCATION 		= "UpdateGPRSLocation";
	private static final String CONST_UPDATEGPRSLOCATION 	= "UpdateLocation";
	
	public HLR() {
		LogHelper.webLog.info("ASA - HLR API- Initializing");
	}

	/**
	 * Inicia una sesion HLR_START
	 * 
	 * @param imsi  IMSI fake
	 * @param msisdn  MSISDN fake
	 * @param activationType tipo de activacion
	 * @param origin origen, el que inicia la sesion
	 * 
	 * @return
	 * 
	 * @throws SOAPException
	 */
	
	@WebMethod(operationName = "startHLRSession")
	public provisioningSIMRsp startHLRSession (
			@WebParam(name = "imsi") String imsi,
			@WebParam(name = "msisdn") String msisdn,
			@WebParam(name = "activationType") String activationType,
			@WebParam(name = "origin") String origin)
	{
		provisioningSIMRsp procSIMRsp = new provisioningSIMRsp();
		
		LogHelper.webLog.debug("startHLRSession: IMSI=" + imsi	+ "; MSISDN=" + msisdn + "; activationType=" + activationType + "; origin=" + origin);

		if (!ServerConfiguration.getInstance().serverRunning()) {
			LogHelper.webLog.fatal(" Operation not processed.  Reason:Server stopped ");

			procSIMRsp.resultCode = 1;
			procSIMRsp.resultDescription = "Error: IMSI and MSISDN are empty";
			return procSIMRsp;
		}
		
		// Control de parametros
		// Alguno de ellos debe tener contenido...
		if ((msisdn == null || msisdn.length() == 0) 
			&& (imsi == null || imsi.length() == 0))
		{
			LogHelper.webLog.fatal("Error: IMSI and MSISDN are empty");
			procSIMRsp.resultCode = 1;
			procSIMRsp.resultDescription = "Error: IMSI and MSISDN are empty";
			return procSIMRsp;
		}
		
		lock.lock();
		try {
			while (pendingStartHlrRequest.contains(imsi)) {
				testCondition.await();
			}
			pendingStartHlrRequest.add(imsi);
		}
		catch (InterruptedException e) {
			LogHelper.webLog.fatal("testCondition.await() interrupted");
		}
		finally {
			lock.unlock();		
		}
  
		procSIMRsp = startSessionExec(imsi, msisdn, activationType, origin);
		
		lock.lock();
		try {
			if (pendingStartHlrRequest.contains(imsi)) {
				pendingStartHlrRequest.remove(imsi);
				testCondition.signal();
			}
		}
		finally {
			lock.unlock();		
		}
		
		return procSIMRsp;
	}
	/**
	 * ProvisioningSIMExec Es llamado por provisioningSIM (WS) (ver doc de
	 * provisioningSIM ws para mas detalles)
	 * 
	 * @param ICCID
	 * @param IMSI_d
	 * @param IMSI_Roaming
	 * @param MSISDN_d
	 * @param countryCode
	 * @param areaCode
	 * @param Origin
	 * @return
	 */
	private provisioningSIMRsp startSessionExec(String imsiFake, String msisdnFake,
		 String activationType, String Origin) 
	{
		provisioningSIMRsp procSIMRsp = new provisioningSIMRsp();
			
		SessionService sessionWS;
		try {
			sessionWS = SessionService.getInstance();
			// Chequea Banda horaria
//			if (!sessionWS.isNowInBand()) {
//				LogHelper.webLog
//				.fatal("HLR StartSession Operation not processed.  Reason:Out of time band ");
//
//				procSIMRsp.resultCode = 0;
//				procSIMRsp.resultDescription = "Out of time band.";
//
//				return procSIMRsp;
//			}			
		} 
		catch (Exception e) {
			LogHelper.webLog
			.debug("HLRStartSession: Could not instanciate SessionService");
			procSIMRsp.resultCode = 200;
			procSIMRsp.resultDescription = "Could not instanciate SessionService.";
			return procSIMRsp;
		}

		SMEFacade sme = SMEFacade.getInstance();
		if (sme == null) {
			LogHelper.webLog.debug("HLRStartSession: Could not instanciate SME Module");
			procSIMRsp.resultCode = Constants.SM_NOT_FOUND;
			procSIMRsp.resultDescription = Constants.SM_NOT_FOUND_DESC;
			return procSIMRsp;
		}
		
		// Controlo existencia de suscriptor
        int subscriberCheck = 0;
        String subscriberCheckDes = "OK";
        
        Subscriber sb = ConnectionManager.getInstance().getSubscriber(null, msisdnFake, imsiFake, null, null);
        //String vlrNUmber = ConnectionManager.getInstance().getVlrNumber(imsiFake, activationType);
        //String vlrNUmber = ConnectionManager.getInstance().getVlrNumber(imsiFake, Origin);
        
        if (sb == null) subscriberCheck = 1;
        else if (sb.getBlockedStatus().equals("B")) { subscriberCheck = 2; subscriberCheckDes = "SIM Blocked"; }
        else if (sb.isExpired()) { subscriberCheck = 3; subscriberCheckDes = "SIM Expired"; }
        else if (!sb.isBatchProccesedOk()) {subscriberCheck = 4; subscriberCheckDes = "Batch was not processed Ok"; }
        else if (sb.isBatchBlocked() ) {subscriberCheck = 5; subscriberCheckDes = "Batch is Blocked"; }
        
//        if (sb.getActivationType() != null && sb.getActivationType() != "PREPAID_STORE") {
//    		activationType = "PREPAID";        	
//        }
        
		if (subscriberCheck != 1) {
			// Buscamos una sesion para este subscriber
		    ManagementService managementWs = ManagementService.getInstance();
		    SessionStatus sessionStatus = managementWs.getSessionStatus(msisdnFake, null, sb.getICCID());
		    if (sessionStatus==null) sessionStatus = managementWs.getSessionStatus(null, null, sb.getICCID());  //pdd2018 para activaciones desde el store
		    
		    ActivationEvent event = null;
		    EventTag eventTag = null;
		    String stateMachineName = null;
		    String stateMachineVersion = null;
		    
			if (sessionStatus == null) {
				// No existe una sesion para este subscriber	
				// Creamos una nueva
				
				if (sb.getActivationType() != null && sb.getActivationType().equals("SIM_SWAP")) {
					LogHelper.webLog.debug("New session HLR_START_SIM_SWAP");
					eventTag = EventTag.getHLRSTARTSIMSWAP();
				    stateMachineName = ServerConfiguration.getInstance().get("ChangeIMSIStateMachineName");
				    stateMachineVersion = ServerConfiguration.getInstance().get("ChangeIMSIStateMachineVersion");
				}
				else {
					LogHelper.webLog.debug("New session HLR_START");
					eventTag = EventTag.getHLRSTART();
				    stateMachineName = ServerConfiguration.getInstance().get("HLRStateMachineName");
				    stateMachineVersion = ServerConfiguration.getInstance().get("HLRStateMachineVersion");
				}
			}
			else {
				// Existe una sesion para este subscriber
				// Primero vemos si esta en memoria o en la base
				// Si esta en memoria mandamos el evento,
				// sino, vemos
				if (sessionStatus.whereIs.equals("M")) {
					LogHelper.webLog.debug("Session running, sending operationResult HLR_START-OK");
					WsAckRsp rsp = sessionWS.operationResult(sessionStatus.sessionId, Integer.parseInt(sessionStatus.transactionId), 
                                "HLR_START", "OK", "HLR_START from "+msisdnFake, null);
						
					//procSIMRsp.resultCode = rsp.resultCode;
					procSIMRsp.resultCode = 0L;
					procSIMRsp.resultDescription = rsp.resultDescription;
					return procSIMRsp;
					
//					LogHelper.webLog.debug("Session running, ignore de request");													 
//		
//					procSIMRsp.resultCode = 0L;
//					procSIMRsp.resultDescription = "There is another session running and activating this SIM";
//					return procSIMRsp;																		   
						
				}
				else {
					// Esta en la base, vemos si tenemos que reasumir o no
					// Vemos como es esa sesion
					if (sessionStatus.finished.equals("T")) {
						StateTag state = sme.getStateTag(sessionStatus.sesStatus, sessionStatus.stateMachineName, sessionStatus.stateMachineVersion);
//						if (state.isFinalError()) {
//							if (sessionStatus.errorStatus != null && !sessionStatus.errorStatus.isEmpty()) {
//		        				LogHelper.webLog.debug("startExec: Session ended with error found, errorState:" + sessionStatus.errorStatus + ", origin:" + sessionStatus.originActivation);
//		        				if (sme.getStateTag(sessionStatus.errorStatus, sessionStatus.stateMachineName, sessionStatus.stateMachineVersion).isProgramming() ||
//		        					 sme.getStateTag(sessionStatus.errorStatus, sessionStatus.stateMachineName, sessionStatus.stateMachineVersion).isUserInfo()) 
//		        				{
//		        					// Hubo error en la programacion, enviamos un start
//		        					//  Dependiendo el tipo de activacion inicial se envia
//		        					//  HLR_START_STORE o HLR_START
//		        					if (sessionStatus.originActivation.equals("STORE_START"))  // pdd2018
//		        					{															//
//			        					eventTag = EventTag.getHLRSTARTSTORE();				//
//		        					}
//		        					else {
//		        						eventTag = EventTag.getHLRSTART();
//		        					}
//									stateMachineName = sessionStatus.stateMachineName;
//									stateMachineVersion = sessionStatus.stateMachineVersion;
//		        				}
//		        				else {
//	            					// Si el error fue durante la activacion
//	            					// Sesion finalizada con error activando SIM.  No hay mas chances.        
//	            					LogHelper.webLog.debug("checking activating_sim error");
//	            					
//	            					procSIMRsp.resultCode = 808L;
//	            					//procSIMRsp.resultDescription = "sesST:" + sessionStatus.sesStatus + " errST:" + sessionStatus.errorStatus + " phase:" + sme.getStateTag(sessionStatus.errorStatus).getPhase();
//	            					procSIMRsp.resultDescription = ServerConfiguration.getInstance().get("usrmsg_err_activating_sim");
//	            					LogHelper.webLog.debug("Sending msisdn="+msisdnFake+": usrmsg_err_activating_sim message");
//	            					return procSIMRsp;
//		        				}
//							}
//							else {
//								// Puede ser una sesion cancelada sin error
//								eventTag = EventTag.getHLRSTART();
//								stateMachineName = sessionStatus.stateMachineName;
//								stateMachineVersion = sessionStatus.stateMachineVersion;
//							}
//						}
//						else if (state.isPersist()) {
						if (state.isPersist()) {
							// Tenemos una sesion persistida
							// Buscamos alguna configuracion sino usamos el default
							// original -- String resumeEvent = ConnectionManager.getInstance().getResumeEvent("HLR_START", state.getName());
							//pdd 2018 para soportar store start
							String resumeEvent = ConnectionManager.getInstance().getResumeEvent(sessionStatus.originActivation, state.getName());
								
							if (resumeEvent == null)
								eventTag = EventTag.getResumeSessionActivation();
							else eventTag = new EventTag(resumeEvent, Event.Type.ACTIVATION.name(), Event.SubType.resumeActivation.name());
							
							stateMachineName = sessionStatus.stateMachineName;
							stateMachineVersion = sessionStatus.stateMachineVersion;
						}
						else {
							eventTag = EventTag.getHLRSTART();
							stateMachineName = sessionStatus.stateMachineName;
							stateMachineVersion = sessionStatus.stateMachineVersion;

//							LogHelper.webLog.debug("Session not persisted, checking activating_sim error");
//        					
//        					procSIMRsp.resultCode = 808L;
//        					procSIMRsp.resultDescription = ServerConfiguration.getInstance().get("usrmsg_err_activating_sim");
//        					LogHelper.webLog.debug("Sending msisdn="+msisdnFake+": usrmsg_err_activating_sim message");
//        					return procSIMRsp;
						}
    				}
					else {
						// No esta finalizada pero esta en la base...
						eventTag = EventTag.getHLRSTART();
						stateMachineName = sessionStatus.stateMachineName;
						stateMachineVersion = sessionStatus.stateMachineVersion;
					}
				}
			}
			
			try {
				event = new ActivationEvent(eventTag, 0); 
				
				// Carga identificador de sesion en el evento
				if (eventTag.isResumeActivation()) {
					// Es un resume session
					event.setICCID(sessionStatus.iccid);
					if (msisdnFake!=null && !msisdnFake.isEmpty())
						event.setMSISDN_FAKE(msisdnFake);
					else
						event.setMSISDN_FAKE(sessionStatus.msisdn_t);
					event.setIMSI_FAKE(sessionStatus.imsi_t);
					event.setIMEI(sessionStatus.imei);
					event.setLOCI(sessionStatus.loci);
					event.setIMSI(sessionStatus.imsi_real);
					event.setMSISDN(sessionStatus.msisdn_real);
					event.setIMSI_ROAMING(sessionStatus.imsi_roaming);
					event.setAreaCode(sessionStatus.areaCode);
					event.setActivationType(sessionStatus.activationType);
		    		event.setVirtualNetwork(sessionStatus.virtualNetwork);
		    		event.setApplication(sessionStatus.subApplication);
		    		event.setOpc(sessionStatus.opc);
		    		event.setOrigin(sessionStatus.origin);
		    		event.setUserType(sessionStatus.userType);
		    		event.setCardType(sessionStatus.cardType);
		    		event.setOpKey(sessionStatus.operatorKey);
					event.addArg("OPERATION", sessionStatus.auxiliarData.get("OPERATION"));
					event.addArg("serverId",ServerConfiguration.getInstance().get("serverId"));
 	            	event.setAuxiliarData("ORIGIN", Origin);
 	            	event.setAuxiliarData("ICCID", sb.getICCID());
					
					int at_param=0;  //pdd2019
					int idxa=0;
					// BEGIN Agrego parametros auxiliares
					LogHelper.webLog.debug("startExec: Deserializing Session AuxiliarData...");
					if (sessionStatus.auxiliarData != null) {
						for (Map.Entry<String, String> elem : sessionStatus.auxiliarData.entrySet()) {
							event.setAuxiliarData((String) elem.getKey(),(String)elem.getValue());
							if (elem.getKey().equals("CARDTYPE")) {
								event.setCardType((String) elem.getValue());
							} else if (elem.getKey().equals("VIRTUALNETWORK")) {
								event.setVirtualNetwork(Long.parseLong((String) elem.getValue()));
							} else if (elem.getKey().equals("APPLICATION")) {
								event.setApplication(Long.parseLong((String) elem.getValue()));
							} else if (elem.getKey().equals("HAPPY")) {
								event.setAuxiliarData(elem.getKey(),(String) elem.getValue());
								at_param=1;
							} else if (elem.getKey().equals("PREPAGO")) {
								event.setAuxiliarData(elem.getKey(),(String) elem.getValue());
								at_param=1;
							} else if (elem.getKey().equals("POSPAGO")) {
								event.setAuxiliarData(elem.getKey(),(String) elem.getValue());
								at_param=1;
							}
							idxa++;
						}
						LogHelper.webLog.debug("startExec: Deserializing Session AuxiliarData finished with " + idxa + "elements.");
					}
					// END
					event.addArg("ResumedFrom", sessionStatus.sessionId);
					event.setAuxiliarData("ResumedFrom", sessionStatus.sessionId);
					event.addArg("ORIGIN_ACTIVATION", sessionStatus.originActivation);
					event.setAuxiliarData("ORIGIN_ACTIVATION", sessionStatus.originActivation);
					event.setAuxiliarData("RETRY", "10");
					
					//pdd2019
					// Si no se arrastró el tipo de activacion desde la sesion anterior
					// se determina a partir del activationType
					if(at_param==0 && activationType!=null) {
	 	            	if (activationType.equals("DIGITAL"))
	 	            		event.setAuxiliarData("HAPPY", "HPY");
	 	            	if (activationType.startsWith("PRE"))
	 	            		event.setAuxiliarData("PREPAGO", "PRE");
	 	            	if (activationType.startsWith("POS"))
	 	            		event.setAuxiliarData("POSPAGO", "POS");
					}	

				}
				else {
					// Es un start
					LogHelper.webLog.debug("startExec: Es un start");
	    			event.setICCID(sb.getICCID());
	    			event.setMSISDN_FAKE(msisdnFake);
	    			event.setIMSI_FAKE(imsiFake);
					event.setActivationType(updateVhlrActivationType(activationType));
	    			event.setOrigin(Origin);
	    			
	    			event.setVirtualNetwork(sb.getVirtualNetwork());
	 	            event.setApplication(sb.getSubApplication());
	 	            event.setOpc(sb.getOpc());
	 	            
	 	            event.setUserType(sb.getUserType());
	 	            event.setCardType(sb.getCardType());
	 	            event.setOpKey(sb.getOperatorKey());
	 	       	    
	 	            event.setAuxiliarData("CARDTYPE", sb.getCardType());
	 	            event.setAuxiliarData("VIRTUALNETWORK", String.valueOf(sb.getVirtualNetwork()));
	 	            event.setAuxiliarData("APPLICATION", String.valueOf(sb.getSubApplication()));
	 	            
	 	            //pdd2019 para solucionar problemas de grupos vacíos en sesiones iniciadas por HLR
 	            	if (activationType.equals("DIGITAL"))
 	            		event.setAuxiliarData("HAPPY", "HPY");
 	            	if (activationType.startsWith("PRE"))
 	            		event.setAuxiliarData("PREPAGO", "PRE");
 	            	if (activationType.startsWith("POS"))
 	            		event.setAuxiliarData("POSPAGO", "POS");
	 	            
 	            	//adding origin to auxData to determine if its hlr o hss
 	            	event.setAuxiliarData("ORIGIN", Origin);
 	            	event.setAuxiliarData("ICCID", sb.getICCID());
 	            	
 	            	if (sb.getActivationType() != null && sb.getActivationType().equals("SIM_SWAP")) { //es un simSwap ya tiene valores en la tabla suscribers
 					    event.setAreaCode(sb.getAreaCode());
 					    event.setIMSI_FAKE(sb.getIMSI_t());
 		    			event.setIMSI(sb.getIMSI_d());
 		    			event.setMSISDN(sb.getMSISDN_d());
 		    			LogHelper.webLog.debug("startExec: Es un start sim swap");
 	            	}
 	            	
// 	            	if (Origin.contentEquals("hlr")) {
// 	            		event.setAuxiliarData("VLR_NUMBER", vlrNUmber);
// 	            		event.setAuxiliarData("HLR_NUMBER", "");
// 	            		event.setAuxiliarData("ORIGIN", Origin);
// 	            		
// 	            	} else if (Origin.contentEquals("hss")) {
// 	            		event.setAuxiliarData("VLR_NUMBER", "");
// 	            		event.setAuxiliarData("HLR_NUMBER", vlrNUmber);
// 	            		event.setAuxiliarData("ORIGIN", Origin);
// 	            	}
				}
			}
			catch(MalformedEventException mee) {
				procSIMRsp.resultCode = 1L;
				procSIMRsp.resultDescription = "Exception creating event "+eventTag.getName()+": " + mee;
				LogHelper.webLog.error("Exception creating event "+eventTag.getName()+": " , mee);
				return procSIMRsp;
			}
			
			// Add StateMachine Name and version and subscriber check result
			event.addArg("StateMachineName", stateMachineName);
			event.addArg("StateMachineVersion",stateMachineVersion);
			
			event.setAuxiliarData("SUBSCHECKRES",String.valueOf(subscriberCheck));
			event.setAuxiliarData("SUBSCHECKDES",subscriberCheckDes);

			// send event to the state machine
			Pair<Integer, String> resp = sessionWS.postStartEvent(sme, event);
			
			if (resp == null) {
				LogHelper.webLog.error("startExec: postStartEvent returned null.");
				procSIMRsp.resultCode = Constants.POST_EVENT_ERROR;
				procSIMRsp.resultDescription = Constants.POST_EVENT_ERROR_DESC + ". Return null";
			    return procSIMRsp;
			}
			else {
			    LogHelper.webLog.debug("startExec: postStartEvent returned (" + resp.getLeft() + "," + resp.getRight() + ")");
			}

            // Reviso el Pair, si es cero, esta bien
            if (resp.getLeft().equals(new Integer(0))) {
                // Persist Session
                WsAckRsp mngResp = managementWs.persistSessionData(resp.getRight().toString());
                
                if (mngResp.resultCode == Constants.PERSIST_SESSION_INSERT_ERROR) {
                	LogHelper.webLog.error("Error on persistSessionData, retrying...");
                	mngResp = managementWs.persistSessionData(resp.getRight().toString());
                }
            	procSIMRsp.resultCode = mngResp.resultCode;
                procSIMRsp.resultDescription = mngResp.resultDescription;
                procSIMRsp.sessionId = resp.getRight().toString();
            }
            else {
            	procSIMRsp.resultCode = Constants.SESSION_CREATION_ERROR;
            	procSIMRsp.resultDescription = Constants.SESSION_CREATION_ERROR_DESC;
                LogHelper.webLog.error("startExec: postEvent error: " + resp.getLeft());
            }
		}
		else {
			LogHelper.webLog.error("StartSIMActivation - Subscriber not found, blocked or expired for MSISDN_FAKE "+msisdnFake);
			procSIMRsp.resultCode = Constants.SUBSCRIBER_NOT_FOUND;
			procSIMRsp.resultDescription = Constants.SUBSCRIBER_NOT_FOUND_DESC + " or " + Constants.SUBSCRIBER_BLOCKED_DESC;
			return procSIMRsp;
		}
		return procSIMRsp;
	}
	
	private String updateVhlrActivationType (String activationType) {

		if ( activationType.equals(CONST_UPDATELOCATION) || activationType.equals(CONST_UPDATEGPRSLOCATION) ) {
			
			return "PREPAID_ACTIVATION";
			
		}
		
		else return activationType;
		
	}
}
