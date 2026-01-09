package common;

import java.io.Serializable;

/**
 * Represents a physical dining table in the restaurant.
 * Shared entity for management and reservation logic.
 */
public class Table implements Serializable {
    
    private static final long serialVersionUID = 1L;

    private int tableId;
    private int capacity;

    /**
     * Constructs a Table object.
     * @param tableId The unique number of the table.
     * @param capacity The maximum number of guests it can seat.
     */
    public Table(int tableId, int capacity) {
        this.tableId = tableId;
        this.capacity = capacity;
    }

    /**
     * Gets the table ID.
     * @return The table number.
     */
    public int getTableId() {
        return tableId;
    }

    /**
     * Sets the table ID.
     * @param tableId The new table number.
     */
    public void setTableId(int tableId) {
        this.tableId = tableId;
    }

    /**
     * Gets the table capacity.
     * @return Number of seats.
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Sets the table capacity.
     * @param capacity Number of seats.
     */
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public String toString() {
        return "Table [ID=" + tableId + ", Capacity=" + capacity + "]";
    }
}