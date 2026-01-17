package common;

/**
 * Defines the command types (actions) sent between the Client and the Server.
 * This Enum acts as the protocol for the OCSF framework, ensuring that both
 * sides know how to handle the data object attached to the message.
 */
public enum ActionType {

    //Authentication & User Management
    
    /**
     * Authenticates a user (Member or Staff) into the system.
     */
    LOGIN,
    
    /**
     * Registers a new member into the database.
     */
    REGISTER_CLEINT,
    
    /**
     * Simulates scanning a QR code or magnetic card at the terminal to identify a member.
     */
    IDENTIFY_BY_QR,
    
    /**
     * Retrieves the history of visits and orders for a specific user.
     */
    GET_USER_HISTORY, 
    
    /**
     * Updates a member's contact information (Phone/Email).
     */
    UPDATE_USER_INFO, 
   
    //Order Management
    
    /**
     * Retrieves a specific order based on its unique confirmation code.
     */
    GET_ORDER_BY_CODE,
    
    /**
     * Cancels an existing order or waiting list entry.
     */
    CANCEL_ORDER,
    
    /**
     * Checks for available table slots for a specific date and time.
     */
    GET_AVAILABLE_TIMES,
    
    /**
     * Retrieves a list of all registered members (for Staff usage).
     */
    GET_ALL_MEMBERS,
    
    /**
     * Creates a new reservation (Order) in the system.
     */
    CREATE_ORDER,
    
    /**
     * Suggests alternative time slots when the requested time is fully booked.
     */
    ORDER_ALTERNATIVES,
    
    /**
     * Retrieves the current waiting list for tables.
     */
    GET_WAITING_LIST,
    
    //Waiting List & Arrival
    
    /**
     * Adds a customer (Member or Guest) to the waiting list.
     */
    ENTER_WAITLIST,
    
    /**
     * Removes a customer from the waiting list (manual exit).
     */
    LEAVE_WAITLIST,
    
    /**
     * Updates the status of an order (e.g., from PENDING to APPROVED).
     */
    UPDATE_ORDER_STATUS,
    
    /**
     * Validates if a customer arrived within the allowed 20-minute window.
     */
    VALIDATE_ARRIVAL,
    
    /**
     * Recovers a lost confirmation code via Email/SMS simulation.
     */
    RESTORE_CODE,
    
    //Payment & Checkout
    
    /**
     * Processes payment for a meal and frees the table.
     */
    PAY_BILL,
    
    // Staff / Manager Operations
    
    /**
     * Retrieves all orders in the system (for Management view).
     */
    GET_ALL_ORDERS,
    
    /**
     * Retrieves the map/list of all tables in the restaurant.
     */
    GET_ALL_TABLES,
    
    /**
     * Adds a new table to the restaurant layout.
     */
    ADD_TABLE,
    
    /**
     * Removes a table from the restaurant layout.
     */
    REMOVE_TABLE,           
    
    /**
     * Updates table details (e.g., number of seats).
     */
    UPDATE_TABLE,
    
    /**
     * Retrieves a snapshot of currently seated customers (Live view).
     */
    GET_ACTIVE_DINERS,
    
    /**
     * Retrieves all orders that are currently active (Seated or Waiting).
     */
    GET_ALL_ACTIVE_ORDERS,
    
    /**
     * Gets the restaurant opening and closing hours.
     */
    GET_OPENING_HOURS,      
    
    /**
     * Updates the restaurant operating hours.
     */
    UPDATE_OPENING_HOURS,  
    
    /**
     * Retrieves active orders relevant to a specific user (for the My Orders screen).
     */
    GET_RELEVANT_ORDERS,

    //Reports
    
    /**
     * Generates the Performance Report (delays, lateness) for a specific month.
     */
    GET_PERFORMANCE_REPORT,  
    
    /**
     * Generates the Orders & Activity Report for a specific month.
     */
    GET_SUBSCRIPTION_REPORT,
    
    //System & Connection
    
    /**
     * Sent from Server to Client to trigger a popup or alert (e.g., Table Ready).
     */
    SERVER_NOTIFICATION,
    
    /**
     * Logs out the current user but keeps the application running.
     */
    LOGOUT,
    
    /**
     * Disconnects the client from the server and closes the application.
     */
    CLIENT_QUIT
}