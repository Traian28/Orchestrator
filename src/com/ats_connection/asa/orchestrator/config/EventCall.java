/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ats_connection.asa.orchestrator.config;

/**
 *
 * @author pdiaz
 */
public class EventCall {
    // Datos del evento
    public String evt_name;
    public String evt_params;
    public String next_operation_id;
    
    public EventCall(String s1, String s2, String s3)
    {
        evt_name=s1;
        evt_params=s2;
        next_operation_id=s3;
    }
}
