package com.ats_connection.asa.orchestrator.config;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import com.ats_connection.asa.orchestrator.helper.LogHelper;
import com.ats_connection.asa.orchestrator.sme.Session.SessionData;
import com.ats_connection.asa.orchestrator.core.SessionStatus;
import com.ats_connection.asa.orchestrator.config.Subscriber;
import com.ats_connection.jdbc.DBPool;
import com.thoughtworks.xstream.XStream;

/**
 * Creates a ConnectionManager
 */
public class ConnectionManager extends DBPool {
    
	private static ConnectionManager  instance = null;
	
    private ConnectionManager() {      
    }
    
    public static synchronized ConnectionManager getInstance() {
    	if (instance == null)
    		instance = new ConnectionManager();
    	
    	return instance;
    }
    
    /**
     * Devuelve un suscriptor
     * @param filterICCID
     * @param filterMSISDN_t
     * @param filterIMSI_t
     * @param filterMSISDN_d
     * @param filterIMSI_d
     * @return
     */
     public Subscriber getSubscriber( String filterICCID, String filterMSISDN_t, String filterIMSI_t, String filterMSISDN_d , String filterIMSI_d) {

        StringBuffer strBuffer = new StringBuffer();
        if (filterICCID != null && !filterICCID.isEmpty())
        	strBuffer.append("ICCID");
        if (filterMSISDN_t != null && !filterMSISDN_t.isEmpty()) {
        	if (strBuffer.length() > 0) strBuffer.append(" and ");
        	strBuffer.append("MSISDN_FAKE");
        }
        if (filterIMSI_t != null && !filterIMSI_t.isEmpty()) {
        	if (strBuffer.length() > 0) strBuffer.append(" and ");
        	strBuffer.append("IMSI_FAKE");
        }
        if (filterMSISDN_d != null && !filterMSISDN_d.isEmpty()) {
        	if (strBuffer.length() > 0) strBuffer.append(" and ");
        	strBuffer.append("MSISDN_REAL");
        }
        if (filterIMSI_d != null && !filterIMSI_d.isEmpty()) {
        	if (strBuffer.length() > 0) strBuffer.append(" and ");
        	strBuffer.append("IMSI_REAL");
        }
        LogHelper.webLog.debug("getSubscriber:Begin : filtering by -> "+strBuffer.toString() ); 

        Subscriber sb = null;
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        ServerConfiguration srvConf = ServerConfiguration.getInstance();

        try {
            LogHelper.webLog.debug("getSubscriber:getConnectionToDatabase()");
            conn = getConnection();

            String query = srvConf.get("query_all_subs");

            if (filterICCID != null)
                query += (" " + srvConf.get("query_all_subs_filter_iccid"));

            if (filterMSISDN_t != null)
                query += (" " + srvConf.get("query_all_subs_filter_msisdn_t"));

            if (filterIMSI_t != null)
                query += (" " + srvConf.get("query_all_subs_filter_imsi_t"));

            if (filterMSISDN_d != null)
                query += (" " + srvConf.get("query_all_subs_filter_msisdn_d"));

            if (filterIMSI_d != null)
                query += (" " + srvConf.get("query_all_subs_filter_imsi_d"));

            stmt = conn.prepareStatement(query);
            int indexParam=0;
            if (filterICCID != null)
                stmt.setString(++indexParam, filterICCID);

            if (filterMSISDN_t!=null)
                stmt.setString(++indexParam, filterMSISDN_t);

            if (filterIMSI_t!=null)
                stmt.setString(++indexParam, filterIMSI_t);

            if (filterMSISDN_d!=null)
                stmt.setString(++indexParam, filterMSISDN_d);

            if (filterIMSI_d != null)
                stmt.setString(++indexParam, filterIMSI_d);

            if (indexParam == 0) {
                LogHelper.webLog.debug("getSubscriber:Statement without filters");

                closeConnection(conn, stmt);
                return sb;
            }

            LogHelper.webLog.debug("getSubscriber:Statement: " + query);
            LogHelper.webLog.info("getSubscriber:Executing Query");

            rs = stmt.executeQuery();

            LogHelper.webLog.info("getSubscriber:Getting resultset");
            
            if (rs != null && rs.next()) {
                //Control de suscriptor Bloqueado
//                if (rs.getString("SUB_BLOCKED_STATUS").contentEquals("B")) {
//                    // suscriptor Bloqueado
//                    LogHelper.dbLogSdr ( null, filterMSISDN_t, filterICCID, "INFO","getSubscriber: Subscriber Blocked","SendDisplayText ( msisdnFake=" + filterMSISDN_t + ", Iccid=" + filterICCID + ")"); 
//                }
//                else {
                    sb = new Subscriber();
                    LogHelper.webLog.debug("getSubscriber:Next()");

                    sb.setICCID(rs.getString("SUB_ICCID"));
                    sb.setEpfCode(rs.getString("SUB_EPF_CODE"));
                    sb.setNetworkAccessMode(rs.getString("SUB_NETWORK_ACCESS_MODE"));
                    sb.setIMSI_t(rs.getString("SUB_IMSI_TMP"));
                    sb.setMSISDN_t(rs.getString("SUB_MSISDN_TMP"));
                    sb.setRetryCounter(rs.getInt("SUB_RETRY_COUNTER"));
                    sb.setAdd(rs.getTimestamp("SUB_ADD_UTC"));
                    sb.setIMEI(rs.getString("SUB_IMEI"));
                    sb.setCategory(rs.getInt("SUB_CATEGORY"));
                    sb.setBlockedStatus(rs.getString("SUB_BLOCKED_STATUS"));
                    sb.setIMSI_d(rs.getString("SUB_IMSI_REAL"));
                    sb.setMSISDN_d(rs.getString("SUB_MSISDN_REAL"));
                    sb.setPrevSstCode(rs.getString("SUB_PREV_SST_CODE"));
                    sb.setSstCode(rs.getString("SUB_SST_CODE"));
                    sb.setSstCodeChange(rs.getTimestamp("SUB_SST_CODE_CHANGE_UTC"));
                    sb.setTraceLevel(rs.getInt("SUB_TRACE_LEVEL"));
                    sb.setKi(rs.getString("SUB_ENCRYPTED_KI"));
                    sb.setTransportKey(rs.getString("SUB_TRANSPORT_KEY_INDEX"));
                    sb.setElectricalProfile(rs.getString("SUB_ELECTRICAL_PROFILE"));
                    sb.setChipBatch(rs.getInt("SUB_CHIP_BATCH"));
                    sb.setRechargeMix(rs.getString("SUB_RECHARGE_MIX"));
                    sb.setMasterKey(rs.getString("SUB_MASTER_KEY_SET"));
                    sb.setFilIdInsert(rs.getInt("SUB_FIL_ID_INSERT"));
                    sb.setFilIdUpdate(rs.getInt("SUB_FIL_ID_UPDATE"));
                    sb.setFilIdLockUnlock(rs.getInt("SUB_FIL_ID_LOCK_UNLOCK"));
                    sb.setPlnCode(rs.getString("SUB_PLN_CODE"));
                    sb.setPlcCode(rs.getString("SUB_PLC_CODE"));
                    sb.setNwtCode(rs.getString("SUB_NWT_CODE"));
                    sb.setLastUpdate(rs.getTimestamp("MODIFICATION_DATE_UTC"));
                    sb.setOrigin(rs.getString("SUB_ORIGIN"));
                    sb.setAreaCode(rs.getString("SUB_AREA_CODE"));
                    sb.setUserType(rs.getString("SUB_USER_TYPE"));
                    sb.setCardType(rs.getString("SUB_CARD_TYPE"));
                    sb.setOperatorKey(rs.getString("SUB_OPERATOR_KEY"));
                    sb.setOpc(rs.getString("SUB_OPC"));
                    sb.setVirtualNetwork(rs.getLong("SUB_VIRTUAL_NETWORK"));
                    sb.setSubApplication(rs.getLong("SUB_APPLICATION"));
                    sb.setActivationType(rs.getString("SUB_ACTIVATION_TYPE"));
                    sb.setExpiration(rs.getString("EXPIRATION"));
                    sb.setBatchStatus(rs.getString("STATUS"));
                    sb.setBatchBlocked(rs.getString("BLOCKED"));
                    
                    LogHelper.webLog.info("getSubscriber:Resultset ok: ICCD "+sb.getICCID());
//                }
            }
            else {
                LogHelper.webLog.error("getSubscriber:Resultset empty");
            }
        } 
        catch (SQLException e) {
            LogHelper.webLog.fatal("getSubscriber:Getting Resultset Exception " + e.getMessage(), e);

        } 
        finally {
        	closeConnection(conn, stmt, rs);
        }
        
         return sb;
     }
     
     /**
      * Verifica si una fecha determinada se encuentra dentro de un rango
      * horario predefinido en el sistema
      * @param date  Fecha a controlar
      * @param bandCode Código de rango horario
      * @param appName Nombre de la aplicación
      * @param modName Nombre del módulo
      * @return
      */
     public boolean isDateInBand(Date date, String bandCode, String appName, String modName)
     {
         Boolean response = null;
         Connection conn = null;
         CallableStatement stmt = null;
         
         LogHelper.webLog.debug(" IsDateInBand ");

         try {
             conn = getConnection();
             String query = "DECLARE vresp varchar2(1); BEGIN vresp:='F'; if ATS_COMMON_GRAL_BANDS.ISDATEINBAND ( ?, ?, ?, ?)=true then vResp:='T'; end if; ?:=vResp; END;";
             stmt = conn.prepareCall(query);
             
             LogHelper.webLog.debug(" IsDateInBand  query:" + query);
             
             // register input parameters
             java.sql.Time dt1 = new java.sql.Time(date.getTime());
             stmt.setTime(1, dt1);
             LogHelper.webLog.debug(" IsDateInBand  date set");
             stmt.setString(2, bandCode);
             LogHelper.webLog.debug(" IsDateInBand  bandCode set");
             stmt.setString(3, appName);
             LogHelper.webLog.debug(" IsDateInBand  appName set");
             stmt.setString(4, modName);
             LogHelper.webLog.debug(" IsDateInBand  modName set");
             // register the type of the out param - an Oracle specific type
             stmt.registerOutParameter(5, Types.VARCHAR);
             LogHelper.webLog.debug(" IsDateInBand  outParam set");

             
             // execute and retrieve the result set
             LogHelper.webLog.debug(" IsDateInBand  Executing query");
             stmt.execute();
             
             String str  = stmt.getString(5);
             if (str.contentEquals("T"))
                 response=true;
             else
                 response=false;
             //webLog.info("response is " + response);
             LogHelper.webLog.fatal(" IsDateInBand  Execution result=" + str);

         } catch (Exception e) {
             //webLog.fatal("SysDate Exception " + e.getMessage());
             LogHelper.webLog.fatal(" IsDateInBand  Exception " + e, e);
             response = false;
             
         } 
         finally {
        	 closeConnection(conn, stmt);
         }
         LogHelper.webLog.debug(" IsDateInBand  returning " + response);
         return response;
         
     }
     
     /**
      * Controla el codigo de area recibido, buscándolo en la base de datos
      * @param DDD Código de Area a controlar
      * @return 0: OK,  !=0 Not Ok
      */
     public int checkDDD(String DDD) {
        LogHelper.webLog.debug("checkDDD:Begin : DDD:" + DDD);

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int response;

        if (DDD == null) return 2;
        
        try {
            LogHelper.webLog.debug("checkDDD:getConnectionToDatabase()");

            String query = "select count(*) as QTTY from ASA_MSC_AREAS where LMS_AREA = ? ";
            conn = getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1 , DDD);

            LogHelper.webLog.debug("checkDDD:Statement:" + query);
            LogHelper.webLog.info("checkDDD:Executing Query");

            rs = stmt.executeQuery();

            LogHelper.webLog.info("checkDDD:Getting resultset");

            if (rs.next()) {
            	LogHelper.webLog.debug("checkDDD:Next()");
            	response=rs.getInt("QTTY");
            	LogHelper.webLog.info("checkDDD:Resultset ok");

            	if (response > 0) {
            		response = 0;
            		//Preguntar por las bloqueadas
            		if (isDDDBlocked(DDD) != 0) {
            			//si está bloqueada, le mando 1, sino 0
            			response = 1;
            		}
            	}
            	else{
            		response = 2; //not valid
            	}
            }
            else {
            	LogHelper.webLog.error("checkDDD:Resultset empty");
            	response=3;
            }
        } 
        catch (SQLException e) {
            LogHelper.webLog.fatal("checkDDD:Getting Resultset Exception " + e.getMessage(), e);
            response = 3; //query exception
        } 
        finally {
        	closeConnection(conn, stmt, rs);
        }

        return response;
    }

     /**
      * Controla si el ddd no se encuentra en la lista negra de ddds
      * @param DDD
      * @return 0: Not blocked, !=0: Blocked
      */
     public int isDDDBlocked(String ddd) {
    	 int response = 0;
    	 int respSP;
    	 Connection conn = null;
    	 CallableStatement stmt = null;

    	 LogHelper.webLog.debug(" isDDDBlocked - ddd:" + ddd);

    	 try {
    		 conn = getConnection();
    		 String query = "BEGIN ? := ATS_COMMON_FILTER_LIST.IsInFilterList ( ?, ?, ?, ?, ?); END;";
    		 stmt = conn.prepareCall(query);

    		 LogHelper.webLog.debug(" IsDDDBlocked  query:" + query);

    		 // register input parameters
    		 stmt.setString(2, "AREA");
    		 stmt.setString(3, ddd);
    		 stmt.setString(4, "ASA");
    		 stmt.setString(5, "ASA");
    		 // register the type of the out param - an Oracle specific type
    		 stmt.registerOutParameter(6, Types.VARCHAR);
    		 stmt.registerOutParameter(1, Types.NUMERIC);
    		 LogHelper.webLog.debug(" IsDDDBlocked  outParam set");


    		 // execute and retrieve the result set
    		 LogHelper.webLog.debug(" IsDDDBlocked  Executing query");
    		 stmt.execute();

    		 // Getting resultset from Statement
    		 respSP  = stmt.getInt(1);
    		 String str  = stmt.getString(6);
    		 if (respSP==0 && str.contentEquals("F"))
    			 response=1;
    		 else
    			 response=0;
    		 
    		 LogHelper.webLog.fatal(" IsDDDBlocked  Execution result=" + response);

    	 } 
    	 catch (SQLException e) {
    		 LogHelper.webLog.fatal(" IsDDDBlocked  Exception" + e, e);
    		 response = 1;

    	 } 
    	 finally {
    		 closeConnection(conn, stmt);
    	 }
    	 LogHelper.webLog.debug(" IsDDDBlocked  returning " + Integer.toString(response));
    	 return response;

     }

     /**
      * Devuelve la primer sesión encontrada en la tabla ASA_SESSIONS que cumple con los filtros.
      * Retorna null si no encuentra ninguna sesión.
      * @param sessionId Filtro de búsqueda
      * @param msisdn_t Filtro de búsqueda
      * @param iccid Filtro de búsqueda
      * @return
      */
     public SessionStatus getSession(String sessionId, String msisdn_t, String iccid) {
         LogHelper.webLog.debug("getSession: sessionId" + sessionId);

         //Si todos son null no se puede realizar la busqueda
         if(sessionId == null && msisdn_t == null &&  iccid == null) {
        	 LogHelper.webLog.error("getSession: sessionId, msisdn_t and iccid are null ");
        	 return null;
         }

         Connection conn = null;
         PreparedStatement stmt = null;
         ResultSet rs = null;
         SessionStatus ses = null;
         
         try {
             conn = getConnection();

             //    ses.iccid=a;
             //    ses.imsi_t=a;
             //    ses.msisdn_t=a;
             //    ses.sesStatus=a;
             //    ses.sessionId=a;
             //    ses.transactionId=a;
             //    ses.finished=a;
             //    ses.imsi_real=a;
             //    ses.msisdn_real=a;
             //    origin ;
             //    userType;
             //    cardType;
             //    opKey;
            //     opc;
            //     virtualNetwork;
            //     application;
             
             // 1  2  3  4  5  6  7   8  9  10  11  12  13  14  15  16 17
             String query = "select SES_ICCID, SES_IMSI_TMP, SES_MSISDN_TMP, SES_ID"
            		 + ", SES_TRANSACTION_ID, SES_FINISHED, SES_IMSI_REAL, SES_MSISDN_REAL, SES_ORIGIN, SES_AREA_CODE"
            		 + ", SES_ACTIVATION_TYPE, SES_ERROR_STATUS, SES_LOCI, SES_AUXDATA, SES_IMSI_ROAMING, SES_IMEI "
            		 + ", STA_STATE_NAME, STM_STATE_MACHINE_NAME, CTA_CONFIG_TAG"
                     + " from ASA_SESSIONS where 1=1 ";
         
             // completo QueryString
             if (sessionId != null)    
                 query += " and SES_ID=? ";
       
             if (msisdn_t != null)    
                 query += " and SES_MSISDN_TMP=? ";

             if (iccid != null)    
                 query += " and SES_ICCID=? ";

             query += " order by ses_status_utc desc";
             // Preparo statement
             stmt = conn.prepareStatement(query);
         
             int fieldIndex=0;
             // Set de parámetros
             if (sessionId != null) {
                 stmt.setString(++fieldIndex, sessionId);
             }
             if (msisdn_t != null) {
                 stmt.setString(++fieldIndex, msisdn_t);
             }
             if (iccid != null) {
                 stmt.setString(++fieldIndex, iccid);
             }

             LogHelper.webLog.debug("getSession:Statement:" + query);
         
             // Ejecuta QueryString
             LogHelper.webLog.info("getSession:Executing Query");
             rs = stmt.executeQuery();
         
             // Obtiene el primer registro y lo devuelve
             LogHelper.webLog.info("getSession:Getting resultset");
             if (rs.next()) {
                 LogHelper.webLog.debug("getSession:Next()");
                 ses = new SessionStatus();
             
                 ses.iccid=rs.getString(1);
                 ses.imsi_t=rs.getString(2);
                 ses.msisdn_t=rs.getString(3);
                 ses.sesStatus=rs.getString(17);
                 ses.sessionId=rs.getString(4);
                 ses.transactionId=rs.getString(5);
                 ses.finished=rs.getString(6);
                 ses.imsi_real=rs.getString(7);
                 ses.msisdn_real=rs.getString(8);
                 ses.originActivation=rs.getString(9);
                 ses.areaCode=rs.getString(10);
                 ses.activationType=rs.getString(11);
                 ses.errorStatus=rs.getString(12);
                 ses.loci=rs.getString(13);
                 ses.auxiliarData=deserializeMap(rs.getString(14));
                 ses.imsi_roaming=rs.getString(15);
                 ses.imei = rs.getString(16);
                 ses.stateName = rs.getString(17);
                 ses.stateMachineName = rs.getString(18);
                 ses.stateMachineVersion = rs.getString(19);
                 
                 LogHelper.webLog.info("getSession:Resultset ok");
             }
             else {
                 LogHelper.webLog.error("getSession:Resultset empty");
             }
         }
         catch (Exception e) {
                 LogHelper.webLog.fatal("getSession:Getting Resultset Exception " + e.getMessage(), e);
         } 
         finally {
        	 closeConnection(conn, stmt, rs);
         }
         return ses;
     }
     
     /**
      * Devuelve la primer sesión encontrada en la tabla ASA_SESSIONS que cumple con los filtros.
      * Retorna null si no encuentra ninguna sesión.
      * @param sessionId Filtro de búsqueda
      * @param msisdn_t Filtro de búsqueda
      * @param iccid Filtro de búsqueda
      * @return
      */
     public SessionStatus getSession(String ID, String IMSI_FAKE, String MSISDN_FAKE, String ICCID, String IMSI, String MSISDN) {
         LogHelper.webLog.debug("getSession: SessionID=" + ID 
      		   + " IMSI_fake=" + IMSI_FAKE 
      		   + " MSISDN_fake=" + MSISDN_FAKE 
      		   + " ICCD=" + ICCID  
      		   + " IMSI=" + IMSI
      		   + " MSISDN=" + MSISDN);         

         //Si todos son null no se puede realizar la busqueda
         if(ID == null && IMSI_FAKE == null &&  MSISDN_FAKE == null && ICCID == null && IMSI == null && MSISDN == null ) {
        	 LogHelper.webLog.error("getSession: all parameters are null ");
        	 return null;
         }

         Connection conn = null;
         PreparedStatement stmt = null;
         ResultSet rs = null;
         SessionStatus ses = null;
         
         try {
             conn = getConnection();

             //    ses.iccid=a;
             //    ses.imsi_t=a;
             //    ses.msisdn_t=a;
             //    ses.sesStatus=a;
             //    ses.sessionId=a;
             //    ses.transactionId=a;
             //    ses.finished=a;
             //    ses.imsi_real=a;
             //    ses.msisdn_real=a;
             //    origin ;
             //    userType;
             //    cardType;
             //    opKey;
            //     opc;
            //     virtualNetwork;
            //     application;
             
             // 1  2  3  4  5  6  7   8  9  10  11  12  13  14  15  16 17
             String query = "select SES_ICCID, SES_IMSI_TMP, SES_MSISDN_TMP, SES_ID"
            		 + ", SES_TRANSACTION_ID, SES_FINISHED, SES_IMSI_REAL, SES_MSISDN_REAL, SES_ORIGIN, SES_AREA_CODE"
            		 + ", SES_ACTIVATION_TYPE, SES_ERROR_STATUS, SES_LOCI, SES_AUXDATA, SES_IMSI_ROAMING, SES_IMEI "
            		 + ", STA_STATE_NAME, STM_STATE_MACHINE_NAME, CTA_CONFIG_TAG"
                     + " from ASA_SESSIONS where 1=1 ";
         
             // completo QueryString
             if (ID != null)    
                 query += " and SES_ID=? ";
       
             if (IMSI_FAKE != null)    
                 query += " and SES_IMSI_TMP=? ";

             if (MSISDN_FAKE != null)    
                 query += " and SES_MSISDN_TMP=? ";

             if (ICCID != null)    
                 query += " and SES_ICCID=? ";

             if (IMSI != null)    
                 query += " and SES_IMSI_REAL=? ";

             if (MSISDN != null)    
                 query += " and SES_MSISDN_REAL=? ";
             
             query += " order by ses_status_utc desc";
             // Preparo statement
             stmt = conn.prepareStatement(query);
         
             int fieldIndex=0;
             // Set de parámetros
             if (ID != null) {
                 stmt.setString(++fieldIndex, ID);
             }
             if (IMSI_FAKE != null) {
                 stmt.setString(++fieldIndex, IMSI_FAKE);
             }
             if (MSISDN_FAKE != null) {
                 stmt.setString(++fieldIndex, MSISDN_FAKE);
             }
             if (ICCID != null) {
                 stmt.setString(++fieldIndex, ICCID);
             }
             if (IMSI != null) {
                 stmt.setString(++fieldIndex, IMSI);
             }
             if (MSISDN != null) {
                 stmt.setString(++fieldIndex, MSISDN);
             }

             LogHelper.webLog.debug("getSession:Statement:" + query);
         
             // Ejecuta QueryString
             LogHelper.webLog.info("getSession:Executing Query");
             rs = stmt.executeQuery();
         
             // Obtiene el primer registro y lo devuelve
             LogHelper.webLog.info("getSession:Getting resultset");
             if (rs.next()) {
                 LogHelper.webLog.debug("getSession:Next()");
                 ses = new SessionStatus();
             
                 ses.iccid=rs.getString(1);
                 ses.imsi_t=rs.getString(2);
                 ses.msisdn_t=rs.getString(3);
                 ses.sesStatus=rs.getString(17);
                 ses.sessionId=rs.getString(4);
                 ses.transactionId=rs.getString(5);
                 ses.finished=rs.getString(6);
                 ses.imsi_real=rs.getString(7);
                 ses.msisdn_real=rs.getString(8);
                 ses.originActivation=rs.getString(9);
                 ses.areaCode=rs.getString(10);
                 ses.activationType=rs.getString(11);
                 ses.errorStatus=rs.getString(12);
                 ses.loci=rs.getString(13);
                 ses.auxiliarData=deserializeMap(rs.getString(14));
                 ses.imsi_roaming=rs.getString(15);
                 ses.imei = rs.getString(16);
                 ses.stateName = rs.getString(17);
                 ses.stateMachineName = rs.getString(18);
                 ses.stateMachineVersion = rs.getString(19);
                 
                 LogHelper.webLog.info("getSession:Resultset ok");
             }
             else {
                 LogHelper.webLog.error("getSession:Resultset empty");
             }
         }
         catch (Exception e) {
                 LogHelper.webLog.fatal("getSession:Getting Resultset Exception " + e.getMessage(), e);
         } 
         finally {
        	 closeConnection(conn, stmt, rs);
         }
         return ses;
     }

     private HashMap<String,String> deserializeMap(String serialized) {
    	 XStream vXtr = new XStream();
    	 HashMap<String,String> map = null;
         try{
             if (serialized != null && !serialized.isEmpty())
                 map = (HashMap<String, String>) vXtr.fromXML(serialized);  // Deserialize
         }
         catch(Exception ex)
         {
             LogHelper.webLog.fatal("ASA - deserializeMap - Exception:" + ex, ex);
         }
         return map;
     }
     
     /**
      * Devuelve la cantidad de sesiones en la tabla ASA_SESSIONS
      * Implementa: "select count(*) from ASA_SESSIONS"
      * @param sessionId
      * @param msisdn_t
      * @param iccid
      * @return
      */
     public long checkSession(String sessionId, String msisdn_t, String iccid) {
         
         LogHelper.webLog.debug("checkSession: sessionId: " + sessionId);

         Connection conn = null;
         PreparedStatement stmt = null;
         ResultSet rs = null;
         
         long response=0;
         
         try {
             LogHelper.webLog.debug("ut_CheckSession: getConnectionToDatabase()");
             conn = getConnection();
             
             String query = "select count(*) from ASA_SESSIONS where 1=1 ";
             
             // completo QueryString
             if (sessionId!=null)    
                 query += " and SES_ID=? ";
           
             if (msisdn_t!=null)    
                 query += " and SES_MSISDN_TMP=? ";

             if (iccid!=null)    
                 query += " and SES_ICCID=? ";

             // Preparo statement
             stmt = conn.prepareStatement(query);
             
             int fieldIndex=0;
             // Set de parámetros
             if (sessionId!=null)    
             {
                 stmt.setString(++fieldIndex, sessionId);
             }
             if (msisdn_t!=null)    
             {
                 stmt.setString(++fieldIndex, msisdn_t);
             }
             if (iccid!=null)    
             {
                 stmt.setString(++fieldIndex, iccid);
             }

             LogHelper.webLog.debug("checkSession:Statement: " + query);
             
             LogHelper.webLog.info("checkSession:Executing Query");
             rs = stmt.executeQuery();

             LogHelper.webLog.info("checkSession:Getting resultset");
             if (rs.next()) {
                 LogHelper.webLog.debug("checkSession:Next()");
                 response = rs.getLong(1);
                 LogHelper.webLog.info("checkSession:Resultset ok, count:" + response);
             }
             else {
                 LogHelper.webLog.error("checkSession:Resultset empty");
             }
         } 
         catch (Exception e) {
             LogHelper.webLog.fatal("checkSession:Getting Resultset Exception " + e.getMessage(), e);
             response = -1;
         } 
         finally {
        	 closeConnection(conn, stmt, rs);
         }
         return response;
     }

     /**
      * Guarda el contenido en memeoria de una sesión a la base de datos
      * Implementa UPDATE o INSERT 
      * @param sesCount 1: UPDATE, !=1: INSERT
      * @param sd Datos de la sesión
      * @return
      */
     public int sessionToDB(long sesCount, SessionData sd) {
         Connection conn = null;
         PreparedStatement stmt = null;

         int response=0;
         
         try {
             LogHelper.webLog.debug("sessionToDB: getConnection()");
             conn = getConnection();
             
             String query;
             
             if (sesCount == 1)  {
                 // Update
                 LogHelper.webLog.debug("sessionToDB: Updating.");
                 query = "update ASA_SESSIONS set SES_TRANSACTION_ID=?";
                 query += ",SES_ICCID=?";
                 query += ",SES_LOCI=?";
                 query += ",SES_IMSI_TMP=?";
                 query += ",SES_MSISDN_TMP=?";
                 query += ",SES_IMSI_REAL=?";
                 query += ",SES_MSISDN_REAL=?";
                 query += ",SES_MSISDN_R_LIST=?";
                 query += ",SES_FINISHED=?";
                 query += ",SES_CREATION_UTC=?";
                 query += ",SES_STATUS_UTC=?";
                 query += ",SES_ORIGIN=?"; // TODO: Agregar AreaCode, ErrorStatus, ActivationType
                 query += ",SES_AREA_CODE=?"; // TODO: Agregar AreaCode, ErrorStatus, ActivationType
                 query += ",SES_ACTIVATION_TYPE=?"; // TODO: Agregar AreaCode, ErrorStatus, ActivationType
                 query += ",SES_ERROR_STATUS=?"; // TODO: Agregar AreaCode, ErrorStatus, ActivationType
                 query += ",SES_IMSI_ROAMING=?";
                 query += ",CTA_CONFIG_TAG=?";
                 query += ",STA_STATE_NAME=?";
                 query += ",STM_STATE_MACHINE_NAME=?";
                 query += ",MODIFICATION_DATE_UTC=?";
                 query += ",CREATION_USER=?";
                 query += ",MODIFICATION_USER=?";
                 query += " where SES_ID=?";
             }
             else {
                 // Insert
                 LogHelper.webLog.debug("sessionToDB: Inserting.");
                 query = "insert into ASA_SESSIONS (SES_TRANSACTION_ID";
                 query += ",SES_ICCID";
                 query += ",SES_LOCI";
                 query += ",SES_IMSI_TMP";
                 query += ",SES_MSISDN_TMP";
                 query += ",SES_IMSI_REAL";
                 query += ",SES_MSISDN_REAL";
                 query += ",SES_MSISDN_R_LIST";
                 query += ",SES_FINISHED";
                 query += ",SES_CREATION_UTC";
                 query += ",SES_STATUS_UTC";
                 query += ",SES_ORIGIN"; // TODO: Agregar AreaCode, ErrorStatus, ActivationType
                 query += ",SES_AREA_CODE"; // TODO: Agregar AreaCode, ErrorStatus, ActivationType
                 query += ",SES_ACTIVATION_TYPE"; // TODO: Agregar AreaCode, ErrorStatus, ActivationType
                 query += ",SES_ERROR_STATUS"; // TODO: Agregar AreaCode, ErrorStatus, ActivationType
                 query += ",SES_IMSI_ROAMING";
                 query += ",CTA_CONFIG_TAG";
                 query += ",STA_STATE_NAME";
                 query += ",STM_STATE_MACHINE_NAME";
                 query += ",MODIFICATION_DATE_UTC";
                 query += ",CREATION_USER";
                 query += ",MODIFICATION_USER";
                 query += ",SES_ID)";
                 query += " values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
             }
             
             stmt = conn.prepareStatement(query);

             LogHelper.webLog.debug("sessionToDB:Statement: " + query);

             String strval;
             Integer intval;
             Calendar cal;
             // Este seteo se hace afuera del IF anterior porque se preparan las
             // consultas con los mismos parametros y en el mismo orden.
//             strval = sd.getStateMachineState().getName();
//             if (strval!=null)
//                 stmt.setString(1, strval);
//             else
//                 stmt.setNull(1, Types.VARCHAR);

             intval = sd.getTransactionID();
             if (intval!=null)
                 stmt.setInt(1, intval);
             else
                 stmt.setNull(1, Types.NUMERIC);

             strval = sd.getICCID();
             if (strval!=null)
                 stmt.setString(2, strval);
             else
                 stmt.setNull(2, Types.VARCHAR);

             strval = sd.getLOCI();
             if (strval!=null)
                 stmt.setString(3, strval);
             else
                 stmt.setNull(3, Types.VARCHAR);

             strval = sd.getIMSI_FAKE();
             if (strval!=null)
                 stmt.setString(4, strval);
             else
                 stmt.setNull(4, Types.VARCHAR);

             strval = sd.getMSISDN_FAKE();
             if (strval!=null)
                 stmt.setString(5, strval);
             else
                 stmt.setNull(5, Types.VARCHAR);

             strval = sd.getIMSI();
             if (strval!=null)
                 stmt.setString(6, strval);
             else
                 stmt.setNull(6, Types.VARCHAR);

             strval = sd.getMSISDN();
             if (strval!=null)
                 stmt.setString(7, strval);
             else
                 stmt.setNull(7, Types.VARCHAR);

             strval = (sd.getMSISDN_RESERVED()==null?null:sd.getMSISDN_RESERVED().toString());
             if (strval!=null)
                 stmt.setString(8, strval);
             else
                 stmt.setNull(8, Types.VARCHAR);

             strval = (sd.isFinished() ? "T" : "F");
             stmt.setString(9, strval);

             cal = sd.getCreationDate();
             if (cal!=null)
                 stmt.setTimestamp(10, new Timestamp(cal.getTimeInMillis()));
             else
                 stmt.setNull(10, Types.VARCHAR);
             
             cal = sd.getUpdateDate();
             if (cal!=null)
                 stmt.setTimestamp(11, new Timestamp(cal.getTimeInMillis()));
             else
                 stmt.setNull(11, Types.VARCHAR);
             
             strval = sd.getOriginActivation().getName();
             if (strval!=null)
                 stmt.setString(12, strval);
             else
                 stmt.setNull(12, Types.VARCHAR);

             strval = sd.getAreaCode();
             if (strval!=null)
                 stmt.setString(13, strval);
             else
                 stmt.setNull(13, Types.VARCHAR);
             
             strval = sd.getActivationType();
             if (strval!=null)
                 stmt.setString(14, strval);
             else
                 stmt.setNull(14, Types.VARCHAR);
             
             strval = sd.getStateMachineStatusError();
             if (strval!=null)
                 stmt.setString(15, strval);
             else
                 stmt.setNull(15, Types.VARCHAR);
             
             strval = sd.getIMSI_ROAMING();
             if (strval!=null)
                 stmt.setString(16, strval);
             else
                 stmt.setNull(16, Types.VARCHAR);

             intval = Integer.valueOf(sd.getStateMachineVersion());
             if (intval!=null)
                 stmt.setInt(17, intval);
             else
                 stmt.setNull(17, Types.NUMERIC);
             
             strval = sd.getStateMachineState().getName();
             if (strval!=null)
                 stmt.setString(18, strval);
             else
                 stmt.setNull(18, Types.VARCHAR);

             strval = sd.getStateMachineName();
             if (strval!=null)
                 stmt.setString(19, strval);
             else
                 stmt.setNull(19, Types.VARCHAR);

             // TODO revisar si queda asi o se ponen valores por defecto en la base
             stmt.setTimestamp(20, new Timestamp(System.currentTimeMillis()));
             stmt.setString(21, "N/A");
             stmt.setString(22, "N/A");

             strval = sd.getID();
             if (strval!=null)
                 stmt.setString(23, strval);
             else
                 stmt.setNull(23, Types.VARCHAR);
             
             LogHelper.webLog.info("sessionToDB:Executing statement");
             
             try {
                 stmt.execute();
                 conn.commit();
             }
             catch (SQLException ex) {
            	 conn.rollback();
                 LogHelper.webLog.error("sessionToDB  Exception executing Query: " + ex.getLocalizedMessage(), ex);
                 response=1;
             }
         } 
         catch (Exception e) {
             LogHelper.webLog.fatal("sessionToDB:Exception: " + e.getMessage(), e);
             response = -1;
            
         } 
         finally {
        	 closeConnection(conn, stmt);
         }
         
         return response;
     }

     /**
      * Incrementa contador de reintentos en la tabla de suscriptores
      * @param IMSI
      * @return
      */
     public int incrementSubscriberRetryCounter(String IMSI) {
         LogHelper.webLog.debug("incremetRetries:Begin : IMSI:" + IMSI);

         if (IMSI == null || IMSI.isEmpty()) {
        	    LogHelper.webLog.error("incremetRetries:IMSI null");
                return 2;
         }
         
         Connection conn = null;
         PreparedStatement stmt = null;
         String query = "update ASA_SUBSCRIBERS set SUB_RETRY_COUNTER = SUB_RETRY_COUNTER + 1  where SUB_IMSI_TMP = ? ";
         int response;
         
         try {
             LogHelper.webLog.debug("incremetRetries:getConnectionToDatabase()");
             conn = getConnection();
             stmt = conn.prepareStatement(query);

             stmt.setString(1 , IMSI);
             LogHelper.webLog.debug("incremetRetries:Statement:" + stmt.toString());

             LogHelper.webLog.info("incremetRetries:Executing Query");
             if (stmt.executeUpdate() > 0)
            	 response = 0;
             else
            	 response = 1;
         } 
         catch (Exception e) {
             LogHelper.webLog.fatal("incremetRetries:Getting Resultset Exception " + e.getMessage(), e);
             response = 1;
         } 
         finally {        
         	closeConnection(conn, stmt);
         }
         
         return response;
     }

     /**
      * Devuelve un listado HTML de los logs de una sesión
      * @param sessionId
      * @return
      * @throws java.lang.Exception
      */
     public String getSessionLogs(String sessionId) throws Exception {

         Connection conn = null;
         PreparedStatement stmt = null;
         String msisdnreal=null;
         ResultSet rs=null;
         String retVal="";

         if (sessionId==null || sessionId.isEmpty())
         {
             return retVal;
         }
         
         SessionStatus sst = getSession(sessionId, null, null);
         if (sst != null){
             msisdnreal = sst.msisdn_real;
         }

         String query=" SELECT ALO_LOG_DATE_UTC,ALO_MODULE, ALO_INSTANCE_ID, ALO_DESCRIPTION, ALO_DETAILS_1, ";
         query += " ALO_LOG_LEVEL,ALO_ICCID, ALO_IMSI, ALO_MSISDN, ALO_LOG_CATEGORY, ";
         query += " ALO_SES_SET_STATUS_CODE, ALO_SES_TRANSACTION_ID, ALO_SES_IMSI_REAL, ALO_SES_MSISDN_REAL";
//         query += " FROM ASA_01ALL0101_SCH.ASA_LOGS Where ALO_SES_ID = '";
         query += " FROM ASA_LOGS Where ALO_SES_ID = '";
         query += sessionId + "' ";
                     
         if (msisdnreal!=null && !msisdnreal.isEmpty())
             query += " or ALO_MSISDN='" + msisdnreal + "'" ;
                     
         query += " ORDER BY ALO_LOG_DATE_UTC DESC NULLS LAST";

         try {
             conn = getConnection();
             String details;
             String category;
             String color;
             String logDate;
             stmt = conn.prepareStatement(query);

             rs = stmt.executeQuery();
             int counter=0;
             if (rs!=null) {
                 while (rs.next() && (counter < 500)) {
                 category=rs.getString("ALO_LOG_CATEGORY");
                 logDate=new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss.SS").format(rs.getTimestamp("ALO_LOG_DATE_UTC"));
                 color="white";
                 counter++;
                 if (counter % 2==0)
                     color="lightgray";
                 if (category.contentEquals("ERROR"))
                     color = "Pink";
                             retVal+="<TR>";    
                             // retVal+="<TD rowspan=2 bgcolor="+ color+ ">"+ rs.getString("ALO_LOG_DATE_UTC")+"</TD>";
                             retVal+="<TD rowspan=2 bgcolor="+ color+ ">"+ logDate+"</TD>";
                                 retVal+="<TD bgcolor="+ color+ ">"+ rs.getString("ALO_MODULE")+"</TD>";
                                 retVal+="<TD bgcolor="+ color+ ">"+ rs.getString("ALO_INSTANCE_ID")+"</TD>";
                                 retVal+="<TD bgcolor="+ color+ ">"+ rs.getString("ALO_DESCRIPTION")+"</TD>";
                                 retVal+="<TD bgcolor="+ color+ ">"+ rs.getString("ALO_LOG_LEVEL")+"</TD>";
                                 retVal+="<TD bgcolor="+ color+ ">"+ rs.getString("ALO_ICCID")+"</TD>";
                                 retVal+="<TD bgcolor="+ color+ ">"+ rs.getString("ALO_IMSI")+"</TD>";
                                 retVal+="<TD bgcolor="+ color+ ">"+ rs.getString("ALO_MSISDN")+"</TD>";
                                 retVal+="<TD bgcolor="+ color+ ">"+ category+"</TD>";
                                 retVal+="<TD bgcolor="+ color+ ">"+ rs.getString("ALO_SES_SET_STATUS_CODE")+"</TD>";
                                 retVal+="<TD bgcolor="+ color+ ">"+ rs.getString("ALO_SES_TRANSACTION_ID")+"</TD>";
                                 retVal+="<TD bgcolor="+ color+ ">"+ rs.getString("ALO_SES_IMSI_REAL")+"</TD>";
                                 retVal+="<TD bgcolor="+ color+ ">"+ rs.getString("ALO_SES_MSISDN_REAL")+"</TD>";
                             retVal+=" </TR><TR>";    
                             details=rs.getString("ALO_DETAILS_1");
                             if (details!=null){
                                 retVal+="<TD COLSPAN=12 bgcolor="+color+" ><font color=gray size=2>"+details.replace("<","&lt;").replace(">","&gt;")+"</font></TD>";
                             }
                             retVal +=" </TR>";    
                 }
                 
                 if (counter>=500)
                     retVal+="<TR COLSPAN=13><font color=red >&nbsp;&nbsp;Use WebPortal to see more rows.</TR>";
                
             }  
         }
         catch(Exception e) {
                 LogHelper.webLog.fatal("getSessionLogs: Exception " + e.getMessage(), e);
         }
         finally {
        	 closeConnection(conn, stmt, rs);
         }
         
         return retVal;
     }

     /**
      * Recupera codigos de area almacenados en la base de datos
      * 
      * @param IMSI IMSI_FAKE del subscriber
      * @param qqty cantidad de codigos de area a recuperar
      * @param ddd lista de codigos de area recuperados
      * @return
      */
     public int getDDD(String IMSI, int qqty, String[] ddd ) {
    	 int response = 0;

    	 Connection conn = null;
    	 PreparedStatement stmt = null;
    	 ResultSet rs = null;

    	 if (IMSI == null || IMSI.isEmpty()) {
    		 LogHelper.webLog.error("getDDD:Resultset empty");
    		 return -1;
    	 }

    	 String query = ServerConfiguration.getInstance().get("query_ddds");

    	 // Las obtienes de la tabla lms_area
    	 try {
    		 LogHelper.webLog.debug("getDDD:getConnectionToDatabase()");

    		 conn = getConnection();
    		 stmt = conn.prepareStatement(query);

    		 stmt.setString(1 , IMSI);
    		 stmt.setInt(2, qqty);

    		 LogHelper.webLog.debug("getDDD:Statement:" + query);
    		 LogHelper.webLog.info("getDDD:Executing Query");
    		 rs = stmt.executeQuery();

    		 LogHelper.webLog.info("getDDD:Getting resultset");

    		 while(response < qqty && rs.next()) {
    			 LogHelper.webLog.debug("getDDD:Next() response: " + response);

    			 ddd[response] = rs.getString("DDD");
    			 response++;
    		 }

    		 if (response == 0) { // no tengo areas
    			 LogHelper.webLog.debug("getDDD: without areas");
    		 }
    	 } 
    	 catch (Exception e) {
    		 LogHelper.webLog.fatal("getDDD:Getting Resultset Exception " + e.getMessage(), e);
    		 response =-1; //query exception
    	 } 
    	 finally {
    		 closeConnection(conn, stmt, rs);
    	 }

    	 return response;
     }
     
     /**
      * 
      * @return devuelve true si existe un subscriptor
      */
     public Boolean isFake(String MSISDN) {
    	 Boolean retorno = false;
    	 if (MSISDN == null || MSISDN.isEmpty())
    		 return retorno;

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        String query = ServerConfiguration.getInstance().get("query_all_subs");

        try {
            LogHelper.webLog.debug("isFake:getConnectionToDatabase()");
            conn = getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, MSISDN);
            rs = stmt.executeQuery();

            if (rs.next()) {
                retorno = true;
            }
        }
        catch(Exception e){
            LogHelper.webLog.error("isFake: Finally exception: "+e.getMessage(), e);
        }
        finally {
        	closeConnection(conn, stmt, rs);
        }
        
        return retorno;
     }

     public int getDisplayMessageForIVR(String origen, String estado, String error) {
    	 LogHelper.webLog.debug("getDisplayMessageForIVR - Origin: " + origen + ", Estado: " + estado + ", Error: " + error);

    	 int codeForIVR = 0;
    	 Connection conn = null;
    	 PreparedStatement stmt = null;
    	 ResultSet rs = null;
         
    	 String query = "select adm_message, adm_config_tag, adm_ivr_code from ASA_DISPLAY_MESSAGES  where adm_origin_activation =? and adm_state_name =? and adm_error_code =?";    	 
         try {
 			conn = getConnection();
 			stmt = conn.prepareStatement(query);
 			stmt.setString(1, origen);
 			stmt.setString(2, estado);
 			stmt.setString(3, error);
 			rs = stmt.executeQuery();
 			if (rs.next()) {
 				codeForIVR =rs.getInt("adm_ivr_code"); 
 			}
         }
         catch(SQLException sqle) {
        	 LogHelper.webLog.error(sqle.getMessage());
         }
         finally {
        	 closeConnection(conn, stmt, rs);
         }
    	 return codeForIVR;
     }
     
     public String getDisplayMessage(String origen, String estado, String error, String stateMachineName, String stateMachineVersion) {
    	 LogHelper.webLog.debug("getDisplayMessageFor - Origin: " + origen + ", Estado: " + estado + ", Error: " + error + ", StateMachine: " + stateMachineName + ", Version: " + stateMachineVersion) ;
    	 String message = "MSG_DEFAULT";
    	 String errorcode = "";
    	 
    	 Connection conn = null;
    	 PreparedStatement stmt = null;
    	 ResultSet rs = null;
         
    	 String query = "select adm_message, adm_error_code from ASA_DISPLAY_MESSAGES  where adm_origin_activation =? and adm_state_name =? and adm_state_machine_name =? and adm_config_tag =?";
    	 //String query = "select adm_message, adm_error_code from ASA_DISPLAY_MESSAGES  where adm_origin_activation =? and adm_state_name =? and adm_state_machine_name =?";
         
         try {
        	LogHelper.webLog.debug("getDisplayMessage - Starting connection"); 
  			conn = getConnection();
  			stmt = conn.prepareStatement(query);
  			stmt.setString(1, origen);
  			stmt.setString(2, estado);
  			stmt.setString(3, stateMachineName);
  			stmt.setString(4, stateMachineVersion);
  			rs = stmt.executeQuery();
  			
  			LogHelper.webLog.debug("getDisplayMessage - Query execute connection"); 
  			if (rs.next()) {
  				errorcode= rs.getString("adm_error_code"); 
  				LogHelper.webLog.debug("getDisplayMessage - ErrorCodeFound on database: " + errorcode) ;
  				if (errorcode.equals(error) || errorcode.equals("*")) {
  	  				message =rs.getString("adm_message"); 
  	  				LogHelper.webLog.debug("getDisplayMessage - Message found on database: " + message) ;
  				}
  			}
         }
         catch(SQLException sqle) {
        	 LogHelper.webLog.error(sqle.getMessage());
         }
         finally {
        	 closeConnection(conn, stmt, rs);
         }
         
    	 return message;
     }
     
     public String getResumeEvent(String origin, String persistState) {
    	 String event = null;
    	 
    	 Connection conn = null;
    	 PreparedStatement stmt = null;
    	 ResultSet rs = null;
    	 
    	 String query = "select orc_resume_event from ASA_ORCH_RESUME_CONFIG where orc_origin_activation = ? and orc_persist_state = ?";
    	 
    	 try {
    		 conn = getConnection();
    		 stmt = conn.prepareStatement(query);
    		 
    		 stmt.setString(1, origin);
    		 stmt.setString(2, persistState);
    		 
    		 rs = stmt.executeQuery();
    		 if (rs != null && rs.next()) {
    			 event = rs.getString("orc_resume_event");
    		 }
    	 }
    	 catch(SQLException sqle) {
        	 LogHelper.webLog.error(sqle.getMessage());
    	 }
    	 finally {
    		 closeConnection(conn, stmt, rs);
    	 }
    	 
    	 return event;
     }
     
     public String getVlrNumber(String imsi, String origin) {
    	 String vlrNumber = null;
    	 
    	 Connection conn = null;
    	 PreparedStatement stmt = null;
    	 ResultSet rs = null;
    	 String query = "";
    	 boolean hlr = false;
    	 
//    	 if (activationType.contentEquals("UpdateLocation") || activationType.contentEquals("UpdateGPRSLocation")) {
//    		 query = "select HLD_VLR_NUMBER from hlr_data_3g where HLD_IMSI =?";
//    	 }
//    	 else {
//    		 query = "select * from hlr_data_lte where HLD_IMSI =?";
//    		 //HLD_LTE_MME_IDENTITY
//    	 }
    	 
    	 if ( origin.contentEquals("hlr") || origin.contentEquals("HLR") ) {
    		 query = "select HLD_VLR_NUMBER from hlr_data_3g where HLD_IMSI =?";
    		 hlr=true;
    	 }
    	 else {
    		 query = "select HLD_LTE_MME_IDENTITY from hlr_data_lte where HLD_IMSI =?";
    		 //HLD_LTE_MME_IDENTITY
    	 }
    	 
		 LogHelper.webLog.debug("getVlrNumber: IMSI-> " + imsi ); 

    	 try {
    		 conn = getConnection();
    		 stmt = conn.prepareStatement(query);
    		 
    		 stmt.setString(1, imsi);
    		 
    		 rs = stmt.executeQuery();
    		 
    		 if (rs != null && rs.next()) {
    			 
    			 if (hlr) {
	    			 vlrNumber = rs.getString("HLD_VLR_NUMBER").substring(2);
	    			 LogHelper.webLog.debug("VlrNumber: " + vlrNumber); 
    			 } else {
	    			 vlrNumber = rs.getString("HLD_LTE_MME_IDENTITY").substring(2);
	    			 LogHelper.webLog.debug("HssNumber: " + vlrNumber); 
    			 }
    		 }
    		 else
    		 {
    			 LogHelper.webLog.debug("Vlr-Hss-Number not found for IMSI: " + imsi);    			 
    		 }
    	 }
    	 catch(SQLException sqle) {
        	 LogHelper.webLog.error(sqle.getMessage());
    	 }
    	 finally {
    		 closeConnection(conn, stmt, rs);
    	 }
    	 
    	 return vlrNumber;
     }
     
}
