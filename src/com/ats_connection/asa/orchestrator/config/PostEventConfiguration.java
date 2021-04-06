/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ats_connection.asa.orchestrator.config;

import com.ats_connection.asa.orchestrator.helper.LogHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Clase para Mantener en memoria la configuración del flujo del orquestador
 * Levanta los datos de la tabla ASA_ORCHESTRATOR_FLOW
 * @author pdiaz
 * 
 */
public class PostEventConfiguration {
   
	/**
	 * Datos para la vista de la configuración
	 *  public String oper; 
	 *  public String resp;
	 *  public String flag;
	 *  public String orig;
	 *  
	 *  public String nxev;
	 *  public String nxop;
	 *  public String nxpar;
	 *
	 */
	private class ConfigReg {
		public String oper; 
		public String resp;
		public String flag;
		public String orig;
		
		public String nxev;
		public String nxop;
		public String nxpar;
		
	}
	
    /** Mapa para mantener en memoria los datos de configuración del flow del orquestador */
    private Map<String, EventCall> config = null;
    private Map<String, ArrayList<ConfigReg>> configView = null;
    private String configTreeView = null;
    
    /** 
     * Separador de elementos dentro de la clave unica del hash.  
     * Es un valor que no se utilice como contenido de una de las claves  
     */
    public static String sepChar = "~";
    
    static private PostEventConfiguration postEventConfiguration=null;
        
    private PostEventConfiguration() throws Exception {
    	loadConfig();
    }

    static public PostEventConfiguration getInstance() throws Exception
    {
        if (postEventConfiguration==null) {
            postEventConfiguration=new PostEventConfiguration();
        }

        return postEventConfiguration;
    }

    public String getConfigTreeView() {
    	return configTreeView;
    }
    
    public Map<String, EventCall> getConfig() {
    	return config;
    }
    
    /**
     * Realiza la lectura de la base de datos y carga en el Map los datos del flow desde la tabla ASA_ORCHESTRATOR_FLOW
     * @throws java.lang.Exception
     */
    private void loadConfig() throws Exception {
        if (config == null) {            
            configTreeView = null;
            configView = new HashMap<String, ArrayList<ConfigReg>>();

            LogHelper.webLog.info("ASA - PostEventConfiguration - loadConfig - Initializing");
            
            // Obtener datos de la Base de Datos
            config = new HashMap<String, EventCall>();

            //String sqlQuery = "select eer_operation_name, eer_return_code, eer_ses_flag, eer_evt_name, eer_evt_params"; 
            //sqlQuery += ", eer_next_operation_name";
            //sqlQuery += " from ASA_ORCHESTRATOR_FLOW";
            //sqlQuery += " order by eer_operation_name, eer_return_code"; 
            
            String sqlQuery = ServerConfiguration.getInstance().get("query_error_codes"); 
            
            LogHelper.webLog.info("ASA - PostEventConfiguration - loadConfig - Executing query");
            PreparedStatement stmt= null;
            Connection conn= null;
            ResultSet rs = null;

            String eer_operation_name;
            String eer_return_code;
            String eer_ses_origin;
            String eer_evt_name;
            String eer_evt_params;
            String eer_next_operation_name;
            
            int eer_ses_flag;
            long recordCount=0;
            
            try {
            	conn = ConnectionManager.getInstance().getConnection();
            	stmt = conn.prepareStatement(sqlQuery);
                rs     = stmt.executeQuery();

                // Popular hashmap
                LogHelper.webLog.info("ASA - PostEventConfiguration - loadConfig - Query executed.");

                if (rs != null) {
                    while (rs.next()) {
                        recordCount++;
                        // Clave del HashMap
                        eer_operation_name = rs.getString("eer_operation_name");
                        eer_return_code = rs.getString("eer_return_code");
                        eer_ses_flag = rs.getInt("eer_ses_flag");
                        eer_ses_origin = rs.getString("eer_origin_status");  // Agregado

                        // Data
                        eer_evt_name = rs.getString("eer_evt_name");
                        eer_evt_params = rs.getString("eer_evt_params");
                        eer_next_operation_name = rs.getString("eer_next_operation_name");

                        config.put(eer_operation_name + sepChar + eer_return_code + sepChar + eer_ses_flag + sepChar + eer_ses_origin, 
                        		new EventCall(eer_evt_name, eer_evt_params, eer_next_operation_name));

                        // View
                        // Actualiza el Map que servirá para controlar la configuración y mostrar
                        // con el toHTML
                        ConfigReg regView = new ConfigReg();
                        
                        regView.oper=eer_operation_name;
                        regView.resp=eer_return_code;
                        regView.flag=new Integer(eer_ses_flag).toString();
                        regView.orig=eer_ses_origin;
                        regView.nxev=eer_evt_name;
                        regView.nxop=eer_next_operation_name;
                        regView.nxpar=eer_evt_params;
                        
                        //Insert en map con Lista para permitir duplicados
                        // Crea el mapa con clave: oper~orig
                        if ( ! configView.containsKey( regView.oper + sepChar + regView.orig ) ) {
                        	ArrayList<ConfigReg> list = new ArrayList<ConfigReg>( );
                            list.add( regView);
                            configView.put( regView.oper + sepChar + regView.orig, list);
                        }
                        else {
                        	ArrayList<ConfigReg> list = (ArrayList<ConfigReg>)configView.get( regView.oper + sepChar + regView.orig);
                            list.add( regView );
                        }
                    }

                    LogHelper.webLog.info("ASA - PostEventConfiguration - loadConfig - Query executed RecordCount:" + recordCount);
                }
                
                if (recordCount==0) {
                    LogHelper.webLog.error("ASA - PostEventConfiguration - loadConfig - SMErrorCodes Table Empty!");
                }  
                
                LogHelper.webLog.info("ASA - PostEventConfiguration - loadConfig - Generating html");
                configTreeView = toHTML();
            }
            catch(SQLException e) {
                    LogHelper.webLog.error("ASA - PostEventConfiguration - loadConfig - Exception: " + e.getLocalizedMessage(), e);
            }
            finally {
            	ConnectionManager.getInstance().closeConnection(conn, stmt, rs);
            	LogHelper.webLog.debug("ASA - PostEventConfiguration - loadConfig - DB Connection Released.");
            }
        }
    }
    
    public String getXMLleaf(String orig, String oper) {
    	String resp = "<NULL />";
    	
    	ArrayList<ConfigReg> list = (ArrayList<ConfigReg>) configView.get( oper + sepChar + orig);
        if (list==null)
            list = (ArrayList<ConfigReg>) configView.get( oper + sepChar + "*");
        
        if (list!=null) {
            resp = "<POST_EVENT_OPTIONS>";

            for (ConfigReg cr : list) {
                    resp += "<POST_EVENT_OPTION>";
                    
                    resp += "<FLAG>"+cr.flag+"</FLAG>";
                    resp += "<NXEV>"+cr.nxev+"</NXEV>";
                    resp += "<NXOP>"+cr.nxop+"</NXOP>";
                    resp += "<NXPAR>"+cr.nxpar+"</NXPAR>";
                    resp += "<OPER>"+cr.oper+"</OPER>";
                    resp += "<ORIG>"+cr.orig+"</ORIG>";
                    resp += "<RESP>"+cr.resp+"</RESP>";
                    
                    resp += "</POST_EVENT_OPTION>";
            }
            
            resp += "</POST_EVENT_OPTIONS>";
        }
        
        return resp;
    }
    
    
    /**
     * Fuerza la lectura de los datos desde la base.
     * @throws java.lang.Exception
     */
    public void refreshData() throws Exception 
    {
    	config.clear();
    	configView.clear();
        config = null;
        configView = null;
        
        loadConfig();
    }
    
    /**
     * Recibe como parámetros las 3 partes de la clave de búsqueda
     * @param opName  Nombre de la operación
     * @param retCode Codigo de respuesta (* para default)
     * @param controlFlag Flag de control (-1 para default)
     * @return Devuelve los datos de configuración del orquestador para contiunar con el flujo a partir de los datos de entrada
     */
    public EventCall getEventCall(String opName, String retCode, int controlFlag, String origin)
    {
        LogHelper.webLog.debug("ASA - PostEventConfiguration - getEventCall: Start: search=" + opName + sepChar + retCode + sepChar + controlFlag);

        int retry=1;
        
        while (retry>=0)
        {
            if (config==null)
            {
                LogHelper.webLog.info("ASA - PostEventConfiguration - getEventCall: Reloading data");
                try
                {
                    refreshData();
                    LogHelper.webLog.info("ASA - PostEventConfiguration - getEventCall: Reloading data Ok.");
                }
                catch(Exception e) 
                {
                    LogHelper.webLog.error("ASA - PostEventConfiguration - getEventCall: Reloading data Exception:" + e.getMessage(), e);
                }   
            }
            
            if (config!=null)
            {
                //
                // O C F R
                // O C F *
                // O C * R
                // O * F R
                // O C * *
                // O * * R
                // O * F *
                // O * * *
                // 
                
                // Política de búsqueda
                // 1) busca =operation, =returnCode, =controlFlag, =origin
                // 2) busca =operation, =returnCode, =controlFlag, cualquier origin
                // 2) busca =operation, =returnCode, cualquier controlFlag, cualquier origin
                // 3) busca =operation, cualquier returnCode, =controlFlag
                // 4) busca =operation, cualquier returnCode, cualquier controlFlag
                //
                
                EventCall rspEvt;
                rspEvt = (EventCall) config.get(opName + sepChar + 
                                                retCode + sepChar + 
                                                controlFlag + sepChar +
                                                origin ); // todos igual

                if (rspEvt==null)
                {
                    rspEvt = (EventCall) config.get(opName + sepChar + retCode + sepChar + new Integer(-1) + sepChar + origin); // Cualquier flag / prioriza origin
                    if (rspEvt==null)
                    {
                        rspEvt = (EventCall) config.get(opName + sepChar + "*" + sepChar + controlFlag + sepChar + origin); // Cualquier respCode
                        if (rspEvt==null)
                        {
                            rspEvt = (EventCall) config.get(opName + sepChar + "*" + sepChar + new Integer(-1) + sepChar + origin); // Cualquier flag y cualquier respCode
                            if (rspEvt==null)
                            {
                                rspEvt = (EventCall) config.get(opName + sepChar + retCode + sepChar + controlFlag + sepChar + "*"); // Cualquier origin
                                if (rspEvt==null)
                                {
                                    rspEvt = (EventCall) config.get(opName + sepChar + retCode + sepChar + new Integer(-1) + sepChar + "*"); // Cualquier flag / prioriza origin
                                    if (rspEvt==null)
                                    {
                                    rspEvt = (EventCall) config.get(opName + sepChar + "*" + sepChar + controlFlag + sepChar + "*"); // Cualquier respCode y cualquier origin
                                        if (rspEvt==null)
                                        {
                                            rspEvt = (EventCall) config.get(opName + sepChar + "*" + sepChar + new Integer(-1) + sepChar + "*"); // Cualquier flag, cualquier origin y cualquier respCode
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                return rspEvt; // busqueda completa exitosa, devuelvo valor
            }
            else    // No hay datos. Posibles problemas con la base de datos.
            {
                LogHelper.webLog.fatal("ASA - PostEventConfiguration - getEventCall: No configuration loaded.");
                ServerConfiguration.getInstance().newLogEntry("ASA - PostEventConfiguration - getEventCall: No configuration loaded.");
                retry=-1;  // En esta condición no realiza mas reintentos.
            }
            
        }
        return null;
        
    }
       
    /**
     * Devuelve una cadena con los datos de configuración
     * @return
     */
    @Override
    public String toString() {
        if (config == null) return null;
        
        if (config.isEmpty()) return "Empty";
        
        String retValue="";
        
        for(Map.Entry<String,	EventCall> entry : config.entrySet()) {
        	EventCall ec = entry.getValue();
        	retValue += entry.getKey().toString() + " = "
        			+ " eventName:" + ec.evt_name
        			+ " eventParams:" + ec.evt_params
        			+ " nextOperation:" + ec.next_operation_id
        			+ "?";
        }
        
        return retValue;
    }
    
    /**
     * Devuelve en formateo String la información de un Nodo (operación) para todos sus
     * códigos de retorno y Flags
     * 
     * @param nodeName
     * @return
     */
    public List<HashMap<String, String>> getNodes(String order, String nodeName){
        
        List<ConfigReg> list = null; 
        List<HashMap<String, String>> nodes = new ArrayList<HashMap<String, String>>();
                
        if (nodeName!=null && !nodeName.isEmpty() && !nodeName.contentEquals("/")) {
                
            list = (ArrayList<ConfigReg>) configView.get( nodeName + sepChar + "");
            if (list==null)
                 list = (ArrayList<ConfigReg>) configView.get( nodeName + sepChar + "*");

            if (list!=null) {
            	for(ConfigReg cr : list) {
                        HashMap<String, String> reg = new HashMap<String, String>();
                        reg.put("oper", cr.oper); 
                        reg.put("resp", cr.resp); 
                        reg.put("flag", cr.flag); 
                        reg.put("orig", cr.orig); 
                        reg.put("nxev", cr.nxev); 
                        reg.put("nxop", cr.nxop); 
                        reg.put("nxpar", cr.nxpar);
                        nodes.add(reg);
            	}
            }
        }
            
        return nodes;
    }
    
    
    
    /**
     * Devuelve los datos de configuración en formato HTML
     * Si el html ya está generado, lo devuelve directamente sin regenerarlo.
     * 
     * @return
     */
    public String toHTML(){
        return "NOT AVAILABLE";
    }
    
}
