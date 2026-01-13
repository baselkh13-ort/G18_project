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
 * It manages the "Clock" requirements:
 * 1. Sending reminders 2 hours before an order.
 * 2. Auto-canceling orders after 15 minutes of delay.
 * 3. Sending invoices 2 hours after the customer is seated.
 */
public class OrderScheduler {

    private final OrderRepository orderRepo;
    private final Timer timer;
    private final ChatIF ui;
    private final AbstractServer server;

    /**
     * Initializes the scheduler with the order repository.
     * @param orderRepo The repository used to perform database updates.
     */
    public OrderScheduler(OrderRepository orderRepo, ChatIF ui, AbstractServer server) {
        this.orderRepo = orderRepo;
        this.ui = ui;
        this.server = server; 
        this.timer = new Timer(true);
    }

    /**
     * Starts the background task to run every 60 seconds.
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