/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ats_connection.asa.orchestrator.service;

import com.ats_connection.asa.orchestrator.config.ServerConfiguration;
import com.ats_connection.asa.orchestrator.response.WsAckRsp;
import com.ats_connection.asa.orchestrator.helper.LogHelper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.Calendar;
import java.util.Date;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

import javax.xml.soap.SOAPException;

/**
 *
 * @author pdiaz
 */
@WebService()
public class Logger {

    /**
     * Permite ingresar una entrada al log de la sesión con un nivel de log (logLevel) numérico
     * que determinará la inclusión del mismo en la BD.  logLevel 1 es el de mayor prioridad 
     * y siempre termina en el base.  Loglevel 3 es el de menor prioridad y las entradas con
     * nivel 3 terminan en la Base de datos sólo si se produjo un error en la sesión de activación.
     * @param module Nombre del módulo
     * @param description Descripción corta de la entrada de log.  Deberá ser de no mas de 128 caracteres
     * @param details Detalle de log.  Puede ser de hasta 16000 caracteres
     * @param logLevel Nivel de log. 1 Nivel mas prioritario, 3 nivel menos prioritario
     * @param category Categoría de log, puede ser uno de los siguientes valores: INFO, WARNING, ERROR
     * @param logDate Fecha de log.  Debe ser envíada en UTC con el siguiente formato yyyyMMdd HH:mm:ss.mil.  Si se envía en null, se setea la fecha y hora de la registración.
     * @param sessionId Id de la sesión
     * @param MSISDN Msisdn de la sesión
     * @param IMSI IMSI de la sesión
     * @param ICCID ICCID de la sesión
     * @param instanceId Instancia del módulo
     * @return .resultCode=0 Ok
     * @throws javax.xml.soap.SOAPException
     */
    @WebMethod(operationName = "activityLog")
    public WsAckRsp activityLog(@WebParam(name = "module")
    String module, @WebParam(name = "description")
    String description, @WebParam(name = "details")
    String details, @WebParam(name = "logLevel")
    String logLevel, @WebParam(name = "category")
    String category, @WebParam(name = "logDate")
    String logDate, @WebParam(name = "sessionId")
    String sessionId, @WebParam(name = "MSISDN")
    String MSISDN, @WebParam(name = "IMSI")
    String IMSI, @WebParam(name = "ICCID")
    String ICCID, @WebParam(name = "instanceId")
    String instanceId) throws SOAPException {

        // Control de estado del orquestador
        if (!ServerConfiguration.getInstance().serverRunning()){
            LogHelper.webLog.fatal(" Operation not processed.  Reason:Server stopped ");
            //return -99;
            throw new javax.xml.soap.SOAPException("ASA-Orchestrator server paused");
        }

        WsAckRsp retVal=new WsAckRsp();

        DateFormat formatter ; 
        Date date ; 
        Calendar cal=null;
        String dateFormat;
        dateFormat=ServerConfiguration.getInstance().get("dateFormat");
        if (dateFormat==null || dateFormat.isEmpty())
            dateFormat="yyyyMMdd HH:mm:ss.S";
        
        if (logDate!=null && !logDate.isEmpty()){
            try {
                formatter = new SimpleDateFormat(dateFormat);
                date = formatter.parse(logDate);
                cal = Calendar.getInstance();
                cal.setTime(date);
                } catch (Exception ex) {
                    LogHelper.webLog.error("dbLoggerAPI(Logger):activityLog - dateFormat date=(" + logDate + ") -> format=("+ dateFormat +") Exception:" + ex, ex);

                    retVal.resultCode=21L; // Date format Error
                    retVal.resultDescription="Date Format Error";
                    
                    return retVal;
                }
        }
        
        
        try{ 
            com.ats_connection.asa.server.logger.AsaServerLogger.getInstance().activityLog(
                module, 
                description, 
                details, 
                Integer.parseInt(logLevel), 
                category, 
                cal, 
                sessionId, MSISDN, IMSI, ICCID, instanceId);

            retVal.resultCode=0L; // Date format Error
            retVal.resultDescription="Ok";

        }catch(Exception e){            
            String fmtDate=null;
            if (cal!=null)
                fmtDate=cal.toString();

            LogHelper.webLog.error("dbLoggerAPI(Logger):activityLog(module=" + module + ", description="+ description +
                    ", category=" + category + ", logLevel="+logLevel+ ", sessionId=" + sessionId + ", MSISDN="+ MSISDN + ", IMSI=" + IMSI + 
                    ", ICCID="+ ICCID + ", date=" + fmtDate + ") - dbLog Exception:" + e, e);
            retVal.resultCode=31L; // Date format Error
            retVal.resultDescription="dbLog Error";
        }
        
        return retVal;
    }

    /**
     * Permite ingresar una entrada al log de la sesión con un nivel de SDR.
     * Estas entradas siempre se van a grabar en la base de datos independientemente del estado
     * de finalización de la sesión.
     * @param module Nombre del módulo
     * @param description Descripción corta de la entrada de log.  Deberá ser de no mas de 128 caracteres
     * @param details Detalle de log.  Puede ser de hasta 16000 caracteres
     * @param category Categoría de log, puede ser uno de los siguientes valores: INFO, WARNING, ERROR
     * @param logDate Fecha de log.  Debe ser envíada en UTC con el siguiente formato yyyyMMdd HH:mm:ss.mil.  Si se envía en null, se setea la fecha y hora de la registración.
     * @param sessionId Id de la sesión
     * @param MSISDN Msisdn de la sesión
     * @param IMSI IMSI de la sesión
     * @param ICCID ICCID de la sesión
     * @param instanceId Instancia del módulo
     * @return .resultCode=0 Ok
     * @throws javax.xml.soap.SOAPException
     */
    @WebMethod(operationName = "sdrLog")
    public WsAckRsp sdrLog(@WebParam(name = "module")
    String module, @WebParam(name = "description")
    String description, @WebParam(name = "details")
    String details, @WebParam(name = "category")
    String category, @WebParam(name = "logDate")
    String logDate, @WebParam(name = "sessionId")
    String sessionId, @WebParam(name = "MSISDN")
    String MSISDN, @WebParam(name = "IMSI")
    String IMSI, @WebParam(name = "ICCID")
    String ICCID, @WebParam(name = "instanceId")
    String instanceId) throws SOAPException {
        
        // Control de estado del orquestador
        if (!ServerConfiguration.getInstance().serverRunning()){
            LogHelper.webLog.fatal(" Operation not processed.  Reason:Server stopped ");
            //return -99;
            throw new javax.xml.soap.SOAPException("ASA-Orchestrator server paused");
        }

        WsAckRsp retVal=new WsAckRsp();

        DateFormat formatter ; 
        Date date ; 
        Calendar cal=null;
        String dateFormat;
        dateFormat=ServerConfiguration.getInstance().get("dateFormat");
        if (dateFormat==null || dateFormat.isEmpty())
            dateFormat="yyyyMMdd HH:mm:ss.S";
        
        if (logDate!=null && !logDate.isEmpty()){
            try {
                formatter = new SimpleDateFormat(dateFormat);
                date = (Date) formatter.parse(logDate);
                cal = Calendar.getInstance();
                cal.setTime(date);
                } catch (Exception ex) {
                    LogHelper.webLog.error("dbLoggerAPI(Logger):sdrLog - dateFormat date=(" + logDate + ") -> format=("+ dateFormat +") Exception:" + ex, ex);

                    retVal.resultCode=21L; // Date format Error
                    retVal.resultDescription="Date Format Error";
                    
                    return retVal;
                }
        }
           
        
        try{ 
            com.ats_connection.asa.server.logger.AsaServerLogger.getInstance().sdrLog(
                module, 
                description, 
                details, 
                category, 
                cal, 
                sessionId, MSISDN, IMSI, ICCID, instanceId);

            retVal.resultCode=0L; // Date format Error
            retVal.resultDescription="Ok";

        }catch(Exception e){            
            
            String fmtDate=null;
            if (cal!=null)
                fmtDate=cal.toString();
            
            LogHelper.webLog.error("dbLoggerAPI(Logger):sdrLog(module=" + module + ", description="+ description +
                    ", category=" + category + ", sessionId=" + sessionId + ", MSISDN="+ MSISDN + ", IMSI=" + IMSI + 
                    ", ICCID="+ ICCID + ", date=" + fmtDate + ") - dbLog Exception:" + e, e);
            retVal.resultCode=31L; // Date format Error
            retVal.resultDescription="dbLog Error";
        }
        
        return retVal;
    }

}
