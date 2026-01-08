package logic;

import java.util.UUID;
import common.User;
import common.Role; 
import server.UserRepository;

/**
 * Service class for managing user-related business logic and restaurant policies.
 * Handles registration, contact updates, and discount calculations.
 */
public class UserManagement {

    private final UserRepository repo = new UserRepository();

    /**
     * Inner helper class to validate contact information.
     */
    static final class ContactInfo {
        ContactInfo(String phone, String email) {
            if ((phone == null || phone.trim().isEmpty()) && (email == null || email.trim().isEmpty())) {
                throw new IllegalArgumentException("At least one contact method (phone or email) is required.");
            }
        }
    }

    /**
     * Registers a new subscriber with validation and QR code generation.
     * @param callerRole The role of the user performing the registration.
     * @param newUser The user data to register.
     * @return The new User ID.
     */
    public int registerMember(Role callerRole, User newUser) {
        if (callerRole != Role.RESTAURANT_MANAGER && callerRole != Role.WORKER) {
            throw new IllegalStateException("Only staff can register new members.");
        }
        if (newUser.getUsername() == null || newUser.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required.");
        }
        if (repo.isUsernameTaken(newUser.getUsername())) {
            throw new IllegalStateException("Username already exists.");
        }

        new ContactInfo(newUser.getPhone(), newUser.getEmail());

        // Generate unique digital card ID
        String qrCode = UUID.randomUUID().toString();
        newUser.setQrCode(qrCode);

        int newId = repo.registerUser(newUser);
        if (newId == -1) throw new RuntimeException("DB Error during registration.");
        return newId;
    }

    /**
     * Calculates final price based on the restaurant's discount policy.
     * Rule: Members get 10% discount.
     */
    public double calculateFinalPrice(User user, double originalPrice) {
        if (user != null && user.getRole() == Role.MEMBER) {
            return originalPrice * 0.9; // 10% Discount
        }
        return originalPrice;
    }

    public void updateContact(int userId, String newPhone, String newEmail) {
        new ContactInfo(newPhone, newEmail);
        if (!repo.updateUserInfo(userId, newPhone, newEmail)) {
            throw new RuntimeException("Update failed.");
        }
    }
}