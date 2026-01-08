package common;

import java.io.Serializable;
import java.util.List;

public class User implements Serializable {

	private static final long serialVersionUID = 1L;

	private Role role;
	private String username;
	private String password;
	private int userID;
	
	//Member only details
	private String firstName;
	private String lastName;
	private String phone;
	private String email;
	private List<Order> ordersHistory;
	

	/**
	 * Single constructor for all roles. Some fields may be ignored / optional
	 * depending on the role.
	 *
	 * @param role      user role (MEMBER / WORKER / MANAGER)
	 * @param username  login username (required for all roles)
	 * @param password  login password (required for all roles)
	 * @param firstName first name (usually required)
	 * @param lastName  last name (usually required)
	 * @param phone     phone number (used for MEMBER)
	 * @param email     email (used for MEMBER )
	 * @param userID    internal id (can be 0 if not assigned yet)
	 */
	public User(Role role, String username, String password, String firstName, String lastName, String phone,
			String email, int userID) {
		
		this.role = role;
		this.username = username;
		this.password = password;
		this.userID = userID;

		switch (role) {

		case MEMBER:
			this.firstName = firstName;
			this.lastName = lastName;
			this.phone = phone;
			this.email = email;
			break;

		case WORKER:
			break;

		case MANAGER:
			break;

		default:
			throw new IllegalArgumentException("Unknown role: " + role);
		}
	}
	public Role getRole() {
		return role;
	}


	public void setRole(Role role) {
		this.role = role;
	}


	public String getUsername() {
		return username;
	}


	public void setUsername(String username) {
		this.username = username;
	}


	public String getPassword() {
		return password;
	}


	public void setPassword(String password) {
		this.password = password;
	}


	public int getUserID() {
		return userID;
	}


	public void setUserID(int userID) {
		this.userID = userID;
	}


	public String getFirstName() {
		return firstName;
	}


	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}


	public String getLastName() {
		return lastName;
	}


	public void setLastName(String lastName) {
		this.lastName = lastName;
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


	public List<Order> getOrdersHistory() {
		return ordersHistory;
	}


	public void setOrdersHistory(List<Order> ordersHistory) {
		this.ordersHistory = ordersHistory;
	}


	public static long getSerialversionuid() {
		return serialVersionUID;
	}

}
