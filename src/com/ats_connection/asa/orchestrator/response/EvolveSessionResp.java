/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ats_connection.asa.orchestrator.response;

/**
 *
 * @author dmarcogliese
 */
public class EvolveSessionResp extends Response {
    
    public EvolveSessionResp() {
    }
 
    public EvolveSessionResp(ResultCode rc) {
        super(rc);
    }

    public EvolveSessionResp(int code, String description ) {
        resultCode = code;
        resultDescription = description;
    }

}
