package com.ats_connection.asa.orchestrator.service;

import javax.jws.WebService;

import com.ats_connection.asa.orchestrator.config.ServerConfiguration;
import com.ats_connection.asa.orchestrator.core.SessionService;
import com.ats_connection.asa.orchestrator.helper.LogHelper;
import com.ats_connection.asa.orchestrator.response.WsAckRsp;

// Imports de StateMachine
import com.ats_connection.asa.orchestrator.sme.Session.SessionData;
import com.ats_connection.asa.orchestrator.sme.api.SMEFacade;
import com.ats_connection.asa.orchestrator.sme.helper.SearchCriteria;
import com.smarttrust.opm.schema.notification.ActivityActionNotification;
import com.smarttrust.opm.schema.notification.ActivityNotification;
import com.smarttrust.opm.schema.notification.RequestNotification;
import com.smarttrust.opm.ws_notification.OpmNotification;

@WebService(serviceName = "opmNotificationService", portName = "opmSoap11", 
endpointInterface = "com.smarttrust.opm.ws_notification.OpmNotification", 
targetNamespace = "http://smarttrust.com/opm/ws-notification", wsdlLocation = "WEB-INF/wsdl/OpmNotification/opm-notification.wsdl")
public class OpmNotif implements OpmNotification {

	@Override
	public void requestStatusChanged(RequestNotification requestNotificationPart) {
		LogHelper.webLog.info("RequestStatusChange");
	}

	@Override
	public void activityStatusChanged(ActivityNotification activityNotificationPart) {
		LogHelper.webLog.info("ActivityStatusChange");
		
		// Control de estado del servidor
		if (!ServerConfiguration.getInstance().serverRunning()){
			LogHelper.webLog.fatal(" Operation not processed.  Reason:Server stopped ");
			return;
		}
		
		// Buscar la sesion en el SME
		SMEFacade stateMachine = SMEFacade.getInstance();
		
		SearchCriteria sc = new SearchCriteria(
				null, // session ID 
				null, // imsi fake
				activityNotificationPart.getSubscriptionInfo().getMsisdn(), // fake 
				activityNotificationPart.getSubscriptionInfo().getIccid());
		
		SessionData sd = stateMachine.getSessionBySearchCriteria(sc);
		
		if (sd == null) {
			LogHelper.webLog.info("ActivityStatusChange: Session not found for subscriber "+
					activityNotificationPart.getSubscriptionInfo().getIccid());
			return;
		}
		
		SessionService sessionMgr;
		try {
			sessionMgr = SessionService.getInstance();
		} 
		catch (Exception e) {
			LogHelper.webLog.error("ActivityStatusChange: internal error: "+e.getMessage());
			return;
		}
		
		String operation = activityNotificationPart.getRequestInfo().getName();
		String resultCode = activityNotificationPart.getActivityInfo().getStatus();
		StringBuffer resultDesc = new StringBuffer();
		resultDesc.append("ActivityNotification - ICCID: ").append(activityNotificationPart.getSubscriptionInfo().getIccid());
		resultDesc.append(", Activity status: ").append(activityNotificationPart.getActivityInfo().getStatus());
		resultDesc.append(", Request name: ").append(activityNotificationPart.getRequestInfo().getName());
		resultDesc.append(", Request status: ").append(activityNotificationPart.getRequestInfo().getStatus());
		
		String resultData = null;
		
		WsAckRsp rsp = sessionMgr.operationResult(sd.getID(), sd.getTransactionID(), 
				                    operation, resultCode, resultDesc.toString(), resultData);
		
		if (rsp.resultCode == 0)
			LogHelper.webLog.info("RequestStatusChange - OperationResult OK");
		else
			LogHelper.webLog.error("RequestStatusChange - OperationResult with error: "+ rsp.resultDescription);
	}

	@Override
	public void activityActionPerformed(ActivityActionNotification activityActionNotificationPart) {
		// TODO Auto-generated method stub

	}

}
