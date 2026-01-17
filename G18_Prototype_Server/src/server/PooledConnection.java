package server;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * A wrapper class for a JDBC Connection.
 *
 * Software Structure:
 * This class acts as a container within the Infrastructure Layer.
 * It holds the actual database connection object and tracks when it was last used.
 * This helps the MySQLConnectionPool decide which connections to keep and which to close.
 *
 * UI Components:
 * None. This is a low-level utility class.
 *
 * @author Dana Zablev
 * @version 1.0
 */
public class PooledConnection {
    
    /** The actual physical DB connection. */
    private Connection connection; 
    /** Timestamp of the last activity in milliseconds. */
    private long lastUsed;         

    
    /**
     * Constructor. Wraps a physical connection and starts the timer.
     *
     * @param connection The JDBC connection to wrap.
     */
    public PooledConnection(Connection connection) {
        this.connection = connection;
        this.lastUsed = System.currentTimeMillis();
    }

    
    /**
     * Returns the underlying physical JDBC connection.
     *
     * @return The SQL Connection object.
     */
    public Connection getConnection() {
        return connection;
    }

   
    /**
     * Updates the timestamp to the current time.
     * Call this whenever the connection is used.
     */
    public void touch() {
        this.lastUsed = System.currentTimeMillis();
    }

  
    /**
     * Returns the time (in ms) when this connection was last used.
     *
     * @return The timestamp of last usage.
     */
    public long getLastUsed() {
        return lastUsed;
    }
    
    
    /**
     * Closes the physical connection to the database.
     *
     * @throws SQLException If a database access error occurs.
     */
    public void closePhysicalConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}