package server;

import java.sql.*;
import common.Table;

/**
 * Repository for accessing and managing table configuration data.
 * Handles adding, removing, updating, and querying tables.
 */
public class TableRepository {
    
    private final MySQLConnectionPool pool;

    public TableRepository() {
        this.pool = MySQLConnectionPool.getInstance();
    }

    /**
     * Counts how many tables exist in the restaurant that can fit the requested number of guests.
     * @param guests Number of people.
     * @return Total count of suitable tables.
     */
    public int countTablesByCapacity(int guests) {
        String sql = "SELECT COUNT(*) FROM tables WHERE capacity >= ?";
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
     * Adds a new table to the restaurant layout.
     * @param tableId The unique table number.
     * @param capacity The number of seats.
     * @return true if added successfully.
     */
    public boolean addTable(int tableId, int capacity) {
        String sql = "INSERT INTO tables (table_id, capacity) VALUES (?, ?)";
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            PreparedStatement ps = pConn.getConnection().prepareStatement(sql);
            ps.setInt(1, tableId);
            ps.setInt(2, capacity);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
    }

    /**
     * Removes a table from the restaurant.
     * @param tableId The table number to remove.
     * @return true if removed successfully.
     */
    public boolean removeTable(int tableId) {
        String sql = "DELETE FROM tables WHERE table_id = ?";
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
     * Updates an existing table's capacity.
     * @param tableId The table number.
     * @param newCapacity The new capacity.
     * @return true if updated successfully.
     */
    public boolean updateTable(int tableId, int newCapacity) {
        String sql = "UPDATE tables SET capacity = ? WHERE table_id = ?";
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            PreparedStatement ps = pConn.getConnection().prepareStatement(sql);
            ps.setInt(1, newCapacity);
            ps.setInt(2, tableId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
    }
}