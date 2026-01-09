package server;

import java.sql.*;
import java.util.ArrayList;
import java.util.Map;      
import java.util.HashMap;  
import common.Order;

/**
 * Repository class responsible for all database operations regarding Orders and Tables.
 * Handles order creation, status updates, history retrieval, real-time availability, 
 * waitlist management, payment, and report generation (Missions 1-7).
 */
public class OrderRepository {

    private final MySQLConnectionPool pool;

    public OrderRepository() {
        this.pool = MySQLConnectionPool.getInstance();
    }

    //  Order Creation 
    public int createOrder(Order o) {
        String sql = "INSERT INTO bistro.`order` (order_date, number_of_guests, confirmation_code, subscriber_id, status, phone, email) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
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

    //  Reception Logic 
    
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

    public int assignFreeTable(int orderId, int guests) {
        String findTableSQL = "SELECT t.table_id FROM tables t " +
                              "WHERE t.capacity >= ? " +
                              "AND t.table_id NOT IN (" +
                              "   SELECT assigned_table_id FROM bistro.`order` " +
                              "   WHERE status = 'SEATED' AND assigned_table_id IS NOT NULL" +
                              ") LIMIT 1";

        String updateOrderSQL = "UPDATE bistro.`order` SET assigned_table_id = ?, status = 'SEATED' WHERE order_number = ?";
        
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            Connection conn = pConn.getConnection();
            
            // 1. Find a free table
            PreparedStatement psFind = conn.prepareStatement(findTableSQL);
            psFind.setInt(1, guests);
            ResultSet rs = psFind.executeQuery();
            
            int freeTableId = -1;
            if (rs.next()) {
                freeTableId = rs.getInt("table_id");
            } else {
                return -1; // No table found
            }
            
            // 2. Assign it to the order
            PreparedStatement psUpdate = conn.prepareStatement(updateOrderSQL);
            psUpdate.setInt(1, freeTableId);
            psUpdate.setInt(2, orderId);
            psUpdate.executeUpdate();
            
            return freeTableId;

        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
    }

    // History 
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

    //  Payment 
    public boolean processPayment(int orderId, double finalPrice) {
        
        String sql = "UPDATE bistro.`order` SET total_price = ?, status = 'COMPLETED', assigned_table_id = NULL " +
                     "WHERE order_number = ?";
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            PreparedStatement ps = pConn.getConnection().prepareStatement(sql);
            ps.setDouble(1, finalPrice);
            ps.setInt(2, orderId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
    }

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

    //  Management
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

    //  Map Row 
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
        
        o.setAssignedTableId(rs.getInt("assigned_table_id")); 
        
        return o;
    }

   

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
}