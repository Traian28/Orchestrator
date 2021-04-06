/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ats_connection.asa.orchestrator.service;

import com.ats_connection.asa.orchestrator.config.ServerConfiguration;
import com.ats_connection.asa.orchestrator.core.ManagementService;
import com.ats_connection.asa.orchestrator.core.SessionStatus;
import com.ats_connection.asa.orchestrator.response.*;
import com.ats_connection.asa.orchestrator.helper.LogHelper;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

import javax.xml.soap.SOAPException;

/**
 * Clase interna para manejo de sesiones.
 * @author pdiaz
 */
@WebService()
public class Management {
    
    public Management() {
        LogHelper.webLog.info("ASA - Management API- Initializing");
    }
    
    /**
     * getSessionData
     * Implementa una llamada el metodo getSessionStatus.
     * Devuelve los datos de una sesion.  La busqueda se realiza primero en memoria.  
     * Si la sesion no existe en memoria, se busca en la base de datos.
     * Devuelve los datos tanto de una sesion activa como de una finalizada.
     * 
     * @param filterMSISDN_t Valor del MSISDN Fake para buscar la sesion.  Puede venir vacio.
     * @param filterSessionId Valor del SessionId para buscar la sesion.  Puede venir vacio.
     * @param filterICCID Valor del ICCID para buscar la sesion.  Puede venir vacio.
     * 
     * @return Datos de la sesion
     */
    @WebMethod(operationName = "getSessionData")
    public SessionStatus getSessionData(
    		@WebParam(name = "filterMSISDN_t") String filterMSISDN_t,
    		@WebParam(name = "filterSessionId")  String filterSessionId,
    		@WebParam(name = "filterICCID") String filterICCID) 
    		throws SOAPException 
    {
        //Devuelve el estado de una sesion
        
        if (!ServerConfiguration.getInstance().serverRunning()){
            LogHelper.webLog.fatal(" Operation not processed.  Reason:Server stopped ");
            //return -99;
            throw new javax.xml.soap.SOAPException("ASA-Orchestrator server paused");
        }
        
        ManagementService mgmSrv = ManagementService.getInstance();
        return mgmSrv.getSessionStatus(filterMSISDN_t, filterSessionId, filterICCID);
    }

    /**
     * <Web service operation>
     * Guarda los datos de una sesión que se encuentra en memoria, a la base de datos.
     * Si la sesión ya existe en la base de datos, se actualiza el registro.
     * Esta operación no afecta el estado de la sesión en memoria.
     * Implementa: ut_sessionToDB
     * @param sessionId Id de la sesión a persistir
     * @return
     */
    @WebMethod(operationName = "persistSessionData")
    public WsAckRsp persistSessionData(
    		@WebParam(name = "sessionId") String sessionId) 
    				throws SOAPException 
    {
        if (!ServerConfiguration.getInstance().serverRunning()){
            LogHelper.webLog.fatal(" Operation not processed.  Reason:Server stopped ");
            //return -99;
            throw new javax.xml.soap.SOAPException("ASA-Orchestrator server paused");
        }
  
        //Periste session data en la table ASA_SESSIONS:
        ManagementService mgmSrv = ManagementService.getInstance();
        return mgmSrv.persistSessionData(sessionId);
    }

    /**
     * <Web service operation>
     * @deprecated No se utiliza
     * @param sessionId
     * @param logLevel
     * @param moduleName
     * @param moduleId
     * @param logDescription
     * @param logDetail
     * @return
     */
    @WebMethod(operationName = "sessionLog")
    public WsAckRsp sessionLog(@WebParam(name = "sessionId")
    String sessionId, @WebParam(name = "logLevel")
    String logLevel, @WebParam(name = "moduleName")
    String moduleName, @WebParam(name = "moduleId")
    String moduleId, @WebParam(name = "logDescription")
    String logDescription, @WebParam(name = "logDetail")
    String logDetail) throws SOAPException {
        //TODO write your implementation code here:
        if (!ServerConfiguration.getInstance().serverRunning()){
            LogHelper.webLog.fatal(" Operation not processed.  Reason:Server stopped ");
            //return -99;
            throw new javax.xml.soap.SOAPException("ASA-Orchestrator server paused");
        }

        WsAckRsp retVal = new WsAckRsp();
        retVal.resultCode=0L;
        retVal.resultDescription="";
        return retVal;
    }

  
    /**
     * Web service operation
     */
    @WebMethod(operationName = "operation")
    public String operation() {
        //TODO write your implementation code here:
        return null;
    }
}