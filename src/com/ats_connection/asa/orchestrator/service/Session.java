package com.ats_connection.asa.orchestrator.service;

import com.ats_connection.asa.orchestrator.core.*;
import com.ats_connection.asa.orchestrator.config.*;
import com.ats_connection.asa.orchestrator.helper.*;
import com.ats_connection.asa.orchestrator.response.WsAckRsp;

// Imports de StateMachine
import com.ats_connection.asa.orchestrator.sme.Session.SessionData;
import com.ats_connection.asa.orchestrator.sme.api.SMEFacade;
import com.ats_connection.asa.orchestrator.sme.event.ActivationEvent;
import com.ats_connection.asa.orchestrator.sme.helper.Pair;
import com.ats_connection.asa.orchestrator.sme.helper.SearchCriteria;

// Imports de MenuManager
import com.ats.menu_manager.ApiManager;
import com.ats.menu_manager.exception.MenuManagerException;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.soap.SOAPException;

/**
 *
 * @author pdiaz
 */
@WebService()
public class Session {

	// Instancia el lector de configuración del parametros del server
	// (server.properties)
	private ServerConfiguration srvConfig;
	private SessionService sessionSrv;

	// Archivo de traducciones
	public static Translation translation = null;

	// Definición de los nombres de archivos par configuración.
	private String i18nFile = "i18n.properties";

	/**
	 * Constructor de la clase sesión
	 */
	public Session() throws Exception {
		LogHelper.webLog.info("ASA - Session API - Initializing");

		if (translation == null) {
			LogHelper.webLog.info("ASA - Session API - Initializing Translation");
			translation = new Translation(i18nFile);
			LogHelper.webLog.info("ASA - Session API - Translation Initialized succesfully");
		}

		if (srvConfig == null) {
			LogHelper.webLog.info("ASA - Session API - Instancing server configuration");
			srvConfig = ServerConfiguration.getInstance();
			if (srvConfig != null)
				LogHelper.webLog.info("ASA - Session API - Configuration serverId=" + srvConfig.getServerId());
			else
				LogHelper.webLog.fatal("ASA - Session API - Error instanciating Server Configuration");

		}
		LogHelper.webLog.info("ASA - Session API - First Time Instancing singleton: PostEventConfiguration");
		PostEventConfiguration.getInstance(); // Ejecuto el constructor del PostEventConfiguration para evitar
												// su construcción con la primer transacción del cliente

		sessionSrv = SessionService.getInstance();
	}

	/**
     * Web service operation: operationResult
     * Lee la configuracion del Map en memoria, genera con la informacion obtenida el siguiente evento y 
     * lo postea a la maquina de estados.
     * 
     * @param sessionId Id de la sesion.  Se utiliza para buscar la sesion y postear el evento
     * @param transactionId Se incrementa y se envia dentro del evento como el next transactionId
     * @param operationId Nombre de la operacion.  Se utiliza como clave de busqueda dentro del Map
     * @param resultCode Codigo de respuesta.  Se utiliza como clave de busqueda dentro del Mao
     * @param resultDescription Descripcion de respuesta. Se en como parametro DESCRIPTION
     * @param resultData Datos de resultado de la operacion.  Se analiza y se utiliza para completar datos del evento y actualizar la sesion.
     * 
     * @return resultCode =0 para indicar que la llamada a la operacion se ejecuto exitosamente.
     */
    @WebMethod(operationName = "operationResult")
    public WsAckRsp operationResult(
    		@WebParam(name = "sessionId") String sessionId, 
    		@WebParam(name = "transactionId") Integer transactionId, 
    		@WebParam(name = "operationId") String operationId, 
    		@WebParam(name = "resultCode") String resultCode, 
    		@WebParam(name = "resultDescription") String resultDescription, 
    		@WebParam(name = "resultData") String resultData) throws SOAPException 
    {
        LogHelper.webLog.info("\nASA - WS:SessionService:operationResult - Begin (sessionId="+ sessionId +", transactionId="+ transactionId+ ", operationId="+operationId+", resultCode="+resultCode+", resultDescription="+ resultDescription+", resultdata="+resultData+")");

        if (!ServerConfiguration.getInstance().serverRunning()){
            LogHelper.webLog.fatal(" Operation not processed.  Reason:Server stopped ");
            //return -99;
            throw new javax.xml.soap.SOAPException("ASA-Orchestrator server paused");
        }

        //LogHelper.dbLog(sessionId, null, "HIGH", "INFO", "-> operationResult (" + operationId +", "+ resultCode+")",
        //        "operationResult( sessionId="+ sessionId +", transactionId="+ transactionId+ ", operationId="+operationId+", resultCode="+resultCode+", resultDescription="+ resultDescription+", resultdata="+resultData+")" ); 
        
        if (transactionId > 500) {
            LogHelper.webLog.warn("ASA - WS:SessionService:operationResult - Check warning transactionId too high(" + transactionId + " (sessionId:" + sessionId + ", operationId:" + operationId +", resultCode:" + resultCode + ")");
        }
               
        // Obtiene instancia del ejecutor (singleton) de la máquina de estados
        LogHelper.webLog.debug("ASA - WS:SessionService:operationResult - GetSMInstance" + " (sessionId:" + sessionId +")");

        WsAckRsp retVal = new WsAckRsp();
        SMEFacade stateMachine = SMEFacade.getInstance();

        if (stateMachine == null) {
            LogHelper.webLog.error("ASA - WS:SessionService:operationResult - Error Getting Instance" + " (sessionId:" + sessionId +")");
            retVal.resultCode = Constants.SM_NOT_FOUND;
            retVal.resultDescription = Constants.SM_NOT_FOUND_DESC;
        }
        else {       	
            if ((sessionId == null || sessionId.isEmpty() || sessionId.contentEquals("-")) && (resultData != null)) {
            	String parameters[] = resultData.split("\\|");
            	String iccid 		= parameters[0]; if (iccid.isEmpty() || iccid.contentEquals("-")) iccid = null;
                String imsi_fake 	= parameters[1]; if (imsi_fake.isEmpty() || imsi_fake.equals("-")) imsi_fake = null;
                String msisdn_fake 	= parameters[2]; if (msisdn_fake.isEmpty() || msisdn_fake.equals("-")) msisdn_fake = null;
                String imsi_real 	= parameters[3]; if (imsi_real.isEmpty() || imsi_real.equals("-")) imsi_real = null;
                String msisdn_real 	= parameters[4]; if (msisdn_real.isEmpty() || msisdn_real.equals("-")) msisdn_real = null;
                
                SearchCriteria searchCriteria = new SearchCriteria(null, imsi_fake, msisdn_fake, iccid, imsi_real, msisdn_real);
                SessionData sessionData = stateMachine.getSessionBySearchCriteria(searchCriteria);
                if (sessionData != null) {
                	sessionId = sessionData.getID();
                	transactionId = sessionData.getTransactionID() + 1;
                }
            }
        	
       		retVal = sessionSrv.operationResult(sessionId, transactionId, operationId, resultCode, resultDescription, resultData);
        }
        
        return retVal;
    }

	/**
	 * Invoca al MenuManager
	 * 
	 * @param sessionId     ID de la sesion
	 * @param transactionId ID de transaccion
	 * @param IMSI_t        IMSI fake
	 * @param MSISDN_t      MSISDN fake
	 * @param ICCID
	 * @param menuCode      indica que hacer con MenuManager (first, next, retry)
	 * @param menuOptions   se usa para retry, contiene el ultimo envio a OTA
	 * 
	 * @return
	 * 
	 * @throws SOAPException
	 */
	@WebMethod(operationName = "sendMenu")
	public WsAckRsp sendMenu(@WebParam(name = "sessionId") String sessionId,
			@WebParam(name = "transactionId") Integer transactionId, @WebParam(name = "IMSI_t") String IMSI_t,
			@WebParam(name = "MSISDN_t") String MSISDN_t, @WebParam(name = "ICCID") String ICCID,
			@WebParam(name = "menuCode") String menuCode, @WebParam(name = "menuOptions") String menuOptions)
			throws SOAPException {
		if (!ServerConfiguration.getInstance().serverRunning()) {
			LogHelper.webLog.fatal(" Operation not processed.  Reason:Server stopped ");
			// return -99;
			throw new javax.xml.soap.SOAPException("ASA-Orchestrator server paused");
		}

		LogHelper.webLog.info("SendMenu: Begin (" + "sessionId=" + sessionId + ",transactionId=" + transactionId
				+ ",IMSI_t=" + IMSI_t + ",MSISDN_t=" + MSISDN_t + ",ICCID=" + ICCID + ",menuCode=" + menuCode
				+ ",menuOptions=" + menuOptions);

		WsAckRsp resp = new WsAckRsp();
		resp.resultCode = Constants.OK;
		resp.resultDescription = "OK";

		SMEFacade stateMachine = SMEFacade.getInstance();

		if (stateMachine == null) {
			LogHelper.webLog.error(
					"ASA - WS:SessionService:sendMenu - Error Getting Instance" + " (sessionId:" + sessionId + ")");
			resp.resultCode = 24L;
			resp.resultDescription = "Error getting SM Instance.";
		} else {
			SearchCriteria sc = new SearchCriteria(sessionId, null, null, null);
			SessionData sd = stateMachine.getSessionBySearchCriteria(sc);

			if (sd != null) {
				boolean done = false;
				// La sesion existe
				LogHelper.dbLog(sessionId, null, "LOW", "INFO", "--> SendMenu:" + menuCode,
						"-> sendMenu (" + "sessionId=" + sessionId + ",transactionId=" + transactionId + ",IMSI_t="
								+ IMSI_t + ",MSISDN_t=" + MSISDN_t + ",ICCID=" + ICCID + ",menuCode=" + menuCode
								+ ",menuOptions=" + menuOptions + ")");

				ApiManager menuManager = ApiManager.getInstance();
				try {
					if (menuCode.equals("first")) {
						menuManager.firstMenu(ServerConfiguration.getInstance().getServerId(), MSISDN_t, sessionId,
								transactionId, menuOptions);
						done = true;
					}

//                	if (menuCode.equals("next")) {
//                		menuManager.nextMenu(ServerConfiguration.getInstance().getServerId(),
//                				MSISDN_t, sessionId, transactionId, ServerConfiguration.getInstance().get("serviceCode"), 
//                				gatewayId, response);
//                	}

					if (menuCode.equals("retry")) {
						menuManager.retryMenu(ServerConfiguration.getInstance().getServerId(), MSISDN_t, sessionId,
								transactionId, menuOptions);
						done = true;
					}

					if (done == false) {
						resp.resultCode = -2l;
						resp.resultDescription = "Invalid menuCode :" + menuCode;
					}
				} catch (MenuManagerException e) {
					resp.resultCode = -1l;
					resp.resultDescription = "Error calling MenuManager: " + e.getMessage();
				}
			} else {
				LogHelper.webLog.error(
						"ASA - WS:SessionService:sendMenu - Session Not Found" + " (sessionId:" + sessionId + ")");
				resp.resultCode = Constants.SESSION_NOT_FOUND;
				resp.resultDescription = Constants.SESSION_NOT_FOUND_DESC;
			}
		}

		return resp;
	}

	/**
	 * Realiza el post de un evento y genera la respuesta de posteo de eventos A
	 * diferencia del PostEvent, este llamado es bloqueante hasta que se genera la
	 * sesion, devuelve el Par <codigo de respuesta, SessionId>
	 * 
	 * @param x   Recibe la instancia (singleton) del SMEFacade
	 * @param evt Recibe el evento a postear
	 * @return
	 */
	// Modificada el 27/8/2011 para evitar llamar al metodo bloqueante .start del
	// SME que podría llegar a tener algún problema.

	public static Pair<Integer, String> postStartEvent2(SMEFacade sme, ActivationEvent evt) {

		if (evt != null)
			LogHelper.webLog
					.debug("Session: PostStartEvent sesId:" + evt.getSessionID() + " evt:" + evt.getTag().getName());
		else {
			LogHelper.webLog.debug("Session: PostStartEvent null Event");
			return null;
		}

		try {
			boolean alreadyCreated = false;
			Integer timeoutSecs = Integer.parseInt(ServerConfiguration.getInstance().get("postStartEventTimeout"));
			if (timeoutSecs == null || timeoutSecs == 0) {
				timeoutSecs = 10;
			}

			// En el start si ya existe una sesión con alguno de los datos claves que se
			// envían para
			// su creación, no se crea una nueva, sino que retorna el ID de la sesión
			// existente.
			// Pair
			// rsp=x.start(evt,Integer.parseInt(ServerConfiguration.getInstance().get("postStartEventTimeout")),TimeUnit.SECONDS);
			// // TODO: Timeout configurable

			SessionData rsp = null;
			SearchCriteria sc = new SearchCriteria(null, evt.getIMSI_FAKE(), evt.getMSISDN_FAKE(), evt.getICCID());
			if (sc != null) {
				LogHelper.webLog.info("PostStartEvent:post: iccid=" + evt.getICCID() + " timeout:" + timeoutSecs);
				sme.post(evt);

				while (!alreadyCreated && timeoutSecs > 0) {
					Thread.sleep(1000);
					rsp = sme.getSessionBySearchCriteria(sc);
					timeoutSecs--;

					if (rsp == null) {
						LogHelper.webLog.info("PostStartEvent:searchSession:  iccid=" + evt.getICCID()
								+ " not found. Retries left:" + timeoutSecs);
					} else {
						alreadyCreated = true;
					}
				}
			}

			if (alreadyCreated) {
				if (rsp != null) {
					LogHelper.dbLogFlow(rsp.getID(), null, "HIGHEST", "INFO",
							"[" + rsp.getTransactionID() + "]" + "  Event: " + evt.getTag().toString(), "");
					Pair<Integer, String> p = new Pair<Integer, String>(0, rsp.getID());
					return p;
				} else {
					LogHelper.webLog.fatal("PostStartEvent: alreadyCreated but rsp=null. iccid=" + evt.getICCID());
					return null;
				}
			} else {
				LogHelper.webLog.fatal("PostStartEvent: Could not create session. iccid=" + evt.getICCID());
				return null;
			}
		} catch (Exception e) {
			LogHelper.webLog.error("PostStartEvent: Exception, more data below ", e);
			LogHelper.webLog.error(
					"PostStartEvent: Exception, continued iccid= " + evt.getICCID() + " tagName=" + evt.getTag());
			return null;
		}

	}

	/**
	 * Web service operation
	 */
	@WebMethod(operationName = "displayText")
	public WsAckRsp displayText(@WebParam(name = "sessionId") String sessionId,
			@WebParam(name = "transactionId") Integer transactionId, @WebParam(name = "msisdnFake") String msisdnFake,
			@WebParam(name = "text") String text, @WebParam(name = "config") String config) {
		// TODO write your implementation code here:
		WsAckRsp retVal = new WsAckRsp();
		int r = sessionSrv.sendDisplayText(sessionId, msisdnFake, transactionId, text, config);

		retVal.resultCode = (long) r;
		retVal.resultDescription = "";

		return retVal;
	}

	/**
	 * Web service operation
	 */
	@WebMethod(operationName = "operation")
	public String operation() {
		// TODO write your implementation code here:
		return null;
	}
}