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
import com.ats_connection.asa.orchestrator.response.*;
// Imports de StateMachine
import com.ats_connection.asa.orchestrator.sme.Session.SessionData;
import com.ats_connection.asa.orchestrator.sme.api.SMEFacade;
import com.ats_connection.asa.orchestrator.sme.event.ActivationEvent;
import com.ats_connection.asa.orchestrator.sme.event.Event;
import com.ats_connection.asa.orchestrator.sme.event.EventTag;
import com.ats_connection.asa.orchestrator.sme.exception.MalformedEventException;
import com.ats_connection.asa.orchestrator.sme.helper.Pair;
import com.ats_connection.asa.orchestrator.sme.helper.SearchCriteria;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.soap.SOAPException;

/**
 *
 * @author pdiaz
 */
@WebService()
public class Ivr {

    public Ivr() {
    	LogHelper.webLog.info("ASA - IVR API - Initializing");
    }

    /**
     * Web service operation
     */
    @WebMethod(operationName = "startSession")
    public StartSessionRsp startSession(
    		@WebParam(name = "ivrId") String ivrId, 
    		@WebParam(name = "msisdn") String msisdn, 
    		@WebParam(name = "appNameVersion") String appNameVersion, 
    		@WebParam(name = "dialogId") String dialogId) 
    				throws SOAPException 
    {
        // Inicia una sesion del tipo IVR Y devuelve el estado y la lista de DDDs

        if (!ServerConfiguration.getInstance().serverRunning()){
            LogHelper.webLog.fatal(" Operation not processed.  Reason:Server stopped ");
            //return -99;
            throw new javax.xml.soap.SOAPException("ASA-Orchestrator server paused");
        }
        
        StartSessionRsp retVal = new StartSessionRsp();
        SMEFacade sme = SMEFacade.getInstance();
        if (sme == null)  // Imposible instanciar el ejecutor de máquinas de estados
        {
        	LogHelper.webLog.debug("IvrStartSession: Could not instanciate SME Module: MSISDN:" + msisdn);
        	retVal.resultCode=200;
        	retVal.resultDescription="Could not instanciate SME Module.";
        	return retVal;
        }

        SessionService sessionWS;
		try {
			sessionWS = SessionService.getInstance();
			// Chequea Banda horaria
			if (!sessionWS.isNowInBand()){
				LogHelper.webLog.fatal(" Ivr StartSession Operation not processed.  Reason:Out of time band ");
				
				retVal.status=5;
				retVal.resultCode=0;
				retVal.errorCode=1;
				retVal.resultDescription="Out of time band.";
				
				return retVal;
			}
		} catch (Exception e) {
        	LogHelper.webLog.debug("IvrStartSession: Could not instanciate SessionService");
        	retVal.resultCode=200;
        	retVal.resultDescription="Could not instanciate SessionService.";
        	return retVal;
		}

        Subscriber subscriber;
        subscriber = ConnectionManager.getInstance().getSubscriber(null, msisdn, null, null, null);
        if (subscriber == null) {
        	// No existe un suscriptor disponible
            LogHelper.webLog.debug("IvrStartSession: MSISDN not found: MSISDN:" + msisdn);
            retVal.resultCode=0;
            retVal.status=5;
            retVal.errorCode=10;
            retVal.resultDescription="MSISDN not found.";
            return retVal;
        }

        //chequeo el contador de intentos
        Integer retries, dtmfMin, dtmfMax, cfgretries;
        retries = new Integer(ServerConfiguration.getInstance().get("max_subscribers_retries"));
        if (retries != null && subscriber.getRetryCounter() != null && retries.compareTo(subscriber.getRetryCounter()) <= 0 ){ 
        	// supero la cant max
            retVal.resultCode=0;
            retVal.status=5;
            retVal.errorCode=3;
            retVal.resultDescription="retries exceeded";
            LogHelper.webLog.debug("IvrStartSession: retries exceeded");
            return retVal;
        }

        ManagementService managementWS =ManagementService.getInstance();
        SessionData sessionData;
        SearchCriteria search;
        StartActivationRsp sar = null;

	    String stateMachineName = null;
	    String stateMachineVersion = null;
        
        search = new SearchCriteria(null,null, msisdn, null);
        sessionData = sme.getSessionBySearchCriteria(search);
        if (sessionData != null) {
        	
            stateMachineName = sessionData.getStateMachineName();
            stateMachineVersion = sessionData.getStateMachineVersion();
        	
            sar = checkSessionStatus(ivrId, appNameVersion, dialogId, sessionData.getID());
            if (sar.resultCode != 99) {
                retVal.resultCode = sar.resultCode;
                retVal.resultDescription = sar.resultDescription;
                retVal.errorCode = sar.errorCode;
                retVal.status = sar.status;
                retVal.transferNumber = sar.transferNumber;
                retVal.sessionId=sessionData.getID();                
            }
        }
        else { 
        	// Controlar si el MSISDN_Fake está en estado error de SIM.  En ese caso debe informar el error y salir
           // Se agrega para solucionar issue #303 de la planilla.
            
            LogHelper.webLog.debug("startSession (IVR): checking session in DB ( msisdnFake=" + msisdn + ") " );
             
            SessionStatus sessionStatus = managementWS.getSessionStatus(msisdn, null, null);
            if (sessionStatus != null) {
            	
                stateMachineName = sessionStatus.stateMachineName;
                stateMachineVersion = sessionStatus.stateMachineVersion;

                LogHelper.webLog.debug("startSession (IVR): Session obtained. status:" + sessionStatus.sesStatus + ", finished:"+ sessionStatus.finished + ", errorStatus:" + sessionStatus.errorStatus);
                if (sessionStatus.errorStatus != null && sme.getStateTag(sessionStatus.errorStatus, sessionStatus.stateMachineName, sessionStatus.stateMachineVersion).isActivation() )
                {
                    retVal.resultCode = 0;
                    retVal.resultDescription = "Previous Activation Error: ACTIVATING_SIM";
                    retVal.errorCode = 11;
                    retVal.status = 5;
                    
                    return retVal;
                }

                if (sessionStatus.finished != null && sessionStatus.finished.equals("T") &&
                		sme.getStateTag(sessionStatus.sesStatus, sessionStatus.stateMachineName, sessionStatus.stateMachineVersion).isPersist())
                {
                    // Sesion persistida
                    //Salir con error
                    LogHelper.webLog.debug("provisioningSIM: Session persisted found after ApiCallback, id: " + sessionStatus.sessionId + "state:" + sessionStatus.sesStatus);
                    retVal.resultCode=0;
                    retVal.resultDescription="OK";
                    retVal.transferNumber="";
                    retVal.status=5; //
                    retVal.errorCode=14;
                    // Pide al usuario que reinicie el telefono.
                    retVal.sessionId=sessionStatus.sessionId;
                    return retVal;
                }                    
            }
        }
        
        if((sessionData == null) ||((sar!=null) && (sar.resultCode==99)) ) // rc=99 (IVR_SERVICE_RUNING o WAITING_FOR_USER_INPUT)
        {
            try {
            	EventTag eventTag = new EventTag(EventTag.IVR_START, Event.Type.ACTIVATION.name());
            	
			    stateMachineName = ServerConfiguration.getInstance().get("IVRStateMachineName");
			    stateMachineVersion = ServerConfiguration.getInstance().get("IVRStateMachineVersion");
            	
                ActivationEvent evt = new ActivationEvent(eventTag, 0);
                
                evt.setMSISDN_FAKE(msisdn);
                evt.setIMSI_FAKE(subscriber.getIMSI_t());
                evt.setICCID(subscriber.getICCID());
                evt.setAuxiliarData("Category", subscriber.getCategory().toString());
                
                LogHelper.webLog.info("PAC --> " + "ACA");
                //PAC -  LTE
                evt.setVirtualNetwork(subscriber.getVirtualNetwork());
                evt.setApplication(subscriber.getSubApplication());
                evt.setOpc(subscriber.getOpc());
                evt.setIMSI_ROAMING(subscriber.getImsiRoaming());
                evt.setOrigin(subscriber.getOrigin());
                evt.setUserType(subscriber.getUserType());
                evt.setCardType(subscriber.getCardType());
                evt.setOpKey(subscriber.getOperatorKey());
                evt.setAuxiliarData("CARDTYPE",  String.valueOf(subscriber.getCardType()));
                evt.setAuxiliarData("VIRTUALNETWORK",  String.valueOf(subscriber.getVirtualNetwork()));
                evt.setAuxiliarData("APPLICATION",  String.valueOf(subscriber.getSubApplication()));
              
                int qtty;
                String[] ddd = new String[5];
                String loci = new String();
                qtty = sessionWS.getDDD(subscriber.getIMSI_t(), ddd, 5, msisdn, loci);

                if (loci != null)
                    evt.setLOCI(loci);
                
                Pair<Integer,String> resp = sessionWS.postStartEvent(sme, evt);

                if (resp == null) {
                    //LogHelper.webLog.debug("IvrStartSession: No session found for MSISDN:" + msisdn+ " Errocode:" + (Integer)resp.getLeft());
                    LogHelper.webLog.debug("IvrStartSession: No session found for MSISDN:" + msisdn+ " PostStartEvent returned null");
                    retVal.resultCode=200;
                    retVal.resultDescription="Could not create a session";
                }
                else{
                    //poner todo en OK y buscar

                    LogHelper.dbLog ( null, msisdn, "HIGH", "INFO","-> IVR Start Session - Session created","IvrStartSession(ivrId="+ivrId+", msisdn="+msisdn+", appNameVersion="+ appNameVersion+", dialogId="+dialogId+")") ; 
                    
                    retVal.resultCode=0;
                    retVal.sessionId=resp.getRight().toString();

                    managementWS.persistSessionData(resp.getRight().toString());
                    
                    if(qtty < 0 ){ //no hay areas
                        retVal.resultCode=0;
                        retVal.status=5;
                        retVal.errorCode=10;
                        retVal.resultDescription="Some problems occurred getting areas from the database.";
                    }else{
                        
                        //leo la configuración
                        dtmfMin=new Integer(ServerConfiguration.getInstance().get("ddd_dtmf_min"));
                        dtmfMax=new Integer(ServerConfiguration.getInstance().get("ddd_dtmf_max"));
                        cfgretries = new Integer(ServerConfiguration.getInstance().get("max_retries_accepted"));

                        //cargo los valores
                        retVal.status=2;
                        retVal.dddQuantity=qtty;
                        retVal.transferNumber="";
                        retVal.max_retries=cfgretries;
                        retVal.ddd_dtmf_min= (dtmfMin==null)? 2 : dtmfMin;
                        retVal.ddd_dtmf_max= (dtmfMax==null)? 2 : dtmfMax;
                        switch(qtty){
                            case 5: retVal.ddd5 = ddd[4];
                            case 4: retVal.ddd4 = ddd[3];
                            case 3: retVal.ddd3 = ddd[2];
                            case 2: retVal.ddd2 = ddd[1];
                            case 1: retVal.ddd1 = ddd[0];
                            default:    
                        }
                        retVal.resultDescription="OK";

                    }
                 }
           } catch (MalformedEventException ex) {
            LogHelper.webLog.fatal("IvrStartSession: Exception: " + ex.getMessage(), ex);
                retVal.resultCode=5; 
                retVal.errorCode=10;
                retVal.resultDescription="Could not create a session, an exception occurs";
           }
        }
        else{
            //PDMB
        }
       
        return retVal;
    }

    /**
     * Web service operation
     */
    @WebMethod(operationName = "incrementCounter")
    public WsAckIntRsp incrementCounter(@WebParam(name = "ivrId")
    String ivrId, @WebParam(name = "msisdn")
    String msisdn, @WebParam(name = "appNameVersion")
    String appNameVersion, @WebParam(name = "sessionId")
    String sessionId, @WebParam(name = "dialogId")
    String dialogId) throws SOAPException {

        if (!ServerConfiguration.getInstance().serverRunning()){
            LogHelper.webLog.fatal(" Operation not processed.  Reason:Server stopped ");
            //return -99;
            throw new javax.xml.soap.SOAPException("ASA-Orchestrator server paused");
        }
        
        
        WsAckIntRsp retVal = new WsAckIntRsp();
        Subscriber subscriber = new Subscriber();
        int subscriberCheck;
        int response;

        try {
        	SessionService sessionWS = SessionService.getInstance();
            subscriberCheck = sessionWS.subscriberCheck(null, msisdn, subscriber);
        } 
        catch (Exception ex) {
            subscriberCheck=-9900;
            LogHelper.webLog.error("IvrStartSession: SubscriberCheck Exception: " + ex.getMessage(), ex);
        }

        if (subscriberCheck != 0)  // No existe un suscriptor disponible o esta bloqueado
        {
            LogHelper.webLog.debug("incrementCounter: subscriber not found or blocked: MSISDN: " + msisdn);
            retVal.resultCode=100;
            retVal.resultDescription="subscriber not found or blocked.";
            return retVal;
        }

        response = ConnectionManager.getInstance().incrementSubscriberRetryCounter(subscriber.getIMSI_t());

        if (response != 0) {
            LogHelper.webLog.debug("incrementCounter: Error in incremetRetries with IMSI: " + subscriber.getIMSI_t());
            retVal.resultCode = response;
            retVal.resultDescription = "Error during incrementation.";
            return retVal;
        }
        
        retVal.resultCode=0;
        retVal.resultDescription="";
        return retVal;
    }

    /**
     * Web service operation
     */
    @WebMethod(operationName = "checkDdd")
    public CheckDddRsp checkDdd(
    		@WebParam(name = "ivrId") String ivrId, 
    		@WebParam(name = "msisdn") String msisdn, 
    		@WebParam(name = "appNameVersion") String appNameVersion, 
    		@WebParam(name = "ddd") String ddd, 
    		@WebParam(name = "sessionId") String sessionId, 
    		@WebParam(name = "dialogId") String dialogId) 
    				throws SOAPException 
    {
        if (!ServerConfiguration.getInstance().serverRunning()){
            LogHelper.webLog.fatal(" Operation not processed.  Reason:Server stopped ");
            throw new javax.xml.soap.SOAPException("ASA-Orchestrator server paused");
        }
        
        CheckDddRsp retVal = new CheckDddRsp();
        int validDDD;

        validDDD = ConnectionManager.getInstance().checkDDD(ddd);
        if (validDDD == 0) {
            retVal.resultCode=0;
            retVal.resultDescription="";
            retVal.isValidDDD=0;
            LogHelper.webLog.debug("IVR:checkDdd: DDD: " + ddd + " is valid ");
            return retVal;
        }

        //si es inválido, mando la descripción
        retVal.isValidDDD = 1;
        retVal.resultCode = 0;
        if (validDDD == 1) {
            retVal.resultDescription="DDD doesn't exits";
            LogHelper.webLog.error("IVR:checkDdd: DDD doesn't exits");
        } 
        else{
            if (validDDD == 2) {
                retVal.resultDescription="DDD is null";
                LogHelper.webLog.error("IVR:checkDdd: DDD is null");
            }
            else{
                retVal.resultDescription="Problem validating DDD";
                LogHelper.webLog.error("IVR:startcheckDddActivation: Problem validating DDD");
            }
        }
        
        return retVal;
    }

    /**
     * Web service operation
     */
    @WebMethod(operationName = "startActivation")
    public StartActivationRsp startActivation(
    		@WebParam(name = "ivrId") String ivrId, 
    		@WebParam(name = "msisdn") String msisdn, 
    		@WebParam(name = "ddd") String ddd, 
    		@WebParam(name = "appNameVersion") String appNameVersion, 
    		@WebParam(name = "sessionId") String sessionId, 
    		@WebParam(name = "dialogId") String dialogId) 
    		throws SOAPException 
    {

        if (!ServerConfiguration.getInstance().serverRunning()) {
            LogHelper.webLog.fatal(" Operation not processed.  Reason:Server stopped ");
            throw new javax.xml.soap.SOAPException("ASA-Orchestrator server paused");
        }
        
        StartActivationRsp retVal = new StartActivationRsp();

        // Buscar la sesion y obtener sessionId y TransactionId
        SMEFacade sme = SMEFacade.getInstance();

        if (sme == null)  {
            LogHelper.webLog.fatal("IVR:startActivation: Can't instanciate SME Module.");
            retVal.resultCode=200;
            retVal.resultDescription="Could not instanciate SME Module.";
            return retVal;
        }
        
        WsAckRsp ws = new WsAckRsp();
        SearchCriteria search = new SearchCriteria(sessionId, null, msisdn, null);
        SessionData sd = sme.getSessionBySearchCriteria(search);

        if (sd != null) {
             LogHelper.dbLog( sd.getID(), null, "HIGH", "INFO","-> IVR StartActivation -> operationResult ","operationResult(sessionId="+sessionId+", transactionId "+ sd.getTransactionID()+", operationId="+ "GET_DDD"+", ResultCode="+ "0"+" ResultDescription="+ "descripcion"+", ResultData= "+ ddd+")") ; 
             retVal.resultCode=0;
             SessionService sessionWS;
			try {
				sessionWS = SessionService.getInstance();
				ws = sessionWS.operationResult(sessionId, sd.getTransactionID(), "GET_DDD|AREA", "0", "Generated from IVR:startActivation", ddd);
			} catch (Exception e) {
				LogHelper.webLog.fatal("IVR:startActivation: Can't instanciate SessionService.");
	            retVal.resultCode=200;
	            retVal.resultDescription="Could not instanciate SessionService.";
	            return retVal;
	         }
			
             if (ws.resultCode != 0L) {
                 retVal.resultDescription=ws.resultDescription;
                 retVal.status=5;
                 retVal.errorCode=3; //diferentes errores
                 LogHelper.webLog.error("IVR:startActivation: error in operationResult");
             }
             else{
                 StartActivationRsp sar = new StartActivationRsp();
                 sar = this.checkSessionStatus(ivrId, appNameVersion, dialogId, sessionId);
                 retVal.resultCode = (sar.resultCode==99)?0:sar.resultCode;
                 retVal.resultDescription = sar.resultDescription;
                 retVal.transferNumber = sar.transferNumber;
                 retVal.status = sar.status;
                 retVal.errorCode = sar.errorCode;
                 LogHelper.webLog.debug("IVR:startActivation: sending status");
             }
        }
        else {
            retVal.resultCode=200;
            retVal.resultDescription="Session doesn't exist";
            LogHelper.webLog.error("IVR:startActivation: Session dosen't exist");
        }
 
        return retVal;
    }

    /**
     * Web service operation
     */
    @WebMethod(operationName = "checkSessionStatus")
    public StartActivationRsp checkSessionStatus(
    		@WebParam(name = "ivrId") String ivrId, 
    		@WebParam(name = "appNameVersion") String appNameVersion, 
    		@WebParam(name = "dialogId") String dialogId, 
    		@WebParam(name = "sessionId") String sessionId) 
    				throws SOAPException 
    {
        if (!ServerConfiguration.getInstance().serverRunning()){
            LogHelper.webLog.fatal(" Operation not processed.  Reason:Server stopped ");
            throw new javax.xml.soap.SOAPException("ASA-Orchestrator server paused");
        }
        
        StartActivationRsp retVal = new StartActivationRsp();
            
        if (sessionId == null || sessionId.isEmpty()) {
            LogHelper.webLog.fatal("IVR:checkSessionStatus: Session is null or Empty.");
            retVal.resultCode=200;
            retVal.resultDescription="Session is null or Empty";
            return retVal;
        }
        
        SessionStatus sessionSt;
        SMEFacade sme = SMEFacade.getInstance();
        
        try { 
        	ManagementService managementWS =ManagementService.getInstance();
            sessionSt = managementWS.getSessionStatus(null, sessionId, null);
            if (sessionSt != null) {
                if (sme.getStateTag(sessionSt.sesStatus, sessionSt.stateMachineName, sessionSt.stateMachineVersion).isActivation() ){
                    LogHelper.webLog.debug("IVR:checkSessionStatus: session " + sessionId + " Activated");
                    retVal.resultCode=0;
                    retVal.errorCode=2;
                    retVal.resultDescription="OK";
                    retVal.transferNumber="";
                    retVal.status=5;
                }
                else if (sessionSt.finished.equals("T")) {
                    retVal.errorCode = ConnectionManager.getInstance().getDisplayMessageForIVR(sessionSt.originActivation, sessionSt.sesStatus, sessionSt.errorStatus);
                    retVal.resultCode = 0;
                    retVal.resultDescription = "OK";
                    retVal.status = 5;
                    retVal.transferNumber = "";
                    LogHelper.webLog.debug("IVR ErrorCode="+retVal.errorCode);
                
                }
                else if (sessionSt.finished.contentEquals("F")){
                	if (sme.getStateTag(sessionSt.sesStatus, sessionSt.stateMachineName, sessionSt.stateMachineVersion).isUserInfo()) {  
                        retVal.resultCode=99;
                        retVal.resultDescription="OK";
                        retVal.transferNumber="";
                        retVal.status=4;
                    }

                    if (sme.getStateTag(sessionSt.sesStatus, sessionSt.stateMachineName, sessionSt.stateMachineVersion).isActivation()){
                        // Activación en proceso
                        LogHelper.webLog.debug("IVR:checkSessionStatus: session " + sessionId + " Activation pending");
                        retVal.resultCode=0;
                        retVal.resultDescription="OK";
                        retVal.transferNumber="";
                        retVal.status=4;
                    }
                }
            }
            else{
                LogHelper.webLog.debug("IVR:checkSessionStatus: session " + sessionId + " not found");
                retVal.resultCode=320;
                retVal.resultDescription="Session not found";
            }     
        } 
        catch (Exception ex) {
            LogHelper.webLog.fatal("IVR:checkSessionStatus: session:" + sessionId + " Exception:" + ex.getMessage(),ex);
            retVal.resultCode=320;
            retVal.resultDescription="Internal error.";
        }
        
        LogHelper.webLog.debug("IVR:checkSessionStatus: session " + sessionId + " IVR ErrorCode="+retVal.errorCode);        
        return retVal;                
    }

    /**
     * Web service operation
     */
    @WebMethod(operationName = "operation")
    public String operation() {
        //TODO write your implementation code here:
        return null;
    }
    
}