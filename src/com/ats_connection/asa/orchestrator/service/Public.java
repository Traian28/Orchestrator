/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ats_connection.asa.orchestrator.service;

import java.util.Map;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.soap.SOAPException;

import java.net.URL;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.namespace.QName;

import com.ats_connection.asa.orchestrator.config.ConnectionManager;
import com.ats_connection.asa.orchestrator.config.ServerConfiguration;
import com.ats_connection.asa.orchestrator.config.Subscriber;
import com.ats_connection.asa.orchestrator.core.Constants;
import com.ats_connection.asa.orchestrator.core.ManagementService;
import com.ats_connection.asa.orchestrator.core.SessionService;
import com.ats_connection.asa.orchestrator.core.SessionStatus;
import com.ats_connection.asa.orchestrator.helper.LogHelper;
import com.ats_connection.asa.orchestrator.response.SendNotificationRsp;
import com.ats_connection.asa.orchestrator.response.GetSIMInfoRsp;
import com.ats_connection.asa.orchestrator.response.WsAckRsp;
import com.ats_connection.asa.orchestrator.response.cancelActivationRsp;
import com.ats_connection.asa.orchestrator.response.getActivationStatusRsp;
import com.ats_connection.asa.orchestrator.response.provisioningSIMRsp;

// Imports de la StateMachine
import com.ats_connection.asa.orchestrator.sme.Session.SessionData;
import com.ats_connection.asa.orchestrator.sme.api.SMEFacade;
import com.ats_connection.asa.orchestrator.sme.event.ActivationEvent;
import com.ats_connection.asa.orchestrator.sme.event.Event;
import com.ats_connection.asa.orchestrator.sme.event.EventTag;
import com.ats_connection.asa.orchestrator.sme.exception.MalformedEventException;
import com.ats_connection.asa.orchestrator.sme.helper.Pair;
import com.ats_connection.asa.orchestrator.sme.helper.SearchCriteria;
import com.ats_connection.asa.orchestrator.sme.stma.executor.SessionEventExecutor;
import com.ats_connection.asa.orchestrator.sme.stma.state.StateTag;

/**
 *
 * @author pdiaz
 */
@WebService()
public class Public {
	
	private static final String CONST_UPDATELOCATION 		= "UpdateGPRSLocation";
	private static final String CONST_UPDATEGPRSLOCATION 	= "UpdateLocation";

	public Public() {
		LogHelper.webLog.info("ASA - Public API- Initializing");
	}
	
	/**
	 * Envia el evento MANUAL_CANCEL a la maquina de estados para la sesion
	 * indicada
	 * 
	 * @param origin
	 * @param sessionId
	 *            Id de la sesion a cancelar
	 * @return resultCode=0:OK, resultCode=77:Error del post de evento,
	 *         resultCode=98:Error inesperado
	 */
	@WebMethod(operationName = "cancelActivation")
	public cancelActivationRsp cancelActivation(
			@WebParam(name = "origin") String origin,
			@WebParam(name = "sessionId") String sessionId)
					throws SOAPException 
	{
		if (!ServerConfiguration.getInstance().serverRunning()) {
			LogHelper.webLog.fatal(" Operation not processed.  Reason:Server stopped ");
			throw new javax.xml.soap.SOAPException("ASA-Orchestrator server paused");
		}

		cancelActivationRsp retVal = new cancelActivationRsp();
		retVal.resultDescription = "OK";

		SMEFacade sme = SMEFacade.getInstance();

		SearchCriteria search = new SearchCriteria(sessionId, null, null, null);
		SessionData sd = sme.getSessionBySearchCriteria(search);		
		if (sd == null) {
			LogHelper.webLog.fatal(" Operation not processed.  Reason: Invalid SessionId");
			throw new javax.xml.soap.SOAPException("ASA-Orchestrator Invalid SessionId");
		}
		
		try {
			
			EventTag eventTag = new EventTag("MANUAL_CANCEL", Event.Type.ACTIVATION.name());
			ActivationEvent evt = new ActivationEvent(eventTag, 1);
			evt.setSessionID(sessionId);
			evt.addArg("StateMachineName",sd.getStateMachineName());
			evt.addArg("StateMachineVersion",sd.getStateMachineVersion());

			if (sme.post(evt)) {
				retVal.resultCode = 0L;
				retVal.resultDescription = "OK";
			} else {
				retVal.resultCode = 77L;
				retVal.resultDescription = "POST_ERROR";
			}
		} catch (Exception e) {
			retVal.resultCode = 98L;
			retVal.resultDescription = e.getMessage();
		}
		return retVal;
	}

	/**
	 * Obtiene el estado del proceso de activacion para una sesion
	 * 
	 * @param origin
	 * @param sessionId  Campo clave para la busqueda de la sesion
	 * @param ICCID  Campo clave para la busqueda de la sesion
	 * @param IMSI_d   No utilizado
	 * @param MSISDN_d   No utilizado
	 * @return resultCode=0 para indicar que la llamada al metodo se realizo correctamente
	 */
	@WebMethod(operationName = "getActivationStatus")
	public getActivationStatusRsp getActivationStatus(
			@WebParam(name = "origin") String origin,
			@WebParam(name = "sessionId") String sessionId,
			@WebParam(name = "ICCID") String ICCID,
			@WebParam(name = "IMSI_d") String IMSI_d,
			@WebParam(name = "MSISDN_d") String MSISDN_d) 
					throws SOAPException 
	{
		if (!ServerConfiguration.getInstance().serverRunning()) {
			LogHelper.webLog.fatal(" Operation not processed.  Reason:Server stopped ");
			// return -99;
			throw new javax.xml.soap.SOAPException("ASA-Orchestrator server paused");
		}

		getActivationStatusRsp retVal = new getActivationStatusRsp();
		SessionStatus sdt = ManagementService.getInstance().getSessionStatus(	null, sessionId, ICCID);

		if (sdt != null) {
			retVal.resultCode          = 0;
			retVal.resultDescription   = "OK";
			retVal.sessionStatus       = sdt.sesStatus;
			retVal.stateName 		   = sdt.stateName;
			retVal.stateMachineName    = sdt.stateMachineName;
			retVal.stateMachineVersion = sdt.stateMachineVersion;
			
			if (SMEFacade.getInstance().getStateTag(retVal.sessionStatus, sdt.stateMachineName, sdt.stateMachineVersion).isFinalError()) {
				retVal.sessionStatusCode = sdt.errorStatus;
				retVal.sessionStatusDescription = sdt.sesStatus;
			} 
			else {
				retVal.sessionStatusCode = "0";
				retVal.sessionStatusDescription = "OK";
			}
		} 
		else {
			if (sessionId != null) {
				retVal.resultCode = 10004;
				retVal.resultDescription = "Session " + sessionId + " not exists";
			} 
			else if (ICCID != null) {
				retVal.resultCode = 10002;
				retVal.resultDescription = "ICCID not found";
			} 
			else {
				retVal.resultCode = 1;
				retVal.resultDescription = "Internal error";
			}
		}

		return retVal;
	}

	/**
	 * Punto de entrada para la activacion de un SIM con el modo SINGLE
	 * 
	 * @param ICCID
	 * @param IMSI
	 * @param IMSI_Roaming
	 * @param MSISDN
	 * @param countryCode
	 * @param areaCode
	 * @param origin
	 * @return Devuelve un 
	 *     resultCode=0 cuando el proceso de activacion pudo ser iniciado 
	 *     resultCode=10002 Cuando no se encuentra un subscriptor 
	 *     resultCode=3 cuando no se pudo avanzar con el proceso de activacion 
	 *     resultCode=10000 Cuando no se puede crear la sesion por un problema interno 
	 *     resultCode=10005 Cuando existen problemas con el IMSI
	 * 
	 * @throws java.lang.InterruptedException
	 * @throws com.ats_connection.asa.orchestrator.sme.exception.
	 *             MalformedEventException
	 */
	@WebMethod(operationName = "startSIMActivation")
	public provisioningSIMRsp startSIMActivation(
			@WebParam(name = "ICCID") String ICCID,
			@WebParam(name = "IMSI") String IMSI_d,
			@WebParam(name = "IMSI_Roaming") String IMSI_Roaming,
			@WebParam(name = "MSISDN") String MSISDN_d,
			@WebParam(name = "countryCode") String countryCode,
			@WebParam(name = "areaCode") String areaCode,
			@WebParam(name = "activationType") String activationType,
			@WebParam(name = "origin") String origin)
					throws InterruptedException, MalformedEventException, SOAPException 
	{
		LogHelper.webLog.debug("startSIMActivation: Begin: ICCID=" + ICCID	+ " IMSI_d=" + IMSI_d);

		if (!ServerConfiguration.getInstance().serverRunning()) {
			LogHelper.webLog.fatal(" Operation not processed.  Reason:Server stopped ");
			// return -99;
			throw new javax.xml.soap.SOAPException("ASA-Orchestrator server paused");
		}

		String prvSimRequest = "ProvisioningSIM Request(" + "ICCID: " + ICCID
				+ ", IMSI_def: " + IMSI_d + ", IMSI_Roaming: " + IMSI_Roaming
				+ ", MSISDN_def: " + MSISDN_d + ", countryCode: " + countryCode
				+ ", areaCode: " + areaCode + ", activationType: " + activationType + ", origin: " + origin + ")";

		LogHelper.webLog.debug(prvSimRequest);

		provisioningSIMRsp procSIMRsp = new provisioningSIMRsp();
		
		// Control de parametros
		// Alguno de ellos debe tener contenido...
		if ((ICCID == null || ICCID.length() == 0) 
			&& (MSISDN_d == null || MSISDN_d.length() == 0) 
			&& (IMSI_d == null || IMSI_d.length() == 0))
		{
			procSIMRsp.resultCode = 1;
			procSIMRsp.resultDescription = "Error: ICCID, IMSI and MSISDN are empty";
			return procSIMRsp;
		}
		
		if (ICCID == null || ICCID.length() == 0 || ICCID.length() > 100) // Solucion
			// CT11580
		{
			procSIMRsp.resultCode = 1;
			procSIMRsp.resultDescription = "Error in ICCID parameter";
			return procSIMRsp;
		}
		
		if (IMSI_d == null || IMSI_d.length() == 0 || IMSI_d.length() > 32) {
			procSIMRsp.resultCode = 1;
			procSIMRsp.resultDescription = "Error in IMSI parameter";
			return procSIMRsp;
		}
		
		if (MSISDN_d == null || MSISDN_d.length() == 0	
			|| MSISDN_d.length() > 15 || !(MSISDN_d.matches("((-|\\+)?[0-9]+(\\.[0-9]+)?)+"))) 
		{
			procSIMRsp.resultCode = 1;
			procSIMRsp.resultDescription = "Error in MSISDN parameter";
			return procSIMRsp;
		}
		
		if (origin == null || origin.length() == 0 || origin.length() > 32)
		{
			procSIMRsp.resultCode = 1;
			procSIMRsp.resultDescription = "Error in origin parameter";
			return procSIMRsp;
		}
		
		if (areaCode == null || areaCode.length() == 0 || areaCode.length() > 5)
		{
			procSIMRsp.resultCode = 1;
			procSIMRsp.resultDescription = "Error in areaCode parameter";
			return procSIMRsp;
		}

		procSIMRsp = startSIMActivationExec(ICCID, IMSI_d,	IMSI_Roaming, MSISDN_d, 
				                                       countryCode, areaCode, activationType, origin);

		String prvSimResponse = new String("Provisioning SIM (ICCID=" + ICCID
				+ ") - Response: " + " resultCode=" + procSIMRsp.resultCode
				+ ", resultDescription=" + procSIMRsp.resultDescription
				+ ", sessionId=" + procSIMRsp.sessionId);

		LogHelper.webLog.debug(prvSimResponse);
		LogHelper.dbLogSdr(null, null, ICCID, "INFO",	"<- startSIMActivation (SINGLE) ", prvSimResponse);

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
	private provisioningSIMRsp startSIMActivationExec(String ICCID,
			String IMSI_d, String IMSI_Roaming, String MSISDN_d,
			String countryCode, String areaCode, String activationType, String Origin) 
	{
		provisioningSIMRsp procSIMRsp = new provisioningSIMRsp();

			
		LogHelper.webLog.debug("provisioningSIM: ParameterCheck Passed");

		SessionService sessionWS;
		try {
			sessionWS = SessionService.getInstance();
			// Chequea Banda horaria
//			if (!sessionWS.isNowInBand()) {
//				LogHelper.webLog.fatal("StartSession Operation not processed. Reason: Out of time band ");
//
//				procSIMRsp.resultCode = 10;
//				procSIMRsp.resultDescription = "Out of time band.";
//
//				return procSIMRsp;
//			}
			
		} catch (Exception e) {
			LogHelper.webLog.debug("StartSession: Could not instanciate SessionService");
			procSIMRsp.resultCode = 200;
			procSIMRsp.resultDescription = "Could not instanciate SessionService.";
			return procSIMRsp;
		}

		SMEFacade sme = SMEFacade.getInstance();
		if (sme == null) {
			LogHelper.webLog.debug("StartSession: Could not instanciate SME Module: ICCID:" + ICCID);
			procSIMRsp.resultCode = Constants.SM_NOT_FOUND;
			procSIMRsp.resultDescription = Constants.SM_NOT_FOUND_DESC;
			return procSIMRsp;
		}
		
		// Controlo existencia de suscriptor
        int subscriberCheck = 0;
        String subscriberCheckDes = "OK";

        Subscriber sb = ConnectionManager.getInstance().getSubscriber(ICCID, null, null, null, null);
        
        if (sb == null) subscriberCheck = 1;
        else if (sb.getBlockedStatus().equals("B")) { subscriberCheck = 2; subscriberCheckDes = "SIM Blocked"; }
        else if (sb.isExpired()) { subscriberCheck = 3; subscriberCheckDes = "SIM Expired"; }
        else if (!sb.isBatchProccesedOk() ) {subscriberCheck = 4; subscriberCheckDes = "Batch was not processed Ok"; }
        else if (sb.isBatchBlocked() ) {subscriberCheck = 5; subscriberCheckDes = "Batch is Blocked"; }

	    String stateMachineName = null;
	    String stateMachineVersion = null;
        
		if (subscriberCheck != 1) {
			// Buscamos una sesion para este subscriber
		    ManagementService managementWs = ManagementService.getInstance();
		    SessionStatus sessionStatus = managementWs.getSessionStatus(null, null, sb.getICCID());
		    ActivationEvent event = null;
		    EventTag eventTag = null;	    
		    
		    ServerConfiguration serverConf = ServerConfiguration.getInstance();
		    String initialEvent = serverConf.get("public.startSIMActivation.initialEvent."+activationType);
		    if (initialEvent == null)
		    	initialEvent = "SINGLE_START";
		    
			if (sessionStatus == null) {
				// No existe una sesion para este subscriber
				// Creamos una nueva
				// PDD 2018 STORE
				//initialEvent="STORE_START";
				LogHelper.webLog.debug("New session initial event: "+initialEvent);
				eventTag = EventTag.getEventTagFromOrigin(initialEvent);
				if (eventTag == null)
					eventTag = new EventTag(initialEvent, Event.Type.ACTIVATION.name(), Event.SubType.initialActivation.name());
				
			    stateMachineName = ServerConfiguration.getInstance().get("StoreStateMachineName");
			    stateMachineVersion = ServerConfiguration.getInstance().get("StoreStateMachineVersion");
			}
			else {
				LogHelper.webLog.debug("Session found: "+sessionStatus.sessionId);
				
				// Existe una sesion para este subscriber
				// Primero vemos si esta en memoria o en la base
				// Si esta en memoria mandamos un OperationResult
				if (sessionStatus.whereIs.equals("M")) {
					String resultData = IMSI_d + "|" + MSISDN_d + "|" + areaCode;
					SendNotificationRsp snr = sendNotificationExec(ICCID, null, null, "SINGLE_START", Origin, "OK", resultData);
					procSIMRsp.resultCode = Long.parseLong(snr.resultCode);
					procSIMRsp.resultDescription = snr.resultDescription;
					return procSIMRsp;
				}
				else {
					// Esta en la base, vemos si tenemos que resumir o no
					// Vemos como es esa sesion
					if (sessionStatus.finished.equals("T")) {
						StateTag state = sme.getStateTag(sessionStatus.sesStatus, sessionStatus.stateMachineName, sessionStatus.stateMachineVersion);
						if (state.isFinalError()) {
							if (sessionStatus.errorStatus != null && !sessionStatus.errorStatus.isEmpty()) {
		        				LogHelper.webLog.debug("startExec: Session ended with error found, errorState:" + sessionStatus.errorStatus);
		        				if (sme.getStateTag(sessionStatus.errorStatus, sessionStatus.stateMachineName, sessionStatus.stateMachineVersion).isProgramming() ||
		        					 sme.getStateTag(sessionStatus.errorStatus, sessionStatus.stateMachineName, sessionStatus.stateMachineVersion).isUserInfo()) 
		        				{
		        					// Hubo error en la programacion, enviamos un start
		        					LogHelper.webLog.debug("New session initial event: "+initialEvent);
		        					eventTag = EventTag.getEventTagFromOrigin(initialEvent);
		        					if (eventTag == null)
		        						eventTag = new EventTag(initialEvent, Event.Type.ACTIVATION.name(), Event.SubType.initialActivation.name());

		        				    stateMachineName = sessionStatus.stateMachineName;
		        				    stateMachineVersion = sessionStatus.stateMachineVersion;
		        					
		        				}
		        				else {
	            					// Si el error fue durante la activacion
	            					// Sesion finalizada con error activando SIM.  No hay mas chances.        
	            					LogHelper.webLog.debug("checking activating_sim error");
	            					
	            					procSIMRsp.resultCode = 808L;
	            					procSIMRsp.resultDescription = ServerConfiguration.getInstance().get("usrmsg_err_activating_sim");
	            					LogHelper.webLog.debug("Sending msisdn="+MSISDN_d+": usrmsg_err_activating_sim message");
	            					return procSIMRsp;
		        				}
							}
							else {
								// Puede ser una sesion cancelada sin error
								LogHelper.webLog.debug("Canceled session without error. New session initial event: "+initialEvent);
								eventTag = EventTag.getEventTagFromOrigin(initialEvent);
								if (eventTag == null)
									eventTag = new EventTag(initialEvent, Event.Type.ACTIVATION.name(), Event.SubType.initialActivation.name());
								
	        				    stateMachineName = sessionStatus.stateMachineName;
	        				    stateMachineVersion = sessionStatus.stateMachineVersion;
							}
						}
						else if (state.isPersist()) {
							// Tenemos una sesion persistida
							// Buscamos alguna configuracion sino usamos el default
							String resumeEvent = ConnectionManager.getInstance().getResumeEvent("SINGLE_START", state.getName());
							if (resumeEvent == null)
								eventTag = EventTag.getResumeSessionActivation();
							else
								eventTag = new EventTag(resumeEvent, Event.Type.ACTIVATION.name(), Event.SubType.resumeActivation.name());
						}
						else {
							LogHelper.webLog.debug("Session not persisted, checking activating_sim error");
        					
        					procSIMRsp.resultCode = 808L;
        					procSIMRsp.resultDescription = ServerConfiguration.getInstance().get("usrmsg_err_activating_sim");
        					LogHelper.webLog.debug("Sending msisdn="+MSISDN_d+": usrmsg_err_activating_sim message");
        					return procSIMRsp;
						}
    				}
					else {
						// No esta finalizada pero esta en la base...
						LogHelper.webLog.debug("Session in DB not ended but.... New session initial event: "+initialEvent);
						eventTag = EventTag.getEventTagFromOrigin(initialEvent);
						if (eventTag == null)
							eventTag = new EventTag(initialEvent, Event.Type.ACTIVATION.name(), Event.SubType.initialActivation.name());
						
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
				}
				else {
					// Es un mo start
	    			event.setICCID(ICCID);
	    			event.setMSISDN(MSISDN_d);
	    			event.setIMSI(IMSI_d);
	    			event.setMSISDN_FAKE(sb.getMSISDN_t());
	    			event.setIMSI_FAKE(sb.getIMSI_t());
	    			event.setIMSI_ROAMING(IMSI_Roaming);
	    			event.setAreaCode(areaCode);
	    			event.setActivationType(activationType);
	    			event.setOrigin(Origin);
	    			
	    			event.setVirtualNetwork(sb.getVirtualNetwork());
	 	            event.setApplication(sb.getSubApplication());
	 	            event.setOpc(sb.getOpc());
	 	            
	 	            event.setUserType(sb.getUserType());
	 	            event.setCardType(sb.getCardType());
	 	            event.setOpKey(sb.getOperatorKey());
	 	       	    
	 	            event.setAuxiliarData("CARDTYPE", String.valueOf(sb.getCardType()));
	 	            event.setAuxiliarData("VIRTUALNETWORK", String.valueOf(sb.getVirtualNetwork()));
	 	            event.setAuxiliarData("APPLICATION", String.valueOf(sb.getSubApplication()));
	 	            	      
	 	            if (initialEvent.equals("STORE_START") || initialEvent.equals("SINGLE_START"))  // pdd2019 agrega SINGLE_START
	 	            {
	 	            	if (activationType.equals("DIGITAL"))
	 	            		event.setAuxiliarData("HAPPY", "HPY");
	 	            	if (activationType.startsWith("PRE"))
	 	            		event.setAuxiliarData("PREPAGO", "PRE");
	 	            	if (activationType.startsWith("POS"))
	 	            		event.setAuxiliarData("POSPAGO", "POS");
	 	            	
	 	            }
				}
				
				// Add StateMachine Name and version
				event.addArg("StateMachineName", stateMachineName);
				event.addArg("StateMachineVersion",stateMachineVersion);
				
				event.setAuxiliarData("SUBSCHECKRES",String.valueOf(subscriberCheck));
				event.setAuxiliarData("SUBSCHECKDES",subscriberCheckDes);
				
			}
			catch(MalformedEventException mee) {
				procSIMRsp.resultCode = 1L;
				procSIMRsp.resultDescription = "Exception creating event "+eventTag.getName()+": " + mee;
				LogHelper.webLog.error("Exception creating event "+eventTag.getName()+": " , mee);
				return procSIMRsp;
			}
			
			Pair<Integer, String> resp = sessionWS.postStartEvent(sme, event);
			
			 if (resp == null) {
	                LogHelper.webLog.error("startExec: postStartEvent returned null.");
	                procSIMRsp.resultCode = Constants.POST_EVENT_ERROR;
	                procSIMRsp.resultDescription = Constants.POST_EVENT_ERROR_DESC + ". Return null";
	                return procSIMRsp;
	         }
	         else{
	                LogHelper.webLog.debug("startExec: postStartEvent returned (" + resp.getLeft() + "," + resp.getRight() + ")");
	         }

            // Reviso el Pair, si es cero, esta bien
            if (resp.getLeft().equals(new Integer(0))) {
                // Persist Session
                WsAckRsp mngResp = managementWs.persistSessionData(resp.getRight().toString());
                
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
			LogHelper.webLog.error("StartSIMActivation - Subscriber not found or blocked: "+ICCID);
			procSIMRsp.resultCode = Constants.SUBSCRIBER_NOT_FOUND;
			procSIMRsp.resultDescription = Constants.SUBSCRIBER_NOT_FOUND_DESC + " or " + Constants.SUBSCRIBER_BLOCKED_DESC;
			return procSIMRsp;
		}

		return procSIMRsp;
	}

	/**
	 * 
	 * @param ICCID
	 * @param imsiFake
	 * @param msisdnFake
	 * @param notification
	 * @param externalError
	 * @param notificationData
	 * @param origin
	 * @return
	 * @throws SOAPException
	 */
	@WebMethod(operationName = "sendNotification")
	public SendNotificationRsp sendNotification(
			@WebParam(name = "ICCID") String ICCID,
			@WebParam(name = "imsiFake") String imsiFake,
			@WebParam(name = "msisdnFake") String msisdnFake,
			@WebParam(name = "notification") String notification,
			@WebParam(name = "externalError") String externalError,
			@WebParam(name = "notificationData") String notificationData,
			@WebParam(name = "origin") String origin) 
					throws SOAPException 
	{

		LogHelper.webLog.debug("sendNotification (ICCID=" + ICCID
				+ " imsiFake=" + imsiFake + " msisdnFake=" + msisdnFake
				+ " notification=" + notification + " externalError="
				+ externalError + " notificationData=" + notificationData
				+ " origin=" + origin + ")");

		if (!ServerConfiguration.getInstance().serverRunning()) {
			LogHelper.webLog
			.fatal(" Operation not processed.  Reason:Server stopped ");
			// return -99;
			throw new javax.xml.soap.SOAPException(
					"ASA-Orchestrator server paused");
		}

		SendNotificationRsp rsp = sendNotificationExec(ICCID, imsiFake, msisdnFake, notification,
				origin, externalError, notificationData);

		// Log Response
		String ApiCallbackResponse = new String("sendNotification (ICCID=" + ICCID
				+ ") - Response: " + " resultCode=" + rsp.resultCode
				+ ", resultDescription=" + rsp.resultDescription);

		LogHelper.webLog.debug(ApiCallbackResponse);

		LogHelper.dbLogSdr(null, null, ICCID, "INFO", 	"<- sendNotification Response", ApiCallbackResponse);

		return rsp;
	}

	/**
	 * pdd2018
	 * Es llamado desde el operador para obtener los datos del SIM
	 * 
	 * @param ICCID
	 * @param imsi
	 * @param msisdn
	 * @param origin
	 * @return
	 * @throws SOAPException
	 */
	@WebMethod(operationName = "getSIMInfo")
	public GetSIMInfoRsp getSIMInfo(
			@WebParam(name = "ICCID") String ICCID,
			@WebParam(name = "imsi") String imsi,
			@WebParam(name = "msisdn") String msisdn,
			@WebParam(name = "origin") String origin) 
					throws SOAPException 
	{

		LogHelper.webLog.debug("getSIMInfo (ICCID=" + ICCID
				+ " imsi=" + imsi + " msisdn=" + msisdn
				+ " origin=" + origin + ")");

		if (!ServerConfiguration.getInstance().serverRunning()) {
			LogHelper.webLog
			.fatal(" Operation not processed.  Reason:Server stopped ");
			// return -99;
			throw new javax.xml.soap.SOAPException(
					"ASA-Orchestrator server paused");
		}

		GetSIMInfoRsp rsp = new GetSIMInfoRsp();
		//SendNotificationRsp rsp = sendNotificationExec(ICCID, imsiFake, msisdnFake, notification,
		//		origin, externalError, notificationData);
		
		if (ICCID!=null)
			if (ICCID.isEmpty()) ICCID=null;
		if (msisdn!=null)
			if (msisdn.isEmpty()) msisdn=null;
		if (imsi!=null)
			if (imsi.isEmpty()) imsi=null;
		
		try {
			Subscriber sb = ConnectionManager.getInstance().getSubscriber (ICCID, msisdn, imsi, null, null);
        
			int subscriberCheck =0;
	        if (sb == null) subscriberCheck = 1;
	        else if (sb.getBlockedStatus().equals("B")) subscriberCheck = 2;
	        else if (sb.getBlockedStatus().equals("B")) subscriberCheck = 2;
	        else if (sb.isExpired()) subscriberCheck = 3;
	        else if (!sb.isBatchProccesedOk()) subscriberCheck = 4;
	        else if (sb.isBatchBlocked()) subscriberCheck = 5;
			
	        if (subscriberCheck==0)
	        {
			rsp.resultCode = 0; //a
			rsp.resultDescription = "OK";
			rsp.ICCID= sb.getICCID();
			rsp.IMSI_Real= sb.getIMSI_d();
			rsp.MSISDN_Real=sb.getMSISDN_d();
			rsp.IMSI_Temp=sb.getIMSI_t();
			rsp.MSISDN_Temp=sb.getMSISDN_t();
	        }
	        else
	        {
	    		rsp.resultCode = Constants.SUBSCRIBER_NOT_FOUND;
				rsp.resultDescription = Constants.SUBSCRIBER_NOT_FOUND_DESC + " or " + Constants.SUBSCRIBER_BLOCKED_DESC;
	    		rsp.ICCID="" ;
	    		rsp.IMSI_Real="" ;
	    		rsp.MSISDN_Real="" ;
	    		rsp.IMSI_Temp="" ;
	    		rsp.MSISDN_Temp="" ;
	        }
		} catch (Exception e) {
			rsp.resultCode = 1; 
			rsp.resultDescription = "ERROR executing operation";
    		rsp.ICCID="" ;
    		rsp.IMSI_Real="" ;
    		rsp.MSISDN_Real="" ;
    		rsp.IMSI_Temp="" ;
    		rsp.MSISDN_Temp="" ;			
    		
    		LogHelper.webLog.error("getSIMInfo - ERROR executing operation. "+ e.getMessage());

    		
		}
       

		return rsp;
	}
	
	
	/**
	 * 
	 * @param ICCID
	 * @param imsi
	 * @param msisdn
	 * @param notification
	 * @param origin
	 * @param externalError
	 * @param notificationData
	 * @return
	 */
	private SendNotificationRsp sendNotificationExec(String ICCID,
			String imsi, String msisdn,
			String notification, String origin, String externalError,
			String notificationData) 
	{
		SendNotificationRsp rsp = new SendNotificationRsp();
		
		LogHelper.dbLogSdr(null, null, ICCID, "INFO", "-> Send Notification",
				"Send Notification - Begin: ICCID=" + ICCID + ", IMSI=" + imsi + ", MSISDN=" + msisdn + 
				", notification=" + notification + ", origin=" + origin +
				", externalError=" + externalError + ", notificationData=" + notificationData);
		
		// Obtengo la sesion
		SessionData sd = null;
		
		if (!isValidRequest (ICCID, imsi, msisdn, notification)) {
			rsp.resultCode = "400";
			rsp.resultDescription = "Bad parameters request for operation sendNotification";
			return rsp;
		}
		
		if (notification.toUpperCase().equals("RESUME_SESSION")) {
			provisioningSIMRsp procSIMRsp = startSessionExec(imsi,msisdn,notification,origin);
			rsp.resultCode = Long.toString(procSIMRsp.resultCode);
			rsp.resultDescription = procSIMRsp.resultDescription;
			return rsp;
		} else {
			try {
				
				sd = SessionService.getInstance().searchSessionByIccidImsiMsisdn(ICCID, imsi, msisdn);
			} catch (Exception e) {
				LogHelper.webLog.error("Send Notification - No instance found by ICCID="+ ICCID);
				LogHelper.webLog.error("Send Notification - "+e.getMessage());
			}
	
			if (sd == null) { //VALIDACION DE MEMORIA EN DISCO
				// No se encontro sesion con el ICCID recibido
				rsp.resultCode = "10002";
				rsp.resultDescription = "Session not found with ICCID:" + ICCID;
				LogHelper.webLog.error("Send Notification - No session found. ICCID="+ ICCID);
				return rsp;
			}
			
			if (notification.toUpperCase().equals("SEND_SMS"))
			{
				// Solicitud externa de envio de un SMS para una sesion activa.
				try {
					//processManualWSEvent(String methodName, String type, String smsMessage, String sesId, String sesMsisdn, String origin) {
					SessionEventExecutor stme = new SessionEventExecutor();
					//sd.getID()
					//sd.getMSISDN()
	
					String partes[] = notificationData.split("\\|");
	
					if (partes.length > 0) {
						String method="sendNotification1";
						if (partes[0].trim().equals("0"))
							method="sendNotification0";
						
						//ENVIA SMS
						String rspWS = stme.processManualWSEvent(method,null,partes[1], sd.getID(), sd.getMSISDN_FAKE(),origin);
						
							if (rspWS=="0")
							{
								rsp.resultCode = "0";
								rsp.resultDescription = "OK";
							}
							else
							{
								rsp.resultCode = rspWS;
								rsp.resultDescription = "Error";
							}
					}
					else
					{
						rsp.resultCode = "197";
						rsp.resultDescription = "ERROR wrong parameters (notificationData)";
					}
				}
				catch(Exception e)
				{
					rsp.resultCode = "1";
					rsp.resultDescription = "ERROR executing operation";
				}
				
			}
			else
			{
				LogHelper.webLog.debug("sendNotification - operationResultCall ("
						+ sd.getID() + "," + sd.getTransactionID() + "," + notification
						+ "," + externalError + ",null," + notificationData + ")");
				try {
					WsAckRsp opRrsp = SessionService.getInstance().operationResult(
							sd.getID(), sd.getTransactionID(), notification,
							externalError, null, notificationData);
		
					rsp.resultCode = opRrsp.resultCode.toString();
					rsp.resultDescription = opRrsp.resultDescription;
		
				} catch (Exception e) {
					rsp.resultCode = "1";
					rsp.resultDescription = "ERROR executing operation";
				}
			}
		}
			
		return rsp;
}


	/**
	 * Lista de estados simplificada
	 */
	public static enum ReadableStatus {
		CANCELLED, ERROR, ACTIVATED, ACTIVATING, STARTING;

	}
	
	/**
	 * Punto de entrada para la pre-activacion de una SIM 
	 * 
	 * @param ICCID_fake
	 * @param IMSI
	 * @param MSISDN
	 * @param areaCode
	 * @param origin
	 * @return Devuelve un 
	 *     resultCode=0 cuando el proceso de preactivacion pudo ser iniciado 
	 *     resultCode=1 cuando no se pudo avanzar con el proceso de preactivacion 
	 *     resultCode=10000 Cuando no se puede crear la sesion por un problema interno 
	 *     resultCode=10001 Cuando existen problemas con el IMSI
	 * 
	 * @throws java.lang.InterruptedException
	 * @throws com.ats_connection.asa.orchestrator.sme.exception.
	 *             MalformedEventException
	 */
	@WebMethod(operationName = "startSIMPreActivation")
	public provisioningSIMRsp startSIMPreActivation(
			@WebParam(name = "ICCID_fake") String ICCID_fake,
			@WebParam(name = "IMSI") String IMSI_real,
			@WebParam(name = "MSISDN") String MSISDN_real,
			@WebParam(name = "areaCode") String areaCode,
			@WebParam(name = "origin") String origin)
					throws InterruptedException, MalformedEventException, SOAPException 
	{
		LogHelper.webLog.debug("startSIMPreActivation: Begin: ICCID_fake=" + ICCID_fake	+ " IMSI=" + IMSI_real + " MSISDN=" + MSISDN_real);

		if (!ServerConfiguration.getInstance().serverRunning()) {
			LogHelper.webLog.fatal(" Operation not processed.  Reason:Server stopped ");
			throw new javax.xml.soap.SOAPException("ASA-Orchestrator server paused");
		}

		String prvSimRequest = "PreActivationSIM Request(" + "ICCID: " + ICCID_fake
				+ ", IMSI: " + IMSI_real + ", MSISDN: " + MSISDN_real 
				+ ", areaCode: " + areaCode + ", origin: " + origin + ")";

		LogHelper.webLog.debug(prvSimRequest);

		provisioningSIMRsp procSIMRsp = new provisioningSIMRsp();
		
		// Control de parametros
		// Alguno de ellos debe tener contenido...
		if ((ICCID_fake == null || ICCID_fake.length() == 0) 
			&& (MSISDN_real == null || MSISDN_real.length() == 0) 
			&& (IMSI_real == null || IMSI_real.length() == 0))
		{
			procSIMRsp.resultCode = 1;
			procSIMRsp.resultDescription = "Error: ICCID_fake, IMSI and MSISDN are empty";
			return procSIMRsp;
		}
		
		if (ICCID_fake == null || ICCID_fake.length() == 0 || ICCID_fake.length() > 100)
		{
			procSIMRsp.resultCode = 1;
			procSIMRsp.resultDescription = "Error in ICCID_fake parameter";
			return procSIMRsp;
		}
		
		if (IMSI_real == null || IMSI_real.length() == 0 || IMSI_real.length() > 32) {
			procSIMRsp.resultCode = 1;
			procSIMRsp.resultDescription = "Error in IMSI parameter";
			return procSIMRsp;
		}
		
		if (MSISDN_real == null || MSISDN_real.length() == 0	
			|| MSISDN_real.length() > 15 || !(MSISDN_real.matches("((-|\\+)?[0-9]+(\\.[0-9]+)?)+"))) 
		{
			procSIMRsp.resultCode = 1;
			procSIMRsp.resultDescription = "Error in MSISDN parameter";
			return procSIMRsp;
		}
		
		if (origin == null || origin.length() == 0 || origin.length() > 32)
		{
			procSIMRsp.resultCode = 1;
			procSIMRsp.resultDescription = "Error in origin parameter";
			return procSIMRsp;
		}
		
		if (areaCode == null || areaCode.length() == 0 || areaCode.length() > 5)
		{
			procSIMRsp.resultCode = 1;
			procSIMRsp.resultDescription = "Error in areaCode parameter";
			return procSIMRsp;
		}

		procSIMRsp = startSIMPreActivationExec(ICCID_fake, IMSI_real,	MSISDN_real, areaCode, origin);

		String prvSimResponse = new String("PreActivationSIM (" + "ICCID: " + ICCID_fake
				+ ", IMSI: " + IMSI_real + ", MSISDN: " + MSISDN_real 
				+ ", areaCode: " + areaCode + ", origin: " + origin + ") - Response: " 
				+ " resultCode=" + procSIMRsp.resultCode
				+ ", resultDescription=" + procSIMRsp.resultDescription
				+ ", sessionId=" + procSIMRsp.sessionId);
				
		LogHelper.webLog.debug(prvSimResponse);
		
		// TODO modificar log de ASA Logger para poder actualziar ICCD_Fake, IMSI real y MSISDN real
		LogHelper.dbLogSdr(null, null, ICCID_fake, "INFO",	"<- startSIMPreActivation ", prvSimResponse);

		return procSIMRsp;
	}
	
	/**
	 * @param ICCID_fake
	 * @param IMSI_real
	 * @param MSISDN_real
	 * @param areaCode
	 * @param Origin
	 * @return
	 */
	private provisioningSIMRsp startSIMPreActivationExec(String ICCID_fake,
			String IMSI_real, String MSISDN_real, String areaCode, String Origin) 
	{
		provisioningSIMRsp procSIMRsp = new provisioningSIMRsp();

		LogHelper.webLog.debug("startSIMPreActivation: ParameterCheck Passed");

		SessionService sessionWS;
		try {
			sessionWS = SessionService.getInstance();
		} catch (Exception e) {
			LogHelper.webLog.debug("StartSIMPreActivation Session: Could not instanciate SessionService");
			procSIMRsp.resultCode = 200;
			procSIMRsp.resultDescription = "Could not instanciate SessionService.";
			return procSIMRsp;
		}

		SMEFacade sme = SMEFacade.getInstance();
		if (sme == null) {
			LogHelper.webLog.debug("StartSIMPreActivation Session: Could not instanciate SME Module: ICCID:" + ICCID_fake);
			procSIMRsp.resultCode = Constants.SM_NOT_FOUND;
			procSIMRsp.resultDescription = Constants.SM_NOT_FOUND_DESC;
			return procSIMRsp;
		}

		// TODO - Verificar que el estado de preactivacion de IMSI_real, MSISD_real esta pendiente.
		// Controlo existencia de suscriptor
        int preactivationPending = 0;
        
	    String stateMachineName = null;
	    String stateMachineVersion = null;
        
		if (preactivationPending == 0) {
			// Buscamos una sesion para este subscriber
		    ManagementService managementWs = ManagementService.getInstance();
		    
		    SessionStatus sessionStatus = managementWs.getSessionStatus(null, null, null, ICCID_fake, IMSI_real, MSISDN_real);
		    ActivationEvent event = null;
		    EventTag eventTag = null;
		    
		    // ServerConfiguration serverConf = ServerConfiguration.getInstance();
		    String initialEvent = "PREACTIVATION_START";
		    
			if (sessionStatus == null) {
				// No existe una sesion para los valores pasados como parametros
				// Creamos una nueva
				LogHelper.webLog.debug("New session initial event: "+initialEvent);
				eventTag = EventTag.getEventTagFromOrigin(initialEvent);
				if (eventTag == null)
					eventTag = new EventTag(initialEvent, Event.Type.ACTIVATION.name(), Event.SubType.initialActivation.name());
				
			    stateMachineName = ServerConfiguration.getInstance().get("PreActivationStateMachineName");
			    stateMachineVersion = ServerConfiguration.getInstance().get("PreActivationStateMachineVersion");
			}
			else {
				// Existe una sesion para los valores pasados como parametros
				// Primero vemos si esta en memoria o en la base
				// Si esta en memoria mandamos un OperationResult
				if (sessionStatus.whereIs.equals("M")) {
					procSIMRsp.resultCode = 808L;
					procSIMRsp.resultDescription = "There is an Active Session in memory still running";
					return procSIMRsp;
				}
				else {
					// Esta en la base, vemos si tenemos que resumir o no
					// Vemos como es esa sesion
					if (sessionStatus.finished.equals("T")) {
						StateTag state = sme.getStateTag(sessionStatus.sesStatus, sessionStatus.stateMachineName, sessionStatus.stateMachineVersion);
//						if (state.isFinalError()) {
//        					LogHelper.webLog.debug("Preactivation SIM Error");            					
//        					procSIMRsp.resultCode = Long.parseLong(sessionStatus.auxiliarData.get("ERRORCODE"));
//        					procSIMRsp.resultDescription = sessionStatus.auxiliarData.get("ERRORDESCRIPTION");
//        					return procSIMRsp;								
//						}
						if (state.isPersist()) {
							// Tenemos una sesion persistida
							// Buscamos alguna configuracion sino usamos el default
							String resumeEvent = ConnectionManager.getInstance().getResumeEvent(sessionStatus.originActivation, state.getName());
							if (resumeEvent != null) {
								eventTag = new EventTag(resumeEvent, Event.Type.ACTIVATION.name(), Event.SubType.resumeActivation.name());
						
								stateMachineName = sessionStatus.stateMachineName;
								stateMachineVersion = sessionStatus.stateMachineVersion;
								
								LogHelper.webLog.debug("Resume event: "+resumeEvent);
							}
						}
						else {
							// Creamos una nueva
							LogHelper.webLog.debug("New session initial event: "+initialEvent);
							eventTag = EventTag.getEventTagFromOrigin(initialEvent);
							if (eventTag == null)
								eventTag = new EventTag(initialEvent, Event.Type.ACTIVATION.name(), Event.SubType.initialActivation.name());
							
						    stateMachineName = ServerConfiguration.getInstance().get("PreActivationStateMachineName");
						    stateMachineVersion = ServerConfiguration.getInstance().get("PreActivationStateMachineVersion");
						}
    				}
					else {
						// No esta finalizada pero esta en la base...
						LogHelper.webLog.debug("Session in DB not ended but.... New session initial event: "+initialEvent);
						eventTag = EventTag.getEventTagFromOrigin(initialEvent);
						if (eventTag == null)
							eventTag = new EventTag(initialEvent, Event.Type.ACTIVATION.name(), Event.SubType.initialActivation.name());
						
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
				}
				else {
					// Es un new start
	    			event.setICCID(ICCID_fake);
	    			event.setMSISDN(MSISDN_real);
	    			event.setIMSI(IMSI_real);
	    			event.setMSISDN_FAKE(null);
	    			event.setIMSI_FAKE(null);
	    			event.setIMSI_ROAMING(null);
	    			event.setAreaCode(areaCode);
	    			event.setActivationType("PREACTIVATION");
	    			event.setOrigin(Origin);
	    			
	    			event.setVirtualNetwork(null);
	 	            event.setApplication(null);
	 	            event.setOpc(null); 	        
	 	            event.setUserType(null);
	 	            event.setCardType(null);
	 	            event.setOpKey(null);
	 	       	    
//	 	            event.setAuxiliarData("CARDTYPE", String.valueOf(sb.getCardType()));
//	 	            event.setAuxiliarData("VIRTUALNETWORK", String.valueOf(sb.getVirtualNetwork()));
//	 	            event.setAuxiliarData("APPLICATION", String.valueOf(sb.getSubApplication()));
	 	            	      
//	 	            if (initialEvent.equals("STORE_START") || initialEvent.equals("SINGLE_START"))  // pdd2019 agrega SINGLE_START
//	 	            {
//	 	            	if (activationType.equals("DIGITAL"))
//	 	            		event.setAuxiliarData("HAPPY", "HPY");
//	 	            	if (activationType.startsWith("PRE"))
//	 	            		event.setAuxiliarData("PREPAGO", "PRE");
//	 	            	if (activationType.startsWith("POS"))
//	 	            		event.setAuxiliarData("POSPAGO", "POS");
//	 	            	
//	 	            }
				}
				
				// Add StateMachine Name and version
				event.addArg("StateMachineName", stateMachineName);
				event.addArg("StateMachineVersion",stateMachineVersion);
			}
			catch(MalformedEventException mee) {
				procSIMRsp.resultCode = 1L;
				procSIMRsp.resultDescription = "Exception creating event "+eventTag.getName()+": " + mee;
				LogHelper.webLog.error("Exception creating event "+eventTag.getName()+": " , mee);
				return procSIMRsp;
			}
			
			Pair<Integer, String> resp = sessionWS.postStartEvent(sme, event);
			if (resp == null) {
				LogHelper.webLog.error("postStartExec: postStartEvent returned null.");
	            procSIMRsp.resultCode = Constants.POST_EVENT_ERROR;
	            procSIMRsp.resultDescription = Constants.POST_EVENT_ERROR_DESC + ". Return null";
	            return procSIMRsp;
	        }
	        else{
	        	LogHelper.webLog.debug("startExec: postStartEvent returned (" + resp.getLeft() + "," + resp.getRight() + ")");
	        }

            // Reviso el Pair, si es cero, esta bien
            if (resp.getLeft().equals(new Integer(0))) {
                // Persist Session
                WsAckRsp mngResp = managementWs.persistSessionData(resp.getRight().toString());                
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
			// TODO
		}
		return procSIMRsp;
	}	
	
	private provisioningSIMRsp startSessionExec(String imsiFake, String msisdnFake,
			 String activationType, String Origin) 
		{
			provisioningSIMRsp procSIMRsp = new provisioningSIMRsp();
				
			SessionService sessionWS;
			try {
				sessionWS = SessionService.getInstance();			
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
	        
	        if (sb == null) subscriberCheck = 1;
	        else if (sb.getBlockedStatus().equals("B")) { subscriberCheck = 2; subscriberCheckDes = "SIM Blocked"; }
	        else if (sb.isExpired()) { subscriberCheck = 3; subscriberCheckDes = "SIM Expired"; }
	        else if (!sb.isBatchProccesedOk()) {subscriberCheck = 4; subscriberCheckDes = "Batch was not processed Ok"; }
	        else if (sb.isBatchBlocked() ) {subscriberCheck = 5; subscriberCheckDes = "Batch is Blocked"; }
	        
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
					
					procSIMRsp.resultCode = 808L;
					procSIMRsp.resultDescription = "No session found for imsi= "+imsiFake+" and msisdn= "+msisdnFake;
					LogHelper.webLog.debug("Sending msisdn="+msisdnFake+": usrmsg_err_activating_sim message");
					return procSIMRsp;
				}
				if (sessionStatus.finished.equals("T")) {
					StateTag state = sme.getStateTag(sessionStatus.sesStatus, sessionStatus.stateMachineName, sessionStatus.stateMachineVersion);
							if (state.isPersist()) {
								// Tenemos una sesion persistida
								// Buscamos alguna configuracion sino usamos el default
								// original -- String resumeEvent = ConnectionManager.getInstance().getResumeEvent("HLR_START", state.getName());
								//pdd 2018 para soportar store start
								String resumeEvent = "";
								resumeEvent = ConnectionManager.getInstance().getResumeEvent(activationType, state.getName());
									
								if (resumeEvent == null)
									eventTag = EventTag.getResumeSessionActivation();
								else eventTag = new EventTag(resumeEvent, Event.Type.ACTIVATION.name(), Event.SubType.resumeActivation.name());
								
								stateMachineName = sessionStatus.stateMachineName;
								stateMachineVersion = sessionStatus.stateMachineVersion;
							}
							else {
	        					procSIMRsp.resultCode = 808L;
	        					procSIMRsp.resultDescription = "Session for imsi= "+imsiFake+" and msisdn= "+msisdnFake+" is not persist";
	        					LogHelper.webLog.debug("Sending msisdn="+msisdnFake+": usrmsg_err_activating_sim message");
	        					return procSIMRsp;
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
		
		private boolean isValidRequest (String ICCID, String imsi, String msisdn, String notification) {
			
			return ( ICCID!=null && ICCID!="" && imsi!=null && imsi!="" && msisdn!=null && msisdn!="" && notification!=null && notification!="");
			
		}

	
}