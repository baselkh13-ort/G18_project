package logic;

// Defines the list of allowed commands between Client and Server
public enum ActionType {
    
    // Ask server to find a single order by ID
    READ_ORDER,
    // Ask server to save changes to an existing order
    UPDATE_ORDER,
    // Ask server for the full list of all orders (for the table view)
    GET_ALL_ORDERS,

    // Register a member
    REGISTER_MEMBER,
    // Update the contact info
    UPDATE_MEMBER_CONTACT,
    // Getting member object by member id
    GET_MEMBER_BY_ID,
    // Getting all the members from DB
    GET_ALL_MEMBERS,
    
    // Tell server the user is closing the application
    CLIENT_QUIT, 
}