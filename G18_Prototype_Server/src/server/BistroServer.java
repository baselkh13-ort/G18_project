package server;

import common.*;
import logic.ReservationLogic;
import logic.UserManagement;
import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.sql.Timestamp;

/**
 * The BistroServer class handles all client-server communications.
 * It coordinates between the UI, business logic, and database repositories.
 * * Supported Features:
 * 1. User Management: Registration, Login, QR Identification (Task 1).
 * 2. Order Management: Reservations, Availability Checks, Waiting List (Task 2).
 * 3. Automated Tasks: Background scheduler for reminders and cancellations.
 */
public class BistroServer extends AbstractServer {

    private OrderRepository orderRepo;
    private UserRepository userRepo;
    private UserManagement userLogic;
    private ReservationLogic reservationLogic; // New for Task 2
    private final ChatIF ui;

    /**
     * Constructs the server, initializes repositories/logic, and starts the scheduler.
     * @param port The port number to listen on.
     * @param ui   The interface for logging server events.
     */
    public BistroServer(int port, ChatIF ui) {
        super(port);
        this.orderRepo = new OrderRepository();
        this.userRepo = new UserRepository();
        this.userLogic = new UserManagement();
        this.reservationLogic = new ReservationLogic(); // Init Task 2 Logic
        this.ui = ui;

        // Start the background clock for automated tasks (Reminders/Cancellations)
        OrderScheduler scheduler = new OrderScheduler(orderRepo);
        scheduler.start();
        
        log("[Server] Order Scheduler initialized and started.");
    }

    private void log(String s) {
        System.out.println(s);
        if (ui != null) ui.display(s);
    }

    @Override
    protected void serverStarted() {
        log("[Server] Bistro Server Listening on port " + getPort());
    }

    @Override
    protected void serverStopped() {
        log("[Server] Server stopped.");
    }

    @Override
    protected void clientConnected(ConnectionToClient client) {
        log("[Client Connected] IP: " + client.getInetAddress().getHostAddress());
    }

    @Override
    protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
        log("-------------------------------------------");
        
        if (!(msg instanceof BistroMessage)) {
            log("[Error] Invalid message type received.");
            return;
        }

        BistroMessage request = (BistroMessage) msg;
        ActionType type = request.getType();
        BistroMessage responseMsg = null;

        log("[Request] Processing Action: " + type);

        try {
            switch (type) {
         
                case LOGIN:
                    if (request.getData() instanceof User) {
                        User loginData = (User) request.getData();
                        User authenticatedUser = userRepo.login(loginData.getUsername(), loginData.getPassword());
                        responseMsg = new BistroMessage(ActionType.LOGIN, authenticatedUser);
                    }
                    break;

                case REGISTER_USER:
                    if (request.getData() instanceof User) {
                        User newUser = (User) request.getData();
                        // Assuming the requester has WORKER privileges for this operation
                        int newId = userLogic.registerMember(Role.WORKER, newUser);
                        responseMsg = new BistroMessage(ActionType.REGISTER_USER, newId);
                        log("[Register] New member registered with ID: " + newId);
                    }
                    break;

                case IDENTIFY_BY_QR:
                    if (request.getData() instanceof String) {
                        String qrCode = (String) request.getData();
                        User identifiedUser = userRepo.getUserByQRCode(qrCode);
                        
                        if (identifiedUser != null) {
                            log("[Identify] Success: " + identifiedUser.getFirstName() + " identified.");
                            responseMsg = new BistroMessage(ActionType.IDENTIFY_BY_QR, identifiedUser);
                        } else {
                            log("[Identify] Failed: QR code not found.");
                            responseMsg = new BistroMessage(ActionType.IDENTIFY_BY_QR, "ERROR: Invalid QR Code");
                        }
                    }
                    break;

                case GET_USER_HISTORY:
                    if (request.getData() instanceof Integer) {
                        int subscriberId = (Integer) request.getData();
                        ArrayList<Order> history = orderRepo.getMemberHistory(subscriberId);
                        log("[History] Sending " + history.size() + " records to subscriber " + subscriberId);
                        responseMsg = new BistroMessage(ActionType.GET_USER_HISTORY, history);
                    }
                    break;

                case UPDATE_USER_INFO:
                    if (request.getData() instanceof User) {
                        User u = (User) request.getData();
                        userLogic.updateContact(u.getUserId(), u.getPhone(), u.getEmail());
                        responseMsg = new BistroMessage(ActionType.UPDATE_USER_INFO, "OK");
                    }
                    break;

      
                case CREATE_ORDER:
                    if (request.getData() instanceof Order) {
                        Order newOrder = (Order) request.getData();
                        try {
                            // 1. Check availability logic (Task 2 requirement)
                            List<Timestamp> alternatives = reservationLogic.checkAvailability(newOrder);

                            if (alternatives == null) {
                                // Table is available -> Create Order in DB
                                int code = (int)(Math.random() * 9000) + 1000;
                                newOrder.setConfirmationCode(code);
                                newOrder.setStatus("PENDING"); // Default status
                                
                                int orderId = orderRepo.createOrder(newOrder); // INSERT to DB
                                newOrder.setOrderNumber(orderId);
                                
                                responseMsg = new BistroMessage(ActionType.CREATE_ORDER, newOrder);
                                log("[Order] Approved. ID: " + orderId + ", Code: " + code);
                            } else {
                                // Table is full -> Return alternatives
                                responseMsg = new BistroMessage(ActionType.ORDER_ALTERNATIVES, alternatives);
                                log("[Order] Requested time full. Suggesting alternatives.");
                            }
                        } catch (IllegalArgumentException e) {
                            responseMsg = new BistroMessage(ActionType.CREATE_ORDER, "ERROR: " + e.getMessage());
                        }
                    }
                    break;

                case ENTER_WAITLIST:
                    if (request.getData() instanceof Order) {
                        Order walkIn = (Order) request.getData();
                        
                        // Check availability NOW (Real-time)
                        if (orderRepo.isTableAvailableNow(walkIn.getNumberOfGuests())) {
                            // Immediate Seating
                            walkIn.setStatus("SEATED");
                            walkIn.setConfirmationCode(0); 
                            int id = orderRepo.createOrder(walkIn);
                            walkIn.setOrderNumber(id);
                            
                            log("[Waitlist] Table available immediately. Seating Customer ID: " + id);
                            responseMsg = new BistroMessage(ActionType.ENTER_WAITLIST, walkIn);
                        } else {
                            // No space -> Add to Waitlist
                            walkIn.setStatus("WAITING");
                            int code = (int)(Math.random() * 9000) + 1000;
                            walkIn.setConfirmationCode(code);
                            int id = orderRepo.createOrder(walkIn);
                            walkIn.setOrderNumber(id);
                            
                            log("[Waitlist] Restaurant full. Customer added to waitlist. Code: " + code);
                            responseMsg = new BistroMessage(ActionType.ENTER_WAITLIST, walkIn);
                        }
                    }
                    break;

                case LEAVE_WAITLIST:
                    // Requirement: Customer can leave waitlist at any time.
                    if (request.getData() instanceof Integer) {
                        int orderId = (Integer) request.getData();
                        orderRepo.updateOrderStatus(orderId, "CANCELLED");
                        responseMsg = new BistroMessage(ActionType.LEAVE_WAITLIST, "OK");
                        log("[Waitlist] Order " + orderId + " removed from waitlist.");
                    }
                    break;

                case UPDATE_ORDER_STATUS:
                    // Handles manual updates (e.g., Staff marks table as SEATED or COMPLETED)
                    if (request.getData() instanceof Order) {
                        Order o = (Order) request.getData();
                        boolean success = orderRepo.updateOrderStatus(o.getOrderNumber(), o.getStatus());
                        
                        // If a table becomes free, notify the next person in line
                        if ("COMPLETED".equals(o.getStatus())) {
                            Order nextPerson = orderRepo.getNextInWaitlist(o.getNumberOfGuests());
                            if (nextPerson != null) {
                                log("[Waitlist] Table freed. Notifying Order #" + nextPerson.getOrderNumber());
                                // Change status to NOTIFIED and reset timer for 15-min countdown
                                orderRepo.updateOrderStatus(nextPerson.getOrderNumber(), "NOTIFIED");
                                orderRepo.updateOrderTime(nextPerson.getOrderNumber());
                            }
                        }
                        
                        responseMsg = new BistroMessage(ActionType.UPDATE_ORDER_STATUS, success ? "OK" : "ERROR");
                    }
                    break;

                case CLIENT_QUIT:
                    log("[Server] Client disconnected.");
                    client.close();
                    return;

                default:
                    log("[Error] Unsupported ActionType: " + type);
                    break;
            }
        } catch (Exception e) {
            log("[Exception] " + type + " failed: " + e.getMessage());
            responseMsg = new BistroMessage(type, "ERROR: " + e.getMessage());
        }

        if (responseMsg != null) {
            try {
                client.sendToClient(responseMsg);
            } catch (IOException e) {
                log("[Error] Could not send response: " + e.getMessage());
            }
        }
        log("-------------------------------------------");
    }
}