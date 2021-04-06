/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ats_connection.asa.orchestrator.service;

import org.apache.log4j.Logger;

import com.ats_connection.asa.orchestrator.config.PostEventConfiguration;
import com.ats_connection.asa.orchestrator.config.ServerConfiguration;
import com.ats_connection.asa.orchestrator.response.*;

import com.ats_connection.asa.orchestrator.sme.api.SMEFacade;
import com.ats_connection.asa.orchestrator.sme.SessionManager.SessionManagerExternState;

import java.util.HashMap;

import javax.jws.WebMethod;
import javax.jws.WebService;

/**
 *
 * @author dmarcogliese
 */
@WebService()
public class Maintenance {

    private static org.apache.log4j.Logger logger = Logger.getLogger(Maintenance.class);
  
    /**
     * Web service operation
     */
    @WebMethod(operationName = "reloadConfig")
    public Response reloadConfig() {

        Response response = new Response(ResultCode.OK);

        try {
            PostEventConfiguration.getInstance().refreshData();
        } catch (Exception e) {
            logger.error(e.getCause());
            logger.error(e.getMessage());
            response = new Response(ResultCode.CAN_NOT_RELOAD_POST_EVENT);
        }
        
        try {
            ServerConfiguration.getInstance().reload();
        } catch (Exception e) {
            logger.error(e.getCause());
            logger.error(e.getMessage());
            if (response.resultCode == ResultCode.OK.getValue()){
                response = new Response(ResultCode.CAN_NOT_RELOAD_SERVER_CONF);    
            } else {
                response = new Response(ResultCode.CAN_NOT_RELOAD_CONFIG);
            }
        }
        
        return response;
    }

    /**
     * Web service operation
     */
    @WebMethod(operationName = "enableNewSessions")
    public Response enableNewSessions() {
        
        SMEFacade stateMachine = SMEFacade.getInstance();
        
        if (stateMachine!=null){
            stateMachine.unblockNewActivations();
            return new Response(ResultCode.OK);
        } else {
            return new Response(ResultCode.STATE_MACHINE_ERROR);
        }

    }

    /**
     * Web service operation
     */
    @WebMethod(operationName = "disableNewSessions")
    public Response disableNewSessions() {
        SMEFacade stateMachine = SMEFacade.getInstance();
        if (stateMachine!=null){
            stateMachine.blockNewActivations();
            return new Response(ResultCode.OK);
        } else {
            return new Response(ResultCode.STATE_MACHINE_ERROR);
        }
    }

    /**
     * Web service operation
     */
    @WebMethod(operationName = "getOrchestratorStatus")
    public OrchestratorStatusResp getOrchestratorStatus() {

        OrchestratorStatusResp response = null;

        SMEFacade stateMachine = SMEFacade.getInstance();
        if (stateMachine!=null){
            
            int sessionCount = stateMachine.getSessionsCount().intValue();
            
            int processors = Runtime.getRuntime().availableProcessors();
            long freeMemory = Runtime.getRuntime().freeMemory();
            long maxMemory = Runtime.getRuntime().maxMemory();
            long totalMemory = Runtime.getRuntime().totalMemory();

            Boolean stateMachineEnable = null;
            if (SessionManagerExternState.UNBLOCKED.equals(stateMachine.getSessionManagerExternState())){
                stateMachineEnable = Boolean.TRUE;
            } else {
                stateMachineEnable = Boolean.FALSE;
            }
                     
            response = new OrchestratorStatusResp(sessionCount, processors, freeMemory, maxMemory, totalMemory, stateMachineEnable);
        } else {
            response = new OrchestratorStatusResp(ResultCode.STATE_MACHINE_ERROR);
        }

        return response;
    }
    
    /**
     * Web service operation
     */
    @WebMethod(operationName = "getServerConfiguration")
    public ServerConfigurationResp getServerConfiguration() {
        ServerConfiguration sc = ServerConfiguration.getInstance();
        
        HashMap<String,String> map = new HashMap<String,String>();
        for (String key : sc.getProperties().stringPropertyNames()) {
            map.put(key, sc.getProperties().getProperty(key));
        }

        return new ServerConfigurationResp(map);
    }

    /**
     * Web service operation
     */
    @WebMethod(operationName = "getPostEventConfiguration")
    public PostEventConfigurationResp getPostEventConfiguration() {
        //PostEventConfigurationResp response = null;
        PostEventConfiguration pEConfig = null;
        try {
            pEConfig = PostEventConfiguration.getInstance();
        } catch (Exception e) {
            logger.error(e.getCause());
            logger.error(e.getMessage());
            return new PostEventConfigurationResp(ResultCode.POST_EVENT_CONF_ERROR);
        }
            
            return new PostEventConfigurationResp(pEConfig.getConfig());
    }

}
