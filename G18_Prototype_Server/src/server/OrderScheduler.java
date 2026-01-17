package server;

import java.util.Timer;
import java.util.TimerTask;
import java.util.ArrayList;
import ocsf.server.AbstractServer; 
import common.ActionType;
import common.BistroMessage;
import common.ChatIF;

/**
 * Background scheduler that runs periodically to handle automated tasks.
 *
 * Software Structure:
 * This class runs as a separate background process within the Server Layer.
 * It is not triggered by a client request but runs automatically based on a timer.
 * It communicates with the OrderRepository to check for overdue orders.
 *
 * UI Components:
 * This class triggers notifications (simulated SMS/Emails) that are sent to the clients.
 * It also logs its activities to the Server Console UI.
 *
 * @author Dana Zablev
 * @version 1.0
 */
public class OrderScheduler {

    /** The repository for database access. */
    private final OrderRepository orderRepo;
    /** The timer for scheduling tasks. */
    private final Timer timer;
    /** Interface to log to the Server GUI. */
    private final ChatIF ui;
    /** Reference to the server to send messages to clients. */
    private final AbstractServer server;

    /**
     * Initializes the scheduler with the order repository and server reference.
     *
     * @param orderRepo The repository used to perform database updates.
     * @param ui The GUI interface for logging.
     * @param server The main server object to send notifications.
     */
    public OrderScheduler(OrderRepository orderRepo, ChatIF ui, AbstractServer server) {
        this.orderRepo = orderRepo;
        this.ui = ui;
        this.server = server; 
        this.timer = new Timer(true);
    }

    /**
     * Starts the background task loop.
     * The task will run every 10 seconds (for testing purposes).
     */
    public void start() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    processAutomatedTasks();
                } catch (Exception e) {
                    System.err.println("[Scheduler Error] " + e.getMessage());
                }
            }
        }, 5000, 10000); // Wait 5 seconds to start, then run every 10 seconds
    }

    /**
     * Executes all time-based restaurant policies.
     * 1. Cancels orders if customers are more than 15 minutes late.
     * 2. Sends reminders for orders that are 2 hours away.
     * 3. Sends invoices for tables that have been occupied for 2 hours.
     */
    private void processAutomatedTasks() {
        System.out.println("[Scheduler] Running automated time checks...");

        // Auto-cancel orders if the customer is 15 minutes late
        int canceledCount = orderRepo.cancelLateOrders(15);
        if (canceledCount > 0) {
                String cancelMsg = "[Scheduler] " + canceledCount + " late orders were automatically canceled.";
            System.out.println(cancelMsg);
            if (ui != null) {
                    ui.display(cancelMsg);
            }
        }

        ArrayList<String> reminders = orderRepo.getRemindersList();
        for (String msg : reminders) {
            System.out.println("[Scheduler] Sending Reminder: " + msg);
            server.sendToAllClients(new BistroMessage(ActionType.SERVER_NOTIFICATION, msg));
        }

        
        ArrayList<String> invoices = orderRepo.getAutomaticInvoices();
        for (String msg : invoices) {
            System.out.println("[Scheduler] Sending Invoice: " + msg);
            server.sendToAllClients(new BistroMessage(ActionType.SERVER_NOTIFICATION, msg));
        }
    }
}