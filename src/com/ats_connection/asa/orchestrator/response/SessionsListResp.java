/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ats_connection.asa.orchestrator.response;

import com.ats_connection.asa.orchestrator.config.ServerConfiguration;
import com.ats_connection.asa.orchestrator.sme.Session.SessionData;

import java.util.ArrayList;

import org.apache.log4j.Logger;

/**
 *
 * @author dmarcogliese
 */
public class SessionsListResp extends Response {

    private static org.apache.log4j.Logger logger = Logger.getLogger(SessionsListResp.class);
    private static final int LIMITSESSIONSLISTDEFAULT = 100;
    
    public int quantity;
    public boolean hasMoreSessions = false;
    public ArrayList <SessionItemResp> sessions = new ArrayList<SessionItemResp>();

    public SessionsListResp() {
        
    }

    public SessionsListResp(ResultCode rc) {
        super(rc);
    }
            
    public SessionsListResp(ArrayList<SessionData> ses){
        super(ResultCode.OK);
        logger.debug("SessionsListResp - Adding sessions");
        
        int maxim;
        try {
            maxim = new Integer(ServerConfiguration.getInstance().get("api_limit_sessions_list")).intValue();
        } catch (Exception e) {
            maxim = LIMITSESSIONSLISTDEFAULT;
        }
        int limit; 
        if (ses.size() <= maxim) {
            limit = ses.size();
        } else {
            limit = maxim;
            hasMoreSessions = true;
        }

        for (int i = 0; i < limit; i++) {
            SessionItemResp sesItem = new SessionItemResp(ses.get(i));
            sessions.add(sesItem);
        }
        quantity = sessions.size();
    }

}