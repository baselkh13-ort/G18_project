package logic;

import java.io.Serializable;
import java.sql.Date; // Imports the SQL-compatible Date class

// Implements Serializable so the Order object can be sent over the network
public class Order implements Serializable {
	
	// Unique ID for serialization version control
	private static final long serialVersionUID = 1L;
	
	// PRIVATE FIELDS
	
	private int orderNumber;        
	private Date orderDate;        
	private int numberOfGuests;   
	private int confirmationCode;   
	private int subscriberID;      
	private Date dateOfPlacingOrder;  
	
	
	// Initializes the object with all the necessary data
	public Order(int orderNumber, Date orderDate, int numberOfGuests, int confirmationCode, int subscriberID, Date dateOfPlacingOrder) {
		super();
		this.orderNumber = orderNumber;
		this.orderDate = orderDate;
		this.numberOfGuests = numberOfGuests;
		this.confirmationCode = confirmationCode;
		this.subscriberID = subscriberID;
		this.dateOfPlacingOrder = dateOfPlacingOrder;
	}

	//GETTERS AND SETTERS	
	
	public int getOrderNumber() {
		return orderNumber;
	}

	public void setOrderNumber(int orderNumber) {
		this.orderNumber = orderNumber;
	}

	public Date getOrderDate() {
		return orderDate;
	}

	public void setOrderDate(Date orderDate) {
		this.orderDate = orderDate;
	}

	public int getNumberOfGuests() {
		return numberOfGuests;
	}

	public void setNumberOfGuests(int numberOfGuests) {
		this.numberOfGuests = numberOfGuests;
	}

	public int getConfirmationCode() {
		return confirmationCode;
	}

	public void setConfirmationCode(int confirmationCode) {
		this.confirmationCode = confirmationCode;
	}

	public int getSubscriberID() {
		return subscriberID;
	}

	public void setSubscriberID(int subscriberID) {
		this.subscriberID = subscriberID;
	}

	public Date getDateOfPlacingOrder() {
		return dateOfPlacingOrder;
	}

	public void setDateOfPlacingOrder(Date dateOfPlacingOrder) {
		this.dateOfPlacingOrder = dateOfPlacingOrder;
	}
	
	// --- TO STRING ---
	// Used for debugging. If you print the object (System.out.println(order)),
	// this format will be displayed in the console.
	@Override
	public String toString() {
		return String.format("Order [Num=%s, Date=%s, Guests=%s, Code=%s, SubID=%s, PlacedOn=%s]", 
				orderNumber, orderDate, numberOfGuests, confirmationCode, subscriberID, dateOfPlacingOrder);
	}
}