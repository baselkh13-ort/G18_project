// Ilya Zeldner
package server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class OrderRepository {
    
    public String getAllOrders() {
        String sql = "SELECT order_number, order_date, number_of_guests, confirmation_code, subscriber_id, date_of_placing_order"+ "FROM sys.`Order`";
        return executeReadQuery(sql);
    }

    public String updateOrder(int orderNumber, java.sql.Date orderDate, int guests) {
        String sql = "UPDATE sys.`Order` SET order_date = ?, number_of_guests = ? WHERE order_number = ?";
        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pConn = null;
        
        try {
            pConn = pool.getConnection();
            if (pConn == null) return "Error: Database Down";
            Connection conn = pConn.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setDate(1, orderDate);
            ps.setInt(2, guests);
            ps.setInt(3, orderNumber);
            int updated  = ps.executeUpdate();
            ps.close();
            if (updated == 0) return "ERROR: order_number not found";
            return "OK";

        } catch (SQLException e) {
            return "DB Error: " + e.getMessage();
        } finally {
            pool.releaseConnection(pConn);
        }
    }
    
    // A helper method to avoid repeating code
    private String executeReadQuery(String sql) {
        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pConn = null;
        StringBuilder result = new StringBuilder();

        try {
            pConn = pool.getConnection();
            if (pConn == null) return "Error: Database Down";

            Connection conn = pConn.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result.append("order_number=").append(rs.getInt("order_number"))
                      .append(", order_date=").append(rs.getDate("order_date"))
                      .append(", guests=").append(rs.getInt("number_of_guests"))
                      .append(", confirmation_code=").append(rs.getInt("confirmation_code"))
                      .append(", subscriber_id=").append(rs.getInt("subscriber_id"))
                      .append(", placed=").append(rs.getDate("date_of_placing_order"))
                      .append("\n");
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return "DB Error: " + e.getMessage();
        } finally {
            // Crucial: Return connection to the pool here!
            pool.releaseConnection(pConn);
        }
        
        return result.toString();
    }
}
