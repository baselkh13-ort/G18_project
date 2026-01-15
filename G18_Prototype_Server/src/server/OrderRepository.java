package server;

import java.sql.*;
import java.util.ArrayList;
import java.util.Map;      
import java.util.HashMap;  
import common.Order;

/**
 * Repository class responsible for all database operations regarding Orders and Tables.
 * Handles order creation, status updates, history retrieval, real-time availability, 
 * waitlist management, payment, and report generation.
 */
public class OrderRepository {

    private final MySQLConnectionPool pool;

    /**
     * Initializes the repository and retrieves the connection pool instance.
     */
    public OrderRepository() {
        this.pool = MySQLConnectionPool.getInstance();
    }

    
    // Order Creation,Retrieval

    /**
     * Creates a new order in the database.
     * Handles both registered members (subscriber_id) and casual customers (null id).
     * * @param o The Order object containing all reservation details.
     * @return The generated Order ID (primary key) from the database, or -1 if the operation failed.
     */
    public int createOrder(Order o) {
        String sql = "INSERT INTO bistro.`order` (order_date, number_of_guests, confirmation_code, subscriber_id, status, phone, email, customer_name) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            if (pConn == null) return -1;
            
            Connection conn = pConn.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            
            Timestamp ts = (o.getOrderDate() != null) ? new Timestamp(o.getOrderDate().getTime()) : new Timestamp(System.currentTimeMillis());
            
            ps.setTimestamp(1, ts);
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

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
        return -1;
    }

    /**
     * Retrieves a specific order by its unique database ID.
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

    
    // Arrival,Seating
 
    
    /**
     * Finds an order by the confirmation code provided by the customer.
     * Used during the arrival validation process.
     * @param code The 4-digit confirmation code.
     * @return The Order object if valid and not cancelled, or null.
     */
    public Order getOrderByCode(int code) {
        String sql = "SELECT * FROM bistro.`order` WHERE confirmation_code = ? AND status != 'CANCELLED'";
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
     * Used for the "Restore Code" functionality.
     * @param identifier Phone number or Email address.
     * @return The active Order object if found, or null.
     */
    public Order findOrderByContact(String identifier) {
        String sql = "SELECT * FROM bistro.`order` WHERE (phone = ? OR email = ?) " +
                     "AND status IN ('PENDING', 'WAITING', 'NOTIFIED') " +
                     "AND DATE(order_date) = CURDATE()"; 
        
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
     * Fetches orders with status WAITING, or PENDING for today.
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
     * * @param orderId The order to seat.
     * @param guests The number of guests (to find appropriate table size).
     * @return The Table ID assigned, or -1 if no suitable table is free.
     */
    public int assignFreeTable(int orderId, int guests) {
        // Updated to use backticks for `tables` as it's a reserved keyword
        String findTableSQL = "SELECT t.table_id FROM `tables` t " +
                              "WHERE t.capacity >= ? " + 
                              "AND t.status = 'AVAILABLE' LIMIT 1";

        String updateOrderSQL = "UPDATE bistro.`order` SET assigned_table_id = ?, status = 'SEATED' WHERE order_number = ?";
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

  
    // History and Status Management
    /**
     * Retrieves the complete order history for a specific registered member.
     * @param subscriberId The member's ID.
     * @return A list of all past and future orders.
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
     * Updates the status of an order  (CANCELLED, COMPLETED).
     * @param orderNumber The ID of the order.
     * @param newStatus The new status string.
     * @return true if the update was successful.
     */
    public boolean updateOrderStatus(int orderNumber, String newStatus) {
        String sql = "UPDATE bistro.`order` SET status = ? WHERE order_number = ?";
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            PreparedStatement ps = pConn.getConnection().prepareStatement(sql);
            ps.setString(1, newStatus);
            ps.setInt(2, orderNumber);
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
  
    // Payment Processing
    /**
     * Processes the payment for an order.
     * Sets status to COMPLETED, frees the table, and records the final price.
     * @param orderId The order being paid.
     * @param finalPrice The amount paid after discounts.
     * @return true if payment recorded successfully.
     */
    public boolean processPayment(int orderId, double finalPrice) {
        // Also need to free the table in the tables table
        // First get the table ID
        Order order = getOrderById(orderId);
        if (order == null) {
        		return false;
        }
        Integer tableId = order.getAssignedTableId();
        //COMPLETED
        String sqlOrder = "UPDATE bistro.`order` SET total_price = ?, status = 'COMPLETED', assigned_table_id = NULL " +
                          "WHERE order_number = ?";
        //AVAILABLE 	
        String sqlTable = "UPDATE `tables` SET status = 'AVAILABLE' WHERE table_id = ?";

        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            Connection conn = pConn.getConnection();
            
            
            //Update Order
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


    // Waitlist and Availability Logic
    /**
     * Checks if there is a table available *right now* for a specific group size.
     * Used for Walk-in customers.
     * @param requiredCapacity Number of seats required.
     * @return true if a table is free, false otherwise.
     */
    public boolean isTableAvailableNow(int requiredCapacity) {
        // Logic: Count total tables of size X minus occupied tables of size X
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

            //Get Occupied Tables matching capacity
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

   
    // Scheduler Tasks 
    /**
     * Cancels orders where the customer is late by more than the specified minutes.
     * @param minutesThreshold Time allowed before cancellation (e.g., 15 mins).
     * @return Number of orders cancelled.
     */
    public int cancelLateOrders(int minutesThreshold) {
        String findTablesSQL = "SELECT assigned_table_id FROM bistro.`order` " +
                               "WHERE (status = 'PENDING' OR status = 'NOTIFIED') " +
                               "AND TIMESTAMPDIFF(MINUTE, order_date, NOW()) > ? " +
                               "AND assigned_table_id IS NOT NULL";

        String cancelOrdersSQL = "UPDATE bistro.`order` SET status = 'CANCELLED', assigned_table_id = NULL " +
                                 "WHERE (status = 'PENDING' OR status = 'NOTIFIED') " +
                                 "AND TIMESTAMPDIFF(MINUTE, order_date, NOW()) > ?";

        String freeTableSQL = "UPDATE `tables` SET status = 'AVAILABLE' WHERE table_id = ?";

        PooledConnection pConn = null;
        int canceledCount = 0;

        try {
            pConn = pool.getConnection();
            Connection conn = pConn.getConnection();
            
            PreparedStatement psFind = conn.prepareStatement(findTablesSQL);
            psFind.setInt(1, minutesThreshold);
            ResultSet rs = psFind.executeQuery();
            
            ArrayList<Integer> tablesToFree = new ArrayList<>();
            while (rs.next()) {
                tablesToFree.add(rs.getInt("assigned_table_id"));
            }

            PreparedStatement psCancel = conn.prepareStatement(cancelOrdersSQL);
            psCancel.setInt(1, minutesThreshold);
            canceledCount = psCancel.executeUpdate();

            if (!tablesToFree.isEmpty()) {
                PreparedStatement psFree = conn.prepareStatement(freeTableSQL);
                for (int tableId : tablesToFree) {
                    psFree.setInt(1, tableId);
                    psFree.executeUpdate();
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
        return canceledCount;
    }
    
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
     * @param orderTime The requested time.
     * @param guests Number of guests.
     * @return Count of overlapping active orders.
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
   
    // Management and Reports
    /**
     * Retrieves all orders in the system.
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
        
        o.setAssignedTableId(rs.getInt("assigned_table_id"));
        if (rs.wasNull()) o.setAssignedTableId(null);
        
        return o;
    }

    /**
     * Generates data for the Monthly Performance Report.
     * Compares "Late/Cancelled" vs "Completed/On-Time" orders.
     * @param month The month to analyze.
     * @param year The year to analyze.
     * @return A map where Key is the category and Value is the count.
     */
    public Map<String, Integer> getPerformanceReportData(int month, int year) {
        Map<String, Integer> data = new HashMap<>();
        
        String sqlLate = "SELECT COUNT(*) FROM bistro.`order` " +
                         "WHERE status = 'CANCELLED' AND MONTH(order_date) = ? AND YEAR(order_date) = ?";
                         
        String sqlOnTime = "SELECT COUNT(*) FROM bistro.`order` " +
                           "WHERE status = 'COMPLETED' AND MONTH(order_date) = ? AND YEAR(order_date) = ?";

        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            if (pConn == null) return data;
            
            PreparedStatement ps1 = pConn.getConnection().prepareStatement(sqlLate);
            ps1.setInt(1, month);
            ps1.setInt(2, year);
            ResultSet rs1 = ps1.executeQuery();
            if (rs1.next()) data.put("Late/Cancelled", rs1.getInt(1));

            PreparedStatement ps2 = pConn.getConnection().prepareStatement(sqlOnTime);
            ps2.setInt(1, month);
            ps2.setInt(2, year);
            ResultSet rs2 = ps2.executeQuery();
            if (rs2.next()) data.put("On-Time", rs2.getInt(1));

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
        return data;
    }

    /**
     * Generates data for the Subscription/Visits Report.
     * Counts total orders per day for the specified month.
     * @param month The month.
     * @param year The year.
     * @return A map where Key is the Day of Month (as String) and Value is the order count.
     */
    public Map<String, Integer> getSubscriptionReportData(int month, int year) {
        Map<String, Integer> data = new HashMap<>();
        
        String sql = "SELECT DAY(order_date) as day, COUNT(*) as count " +
                     "FROM bistro.`order` " +
                     "WHERE MONTH(order_date) = ? AND YEAR(order_date) = ? " +
                     "GROUP BY DAY(order_date) ORDER BY day ASC";
                     
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            if (pConn == null) return data;

            PreparedStatement ps = pConn.getConnection().prepareStatement(sql);
            ps.setInt(1, month);
            ps.setInt(2, year);
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
                data.put(String.valueOf(rs.getInt("day")), rs.getInt("count"));
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
   
}