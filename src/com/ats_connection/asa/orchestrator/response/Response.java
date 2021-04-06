/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ats_connection.asa.orchestrator.response;

/**
 *
 * @author dmarcogliese
 */
public class Response {
    
    public int resultCode;
    public String resultDescription;
    
    public Response(){
    }
    
    public Response(ResultCode rc) {
        resultCode = rc.getValue();
        resultDescription = rc.getDescription();              
    }   

    protected void setResultCode(ResultCode rc){
        resultCode = rc.getValue();
        resultDescription = rc.getDescription();              
    }
}
