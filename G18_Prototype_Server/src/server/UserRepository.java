package server;

import java.sql.*;
import java.util.ArrayList;
import common.User;
import common.Role;

/**
 * Repository class for managing database operations related to users.
 * This class handles authentication, registration, user identification via QR,
 * and retrieval of user lists for management purposes.
 * Supports Missions 1 (User Mgmt) and 7 (Management Reports).
 */
public class UserRepository {

    private final MySQLConnectionPool pool;

    /**
     * Initializes the repository and gets an instance of the connection pool.
     */
    public UserRepository() {
        this.pool = MySQLConnectionPool.getInstance();
    }

    /**
     * Authenticates a user by their credentials.
     * @param username The unique username.
     * @param password The user password.
     * @return The User object if found, null otherwise.
     */
    public User login(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            if (pConn == null) return null;

            Connection conn = pConn.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ps.setString(2, password);
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
     * Identifies a user by scanning their unique QR code.
     * The scanner provides a string, which is parsed to an integer (member_code)
     * to perform the lookup in the database.
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
     * Retrieves all registered users (members, workers, managers) from the database.
     * Required for Mission 7 (Management View) to display the subscriber list.
     * @return An ArrayList of all User objects.
     */
    public ArrayList<User> getAllUsers() {
        ArrayList<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";
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
     * Note: Credit card information is NOT stored.
     * The member_code is stored as an integer for QR generation.
     * @param u The user object containing registration details.
     * @return The auto-generated user_id, or -1 on failure.
     */
    public int registerUser(User u) {
        String sql = "INSERT INTO users (username, password, first_name, last_name, role, phone, email, member_code) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            if (pConn == null) return -1;

            Connection conn = pConn.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getPassword());
            ps.setString(3, u.getFirstName());
            ps.setString(4, u.getLastName());
            ps.setString(5, u.getRole().toString());
            ps.setString(6, u.getPhone());
            ps.setString(7, u.getEmail());
            ps.setInt(8, u.getMemberCode()); // Storing the integer code

            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
        return -1;
    }

    /**
    * Updates contact info for an existing user.
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
     * Extracts member_code as an integer.
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
            rs.getInt("member_code") // Fetching the integer code
        );
    }
}