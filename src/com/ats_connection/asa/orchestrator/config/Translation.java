/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ats_connection.asa.orchestrator.config;
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 *
 * @author Kimi
 */
public class Translation {

    private Properties properties;    
    private String propfile = "";
    
    public Translation(String filename) {
        
        // open Property file
        propfile = filename;
        properties = new Properties();
        
        try {
       
            InputStream ins = (Translation.class).getResourceAsStream("/" +propfile);
                        
            if (ins!=null) 
                properties.load(ins);
    
    
        } catch (IOException e) {
            //e.printStackTrace();
        }
    }
    
    public String getString(String string) {
        
        // get String from Property file        
        String msg = properties.getProperty(string);
        if (msg == null)
            msg = "Translation not found";
        return msg;
    }

}
