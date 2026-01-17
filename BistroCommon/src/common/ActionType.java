package common;
//Defines the command types sent between Client and Server.

public enum ActionType {
	LOGIN,
	REGISTER_CLEINT,// Register a new member
	IDENTIFY_BY_QR,
	GET_USER_HISTORY, //Getting the user history from DB
	UPDATE_USER_INFO, // Update the contact info
   
	GET_ORDER_BY_CODE,
    CANCEL_ORDER,
	GET_AVAILABLE_TIMES,
	GET_ALL_MEMBERS,
    CREATE_ORDER,
    ORDER_ALTERNATIVES,
    GET_WAITING_LIST,
    
    ENTER_WAITLIST,
    LEAVE_WAITLIST,
    UPDATE_ORDER_STATUS,
    
    VALIDATE_ARRIVAL,
    RESTORE_CODE,
    
    PAY_BILL,
    
    GET_ALL_ORDERS,
    
    GET_ALL_TABLES,
    ADD_TABLE,
    REMOVE_TABLE,           
    UPDATE_TABLE,
    
    GET_ACTIVE_DINERS,
    GET_ALL_ACTIVE_ORDERS,
    GET_OPENING_HOURS,      
    UPDATE_OPENING_HOURS,  
    GET_RELEVANT_ORDERS,

    GET_PERFORMANCE_REPORT,  
    GET_SUBSCRIPTION_REPORT,
    
    SERVER_NOTIFICATION,
    LOGOUT,
    CLIENT_QUIT
}