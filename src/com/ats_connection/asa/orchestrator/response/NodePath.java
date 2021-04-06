/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ats_connection.asa.orchestrator.response;

import java.util.Calendar;

/**
 *
 * @author dmarcogliese
 */
public class NodePath {

    public int order;
    public String event = null;
    public String state = null;
    public Calendar calendar = null;
    public Long duration = null;

    public NodePath() {
    }

    public NodePath(int order, String event, String state, Calendar calendar, Long duration) {
        this.order = order;
        this.event = event;
        this.state = state;
        this.calendar = calendar;
        this.duration = duration;
    }

    public String getLeft() {
        return event;
    }

    public String getRight() {
        return state;
    }

    public Calendar getCalendar() {
        return calendar;
    }

    public Long getDuration() {
        return duration;
    }
}
