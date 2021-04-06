/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ats_connection.asa.orchestrator.response;

import com.ats_connection.asa.orchestrator.sme.Session.SessionData;
import com.ats_connection.asa.orchestrator.sme.Session.SessionNodePath;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import org.apache.log4j.Logger;

/**
 *
 * @author dmarcogliese
 */
public class SessionDetailsResp extends Response {
    
    private static org.apache.log4j.Logger logger = Logger.getLogger(SessionDetailsResp.class);

    public String ID;
    public Integer transactionID;
    public String activationType;
    public String areaCode;
    public String ICCID;
    public String IMEI;
    public String IMSI;
    public String IMSIFake;
    public String IMSIRoaming;
    public String LOCI;
    public String MSISDN;
    public String MSISDNFake;
    public String orchestratorID;
    public String stateMachineStatusError;
    public String stateMachineState;
    public String originActivation;
    public Calendar updateDate;
    public Calendar creationDate;
    public Boolean isActivated;
    public Boolean isFinished;
    public HashMap <String, String> auxiliarData;
    public HashMap <String, Integer> internalFlags;
    public ArrayList <NodePath> nodesPath = new ArrayList<NodePath>(); 
  //LTE
    public String origin;
    public String userType;
    public String opc;
    public long   virtualNetwork;
    public long   subApplication;
    public String cardType;
    public String operatorKey;
    
    
    
    public SessionDetailsResp() {
    }
    
    public SessionDetailsResp(ResultCode rc) {
        super(rc);
    }    
    
    public SessionDetailsResp(SessionData sessionData) {
        super(ResultCode.OK);
        ID = sessionData.getID();
        transactionID = sessionData.getTransactionID();
        activationType = sessionData.getActivationType();
        areaCode = sessionData.getAreaCode();
        ICCID = sessionData.getICCID();
        IMEI = sessionData.getIMEI();
        IMSI = sessionData.getIMSI();
        IMSIFake = sessionData.getIMSI_FAKE();
        IMSIRoaming = sessionData.getIMSI_ROAMING();
        LOCI = sessionData.getLOCI();
        MSISDN = sessionData.getMSISDN();
        MSISDNFake = sessionData.getMSISDN_FAKE();
        orchestratorID = sessionData.getOrchestratorID();
        stateMachineStatusError = sessionData.getStateMachineStatusError();
        stateMachineState = sessionData.getStateMachineState().getName();
        originActivation = sessionData.getOriginActivation().getName();
        updateDate = sessionData.getUpdateDate();
        creationDate = sessionData.getCreationDate();
        isActivated = sessionData.isActivated();
        isFinished = sessionData.isFinished();
        auxiliarData = sessionData.getAuxiliarData();
        internalFlags = sessionData.getInternalFlags();
        //LTE PAC
        origin=sessionData.getOrigin();
        userType=sessionData.getUserType();
        opc=sessionData.getOpc();
        virtualNetwork=sessionData.getVirtualNetwork();
        subApplication=sessionData.getApplication();
        cardType=sessionData.getCardType();
        operatorKey=sessionData.getOpKey();
        
        setTrace(sessionData.getSessionPath());
    }
    
    private void setTrace(ArrayList <SessionNodePath> nodePaths){
        logger.debug("SessionResp - Setting trace");
        for (SessionNodePath nodePath : nodePaths) {    
            String event = (nodePath.getEventTag() != null) ? nodePath.getEventTag().getName() : null;
            String state  = (nodePath.getStateTag() != null) ? nodePath.getStateTag().getName() : null;
            NodePath node = new NodePath(nodesPath.size()+1, event, state, nodePath.getCalendar(), nodePath.getDuration());
            nodesPath.add(node);
        }
    }

}
