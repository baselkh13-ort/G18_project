package common;

/**
 * Defines the user roles and permission levels within the Bistro application.
 * This enum is used for authorization logic to determine which screens and
 * actions a specific user can access.
 */
public enum Role {
    
    /**
     * A registered customer with a saved profile in the database.
     * Members are eligible for benefits such as the 10% discount and order history tracking.
     */
    MEMBER,       
    
    /**
     * A representative restaurant employee
     * Has access to operational tasks like viewing tables and managing active orders.
     */
    WORKER,       
    
    /**
     * An administrative user with full system access.
     * Managers can view reports, modify the restaurant layout, and change system settings.
     */
    MANAGER,        
    
    /**
     * An unregistered user or a walk-in customer.
     * Guests can perform basic actions like ordering and paying but do not have a saved history.
     */
    GUEST,
}