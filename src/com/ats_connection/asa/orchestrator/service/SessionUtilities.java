package com.ats_connection.asa.orchestrator.service;

import com.ats_connection.asa.orchestrator.response.*;
import com.ats_connection.asa.orchestrator.core.SessionService;
import com.ats_connection.asa.orchestrator.exception.*;
import com.ats_connection.asa.orchestrator.sme.Session;
import com.ats_connection.asa.orchestrator.sme.Session.SessionData;
import com.ats_connection.asa.orchestrator.sme.api.SMEFacade;
import com.ats_connection.asa.orchestrator.sme.helper.FilterCriteria;
import com.ats_connection.asa.orchestrator.sme.helper.SearchCriteria;
import com.ats_connection.asa.orchestrator.sme.stma.state.StateTag;
import com.ats_connection.asa.server.logger.AsaServerLogger;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

import org.apache.log4j.Logger;

/**
 *
 * @author dmarcogliese
 */
@WebService()
public class SessionUtilities {

    private static org.apache.log4j.Logger logger = Logger.getLogger(SessionUtilities.class);

    /**
     * Web service operation
     */
    @WebMethod(operationName = "getSessionDetails")
    public SessionDetailsResp getSessionDetails(@WebParam(name = "sessionID") String sessionID) 
    {
        SessionDetailsResp sessionResp = null;

        if (sessionID == null || sessionID.trim().isEmpty()) {
            sessionResp = new SessionDetailsResp(ResultCode.NOT_SESSION_ID);
            logger.info("getSession - "+sessionResp.resultDescription);
            return  sessionResp;
        } 

        SMEFacade stateMachine = SMEFacade.getInstance();

        if (stateMachine != null) {
            logger.debug("getSession - FilterCriteria sessionID:" + sessionID );
            SearchCriteria searchCriteria = new SearchCriteria(sessionID, null, null, null, null, null);
            SessionData sessionData = stateMachine.getSessionBySearchCriteria(searchCriteria);

            if (sessionData != null) {
                sessionResp = new SessionDetailsResp(sessionData);
                logger.info("getSession - Session dataID: " + sessionData.getID());
            }
            else {
                sessionResp = new SessionDetailsResp(ResultCode.SESSION_NOT_FOUND);
                logger.info("getSession - "+sessionResp.resultDescription);
            }
        }
        else {
            sessionResp = new SessionDetailsResp(ResultCode.STATE_MACHINE_ERROR);
            logger.error("getSession - "+sessionResp.resultDescription);
        }

        return sessionResp;
    }

    /**
     * Web service operation
     */
    @WebMethod(operationName = "getSessionsList")
    public SessionsListResp getSessionsList(
            @WebParam(name = "state") String stateFilter,
            @WebParam(name = "beginDate") String beginDate,
            @WebParam(name = "endDate") String endDate) 
    {
        logger.debug("getSessionsList - stateFilter:" + stateFilter + " beginDate:" + beginDate + " endDate:" + endDate);

        SessionsListResp sessionsListResp = null;
        SMEFacade stateMachine = SMEFacade.getInstance();
       
        if (stateMachine!=null) {
            try {
                ArrayList<SessionData> sessionsList = getFilteredSessions(beginDate, endDate, stateFilter, stateMachine);
                logger.info("getSessionsList - Sessions qty:" + sessionsList.size());
                sessionsListResp = new SessionsListResp(sessionsList);
            } 
            catch (ParseDateException ex) {
                logger.error(ex.getMessage());
                logger.error(ex.getCause());
                sessionsListResp = new SessionsListResp(ResultCode.INVALID_DATE_FORMAT);    
            } 
            catch(IllegalArgumentException ex) {
                logger.error(ex.getMessage());
                logger.error(ex.getCause());
                sessionsListResp = new SessionsListResp(ResultCode.INVALID_STATE);                    
            }
        }
        else {
            sessionsListResp = new SessionsListResp(ResultCode.STATE_MACHINE_ERROR);
        }

        logger.info("getSessionsList - sessionsListResp:" + sessionsListResp.resultDescription);
        return sessionsListResp;
    }

    /**
     * Web service operation
     */
    @WebMethod(operationName = "getSession")
    public SessionItemResp getSession(
            @WebParam(name = "sessionID") String sessionID,
            @WebParam(name = "imsiFake") String imsiFake,
            @WebParam(name = "msisdnFake") String msisdnFake,
            @WebParam(name = "iccid") String iccid,
            @WebParam(name = "imsi") String imsi,
            @WebParam(name = "msisdn") String msisdn) 
    {
        SessionItemResp sessionItem = null;

        if (sessionID != null && sessionID.trim().isEmpty()) {
            sessionID = null;
        }
        if (imsiFake != null && imsiFake.trim().isEmpty()) {
            imsiFake = null;
        }
        if (msisdnFake != null && msisdnFake.trim().isEmpty()) {
            msisdnFake = null;
        }
        if (iccid != null && iccid.trim().isEmpty()) {
            iccid = null;
        }
        if (imsi != null && imsi.trim().isEmpty()) {
            imsi = null;
        }
        if (msisdn != null && msisdn.trim().isEmpty()) {
            msisdn = null;
        }

        if ((sessionID == null) && (imsiFake == null) && (msisdnFake == null) && (iccid == null) && (imsi == null) && (msisdn == null)) {
            sessionItem = new SessionItemResp(ResultCode.SEARCH_CRITERIA_NOT_DEFINED);
            logger.info("getSession - "+sessionItem.resultDescription);
            return  sessionItem;
        } 

        SMEFacade stateMachine = SMEFacade.getInstance();

        if (stateMachine != null) {

            logger.debug("getSession - FilterCriteria sessionID:" + sessionID + " imsiFake:" + imsiFake + " msisdnFake:" + msisdnFake + " iccid:" + iccid + " imsi:" + imsi + " msisdn:" + msisdn);
            SearchCriteria searchCriteria = new SearchCriteria(sessionID, imsiFake, msisdnFake, iccid, imsi, msisdn);
            SessionData sessionData = stateMachine.getSessionBySearchCriteria(searchCriteria);

            if (sessionData != null) {
                sessionItem = new SessionItemResp(sessionData);
                logger.info("getSession - Session dataID: " + sessionData.getID());
            } else {
                sessionItem = new SessionItemResp(ResultCode.SESSION_NOT_FOUND);
                logger.info("getSession - "+sessionItem.resultDescription);
            }
            
        } else {
            sessionItem = new SessionItemResp(ResultCode.STATE_MACHINE_ERROR);
            logger.error("getSession - "+sessionItem.resultDescription);
        }

        return sessionItem;
    }

    /**
     * Web service operation
     */
    
    @WebMethod(operationName = "evolveSession")
    public EvolveSessionResp evolveSession(
            @WebParam(name = "sessionID") String sessionID,
            @WebParam(name = "operation") String operation,
            @WebParam(name = "resultCode") String resultCode,
            @WebParam(name = "resultData") String resultData) {

        EvolveSessionResp response = null;

        if (sessionID==null || sessionID.trim().isEmpty()) {
            response = new EvolveSessionResp(ResultCode.NOT_SESSION_ID);
            logger.info("evolveSession - " + response.resultDescription);            
            return response;
        }
        if (operation==null || operation.trim().isEmpty()) {
            response = new EvolveSessionResp(ResultCode.NOT_OPERATION_NAME);
            logger.info("evolveSession - " + response.resultDescription);
            return response;
        }
        
        SMEFacade stateMachine = SMEFacade.getInstance();

        if (stateMachine!=null) {

            SearchCriteria searchCriteria = new SearchCriteria(sessionID, null, null, null);
            SessionData sessionData = stateMachine.getSessionBySearchCriteria(searchCriteria);
            
            if (sessionData!=null) {
                response = sendOperation(sessionData, operation, resultCode, resultData);                
            } else {
                response = new EvolveSessionResp(ResultCode.SESSION_NOT_FOUND);
            }
            
        } else {
            response = new EvolveSessionResp(ResultCode.STATE_MACHINE_ERROR);
        }                                   
        
        return response;
    }
    
    /**
     * Web service operation
     */
    @WebMethod(operationName = "evolveSessionsFiltered")
    public EvolveListSessionResp evolveSessionsFiltered(
            @WebParam(name = "state") String state,
            @WebParam(name = "beginDate") String beginDate,
            @WebParam(name = "endDate") String endDate,
            @WebParam(name = "operation") String operation,
            @WebParam(name = "resultCode") String resultCode,
            @WebParam(name = "resultData") ArrayList<String> resultData) 
    {
        EvolveListSessionResp evolListSessionResp = null;
        
        if (operation==null || operation.trim().isEmpty()) {
            evolListSessionResp = new EvolveListSessionResp(ResultCode.NOT_OPERATION_NAME);
            logger.info("evolveSessionsFiltered - "+evolListSessionResp.resultDescription);
            return evolListSessionResp;
        }

        SMEFacade stateMachine = SMEFacade.getInstance();
       
        if (stateMachine!=null) {

            try {
                ArrayList<SessionData> sessionsList = getFilteredSessions(beginDate, endDate, state, stateMachine);
                for (Iterator<SessionData> it = sessionsList.iterator(); it.hasNext();) {
                    SessionData sessionData = it.next();
                    sendOperation(sessionData, operation, resultCode, "");
                    //evolListSessionResp.add(sendOperation(sessionData, operation, resultCode, ""));
                }
                evolListSessionResp = new EvolveListSessionResp(ResultCode.OK);
            } catch (ParseDateException ex){
                logger.error(ex.getMessage());
                logger.error(ex.getCause());
                evolListSessionResp = new EvolveListSessionResp(ResultCode.INVALID_DATE_FORMAT);    
            }

        } else {
            evolListSessionResp = new EvolveListSessionResp(ResultCode.STATE_MACHINE_ERROR);
        }

        return evolListSessionResp;
    }

    
    /**
     * Web service operation
     */
    @WebMethod(operationName = "getAnalytics")
        public AnalitycsResp getAnalytics() {
          
        AnalitycsResp response = null;
        SMEFacade stateMachine = SMEFacade.getInstance();
       
        if (stateMachine!=null) {
            long enqueuedEvents = stateMachine.getEnqueueEvents();
            ArrayList<Session> runningSessions = getSessionsFromSessionsData(stateMachine);
            ArrayList<Session> sessionsOutOfSync = stateMachine.getSessionsOutOfSyncForStats(); 
            ArrayList<Session> sessionsDifferenceQueue = getSessionsDifferenceQueue(stateMachine);    
            response = new AnalitycsResp(enqueuedEvents, runningSessions, sessionsOutOfSync, sessionsDifferenceQueue);
            
        } else {
            response =new AnalitycsResp(ResultCode.STATE_MACHINE_ERROR);
        }

        return response;
    }
    

    /**
     * Web service operation
     */
    @WebMethod(operationName = "persistSession")
    public PersistSessionResp persistSession(
            @WebParam(name = "sessionID") String sessionID) {
        
        PersistSessionResp persistSesResp = null;

        if (sessionID == null || sessionID.trim().isEmpty()) {
            persistSesResp = new PersistSessionResp(ResultCode.NOT_SESSION_ID);
            logger.info("persistSession - "+persistSesResp.resultDescription);
            return persistSesResp;
        } 

        try {                
            AsaServerLogger serverLogger = AsaServerLogger.getInstance();
            int result = serverLogger.persistError(sessionID,null,null,null);
            switch (result) {
                case AsaServerLogger.ERROR_CODE_OK:
                    persistSesResp = new PersistSessionResp(ResultCode.OK);
                    logger.info("persistSession - Session persisted OK");
                    break;
                case AsaServerLogger.ERROR_CODE_NO_DATA_FOUND:
                    persistSesResp = new PersistSessionResp(ResultCode.SESSION_NOT_FOUND);
                    logger.info("persistSession - "+persistSesResp.resultDescription);
                    break;
                case AsaServerLogger.ERROR_CODE_DATABASE_ERROR:
                    persistSesResp = new PersistSessionResp(ResultCode.ACCESS_DATA_ERROR);
                    logger.info("persistSession - "+persistSesResp.resultDescription);
                    break;
                default:
                    persistSesResp = new PersistSessionResp(ResultCode.UNKNOWN_ERROR);
                    break;
            }
        } catch (Exception ex) {
            logger.error("Could not instanciate Server Logger - " + ex.getMessage());
            logger.error(ex.getStackTrace());
            persistSesResp = new PersistSessionResp(ResultCode.SERVER_LOGGER_ERROR);
        }

        return persistSesResp;
        
    }

    
    
    private ArrayList<Session> getSessionsDifferenceQueue(SMEFacade stateMachine){

        ArrayList<Session> sessions = stateMachine.getSessionsForStats();
        ArrayList<Session> sessionsDif = new ArrayList<Session>();

        for (Session session : sessions) {
            if (session.getEventCounter().getCounter() != session.getEventQueueSize() && !session.isProcessingEvent()) {
                sessionsDif.add(session);
            }
        }
        return sessionsDif;
    }    
    
        
    private ArrayList<Session> getSessionsFromSessionsData(SMEFacade stateMachine ){
        ArrayList<SessionData> sessionsData = stateMachine.getRunningSessions();
        ArrayList<Session> sessions = new ArrayList<Session>(); 
        for (SessionData sessionData : sessionsData) {
            Session ses = stateMachine.getSessionForStats(sessionData.getID());
            sessions.add(ses);
        }
        return (sessions.size()>0)?sessions:null;
    }

    private EvolveSessionResp sendOperation(SessionData session, String operation, String resultCode, String resultData){
        WsAckRsp ws = new WsAckRsp();
        String message = new String("-- Sent from API --");
        try {
            ws = SessionService.getInstance().operationResult(session.getID(), session.getTransactionID(), operation, resultCode, message, resultData);
        } catch (Exception ex) {
            logger.error("Could not evolve session - " + ex.getMessage());
            logger.error(ex.getStackTrace());
            return new EvolveSessionResp(ResultCode.CAN_NOT_EVOLVE_SESSION);
        }

        return new EvolveSessionResp(ws.resultCode.intValue(), ws.resultDescription);
    }
    
    
    private ArrayList<SessionData> getFilteredSessions(String beginDate, String endDate, String stateFilter, SMEFacade stateMachine) 
    		throws ParseDateException
    {
        Calendar beginDateCal = createDate(beginDate);
        Calendar endDateCal = createDate(endDate);

        StateTag state = null;
        if (stateFilter != null && !stateFilter.trim().isEmpty()) {
            state = stateMachine.getStateTag(stateFilter);
            logger.debug("getFilteredSessions - State: " + state.getName());
        }

        ArrayList<SessionData> sessionsList = null;
        if ((beginDateCal != null) || (endDateCal != null) || (state != null)) {
            FilterCriteria filterCriteria = new FilterCriteria(state, beginDateCal, endDateCal);
            logger.debug("getFilteredSessions - getting filtered sessions - beginDateCal:" + beginDateCal + " endDateCal:" + endDateCal + " state:" + state);
            sessionsList = stateMachine.getSessionsByFilterCriteria(filterCriteria);
        } else {
            logger.debug("getFilteredSessions - getting all sessions");
            sessionsList = stateMachine.getSessions();
        }
        
        return sessionsList;
    }
    
    
    private Calendar createDate(String sDate) throws ParseDateException {

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Calendar calendar = null;
        Date date = null;
        
        if (sDate!=null && !sDate.trim().isEmpty()) {
            try {
                calendar = GregorianCalendar.getInstance();
                date = sdf.parse(sDate);
                calendar.setTime(date);
            } catch (ParseException ex) {
                throw new ParseDateException(ex);
            }
        }
        return calendar;
    }


}
