package server;

import java.sql.*;
import java.util.ArrayList;
import common.User;
import common.Role;

/**
 * Repository class for managing database operations related to users.
 *
 * Software Structure:
 * This class is the Data Access Object (DAO) for the Users table.
 * It is located in the Database Layer and is used by the Server to handle
 * user authentication and registration logic.
 *
 * UI Components:
 * This class powers the Login Screen (authentication), the Registration Screen (adding users),
 * and the Management Screen (viewing the list of subscribers).
 *
 * @author Dana Zablev
 * @version 1.0
 */
public class UserRepository {

    /** Connection pool to manage DB access. */
    private final MySQLConnectionPool pool;

    /**
     * Initializes the repository and gets an instance of the connection pool.
     */
    public UserRepository() {
        this.pool = MySQLConnectionPool.getInstance();
    }

    /**
     * Authenticates a user.
     * Enforces "Single User Connection": Checks if the user is already logged in.
     * If credentials are correct AND user is not logged in, sets is_logged_in = 1.
     *
     * @param username The username.
     * @param password The password.
     * @return The User object if login succeeds, null if failed or already connected.
     */
    public User login(String username, String password) {
        String selectSQL = "SELECT * FROM users WHERE username = ? AND password = ?";
        String updateSQL = "UPDATE users SET is_logged_in = 1 WHERE user_id = ?";
        
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            if (pConn == null) return null;

            Connection conn = pConn.getConnection();
            
            //  Check credentials
            PreparedStatement psSelect = conn.prepareStatement(selectSQL);
            psSelect.setString(1, username);
            psSelect.setString(2, password);
            ResultSet rs = psSelect.executeQuery();

            if (rs.next()) {
                // User found. Now check logic.
                boolean isAlreadyLoggedIn = rs.getBoolean("is_logged_in");
                
                if (isAlreadyLoggedIn) {
                    System.out.println("[Auth] Blocked login: User " + username + " is already connected.");
                    return null; // REJECT: Already online
                }

                //  Mark user as connected (is_logged_in = 1)
                User user = mapRowToUser(rs);
                
                PreparedStatement psUpdate = conn.prepareStatement(updateSQL);
                psUpdate.setInt(1, user.getUserId());
                psUpdate.executeUpdate();
                
                return user; // SUCCESS
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
        return null; // REJECT: Wrong password or User not found
    }

    /**
     * Identifies a user by scanning their unique QR code.
     * The scanner provides a string, which is parsed to an integer (member_code).
     *
     * @param qrCode The string representation of the scanned code.
     * @return The User object associated with the code, null if not found.
     */
    public User getUserByQRCode(String qrCode) {
        String sql = "SELECT * FROM users WHERE member_code = ?";
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            if (pConn == null) return null;

            Connection conn = pConn.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            
            // Parse string input from scanner to integer for DB lookup
            try {
                int code = Integer.parseInt(qrCode);
                ps.setInt(1, code);
            } catch (NumberFormatException e) {
                System.out.println("Error: Scanned QR is not a number.");
                return null;
            }

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapRowToUser(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
        return null;
    }

    /**
     * Retrieves all registered users (members) from the database.
     * Used by the Manager to see the subscriber list.
     *
     * @return An ArrayList of all User objects.
     */
    public ArrayList<User> getAllMembers() {
        ArrayList<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE role = 'MEMBER'";
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            if (pConn == null) return users;

            Connection conn = pConn.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                users.add(mapRowToUser(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
        return users;
    }

    /**
     * Checks if a username already exists in the system.
     * Used during registration to prevent duplicates.
     *
     * @param username The username to verify.
     * @return true if the username is taken, false otherwise.
     */
    public boolean isUsernameTaken(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            if (pConn == null) return true;

            Connection conn = pConn.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
        return false;
    }

    /**
     * Registers a new user into the database.
     * Generates a unique 6-digit member code for the new user.
     *
     * @param u The user object containing registration details.
     * @return The auto-generated member_code, or -1 on failure.
     */
    public int registerUser(User u) {
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            if (pConn == null) return -1;
            Connection conn = pConn.getConnection();

            int generatedMemberCode;
            boolean isCodeTaken;
            do {
                generatedMemberCode = (int)(Math.random() * 900000) + 100000;
                String checkSql = "SELECT COUNT(*) FROM users WHERE member_code = ?";
                PreparedStatement psCheck = conn.prepareStatement(checkSql);
                psCheck.setInt(1, generatedMemberCode);
                ResultSet rs = psCheck.executeQuery();
                rs.next();
                isCodeTaken = rs.getInt(1) > 0;
            } while (isCodeTaken); 

            u.setMemberCode(generatedMemberCode);
            
            String sql = "INSERT INTO users (user_id, username, password, first_name, last_name, role, phone, email, member_code) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, u.getUserId());
            ps.setString(2, u.getUsername());
            ps.setString(3, u.getPassword());
            ps.setString(4, u.getFirstName());
            ps.setString(5, u.getLastName());
            ps.setString(6, u.getRole().toString());
            ps.setString(7, u.getPhone());
            ps.setString(8, u.getEmail());
            ps.setInt(9, generatedMemberCode);

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                return generatedMemberCode;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
        return -1;
    }

    /**
    * Updates contact info (Phone and Email) for an existing user.
    *
    * @param userId The ID of the user.
    * @param newPhone The updated phone number.
    * @param newEmail The updated email address.
    * @return true if update succeeded.
    */
    public boolean updateUserInfo(int userId, String newPhone, String newEmail) {
        String sql = "UPDATE users SET phone = ?, email = ? WHERE user_id = ?";
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            if (pConn == null) return false;

            Connection conn = pConn.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, newPhone);
            ps.setString(2, newEmail);
            ps.setInt(3, userId);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
    }

    /**
     * Maps a ResultSet row to a User object.
     * Helper method to convert database rows into Java objects.
     *
     * @param rs The ResultSet from the database query.
     * @return A User object initialized with the row data.
     * @throws SQLException if data retrieval fails.
     */
    private User mapRowToUser(ResultSet rs) throws SQLException {
        return new User(
            rs.getInt("user_id"),
            rs.getString("username"),
            rs.getString("password"),
            rs.getString("first_name"),
            rs.getString("last_name"),
            Role.valueOf(rs.getString("role")),
            rs.getString("phone"),
            rs.getString("email"),
            rs.getInt("member_code") 
        );
    }
    /**
     * Logs out a user by updating their login status in the database.
     * Sets the 'is_logged_in' column to 0.
     * This allows the user to log in again from a different client later.
     * * @param userId The unique ID of the user to log out.
     */
    public void logoutUser(int userId) {
        String sql = "UPDATE users SET is_logged_in = 0 WHERE user_id = ?";
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            if (pConn == null) return;

            Connection conn = pConn.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
    }
    /**
     * Resets the login status of ALL users to 0 (Disconnected).
     * This should be called when the server starts to clear "stuck" sessions.
     */
    public void resetAllLoginStatus() {
        String sql = "UPDATE users SET is_logged_in = 0";
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            if (pConn == null) return;
            
            Connection conn = pConn.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.executeUpdate();
            System.out.println("[DB] All user login statuses have been reset to 0.");
            
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
    }
}