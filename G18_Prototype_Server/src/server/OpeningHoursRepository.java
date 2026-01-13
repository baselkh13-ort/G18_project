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
        String sql = "UPDATE opening_hours SET open_time = ?, close_time = ?, is_closed = ? WHERE day_of_week = ?";
        
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            PreparedStatement ps = pConn.getConnection().prepareStatement(sql);
            
            ps.setTime(1, hour.getOpenTime());
            ps.setTime(2, hour.getCloseTime());
            ps.setBoolean(3, hour.isClosed());
            ps.setInt(4, hour.getDayOfWeek()); 

            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
    }
}