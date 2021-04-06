/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ats_connection.asa.orchestrator.response;

import com.ats_connection.asa.orchestrator.sme.Session;
import java.util.ArrayList;
import org.apache.log4j.Logger;

/**
 *
 * @author dmarcogliese
 */
public class AnalitycsResp extends Response{

    private static org.apache.log4j.Logger logger = Logger.getLogger(AnalitycsResp.class);
        
    public long enqueuedEvents;
    
    public ArrayList<RunningSessionResp> runningSessions = new ArrayList<RunningSessionResp>();
    public int runningSessionsQty;
    
    public ArrayList<SessionOutOfSyncResp> sessionsOutOfSync = new ArrayList<SessionOutOfSyncResp>();
    public int sessionsOutOfSyncQty;
    
    public ArrayList<SessionDifferenceQueueResp> sessionsDifferenceQueue = new ArrayList<SessionDifferenceQueueResp>();
    public int sessionsDifferenceQueueQty;
    
    public AnalitycsResp(){
    }

    public AnalitycsResp(ResultCode rc){
        super(rc);
    }
    
    public AnalitycsResp(long enqueuedEvents, ArrayList<Session> runningSes, ArrayList<Session> sesOutOfSync, ArrayList<Session> sesDiffQueue){
        super(ResultCode.OK);

        logger.debug("AnalitycsResp - Creating AnalitycsResp");
        
        this.enqueuedEvents = enqueuedEvents;
        
        if (runningSes!=null){
            logger.debug("AnalitycsResp - hay runningSessions");
            for (Session session : runningSes) {
                logger.debug("AnalitycsResp - listando runningSessions");
                runningSessions.add(new RunningSessionResp(session));
            }
            runningSessionsQty =  runningSessions.size();
        }
        
        if (sesDiffQueue!=null){
            logger.debug("AnalitycsResp - hay sesDiffQueue");
            for (Session session : sesDiffQueue) {
                logger.debug("AnalitycsResp - listando sesDiffQueue");
                sessionsDifferenceQueue.add(new SessionDifferenceQueueResp(session));
            }
            sessionsDifferenceQueueQty = sessionsDifferenceQueue.size();
        }
        
        if (sesOutOfSync!=null){
            logger.debug("AnalitycsResp - hay sesOutOfSync");
            for (Session session : sesOutOfSync) {
                logger.debug("AnalitycsResp - listando runningSessions");
                sessionsOutOfSync.add(new SessionOutOfSyncResp(session));
            }
            sessionsOutOfSyncQty = sessionsOutOfSync.size();
        }
    }
    
}
