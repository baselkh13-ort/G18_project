package server;

import java.util.Timer;
import java.util.TimerTask;

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

    /**
     * Initializes the scheduler with the order repository.
     * @param orderRepo The repository used to perform database updates.
     */
    public OrderScheduler(OrderRepository orderRepo) {
        this.orderRepo = orderRepo;
        this.timer = new Timer(true); // true means it runs as a daemon thread
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
        }, 5000, 60000); // Wait 5 seconds to start, then run every 60 seconds
    }

    /**
     * Executes all time-based restaurant policies.
     */
    private void processAutomatedTasks() {
        System.out.println("[Scheduler] Running automated time checks...");

        // Task 1: Auto-cancel orders if the customer is 15 minutes late
        int canceledCount = orderRepo.cancelLateOrders(15);
        if (canceledCount > 0) {
            System.out.println("[Scheduler] " + canceledCount + " late orders were automatically canceled.");
        }

        // Task 2: Simulation of sending reminders (2 hours before)
        // This will print to the console which customers should receive a notification
        orderRepo.processReminders();

        // Task 3: Simulation of sending invoices (2 hours after seating)
        orderRepo.processAutomaticInvoices();
    }
}