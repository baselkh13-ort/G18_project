package server;

import java.sql.*;
import common.Table;
import common.Order; 
import java.util.ArrayList;

/**
 * Repository class for accessing and managing restaurant tables in the database.
 *
 * Software Structure:
 * This class belongs to the Database Layer. It handles all SQL queries related to the
 * "tables" table in the database. It is used by the Server Controller to Add, Remove,
 * or Update table configurations.
 *
 * UI Components:
 * This class supports the "Restaurant Management" screen, where the Manager can view
 * the list of tables, add new ones, or delete existing ones.
 *
 * @author Dana Zablev
 * @version 1.0
 */
public class TableRepository {
    
    /** The connection pool instance. */
    private final MySQLConnectionPool pool;

    /**
     * Constructor. Gets the connection pool instance.
     */
    public TableRepository() {
        this.pool = MySQLConnectionPool.getInstance();
    }

    /**
     * Counts how many tables exist that can fit a specific number of guests.
     * Used by the Logic Layer to check if the restaurant has large enough tables.
     *
     * @param guests The minimum capacity required.
     * @return The number of suitable tables found.
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

    /**
     * Adds a new table to the database.
     *
     * @param table The Table object containing ID and Capacity.
     * @return true if the table was added successfully.
     */
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

    /**
     * Removes a table from the database (Standard delete).
     *
     * @param tableId The ID of the table to remove.
     * @return true if the deletion was successful.
     */
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
    
    /**
     * Updates the capacity of an existing table.
     *
     * @param table The Table object with the new details.
     * @return true if the update was successful.
     */
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

    /**
     * Retrieves a list of all tables in the restaurant.
     *
     * @return An ArrayList of Table objects.
     */
    public ArrayList<Table> getAllTables() {
        ArrayList<Table> list = new ArrayList<>();
        String sql = "SELECT * FROM `tables`"; 
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            PreparedStatement ps = pConn.getConnection().prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Table(
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

    /**
     * Gets the seating capacity of a specific table.
     *
     * @param tableId The ID of the table.
     * @return The capacity (number of seats).
     */
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
     * Deletes a table safely by checking for conflicts.
     * Logic: If removing a table causes overbooking (more orders than remaining tables),
     * it cancels the excess orders and returns them so the server can notify the customers.
     *
     * @param tableId The ID of the table to remove.
     * @return ArrayList of Order objects that were cancelled due to this removal.
     */
   
    public ArrayList<Order> deleteTableSafely(int tableId) {
        ArrayList<Order> cancelledOrders = new ArrayList<>();
        PooledConnection pConn = null;
        
        try {
            pConn = pool.getConnection();
            Connection conn = pConn.getConnection();
            
            //  Check status
            String checkStatusSQL = "SELECT status FROM `tables` WHERE table_id = ?";
            PreparedStatement psStatus = conn.prepareStatement(checkStatusSQL);
            psStatus.setInt(1, tableId);
            ResultSet rsStatus = psStatus.executeQuery();

            if (!rsStatus.next()) return null;
            if ("OCCUPIED".equalsIgnoreCase(rsStatus.getString("status"))) {
                throw new IllegalStateException("Cannot remove table: It is currently OCCUPIED.");
            }

            //  Delete
            String deleteSQL = "DELETE FROM `tables` WHERE table_id = ?";
            PreparedStatement psDelete = conn.prepareStatement(deleteSQL);
            psDelete.setInt(1, tableId);
            psDelete.executeUpdate();

            //  Check for conflicts
            checkAndCancelConflicts(conn, cancelledOrders);

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } catch (IllegalStateException e) {
            throw e; 
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
        return cancelledOrders;
    }
    /**
     * Helper to perform the cancellation update and add the order to the list.
     */
    private void cancelOrderAndAddToList(Connection conn, ArrayList<Order> list, int orderId, ResultSet rs) throws SQLException {
        String cancelSQL = "UPDATE bistro.`order` SET status = 'CANCELLED' WHERE order_number = ?";
        PreparedStatement psCancel = conn.prepareStatement(cancelSQL);
        psCancel.setInt(1, orderId);
        psCancel.executeUpdate();

        Order o = new Order();
        o.setOrderNumber(orderId);
        o.setCustomerName(rs.getString("customer_name"));
        o.setEmail(rs.getString("email"));
        o.setOrderDate(rs.getTimestamp("order_date"));
        list.add(o);
    }
    /**
     * Updates a table's capacity safely.
     * 1. Checks if table is OCCUPIED (blocks update if so).
     * 2. Updates the capacity.
     * 3. Checks for conflicts with future orders and cancels them if necessary.
     * * @param table The table with new details.
     * @return List of cancelled orders, or null if table not found/error.
     */
    public ArrayList<Order> updateTableSafely(Table table) {
        ArrayList<Order> cancelledOrders = new ArrayList<>();
        PooledConnection pConn = null;
        
        try {
            pConn = pool.getConnection();
            Connection conn = pConn.getConnection();

            //  Check status (Cannot update occupied table)
            String checkSQL = "SELECT status FROM `tables` WHERE table_id = ?";
            PreparedStatement psCheck = conn.prepareStatement(checkSQL);
            psCheck.setInt(1, table.getTableId());
            ResultSet rs = psCheck.executeQuery();
            
            if (!rs.next()) return null; // Table not found
            if ("OCCUPIED".equalsIgnoreCase(rs.getString("status"))) {
                throw new IllegalStateException("Cannot update table: It is currently OCCUPIED.");
            }

            //  Perform the Update
            String updateSQL = "UPDATE `tables` SET capacity = ? WHERE table_id = ?";
            PreparedStatement psUpdate = conn.prepareStatement(updateSQL);
            psUpdate.setInt(1, table.getCapacity());
            psUpdate.setInt(2, table.getTableId());
            psUpdate.executeUpdate();

            //  Re-validate all future orders (Shared Logic)
            checkAndCancelConflicts(conn, cancelledOrders);

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } catch (IllegalStateException e) {
            throw e; 
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
        return cancelledOrders;
    }
    
    /**
     * Gets unique capacities for the scheduler logic.
     */
    public ArrayList<Integer> getUniqueCapacities() {
        ArrayList<Integer> capacities = new ArrayList<>();
        String sql = "SELECT DISTINCT capacity FROM `tables` ORDER BY capacity ASC";
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            if (pConn == null) return capacities;
            PreparedStatement ps = pConn.getConnection().prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                capacities.add(rs.getInt("capacity"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
        return capacities;
    }
    
    /**
     * Shared helper method to check for conflicts after any table change.
     * It scans all PENDING orders and cancels them if there are no suitable tables left.
     *
     * @param conn The DB connection.
     * @param cancelledOrders The list to fill with cancelled orders.
     */
    
    private void checkAndCancelConflicts(Connection conn, ArrayList<Order> cancelledOrders) throws SQLException {
        String findOrdersSQL = "SELECT * FROM bistro.`order` WHERE status = 'PENDING' AND order_date > NOW() ORDER BY order_date ASC";
        PreparedStatement psOrders = conn.prepareStatement(findOrdersSQL);
        ResultSet rsOrders = psOrders.executeQuery();

        while (rsOrders.next()) {
            int orderId = rsOrders.getInt("order_number");
            Timestamp orderDate = rsOrders.getTimestamp("order_date");
            int guestsInOrder = rsOrders.getInt("number_of_guests");

            //  Check if ANY table fits this group size
            String countSuitableSQL = "SELECT COUNT(*) FROM `tables` WHERE capacity >= ?";
            PreparedStatement psCount = conn.prepareStatement(countSuitableSQL);
            psCount.setInt(1, guestsInOrder);
            ResultSet rsCount = psCount.executeQuery();
            int suitableTablesCount = 0;
            if (rsCount.next()) suitableTablesCount = rsCount.getInt(1);

            if (suitableTablesCount == 0) {
                cancelOrderAndAddToList(conn, cancelledOrders, orderId, rsOrders);
                continue; 
            }

            //  Check specific time load
            String checkLoadSQL = "SELECT COUNT(*) FROM bistro.`order` WHERE status = 'PENDING' AND number_of_guests > 0 AND ABS(TIMESTAMPDIFF(MINUTE, order_date, ?)) < 120";
            PreparedStatement psCheck = conn.prepareStatement(checkLoadSQL);
            psCheck.setTimestamp(1, orderDate);
            ResultSet rsLoad = psCheck.executeQuery();

            if (rsLoad.next()) {
                int totalConcurrentOrders = rsLoad.getInt(1);
                
                String totalTablesSQL = "SELECT COUNT(*) FROM `tables`";
                PreparedStatement psTotal = conn.prepareStatement(totalTablesSQL);
                ResultSet rsTotal = psTotal.executeQuery();
                int totalRestaurantTables = 0;
                if (rsTotal.next()) totalRestaurantTables = rsTotal.getInt(1);

                if (totalConcurrentOrders > totalRestaurantTables) {
                     cancelOrderAndAddToList(conn, cancelledOrders, orderId, rsOrders);
                } 
            }
        }
    }
}