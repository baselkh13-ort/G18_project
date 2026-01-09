package common;

import java.io.Serializable;

/**
 * Represents a registered user in the Bistro system.
 * Implements Serializable for network transmission between Server and Client.
 * This class handles all user roles: Manager, Worker, Member, and Casual Customer.
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    // Database Identifiers 
    private int userId;
    private String username;
    private String password;
    
    // Personal Information 
    private String firstName;
    private String lastName;
    private Role role; // Enum: MANAGER, WORKER, MEMBER
    
    //  Contact Information  for Members 
    private String phone;
    private String email;
    
    //  Membership Details 
    /**
     * Represents the unique integer code stored in the QR image.
     * When scanned, this code identifies the member in the database.
     */
    private int memberCode; 

    /**
     * Full Constructor for database retrieval.
     * * @param userId     The unique database ID (Primary Key).
     * @param username   Login username.
     * @param password   Login password.
     * @param firstName  User's first name.
     * @param lastName   User's last name.
     * @param role       User's role in the system.
     * @param phone      Contact phone number.
     * @param email      Contact email address.
     * @param memberCode Unique membership code (used for QR identification).
     */
    public User(int userId, String username, String password, String firstName, String lastName, 
                Role role, String phone, String email, int memberCode) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.phone = phone;
        this.email = email;
        this.memberCode = memberCode;
    }

    //  Getters and Setters 

    public int getUserId() { 
        return userId; 
    }

    public void setUserId(int userId) { 
        this.userId = userId; 
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

    public Role getRole() { 
        return role; 
    }

    public void setRole(Role role) { 
        this.role = role; 
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

    public int getMemberCode() { 
        return memberCode; 
    }

    public void setMemberCode(int memberCode) { 
        this.memberCode = memberCode; 
    }

    @Override
    public String toString() {
        return String.format("User [ID=%d, Role=%s, Name=%s %s]", userId, role, firstName, lastName);
    }
}