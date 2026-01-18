package common;

import java.io.Serializable;

/**
 * Represents a physical dining table within the restaurant system.
 * This entity is shared between the client and server for table management,
 * map visualization, and reservation handling.
 * * Implements Serializable to facilitate network transmission via OCSF.
 */
public class Table implements Serializable {
    
    /**
     * The serialization runtime identifier, ensuring version compatibility.
     */
    private static final long serialVersionUID = 1L;

    private int tableId;
    private int capacity;
    private String status;

    /**
     * Constructs a new Table instance with specific details.
     * * @param tableId The unique numeric identifier for the table.
     * @param capacity The maximum number of diners this table can accommodate.
     * @param status The current operational status of the table (e.g., AVAILABLE, OCCUPIED).
     */
    public Table(int tableId, int capacity ,String status) {
        this.tableId = tableId;
        this.capacity = capacity;
        this.status = status;
    }

    /**
     * Retrieves the unique table number.
     * @return The table ID.
     */
    public int getTableId() {
        return tableId;
    }

    /**
     * Updates the unique table number.
     * @param tableId The new table ID to set.
     */
    public void setTableId(int tableId) {
        this.tableId = tableId;
    }

    /**
     * Retrieves the seating capacity of the table.
     * @return The maximum number of seats.
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Updates the seating capacity of the table.
     * Used when the physical layout of the restaurant changes.
     * @param capacity The new number of seats.
     */
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
    
    /**
     * Retrieves the current availability status of the table.
     * @return The status string (e.g., AVAILABLE, OCCUPIED, BOOKED).
     */
    public String getStatus() { 
        return status;
    }

    /**
     * Updates the current availability status of the table.
     * This is typically updated when a customer is seated or pays the bill.
     * @param status The new status to set.
     */
    public void setStatus(String status) {
        this.status = status; 
    }
    
    /**
     * Returns a string representation of the Table object.
     * Useful for logging and debugging purposes.
     * @return A string containing the Table ID and Capacity.
     */
    @Override
    public String toString() {
        return "Table [ID=" + tableId + ", Capacity=" + capacity + "]";
    }
}