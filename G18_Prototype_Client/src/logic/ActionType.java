package logic;

// Defines the list of allowed commands between Client and Server
public enum ActionType {
    
    // Ask server to find a single order by ID
    READ_ORDER,
    // Ask server to save changes to an existing order
    UPDATE_ORDER,
    // Ask server for the full list of all orders (for the table view)
    GET_ALL_ORDERS,
    // Tell server the user is closing the application
    CLIENT_QUIT
}