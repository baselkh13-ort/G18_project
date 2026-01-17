package logic;

import common.User;
import common.Role; 
import server.UserRepository;

/**
 * Service class for managing user-related business logic.
 *
 * Software Structure:
 * This class belongs to the Logic Layer. It handles business rules related to users,
 * such as registering new members and calculating discounts.
 * It connects the Server requests to the UserRepository in the Database Layer.
 *
 * @author Dana Zablev
 * @version 1.0
 */
public class UserManagement {

    /** The repository for accessing user data in the database. */
    private final UserRepository repo = new UserRepository();

    /**
     * Inner helper class to validate contact information.
     * Ensures that either phone or email is provided.
     */
    static final class ContactInfo {
        ContactInfo(String phone, String email) {
            if ((phone == null || phone.trim().isEmpty()) && (email == null || email.trim().isEmpty())) {
                throw new IllegalArgumentException("At least one contact method (phone or email) is required.");
            }
        }
    }

    /**
     * Registers a new subscriber into the system.
     * It checks permissions (only staff can register), validates the username,
     * and generates a unique member code.
     *
     * @param callerRole The role of the user trying to perform the registration.
     * @param newUser The object containing the new user's data.
     * @return The new User ID from the database.
     */
    public int registerMember(Role callerRole, User newUser) {
        
        if (callerRole != Role.MANAGER && callerRole != Role.WORKER) {
            throw new IllegalStateException("Only staff can register new members.");
        }
        
        if (newUser.getUsername() == null || newUser.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required.");
        }
        if (repo.isUsernameTaken(newUser.getUsername())) {
            throw new IllegalStateException("Username already exists.");
        }

        new ContactInfo(newUser.getPhone(), newUser.getEmail());

        int generatedMemberCode = (int)(Math.random() * 900000) + 100000; 
        newUser.setMemberCode(generatedMemberCode);

        int newId = repo.registerUser(newUser);
        if (newId == -1) throw new RuntimeException("Database Error during registration.");
        
        return newId;
    }

    /**
     * Calculates the final price for a bill based on the user type.
     * Rule: Registered Members get a 10% discount.
     *
     * @param user The user paying the bill.
     * @param originalPrice The original price before discount.
     * @return The final price to be paid.
     */
    public double calculateFinalPrice(User user, double originalPrice) {
        if (user != null && user.getRole() == Role.MEMBER) {
            return originalPrice * 0.9; // 10% Discount applied
        }
        return originalPrice;
    }

    /**
     * Updates the phone and email for an existing user.
     *
     * @param userId The ID of the user to update.
     * @param newPhone The new phone number.
     * @param newEmail The new email address.
     */
    public void updateContact(int userId, String newPhone, String newEmail) {
        new ContactInfo(newPhone, newEmail);
        if (!repo.updateUserInfo(userId, newPhone, newEmail)) {
            throw new RuntimeException("Update failed in database.");
        }
    }
}