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
public class SessionDifferenceQueueResp extends Response {
    
    public String ID;
    public String MSISDNFake;
    public String ICCID;
    public int enqueueEvents;
    public int eventCounter;


    public SessionDifferenceQueueResp(){
    }
    
    public SessionDifferenceQueueResp(ResultCode rc){
        super(rc);
    }

    public SessionDifferenceQueueResp(Session session){
        super(ResultCode.OK);
        SessionData sessionData = session.getSessionData();
        ID = sessionData.getID();
        MSISDNFake = sessionData.getMSISDN_FAKE();
        ICCID = sessionData.getICCID();
        enqueueEvents = session.getEventCounter().getCounter();
        eventCounter = session.getEventQueueSize();
    }
}
