package server;

import java.sql.*;
import java.util.ArrayList;
import common.OpeningHour;

/**
 * Repository class specifically for managing the restaurant's opening and closing hours.
 */
public class OpeningHoursRepository {
    
    private final MySQLConnectionPool pool;
   
    
    /**
     * Constructor: Initializes the connection to the database.
     */
    public OpeningHoursRepository() {
        this.pool = MySQLConnectionPool.getInstance();
    }

    /**
     * Gets a list of all opening hours from the database.
     * @return A list of OpeningHour objects.
     */
    public ArrayList<OpeningHour> getAllOpeningHours() {
        ArrayList<OpeningHour> list = new ArrayList<>();
        String sql = "SELECT * FROM opening_hours";
        
        PooledConnection pConn = null; 
        
        try {
            pConn = pool.getConnection();
            ResultSet rs = pConn.getConnection().prepareStatement(sql).executeQuery();
            
            while (rs.next()) {
                OpeningHour hour = new OpeningHour(
                    rs.getInt("id"),
                    rs.getInt("day_of_week"),
                    rs.getDate("specific_date"),
                    rs.getTime("open_time"),
                    rs.getTime("close_time"),
                    rs.getBoolean("is_closed")
                );
                list.add(hour);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
        return list;
    }

    public OpeningHour getHoursForDate(java.sql.Date date, int dayOfWeek) {
        String sql = "SELECT * FROM opening_hours WHERE specific_date = ? " +
                     "OR (specific_date IS NULL AND day_of_week = ?) " +
                     "ORDER BY specific_date DESC LIMIT 1";
        
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            PreparedStatement ps = pConn.getConnection().prepareStatement(sql);
            ps.setDate(1, date);
            ps.setInt(2, dayOfWeek);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return new OpeningHour(
                    rs.getInt("id"),
                    rs.getInt("day_of_week"),
                    rs.getDate("specific_date"),
                    rs.getTime("open_time"),
                    rs.getTime("close_time"),
                    rs.getBoolean("is_closed")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
        return null;
    }

    /**
     * Updates the time or status (open/closed) for a specific record.
     * Fixed implementation using the Connection Pool.
     */

    public boolean updateOpeningHour(OpeningHour hour) {
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            Connection conn = pConn.getConnection();

            int rowsAffected = 0;
            
            if (hour.getSpecificDate() != null) {
                System.out.println("[Server DB] Trying to update Specific Date: " + hour.getSpecificDate());
                
                String updateSQL = "UPDATE opening_hours SET open_time = ?, close_time = ?, is_closed = ? WHERE specific_date = ?";
                PreparedStatement psUpdate = conn.prepareStatement(updateSQL);
                psUpdate.setTime(1, hour.getOpenTime());
                psUpdate.setTime(2, hour.getCloseTime());
                psUpdate.setBoolean(3, hour.isClosed());
                psUpdate.setDate(4, hour.getSpecificDate()); 
                
                rowsAffected = psUpdate.executeUpdate();
                psUpdate.close();
                
                if (rowsAffected == 0) {
                    System.out.println("[Server DB] Date not found. Inserting new row...");
                    String insertSQL = "INSERT INTO opening_hours (day_of_week, specific_date, open_time, close_time, is_closed) VALUES (0, ?, ?, ?, ?)";
                    PreparedStatement psInsert = conn.prepareStatement(insertSQL);
                    
                    psInsert.setDate(1, hour.getSpecificDate());
                    psInsert.setTime(2, hour.getOpenTime());
                    psInsert.setTime(3, hour.getCloseTime());
                    psInsert.setBoolean(4, hour.isClosed());
                    
                    rowsAffected = psInsert.executeUpdate();
                }
            } 
            else {
                System.out.println("[Server DB] Updating regular day: " + hour.getDayOfWeek());
                
                String sql = "UPDATE opening_hours SET open_time = ?, close_time = ?, is_closed = ? WHERE day_of_week = ?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setTime(1, hour.getOpenTime());
                ps.setTime(2, hour.getCloseTime());
                ps.setBoolean(3, hour.isClosed());
                ps.setInt(4, hour.getDayOfWeek());
                
                rowsAffected = ps.executeUpdate();
            }

            return rowsAffected > 0;

        } catch (SQLException e) {
            System.out.println("SQL ERROR ");
            System.out.println("Error while saving opening hours:");
            System.out.println("Message: " + e.getMessage());
            System.out.println("SQL State: " + e.getSQLState());
            e.printStackTrace();
            System.out.println("=======================================");
            return false;
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
    }
   
}