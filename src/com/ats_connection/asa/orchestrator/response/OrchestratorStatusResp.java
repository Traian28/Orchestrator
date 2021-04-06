/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ats_connection.asa.orchestrator.response;

/**
 *
 * @author dmarcogliese
 */
public class OrchestratorStatusResp extends Response {
    
    public int sessionCount;
    public int processors;
    public long freeMemory;
    public long maxMemory;
    public long totalMemory;
    public Boolean stateMachineEnable;
                    
    public OrchestratorStatusResp(){
    }
    
    public OrchestratorStatusResp(ResultCode rc){
        super(rc);
    }
    
    public OrchestratorStatusResp(int sessionCount, int processors, long freeMemory, long maxMemory, long totalMemory, Boolean enable){
        super(ResultCode.OK);
        this.sessionCount = sessionCount;
        this.processors = processors;
        this.freeMemory = freeMemory;
        this.maxMemory = maxMemory;
        this.totalMemory = totalMemory;
        this.stateMachineEnable = enable;
    }

}
