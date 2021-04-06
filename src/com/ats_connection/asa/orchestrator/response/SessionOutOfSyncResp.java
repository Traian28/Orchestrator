/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ats_connection.asa.orchestrator.response;

import com.ats_connection.asa.orchestrator.sme.Session;
import com.ats_connection.asa.orchestrator.sme.Session.SessionData;

/**
 *
 * @author dmarcogliese
 */
public class SessionOutOfSyncResp extends Response {
    
    public String ID;
    public String MSISDNFake;
    public String ICCID;
    public int enqueueEvents;
    public int eventCounter;
    public String level;
    
    public enum Level {UNKNOWN, WARNING, ERROR};

    public SessionOutOfSyncResp(){
    }
    
    public SessionOutOfSyncResp(ResultCode rc){
        super(rc);
    }
    
    public SessionOutOfSyncResp(Session session){
        
        super(ResultCode.OK);
        
        SessionData sessionData = session.getSessionData();
        ID = sessionData.getID();
        MSISDNFake = sessionData.getMSISDN_FAKE();
        ICCID = sessionData.getICCID();
        enqueueEvents = session.getEventCounter().getCounter();
        eventCounter = session.getEventQueueSize();
        
        if(enqueueEvents > 0 && eventCounter == 0){ 
            level = Level.ERROR.toString();
        } else if(enqueueEvents == 0 && eventCounter > 0){ 
            level = Level.WARNING.toString();
        } else {
            level = Level.UNKNOWN.toString();
        }

     }

}
