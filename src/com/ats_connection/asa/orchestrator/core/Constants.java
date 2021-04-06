package com.ats_connection.asa.orchestrator.core;

public class Constants {
	
	public static long OK = 0;

	public static long PERSIST_SESSION_ERROR = 11;
	public static String PERSIST_SESSION_ERROR_DESC = "Error persisting session";
	
	public static long PERSIST_SESSION_COUNT_ERROR = 12;
	public static String PERSIST_SESSION_COUNT_ERROR_DESC = "Error persisting, session count";
	
	public static long PERSIST_SESSION_NOT_FOUND = 13;
	public static String PERSIST_SESSION_NOT_FOUND_DESC = "Error persisting, session not found: ";
	
	public static long PERSIST_SESSION_INSERT_ERROR = 14;
	public static String PERSIST_SESSION_INSERT_ERROR_DESC = "Error persisting session on insert";
	
	public static long SESSION_NOT_FOUND = 21;
	public static String SESSION_NOT_FOUND_DESC = "Session not found";
	
	public static long SM_NOT_FOUND = 24;
	public static String SM_NOT_FOUND_DESC = "Error getting SM Instance";
	
	public static long SESSION_CREATION_ERROR = 25;
	public static String SESSION_CREATION_ERROR_DESC = "Error creating session";
	
	public static long POST_EVENT_ERROR = 43;
	public static String POST_EVENT_ERROR_DESC = "Error posting event";
	
	public static long TRANSACTION_ID_EXPIRED = 73;
	public static String TRANSACTION_ID_EXPIRED_DESC = "Transaction ID out of sequence, operation discarded";
	
	public static long EVENT_CONFIG_NOT_FOUND = 80;
	public static String EVENT_CONFIG_NOT_FOUND_DESC = "PostEvent configuration not found";
	
	public static long SUBSCRIBER_NOT_FOUND = 101;
	public static String SUBSCRIBER_NOT_FOUND_DESC = "Subscriber not found";
	
	public static long SUBSCRIBER_BLOCKED = 102;
	public static String SUBSCRIBER_BLOCKED_DESC = "Subscriber blocked";
}
