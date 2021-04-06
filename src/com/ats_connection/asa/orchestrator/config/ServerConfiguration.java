/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ats_connection.asa.orchestrator.config;

import com.ats_connection.asa.orchestrator.helper.LogHelper;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.Iterator;
import java.util.Properties;


/**
 * <Singleton>
 * Esta clase se utiliza para leer la configuración del archivo y para manejar el estado del orquestador
 * @author pdiaz
 */
public class ServerConfiguration {

        static private ServerConfiguration serverConfiguration=null;
        static private String systemErrorsLog=null;
        
        private Integer serverStatus = 0;
        private String configFile = "/mcom/orchestrator/cfg/server.properties";
        private Properties props = null;
        
        private String serverId = null;

        private ServerConfiguration()
        {
        	loadConfig();
        }

        static public ServerConfiguration getInstance() {
            if (serverConfiguration == null) {
                serverConfiguration = new ServerConfiguration();
            }

            return serverConfiguration;
        }
        
        
        /**
         * Carga las propiedades del archivo server.properties
         */
        private void loadConfig()
        {
            if (props==null || serverId==null)  // ServerId es el valor requerido del file de configuración
            {
                try{
                
                props = new Properties();            

                //
                // Carga desde Archivo
                //
                    LogHelper.webLog.info("ServerConfiguration: Loading file..."  + configFile);
                    InputStream ins=null;
                    try {
                    	ins = new FileInputStream(configFile);
                        //ins = (ServerConfiguration.class).getResource("/" + configFile).openStream();
                    } catch (IOException ex) {
                        LogHelper.webLog.error("ServerConfiguration: File not Found or error. (" + configFile + ") Exception:"+ ex, ex);
                    }

                    if (ins==null)
                        LogHelper.webLog.error("ServerConfiguration: File not Found. (" + configFile + ")");
                    else                
                        LogHelper.webLog.info("ServerConfiguration: File Loaded");

                    try{
                        props.load(ins);
                        ins.close();
                        if ((props).get("serverId")==null) 
                        {   
                            LogHelper.webLog.error("ServerConfiguration: serverId not Found.");
                            throw new Exception();
                        }
                    }
                    catch(Exception e)
                    {
                        props=null;
                        LogHelper.webLog.error("ServerConfiguration: Exception:" + e.getLocalizedMessage(), e);
                        return;    
                    }

                //
                // Carga desde DB
                //    
                    
                    String sqlQuery="select par_name, par_value from comm_parameters where par_app_name='ASA' and par_module_name='ORCH' order by par_name";
                    
                    LogHelper.webLog.info("ASA - ServerConfiguration DB - loadConfig - Executing query "+sqlQuery);
                    PreparedStatement stmt= null;
                    Connection conn= null;
                    ResultSet rs = null;

                    String cfg_parname;
                    String cfg_parvalue;

                    long recordCount=0;

                    try {
                    	conn = ConnectionManager.getInstance().getConnection();
                    	stmt = conn.prepareStatement(sqlQuery);
                        rs     = stmt.executeQuery();

                        // Popular hashmap
                        LogHelper.webLog.info("ASA - ServerConfiguration DB  - loadConfig - Query executed.");

                        if (rs!=null) {
                            while (rs.next()) {
                                recordCount++;
                                // Clave del HashMap
                                cfg_parname = rs.getString("par_name");
                                cfg_parvalue = rs.getString("par_value");

                                props.setProperty(cfg_parname, cfg_parvalue);
                            }

                            LogHelper.webLog.info("ASA - ServerConfiguration DB  - loadConfig - Query executed RecordCount:" + recordCount);

                        }
                        
                        if (recordCount==0) {
                            LogHelper.webLog.error("ASA - ServerConfiguration DB  - loadConfig - SMErrorCodes Table Empty!");
                        }  

                        LogHelper.webLog.info("ASA - ServerConfiguration DB  - loadConfig - Generating html");
                    }
                    catch(SQLException e) {
                            LogHelper.webLog.error("ASA - ServerConfiguration DB  - loadConfig - Exception: " + e.getLocalizedMessage(), e);
                            LogHelper.webLog.debug("ASA - ServerConfiguration DB  - loadConfig - DB Connection Released.");
                    }
                    finally {
                    	ConnectionManager.getInstance().closeConnection(conn, stmt, rs);
                    }
                }
                catch(Exception ex)
                {
                    LogHelper.webLog.error("ASA - ServerConfiguration DB  - loadConfig - Exception2: " + ex, ex);
                }
            }   

        }
        
        /**
         * Obtiene el valor de un parámetro recibiendo el nombre de parámetro como clave de búsqueda
         * @param paramName Nombre del parámetro a buscar
         * @return
         */
        public String get (String paramName)
        {
            if (props!=null)
                return props.getProperty(paramName);
            else
                return null;
        }

        /**
         * Obtiene una lista con las claves de los parametros del server.
         * @return
         */
        public Properties getProperties() {
            if (props!=null)
                return props;
            else
                return null;
        }
        
        
        /**
         * Obtiene el Id del servidor 
         * Es similar al get salvo que no recibe nombre de parámetro a buscar, sino que busca el parámetro denominado 'serverId'
         * @return
         */
        public String getServerId ()
        {
            if (props!=null)
                return props.getProperty("serverId");
            else
                return null;
        }

        /**
         * Setea la variable serverStatus con 1
         */
        public void serverPause (){
            serverStatus=1;  // Paused
        }
        
        /**
         * Setea la variable serverStatus con 0
         */
        public void serverResume () {
            serverStatus=0;  // Started
        }
        
        /**
         * Devuelve true si serverStatus es 0
         * @return
         */
        public boolean serverRunning(){
            if (serverStatus==0) return true;
            return false;
        }
        
        /**
         * Recarga la configuración del archivo server.properties
         */
        public void reload(){
            props=null;
            loadConfig();
        }
        
        
        public String toHtml(){
            
            String retVal = new String();
            
            retVal+="<TABLE>";
            if (props!=null){
                Iterator itr = props.entrySet().iterator();

                while (itr.hasNext()){
                    retVal+="<TR><TD>"+itr.next().toString()+"</TD></TR>";
                }
            }
            else{
                retVal += "<TR><TD>"+"Properties could not be loaded from server.properties"+"</TD></TR>";
            }
            retVal+="</TABLE>";
             
            return retVal;
        }
        
        public void newLogEntry(String log)
        {
            int logLength=(int) (1024 * 6);
            String serverLogDate = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date().getTime());
            
            if (systemErrorsLog==null)
            {
                systemErrorsLog="";
            }
            
            systemErrorsLog += (serverLogDate + " - " + log);
            
            String callTrace="";
            StackTraceElement[] elements = Thread.currentThread().getStackTrace();
            for(int i=0; i<elements.length && i<10; i++) 
            {             
        	if (i>1)
                    callTrace += ("\\n  - --- " + elements[i]);
            }        
            systemErrorsLog +=  callTrace   + "\\n\\n";
            
            if (systemErrorsLog.length()>logLength)
            {
                systemErrorsLog = "..." + systemErrorsLog.substring( systemErrorsLog.length()-(logLength) );
            }
            
        }
            
        public String getErrorLog()
        {
            return systemErrorsLog;
        }
           
}
