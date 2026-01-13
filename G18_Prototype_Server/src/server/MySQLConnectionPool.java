// Ilya Zeldner
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

/**
 * Singleton class that manages a pool of MySQL database connections.
 * It reuses connections to improve performance and closes idle connections automatically.
 */
public class MySQLConnectionPool {

    private static MySQLConnectionPool instance;
    
    // Database Configuration
    private static String DB_URL = "jdbc:mysql://localhost:3306/bistro?serverTimezone=Asia/Jerusalem";    
    private static String USER = "root";
    private static String PASS = "Danadana1";

    // Pool Configuration
    private static int MAX_POOL_SIZE = 10;       // Maximum number of connections in the pool
    private static long MAX_IDLE_TIME = 5000;    // Time (ms) before an idle connection is closed
    private static long CHECK_INTERVAL = 2;      // Time (seconds) between cleanup checks

    private BlockingQueue<PooledConnection> pool; // Thread-safe queue to hold idle connections
    private ScheduledExecutorService cleanerService;

    /**
     * Private constructor to enforce Singleton pattern.
     * Initializes the connection queue and starts the background cleanup timer.
     */
    private MySQLConnectionPool() {
        pool = new LinkedBlockingQueue<>(MAX_POOL_SIZE);
        startCleanupTimer();
        System.out.println("[Pool] Initialized. Max Size: " + MAX_POOL_SIZE);
    }

    // Returns the single instance of the connection pool.
    
    public static synchronized MySQLConnectionPool getInstance() {
        if (instance == null) {
            instance = new MySQLConnectionPool();
        }
        return instance;
    }

    
    //Retrieves a connection for use.
    
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

    
     //Returns a connection back to the pool after use.
     // If the pool is full, the connection is physically closed.
     
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

    
     // Establishes a new connection to the MySQL database.
     
    private PooledConnection createNewConnection() {
        try {
            return new PooledConnection(DriverManager.getConnection(DB_URL, USER, PASS));
        } catch (SQLException e) {
            System.err.println("CONNECTION ERROR DETAILS");
            e.printStackTrace();
            return null;
        }
    }

    //Background Cleanup Logic 

  
    private void startCleanupTimer() {
        cleanerService = Executors.newSingleThreadScheduledExecutor();
        cleanerService.scheduleAtFixedRate(this::checkIdleConnections, CHECK_INTERVAL, CHECK_INTERVAL, TimeUnit.SECONDS);
    }

  
     //Checks all idle connections in the pool.
    
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
