/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ats_connection.asa.orchestrator.response;

import java.util.HashMap;

/**
 *
 * @author dmarcogliese
 */
public class ServerConfigurationResp extends Response {

    public HashMap<String,String>parameters = null;
    
    public ServerConfigurationResp(){
    }
    
    public ServerConfigurationResp(ResultCode rc){
        super(rc);
    }

    public ServerConfigurationResp(HashMap<String,String> hm){
        super(ResultCode.OK);
        parameters = hm;
    }
    
}
