package logic;

import java.io.Serializable;
import java.sql.Date;

// Represents an order entity implementing Serializable for data transfer.
public class Order implements Serializable {
	
	// Unique identifier for serialization versioning.
	private static final long serialVersionUID = 1L;
	
	private int orderNumber; // Unique ID identifying the order.
	private Date orderDate; // The scheduled date for the order.
	private int numberOfGuests; // Total count of guests included in the order.
	private int confirmationCode; // Unique code used for order verification.
	private int subscriberID; // ID of the subscriber who placed the order.
	private Date dateOfPlacingOrder; // The timestamp when the order was created.
	
	// Initializes a new Order instance with all attributes.
	public Order(int orderNumber, Date orderDate, int numberOfGuests, int confirmationCode, int subscriberID, Date dateOfPlacingOrder) {
		super();
		this.orderNumber = orderNumber;
		this.orderDate = orderDate;
		this.numberOfGuests = numberOfGuests;
		this.confirmationCode = confirmationCode;
		this.subscriberID = subscriberID;
		this.dateOfPlacingOrder = dateOfPlacingOrder;
	}

	// Retrieves the order number.
	public int getOrderNumber() {
		return orderNumber;
	}

	// Updates the order number.
	public void setOrderNumber(int orderNumber) {
		this.orderNumber = orderNumber;
	}

	// Retrieves the scheduled order date.
	public Date getOrderDate() {
		return orderDate;
	}

	// Updates the scheduled order date.
	public void setOrderDate(Date orderDate) {
		this.orderDate = orderDate;
	}

	// Retrieves the number of guests.
	public int getNumberOfGuests() {
		return numberOfGuests;
	}

	// Updates the number of guests.
	public void setNumberOfGuests(int numberOfGuests) {
		this.numberOfGuests = numberOfGuests;
	}

	// Retrieves the confirmation code.
	public int getConfirmationCode() {
		return confirmationCode;
	}

	// Updates the confirmation code.
	public void setConfirmationCode(int confirmationCode) {
		this.confirmationCode = confirmationCode;
	}

	// Retrieves the subscriber ID.
	public int getSubscriberID() {
		return subscriberID;
	}

	// Updates the subscriber ID.
	public void setSubscriberID(int subscriberID) {
		this.subscriberID = subscriberID;
	}

	// Retrieves the date the order was placed.
	public Date getDateOfPlacingOrder() {
		return dateOfPlacingOrder;
	}

	// Updates the date the order was placed.
	public void setDateOfPlacingOrder(Date dateOfPlacingOrder) {
		this.dateOfPlacingOrder = dateOfPlacingOrder;
	}
	
	// Returns a string representation of the order details.
	@Override
	public String toString() {
		return String.format("Order [Num=%s, Date=%s, Guests=%s, Code=%s, SubID=%s, PlacedOn=%s]", 
				orderNumber, orderDate, numberOfGuests, confirmationCode, subscriberID, dateOfPlacingOrder);
	}
}