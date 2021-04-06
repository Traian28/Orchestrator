/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ats_connection.asa.orchestrator.service;

import orc.ORCPortType;
import orc.REQSTATUS;

import com.ats_connection.asa.orchestrator.config.ConnectionManager;
import com.ats_connection.asa.orchestrator.config.ServerConfiguration;
import com.ats_connection.asa.orchestrator.config.Subscriber;
import com.ats_connection.asa.orchestrator.core.Constants;
import com.ats_connection.asa.orchestrator.core.ManagementService;
import com.ats_connection.asa.orchestrator.core.SessionService;
import com.ats_connection.asa.orchestrator.core.SessionStatus;
import com.ats_connection.asa.orchestrator.helper.LogHelper;
import com.ats_connection.asa.orchestrator.response.WsAckRsp;

// Imports de StateMachine
import com.ats_connection.asa.orchestrator.sme.Session.SessionData;
import com.ats_connection.asa.orchestrator.sme.api.SMEFacade;
import com.ats_connection.asa.orchestrator.sme.event.ActivationEvent;
import com.ats_connection.asa.orchestrator.sme.event.EventTag;
import com.ats_connection.asa.orchestrator.sme.exception.MalformedEventException;
import com.ats_connection.asa.orchestrator.sme.helper.Pair;
import com.ats_connection.asa.orchestrator.sme.helper.SearchCriteria;

// Imports de MenuManager
import com.ats.menu_manager.ApiManager;
import com.ats.menu_manager.exception.MenuManagerException;

import java.util.Map;

import javax.jws.WebService;

/**
 *
 * @author pdiaz
 */
@WebService(
		serviceName = "ORC", 
		portName = "ORCPort", 
		endpointInterface = "orc.ORCPortType", 
		targetNamespace = "ORC", 
		wsdlLocation = "WEB-INF/wsdl/SONotif/SONotif.wsdl")
public class SONotif implements ORCPortType {

    private SessionService sessionMgr = null;

    public SONotif() throws Exception {
        LogHelper.webLog.info ("ASA - SONotif API- Initializing");
        
        if (sessionMgr == null){
            sessionMgr = SessionService.getInstance();
        }
    }
    
    /**
     * <WebService operation>
     * Es invocado para confirmar que fue procesado por el SIMOTA alguna de las siguientes operaciones
     *  Activate IMEI Tracking
     *  Activate SimChronize
     *  Activate Sim
     *  InteractUser (Interaccion con el Menu Applet)
     *  Cancel Display
     * 
     * @param smmId Id del SIMOTA
     * @param msisdn MsisdnFake
     * @param transactionId Id de transaccion
     * @param deliveryStatus Estado de entrega.  Se espera orc.REQSTATUS.OK o orc.REQSTATUS.TO_RETRY
     * @param porData
     * @return Devuelve 0 si se puede procesar correctamente
     */
    @Override
    public int asyncACK(int smmId, String msisdn, int transactionId, orc.REQSTATUS deliveryStatus, String message, String porData) 
    {
        LogHelper.webLog.info("SONotif:asyncACK: begin msisdnFake=" + msisdn + ", smmId=" + smmId + ", transactionId=" + transactionId 
        		+ ", deliveryStatus=" + deliveryStatus + ", message="+message+", porData=" + porData);

        // Control de estado del servidor
        if (!ServerConfiguration.getInstance().serverRunning()){
            LogHelper.webLog.fatal(" Operation not processed.  Reason:Server stopped ");
            return -99;
        }

        // Buscar la sesion en el SME
        SMEFacade stateMachine = SMEFacade.getInstance();
        SearchCriteria sc = new SearchCriteria(null, null, msisdn, null);
        // Verificar el estado
        SessionData sd = null; 
        // Reintenta, ya que al ser un evento que tiene que verificar el estado, 
        // puede ser que aun no se haya replicado.
        for (int retries = 3; retries >0; retries--) {
        	sd = stateMachine.getSessionBySearchCriteria(sc);
            if (sd != null) break; 
            try {    
            	// Espera 1 segundo para volver a solicitar datos de la sesion
            	Thread.sleep(1000);
            }
            catch (InterruptedException ex) {
            	LogHelper.webLog.warn("SONotif:asyncACK: sleep Exception: [" + ex.getMessage() + " ].  msisdnFake=" + msisdn, ex );
            }
        }
        
        if (sd == null) {
        	LogHelper.webLog.info("SONotif:asyncACK: Session not found.  msisdnFake=" + msisdn);
        	return 8;
        }
        
        String operationId = "ACK"; 
        if (sd.getStateMachineState().isUserInfo())
        	operationId += "_MENU";
        if (sd.getStateMachineState().isActivation())
        	operationId += "_ACTIVATION";
        
        String resultCode = null;
        String resultDesc = "$CHECKTID:" + transactionId + "";
        String resultData = "";
        
        if (deliveryStatus.equals(REQSTATUS.OK)) {
        	resultCode = "0";
        }
        
		if (deliveryStatus.equals(REQSTATUS.TO_RETRY)) {
			String aux = sd.getAuxiliarData("RETRY", "0");
			try {
				Integer retry = Integer.valueOf(aux);
				if (retry.intValue() > 0)
					resultCode = "RETRY";
				else
					resultCode = "NORETRY";
			}
			catch(NumberFormatException e) {
				LogHelper.webLog.error("Error getting retry counter: "+e.getMessage());
				resultCode = "NORETRY";
			}
        }
		
		if (deliveryStatus.equals(REQSTATUS.TURNED_OFF)) {
			resultCode = "HANDSET_OFF";
		}
		
		if (deliveryStatus.equals(REQSTATUS.FAILED)) {
			resultCode = "ERROR";
		}
		
		WsAckRsp rsp = sessionMgr.operationResult(sd.getID(), sd.getTransactionID(), operationId, resultCode, resultDesc, resultData);
		
		int result = 0;
		if (rsp.resultCode != 0) {
			result = -1;
			LogHelper.webLog.error("Error in operationResult: " + rsp.resultDescription);
		}
		
        return result;
    }

    /**
     * <WebService operation>
     * Es invocado cada vez que un telefono se enciende
     * @param smmId Id del SIMOTA
     * @param msisdnFake msisdn Fake
     * @param imei IMEI
     * @param loci LOCI
     * @return Devuelve 0 si se puede procesar correctamente
     */
    @Override
    public int initialSMS(int smmId, String msisdnFake, String imei, String loci) {
        LogHelper.webLog.info("SONotif:initialSMS:start: msisdnFake=" + msisdnFake 
        		+ ", smmId=" + smmId + ", imei="+ imei + ", loci=" + loci);

        // Control de estado del servidor
        if (!ServerConfiguration.getInstance().serverRunning()){
            LogHelper.webLog.fatal(" Operation not processed.  Reason:Server stopped ");
            return -99;
        }

        // Chequea Banda horaria
        if (!sessionMgr.isNowInBand()) {
            LogHelper.webLog.fatal(" Operation not processed.  Reason:Out of time band. M ");
            int sendTextRsp = sessionMgr.sendDisplayText(null, msisdnFake, 0, ServerConfiguration.getInstance().get("usrmsg_outoftimeband"));
            
            return -98;
        }
        
        // Controlo existencia de suscriptor
        int subscriberCheck = 0;
        Subscriber sb = ConnectionManager.getInstance().getSubscriber(null, msisdnFake, null, null, null);
        //subscriberCheck = sessionMgr.subscriberCheck(null, msisdnFake, sb);
        if (sb == null) subscriberCheck = 1;
        else if (sb.getBlockedStatus().equals("B")) subscriberCheck = 2;
        else if (sb.isExpired()) subscriberCheck = 3;
 
        try {
            if (subscriberCheck == 0) {
            	String imeiDecoded = decodeIMEI(imei);
            	if (imeiDecoded == null) {
            		
            	}
            	
                WsAckRsp rsp = startExec(imeiDecoded, msisdnFake, loci, smmId, sb);

                if (rsp != null) {
                    LogHelper.webLog.info("SONotif:initialSMS: startExec returnCode=" + rsp.resultCode);

                    if (rsp.resultCode == 0) {
                        LogHelper.dbLog ( null, msisdnFake, "HIGH", "INFO","-> initialSMS OK - ","initialSMS (smmId=" + smmId + ", msisdnFake=" + msisdnFake + ", imei=" + imei + ", loci=" + loci + ")"); 
                        return 0;
                    }
                    else {
                        LogHelper.dbLog ( null, msisdnFake, "HIGH", "ERROR","-> initialSMS ERROR - Processing  ","initialSMS (smmId=" + smmId + ", msisdnFake=" + msisdnFake + ", imei=" + imei + ", loci=" + loci + ") ErrorCode="+rsp.resultCode); 
                        return 106;
                    }
                }
                else{
                        LogHelper.dbLog ( null, msisdnFake, "HIGH", "ERROR","-> initialSMS ERROR - Processing  ","initialSMS (smmId=" + smmId + ", msisdnFake=" + msisdnFake + ", imei=" + imei + ", loci=" + loci + ") StartExec Error"); 
                        return 191;
                }
            }
            else {
                WsAckRsp rsp = new WsAckRsp();
                LogHelper.webLog.error("SONotif:initialSMS: Subscriber Not Found msisdn=" + msisdnFake + " check=" + subscriberCheck);
                rsp.resultCode=101L;
                rsp.resultDescription="Subscriber not found.";
                return 101;
            }
        }
        catch (Exception e) {
            LogHelper.webLog.error("SONotif:initialSMS: Exception msisdn=" + msisdnFake + " check=" + subscriberCheck + " message=" + e.getLocalizedMessage(), e);
            return 990;
        }

    }

    /**
     * <WebService operation>
     * Se invoca cuando se recibe una respuesta del usuario ingresada desde el menuApplet
     * @param smmId
     * @param msisdnFake
     * @param transactionId
     * @param status
     * @param cmdType
     * @param generalResultCode
     * @param generalResultMsg
     * @param choice
     * @return Devuelve 0 si se pudo procesar correctamente el mensaje
     */
    @Override
    public int interactUserAnswer(int smmId, String msisdnFake, int transactionId, orc.HRSGUICMDTYPE cmdType, int generalResultCode, 
    		orc.HRSGUIGENERALRESMSG generalResultMsg, orc.HRSGUICHOICE choice) 
    {
        LogHelper.webLog.info("SONotif:interactUserAnswer:start: msisdnFake=" + msisdnFake + ", smmId= " + smmId + 
        		", transactionId= " + transactionId + ", cmdType= " + cmdType + ", generalResultCode= " + generalResultCode + 
        		", generalResultMsg= " + generalResultMsg + ", choice= [Input:" + choice.getGInputValue().getText() + ", Index: "+ choice.getSItemValue().getIndex() + "]");
        
        // Control de estado del servidor
        if (!ServerConfiguration.getInstance().serverRunning()) {
            LogHelper.webLog.fatal(" Operation not processed.  Reason:Server stopped ");
            return -99;
        }

        ApiManager mma = ApiManager.getInstance();
        if (mma == null) {
            LogHelper.webLog.fatal("SONotif:interactUserAnswer: Can't instanciate MenuManager Module.");
            return 109;  // No se puede instanciar Menu Manager
        }
        
        SMEFacade sme = SMEFacade.getInstance();
        if (sme == null) {
            LogHelper.webLog.fatal("SONotif:interactUserAnswer: Can't instanciate SME Module.");
            return 104;  // No se puede instanciar SME
        }

        String userChoice = null;
        boolean generateNextMenu = false;
        String serviceCode = null;
        String OrchId = ServerConfiguration.getInstance().getServerId();
        
        SearchCriteria search = new SearchCriteria(null, null, msisdnFake, null);
        SessionData sd = sme.getSessionBySearchCriteria(search);
        if (sd != null) {
            
            LogHelper.dbLog(null, msisdnFake, "HIGH", "INFO", "-> interactUserAnswer", "interactUserAnswer (smmId=" + smmId + ", msisdnFake=" + msisdnFake + ", transactionId=" + transactionId + ", cmdType=" + cmdType + ", generalResultCode=" + generalResultCode + ", genRMsg=" + generalResultMsg + ", choice= [Input:" + choice.getGInputValue().getText() + ", Index: "+ choice.getSItemValue().getIndex() + "]");

            // Controlar Transaction Id
            if (sd.getTransactionID() > transactionId) {
            	LogHelper.dbLog(null, msisdnFake, "HIGH", "WARNING", "<- interactUserAnswer Response Discarded", "interactUserAnswer Response Discarded, expired TransactionId (smmId=" + smmId + ", msisdnFake=" + msisdnFake + ", transactionId=" + transactionId + ", cmdType=" + cmdType + ", generalResultCode=" + generalResultCode + ", genRMsg=" + generalResultMsg + ", choice= [Input:" + choice.getGInputValue().getText() + ", Index: "+ choice.getSItemValue().getIndex() + "]");
                return 3;
            }
            // Agregado ticket 46463
            if (!sd.getStateMachineState().isUserInfo()) {
            	LogHelper.dbLog(null, msisdnFake, "HIGH", "WARNING", "<- interactUserAnswer Response Discarded", "interactUserAnswer Response Discarded, State different from WAITING_FOR_USER_INPUT (smmId=" + smmId + ", msisdnFake=" + msisdnFake + ", transactionId=" + transactionId + ", cmdType=" + cmdType + ", generalResultCode=" + generalResultCode + ", genRMsg=" + generalResultMsg + ", choice= [Input:" + choice.getGInputValue().getText() + ", Index: "+ choice.getSItemValue().getIndex() + "]");
                return 3;
            }

            LogHelper.webLog.info("SONotif:interactUserAnswer: deserializing response:[" + sd.getAuxiliarData("MENU_RESPONSE")+"]" );
            
            switch(generalResultCode) {
	            case 0:
	            case 1:
	            case 2:
	            case 4:
	            	if (cmdType.equals(orc.HRSGUICMDTYPE.GET_INPUT)) {
	            		LogHelper.webLog.debug("SONotif:interactUserAnswer: [cmdType=getInput] msisdnFake=" + msisdnFake);
	            		userChoice = choice.getGInputValue().getText();
	            		generateNextMenu = true;
	            	}
	            	if (cmdType.equals(orc.HRSGUICMDTYPE.SELECT_ITEM)) {
	            		LogHelper.webLog.debug("SONotif:interactUserAnswer: [cmdType=select_item] msisdnFake=" + msisdnFake);
	            		userChoice = new Integer(choice.getSItemValue().getIndex()).toString();
	            		generateNextMenu = true;
	            	}
	            	if (cmdType.equals(orc.HRSGUICMDTYPE.DISPLAY_TEXT)) {
	            		LogHelper.webLog.debug("SONotif:interactUserAnswer: [cmdType=display_text] msisdnFake=" + msisdnFake);
	            	}
	            	if (cmdType.equals(orc.HRSGUICMDTYPE.PROVIDE_LOCAL_INFO)) {
	            		LogHelper.webLog.debug("SONotif:interactUserAnswer: [cmdType=provide_local_info] msisdnFake=" + msisdnFake);
	            	}
	            	break;
            	case 12:
            	case 20:
            		// Timeout - Reintentar
            		// Controla si no llego al maximo de reintentos 
            		String numstr = sd.getAuxiliarData("MENUCOUNTER");
            		if (numstr == null)
            			numstr = "0";
            		Integer iVal = 0; 
            		try {    
            			iVal = Integer.parseInt(numstr) ;
            		}
            		catch(Exception e) { 
            			iVal=0; 
            		} 
            		
            		Integer maxCounter=5;
            		try{
            			maxCounter = Integer.parseInt(ServerConfiguration.getInstance().get("menu_retry_counter"));
            		}
            		catch(Exception e) {
            			maxCounter=5;
            		}

            		if (iVal < maxCounter) {
            			// Aun podemos reintentar
            			// Incrementa TransactionId
            			// En el retry de InteractUser hay que incrmentar TrId porque pasamos por el updateMenuInfo que la incrementa en la sesion
            			try{
            				serviceCode = ServerConfiguration.getInstance().get("serviceCode"); 
            			}
            			catch(Exception e) {
            				LogHelper.webLog.error("SONotif:interactUserAnswer: Exception getting serviceCode. SessionId=" + sd.getID() + "msisdnFake=" + msisdnFake + " Exception:" + e, e);
            			}
            			try {
							mma.retryMenu(OrchId, msisdnFake, sd.getID(), transactionId, sd.getAuxiliarData("MENU_MESSAGE"));
						} 
            			catch (MenuManagerException e) {
            				LogHelper.webLog.error("SONotif:interactUserAnswer: Error calling MenuManager: "+e.getMessage());
							sessionMgr.operationResult(sd.getID(), sd.getTransactionID(), "MENU", "INTERNAL_ERROR", e.getMessage(), "0");
							return 4;
						}
            			//Fin incremento TrId
            			
            			// Incrementar counter
           				sessionMgr.operationResult(sd.getID(), sd.getTransactionID(), "MENU", "RETRY", "Generated by SONotif.interactUserAnswer", "0");

            			LogHelper.webLog.info("SONotif:interactUserAnswer: generalResultCode=" + generalResultCode + " msisdnFake=" + msisdnFake);
            		}
            		else {
            			// Ya no reintentamos mas
            			LogHelper.webLog.debug("SONotif:interactUserAnswer: [Retries Exceeded --> End] msisdnFake=" + msisdnFake);
            			sessionMgr.operationResult(sd.getID(), sd.getTransactionID(), "MENU", "NO_RETRY", "Generated by SONotif.interactUserAnswer", "0");
            		}
            		break;
            	case 11:
            		// El usuario presiona "Volver"
					try {
						mma.sendNotification(OrchId, sd.getID(), msisdnFake, sd.getTransactionID().intValue(), ServerConfiguration.getInstance().get("usrmsg_restart"));
					} catch (MenuManagerException e1) {
						LogHelper.webLog.error("Error sending message to subscriber: "+e1.getMessage());
					}
					
            		sessionMgr.operationResult(sd.getID(), sd.getTransactionID(), "MENU", "BACK", "Generated by SONotif.interactUserAnswer", "0");
            		break;
            	case 10:
            		// El usuario presiona "Salir"
            		// En este caso, hay que cancelar la sesion mandando un operationResult
            		sessionMgr.operationResult(sd.getID(), transactionId, "MENU", "QUIT", "User chose Quit option", null);
            		break;
            	case 36:
            		// El usuario respondio un GET_INPUT sin datos o vacio
            		// Reenviamos el menu
            		try {
						mma.retryMenu(OrchId, msisdnFake, sd.getID(), sd.getTransactionID(), sd.getAuxiliarData("MENU_MESSAGE"));
					} 
        			catch (MenuManagerException e) {
        				LogHelper.webLog.error("SONotif:interactUserAnswer: Error calling MenuManager: "+e.getMessage());
						sessionMgr.operationResult(sd.getID(), sd.getTransactionID(), "MENU", "INTERNAL_ERROR", e.getMessage(), "0");
						return 4;
					}
            		break;
            	default:
            		sessionMgr.operationResult(sd.getID(), sd.getTransactionID(), "MENU", "ERROR", generalResultMsg.toString(), "0");
            }
            
            if (generateNextMenu) {
            	String gatewayId = sd.getAuxiliarData("UGTWID");
            	try{
    				serviceCode = ServerConfiguration.getInstance().get("serviceCode"); 
    			}
    			catch(Exception e) {
    				LogHelper.webLog.error("SONotif:interactUserAnswer: Exception getting serviceCode. SessionId=" + sd.getID() + " msisdnFake=" + msisdnFake + " Exception: " + e, e);
    			}
            	
                try {
					mma.nextMenu(OrchId, msisdnFake, sd.getID(), transactionId, serviceCode, gatewayId, userChoice);
				} catch (MenuManagerException e) {
					LogHelper.webLog.error("SONotif:interactUserAnswer: Exception calling nextMenu. SessionId=" + sd.getID() + " msisdnFake=" + msisdnFake + " Exception: " + e);
				}
            }
        } 
        else {
        	// Si no encuentra la sesion deberia enviar un mensaje pidiendo que reinicie el celular
        	if (cmdType.equals(orc.HRSGUICMDTYPE.GET_INPUT) || cmdType.equals(orc.HRSGUICMDTYPE.SELECT_ITEM)) {
        		// Solamente si la respuesta es un ingreso de datos o seleccion de opcion
        		try {
        			mma.sendNotification(OrchId, "not_found", msisdnFake, 0, ServerConfiguration.getInstance().get("usrmsg_no_session"));
        		} catch (MenuManagerException e) {
        			LogHelper.webLog.error("SONotif:interactUserAnswer: Exception calling sendNotification. SessionId=not found; msisdnFake=" + msisdnFake + " Exception: " + e);
        		}
        	}
        	
        }
        
        return 0;
    }

    
    /**
     * ActivationAnswer
     * Se recibe la invocacion cuando el telefono se reinicia y fue exitosamente activado.
     * Llega como parametro sl msisdnReal.
     * @param smmId
     * @param msisdnReal
     * @param transactionId
     * @param iccid
     * @return
     */
    @Override
    public int activationAnswer(int smmId, String msisdnReal, int transactionId, String iccid) {
        // Esta operacion es llamada desde el SIMOTA cuando el telefono se
        // pudo reprogramar y activar en el a red del operador con los nuevos
        // parametros.
        
        // Tiene que buscar la sesion y generar un operation result para
        // operation: AWAIT_CONFIRMATION
        // respcode: 0
        
        LogHelper.webLog.info("SONotif:activationAnswer: Begin. smmId=" + smmId 
        		+ " msisdnReal=" + msisdnReal + " iccid="+ iccid + " transactionId=" + transactionId);

        // Control de estado del servidor
        if (!ServerConfiguration.getInstance().serverRunning()){
            LogHelper.webLog.fatal(" Operation not processed.  Reason:Server stopped ");
            return -99;
        }
        
        ActivationEvent event = null;
        
        String tmp = iccid;
        if (iccid.endsWith("F"))
        	tmp = iccid.substring(0, iccid.length() - 1);
        
        SessionData sd = sessionMgr.searchSessionByIccid(tmp);
        
        if (sd == null) {
            // No se encontro una sesion sobre la cual aplicar el activationAnswer
            // Buscar si no esta persistida
            SearchCriteria search;
            ManagementService managementWs = ManagementService.getInstance();
            SessionStatus sessionStatus = managementWs.getSessionStatus(null, null, iccid);
            if (sessionStatus != null) {
                LogHelper.webLog.info("activationAnswer: Session obtained. status: " + sessionStatus.sesStatus 
                		+ ", finished: "+ sessionStatus.finished+ ", errorStatus: " + sessionStatus.errorStatus 
                		+ ", statusPhase: "+sessionStatus.sesStatusPhase);
            }
            else {
                LogHelper.webLog.error("SONotif:activationAnswer: Session not found.  msisdnReal=" + msisdnReal + " iccid="+ iccid);
                return 100;
            }

            // Vemos si la sesion esta finalizada o no 
            if (sessionStatus.finished.contentEquals("T")) {
            	if (sessionStatus.sesStatusPhase.equals("5")) {
	                // Sesion persistida
	                // Generar Evento: Resume Session Activation Confirmation
	                LogHelper.webLog.debug("activationAnswer: Session persisted found, state: " + sessionStatus.sesStatus);
	
	                try {
	                	EventTag eventTag = EventTag.getResumeSessionActivationConfirmation();
	                    event = new ActivationEvent(eventTag, 0);  
	                    // Carga identificador de sesion en el evento
	                    event.setLOCI(sessionStatus.loci);
	                    event.setMSISDN_FAKE(sessionStatus.msisdn_t);
	                    event.setIMSI_FAKE(sessionStatus.imsi_t);
	                    event.setICCID(sessionStatus.iccid);
	                    event.setAreaCode(sessionStatus.areaCode);
	                    event.setActivationType(sessionStatus.activationType);
	                    event.setIMSI(sessionStatus.imsi_real);
	                    event.setMSISDN(sessionStatus.msisdn_real);
	                    event.setIMSI_ROAMING(sessionStatus.imsi_roaming);
	
	                    event.setVirtualNetwork(sessionStatus.virtualNetwork);
	                    event.setApplication(sessionStatus.subApplication);
	                    event.setOpc(sessionStatus.opc);
	                    event.setOrigin(sessionStatus.origin);
	                    event.setUserType(sessionStatus.userType);
	                    event.setCardType(sessionStatus.cardType);
	                    event.setOpKey(sessionStatus.operatorKey);
	                    
	                    event.addArg("OPERATION", "/");
	                    event.addArg("serverId", ServerConfiguration.getInstance().get("serverId"));
	                    
	                    event.setAuxiliarData("CARDTYPE", String.valueOf(sessionStatus.cardType));
	                    event.setAuxiliarData("VIRTUALNETWORK", String.valueOf(sessionStatus.virtualNetwork));
	                    event.setAuxiliarData("APPLICATION", String.valueOf(sessionStatus.subApplication));
	                    
	                    // BEGIN Agrego parametros auxiliares
	                    if (sessionStatus.auxiliarData != null) {
	                    	int idxa = 0;
	                    	LogHelper.webLog.debug("activationAnswer: Deserializing Session AuxiliarData...");
	                        for(Map.Entry<String, String> elem : sessionStatus.auxiliarData.entrySet()) {	
	                        	idxa++;
	                             event.setAuxiliarData((String) elem.getKey(),(String)elem.getValue());
	                        }
	                        LogHelper.webLog.debug("activationAnswer: Deserializing Session AuxiliarData finished with " + idxa + "elements.");
	                    }
	                    // END
	
	                    event.addArg("ResumedFrom", sessionStatus.sessionId);
	                    event.setAuxiliarData("ResumedFrom", sessionStatus.sessionId);
	                    event.addArg("ORIGIN_ACTIVATION", sessionStatus.originActivation);
	                    event.setAuxiliarData("ORIGIN_ACTIVATION", sessionStatus.originActivation);
	                    event.setAuxiliarData("SIMRETRY", "20");
	
	                    try {
	                        // Postea evento y devuelve resultado (class WsAckRsp).    
	                        Pair<Integer,String> a = sessionMgr.postStartEvent(SMEFacade.getInstance(),event);
	
	                        if (a == null) {
	                            LogHelper.webLog.error("startExec: postStartEvent returned null.");
	                            return 140;
	                        }
	                        else{
	                            LogHelper.webLog.error("startExec: postStartEvent returned. " + a.getLeft() + ","+a.getRight());
	                        }
	
	                        //Reviso el Pair, si es cero, esta bien
	                        if (a.getLeft().equals(new Integer(0))) {
	                            search = new SearchCriteria(a.getRight().toString(), null, sessionStatus.msisdn_t, iccid);
	                            sd = SMEFacade.getInstance().getSessionBySearchCriteria(search);
	
	                            if (sd != null) {
	                            	LogHelper.dbLog ( sd.getID(), null, "HIGH", "INFO","-> ActivationAnswer - Resume - Session created","sessionId=" + sd.getID()+ ", ICCID="+iccid+", IMEI=, MSISDN_t="+sessionStatus.msisdn_t+", IMSI_t="+sessionStatus.imsi_t+", LOCI="+sessionStatus.loci ); 
	                            }
	                            else {
	                                LogHelper.webLog.error("activationAnswer: new session not created yet.");
	                                return 128;
	                            }
	
	                            managementWs.persistSessionData(sd.getID());  // Persist Session
	
	                            return 0;
	                        }
	                        else {
	                                LogHelper.webLog.error("activationAnswer: postEvent error:"+a.getLeft());
	                                return 116;
	                        }
	                    } 
	                    catch (Exception ex) {
	                        LogHelper.webLog.fatal("activationAnswer: Exception " + ex.getMessage(),ex);
	                        return 152;
	                    }
	                
	                }
	                catch(Exception ex) {
	                    LogHelper.webLog.error("activationAnswer: Exception resuming msisdn:" + sessionStatus.msisdn_t , ex);
	                    return 119;
	                }
            	}
            	else {
            		// La sesion NO esta persistida
            		// TODO
            		return 100;
            	}
            }
            else {
            	// La sesion NO esta finalizada
                //LogHelper.webLog.error("SONotif:activationAnswer: Session not found.  msisdnReal=" + msisdnReal + " iccid="+ iccid);
                return 100;
            }
        }
        else {
        	// Encontramos una sesion
        	LogHelper.dbLog ( sd.getID(), null, "HIGH", "INFO","-> activationAnswer","activationAnswer (smmId=" + smmId + " msisdnReal=" + msisdnReal + " iccid="+ iccid + " transactionId=" + transactionId+")"); 
        	
        	// Llamar al operation result
      		LogHelper.dbLog (sd.getID(), null, "HIGH", "INFO", "-> activationAnswer -> operationResult","operationResult (sessionId="+ sd.getID() 
      				+ ", transactionId="+ sd.getTransactionID()+ ", operationId="+  "AWAIT_CONFIRMATION" + ", ResultCode"
      				+ ", ResultDescription"+  "Generated by SONotif:activationAnswer"+ ", ResultData=" + "" + " )");
      		
       		WsAckRsp rsp = sessionMgr.operationResult(sd.getID(), sd.getTransactionID(), "ACTIVATION_ANSWER", "OK", 
       				"Generated by SONotif:activationAnswer", "");
       		
        	return 0;
        }
    }


    /**
     * Genera un evento inicial para crear una nueva maquina de estados para atender la activacion
     * Si ya existe una sesion para el ICCID dado, envia un mensaje inicial a la maquina de estados existente.
     * 
     * @param ICCID
     * @param IMEI
     * @param MSISDN_t es el fake
     * @param IMSI_t es el fake
     * @param LOCI
     * @param smmId es el ID del Orchestrator
     * 
     * @return 
     */
    private WsAckRsp startExec(String IMEI, String MSISDN_t, String LOCI, int smmId, Subscriber sb)
    {
        WsAckRsp retVal = new WsAckRsp();
        retVal.resultCode = 0l;
        retVal.resultDescription = "OK";

        // Buscamos la sesion
        // Si existe hay que enviar un operation result con el evento.
        SessionData sessionData = sessionMgr.searchSessionByIccid(sb.getICCID());
        if (sessionData != null) {
        	LogHelper.dbLog(sessionData.getID(), null, "HIGH", "INFO","-> Start Session - Session already exists", 
        			"sessionId = " + sessionData.getID() + ", stateMachine = "+ sessionData.getStateMachineState().getName()); 
            String operation = "TURN_ON";
            String resultData = sessionData.getLOCI() + "|" + sessionData.getIMEI() + "|" + sessionData.getMSISDN_FAKE() 
            		+ "|" + sessionData.getIMSI_FAKE() + "|" + sessionData.getICCID(); 

            retVal = sessionMgr.operationResult(sessionData.getID(), sessionData.getTransactionID(), operation, "0", "Generated by startExec", resultData);
            LogHelper.webLog.debug("startExec: Session found, state:" + sessionData.getStateMachineState().toString() + " generating operation="+ operation + " with resultData=" + resultData);
            
            return retVal;
        }

        EventTag eventTag = null;
        ActivationEvent event = null;
	    String stateMachineName = null;
	    String stateMachineVersion = null;
        
        SMEFacade sme = SMEFacade.getInstance();
        // Buscamos una sesion para este subscriber
        ManagementService managementWs = ManagementService.getInstance();
        SessionStatus sessionStatus = managementWs.getSessionStatus(MSISDN_t, null, sb.getICCID());
        
        // Si la sesion no existe o existe en memoria
        if (sessionStatus == null || sessionStatus.whereIs.equals("M")) {
        	// Enviamos un evento MO_START
            LogHelper.webLog.debug("startExec: [new session or in memory] Begin iccid=" + sb.getICCID() + ", msisdnFake=" + MSISDN_t );
            
            eventTag = EventTag.getMOSTART();
		    stateMachineName = ServerConfiguration.getInstance().get("MOStateMachineName");
		    stateMachineVersion = ServerConfiguration.getInstance().get("MOStateMachineVersion");
        }
        else {        	
		    stateMachineName = sessionStatus.stateMachineName;
		    stateMachineVersion = sessionStatus.stateMachineVersion;
        	
        	// La sesion existe en la base de datos
            LogHelper.webLog.info("startExec: Session obtained in DB. status:" + sessionStatus.sesStatus + ", finished:"+ sessionStatus.finished + ", errorStatus:" + sessionStatus.errorStatus);
            
            if (sessionStatus.finished.contentEquals("T")) {
            	// Esta finalizada. Analizamos el caso.

        		if (sme.getStateTag(sessionStatus.sesStatus, sessionStatus.stateMachineName, sessionStatus.stateMachineVersion).isFinalError()) {
        			LogHelper.webLog.debug("startExec: Session ended with error found, state:" + sessionStatus.sesStatus);
        			if (sessionStatus.errorStatus != null && !sessionStatus.errorStatus.isEmpty()) {
        				LogHelper.webLog.debug("startExec: Session ended with error found, errorState:" + sessionStatus.errorStatus);
        				if (sme.getStateTag(sessionStatus.errorStatus, sessionStatus.stateMachineName, sessionStatus.stateMachineVersion).isProgramming() ||
        						sme.getStateTag(sessionStatus.errorStatus, sessionStatus.stateMachineName, sessionStatus.stateMachineVersion).isUserInfo()) 
        				{
        					// Si el error fue durante la etapa de programacion entonces mandamos un MO_START
        					eventTag = EventTag.getMOSTART();
        				}
        				else {
        					// Si el error fue durante la activacion
        					// Sesion finalizada con error activando SIM.  No hay mas chances.        
        					LogHelper.webLog.debug("startExec: Session not persisted, checking activating_sim error");
        					
        					retVal.resultCode = 808L;
        					retVal.resultDescription = ServerConfiguration.getInstance().get("usrmsg_err_activating_sim");
        					LogHelper.webLog.debug("startExec: Sending msisdn="+MSISDN_t+": usrmsg_err_activating_sim message");
        					int sendTextRsp = sessionMgr.sendDisplayText(null, MSISDN_t, 0, ServerConfiguration.getInstance().get("usrmsg_err_activating_sim"));
        					
        					return retVal;    
        				}
        			}
        			else {
        				LogHelper.webLog.debug("startExec: Session error status is empty");
        				// La sesion fue cancelada por el usuario, no tiene estado de error.
        				eventTag = EventTag.getMOSTART();
        			}
        		}
        		else if (sme.getStateTag(sessionStatus.sesStatus, sessionStatus.stateMachineName, sessionStatus.stateMachineVersion).isPersist()) {
        			// Esta persistida
        			// Fue persistida, enviamos RESUME
        			// Generar Evento: Resume Session
        			LogHelper.webLog.debug("startExec: Session persisted found, state:" + sessionStatus.sesStatus);
        			eventTag = EventTag.getResumeSessionActivation();
        		}
        		else {
					// Sesion finalizada pero....        
					LogHelper.webLog.debug("startExec: Session ended");
					
					// Si la sesion termino en el menu, antes de comenzar la programacion
					// entonces iniciamos una sesion, sino devolvemos un error y le enviamos un mensaje
					// al usuario
					if (sme.getStateTag(sessionStatus.sesStatus, sessionStatus.stateMachineName, sessionStatus.stateMachineVersion).isUserInfo()) {
						eventTag = EventTag.getMOSTART();
					}
					else {
						retVal.resultCode = 808L;
						retVal.resultDescription = ServerConfiguration.getInstance().get("usrmsg_err_activating_sim");
						LogHelper.webLog.debug("startExec: Sending msisdn="+MSISDN_t+": usrmsg_err_activating_sim message");
						int sendTextRsp = sessionMgr.sendDisplayText(null, MSISDN_t, 0, ServerConfiguration.getInstance().get("usrmsg_err_activating_sim"));
						
						return retVal;
					}
        		}
            }
            else {
            	// Esta en la base pero NO esta finalizada.
            	eventTag = EventTag.getMOSTART();
            }
        }
        
    	try {
			event = new ActivationEvent(eventTag, 0); 
			
			// Carga identificador de sesion en el evento
			if (eventTag.isResumeActivation()) {
				// Es un resume session
				event.setICCID(sessionStatus.iccid);
				event.setMSISDN_FAKE(sessionStatus.msisdn_t);
				event.setIMSI_FAKE(sessionStatus.imsi_t);
				event.setIMEI(sessionStatus.imei);
				event.setLOCI(sessionStatus.loci);
				event.setIMSI(sessionStatus.imsi_real);
				event.setMSISDN(sessionStatus.msisdn_real);
				event.setIMSI_ROAMING(sessionStatus.imsi_roaming);
				event.setAreaCode(sessionStatus.areaCode);
				event.setActivationType(sessionStatus.activationType);
	    		event.setVirtualNetwork(sessionStatus.virtualNetwork);
	    		event.setApplication(sessionStatus.subApplication);
	    		event.setOpc(sessionStatus.opc);
	    		event.setOrigin(sessionStatus.origin);
	    		event.setUserType(sessionStatus.userType);
	    		event.setCardType(sessionStatus.cardType);
	    		event.setOpKey(sessionStatus.operatorKey);
				
				event.addArg("OPERATION", sessionStatus.auxiliarData.get("OPERATION"));
				event.addArg("serverId",ServerConfiguration.getInstance().get("serverId"));
				
				int idxa=0;
				// BEGIN Agrego parametros auxiliares
				LogHelper.webLog.debug("startExec: Deserializing Session AuxiliarData...");
				if (sessionStatus.auxiliarData != null) {
					for (Map.Entry<String, String> elem : sessionStatus.auxiliarData.entrySet()) {
						event.setAuxiliarData((String) elem.getKey(),(String)elem.getValue());
						if (elem.getKey().equals("CARDTYPE")) {
							event.setCardType((String) elem.getValue());
						} else if (elem.getKey().equals("VIRTUALNETWORK")) {
							event.setVirtualNetwork(Long.parseLong((String) elem.getValue()));
						} else if (elem.getKey().equals("APPLICATION")) {
							event.setApplication(Long.parseLong((String) elem.getValue()));
						}
						idxa++;
					}
					LogHelper.webLog.debug("startExec: Deserializing Session AuxiliarData finished with " + idxa + "elements.");
				}
				// END
				event.addArg("ResumedFrom", sessionStatus.sessionId);
				event.setAuxiliarData("ResumedFrom", sessionStatus.sessionId);
				event.addArg("ORIGIN_ACTIVATION", sessionStatus.originActivation);
				event.setAuxiliarData("ORIGIN_ACTIVATION", sessionStatus.originActivation);
				event.setAuxiliarData("RETRY", "10");
			}
			else {
				// Es un mo start
				event.setLOCI(LOCI);
	            event.setIMEI(IMEI);
	            event.setMSISDN_FAKE(MSISDN_t);
	            event.setIMSI_FAKE(sb.getIMSI_t());
	            event.setICCID(sb.getICCID());
	            
	            event.setVirtualNetwork(sb.getVirtualNetwork());
	            event.setApplication(sb.getSubApplication());
	            event.setOpc(sb.getOpc());
	            event.setOrigin(sb.getOrigin());
	            event.setUserType(sb.getUserType());
	            event.setCardType(sb.getCardType());
	            event.setOpKey(sb.getOperatorKey());
	       	    
	            event.setAuxiliarData("CARDTYPE", String.valueOf(sb.getCardType()));
	            event.setAuxiliarData("VIRTUALNETWORK", String.valueOf(sb.getVirtualNetwork()));
	            event.setAuxiliarData("APPLICATION", String.valueOf(sb.getSubApplication()));
			}
		}
		catch(MalformedEventException mee) {
			retVal.resultCode = 19L;
			retVal.resultDescription = "Exception creating event "+eventTag.getName()+": " + mee;
			LogHelper.webLog.error("Exception creating event "+eventTag.getName()+": " , mee);
			return retVal;
		}

		// Add StateMachine Name and version
		event.addArg("StateMachineName", stateMachineName);
		event.addArg("StateMachineVersion",stateMachineVersion);
    	
        try {
            // Postea evento y devuelve resultado (class WsAckRsp).    
            // En a tenemos el codigo de error (left) y el identificador de la sesion (rigth)
            Pair<Integer,String> a = SessionService.getInstance().postStartEvent(sme, event);

            if (a == null) {
                LogHelper.webLog.error("startExec: postStartEvent returned null.");
                retVal.resultCode = Constants.POST_EVENT_ERROR;
                retVal.resultDescription = Constants.POST_EVENT_ERROR_DESC + ". Return null";
                return retVal;
            }
            else{
                LogHelper.webLog.debug("startExec: postStartEvent returned (" + a.getLeft() + "," + a.getRight() + ")");
            }

            // Reviso el Pair, si es cero, esta bien
            if (a.getLeft().equals(new Integer(0))) {
            	SearchCriteria search = new SearchCriteria(a.getRight().toString(), sb.getIMSI_t(), MSISDN_t, sb.getICCID());
                sessionData = sme.getSessionBySearchCriteria(search);

                if (sessionData != null) {
                	LogHelper.dbLog(sessionData.getID(), null, "HIGH", "INFO", 
                			"-> Start Session - Session created","sessionId=" + sessionData.getID()+ ", ICCID="+sb.getICCID()
                			+", IMEI="+IMEI+", MSISDN_t="+MSISDN_t+", IMSI_t="+sb.getIMSI_t()+", LOCI="+LOCI ); 

                    retVal.resultCode = Constants.OK;
                    retVal.resultDescription = "SessionId: " + sessionData.getID();
                    LogHelper.webLog.error("startExec: new session created "+sessionData.getID()+" try to persist");
                    // Persist Session
                    managementWs.persistSessionData(sessionData.getID());
                }
                else {
                    retVal.resultCode = Constants.POST_EVENT_ERROR;
                    retVal.resultDescription = Constants.POST_EVENT_ERROR + ". Session not created yet.";
                    LogHelper.webLog.error("startExec: new session not created yet.");
                }
            }
            else {
                    retVal.resultCode = Constants.SESSION_CREATION_ERROR;
                    retVal.resultDescription = Constants.SESSION_CREATION_ERROR_DESC;
                    LogHelper.webLog.error("startExec: postEvent error: " + a.getLeft());
            }
        } 
        catch (Exception ex) {
            LogHelper.webLog.fatal("startExec: Exception " + ex.getMessage(),ex);
            retVal.resultCode = Constants.POST_EVENT_ERROR;
            retVal.resultDescription = Constants.POST_EVENT_ERROR_DESC + ". Unknown error.";
        }

        return retVal;
    }

    /**
     * Dado un IMEI recibido desde la red, retorna el mismo IMEI decodificado.
     * Se debe calcular el digito verificador con la formula de Luhn.
     * El IMEI viene con un digito A y los pares de nibles invertidos. 
     * Ejemplo: 8A46220402007109  -->  864224020001799 
     * @param imei
     * @return
     */
    private String decodeIMEI(String imei) {
    	StringBuffer result = new StringBuffer();
    	
    	if (imei == null || imei.isEmpty()) {
    		LogHelper.webLog.error("IMEI is null or empty");
    		return null;
    	}
    	
    	// Primero normalizamos el IMEI
    	result.append(imei.charAt(0));
    	for(int i = 2; (i + 1) < imei.length(); i = i + 2) {
    		result.append(imei.charAt(i + 1)); 
    		result.append(imei.charAt(i));
    	}
    	
    	// Ahora calculamos el digito verificador con el algoritmo de Luhn
    	// Comenzando por el segundo digito del extremo derecho, se deben duplicar cada 2 digitos,
    	// es decir, duplico uno, el siguiente queda como es, el siguiente duplico, y asi
    	int imeiLen = result.length();
    	int[] doble = new int[imeiLen];
    	for(int j = imeiLen; j > 0; j--) {
    		int pos = j - 1;
    		int digito = 0;
    		if (Character.isDigit(result.charAt(pos))) {
    			digito = result.charAt(pos) - '0';
    			if (((imeiLen - j) % 2) == 0)
    				doble[pos] = digito;
    			else
    				doble[pos] = digito * 2;
    		}
    		else {
    			LogHelper.webLog.error("IMEI have a non decimal digit: "+result.toString());
    			return null;
    		}
    	}
    	
    	int suma = 0;
    	// Ahora se deben sumar los digitos. Si alguno es mayor que 10, se deben sumar esos digitos por separado
    	for(int k = 0; k < imeiLen; k++) {
    		if (doble[k] > 9) {
    			String valor = String.valueOf(doble[k]);
    			for(int p = 0; p < valor.length(); p++)
    				suma += valor.charAt(p) - '0';
    		}
    		else
    			suma += doble[k];
    	}
    	
    	int checkDigit = (suma * 9) % 10;
    	
    	String imeiDecoded = result.substring(0, imeiLen - 1) + checkDigit;
    	
    	return imeiDecoded;
    }
}
