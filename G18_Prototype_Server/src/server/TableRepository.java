package server;

import java.sql.*;
import common.Table;
import common.Order; // Make sure to import Order
import java.util.ArrayList;

/**
 * Repository for accessing and managing table configuration data.
 */
public class TableRepository {
    
    private final MySQLConnectionPool pool;

    public TableRepository() {
        this.pool = MySQLConnectionPool.getInstance();
    }

    /**
     * Counts how many tables exist in the restaurant that can fit the requested number of guests.
     */
    public int countTablesByCapacity(int guests) {
        String sql = "SELECT COUNT(*) FROM `tables` WHERE capacity >= ?";
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            PreparedStatement ps = pConn.getConnection().prepareStatement(sql);
            ps.setInt(1, guests);
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

    public boolean addTable(Table table) {
        String sql = "INSERT INTO `tables` (table_id, capacity, status) VALUES (?, ?, ?)";
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            PreparedStatement ps = pConn.getConnection().prepareStatement(sql);
            ps.setInt(1, table.getTableId()); 
            ps.setInt(2, table.getCapacity());
            ps.setString(3, "AVAILABLE"); // Default status
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
    }

    // Standard remove (used internally or for simple removals)
    public boolean removeTable(int tableId) {
        String sql = "DELETE FROM `tables` WHERE table_id = ?";
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            PreparedStatement ps = pConn.getConnection().prepareStatement(sql);
            ps.setInt(1, tableId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
    }
    
    public boolean updateTable(Table table) {
        String sql = "UPDATE `tables` SET capacity = ? WHERE table_id = ?";
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            PreparedStatement ps = pConn.getConnection().prepareStatement(sql);
            ps.setInt(1, table.getCapacity());
            ps.setInt(2, table.getTableId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
    }

    public ArrayList<common.Table> getAllTables() {
        ArrayList<common.Table> list = new ArrayList<>();
        String sql = "SELECT * FROM `tables`"; 
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            PreparedStatement ps = pConn.getConnection().prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new common.Table(
                    rs.getInt("table_id"),
                    rs.getInt("capacity"),
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
        return list;
    }

    public int getTableCapacity(int tableId) {
        String sql = "SELECT capacity FROM bistro.`tables` WHERE table_id = ?";
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            PreparedStatement ps = pConn.getConnection().prepareStatement(sql);
            ps.setInt(1, tableId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("capacity");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
        return 0; 
    }

    /**
     * Deletes a table, checks for overbooking issues, cancels excess orders, 
     * and returns the cancelled orders list.
     * @param tableId The ID of the table to remove.
     * @return ArrayList of Order objects that were cancelled.
     */
    public ArrayList<Order> deleteTableSafely(int tableId) {
        ArrayList<Order> cancelledOrders = new ArrayList<>();

        // 1. Get capacity before deletion
        int capacity = getTableCapacity(tableId);
        if (capacity == 0) return cancelledOrders; // Table not found

        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            Connection conn = pConn.getConnection();

            // 2. Delete the table
            String deleteSQL = "DELETE FROM `tables` WHERE table_id = ?";
            PreparedStatement psDelete = conn.prepareStatement(deleteSQL);
            psDelete.setInt(1, tableId);
            psDelete.executeUpdate();

            // 3. Count how many tables are LEFT with this capacity
            String countSQL = "SELECT COUNT(*) FROM `tables` WHERE capacity >= ?";
            PreparedStatement psCount = conn.prepareStatement(countSQL);
            psCount.setInt(1, capacity);
            ResultSet rsCount = psCount.executeQuery();
            int remainingTables = 0;
            if (rsCount.next()) remainingTables = rsCount.getInt(1);

            // 4. Find future PENDING orders that might be affected
            String findOrdersSQL = "SELECT * FROM bistro.`order` WHERE number_of_guests <= ? AND status = 'PENDING' AND order_date > NOW() ORDER BY order_date ASC";
            PreparedStatement psOrders = conn.prepareStatement(findOrdersSQL);
            psOrders.setInt(1, capacity); // Check orders that fit this table size
            ResultSet rsOrders = psOrders.executeQuery();

            // 5. Iterate and check for overbooking
            while (rsOrders.next()) {
                int orderId = rsOrders.getInt("order_number");
                Timestamp orderDate = rsOrders.getTimestamp("order_date");

                // Check how many orders exist in this specific time slot (2 hour window)
                String checkLoadSQL = "SELECT COUNT(*) FROM bistro.`order` " +
                                      "WHERE number_of_guests <= ? " +
                                      "AND status = 'PENDING' " +
                                      "AND ABS(TIMESTAMPDIFF(MINUTE, order_date, ?)) < 120";
                
                PreparedStatement psCheck = conn.prepareStatement(checkLoadSQL);
                psCheck.setInt(1, capacity);
                psCheck.setTimestamp(2, orderDate);
                ResultSet rsLoad = psCheck.executeQuery();

                if (rsLoad.next()) {
                    int currentLoad = rsLoad.getInt(1);

                    // If we have more orders than tables, we must cancel this one
                    if (currentLoad > remainingTables) {
                        
                        // Cancel in DB
                        String cancelSQL = "UPDATE bistro.`order` SET status = 'CANCELLED_BY_SYSTEM' WHERE order_number = ?";
                        PreparedStatement psCancel = conn.prepareStatement(cancelSQL);
                        psCancel.setInt(1, orderId);
                        psCancel.executeUpdate();

                        // Add to list for notification
                        Order o = new Order();
                        o.setOrderNumber(orderId);
                        o.setCustomerName(rsOrders.getString("customer_name"));
                        o.setEmail(rsOrders.getString("email"));
                        o.setOrderDate(orderDate);
                        
                        cancelledOrders.add(o);
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }

        return cancelledOrders;
    }
}