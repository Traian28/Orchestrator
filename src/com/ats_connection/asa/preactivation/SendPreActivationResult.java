package com.ats_connection.asa.preactivation;

public class SendPreActivationResult {

	SendPreActivationResult() {
	}
	
	String SendResult(String sessionId, String resultCode, String resultDescription, String resultData ) {
		
		String result = "SessionId= " + sessionId 
					  + ", resultCode=" + resultCode
		  			  + ", resultDescription=" + resultDescription
		  			  + ", resultDescription=" + resultData;
		return result;
	}
}
