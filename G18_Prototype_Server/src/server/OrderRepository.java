package server;

import java.sql.*;
import java.util.ArrayList;
import java.util.Map;        
import java.util.HashMap;    
import common.Order;

/**
 * Repository class responsible for all database operations regarding Orders and Tables.
 *
 * Software Structure:
 * This class acts as the Data Access Object (DAO) in the Database Layer.
 * It contains all the SQL queries used to create, read, update, and delete orders.
 * It is used by the Server Controller and the Logic classes to access the MySQL database.
 *
 * Key Functionalities:
 * - Creating and retrieving orders.
 * - Managing waitlists and table assignments.
 * - Generating statistical data for reports.
 * - Handling automated tasks (cancellations, reminders)
 *
 * @author Dana Zablev
 * @version 1.0
 */
public class OrderRepository {

    /** The connection pool instance to manage DB connections. */
    private final MySQLConnectionPool pool;

    /**
     * Initializes the repository and retrieves the connection pool instance.
     */
    public OrderRepository() {
        this.pool = MySQLConnectionPool.getInstance();
    }

    /**
     * Creates a new order in the database.
     * Handles both registered members (subscriber_id) and casual customers (null id).
     *
     * @param o The Order object containing all reservation details.
     * @return The generated Order ID (primary key) from the database, or -1 if the operation failed.
     */
    public int createOrder(Order o) {
        String sql = "INSERT INTO bistro.`order` (order_date, number_of_guests, confirmation_code, subscriber_id, status, phone, email, customer_name, entered_waitlist) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            Connection conn = pConn.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            
            ps.setTimestamp(1, o.getOrderDate());
            ps.setInt(2, o.getNumberOfGuests());
            ps.setInt(3, o.getConfirmationCode());
            
            if (o.getMemberId() > 0) {
                ps.setInt(4, o.getMemberId());
            } else {
                ps.setNull(4, java.sql.Types.INTEGER);
            }
            
            ps.setString(5, o.getStatus());
            ps.setString(6, o.getPhone());
            ps.setString(7, o.getEmail());
            ps.setString(8, o.getCustomerName());
            
            // entered_waitlist
            ps.setBoolean(9, "WAITING".equals(o.getStatus()));

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) return rs.getInt(1); 
            }
        } catch (SQLException e) {
            System.err.println("SQL ERROR: " + e.getMessage()); 
            e.printStackTrace();
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
        return -1;
    }
    /**
     * Checks if a confirmation code already exists in the database.
     * Used to ensure the random code generator creates a unique code.
     *
     * @param code The code to check.
     * @return true if the code exists, false otherwise.
     */
    public boolean isCodeExists(int code) {
        String sql = "SELECT confirmation_code FROM bistro.`order` WHERE confirmation_code = ? AND status != 'CANCELLED'";
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            PreparedStatement ps = pConn.getConnection().prepareStatement(sql);
            ps.setInt(1, code);
            ResultSet rs = ps.executeQuery();
            return rs.next(); 
        } catch (SQLException e) {
            e.printStackTrace();
            return true; 
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
    }

    /**
     * Retrieves a specific order by its unique database ID (Primary Key).
     *
     * @param orderId The primary key of the order.
     * @return The Order object if found, or null otherwise.
     */
    public Order getOrderById(int orderId) {
        String sql = "SELECT * FROM bistro.`order` WHERE order_number = ?";
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            PreparedStatement ps = pConn.getConnection().prepareStatement(sql);
            ps.setInt(1, orderId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRowToOrder(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
        return null;
    }

    /**
     * Finds an order by the confirmation code provided by the customer.
     * Used during the arrival validation process at the restaurant entrance.
     *
     * @param code The 4-digit confirmation code.
     * @return The Order object if valid and not cancelled, or null.
     */
    public Order getOrderByCode(int code) {
    		String sql = "SELECT * FROM bistro.`order` WHERE confirmation_code = ? " +
    					"AND status IN ('PENDING', 'NOTIFIED', 'WAITING', 'SEATED', 'BILLED')";
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            PreparedStatement ps = pConn.getConnection().prepareStatement(sql);
            ps.setInt(1, code);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRowToOrder(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
        return null;
    }

    /**
     * Attempts to find an active order (PENDING/WAITING) for today using contact info.
     * Used for the "Restore Code" functionality if a customer lost their code.
     *
     * @param identifier Phone number or Email address.
     * @return The active Order object if found, or null.
     */
    public Order findOrderByContact(String identifier) {
        String sql = "SELECT * FROM bistro.`order` WHERE (phone = ? OR email = ?) " +
                "AND status IN ('PENDING', 'WAITING', 'NOTIFIED') " +
                "AND order_date >= CURDATE()"; 
        
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            PreparedStatement ps = pConn.getConnection().prepareStatement(sql);
            ps.setString(1, identifier);
            ps.setString(2, identifier);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRowToOrder(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
        return null;
    }

    /**
     * Retrieves the live waitlist and active orders for the dashboard.
     * Fetches orders with status WAITING, or PENDING for the current day.
     *
     * @return A list of orders currently active or waiting.
     */
    public ArrayList<Order> getLiveWaitingList() {
        ArrayList<Order> list = new ArrayList<>();
        String sql = "SELECT * FROM bistro.`order` " +
                     "WHERE status = 'WAITING' " +
                     "OR (status = 'PENDING' AND DATE(order_date) = CURDATE()) " +
                     "ORDER BY order_date ASC"; 
        
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            PreparedStatement ps = pConn.getConnection().prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRowToOrder(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
        return list;
    }

    /**
     * Assigns a physical table to an order upon arrival.
     * Logic: Finds a table with status 'AVAILABLE' and sufficient capacity.
     * Updates the order status to 'SEATED' and links the table ID.
     *
     * @param orderId The order to seat.
     * @param guests The number of guests (to find appropriate table size).
     * @return The Table ID assigned, or -1 if no suitable table is free.
     */
    public int assignFreeTable(int orderId, int guests) {
        String findTableSQL = "SELECT t.table_id FROM `tables` t " +
                              "WHERE t.capacity >= ? " + 
                              "AND t.status = 'AVAILABLE' LIMIT 1";

        String updateOrderSQL = "UPDATE bistro.`order` SET assigned_table_id = ?, status = 'SEATED', actual_arrival_time = NOW() WHERE order_number = ?";
        String updateTableSQL = "UPDATE `tables` SET status = 'OCCUPIED' WHERE table_id = ?";
        
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            Connection conn = pConn.getConnection();
            
            // Find a free table
            PreparedStatement psFind = conn.prepareStatement(findTableSQL);
            psFind.setInt(1, guests);
            ResultSet rs = psFind.executeQuery();
            
            int freeTableId = -1;
            if (rs.next()) {
                freeTableId = rs.getInt("table_id");
            } else {
                return -1; // No table found
            }
            
            //  Assign it to the order
            PreparedStatement psUpdateOrder = conn.prepareStatement(updateOrderSQL);
            psUpdateOrder.setInt(1, freeTableId);
            psUpdateOrder.setInt(2, orderId);
            psUpdateOrder.executeUpdate();

            // Mark table as OCCUPIED
            PreparedStatement psUpdateTable = conn.prepareStatement(updateTableSQL);
            psUpdateTable.setInt(1, freeTableId);
            psUpdateTable.executeUpdate();
            
            return freeTableId;

        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
    }

    /**
     * Retrieves the complete order history for a specific registered member.
     *
     * @param subscriberId The member's ID.
     * @return A list of all past and future orders for this user.
     */
    public ArrayList<Order> getMemberHistory(int subscriberId) {
        ArrayList<Order> history = new ArrayList<>();
        String sql = "SELECT * FROM bistro.`order` WHERE subscriber_id = ? ORDER BY order_date DESC";
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            PreparedStatement ps = pConn.getConnection().prepareStatement(sql);
            ps.setInt(1, subscriberId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                history.add(mapRowToOrder(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
        return history;
    }

    /**
     * Updates the status of an order (e.g., changing to CANCELLED, COMPLETED).
     *
     * @param orderNumber The ID of the order.
     * @param newStatus The new status string.
     * @return true if the update was successful.
     */
    public boolean updateOrderStatus(int orderNumber, String newStatus) {
        // Logic: Keep entered_waitlist TRUE if it was already TRUE, or set to TRUE if new status is WAITING
        String sql = "UPDATE bistro.`order` SET status = ?, entered_waitlist = (entered_waitlist OR ?) WHERE order_number = ?";
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            PreparedStatement ps = pConn.getConnection().prepareStatement(sql);
            
            ps.setString(1, newStatus);
            
            boolean isMovingToWaitlist = "WAITING".equals(newStatus);
            ps.setBoolean(2, isMovingToWaitlist);
            
            ps.setInt(3, orderNumber);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
    }

    /**
     * Updates the order time to the current server time.
     * Used when notifying a waiting customer that their table is ready now.
     *
     * @param orderNumber The order ID.
     */
    public void updateOrderTime(int orderNumber) {
        String sql = "UPDATE bistro.`order` SET order_date = NOW() WHERE order_number = ?";
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            PreparedStatement ps = pConn.getConnection().prepareStatement(sql);
            ps.setInt(1, orderNumber);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
    }

    /**
     * Updates the date and number of guests for an existing order.
     *
     * @param orderId The ID of the order to update.
     * @param newDate The new requested date and time.
     * @param newGuests The new number of guests.
     * @return true if the update was successful.
     */
    public boolean updateOrder(int orderId, java.sql.Timestamp newDate, int newGuests) {
        String sql = "UPDATE bistro.`order` SET order_date = ?, number_of_guests = ? WHERE order_number = ?";
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            PreparedStatement ps = pConn.getConnection().prepareStatement(sql);
            ps.setTimestamp(1, newDate);
            ps.setInt(2, newGuests);
            ps.setInt(3, orderId);
            
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
    }

    /**
     * Processes the payment for an order.
     * Sets status to COMPLETED, frees the table, and records the final price.
     *
     * @param orderId The order being paid.
     * @param finalPrice The amount paid after discounts.
     * @return true if payment recorded successfully.
     */
    public boolean processPayment(int orderId, double finalPrice) {
        Order order = getOrderById(orderId);
        if (order == null) {
                return false;
        }
        Integer tableId = order.getAssignedTableId();
        
        String sqlOrder = "UPDATE bistro.`order` SET total_price = ?, status = 'COMPLETED', assigned_table_id = NULL, actual_leave_time = NOW() WHERE order_number = ?";
        String sqlTable = "UPDATE `tables` SET status = 'AVAILABLE' WHERE table_id = ?";

        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            Connection conn = pConn.getConnection();
            
            // Update Order
            PreparedStatement psOrder = conn.prepareStatement(sqlOrder);
            psOrder.setDouble(1, finalPrice);
            psOrder.setInt(2, orderId);
            int rows = psOrder.executeUpdate();
            
            // Free Table (if one was assigned)
            if (rows > 0 && tableId != null) {
                PreparedStatement psTable = conn.prepareStatement(sqlTable);
                psTable.setInt(1, tableId);
                psTable.executeUpdate();
            }

            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
    }

    /**
     * Checks if there is a table available *right now* for a specific group size.
     * Used for Walk-in customers.
     *
     * @param requiredCapacity Number of seats required.
     * @return true if a table is free, false otherwise.
     */
    public boolean isTableAvailableNow(int requiredCapacity) {
        String sqlTotal = "SELECT COUNT(*) FROM `tables` WHERE capacity >= ?";
        String sqlOccupied = "SELECT COUNT(*) FROM `tables` WHERE capacity >= ? AND status = 'OCCUPIED'";
        
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            if (pConn == null) return false;
            
            Connection conn = pConn.getConnection();
            
            // Get Total Tables matching capacity
            PreparedStatement ps1 = conn.prepareStatement(sqlTotal);
            ps1.setInt(1, requiredCapacity);
            ResultSet rs1 = ps1.executeQuery();
            int total = 0;
            if (rs1.next()) total = rs1.getInt(1);

            // Get Occupied Tables matching capacity
            PreparedStatement ps2 = conn.prepareStatement(sqlOccupied);
            ps2.setInt(1, requiredCapacity);
            ResultSet rs2 = ps2.executeQuery();
            int occupied = 0;
            if (rs2.next()) occupied = rs2.getInt(1);

            return (total - occupied) > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
    }

    /**
     * Finds the next order in the waitlist that fits the given capacity.
     * Used to notify the next customer when a table is freed.
     *
     * @param capacity The size of the table that just became free.
     * @return The Order object of the next person in line, or null.
     */
    public Order getNextInWaitlist(int capacity) {
        String sql = "SELECT * FROM bistro.`order` WHERE status = 'WAITING' " +
                     "AND number_of_guests <= ? " +
                     "ORDER BY order_date ASC LIMIT 1";
        
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            PreparedStatement ps = pConn.getConnection().prepareStatement(sql);
            ps.setInt(1, capacity);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRowToOrder(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
        return null;
    }

    /**
     * Cancels orders where the customer is late OR waiting too long.
     * * Logic:
     * 1. WAITING customers -> Status becomes 'CANCELLED' (did not get a table).
     * 2. PENDING/NOTIFIED customers -> Status becomes 'NO_SHOW' (did not arrive).
     * 3. Frees any tables that were assigned to NO_SHOW customers.
     *
     * @param minutesThreshold Time allowed before cancellation (e.g., 15 mins).
     * @return Total number of orders cancelled.
     */
    public int cancelLateOrders(int minutesThreshold) {
        int totalCanceled = 0;
        PooledConnection pConn = null;

        //  Handle WAITING list (Change to CANCELLED)
        String cancelWaitingSQL = "UPDATE bistro.`order` SET status = 'CANCELLED' " +
                                  "WHERE status = 'WAITING' " +
                                  "AND TIMESTAMPDIFF(MINUTE, order_date, NOW()) > ?";

        //  Find tables to free for NO_SHOWs (PENDING/NOTIFIED only)
        String findTablesSQL = "SELECT assigned_table_id FROM bistro.`order` " +
                               "WHERE (status = 'PENDING' OR status = 'NOTIFIED') " +
                               "AND TIMESTAMPDIFF(MINUTE, order_date, NOW()) > ? " +
                               "AND assigned_table_id IS NOT NULL";

        //  Handle Late Arrivals (Change to NO_SHOW)
        String cancelNoShowSQL = "UPDATE bistro.`order` SET status = 'NO_SHOW', assigned_table_id = NULL " +
                                 "WHERE (status = 'PENDING' OR status = 'NOTIFIED') " +
                                 "AND TIMESTAMPDIFF(MINUTE, order_date, NOW()) > ?";

        String freeTableSQL = "UPDATE `tables` SET status = 'AVAILABLE' WHERE table_id = ?";

        try {
            pConn = pool.getConnection();
            Connection conn = pConn.getConnection();

            //  Cancel WAITING customers
            try (PreparedStatement psWaiting = conn.prepareStatement(cancelWaitingSQL)) {
                psWaiting.setInt(1, minutesThreshold);
                totalCanceled += psWaiting.executeUpdate();
            }

            //  Collect tables to free (only for PENDING/NOTIFIED)
            ArrayList<Integer> tablesToFree = new ArrayList<>();
            try (PreparedStatement psFind = conn.prepareStatement(findTablesSQL)) {
                psFind.setInt(1, minutesThreshold);
                ResultSet rs = psFind.executeQuery();
                while (rs.next()) {
                    tablesToFree.add(rs.getInt("assigned_table_id"));
                }
            }

            //  Cancel PENDING/NOTIFIED customers (NO_SHOW)
            try (PreparedStatement psNoShow = conn.prepareStatement(cancelNoShowSQL)) {
                psNoShow.setInt(1, minutesThreshold);
                totalCanceled += psNoShow.executeUpdate();
            }

            //  Free the tables
            if (!tablesToFree.isEmpty()) {
                try (PreparedStatement psFree = conn.prepareStatement(freeTableSQL)) {
                    for (int tableId : tablesToFree) {
                        psFree.setInt(1, tableId);
                        psFree.executeUpdate();
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
        
        return totalCanceled; // Returns total count so Scheduler can log it
    }
    
    /**
     * Cancels a specific order by its confirmation code.
     *
     * @param customerCode The confirmation code of the order.
     * @return true if the order was found and cancelled.
     */
    public boolean cancelOrderByCode(int customerCode) {
        String sql = "UPDATE bistro.`order` SET status = 'CANCELLED' " +
                "WHERE confirmation_code = ? AND status IN ('WAITING', 'NOTIFIED', 'PENDING')";
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            PreparedStatement ps = pConn.getConnection().prepareStatement(sql);
            ps.setInt(1, customerCode);
            
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
    }
    
    /**
     * Finds orders that are about 2 hours away and sends a reminder.
     *
     * @return A list of strings (messages) to send to customers.
     */
    public ArrayList<String> getRemindersList() {
        ArrayList<String> messages = new ArrayList<>();
        
        String sqlSelect = "SELECT * FROM bistro.`order` WHERE status = 'PENDING' " +
                "AND TIMESTAMPDIFF(MINUTE, NOW(), order_date) BETWEEN 115 AND 125";
        
        String sqlUpdate = "UPDATE bistro.`order` SET status = 'NOTIFIED' WHERE order_number = ?";
        
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            Connection conn = pConn.getConnection();
            ResultSet rs = conn.prepareStatement(sqlSelect).executeQuery();            
            
            while (rs.next()) {
                int orderNum = rs.getInt("order_number");
                
                String email = rs.getString("email"); 
                
                String msg = "Reminder for " + email + ": Your reservation is in 2 hours! Order #" + orderNum;
                
                messages.add(msg);
                
                PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate);
                psUpdate.setInt(1, orderNum);
                psUpdate.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
        return messages;
    }
    
    /**
     * Checks for orders that have been seated for 2 hours and sends an automatic invoice.
     * Changes status to 'BILLED'.
     *
     * @return A list of invoice strings to display/send.
     */
    public ArrayList<String> getAutomaticInvoices() {
        ArrayList<String> messages = new ArrayList<>();
        
        String sqlSelect = "SELECT * FROM bistro.`order` WHERE status = 'SEATED' " +
                "AND TIMESTAMPDIFF(MINUTE, order_date, NOW()) >= 120";
        String sqlUpdate = "UPDATE bistro.`order` SET status = 'BILLED' WHERE order_number = ?";
        PooledConnection pConn = null;
        try {
                pConn = pool.getConnection();
            Connection conn = pConn.getConnection();
            ResultSet rs = conn.prepareStatement(sqlSelect).executeQuery();
            
            while (rs.next()) {
                int guests = rs.getInt("number_of_guests");
                double price = guests * 100.0;
                
                String msg = "[Invoice] Your time is over Order " + rs.getInt("order_number") + 
                        " | Email: " + rs.getString("email") +
                        " | Details: " + guests + " Chef Meals" +
                        " | Total: " + price + " NIS";
                        
                messages.add(msg);
                PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate);
                psUpdate.setInt(1, rs.getInt("order_number"));
                psUpdate.executeUpdate();
                
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
        return messages;
    }

    /**
     * Counts how many orders overlap with a requested time.
     * Used for Reservation Availability Logic.
     *
     * @param checkTime The requested time to check against.
     * @return List of overlapping active orders.
     */
    public ArrayList<common.Order> getOverlappingOrders(java.sql.Timestamp checkTime) {
        ArrayList<common.Order> list = new ArrayList<>();
        
        String sql = "SELECT * FROM bistro.`order` " +
                     "WHERE status != 'CANCELLED' " +
                     "AND ABS(TIMESTAMPDIFF(MINUTE, order_date, ?)) < 120"; 

        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            PreparedStatement ps = pConn.getConnection().prepareStatement(sql);
            ps.setTimestamp(1, checkTime);
            
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
            
                common.Order order = new common.Order(); 
                order.setOrderNumber(rs.getInt("order_number"));
                order.setOrderDate(rs.getTimestamp("order_date"));
                order.setNumberOfGuests(rs.getInt("number_of_guests"));
                order.setPhone(rs.getString("phone"));
                order.setEmail(rs.getString("email"));
                order.setCustomerName(rs.getString("customer_name"));

                list.add(order);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
        return list;
    }
    
    /**
     * Retrieves a list of currently seated customers (active tables).
     * Includes status SEATED and BILLED.
     *
     * @return List of orders currently at a table.
     */
    public ArrayList<Order> getActiveDiners() {
        ArrayList<Order> list = new ArrayList<>();
        String sql = "SELECT * FROM bistro.`order` WHERE status IN ('SEATED', 'BILLED') ORDER BY order_date ASC";
        
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            PreparedStatement ps = pConn.getConnection().prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                list.add(mapRowToOrder(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
        return list;
    }
    
    
    /**
     * Retrieves ALL relevant orders for the current day/shift.
     * Includes: Future arrivals today, Waiting list, and Seated customers.
     * Excludes: Cancelled orders and Completed (paid) orders.
     *
     * @return List of active orders for the dashboard view.
     */
    public ArrayList<Order> getAllActiveOrdersForToday() {
        ArrayList<Order> list = new ArrayList<>();
        
        
        String sql = "SELECT * FROM bistro.`order` " +
                     "WHERE status IN ('SEATED', 'BILLED', 'WAITING', 'NOTIFIED') " +
                     "OR (status = 'PENDING' AND DATE(order_date) = CURDATE()) " +
                     "ORDER BY order_date ASC"; 
                     
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            PreparedStatement ps = pConn.getConnection().prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                list.add(mapRowToOrder(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
        return list;
    }
   
    /**
     * Retrieves all orders stored in the system.
     * Used for the general history view.
     *
     * @return List of all orders.
     */
    public ArrayList<Order> getAllOrders() {
        ArrayList<Order> ordersList = new ArrayList<>();
        String sql = "SELECT * FROM `bistro`.`order`";
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            PreparedStatement ps = pConn.getConnection().prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ordersList.add(mapRowToOrder(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
        return ordersList;
    }

    /**
     * Maps a ResultSet row to an Order object.
     * Helper method to reduce code duplication.
     *
     * @param rs The ResultSet from the query.
     * @return An Order object populated with data.
     * @throws SQLException If column access fails.
     */
    private Order mapRowToOrder(ResultSet rs) throws SQLException {
        Order o = new Order(
            rs.getInt("order_number"),
            rs.getTimestamp("order_date"),
            rs.getInt("number_of_guests"),
            rs.getInt("confirmation_code"),
            rs.getInt("subscriber_id"),
            rs.getTimestamp("date_of_placing_order")
        );
        int guests = rs.getInt("number_of_guests");
        o.setNumberOfGuests(guests);
        
        o.setStatus(rs.getString("status"));
        o.setTotalPrice(rs.getDouble("total_price"));
        o.setPhone(rs.getString("phone"));
        o.setEmail(rs.getString("email"));
        
        o.setCustomerName(rs.getString("customer_name"));
        
        o.setActualArrivalTime(rs.getTimestamp("actual_arrival_time"));
        o.setActualLeaveTime(rs.getTimestamp("actual_leave_time"));
        
        o.setAssignedTableId(rs.getInt("assigned_table_id"));
        if (rs.wasNull()) o.setAssignedTableId(null);
        
        return o;
    }

    /**
     * Generates data for the Monthly Performance Report based on specific client requirements.
     * Metrics included:
     * 1. Avg Arrival Lateness (Min): (Actual Arrival - Order Time)
     * 2. Avg Stay Duration (Min): (Actual Leave - Actual Arrival)
     * 3. Avg Departure Delay (Min): Time spent beyond the allocated 120 minutes.
     * 4. Late Arrivals Count: Count of orders arriving > 15 minutes late.
     * 5. General status: Total Completed, Waitlist entries.
     *
     * @param month The month to analyze.
     * @param year The year to analyze.
     * @return A map where Key is the metric name (String) and Value is the data (Integer).
     */
    public Map<String, Integer> getPerformanceReportData(int month, int year) {
        Map<String, Integer> data = new HashMap<>();
        
        Connection conn = null;
        PooledConnection pConn = null;

        //  Avg Arrival Lateness: Includes everyone (on-time arrivals count as 0)
        String sqlAvgArrivalLateness = "SELECT AVG(GREATEST(0, TIMESTAMPDIFF(MINUTE, order_date, actual_arrival_time))) " +
                                       "FROM bistro.`order` " +
                                       "WHERE (status = 'COMPLETED' OR status = 'SEATED') " +
                                       "AND actual_arrival_time IS NOT NULL " + 
                                       "AND MONTH(order_date) = ? AND YEAR(order_date) = ?";

        //  Avg Stay Duration: (Actual Leave - Actual Arrival)
        String sqlAvgStayDuration = "SELECT AVG(TIMESTAMPDIFF(MINUTE, actual_arrival_time, actual_leave_time)) " +
                                    "FROM bistro.`order` " +
                                    "WHERE status = 'COMPLETED' " +
                                    "AND actual_arrival_time IS NOT NULL AND actual_leave_time IS NOT NULL " +
                                    "AND MONTH(order_date) = ? AND YEAR(order_date) = ?";

        //  Avg Departure Delay: Time beyond 120 minutes (No delay counts as 0)
        String sqlAvgDepartureDelay = "SELECT AVG(GREATEST(0, TIMESTAMPDIFF(MINUTE, actual_arrival_time, actual_leave_time) - 120)) " +
                                      "FROM bistro.`order` " +
                                      "WHERE status = 'COMPLETED' " +
                                      "AND actual_arrival_time IS NOT NULL AND actual_leave_time IS NOT NULL " +
                                      "AND MONTH(order_date) = ? AND YEAR(order_date) = ?";

        //  Late Arrivals Count: Only counts significant delays (> 15 min)
        String sqlLateArrivalsCount = "SELECT COUNT(*) FROM bistro.`order` " +
                                      "WHERE (status = 'COMPLETED' OR status = 'SEATED') " +
                                      "AND actual_arrival_time IS NOT NULL " +
                                      "AND TIMESTAMPDIFF(MINUTE, order_date, actual_arrival_time) > 15 " +
                                      "AND MONTH(order_date) = ? AND YEAR(order_date) = ?";

        //  General Counts (Waitlist & Total Completed)
        String sqlCompleted = "SELECT COUNT(*) FROM bistro.`order` WHERE status = 'COMPLETED' AND MONTH(order_date) = ? AND YEAR(order_date) = ?";
        String sqlWaitlist  = "SELECT COUNT(*) FROM bistro.`order` WHERE entered_waitlist = 1 AND MONTH(order_date) = ? AND YEAR(order_date) = ?";

        try {
            pConn = pool.getConnection();
            conn = pConn.getConnection();

            // Execute Averages
            executeAvgQuery(conn, sqlAvgArrivalLateness, month, year, "Avg Arrival Lateness (Min)", data);
            executeAvgQuery(conn, sqlAvgStayDuration, month, year, "Avg Stay Duration (Min)", data);
            executeAvgQuery(conn, sqlAvgDepartureDelay, month, year, "Avg Departure Delay (Min)", data);

            // Execute Counts
            executeCountQuery(conn, sqlLateArrivalsCount, month, year, "Late Arrivals Count", data);
            executeCountQuery(conn, sqlCompleted, month, year, "Total Completed Visits", data);
            executeCountQuery(conn, sqlWaitlist, month, year, "Total Waitlist Entries", data);

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
        
        return data;
    }

    /**
    * Helper method to execute an Average SQL query and store result in the map.
    *
    * @param conn The database connection.
    * @param sql The SQL query string.
    * @param month The month parameter.
    * @param year The year parameter.
    * @param key The key to store in the map.
    * @param data The map to store the result.
    * @throws SQLException If a database error occurs.
    */
    private void executeAvgQuery(Connection conn, String sql, int month, int year, String key, Map<String, Integer> data) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, month);
            ps.setInt(2, year);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double val = rs.getDouble(1);
                    data.put(key, (int) val); 
                } else {
                    data.put(key, 0);
                }
            }
        }
    }

    /**
     * Helper method to execute a Count SQL query and store result in the map.
     *
     * @param conn The database connection.
     * @param sql The SQL query string.
     * @param month The month parameter.
     * @param year The year parameter.
     * @param key The key to store in the map.
     * @param data The map to store the result.
     * @throws SQLException If a database error occurs.
     */
    private void executeCountQuery(Connection conn, String sql, int month, int year, String key, Map<String, Integer> data) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, month);
            ps.setInt(2, year);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    data.put(key, rs.getInt(1));
                }
            }
        }
    }
    /**
     * Generates data for the Subscription Report (Subscribers only).
     * Collects two types of data:
     * 1. Total Orders by subscribers per day.
     * 2. Total Waitlist entries by subscribers per day.
     *
     * @param month The month.
     * @param year The year.
     * @return A map where Key distinguishes the day and type (e.g. "5" for orders, "W-5" for waitlist).
     */
    public Map<String, Integer> getSubscriptionReportData(int month, int year) {
        Map<String, Integer> data = new HashMap<>();
        
        //  Count Subscriber Orders per day
        String sqlOrders = "SELECT DAY(order_date) as day, COUNT(*) as count FROM bistro.`order` " +
                           "WHERE subscriber_id IS NOT NULL " + 
                           "AND MONTH(order_date) = ? AND YEAR(order_date) = ? " +
                           "GROUP BY DAY(order_date)";

        //  Count Subscriber Waitlist entries per day
        String sqlWaitlist = "SELECT DAY(order_date) as day, COUNT(*) as count FROM bistro.`order` " +
                             "WHERE subscriber_id IS NOT NULL " + 
                             "AND entered_waitlist = 1 " +        
                             "AND MONTH(order_date) = ? AND YEAR(order_date) = ? " +
                             "GROUP BY DAY(order_date)";

        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            if (pConn == null) return data;
            Connection conn = pConn.getConnection();

            // Execute Orders Query
            PreparedStatement psOrders = conn.prepareStatement(sqlOrders);
            psOrders.setInt(1, month);
            psOrders.setInt(2, year);
            ResultSet rsOrders = psOrders.executeQuery();
            while (rsOrders.next()) {
                // Key is just the day number (e.g., "1", "2")
                data.put(String.valueOf(rsOrders.getInt("day")), rsOrders.getInt("count"));
            }

            // Execute Waitlist Query
            PreparedStatement psWaitlist = conn.prepareStatement(sqlWaitlist);
            psWaitlist.setInt(1, month);
            psWaitlist.setInt(2, year);
            ResultSet rsWaitlist = psWaitlist.executeQuery();
            while (rsWaitlist.next()) {
                // Key is prefixed with W- (e.g., "W-1", "W-2") to distinguish from orders
                data.put("W-" + rsWaitlist.getInt("day"), rsWaitlist.getInt("count"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
        return data;
    }

    /**
     * Checks for orders conflicting with new opening hours, cancels them,
     * and returns the list of cancelled orders so the server can notify them.
     *
     * @param newRules The new OpeningHour object.
     * @return List of orders that were cancelled due to the change.
     */
    public ArrayList<common.Order> cancelConflictingOrders(common.OpeningHour newRules) {
        ArrayList<common.Order> cancelledList = new ArrayList<>();
        
        String sqlCancel = "UPDATE bistro.`order` SET status = 'CANCELLED' WHERE order_number = ?";
        
        String sqlSelect;
        if (newRules.getSpecificDate() != null) {
            sqlSelect = "SELECT * FROM bistro.`order` WHERE DATE(order_date) = ? AND status != 'CANCELLED'";
        } else {
            sqlSelect = "SELECT * FROM bistro.`order` WHERE DAYOFWEEK(order_date) = ? AND order_date > NOW() AND status != 'CANCELLED'";
        }

        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            Connection conn = pConn.getConnection();
            PreparedStatement psSelect = conn.prepareStatement(sqlSelect);
            
            if (newRules.getSpecificDate() != null) {
                psSelect.setDate(1, newRules.getSpecificDate());
            } else {
                psSelect.setInt(1, newRules.getDayOfWeek());
            }

            ResultSet rs = psSelect.executeQuery();
            
            while (rs.next()) {
                boolean shouldCancel = false;
                Time orderTime = rs.getTime("order_date");
                
                if (newRules.isClosed()) {
                    shouldCancel = true;
                } else {
                    java.time.LocalTime ord = orderTime.toLocalTime();
                    java.time.LocalTime open = newRules.getOpenTime().toLocalTime();
                    java.time.LocalTime close = newRules.getCloseTime().toLocalTime();
                    
                    if (ord.isBefore(open) || ord.isAfter(close)) {
                        shouldCancel = true;
                    }
                }
                
                if (shouldCancel) {
                    common.Order orderToCancel = mapRowToOrder(rs);
                    
                    PreparedStatement psCancel = conn.prepareStatement(sqlCancel);
                    psCancel.setInt(1, orderToCancel.getOrderNumber());
                    psCancel.executeUpdate();
                    
                    cancelledList.add(orderToCancel);
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
        
        return cancelledList; 
    }

    /**
     * Checks if a customer already has an active order (SEATED, WAITING, PENDING) for today.
     * Prevents double booking or entering waitlist while seated.
     *
     * @param phone The customer's phone.
     * @param email The customer's email.
     * @return true if an active order exists.
     */
    public boolean hasActiveOrder(String phone, String email) {
        String sql = "SELECT order_number FROM bistro.`order` " +
                     "WHERE ( " +
                     "  (? IS NOT NULL AND ? != '' AND phone = ?) " +
                     "  OR (? IS NOT NULL AND ? != '' AND email = ?) " +
                     ") " +
                     "AND status IN ('SEATED', 'WAITING', 'PENDING', 'NOTIFIED') " +
                     "AND DATE(order_date) = CURDATE()";
        
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            PreparedStatement ps = pConn.getConnection().prepareStatement(sql);
            
            ps.setString(1, phone);
            ps.setString(2, phone);
            ps.setString(3, phone);
            
            ps.setString(4, email);
            ps.setString(5, email);
            ps.setString(6, email);
            
            ResultSet rs = ps.executeQuery();
            return rs.next(); 
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
    }
    /**
     * Fetches active orders for a specific member scheduled for today.
     * Statuses included: 
     * - PENDING (Future reservation for today)
     * - WAITING (Currently in waitlist)
     * - NOTIFIED (Table ready, waiting for arrival)
     * * @param memberId The ID of the subscriber.
     * @return ArrayList of relevant orders.
     */
    public ArrayList<Order> getRelevantOrdersForToday(int memberId) {
        ArrayList<Order> list = new ArrayList<>();
        
      
        String sql = "SELECT * FROM bistro.`order` " +
                     "WHERE subscriber_id = ? " +
                     "AND DATE(order_date) = CURDATE() " +
                     "AND status IN ('PENDING', 'WAITING', 'NOTIFIED')";

        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            PreparedStatement ps = pConn.getConnection().prepareStatement(sql);
            ps.setInt(1, memberId);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                list.add(mapRowToOrder(rs)); 
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
        return list;
    }
}