package com.ats_connection.asa.orchestrator.core;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.ats.menu_manager.ApiManager;
import com.ats.menu_manager.exception.MenuManagerException;
import com.ats_connection.asa.orchestrator.config.ConnectionManager;
import com.ats_connection.asa.orchestrator.config.EventCall;
import com.ats_connection.asa.orchestrator.config.PostEventConfiguration;
import com.ats_connection.asa.orchestrator.config.ServerConfiguration;
import com.ats_connection.asa.orchestrator.config.Subscriber;
import com.ats_connection.asa.orchestrator.helper.LogHelper;
import com.ats_connection.asa.orchestrator.helper.Utils;
import com.ats_connection.asa.orchestrator.response.WsAckRsp;

// Imports de StateMachine
import com.ats_connection.asa.orchestrator.sme.Session.SessionData;
import com.ats_connection.asa.orchestrator.sme.api.SMEFacade;
import com.ats_connection.asa.orchestrator.sme.event.ActivationEvent;
import com.ats_connection.asa.orchestrator.sme.event.Event;
import com.ats_connection.asa.orchestrator.sme.event.EventTag;
import com.ats_connection.asa.orchestrator.sme.exception.MalformedEventException;
import com.ats_connection.asa.orchestrator.sme.helper.Pair;
import com.ats_connection.asa.orchestrator.sme.helper.SearchCriteria;

public class SessionService {

	private static SessionService instance = null;
	
	private ServerConfiguration srvConf = null;
	private PostEventConfiguration postEventConf = null;
	
	private SessionService() throws Exception {
		srvConf = ServerConfiguration.getInstance();
		postEventConf = PostEventConfiguration.getInstance();
	}
	
	public static synchronized SessionService getInstance() throws Exception {
		if (instance == null)
			instance = new SessionService();
		
		return instance;
	}
	
	public WsAckRsp operationResult( String sessionId, int transactionId, String operationId, 
    		String resultCode, String resultDescription, String resultData) 
	{
		LogHelper.webLog.debug("params - sessionId="+sessionId+
				", transactionId="+transactionId+
				", operationId="+operationId+
				", resultCode="+resultCode+
				", resultDescription="+resultDescription+
				", resultData="+resultData);
				
		WsAckRsp response = new WsAckRsp();
		SMEFacade sessionManager = SMEFacade.getInstance();
		
		SessionData sessionData = null;
		
		// Vemos si existe la sesion
		if (sessionId.equals("-")) {
            if (resultData != null) {
            	// resultData = iccid+"|"+imsi_fake+"|"+msisdn_fake+"|"+imsi_real+"|"+msisdn_real;
                String parameter[];
            	parameter = resultData.split("\\|");        // Parametros a evaluar
			
            	String iccid = (parameter[0].equals("-") ? null : parameter[0]);
            	String imsi_fake = (parameter[1].equals("-") ? null : parameter[1]);
            	String msisdn_fake = (parameter[2].equals("-") ? null : parameter[2]);
            	String imsi_real = (parameter[3].equals("-") ? null : parameter[3]);
            	String msisdn_real = (parameter[4].equals("-") ? null : parameter[4]);

            	SearchCriteria searchCriteria = new SearchCriteria(null , imsi_fake, msisdn_fake, iccid, imsi_real, msisdn_real);
            	sessionData = sessionManager.getSessionBySearchCriteria(searchCriteria);
            }
		}
		else {
			SearchCriteria searchCriteria = new SearchCriteria(sessionId, null, null, null);
	        sessionData = sessionManager.getSessionBySearchCriteria(searchCriteria);
		}
		
        if (sessionData == null) {
            LogHelper.webLog.error("ASA - WS:SessionService:operationResult - Session not found for [operation,code] [" 
                    + operationId + "," + resultCode + "]" + " (sessionId:" + sessionId +")");
               
        	response.resultCode = Constants.SESSION_NOT_FOUND;
        	response.resultDescription = Constants.SESSION_NOT_FOUND_DESC;
        	return response;
        }
        
		LogHelper.webLog.debug(sessionData.toString());

        // Controlamos el ID de transaccion
        int sessionTrId = sessionData.getTransactionID();
        if (transactionId < 0) {
        	// Todas las sesiones empiezan con transactionId 0,
        	// entonces, si viene negativo es porque es una llamada externa
        	// que desconoce el valor de transactionId. Usamos el que tiene la sesion.
        	transactionId = sessionTrId;
        }
		
        // Buscamos en la configuracion de eventos
        Integer controlFlag = sessionData.getInternalFlag("FLAG");
        if (controlFlag == null) controlFlag = new Integer(-1);
        
        EventCall eventConfig = postEventConf.getEventCall(operationId, resultCode, controlFlag, sessionData.getOriginActivation().getName());
        if (eventConfig == null) {
        	response.resultCode = Constants.EVENT_CONFIG_NOT_FOUND;
            response.resultDescription = Constants.EVENT_CONFIG_NOT_FOUND_DESC;
            
            LogHelper.webLog.error("ASA - WS:SessionService:operationResult - No PostEvent configuration for [operation,code,flag,origin] [" 
                 + operationId + "," + resultCode+ "," + controlFlag + "," + sessionData.getOriginActivation().getName() + "]" + " (sessionId:" + sessionId +")");
            
            LogHelper.dbLog ( sessionId, null, "HIGH", "ERROR","-> operationResult -> postEvent Fail !!","No Post Event Configuration for [operation,code,flag,origin] [" 
                 + operationId + "," + resultCode+ "," + controlFlag + "," + sessionData.getOriginActivation().getName() +  "]"  );
            
            return response;
        }
        else {
        	LogHelper.webLog.debug("EventCall: Name: " + eventConfig.evt_name + 
        			               ", Parameter: " + (eventConfig.evt_params != null ? eventConfig.evt_params : "") + 
        			               ", Next Operation: " + (eventConfig.next_operation_id != null ? eventConfig.next_operation_id : ""));
        }

        // Para el caso de los eventos que pueden llegar luego de 1 incremento del TID producido desde afuera, 
        // para que no se descarten pdd2018
        if ((eventConfig.evt_params!=null) && (!eventConfig.evt_params.isEmpty()) )
        	if (eventConfig.evt_params.contains("$INCTID"))
        		transactionId ++;
        
        //pdd2018 nuevo comando $ADICTID
        int adicTid = 0;
        if ((eventConfig.evt_params!=null) && (!eventConfig.evt_params.isEmpty()) )
        	if (eventConfig.evt_params.contains("$ADICTID"))
        		adicTid = 1;
        if ((eventConfig.evt_params!=null) && (!eventConfig.evt_params.isEmpty()) )
        	if (eventConfig.evt_params.contains("$EQUALTID"))
        		transactionId = sessionTrId;

        
        // Le agrega adicTid para tolerar hasta 1 numeros mas en el valor del TID en caso
        // de que se use la tolerancia con el comando $ADICTID
        
        if (sessionTrId  > transactionId + adicTid) {
            // Esto significa, independientemente de atrasos en la replicacion, que 
            // la respuesta que nos llega es vieja y debemos descartarla
            LogHelper.dbLog(sessionId, null, "HIGH","WARNING", "<- operationResult Discarded (" + operationId +", "+ resultCode+")",
            		"operationResult discarded, Expired transactionId received ( sessionId="+ sessionId +", transactionId="+ transactionId
            		+ ", operationId="+operationId+", resultCode="+resultCode+", resultDescription="+ resultDescription+", resultdata="+resultData+")" ); 

            LogHelper.webLog.warn("operationResult discarded, Expired transactionId received ( sessionId="+ sessionId +", transactionId="
            + transactionId+ ", operationId="+operationId+", resultCode="+resultCode+", resultDescription="+ resultDescription+", resultdata="+resultData+")");
            
            response.resultCode = Constants.TRANSACTION_ID_EXPIRED;
            response.resultDescription = Constants.TRANSACTION_ID_EXPIRED_DESC;
            return response;
        }
        
        
        if (eventConfig.evt_name.contentEquals("/")) { 
        	// No tiene que generar evento
        	response.resultCode = 0l;
        	response.resultDescription = "";
        	
            LogHelper.webLog.debug("ASA - WS:SessionService:operationResult - (sessionId:" + sessionId +", transactionId="
                 + transactionId+ ", operationId="+operationId+", resultCode="+resultCode+", resultDescription="+ resultDescription+", resultdata="+resultData+")" );
            LogHelper.dbLog ( sessionId, null, "HIGH", "INFO","-> operationResult -> No event to post '/'", "No event to post. OK." );
            
            return response;
        }
        
        String eventName = eventConfig.evt_name;
        int incrementTID = 1;
        if (eventConfig.evt_name.startsWith("$/")) {
        	incrementTID = 0;
        	eventName = eventConfig.evt_name.substring(2);
        }
        
        // Creamos el evento
        EventTag eventTag = new EventTag(eventName, Event.Type.ACTIVATION.name());
        ActivationEvent event;
		try {
			event = new ActivationEvent(eventTag, transactionId + incrementTID);
			event.setSessionID(sessionId);
			if (sessionData.getICCID() != null)
	            event.setICCID(sessionData.getICCID());
			if (sessionData.getLOCI() != null)
	            event.setLOCI(sessionData.getLOCI());
			if (sessionData.getMSISDN_FAKE() != null)
	            event.setMSISDN_FAKE(sessionData.getMSISDN_FAKE());
			if (sessionData.getIMEI() != null)
	            event.setIMEI(sessionData.getIMEI());
			if (sessionData.getIMSI() != null)
	            event.setIMSI(sessionData.getIMSI());
			if (sessionData.getMSISDN() != null)
	            event.setMSISDN(sessionData.getMSISDN());
		} 
		catch (MalformedEventException mee) {
			response.resultCode = 0l;
			response.resultDescription = mee.getMessage();
			
			LogHelper.webLog.debug("ASA - WS:SessionService:operationResult- Error creating event");
			LogHelper.dbLog(sessionId, null, "HIGH", "INFO", "ASA - WS:SessionService:operationResult- Error creating event", mee.getMessage());
			
			return response;
		}
    
		// Add StateMachine Name and version
		event.addArg("StateMachineName", sessionData.getStateMachineName());
		event.addArg("StateMachineVersion",sessionData.getStateMachineVersion());
        
        // Carga identificador de sesión en el evento
        event.setApplication(sessionData.getApplication());
        event.setVirtualNetwork(sessionData.getVirtualNetwork());
        event.setOpKey(sessionData.getOpKey());
        event.setOpc(sessionData.getOpc());
        event.setCardType(sessionData.getCardType());
        event.setUserType(sessionData.getUserType());

        if (sessionData.getCardType() != null)
            event.setAuxiliarData("CARDTYPE", sessionData.getCardType());
        else
            event.setAuxiliarData("CARDTYPE", "PRE");
        
        if (sessionData.getVirtualNetwork() != null)
        	event.setAuxiliarData("VIRTUALNETWORK", String.valueOf(sessionData.getVirtualNetwork()));
        else
        	event.setAuxiliarData("VIRTUALNETWORK", "0");
        
        if (sessionData.getApplication() != null)
        	event.setAuxiliarData("APPLICATION", String.valueOf(sessionData.getApplication()));
        else
        	event.setAuxiliarData("APPLICATION", "0");
	
        // Si la proxima operacion es una variable ($...) la parsea. 
        String nextOperationParsed = null;
        if (eventConfig.next_operation_id != null) {
            if (eventConfig.next_operation_id.startsWith("$")){
                if (eventConfig.next_operation_id.contentEquals("$GETOPERATION")) {
                    nextOperationParsed = sessionData.getAuxiliarData("/SETOPERATION");
                }
                else if (eventConfig.next_operation_id.startsWith("$/")) {
                    nextOperationParsed = eventConfig.next_operation_id.substring(2);
                }
            }
            else {
                nextOperationParsed = eventConfig.next_operation_id;
            }
        }
        LogHelper.webLog.debug("ASA - WS:SessionService:operationResult - transforming nextOperation = " + eventConfig.next_operation_id  + " parsed= " + nextOperationParsed + " (sessionId:" + sessionId +")");
        event.addArg("OPERATION", nextOperationParsed);
        event.setAuxiliarData("OPERATION", nextOperationParsed);

       // Verifica el valor de configuración en parámetro y decide como completar el evt
       // antes de postear el evento

       // los parametros recibidos vienen todos en ResultData (string) seperador por el 
        // caracter | (pipe)
        // Los comandos para saber que hacer con le valores de resultData estan en la configuracion(tabla)
        // en el campo PARAMETERS y vienen separados por | (pipe) tambien cuando tienen que analizar mas de un
        // parametro de entrada (resultData)

        int cancelPost = 0;
        if ((eventConfig.evt_params!=null) && (!eventConfig.evt_params.isEmpty()) )
        {
        	String paramPrefix = "PARAMETER";
            String resultDataN=new String("");

            String parameters[];
            if (resultData == null)
            	parameters = new String[] { "" };
            else
            	parameters = resultData.split("\\|");        // Parametros a evaluar
            
            String commands[] = eventConfig.evt_params.split("\\|");   // Comandos para evaluarlos


            int pidx=0;  // índice de parametros por si hay mas de un setter
            for (int cmd=0; cmd < commands.length; cmd++) {
                if (parameters.length > cmd) {
                    resultDataN=parameters[cmd];
                }
                // Sino sigue procesando el anterior resultData
                LogHelper.webLog.debug("ASA - WS:SessionService:operationResult - transforming resultDataN=" + resultDataN  + " with [" + commands[cmd] + "] (sessionId:" + sessionId +")");

                String setters[] = commands[cmd].split("\\,");
                // $LOCI|$IMEI|$MSISDN_FAKE|$IMSI_FAKE|$ICCID
                try {                
                	for (int idx=0; idx < setters.length; idx++)
	                {
	                    if (setters[idx].contentEquals("$MSISDN")) {
	                        event.setMSISDN(resultDataN);
	                    }
	                    else if (setters[idx].contentEquals("$MSISDNCLEAR")) {
	                        event.setMSISDN("");
	                    }
	                    else if (setters[idx].contentEquals("$ICCID")) {
	                    	event.setICCID(resultDataN);
	                    }
	                    else if (setters[idx].contentEquals("$IMEI")) {
	                    	event.setIMEI(resultDataN);
	                    }
	                    else if (setters[idx].contentEquals("$IMSI")) {
	                    	event.setIMSI(resultDataN);
	                    }
	                    else if (setters[idx].contentEquals("$IMSI_FAKE")) {
	                    	event.setIMSI_FAKE(resultDataN);
	                    }
	                    else if (setters[idx].contentEquals("$LOCI")) {
	                    	event.setLOCI(resultDataN);
	                    }
	                    else if (setters[idx].contentEquals("$MSISDN_FAKE")) {
	                    	event.setMSISDN_FAKE(resultDataN);
	                    }
	                    else if (setters[idx].contentEquals("$AREACODE")) {
	                        event.setAreaCode(resultDataN);
	                    }
	                    else if (setters[idx].contentEquals("$ACTTYPE")) {
	                        event.setActivationType(resultDataN);
	                    }
	                    if (setters[idx].contentEquals("$SKIP")) {
	                    }
	                    else if (setters[idx].startsWith("$ACTTYPE:")) {
	                        String[]auxVar = setters[idx].split("\\:");
	                        if (auxVar.length>1)
	                        {
	                            event.setActivationType(auxVar[1]);
	                        }
	                    }
	                    else if (setters[idx].startsWith("$SETAUX:"))
	                    {
	                        String[]auxVar = setters[idx].split("\\:");
	                        if (auxVar.length>1)
	                        {
	                            event.setAuxiliarData(auxVar[1], resultDataN);
	                        }
	                    }
	                    else if (setters[idx].startsWith("$SETERRCODE_2_AUX:"))
	                    {
	                        String[]auxVar = setters[idx].split("\\:");
	                        if (auxVar.length>1)
	                        {
	                            event.setAuxiliarData(auxVar[1], resultCode);
	                        }
	                    }
	                    else if (setters[idx].startsWith("$SETERRDESC_2_AUX:"))
	                    {
	                        String[]auxVar = setters[idx].split("\\:");
	                        if (auxVar.length>1)
	                        {
	                            event.setAuxiliarData(auxVar[1], resultDescription);
	                        }
	                    }
	                    else if (setters[idx].startsWith("$SETRCODE")) {
	                        event.setAuxiliarData("ERRORCODE", resultCode);
	                    }
	                    else if (setters[idx].contentEquals("$MSISDN_RESERVED")) {
	                    	// Los MSISDN Reservados se esperan en el resultdata separados por ; (punto y coma)
	                        event.setMSISDN_RESERVED(resultDataN.split("\\;")); 
	                    }
	                    else if (setters[idx].contentEquals("$PAR")){
	                        event.addArg(paramPrefix + (++pidx),resultDataN);
	                        LogHelper.webLog.debug("ASA - WS:SessionService:operationResult - [ProcessingEvt] (sessionId:" + sessionId +") +arg(parName,Value)=("+ paramPrefix + pidx + ","+resultDataN+")");
	                    }
	                    else
	                    // Los siguientes comandos/Setters son independientes del resultData 
	                    if (setters[idx].startsWith("$PARBYNAME:")){
	                         String[]auxVar = setters[idx].split("\\:");
	                         if (auxVar.length==3)  {
	                                 event.addArg(auxVar[1], auxVar[2]);
	                                 LogHelper.webLog.debug("ASA - WS:SessionService:operationResult - [ProcessingEvt] (sessionId:" + sessionId +") parByName(parName,Value)=(" + auxVar[1] + ","+ auxVar[2] +")");
	                         }
	                         else {
	                                 LogHelper.webLog.error("ASA - WS:SessionService:operationResult - [ProcessingEvt] (sessionId:" + sessionId +") parByName(parName,Value) Missing parameters");
	                         }
	                    }
	                    else if (setters[idx].contentEquals("$FLAGSET")){
	                            event.setInternalFlag("FLAG",1);
	                            LogHelper.webLog.debug("ASA - WS:SessionService:operationResult - [ProcessingEvt] (sessionId:" + sessionId +") FlagSet");
	                    }
	                    else if (setters[idx].contentEquals("$PAUSE")){
	                            try {
									Thread.sleep(1000);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
	                            LogHelper.webLog.debug("ASA - WS:SessionService:operationResult - [ProcessingEvt] (sessionId:" + sessionId +") Pause");
	                    }
	                    else if (setters[idx].contentEquals("$SAVETID")) {
	                            Integer newTid = transactionId + incrementTID;
	                            event.setAuxiliarData("_TID_", newTid.toString());
	                            LogHelper.webLog.debug("ASA - WS:SessionService:operationResult - [ProcessingEvt] (sessionId:" + sessionId +") Save Tid: ");
	                    }
	                    else if (setters[idx].contentEquals("$CHECKTID")) {
	                            Integer receivedTid = 0;
	                            String numstr = sessionData.getAuxiliarData("_TID_");
	                            if (numstr == null) numstr = "0";
	                            Integer savedTid; 
	                            try {
	                            	savedTid = Integer.parseInt(numstr) ;
	                            }
	                            catch(Exception e) {
	                            	savedTid=0;
	                            } 
	
	                            if (resultDescription.startsWith("$CHECKTID:")) {
	                                String[]auxVar = resultDescription.split("\\:");
	                                if (auxVar.length > 1) {
	                                    try {
	                                    	receivedTid = Integer.parseInt(auxVar[1]);
	                                    }
	                                    catch(Exception e) {
	                                    	receivedTid = 0;
	                                    } 
	                                }
	                                else
	                                    LogHelper.webLog.debug("ASA - WS:SessionService:operationResult - [ProcessingEvt] (sessionId:" + sessionId +") Check Tid - Tid format error in resultDescription"    );
	                            }
	                            else
	                                LogHelper.webLog.debug("ASA - WS:SessionService:operationResult - [ProcessingEvt] (sessionId:" + sessionId +") Check Tid - Tid not present in resultDescription"    );
	
	                            LogHelper.webLog.debug("ASA - WS:SessionService:operationResult - [ProcessingEvt] (sessionId:" + sessionId +") Check Tid - Tid:" + transactionId + " savedTid:" + savedTid    );
	                            if (receivedTid < savedTid) {
	                                LogHelper.webLog.info("ASA - WS:SessionService:operationResult - [ProcessingEvt] POST DISCARDED");
	                                cancelPost = 1;
	                            }
	                    }
	                    else if (setters[idx].contentEquals("$FLAGCLEAR")){
	                            event.setInternalFlag("FLAG",0);
	                            LogHelper.webLog.debug("ASA - WS:SessionService:operationResult - [ProcessingEvt] (sessionId:" + sessionId +") FlagClear");
	                    }
	                    else if (setters[idx].contentEquals("$SETOPERATION")){
	                            event.setAuxiliarData("/SETOPERATION", operationId);
	                            LogHelper.webLog.debug("ASA - WS:SessionService:operationResult - [ProcessingEvt] (sessionId:" + sessionId +") Set Operation:" + operationId);
	                    }
	                    else if (setters[idx].startsWith("$GETCONFIG:")){
	                            String[]auxVar = setters[idx].split("\\:");
	                            if (auxVar.length>1)
	                            {
	                                event.addArg(auxVar[1], srvConf.get(auxVar[1]));
	                                LogHelper.webLog.debug("ASA - WS:SessionService:operationResult - [ProcessingEvt] (sessionId:" + sessionId +") +arg(parName,Value)=(" + auxVar[1] + ","+srvConf.get(auxVar[1])+")");
	                            }
	                    }
	                    else if (setters[idx].startsWith("$AUXCONFIG:")){
	                            String[]auxVar = setters[idx].split("\\:");
	                            if (auxVar.length>1)
	                            {
	                                event.setAuxiliarData(auxVar[1], srvConf.get(auxVar[1]));
	                                LogHelper.webLog.debug("ASA - WS:SessionService:operationResult - [ProcessingEvt] (sessionId:" + sessionId +") +aux(key,Value)=(" + auxVar[1] + ","+srvConf.get(auxVar[1])+")");
	                            }
	                    }
	                    else if (setters[idx].contentEquals("$GETOPERATION")){  // lo agrega como parametro
	                            event.addArg(paramPrefix + (++pidx), event.getAuxiliarData("/SETOPERATION", operationId));
	                            LogHelper.webLog.debug("ASA - WS:SessionService:operationResult - [ProcessingEvt] (sessionId:" + sessionId +") Get Operation");
                        }
                        else if (setters[idx].contentEquals("$ERRSET")){
                            event.setStateMachineStatusError(sessionData.getStateMachineState().getName());

                            LogHelper.webLog.debug("ASA - WS:SessionService:operationResult - [ProcessingEvt] SessionId=" + sessionId +"ERRSET ErrorStatus={" + sessionData.getStateMachineState().getName() + "} ErrorDescription={" + resultDescription +"}");
                            LogHelper.dbLog ( sessionId, null, "HIGH", "ERROR","-> operationResult -> ERRSET State="+ sessionData.getStateMachineState().getName(), "ERRSET State={" + sessionData.getStateMachineState().getName() + "} ErrorDescription={" + resultDescription +"}" ); 

                            //Arma cadenas de estados de error y descripciones
                            //String prevStatus=sessionData.getAuxiliarData("ERRORSTATUSNAME","");
                            event.setAuxiliarData("ERRORSTATUSNAME", "{" + sessionData.getStateMachineState().getName() + "}");

                            //String prevDescription=sessionData.getAuxiliarData("ERRORDESCRIPTION","");
                            event.setAuxiliarData("ERRORDESCRIPTION", "{" + resultDescription + "}");
                        }
                        else if (setters[idx].contentEquals("$PROC_AREA_CODE")){
                        	updateAreaCode(sessionId, event);
                        }
                        else if (setters[idx].contentEquals("$PROC_BORDER_INFO")){
                        	processBorderInfo(sessionId, event);
                        }
                        else if (setters[idx].contentEquals("$SETRESUMECODE")){
	                        event.setAuxiliarData("RESUMECODE", resultDescription);
                        }
                        else if (setters[idx].startsWith("$PARAUX:")){
                            String[]auxVar = setters[idx].split("\\:");
                            if (auxVar.length>1)
                            {
                                event.addArg(auxVar[1],sessionData.getAuxiliarData(auxVar[1]));
                                LogHelper.webLog.debug("ASA - WS:SessionService:operationResult - [ProcessingEvt] (sessionId:" + sessionId +") +arg(parName,Value)=(" + auxVar[1] + ","+sessionData.getAuxiliarData(auxVar[1])+")");
                            }
                        }
                        else if (setters[idx].startsWith("$DECREMENT:")){
                            String[]auxVar = setters[idx].split("\\:");
                            if (auxVar.length>1)
                            {
                                //Obtiene valor actual
                                String numstr = sessionData.getAuxiliarData(auxVar[1]);
                                if (numstr==null)
                                {
                                    LogHelper.webLog.debug("Decrement numstr null" );
                                    numstr="0";
                                }
                                LogHelper.webLog.debug("Decrement Value Read: ("+auxVar[1]+", '"+numstr+"')" );
                                Integer iVal; 
                                try{    iVal = Integer.parseInt(numstr) ;}
                                catch(Exception e)
                                {       
                                    LogHelper.webLog.error("Decrement Exception (numstr, exception): ("+numstr+", "+e+")", e );
                                    iVal=0;
                                } 
                                
                                // Guarda valor incrementado
                                if (iVal>0)
                                    event.setAuxiliarData(auxVar[1], String.valueOf(iVal-1));
                                
                                LogHelper.webLog.debug("ASA - WS:SessionService:operationResult - [ProcessingEvt] (sessionId:" + sessionId +") decrement(parName,newValue)=(" + auxVar[1] + ","+ (iVal+1) +")");
                            }
                        }
                        else if (setters[idx].startsWith("$INCREMENT:")){
                            String[]auxVar = setters[idx].split("\\:");
                            if (auxVar.length>1)
                            {
                                //Obtiene valor actual
                                String numstr = sessionData.getAuxiliarData(auxVar[1]);
                                if (numstr==null)
                                    numstr="0";
                                Integer iVal; 
                                try{    iVal = Integer.parseInt(numstr) ;}
                                catch(Exception e)
                                {       iVal=0;} 
                                
                                // Guarda valor incrementado
                                if (iVal<65535)
                                    event.setAuxiliarData(auxVar[1], String.valueOf(iVal+1));
                                
                                LogHelper.webLog.debug("ASA - WS:SessionService:operationResult - [ProcessingEvt] (sessionId:" + sessionId +") increment(parName,newValue)=(" + auxVar[1] + ","+ (iVal+1) +")");
                            }
                        }
                        else if (setters[idx].startsWith("$INITIALIZE:")){
                            String[]auxVar = setters[idx].split("\\:");
                            if (auxVar.length==2)
                            {
                                 event.setAuxiliarData(auxVar[1], String.valueOf(0));
                                 LogHelper.webLog.debug("ASA - WS:SessionService:operationResult - [ProcessingEvt] (sessionId:" + sessionId +") initialize(parName,Value)=(" + auxVar[1] + ","+ 0 +")");
                            }
                            if (auxVar.length>2)
                            {
                                 event.setAuxiliarData(auxVar[1], String.valueOf(auxVar[2]));
                                 LogHelper.webLog.debug("ASA - WS:SessionService:operationResult - [ProcessingEvt] (sessionId:" + sessionId +") initialize(parName,Value)=(" + auxVar[1] + ","+ auxVar[2] +")");
                            }
                        }
                        else{
                            event.addArg(paramPrefix + (++pidx), setters[idx]);  // setea el valor en el campo params
                            LogHelper.webLog.debug("ASA - WS:SessionService:operationResult - [ProcessingEvt] (sessionId:" + sessionId +") +arg(parName,Value)=("+paramPrefix + pidx + ","+setters[idx]+")");
                        }
	                    /*
	                     else if (setters[idx].contentEquals("$FLAG+"))
	                        evt.setInternalFlag("FLAG", controlFlag+1);
	                    else if (setters[idx].contentEquals("$FLAG-"))
	                        evt.setInternalFlag("FLAG", controlFlag-1);
	                    */
	                }
                
                }
                catch(MalformedEventException mee) {
                	response.resultCode = 0l;
        			response.resultDescription = mee.getMessage();
        			
        			LogHelper.webLog.debug("ASA - WS:SessionService:operationResult- Error creating event: " + mee.getMessage());
        			LogHelper.dbLog(sessionId, null, "HIGH", "INFO", "ASA - WS:SessionService:operationResult- Error creating event", mee.getMessage());
        			
        			return response;
                }
            }
        }
        // Postea evento y devuelve resultado (class WsAckRsp).    
        

        if (cancelPost==0){
        
            LogHelper.webLog.info("ASA - WS:SessionService:operationResult PostEvent - (sessionId:" + sessionId +") postEvent:" + event.toString());
            LogHelper.dbLog ( sessionId, null, "HIGH", "INFO","-> operationResult -> postEvent "+ event.getTag().toString(),"postEvent( sessionId="+ sessionId +", Operation=" + nextOperationParsed + ", Event="+ event.toString()+")" ); 
            // Seteo msisdnFake

            LogHelper.dbLogFlow (sessionId, null, "HIGHEST", "INFO"," State: "+ sessionData.getStateMachineState().getName(), ""); 
            return postEvent(sessionManager, event);
        }
        else
        {
            LogHelper.webLog.info("ASA - WS:SessionService:operationResult PostEvent - (sessionId:" + sessionId +") POST DISCARDED - postEvent:" + event.toString());
            LogHelper.dbLog ( sessionId, null, "HIGH", "INFO","-> operationResult -> postEvent (DISCARDED) "+ event.getTag().toString(),"postEvent( sessionId="+ sessionId +", Event="+ event.toString()+")" ); 
        }                                
        
		return response;
	}

   /**
     * Realiza el post de un evento y genera la respuesta de posteo de eventos 
     * @param sme Recibe la instancia (singleton) del SMEFacade
     * @param event Recibe el evento a postear
     * @return resultCode=0 para indicar que la operación se ejecutó exitosamente.
     */
   public WsAckRsp postEvent(SMEFacade sme, ActivationEvent event)    {
       WsAckRsp retVal = new WsAckRsp();
       try {
    	   if (sme.post(event)) {
    		   retVal.resultCode=0L;
    		   retVal.resultDescription="OK";
    		   LogHelper.dbLogFlow (event.getSessionID(), null, "HIGHEST", "INFO", "[" + event.getTransactionID() 
    				   + "]" + "  Event: "+event.getTag().toString(), ""  );
    	   }
    	   else {
    		   retVal.resultCode=77L;
    		   retVal.resultDescription="POST_ERROR";
    		   LogHelper.webLog.error("PostEvent: PostError." );
    	   }
       }
       catch(Exception e) {
           retVal.resultCode=99L;
           retVal.resultDescription="Exception:" + e.getMessage();
           LogHelper.webLog.error("PostEvent: Exception" + e.getLocalizedMessage(), e);
       }
       
       return retVal;
   }

   public Pair<Integer,String> postStartEvent(SMEFacade sme, ActivationEvent event) {
       
       if (event != null) 
           LogHelper.webLog.debug("provisioningSIM: PostStartEvent sesId:"+event.getSessionID()+ " evt:"+event.getTag().getName());
       else {
           LogHelper.webLog.debug("provisioningSIM: PostStartEvent null Event");
           return null;
       }

       Pair<Integer,String> rsp = null;
       try {
           // En el start si ya existe una sesion con alguno de los datos claves que se envian para
           // su creacion, no se crea una nueva, sino que retorna el ID de la sesion existente.
           rsp = sme.start(event, Integer.parseInt(srvConf.get("postStartEventTimeout")), TimeUnit.SECONDS);
           if (rsp.getLeft().equals(new Integer(0))) {
               LogHelper.dbLogFlow (event.getSessionID(), null, "HIGHEST", "INFO", "[" + event.getTransactionID() + "]" + "  Event: "+event.getTag().toString(), ""  );
               return rsp;
           }
           else {
               LogHelper.webLog.error("PostStartEvent: Error(return.left)=" + rsp.getLeft());
               rsp = null;
           }
       }
       catch(Exception e) {
           LogHelper.webLog.error("PostStartEvent: Exception " + e.getLocalizedMessage(),e);
           rsp =null;
       }
       
       return rsp;
   }
   
   /**
    * Busca una sesión dentro del ejecutor activo utilizando el sessionId y/o iccid como condiciones de búsqueda
    * @param sessionId Identificador de la sesión a buscar.
    * @return Devuelve null si no se encuentra una sesión o si hubo algún problema.  Sino retorna un objeto sesión.
    * 
    */
   public SessionData searchSession(String sessionId, String iccid) {
       LogHelper.webLog.debug("searchSession: sessionId=" + sessionId + " iccid=" + iccid);

       SessionData rsp = null;
       
       SearchCriteria sc = new SearchCriteria (sessionId, null, null, iccid);
       rsp = SMEFacade.getInstance().getSessionBySearchCriteria(sc);
           
       if (rsp == null) {
            LogHelper.webLog.error("searchSession: sessionId=" + sessionId  + " iccid=" + iccid + " not found.");
       }
       
       return rsp;
   }
   
   /**
    * Busca una sesion dentro del ejecutor activo utilizando el sessionId como condicion de busqueda
    * @return Devuelve null si no se encuentra una sesion o si hubo algun problema.  Sino retorna un objeto sesion.
    * 
    */
   public SessionData searchSessionById(java.lang.String sessionId)
   {
       return searchSession(sessionId,null);
   }
   
   /**
    * Busca una sesion dentro del ejecutor activo utilizando el iccid como condicion de busqueda
    * @return Devuelve null si no se encuentra una sesion o si hubo algun problema.  Sino retorna un objeto sesion.
    * 
    */
   public SessionData searchSessionByIccid(java.lang.String iccid)
   {
       return searchSession(null,iccid);
   }

   /**
    * Busca una sesion dentro del ejecutor activo utilizando el MSISDN fake como condicion de busqueda
    * 
    * @param msisdn
    * @return Devuelve null si no se encuentra una sesion o si hubo algun problema.  Sino retorna un objeto sesion.
    * 
    */
   public SessionData searchSessionByMsisdn(java.lang.String msisdn) {
       LogHelper.webLog.debug("searchSessionByMsisdn: msisdn=" + msisdn);
       
       SearchCriteria sc = new SearchCriteria (null,null,msisdn,null);
       SessionData rsp = SMEFacade.getInstance().getSessionBySearchCriteria(sc);
           
       if (rsp==null) {
              LogHelper.webLog.error("searchSessionByMsisdn: msisdn=" + msisdn + " not found." );
      }
       
       return rsp;
   }

   /**
    * Busca una sesion dentro del ejecutor activo utilizando alguno de: ICCID, IMSI fake, MSISDN fake como condicion de busqueda
    * @param iccid
    * @param imsi
    * @param msisdn
    * @return
    */
   public SessionData searchSessionByIccidImsiMsisdn(String iccid, String imsi, String msisdn) {
       LogHelper.webLog.debug("searchSessionByImsiMsisdn: iccid=" + iccid+ " imsi=" + imsi + " msisdn=" + msisdn);
       
       SearchCriteria sc = new SearchCriteria (null, imsi, msisdn, iccid);
       SessionData rsp = SMEFacade.getInstance().getSessionBySearchCriteria(sc);
           
       if (rsp==null) {
              LogHelper.webLog.error("searchSessionByImsiMsisdn: iccid=" + iccid+ " imsi=" + imsi + " msisdn=" + msisdn + " not found." );
      }
       
       return rsp;
   }
   
   /**
    * Busca un subscriber a partir del ICCID y del MSISDN fake y se fija el estado.
    * 
    * @param filter_ICCID
    * @param filter_MSISDN_t
    * @param sub
    * 
    * @return 0 si fue encontrado y esta desbloqueado; 2 si fue encontrado y esta bloqueado; 1 si no existe
    */
   public int subscriberCheck(String filter_ICCID, String filter_MSISDN_t, Subscriber sub) {
       LogHelper.webLog.debug("ut_subscriberCheck:Begin");

       int response = 0;

       sub = ConnectionManager.getInstance().getSubscriber(filter_ICCID, filter_MSISDN_t, null, null, null);

       if (sub == null)
    	   response = 1;
       else if (sub.getBlockedStatus().equals("B")) {
           response = 2;
       }
       else if (sub.isExpired()) {
           response = 3;
       }
       
       return response;
   }
   
   /**
    * Controla que la fecha actual se encuentre dentro del rango horario
    * permitido para realizar activaciones
    * Se realiza una llamada al metodo IsDateInBand con los siguientes parametros
    * - date: Fecha actual del sistema
    * - bandCode: 'ACT'
    * - appName: 'Orchestrator'
    * - modName: 'Api'
    * 
    * @return true si esta en horario, false caso contrario.
    */
   public boolean isNowInBand() {
       Date xNow = new Date();
       try {
	       Boolean rsp = ConnectionManager.getInstance().isDateInBand(xNow, "TACT", "ASA", "ASA");
	       if (rsp != null)
	           return rsp;
       }
       catch(Exception e) {
           LogHelper.webLog.fatal("isDateInBand Exception. ", e);
       }
       
       return false;
   }
   
   /**
    * getDDD: Obtiene a partir de un IMSI, los códigos de área disponibles.
    * @param IMSI IMSI
    * @param ddd  Arreglo de areas
    * @param qqty Cantidad de areas máximas
    * @return int cantidad de areas devueltas
    *
    */
   public int getDDD(String IMSI , String[] ddd, int qqty, String MSISDN, String loci) {
       int response = 0;
   
       // Intenta obtenerlas a partir del loci en una sesión anterior.
       LogHelper.webLog.debug(" getDDD - Calling getSessionData");
       ConnectionManager connMgr = ConnectionManager.getInstance();
       SessionStatus sds = connMgr.getSession(MSISDN, null, null);  // busco por msisdn en memoria o BD
       
       if (sds != null) {
           // Loci found
           loci = sds.loci;
           LogHelper.webLog.debug(" getDDD: Loci acquired.  loci=" + loci);
           
           String lac_len = srvConf.get("areaCode_len");
           String lac_pos = srvConf.get("areaCode_pos");
           int pos, len;
           try {
           	pos = Integer.parseInt(lac_pos);
           }
           catch(NumberFormatException nfe) {
           	pos = 0;
           }
           try {
           	len = Integer.parseInt(lac_len);
           }
           catch(NumberFormatException nfe) {
           	len = 0;
           }
           
           String areaCode = Utils.getAreaCodeFromLoci(loci, len, pos);
           if (areaCode != null) {
               ddd[0]=areaCode;
               return 1;
           }
       }
       
       // Buscamos las areas en la tabla de areas
       response = connMgr.getDDD(IMSI, qqty, ddd);
      
       return response;
   }

   /**
    * updateAreaCode: actualiza el event AreaCode y LOCI en funcion de la localizacion devuelta por el HLR.
    * @param sessionId sessionId
    * @param event  Activation Event
    *
    */
   	public void updateAreaCode(String sessionId, ActivationEvent event) {
   		
   		String mcc    = event.getAuxiliarData("MCC");
   		String mnc    = event.getAuxiliarData("MNC");
   		String lac    = event.getAuxiliarData("LAC");
   		String cellId = event.getAuxiliarData("CELLID");
   		String net 	  = event.getAuxiliarData("NET");
	   	
   		try {
   	   		String areaCode = String.format("%05d", Integer.parseInt(lac, 16)).substring(3);
   	   		//String cgi = mcc.charAt(1)+mcc.charAt(0)+'F'+mcc.charAt(2)+mnc.charAt(1)+mnc.charAt(0)+lac+cellId;
   	   		lac  = String.format("%05d", Integer.parseInt(lac, 16));
   	   		event.setAuxiliarData("LAC", lac);
   			//event.setLOCI(cgi);
   			event.setAreaCode(areaCode);

   			String localArea = String.format("%05d", Integer.parseInt(lac, 16)).substring(0,3);  			
   			if (localArea.charAt(0) == '6') {
   	   			event.setAuxiliarData("ISBORDER_CN", "T");
   			}
   			else {
   	   			event.setAuxiliarData("ISBORDER_CN", "F");   				
   			}
			LogHelper.webLog.debug("Event UpdateAreaCode [MCC: "+mcc+", MNC: "+mnc+", LAC: "+lac +", CELLID: "+cellId + ", AreaCode: " + areaCode + ", Net: " + net +", Border: " + event.getAuxiliarData("ISBORDER_CN") + "]");
   		}
		catch (Exception mee) {		
			LogHelper.webLog.debug("SessionService:updateAreaCode - Error: " + mee.getMessage());
			LogHelper.dbLog(sessionId, null, "HIGH", "INFO", "SessionService:updateAreaCode- Error !!",	mee.getMessage());	
	   }
   	}

    /**
     * processBorderInfo: procesa el resultado del llamado a WS getBorderInfo.
     * @param sessionId sessionId
     * @param event  Activation Event
     *
     */
   	public void processBorderInfo(String sessionId, ActivationEvent event) {
   		
   		String flg_border    = event.getAuxiliarData("FLG_BORDER");
   		String flg_cnselect  = event.getAuxiliarData("FLG_CNSELECT");
   		String flg_msisdnsel = event.getAuxiliarData("FLG_MSISDN_SEL");
	   	
   		try {
   			if (flg_border.equals("T")) event.setAuxiliarData("RESULT_BORDER_INFO", "FLG_BORDER");
   			if (flg_cnselect.equals("T")) event.setAuxiliarData("RESULT_BORDER_INFO", "FLG_CNSELECT");
   			if (flg_msisdnsel.equals("T")) event.setAuxiliarData("RESULT_BORDER_INFO", "FLG_MSISDN_SEL");
   			if (flg_border.equals("F") && flg_cnselect.equals("F")) event.setAuxiliarData("RESULT_BORDER_INFO", "FLG_DEFAULT");

   			LogHelper.webLog.debug("SessionService:processBorderInfo - RESULT_BORDER_INFO: " + event.getAuxiliarData("RESULT_BORDER_INFO"));
   		}
		catch (Exception mee) {		
			LogHelper.webLog.debug("SessionService:processBorderInfo - Error updating ActivationEvent: " + mee.getMessage());
			LogHelper.dbLog(sessionId, null, "HIGH", "INFO", "ASA - SessionService:processBorderInfo- Error updating ActivationEvent", 
					mee.getMessage());	
	   }
   }
   	
   /**
    * Envia a traves del menuManager un pedido al SIMOTA para que genere un mensaje en la pantalla del teléfono del cliente
    * @param sessionId Id de la sesion
    * @param msisdnFake Numero de msisdnFake del cliente
    * @param transactionId Id de transacción
    * @param Text Texto a mostrar
    * @return devuelve 0 si estuvo todo ok, o distinto de 0 para informar de algún error.
    */
   public int sendDisplayText(String sessionId, String msisdnFake, Integer transactionId, String Text){
	   return sendDisplayText( sessionId,  msisdnFake,  transactionId,  Text,  null);
   }
   
   public int sendDisplayText(String sessionId, String msisdnFake, Integer transactionId, String Text, String config) {
	   int result = 0;

       LogHelper.webLog.info("Session:sendDisplayText: msisdnFake=" + msisdnFake + ", transactionId=" + transactionId +
    		   ", sessionId=" + sessionId + ", Text="+ Text );
       
       // Control de estado del servidor
       if (!srvConf.serverRunning()){
           LogHelper.webLog.fatal(" Operation not processed.  Reason:Server stopped ");
           return -99;
       }

       LogHelper.dbLogSdr ( sessionId, msisdnFake, null, "INFO","-> SendDisplayText", 
       		"SendDisplayText ( msisdnFake=" + msisdnFake + ", transactionId=" + transactionId + ", sessionId=" + sessionId 
       		+ ", Text="+ Text + ")"); 
       
       // Envio a traves de Menu Applet
       LogHelper.webLog.info("Session:sendDisplayText: sending displayText to Menu  MsisdnFake=" + msisdnFake + 
    		   ", transactionId=" + transactionId + ", sessionId=" + sessionId  );
       
       String OrchId =  srvConf.getServerId();
       ApiManager menuManager = ApiManager.getInstance();
       
       try {
		  menuManager.sendNotification(OrchId, sessionId, msisdnFake, transactionId, Text);
	   } 
       catch (MenuManagerException e) {
    	   LogHelper.webLog.debug("Session:sendDisplayText: MenuManagerException  MsisdnFake=" + msisdnFake + 
    			   ", transactionId=" + transactionId + ", sessionId=" + sessionId + ", Text="+ Text, e );
    	   
           LogHelper.dbLogSdr ( sessionId, msisdnFake, null, "ERROR","-> SendDisplayText", 
              		"SendDisplayText ( msisdnFake=" + msisdnFake + ", transactionId=" + transactionId + 
              		", sessionId=" + sessionId + ", Text="+ Text + ")"); 

    	   result = -1;
	   }

       LogHelper.webLog.info("Session:sendDisplayText: MenuManager returned response=null.  MsisdnFake=" + msisdnFake + 
    		   ", transactionId=" + transactionId + ", sessionId=" + sessionId + ", Text="+ Text );

       return result;
   }    
}
