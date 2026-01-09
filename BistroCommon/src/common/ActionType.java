package common;
//Defines the command types sent between Client and Server.

public enum ActionType {
	LOGIN,
	REGISTER_CLEINT,// Register a new member
	IDENTIFY_BY_QR,
	GET_USER_HISTORY, //Getting the user history from DB
	UPDATE_USER_INFO, // Update the contact info
   
	GET_ALL_MEMBERS,
    CREATE_ORDER,
    ORDER_ALTERNATIVES,
    
    ENTER_WAITLIST,
    LEAVE_WAITLIST,
    UPDATE_ORDER_STATUS,
    
    VALIDATE_ARRIVAL,
    RESTORE_CODE,
    
    PAY_BILL,
    
    GET_ALL_ORDERS,
    ADD_TABLE,
    
    REMOVE_TABLE,           
    UPDATE_TABLE,           
    GET_OPENING_HOURS,      
    UPDATE_OPENING_HOURS,   

    GET_PERFORMANCE_REPORT,  
    GET_SUBSCRIPTION_REPORT, 
    CLIENT_QUIT
}