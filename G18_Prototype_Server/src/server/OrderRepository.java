package server;

import java.sql.*;
import java.util.ArrayList;
import common.Order;

/**
 * Repository class responsible for all database operations regarding Orders and Tables.
 * Handles order creation, status updates, history retrieval, real-time availability, and waitlist management.
 */
public class OrderRepository {

    private final MySQLConnectionPool pool;

    /**
     * Initializes the repository and gets an instance of the connection pool.
     */
    public OrderRepository() {
        this.pool = MySQLConnectionPool.getInstance();
    }

    /**
     * Creates a new order entry in the database.
     * This handles both future reservations and immediate walk-in orders.
     * * @param o The order object containing details such as date, guests, and contact info.
     * @return The generated order_number (ID), or -1 on failure.
     */
    public int createOrder(Order o) {
        String sql = "INSERT INTO bistro.`order` (order_date, number_of_guests, confirmation_code, subscriber_id, status, phone, email) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            if (pConn == null) return -1;
            
            Connection conn = pConn.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            
            // If date is null (immediate walk-in), use current time
            Timestamp ts = (o.getOrderDate() != null) ? new Timestamp(o.getOrderDate().getTime()) : new Timestamp(System.currentTimeMillis());
            
            ps.setTimestamp(1, ts);
            ps.setInt(2, o.getNumberOfGuests());
            ps.setInt(3, o.getConfirmationCode());
            
            // Handle Subscriber ID (null if casual customer)
            if (o.getSubscriberId() > 0) {
                ps.setInt(4, o.getSubscriberId());
            } else {
                ps.setNull(4, java.sql.Types.INTEGER);
            }
            
            ps.setString(5, o.getStatus());
            ps.setString(6, o.getPhone());
            ps.setString(7, o.getEmail());

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
     * Fetches a single order by its unique order number.
     * * @param orderId The unique identifier of the order.
     * @return An Order object if found, otherwise null.
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
     * Retrieves the entire visit and order history for a specific registered subscriber.
     * * @param subscriberId The unique ID of the subscriber.
     * @return An ArrayList of Order objects belonging to the subscriber.
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
     * Updates the status of an existing order.
     * Used for check-ins (SEATED), cancellations (CANCELLED), or completion (COMPLETED).
     * * @param orderNumber The unique ID of the order.
     * @param newStatus The new status string to set.
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
     * Resets the order date to the current timestamp.
     * This is crucial when notifying a customer from the waitlist to ensure their
     * 15-minute arrival window starts from the moment of notification.
     * * @param orderNumber The unique ID of the order.
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
     * Updates the final calculated price of an order.
     * * @param orderNumber The unique ID of the order.
     * @param finalPrice The final price to be stored.
     * @return true if the update was successful.
     */
    public boolean updateOrderPrice(int orderNumber, double finalPrice) {
        String sql = "UPDATE bistro.`order` SET total_price = ? WHERE order_number = ?";
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            PreparedStatement ps = pConn.getConnection().prepareStatement(sql);
            ps.setDouble(1, finalPrice);
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
     * Checks if a physical table is currently available for immediate seating.
     * Logic compares the total tables of a certain capacity against the number of
     * currently active (SEATED) orders for that capacity.
     * * @param requiredCapacity The number of seats needed.
     * @return true if a table is free right now, false otherwise.
     */
    public boolean isTableAvailableNow(int requiredCapacity) {
        String sqlTotal = "SELECT COUNT(*) FROM tables WHERE capacity >= ?";
        String sqlOccupied = "SELECT COUNT(*) FROM bistro.`order` WHERE status = 'SEATED' AND number_of_guests >= ?";
        
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            if (pConn == null) return false;
            
            // 1. Get Total Tables
            PreparedStatement ps1 = pConn.getConnection().prepareStatement(sqlTotal);
            ps1.setInt(1, requiredCapacity);
            ResultSet rs1 = ps1.executeQuery();
            int total = 0;
            if (rs1.next()) total = rs1.getInt(1);

            // 2. Get Occupied Tables
            PreparedStatement ps2 = pConn.getConnection().prepareStatement(sqlOccupied);
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
     * Retrieves the next customer in the waitlist for a specific table capacity.
     * Orders are retrieved based on First-In-First-Out (FIFO) logic.
     * * @param capacity The capacity of the table that became available.
     * @return The Order object of the next person in line, or null if the queue is empty.
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
     * Automatically cancels orders if the customer has not arrived within the grace period.
     * Handles both future reservations (PENDING) and waitlist callbacks (NOTIFIED).
     * * @param minutesThreshold The time in minutes allowed before cancellation.
     * @return The number of cancelled orders.
     */
    public int cancelLateOrders(int minutesThreshold) {
        String sql = "UPDATE bistro.`order` SET status = 'CANCELLED' " +
                     "WHERE (status = 'PENDING' OR status = 'NOTIFIED') " +
                     "AND TIMESTAMPDIFF(MINUTE, order_date, NOW()) > ?";
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            PreparedStatement ps = pConn.getConnection().prepareStatement(sql);
            ps.setInt(1, minutesThreshold);
            return ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
    }

    /**
     * Scheduler Helper: Sends reminders to customers 2 hours before their scheduled time.
     */
    public void processReminders() {
        String sql = "SELECT * FROM bistro.`order` WHERE status = 'PENDING' " +
                     "AND TIMESTAMPDIFF(MINUTE, NOW(), order_date) BETWEEN 115 AND 125";
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            ResultSet rs = pConn.getConnection().prepareStatement(sql).executeQuery();
            while (rs.next()) {
                System.out.println("[SMS] Reminder for Order #" + rs.getInt("order_number") + " to " + rs.getString("phone"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
    }

    /**
     * Scheduler Helper: Sends invoices to customers 2 hours after they were seated.
     */
    public void processAutomaticInvoices() {
        String sql = "SELECT * FROM bistro.`order` WHERE status = 'SEATED' " +
                     "AND TIMESTAMPDIFF(MINUTE, order_date, NOW()) >= 120";
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            ResultSet rs = pConn.getConnection().prepareStatement(sql).executeQuery();
            while (rs.next()) {
                System.out.println("[Invoice] Sending bill for Order #" + rs.getInt("order_number") + " to email: " + rs.getString("email"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
    }

    /**
     * Counts how many confirmed orders exist around a specific time.
     * Used to calculate future table availability.
     * * @param orderTime The requested reservation time.
     * @param guests The number of guests required.
     * @return The count of occupied tables.
     */
    public int getOverlappingOrdersCount(java.sql.Timestamp orderTime, int guests) {
        String sql = "SELECT COUNT(*) FROM bistro.`order` " +
                     "WHERE status != 'CANCELLED' " +
                     "AND number_of_guests >= ? " + 
                     "AND ABS(TIMESTAMPDIFF(MINUTE, order_date, ?)) < 120"; 
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            if (pConn == null) return 0;
            PreparedStatement ps = pConn.getConnection().prepareStatement(sql);
            ps.setInt(1, guests);
            ps.setTimestamp(2, orderTime);
            
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
        return 0;
    }

    /**
     * Retrieves all orders currently in the database.
     * * @return An ArrayList of all Order objects.
     */
    public ArrayList<Order> getAllOrders() {
        ArrayList<Order> ordersList = new ArrayList<>();
        String sql = "SELECT * FROM bistro.`order`";
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
     * Maps a database ResultSet row to an Order object.
     * * @param rs The ResultSet containing the row data.
     * @return An instantiated Order object.
     * @throws SQLException If a database access error occurs.
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
        o.setStatus(rs.getString("status"));
        o.setTotalPrice(rs.getDouble("total_price"));
        o.setPhone(rs.getString("phone"));
        o.setEmail(rs.getString("email"));
        return o;
    }
}