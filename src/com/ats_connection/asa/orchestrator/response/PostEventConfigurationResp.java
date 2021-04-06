/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ats_connection.asa.orchestrator.response;

import com.ats_connection.asa.orchestrator.config.Event;
import com.ats_connection.asa.orchestrator.config.EventCall;

import java.util.ArrayList;
import java.util.Map;

/**
 *
 * @author dmarcogliese
 */
public class PostEventConfigurationResp extends Response {
    
    public ArrayList<Event> events;

    public PostEventConfigurationResp(){
    }
    
    public PostEventConfigurationResp(ResultCode rc){
        super(rc);
    }

    public PostEventConfigurationResp(Map<String, EventCall> eventsConfig){
        if (eventsConfig.size() > 0) {
            events = new ArrayList<Event>();         

            for (String eventKey : eventsConfig.keySet()) {
                EventCall eventCall = eventsConfig.get(eventKey);
                Event event = new Event(eventKey, eventCall.evt_name, eventCall.evt_params, eventCall.next_operation_id);
                events.add(event);
            }

            this.setResultCode(ResultCode.OK);

        }

    }
}
