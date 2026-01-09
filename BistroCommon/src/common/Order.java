package common;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Represents a dining order or reservation in the Bistro system.
 * Contains details about the schedule, guests, status, and associated table.
 * Used for Missions 2 (Reservation), 3 (Waitlist), 4 (Arrival), and 5 (Payment).
 */
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    // --- Database Identifiers ---
    private int orderNumber;
    private int confirmationCode;
    private int subscriberId;      // 0 if it is a casual customer (non-member)
    private Timestamp orderDate;   // Scheduled date and time of the visit
    private Timestamp dateOfPlacingOrder; // Timestamp of when the order was created
    
    // --- Order Details ---
    private int numberOfGuests;
    private String status;         // Enum values: PENDING, WAITING, NOTIFIED, SEATED, COMPLETED, CANCELLED
    private Double totalPrice;     // Calculated bill amount (Mission 5)
    private Integer assignedTableId; // Physical table ID, null if not currently seated (Mission 4)

    // --- Contact Info (For Casual & Members) ---
    private String phone;
    private String email;
    private String customerName;   // Optional display name (First + Last)

    /**
     * Full Constructor for Database Retrieval (Server Side).
     * Used when mapping a ResultSet row to an Order object.
     * * @param orderNumber       Unique order ID from DB.
     * @param orderDate         Scheduled time for the visit.
     * @param numberOfGuests    Amount of guests.
     * @param confirmationCode  Unique code for verification.
     * @param subscriberId      The Member ID (or 0 for casual).
     * @param dateOfPlacingOrder Creation timestamp.
     */
    public Order(int orderNumber, Timestamp orderDate, int numberOfGuests, int confirmationCode, 
                 int subscriberId, Timestamp dateOfPlacingOrder) {
        this.orderNumber = orderNumber;
        this.orderDate = orderDate;
        this.numberOfGuests = numberOfGuests;
        this.confirmationCode = confirmationCode;
        this.subscriberId = subscriberId;
        this.dateOfPlacingOrder = dateOfPlacingOrder;
    }

    /**
     * Partial Constructor for Client Side creation.
     * Used when a user fills out the reservation form.
     * * @param orderDate      Requested time for the visit.
     * @param numberOfGuests Amount of guests.
     * @param phone          Contact phone number.
     * @param email          Contact email address.
     */
    public Order(Timestamp orderDate, int numberOfGuests, String phone, String email) {
        this.orderDate = orderDate;
        this.numberOfGuests = numberOfGuests;
        this.phone = phone;
        this.email = email;
        this.status = "PENDING"; // Default status for new orders
        this.dateOfPlacingOrder = new Timestamp(System.currentTimeMillis());
    }

    // --- Getters and Setters ---

    public int getOrderNumber() { 
        return orderNumber; 
    }

    public void setOrderNumber(int orderNumber) { 
        this.orderNumber = orderNumber; 
    }

    public int getConfirmationCode() { 
        return confirmationCode; 
    }

    public void setConfirmationCode(int confirmationCode) { 
        this.confirmationCode = confirmationCode; 
    }

    public int getSubscriberId() { 
        return subscriberId; 
    }

    public void setSubscriberId(int subscriberId) { 
        this.subscriberId = subscriberId; 
    }

    public Timestamp getOrderDate() { 
        return orderDate; 
    }

    public void setOrderDate(Timestamp orderDate) { 
        this.orderDate = orderDate; 
    }

    public Timestamp getDateOfPlacingOrder() { 
        return dateOfPlacingOrder; 
    }

    public void setDateOfPlacingOrder(Timestamp dateOfPlacingOrder) { 
        this.dateOfPlacingOrder = dateOfPlacingOrder; 
    }

    public int getNumberOfGuests() { 
        return numberOfGuests; 
    }

    public void setNumberOfGuests(int numberOfGuests) { 
        this.numberOfGuests = numberOfGuests; 
    }

    public String getStatus() { 
        return status; 
    }

    public void setStatus(String status) { 
        this.status = status; 
    }

    public Double getTotalPrice() { 
        return totalPrice; 
    }

    public void setTotalPrice(Double totalPrice) { 
        this.totalPrice = totalPrice; 
    }

    public Integer getAssignedTableId() { 
        return assignedTableId; 
    }

    public void setAssignedTableId(Integer assignedTableId) { 
        this.assignedTableId = assignedTableId; 
    }

    public String getPhone() { 
        return phone; 
    }

    public void setPhone(String phone) { 
        this.phone = phone; 
    }

    public String getEmail() { 
        return email; 
    }

    public void setEmail(String email) { 
        this.email = email; 
    }
    
    public String getCustomerName() { 
        return customerName; 
    }

    public void setCustomerName(String customerName) { 
        this.customerName = customerName; 
    }

    @Override
    public String toString() {
        return "Order [ID=" + orderNumber + ", Date=" + orderDate + ", Status=" + status + "]";
    }
}