/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ats_connection.asa.orchestrator.helper;

import org.apache.log4j.Logger;

import com.ats_connection.asa.orchestrator.config.ServerConfiguration;

/**
 * LogHelper
 * @author pdiaz
 */
public class LogHelper {
    
    public static Logger webLog = Logger.getLogger(LogHelper.class);
    public  LogHelper() {

    }

    /**
     * dbLog
     */
    public static void dbLog (String sessionId, String msisdnFake, String logPriority, String category, String title, String message)
    {
        // Registro de Log en la base de datos
        webLog.debug("\ndbLog --> ----------------------------------- \n  SessionId="+sessionId+"\n  msisdnFake="+msisdnFake+"\n  logPriority="+logPriority+"\n  title="+title+"\n  message="+message+"\nend dbLog -----------------------------------");
        int resp=0;
        try{ 
//            resp = com.ats_connection.asa.server.logger.AsaServerLogger.getInstance().activityLog("Orchestrator", 
//            		title, message, 
//            		Integer.parseInt(ServerConfiguration.getInstance().get("LogLevel"+logPriority)), 
//            		category, null, sessionId, msisdnFake, null, null,null);
        }
        catch(Exception e) {            
            webLog.error("SessionService:---  - dbLog Exception:" + e, e);
        }
        webLog.debug("dbLog -->  RespCode="+resp);
    }

    /**
     * dbLogSdr
     */
    public static void dbLogSdr (String sessionId, String msisdnFake, String iccid, String category, String title, String message)
    {
        // Registro de Log en la base de datos
        webLog.debug("\ndbLog --> ----------------------------------- \n  SessionId="+sessionId+"\n  msisdnFake="+msisdnFake+"\n  title="+title+"\n  message="+message+"\nend dbLog -----------------------------------");
        int resp=0;
        try{ 
//            resp=com.ats_connection.asa.server.logger.AsaServerLogger.getInstance().sdrLog("Orchestrator", 
//                title, message, category, null, sessionId, msisdnFake, null, iccid, null);
        }
        catch(Exception e) {            
            webLog.error("SessionService:---  - dbLogSdr Exception:" + e, e);
        }
        webLog.debug("dbLogSdr -->  RespCode="+resp);
    
    }
    
    /**
     * dbLog Flow
     */
    public static void dbLogFlow (String sessionId, String msisdnFake, String logPriority, String category, String title, String message) {
        // Registro de Log en la base de datos
        webLog.debug("\ndbLog --> ----------------------------------- \n  SessionId="+sessionId+"\n  msisdnFake="+msisdnFake+"\n  logPriority="+logPriority+"\n  title="+title+"\n  message="+message+"\nend dbLog -----------------------------------");
        int resp = 0;
        try{ 
//            resp = com.ats_connection.asa.server.logger.AsaServerLogger.getInstance().activityLog(
//                "orch.Flow", title, message, 
//                Integer.parseInt(ServerConfiguration.getInstance().get("LogLevel"+logPriority)), 
//                category, null, sessionId, msisdnFake, null, null,null);
        }
        catch(Exception e) {            
            webLog.error("SessionService:---  - dbLog Exception:" + e, e);
        }
        webLog.debug("dbLog -->  RespCode="+resp);
    }

}
