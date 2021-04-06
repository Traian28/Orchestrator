/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ats_connection.asa.orchestrator.config;

/**
 *
 * @author dmarcogliese
 */
//public class Event implements CycleRecoverable {
public class Event {
    
    private String key;
    public String operation;
    public String returnCode;
    public int flag;
    public String name;
    public String parameters;
    public String nextOperation;
    
    //public ArrayList<Event> events = null;
    
    public Event(){
    }
    
    public Event(String key, String name, String parameters, String nextOperation){
        
        this.key = key;                    

        String[] keySplitted = key.split("~");
        for (int i = 0; i < keySplitted.length; i++) {
            switch(i){
                case 0:
                    operation = keySplitted[i];
                    break;
                case 1:
                    returnCode = keySplitted[i];
                    break;
                case 2:
                    flag = Integer.parseInt(keySplitted[i]);
                    break;
            }
        }

        this.name = name;                  
        this.parameters = parameters;      
        this.nextOperation = nextOperation;
        
    }

//    @Override
//    public Object onCycleDetected(Context arg0) {
//      //  throw new UnsupportedOperationException("Not supported yet.");
//        Event evt = new Event(this.key,this.name,this.parameters,this.nextOperation);
//        //this.events = null;
//        return evt;
//    }

}
