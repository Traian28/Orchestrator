package com.ats_connection.asa.orchestrator.core;

import com.ats_connection.asa.orchestrator.config.ConnectionManager;
import com.ats_connection.asa.orchestrator.helper.LogHelper;
import com.ats_connection.asa.orchestrator.response.WsAckRsp;
import com.ats_connection.asa.orchestrator.sme.Session.SessionData;
import com.ats_connection.asa.orchestrator.sme.api.SMEFacade;
import com.ats_connection.asa.orchestrator.sme.helper.SearchCriteria;
import com.ats_connection.asa.orchestrator.sme.stma.state.StateTag;

public class ManagementService {

	private static ManagementService instance = null;
	
	private ManagementService() {
		
	}
	
	public static synchronized ManagementService getInstance() {
		if (instance == null)
			instance = new ManagementService();
		
		return instance;
	}
	
	 /**
     * Devuelve los datos de una sesion.  La busqueda se realiza primero en memoria.  
     * Si la sesion no existe en memoria, se busca en la base de datos.
     * Devuelve los datos tanto de una sesion activa como de una finalizada.
     * 
     * @param filterMSISDN_t Valor del MSISDN Fake para buscar la sesion.  Puede venir vacio.
     * @param filterSessionId Valor del SessionId para buscar la sesion.  Puede venir vacio.
     * @param filterICCID Valor del ICCID para buscar la sesion.  Puede venir vacio.
     * @return una sesion o null
     */
    public SessionStatus getSessionStatus(String filterMSISDN_t, String filterSessionId, String filterICCID) 
    {
       LogHelper.webLog.debug("getSessionStatus: Start filterMSISDN=" + filterMSISDN_t + " sessionId="+ filterSessionId + " Iccid=" + filterICCID);
       
       SessionStatus session = null;
       StateTag state = null;

       SMEFacade stateMachineExec = SMEFacade.getInstance();

       if (stateMachineExec == null) {
            LogHelper.webLog.fatal("getSessionStatus: Can't instanciate SME Module.");
            return null;
       }
       
       /*  Busca Sesion
         SearchCriteria constructor Parameters:
            ID - 
            IMSI_FAKE - 
            MSISDN_FAKE - 
            ICCID -
       */
       if (filterMSISDN_t != null && filterMSISDN_t.isEmpty()) filterMSISDN_t=null;
       if (filterSessionId != null && filterSessionId.isEmpty()) filterSessionId=null;
       if (filterICCID != null && filterICCID.isEmpty()) filterICCID=null;
       
       SearchCriteria search = new SearchCriteria(filterSessionId, null, filterMSISDN_t, filterICCID);
       SessionData sd = stateMachineExec.getSessionBySearchCriteria(search);
        
       // Si no existe en memoria lo busca en la base de datos
       if (sd == null) { 
            //busca el registro en la base.
            LogHelper.webLog.debug("getSessionStatus: Session not found in memory. Searching in DB.  FilterMSISDN=" + filterMSISDN_t 
            		+ " sessionId="+ filterSessionId + " Iccid=" + filterICCID);
            
            session = ConnectionManager.getInstance().getSession(filterSessionId, filterMSISDN_t, filterICCID);

            if (session != null) {
            	session.whereIs = "DB";
            	session.sesStatusPhase = "0";
            	
            	if (session.sesStatus != null) {
            		state = stateMachineExec.getStateTag(session.sesStatus, session.stateMachineName, session.stateMachineVersion);
            	}
            }
       }
       else {
    	    session = new SessionStatus();
            LogHelper.webLog.debug("getSessionStatus: Session found in memory. FilterMSISDN=" + filterMSISDN_t 
            		+ " sessionId="+ filterSessionId + " Iccid=" + filterICCID);
            
            session.whereIs = "M";
            session.sesStatus = sd.getStateMachineState().getName();
            session.iccid = sd.getICCID();
            session.imsi_t = sd.getIMSI_FAKE();
            session.msisdn_t = sd.getMSISDN_FAKE();
            session.sessionId = sd.getID();
            session.transactionId = sd.getTransactionID().toString();
            session.finished= (sd.isFinished()==true?"T":"F");
            session.imsi_real = sd.getIMSI();
            session.msisdn_real = sd.getMSISDN();
            session.originActivation = sd.getOriginActivation().getName();
            session.activationType = sd.getActivationType();
            session.areaCode = sd.getAreaCode();
            session.errorStatus = sd.getStateMachineStatusError();
            session.loci = sd.getLOCI();
            session.imsi_roaming = sd.getIMSI_ROAMING();
            session.imei = sd.getIMEI();
            session.auxiliarData = sd.getAuxiliarData();
            session.virtualNetwork = sd.getVirtualNetwork();
            session.userType = sd.getUserType();
            session.opc =  sd.getOpc();
            session.subApplication = sd.getApplication();
            session.cardType = sd.getCardType();
            session.operatorKey = sd.getOpKey();
            session.stateMachineName = sd.getStateMachineName();
            session.stateMachineVersion = sd.getStateMachineVersion();
            session.stateName = sd.getStateMachineState().getName();
            
            session.sesStatusPhase = "0";
            
            state = sd.getStateMachineState();
       }
       
       if (session != null && state != null) {
    	   if (state.isUserInfo())
    		   session.sesStatusPhase = "1";
           if (state.isProgramming())
        	   session.sesStatusPhase = "2";
           if (state.isActivation())
        	   session.sesStatusPhase = "3";
           if (state.isRollback())
        	   session.sesStatusPhase = "4";
           if (state.isPersist())
        	   session.sesStatusPhase = "5";
           if (state.isPreActivation())
        	   session.sesStatusPhase = "6";
       }

       return session;
    }

	 /**
     * Devuelve los datos de una sesion.  La busqueda se realiza primero en memoria.  
     * Si la sesion no existe en memoria, se busca en la base de datos.
     * Devuelve los datos tanto de una sesion activa como de una finalizada.
     * 
     * @param ID sessionId.  Puede venir vacio.
     * @param IMSI_FAKE.  Puede venir vacio.
     * @param MSISDN_FAKE.  Puede venir vacio.
     * @param ICCID.  real o fake. Puede venir vacio.
     * @param IMSI.  real Puede venir vacio.
     * @param MSISDN.  real Puede venir vacio.
     * @return una sesion o null
     */
    public SessionStatus getSessionStatus(String ID, String IMSI_FAKE, String MSISDN_FAKE, String ICCID, String IMSI, String MSISDN) 
    {
       LogHelper.webLog.debug("getSessionStatus: Start SessionID=" + ID 
    		   + " IMSI_fake=" + IMSI_FAKE 
    		   + " MSISDN_fake=" + MSISDN_FAKE 
    		   + " ICCD=" + ICCID  
    		   + " IMSI=" + IMSI
    		   + " MSISDN=" + MSISDN);
       
       SessionStatus session = null;
       StateTag state = null;

       SMEFacade stateMachineExec = SMEFacade.getInstance();

       if (stateMachineExec == null) {
            LogHelper.webLog.fatal("getSessionStatus: Can't instanciate SME Module.");
            return null;
       }
       
       /*  Busca Sesion
         SearchCriteria constructor Parameters:
            ID - 
            IMSI_FAKE - 
            MSISDN_FAKE - 
            ICCID -
       */
       if (ID != null && ID.isEmpty()) ID=null;
       if (IMSI_FAKE != null && IMSI_FAKE.isEmpty()) IMSI_FAKE=null;
       if (MSISDN_FAKE != null && MSISDN_FAKE.isEmpty()) MSISDN_FAKE=null;
       if (IMSI != null && IMSI.isEmpty()) IMSI=null;
       if (MSISDN != null && MSISDN.isEmpty()) MSISDN=null;
       
       SearchCriteria search = new SearchCriteria(ID, IMSI_FAKE, MSISDN_FAKE, ICCID, IMSI, MSISDN);
       SessionData sd = stateMachineExec.getSessionBySearchCriteria(search);
        
       // Si no existe en memoria lo busca en la base de datos
       if (sd == null) { 
            //busca el registro en la base.
            LogHelper.webLog.debug("getSessionStatus: Session not found in memory. Searching in DB.  SessionID=" + ID 
						 		   + " IMSI_fake=" + IMSI_FAKE 
						 		   + " MSISDN_fake=" + MSISDN_FAKE 
						 		   + " ICCD=" + ICCID  
						 		   + " IMSI=" + IMSI
						 		   + " MSISDN=" + MSISDN);
            
            session = ConnectionManager.getInstance().getSession(ID, IMSI_FAKE, MSISDN_FAKE, ICCID, IMSI, MSISDN);
            
            if (session != null) {
            	session.whereIs = "DB";
            	session.sesStatusPhase = "0";
            	
            	if (session.sesStatus != null) {
            		state = stateMachineExec.getStateTag(session.sesStatus, session.stateMachineName, session.stateMachineVersion);
            	}
            }
       }
       else {
    	    session = new SessionStatus();
            LogHelper.webLog.debug("getSessionStatus: Session found in memory. SessionID=" + ID 
						 		   + " IMSI_fake=" + IMSI_FAKE 
						 		   + " MSISDN_fake=" + MSISDN_FAKE 
						 		   + " ICCD=" + ICCID  
						 		   + " IMSI=" + IMSI
						 		   + " MSISDN=" + MSISDN);
            
            session.whereIs = "M";
            session.sesStatus = sd.getStateMachineState().getName();
            session.iccid = sd.getICCID();
            session.imsi_t = sd.getIMSI_FAKE();
            session.msisdn_t = sd.getMSISDN_FAKE();
            session.sessionId = sd.getID();
            session.transactionId = sd.getTransactionID().toString();
            session.finished= (sd.isFinished()==true?"T":"F");
            session.imsi_real = sd.getIMSI();
            session.msisdn_real = sd.getMSISDN();
            session.originActivation = sd.getOriginActivation().getName();
            session.activationType = sd.getActivationType();
            session.areaCode = sd.getAreaCode();
            session.errorStatus = sd.getStateMachineStatusError();
            session.loci = sd.getLOCI();
            session.imsi_roaming = sd.getIMSI_ROAMING();
            session.imei = sd.getIMEI();
            session.auxiliarData = sd.getAuxiliarData();
            session.virtualNetwork = sd.getVirtualNetwork();
            session.userType = sd.getUserType();
            session.opc =  sd.getOpc();
            session.subApplication = sd.getApplication();
            session.cardType = sd.getCardType();
            session.operatorKey = sd.getOpKey();
            session.stateMachineName = sd.getStateMachineName();
            session.stateMachineVersion = sd.getStateMachineVersion();
            session.stateName = sd.getStateMachineState().getName();
            
            session.sesStatusPhase = "0";
            
            state = sd.getStateMachineState();
       }
       
       if (session != null && state != null) {
    	   if (state.isUserInfo())
    		   session.sesStatusPhase = "1";
           if (state.isProgramming())
        	   session.sesStatusPhase = "2";
           if (state.isActivation())
        	   session.sesStatusPhase = "3";
           if (state.isRollback())
        	   session.sesStatusPhase = "4";
           if (state.isPersist())
        	   session.sesStatusPhase = "5";
           if (state.isPreActivation())
        	   session.sesStatusPhase = "6";
       }
       return session;
    }
    
    public WsAckRsp persistSessionData(String sessionId) {
    	LogHelper.webLog.debug("Management: persistSessionData("+sessionId+")");
    	WsAckRsp retVal = new WsAckRsp();
    	
    	LogHelper.webLog.debug("Management: persistSessionData() - getting SME");
    	// Obtiene instancia de la máquina de estados
        SMEFacade stateMachineExec = SMEFacade.getInstance();
        
        /*  Busca Sesion
         SearchCriteria constructor Parameters:
            ID - 
            IMSI_FAKE - 
            MSISDN_FAKE - 
            ICCID -
         */
        SearchCriteria search = new SearchCriteria(sessionId, null, null, null);
        
        LogHelper.webLog.debug("Management: persistSessionData() - getting SessionData");
        SessionData sd = stateMachineExec.getSessionBySearchCriteria(search);
        
        if (sd != null) {
        	LogHelper.webLog.error("Management: persistSessionData(): Session found: sessionId:"+ sessionId);
            //busca el registro en la base.
        	ConnectionManager connMgr = ConnectionManager.getInstance();
            long sesCount = connMgr.checkSession(sessionId, null, null);

            // actualiza o inserta el registro
            if (sesCount == 1 || sesCount == 0) {
            	
            	// Verificamos consistencia de datos
            	if ((sd.getTransactionID().intValue() > 0 && sesCount == 0) /*||
            		(sd.getTransactionID().intValue() == 0 && sesCount == 1)*/ ) // pdd2018 Eliminamos el chequeo por el problema de los startSIM que llegan juntos
            	{
            		retVal.resultCode=10L;
                    retVal.resultDescription="Data consistency error on session "+ sessionId;
                    LogHelper.webLog.debug("Management: persistSessionData() - Data consistency error on session " + sessionId + "sesCount: "+sesCount+" - trxId: "+sd.getTransactionID().intValue());
                    LogHelper.dbLog ( sd.getID(), null, "HIGH", "ERROR","-> PersistSessiondata - Data consistency error on session ","Persist (sessionId=" + sessionId + ")" );
                    return retVal;
            	}
            	
            	int resp = connMgr.sessionToDB(sesCount, sd);
                if (resp == 0) {
                    retVal.resultCode=0L;
                    retVal.resultDescription="OK";
                    LogHelper.webLog.debug("Management: persistSessionData(): Ok session persisted: " + sessionId);
                    LogHelper.dbLog ( sd.getID(), null, "HIGH", "INFO","-> PersistSessiondata - OK","Persist (sessionId=" + sessionId + ")" ); 
                }
                else {
                	if (resp == 1 && sesCount == 0) {
                		// Si fallo el insert informo a la capa superior con otro error
                		retVal.resultCode = Constants.PERSIST_SESSION_INSERT_ERROR;
                		retVal.resultDescription = Constants.PERSIST_SESSION_INSERT_ERROR_DESC;
                	}
                	else {
                		retVal.resultCode = Constants.PERSIST_SESSION_ERROR;
                		retVal.resultDescription = Constants.PERSIST_SESSION_ERROR_DESC;
                	}
                    
                    LogHelper.webLog.error("Management: persistSessionData(): Error persisting session: " + sessionId);
                    LogHelper.dbLog ( sd.getID(), null, "HIGH", "INFO","-> PersistSessiondata - !ERROR"," (Error) Persist (sessionId=" + sessionId + ")" ); 
                }
            }
            else {
                retVal.resultCode = Constants.PERSIST_SESSION_COUNT_ERROR;
                retVal.resultDescription = Constants.PERSIST_SESSION_COUNT_ERROR_DESC;
                LogHelper.webLog.fatal("Management: persistSessionData(): SessionCount Error: sesCount=" + sesCount+ " sessionId="+ sessionId);
            }
        }
        else {
            retVal.resultCode = Constants.PERSIST_SESSION_NOT_FOUND;
            retVal.resultDescription = Constants.PERSIST_SESSION_NOT_FOUND_DESC + sessionId;
            LogHelper.webLog.error("Management: persistSessionData(): Session not found: sessionId:"+ sessionId);
        }

    	return retVal;
    }
}
