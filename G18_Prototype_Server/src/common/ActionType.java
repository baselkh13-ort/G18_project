package logic;
//Defines the command types sent between Client and Server.

public enum ActionType {
    READ_ORDER,      // Request to fetch a specific order by ID
    UPDATE_ORDER,    // Request to update an existing order
    GET_ALL_ORDERS,   // Request to retrieve all orders from the DB
    
    // Register a member
    REGISTER_MEMBER,
    // Update the contact info
    UPDATE_MEMBER_CONTACT,
    // Getting member object by member id
    GET_MEMBER_BY_ID,
    // Getting all the members from DB
    GET_ALL_MEMBERS,
    
    CLIENT_QUIT 	// Indicates that the client is requesting a graceful disconnection from the server
}