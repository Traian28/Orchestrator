/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ats_connection.asa.orchestrator.response;

import com.ats_connection.asa.orchestrator.sme.Session.SessionData;
import java.util.Calendar;
//import org.apache.log4j.Logger;

/**
 *
 * @author dmarcogliese
 */
public class SessionItemResp extends Response {
    
    //private static org.apache.log4j.Logger logger = Logger.getLogger(SessionItemResp.class); 

    public String ID;
    public String ICCID;
    public String IMSI;
    public String IMSIFake;
    public String MSISDN;
    public String MSISDNFake;
    public String stateMachineState;
    public Calendar creationDate;
    public Calendar updateDate;
    public String originActivation;
    public String stateMachineStatusError;
            
    public SessionItemResp() {
    }
    
    public SessionItemResp(ResultCode rc) {
        super(rc);
    }    
    
    public SessionItemResp(SessionData sessionData) {
        super(ResultCode.OK);
        ID = sessionData.getID();
        ICCID = sessionData.getICCID();
        IMSI = sessionData.getIMSI();
        IMSIFake = sessionData.getIMSI_FAKE();
        MSISDN = sessionData.getMSISDN();
        MSISDNFake = sessionData.getMSISDN_FAKE();
        stateMachineState = sessionData.getStateMachineState().getName();
        creationDate = sessionData.getCreationDate();
        updateDate  = sessionData.getChangeStateDate();
        originActivation = sessionData.getOriginActivation().getName();
        stateMachineStatusError = sessionData.getStateMachineStatusError();
    }

}
