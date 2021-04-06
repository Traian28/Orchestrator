/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ats_connection.asa.orchestrator.response;

/**
 *
 * @author dmarcogliese
 */
public enum ResultCode {
    OK(0, "OK"),
    UNKNOWN_ERROR(1, "Unknown Error"),
    STATE_MACHINE_ERROR(2, "Could not instantiate State Machine Executor"),
    SESSION_NOT_FOUND(3, "Session not found"),
    NOT_SESSION_ID(4, "Session ID null"),
    NOT_OPERATION_NAME(5, "Operation Name null"),
    CAN_NOT_CREATE_SESSION(6, "Could not create session to evolve"),
    CAN_NOT_EVOLVE_SESSION(7, "Could not evolve session"),
    CAN_NOT_RELOAD_POST_EVENT(8, "Could not refresh Post Event Configuration Data"),
    CAN_NOT_RELOAD_SERVER_CONF(9, "Could not refresh Server Configuration"),
    SEARCH_CRITERIA_NOT_DEFINED(10, "Session Search Criteria not defined"),
    INVALID_DATE_FORMAT(11, "Error parsing Date"),
    INVALID_STATE(12, "Specified State invalid"),
    CAN_NOT_RELOAD_CONFIG(13, "Can not reload configuration"),
    SERVER_LOGGER_ERROR(14, "Could not instantiate  Server Logger"),
    ACCESS_DATA_ERROR(15, "Error accessing data"),
    POST_EVENT_CONF_ERROR(16, "Could not instantiate Post Event Configuration");

    private int value;
    private String description;

    private ResultCode(int value, String description){
        this.value = value;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public int getValue() {
        return value;
    }
}

