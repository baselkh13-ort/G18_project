// Ilya Zeldner
package server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import common.Order;

/**
 * Repository class responsible for all database operations regarding Orders.
 * Executes SQL queries (SELECT, UPDATE) using the connection pool.
 */
public class OrderRepository {
    
    
    //Retrieves all orders from the database.
    //Maps the ResultSet into an ArrayList of Order objects.
     
    public ArrayList<Order> getAllOrders() {
        ArrayList<Order> ordersList = new ArrayList<>();
        String sql = "SELECT * FROM bistro.order"; 
        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pConn = null;
        try {
            // Get connection from pool
            pConn = pool.getConnection();
            if (pConn == null) return null;

            //Execute Query
            Connection conn = pConn.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            //Iterate over results and map to Order objects
            while (rs.next()) {
                int orderNumber = rs.getInt("order_number");
                java.sql.Date orderDate = rs.getDate("order_date");
                int guests = rs.getInt("number_of_guests");
                int confirmationCode = rs.getInt("confirmation_code");
                int subscriberId = rs.getInt("subscriber_id");
                java.sql.Date placed = rs.getDate("date_of_placing_order");

                ordersList.add(new Order(orderNumber, orderDate, guests, confirmationCode, subscriberId, placed));
            }
            
            rs.close();
            ps.close();
            
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            //Release connection back to pool
            pool.releaseConnection(pConn);
        }
        
        return ordersList;
    }

    //Updates the date and number of guests for a specific order.
     
    public String updateOrder(int orderNumber, java.sql.Date orderDate, int guests) {
        String sql = "UPDATE bistro.`order` SET order_date = ?, number_of_guests = ? WHERE order_number = ?";
        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pConn = null;
        
        try {
            pConn = pool.getConnection();
            if (pConn == null) return "Error: Database Down";

            Connection conn = pConn.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            
            // Set parameters
            ps.setDate(1, orderDate);
            ps.setInt(2, guests);
            ps.setInt(3, orderNumber);
            
            int updated = ps.executeUpdate();
            ps.close();
            
            if (updated == 0) return "ERROR: order_number not found";
            return "OK";

        } catch (SQLException e) {
            return "DB Error: " + e.getMessage();
        } finally {
            pool.releaseConnection(pConn);
        }
    }

    
    //Fetches a single order by its unique ID (Primary Key).
     
    public Order getOrderById(int orderId) {
        Order order = null;
        String sql = "SELECT * FROM bistro.order WHERE order_number = ?";
        
        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pConn = null;

        try {
            pConn = pool.getConnection();
            Connection conn = pConn.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, orderId); // Set ID parameter
            
            ResultSet rs = ps.executeQuery();
            
            // Check if a result exists (expecting only one)
            if (rs.next()) {
                 int orderNumber = rs.getInt("order_number");
                 java.sql.Date orderDate = rs.getDate("order_date");
                 int guests = rs.getInt("number_of_guests");
                 int confirmationCode = rs.getInt("confirmation_code");
                 int subscriberId = rs.getInt("subscriber_id");
                 java.sql.Date placed = rs.getDate("date_of_placing_order");

                 order = new Order(orderNumber, orderDate, guests, confirmationCode, subscriberId, placed);
            }
            
            rs.close();
            ps.close();
            
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            pool.releaseConnection(pConn);
        }
        
        return order;
    }
}
