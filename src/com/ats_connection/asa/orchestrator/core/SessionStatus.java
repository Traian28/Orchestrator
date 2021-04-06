/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ats_connection.asa.orchestrator.core;

import java.util.HashMap;

/**
 * Datos de la sesion
 * @author pdiaz
 */
public class SessionStatus {
	public String whereIs;
    public String sessionId;
    public String iccid;
    public String msisdn_t;
    public String imsi_t;
    public String msisdn_real;
    public String imsi_real;
    public String originActivation;
    public String sesStatus;
    public String sesStatusPhase;
    public String transactionId;
    public String errorStatus;
    public String areaCode;
    public String activationType;
    public String finished;
    public String loci;
    public String imsi_roaming;
    public String imei;
    public HashMap<String,String> auxiliarData;
    
    public String imsiRoaming;
    public String origin;
    public String userType;
    public String opc;
    public long   virtualNetwork;
    public long   subApplication;
    public String cardType;
    public String operatorKey;
    
    public String stateName;
    public String stateMachineName;
    public String stateMachineVersion;
}
