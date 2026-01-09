package server;

import java.sql.*;
import java.util.ArrayList;
import common.OpeningHour;

/**
 * Repository class specifically for managing the restaurant's opening and closing hours.
 */
public class OpeningHoursRepository {
    
    private final MySQLConnectionPool pool;

    public OpeningHoursRepository() {
        this.pool = MySQLConnectionPool.getInstance();
    }

    /**
     * Retrieves all configured opening hours rules.
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
                list.add(new OpeningHour(
                    rs.getInt("id"),
                    rs.getInt("day_of_week"),
                    rs.getDate("specific_date"),
                    rs.getTime("open_time"),
                    rs.getTime("close_time"),
                    rs.getBoolean("is_closed")
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
     * Updates an existing opening hour rule.
     * @param oh The OpeningHour object containing updated data.
     * @return true if update was successful.
     */
    public boolean updateOpeningHour(OpeningHour oh) {
        String sql = "UPDATE opening_hours SET open_time=?, close_time=?, is_closed=? WHERE id=?";
        PooledConnection pConn = null;
        try {
            pConn = pool.getConnection();
            PreparedStatement ps = pConn.getConnection().prepareStatement(sql);
            ps.setTime(1, oh.getOpenTime());
            ps.setTime(2, oh.getCloseTime());
            ps.setBoolean(3, oh.isClosed());
            ps.setInt(4, oh.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (pConn != null) pool.releaseConnection(pConn);
        }
    }
}