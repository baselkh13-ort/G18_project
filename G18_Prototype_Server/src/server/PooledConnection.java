// Ilya Zeldner
package server;

import java.sql.Connection;
import java.sql.SQLException;


//wrapper class for a JDBC Connection.
 
public class PooledConnection {
    
    private Connection connection; // The actual physical DB connection
    private long lastUsed;         // Timestamp of the last activity

    
    //Wraps a physical connection and starts the timer.
     
    public PooledConnection(Connection connection) {
        this.connection = connection;
        this.lastUsed = System.currentTimeMillis();
    }

    
    //Returns the underlying physical JDBC connection.
     
    public Connection getConnection() {
        return connection;
    }

   
     //Updates the timestamp to the current time.
    
    public void touch() {
        this.lastUsed = System.currentTimeMillis();
    }

  
 	//Returns the time (in ms) when this connection was last used.
     
    public long getLastUsed() {
        return lastUsed;
    }
    
    
   // Closes the physical connection to the database.
     
    public void closePhysicalConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}