/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ats_connection.asa.orchestrator.response;

/**
 *
 * @author pdiaz
 */
public class StartSessionRsp {
    public int resultCode;
    public String resultDescription;
    public int status;
    public String transferNumber;
    public String ddd1;
    public String ddd2;
    public String ddd3;
    public String ddd4;
    public String ddd5;
    public int dddQuantity;
    public String sessionId;
    public int errorCode;
    public int ddd_dtmf_min;
    public int ddd_dtmf_max;
    public int max_retries;
}
