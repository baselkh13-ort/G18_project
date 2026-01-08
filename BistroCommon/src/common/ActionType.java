package common;
//Defines the command types sent between Client and Server.

public enum ActionType {
	LOGIN,
    READ_ORDER,      // Request to fetch a specific order by ID
    UPDATE_ORDER,    // Request to update an existing order
    GET_ALL_ORDERS,   // Request to retrieve all orders from the DB
    REGISTER_NEW_MEMBER, // Register a new member
    UPDATE_MEMBER_CONTACT, // Update the contact info
    GET_MEMBER_BY_ID, // Getting member object by member id
    GET_ALL_MEMBERS, // Getting all the members from DB
    GET_USER_HISTORY, //Getting the user history from DB
    JOIN_WAITLIST, //join to waitingList
    ENTER_CONFIRMATION_CODE,
    ORDER_ALTERNATIVES,
    CLIENT_QUIT 	// Indicates that the client is requesting a graceful disconnection from the server
}