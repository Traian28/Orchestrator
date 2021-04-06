/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ats_connection.asa.orchestrator.response;

import com.ats_connection.asa.orchestrator.sme.Session;
import com.ats_connection.asa.orchestrator.sme.Session.SessionData;
import java.util.Date;

/**
 *
 * @author dmarcogliese
 */
public class RunningSessionResp extends Response{
    
    public String ID;
    public String status;
    public Date lastEnqueueDate;
    public Date lastExecuteDate;
    public int enqueuedEvents;
    public int eventCounter;
    
    public  RunningSessionResp(){
    }
    
    public RunningSessionResp(ResultCode rc){
        super(rc);
    }
    
    public  RunningSessionResp(Session session){
        super(ResultCode.OK);
        SessionData sessionData = session.getSessionData();
        ID = sessionData.getID();
        status = sessionData.getStateMachineState().getName();
        lastEnqueueDate = session.getLastDateEnqueueInThreadPool().getTime();
        lastExecuteDate = session.getLastDateExecuteInThreadPool().getTime();
        enqueuedEvents = session.getEventCounter().getCounter();
        eventCounter = session.getEventQueueSize();
    }

}
