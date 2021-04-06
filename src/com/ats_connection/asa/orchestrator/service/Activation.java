/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ats_connection.asa.orchestrator.service;

import com.ats_connection.asa.orchestrator.config.ConnectionManager;
import com.ats_connection.asa.orchestrator.config.ServerConfiguration;
import com.ats_connection.asa.orchestrator.config.Subscriber;
import com.ats_connection.asa.orchestrator.core.ManagementService;
import com.ats_connection.asa.orchestrator.core.SessionService;
import com.ats_connection.asa.orchestrator.core.SessionStatus;
import com.ats_connection.asa.orchestrator.helper.LogHelper;
import com.ats_connection.asa.orchestrator.helper.Utils;
import com.ats_connection.asa.orchestrator.response.WsAckRsp;
import com.ats_connection.asa.orchestrator.service.activationservice.*;
import com.ats_connection.asa.orchestrator.sme.api.SMEFacade;
import com.ats_connection.asa.orchestrator.sme.event.ActivationEvent;
import com.ats_connection.asa.orchestrator.sme.event.EventTag;
import com.ats_connection.asa.orchestrator.sme.exception.MalformedEventException;
import com.ats_connection.asa.orchestrator.sme.helper.Pair;

import java.util.Map;

import javax.jws.WebService;

/**
 *
 * @author airibarren
 */
@WebService(
        serviceName = "ActivationService", 
        portName = "ActivationServiceSOAP", 
        endpointInterface = "com.ats_connection.asa.orchestrator.service.activationservice.ActivationService", 
        targetNamespace = "http://service.orchestrator.asa.ats_connection.com/ActivationService/", 
        wsdlLocation = "WEB-INF/wsdl/Activation/ActivationService.wsdl")
public class Activation {
	
    /**
     * Inicia una activacion. Esto es, continua con la sesion luego de obtener los datos del usuario.
     * 
     * @param sessionId ID de la sesion del Orchestrator
     * @param type tipo de activacion (ACTIVATION o SIMCARD_CHANGE)
     * @param data datos de usuario en formato operationResult (dato1|dato2|....)
     * @param origin
     * @param originSessionId
     * @param serviceCode
     * @param appVersion
     * @return 
     */
    public StartActivationVO startActivation(String sessionId, String type, String data, String origin, 
    		String originSessionId, String serviceCode, String appVersion) 
    {
    	LogHelper.webLog.info("startActivation("+sessionId+","+type+","+data+","+origin+","+originSessionId+","+serviceCode+","+appVersion+")");
    	
        StartActivationVO response = new StartActivationVO();
        // Verifico los datos
        if (data == null || data.isEmpty()) {
            response.setResultCode("-1");
            response.setResultDescription("Field data empty or null");
            LogHelper.dbLog(sessionId, null, "HIGH", "HIGH", "ActivationService.startActivation", "User data null or empty");
            LogHelper.webLog.error("Field data is empty or null");
            return response;
        }
        
        try {
            SessionService sessionService = SessionService.getInstance();
            WsAckRsp opeRes = sessionService.operationResult(sessionId, -1, type, "OK", "User info received for "+type, data);
            response.setResultCode(opeRes.resultCode.toString());
            response.setResultDescription(opeRes.resultDescription);
        } catch (Exception ex) {
            response.setResultCode("-3");
            response.setResultDescription("Internal error: "+ex.getMessage());
            LogHelper.dbLog(sessionId, null, "HIGH", "HIGH", "ActivationService.startActivation", "Internal error");
        }
        
        return response;
    }

    /**
     * Obtiene el estado de una activacion
     * 
     * @param sessionId ID de la sesion del SLEE
     * @param msisdnF
     * @param origin
     * @return 
     */
    public GetActivationStatusVO getActivationStatus(String sessionId, String msisdnF, String origin) {
    	LogHelper.webLog.info("getActivationStatus("+sessionId+","+msisdnF+","+origin+")");
    	
        GetActivationStatusVO response = new GetActivationStatusVO();
        response.setDdd(" ");
        response.setImei(" ");
        response.setImsiF(" ");
        response.setSessionId(" ");
        response.setSessionStatus(" ");
        response.setSessionStatusCode(" ");
        response.setUserMessage(" ");
        
        // Verificamos el msisdn
        if (msisdnF == null || msisdnF.isEmpty()) {
        	response.setResultCode("-2");
        	response.setResultDescription("MSISDN is empty or null");
        	return response;
        }
        
        ManagementService managementService = ManagementService.getInstance();
        SessionStatus sessionStatus = managementService.getSessionStatus(msisdnF, null, null);
        if (sessionStatus != null) {
        	LogHelper.dbLog(sessionId, msisdnF, "HIGH", "HIGH", "ActivationService.getActivationStatus", "Session found");
            response.setResultCode("0");
            response.setResultDescription("Session found");
            
            response.setImsiF(sessionStatus.imsi_t);
            response.setSessionId(sessionStatus.sessionId);
            response.setSessionStatus(sessionStatus.sesStatus);
            response.setImei(sessionStatus.imei);
            response.setSessionStatusCode(sessionStatus.sesStatusPhase);
            
            String lac_pos = ServerConfiguration.getInstance().get("areaCode_pos");
            String lac_len = ServerConfiguration.getInstance().get("areaCode_len");
            int pos, len;
            
            try {
            	pos = Integer.parseInt(lac_pos);
            }
            catch(NumberFormatException nfe) {
            	pos = 0;
            }
            try {
            	len = Integer.parseInt(lac_len);
            }
            catch(NumberFormatException nfe) {
            	len = 0;
            }
            
            String areaCode = Utils.getAreaCodeFromLoci(sessionStatus.loci, len, pos);
            if (areaCode != null && !areaCode.isEmpty())
            	response.setDdd(areaCode);
            
            String msg = ConnectionManager.getInstance()
            		.getDisplayMessage(sessionStatus.originActivation, sessionStatus.sesStatus, "*", sessionStatus.stateMachineName, sessionStatus.stateMachineVersion);
            if (msg != null && !msg.isEmpty())
            	response.setUserMessage(msg);
        }
        else {
            response.setResultCode("-1");
            response.setResultDescription("Session not found for MSISDN_FAKE "+msisdnF);
            LogHelper.dbLog(sessionId, msisdnF, "HIGH", "HIGH", "ActivationService.getActivationStatus", "Session not found");
            LogHelper.webLog.info("Session not found");
        }
        
        return response;
    }

    /**
     * Inicia una sesion
     * 
     * @param msisdnF
     * @param origin el origen de la activacion
     * @param originSessionId
     * @param serviceCode
     * @param appVersion
     * @return 
     */
    public StartSessionVO startSession(String msisdnF, String origin, String originSessionId, 
            String serviceCode, String appVersion) 
    {
    	LogHelper.webLog.info("startSession("+msisdnF+","+origin+","+originSessionId+","+serviceCode+","+appVersion+")");
    	
    	StartSessionVO response = new StartSessionVO();
    	response.setDdd(" ");
    	response.setImei(" ");
    	response.setImsiF(" ");
    	response.setSessionId(" ");
    	response.setUserMessage(" ");
    	
    	// Verificamos el MSISDN_FAKE
    	if (msisdnF == null || msisdnF.isEmpty()) {
    		response.setResultCode("-1");
        	response.setResultDescription("MSISDN_FAKE empty or null");
        	return response;
    	}
    	
    	SessionService sessionService = null;
    	try {
			sessionService = SessionService.getInstance();
		} 
    	catch (Exception e) {
    		response.setResultCode("-5");
        	response.setResultDescription("Internal error getting instance of SessionService: "+e.getMessage());
        	return response;
		}
    	
    	response.setResultCode("0");
    	response.setResultDescription("OK");
    	
    	// Verificamos la banda horaria
    	if (sessionService.isNowInBand()) {
    		// Verificamos el subscriber
    		Subscriber subscriber = ConnectionManager.getInstance().getSubscriber(null, msisdnF, null, null, null);
    		if (subscriber != null) {
    			ActivationEvent event = null;
    			// Verificamos sesiones para este subscribers
    			SessionStatus sessionStatus = ManagementService.getInstance().getSessionStatus(msisdnF, null, subscriber.getICCID());
    			
    			try {
	    			if (sessionStatus == null) {
	    				// No existe una sesion para este subscriber
	    				// Creamos una nueva
	    				LogHelper.webLog.debug("New session USSD_START");
	    				event = new ActivationEvent(EventTag.getUSSDSTART(), 0);
	    			}
	    			else {
	    				// Existe una sesion para este subscriber
	    				// Vemos como es esa sesion
	    				if (sessionStatus.finished.equals("T")) {
	    					// Esta finalizada
	    					response.setSessionId(sessionStatus.sessionId);
	    					response.setImei(subscriber.getIMEI());
	    					response.setImsiF(subscriber.getIMSI_t());
	    					response.setDdd(sessionStatus.areaCode);
	    					response.setUserMessage(ConnectionManager.getInstance().getDisplayMessage(originSessionId, sessionStatus.sesStatus, "*", sessionStatus.stateMachineName, sessionStatus.stateMachineVersion));
	    					response.setResultCode("-7");
	    					response.setResultDescription("Session finished for MSISDN "+msisdnF);	    					
	    					return response;
	    				}
	    				else {
	    					// Vemos si esta persistida
	    					if (sessionStatus.whereIs.equals("M")) {
	    						// esta en memoria
	    						response.setSessionId(sessionStatus.sessionId);
		    					response.setImei(subscriber.getIMEI());
		    					response.setImsiF(subscriber.getIMSI_t());
		    					response.setDdd(sessionStatus.areaCode);
		    					response.setUserMessage(ConnectionManager.getInstance().getDisplayMessage(originSessionId, sessionStatus.sesStatus, "*", sessionStatus.stateMachineName, sessionStatus.stateMachineVersion));
		    					response.setResultCode("0");
		    					response.setResultDescription("Session in process for MSISDN "+msisdnF);
		    					
		    					return response;
	    					}
	    					else {
	    						// Esta persistida
	    						LogHelper.webLog.debug("Resume session");
	    						event = new ActivationEvent(EventTag.getResumeSessionActivation(), 0);
	    					}
	    				}
	    			}
	    			
	    			event.setICCID(subscriber.getICCID());
	    			event.setMSISDN_FAKE(msisdnF);
	    			event.setIMSI_FAKE(subscriber.getIMSI_t());
	    			event.setIMEI(subscriber.getIMEI());
	    			
	    			event.setVirtualNetwork(subscriber.getVirtualNetwork());
	    			event.setApplication(subscriber.getSubApplication());
	    			event.setOpc(subscriber.getOpc());
	    			event.setOrigin(subscriber.getOrigin());
	    			event.setUserType(subscriber.getUserType());
	    			event.setCardType(subscriber.getCardType());
	    			event.setOpKey(subscriber.getOperatorKey());
	    			
	    			//ini pdd2019
	    			
	    			// Se agrega paa que recupere los datos auxiliares de una sesión al momento del RESUME
					int idxa=0;
					// BEGIN Agrego parametros auxiliares
					LogHelper.webLog.debug("startExec: Deserializing Session AuxiliarData...");
					if (sessionStatus != null)
					if (sessionStatus.auxiliarData != null) {
						for (Map.Entry<String, String> elem : sessionStatus.auxiliarData.entrySet()) {
							event.setAuxiliarData((String) elem.getKey(),(String)elem.getValue());
							if (elem.getKey().equals("CARDTYPE")) {
								event.setCardType(elem.getValue());
							} else if (elem.getKey().equals("VIRTUALNETWORK")) {
								event.setVirtualNetwork(Long.parseLong((String) elem.getValue()));
							} else if (elem.getKey().equals("APPLICATION")) {
								event.setApplication(Long.parseLong((String) elem.getValue()));
							}
							idxa++;
						}
						LogHelper.webLog.debug("startExec: Deserializing Session AuxiliarData finished with " + idxa + "elements.");
					}

					event.addArg("ResumedFrom", sessionStatus.sessionId);
					event.setAuxiliarData("ResumedFrom", sessionStatus.sessionId);
		    			
	    			
	    			
	    			// fin pdd2019
	    			
	    			event.setAuxiliarData("CARDTYPE", String.valueOf(subscriber.getCardType()));
	    			event.setAuxiliarData("VIRTUALNETWORK", String.valueOf(subscriber.getVirtualNetwork()));
	    			event.setAuxiliarData("APPLICATION", String.valueOf(subscriber.getSubApplication()));
	    			
	    			// Posteamos el evento
	    			Pair<Integer,String> pseResult = sessionService.postStartEvent(SMEFacade.getInstance(), event);
	    			if (pseResult == null) {
	    				// Error al postear evento
	    				response.setResultCode("-4");
	    				response.setResultDescription("Error creating session");
	    			}
	    			else {
	    				int errorCode = pseResult.getLeft().intValue();
	    				if (errorCode == 0) {
	    					// Creacion exitosa
	    					
	    					response.setImei(subscriber.getIMEI());
	    					response.setImsiF(subscriber.getIMSI_t());
	    					response.setSessionId(pseResult.getRight());
	    				}
	    				else {
	    					// Error creando la sesion
	    					response.setResultCode(pseResult.getLeft().toString());
	    					response.setResultDescription("Error creating session");
	    				}
	    			}
    			}
    			catch(MalformedEventException mee) {
    				response.setResultCode("-6");
    				response.setResultDescription("Error creating event USSD_START: "+mee.getMessage());
    				LogHelper.webLog.error("Error creating event: "+mee.getMessage());
    			}
    			
    		}
    		else {
    			// No existe el subscriber
    			response.setResultCode("-3");
        		response.setResultDescription("Subscriber not found");
        		LogHelper.webLog.error("Subscriber not found: "+msisdnF);
    		}
    	}
    	else {
    		// Esta fuera de horario
    		response.setResultCode("-2");
    		response.setResultDescription("Service out of time band");
    		LogHelper.webLog.error("Service out of time band");
    	}
    	
    	return response;
    }
    
}
