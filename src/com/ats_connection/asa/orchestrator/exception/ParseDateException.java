/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ats_connection.asa.orchestrator.exception;

import java.text.ParseException;

/**
 *
 * @author dmarcogliese
 */
public class ParseDateException extends Exception {
    
    public ParseDateException(ParseException ex){
        super(ex);
    }

}
