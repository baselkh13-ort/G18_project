package server;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;
import java.sql.Connection;

/**
 * Singleton class that manages a pool of MySQL database connections.
 *
 * Software Structure:
 * This class is a utility component in the Infrastructure Layer. It uses the Singleton pattern
 * to ensure only one pool exists. All Repositories use this class to get access to the database.
 * It improves performance by reusing connections instead of creating new ones every time.
 *
 * UI Components:
 * Does not interact directly with the UI, but prints status logs to the server console.
 *
 * @author Dana Zablev
 * @version 1.0
 */
public class MySQLConnectionPool {

    private static MySQLConnectionPool instance;
    
    // Database Configuration
    private static String DB_URL = "jdbc:mysql://localhost:3306/bistro?serverTimezone=Asia/Jerusalem";    
    private static String USER = "root";
    private static String PASS;

    // Pool Configuration
    private static int MAX_POOL_SIZE = 10;       // Maximum number of connections in the pool
    private static long MAX_IDLE_TIME = 5000;    // Time (ms) before an idle connection is closed
    private static long CHECK_INTERVAL = 2;      // Time (seconds) between cleanup checks

    private BlockingQueue<PooledConnection> pool; // Thread-safe queue to hold idle connections
    private ScheduledExecutorService cleanerService;
    
    
    /**
     * Sets the password used to connect to the MySQL database.
     * This must be called before the first connection is attempted.
     * @param password The MySQL password provided by the user.
     */
    public static void setDBPassword(String password) {
        PASS = password;
    }

    /**
     * Private constructor to enforce Singleton pattern.
     * Initializes the connection queue and starts the background cleanup timer.
     */
    private MySQLConnectionPool() {
        pool = new LinkedBlockingQueue<>(MAX_POOL_SIZE);
        startCleanupTimer();
        System.out.println("[Pool] Initialized. Max Size: " + MAX_POOL_SIZE);
    }

    /**
     * Retrieves the single instance of the connection pool.
     * @return The singleton instance of MySQLConnectionPool.
     */
    public static synchronized MySQLConnectionPool getInstance() {
        if (instance == null) {
            instance = new MySQLConnectionPool();
        }
        return instance;
    }

    
    /**
     * Retrieves a connection for use from the pool.
     * If the pool is empty, a new physical connection is created.
     * @return A valid PooledConnection object.
     */
    public PooledConnection getConnection() {
        PooledConnection pConn = pool.poll(); // Try to get from queue
        
        if (pConn == null) {
            System.out.println("[Pool] Queue empty. Creating NEW physical connection!!!");
            return createNewConnection();
        }
        
        pConn.touch(); // Update last used time
        System.out.println("[Pool] Reusing existing connection.");
        return pConn;
    }

    /**
     * Returns a connection back to the pool after use.
     * If the pool is full, the connection is physically closed to save resources.
     * @param pConn The connection object to be released.
     */
    public void releaseConnection(PooledConnection pConn) {
        if (pConn != null) {
            pConn.touch();
            boolean added = pool.offer(pConn); // Return to queue
            if (added) {
                System.out.println("[Pool] Connection returned. Current Pool Size: " + pool.size());
            } else {
                // Pool is full, close the connection to save resources
                try { pConn.closePhysicalConnection(); } catch (Exception e) {}
            }
        }
    }

    
    /**
     * Establishes a new physical connection to the MySQL database.
     * @return A new PooledConnection wrapped around a JDBC Connection, or null if failed.
     */
    private PooledConnection createNewConnection() {
        try {
            return new PooledConnection(DriverManager.getConnection(DB_URL, USER, PASS));
        } catch (SQLException e) {
            System.err.println("CONNECTION ERROR DETAILS");
            e.printStackTrace();
            return null;
        }
    }

    
    // Background Cleanup Logic 
    /**
     * Starts a scheduled task that runs periodically to check for idle connections.
     */
    private void startCleanupTimer() {
        cleanerService = Executors.newSingleThreadScheduledExecutor();
        cleanerService.scheduleAtFixedRate(this::checkIdleConnections, CHECK_INTERVAL, CHECK_INTERVAL, TimeUnit.SECONDS);
    }

    /**
     * A helper method to test if a connection can be established.
     * @throws SQLException If connection fails.
     */
    public static void testConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
        conn.close(); 
    }

    /**
     * Checks all idle connections in the pool.
     * If a connection hasn't been used for longer than MAX_IDLE_TIME, it is closed and removed.
     */
    private void checkIdleConnections() {
        if (pool.isEmpty()) return;

        List<PooledConnection> activeConnections = new ArrayList<>();
        pool.drainTo(activeConnections); // Move all connections to a temp list for inspection

        long now = System.currentTimeMillis();
        int closedCount = 0;

        for (PooledConnection pConn : activeConnections) {
            if (now - pConn.getLastUsed() > MAX_IDLE_TIME) {
                try {
                    // Connection is too old -> Close it
                    pConn.closePhysicalConnection();
                    closedCount++;
                } catch (SQLException e) { e.printStackTrace(); }
            } else {
                // Connection is still fresh -> Return to pool
                pool.offer(pConn); 
            }
        }
        
        if (closedCount > 0) { 
            System.out.println("[Timer] Evicted " + closedCount + " idle connections. Pool Size: " + pool.size());
        }
    }
}