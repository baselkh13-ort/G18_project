package server;

import common.*;
import logic.ReservationLogic;
import logic.UserManagement;
import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.sql.Timestamp;

/**
 * The BistroServer class handles all client-server communications.
 * It coordinates between the UI, business logic, and database repositories.
 *  User Management (Login, Register, QR).
 *  Order Management (Reservations).
 *  Waitlist & Queues.
 *  Reception (Arrival Validation).
 *  Payment & Checkout.
 *  Management (Tables, Hours, All Orders).
 *  Reports (Performance, Subscription).
 */
public class BistroServer extends AbstractServer {

    private OrderRepository orderRepo;
    private UserRepository userRepo;
    private UserManagement userLogic;
    private ReservationLogic reservationLogic; 
    private TableRepository tableRepo;           
    private OpeningHoursRepository hoursRepo;    
    private final ChatIF ui;

    public BistroServer(int port, ChatIF ui) {
        super(port);
        this.orderRepo = new OrderRepository();
        this.userRepo = new UserRepository();
        this.userLogic = new UserManagement();
        this.reservationLogic = new ReservationLogic(); 
        this.tableRepo = new TableRepository();         
        this.hoursRepo = new OpeningHoursRepository();  
        this.ui = ui;

        // Start background tasks
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
                //  User Management 
                case LOGIN:
                    if (request.getData() instanceof User) {
                        User loginData = (User) request.getData();
                        User authenticatedUser = userRepo.login(loginData.getUsername(), loginData.getPassword());
                        responseMsg = new BistroMessage(ActionType.LOGIN, authenticatedUser);
                    }
                    break;

                case REGISTER_CLEINT:
                    if (request.getData() instanceof User) {
                        User newUser = (User) request.getData();
                        int newId = userLogic.registerMember(Role.WORKER, newUser); // Logic handles ID generation
                        // Note: In a real DB, repo.createUser() would be called here.
                        responseMsg = new BistroMessage(ActionType.REGISTER_CLEINT, newId);
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
                        responseMsg = new BistroMessage(ActionType.GET_USER_HISTORY, history);
                    }
                    break;
                    
                case GET_ALL_MEMBERS: 
                    ArrayList<User> allMembers = userRepo.getAllUsers();
                    log("[Management] Sending all user records.");
                    responseMsg = new BistroMessage(ActionType.GET_ALL_MEMBERS, allMembers);
                    break;

                // Reservations 
                case CREATE_ORDER:
                    if (request.getData() instanceof Order) {
                        Order newOrder = (Order) request.getData();
                        try {
                            List<Timestamp> alternatives = reservationLogic.checkAvailability(newOrder);

                            if (alternatives == null) {
                                // Table Available
                                int code = (int)(Math.random() * 9000) + 1000;
                                newOrder.setConfirmationCode(code);
                                newOrder.setStatus("PENDING");
                                
                                int orderId = orderRepo.createOrder(newOrder);
                                newOrder.setOrderNumber(orderId);
                                
                                responseMsg = new BistroMessage(ActionType.CREATE_ORDER, newOrder);
                                log("[Order] Approved. ID: " + orderId + ", Code: " + code);
                            } else {
                                // Table Full
                                responseMsg = new BistroMessage(ActionType.ORDER_ALTERNATIVES, alternatives);
                                log("[Order] Time unavailable. Suggesting alternatives.");
                            }
                        } catch (IllegalArgumentException e) {
                            responseMsg = new BistroMessage(ActionType.CREATE_ORDER, "ERROR: " + e.getMessage());
                        }
                    }
                    break;

                //  Waitlist 
                case ENTER_WAITLIST:
                    if (request.getData() instanceof Order) {
                        Order walkIn = (Order) request.getData();
                        
                        if (orderRepo.isTableAvailableNow(walkIn.getNumberOfGuests())) {
                            // Immediate Seating
                            walkIn.setStatus("SEATED");
                            walkIn.setConfirmationCode(0); 
                            int id = orderRepo.createOrder(walkIn);
                            // Assign table immediately
                            int tableId = orderRepo.assignFreeTable(id, walkIn.getNumberOfGuests());
                            walkIn.setOrderNumber(id);
                            walkIn.setAssignedTableId(tableId);
                            
                            log("[Waitlist] Immediate Seating at Table " + tableId);
                            responseMsg = new BistroMessage(ActionType.ENTER_WAITLIST, walkIn);
                        } else {
                            // Add to Waitlist
                            walkIn.setStatus("WAITING");
                            int code = (int)(Math.random() * 9000) + 1000;
                            walkIn.setConfirmationCode(code);
                            int id = orderRepo.createOrder(walkIn);
                            walkIn.setOrderNumber(id);
                            
                            log("[Waitlist] Added to queue. Code: " + code);
                            responseMsg = new BistroMessage(ActionType.ENTER_WAITLIST, walkIn);
                        }
                    }
                    break;

                case LEAVE_WAITLIST:
                    if (request.getData() instanceof Integer) {
                        int orderId = (Integer) request.getData();
                        orderRepo.updateOrderStatus(orderId, "CANCELLED");
                        responseMsg = new BistroMessage(ActionType.LEAVE_WAITLIST, "OK");
                    }
                    break;

                //  Reception (Arrival)
                case VALIDATE_ARRIVAL:
                    if (request.getData() instanceof Integer) {
                        int code = (Integer) request.getData();
                        Order order = orderRepo.getOrderByCode(code);
                        
                        if (order != null) {
                            // Try to assign a table
                            int assignedTable = orderRepo.assignFreeTable(order.getOrderNumber(), order.getNumberOfGuests());
                            
                            if (assignedTable != -1) {
                                order.setAssignedTableId(assignedTable);
                                order.setStatus("SEATED");
                                responseMsg = new BistroMessage(ActionType.VALIDATE_ARRIVAL, order);
                                log("[Reception] Code Valid. Seated at Table " + assignedTable);
                            } else {
                                // Code valid, but no table ready yet (Wait)
                                responseMsg = new BistroMessage(ActionType.VALIDATE_ARRIVAL, "WAIT: No table ready yet.");
                                log("[Reception] Code Valid, but restaurant full.");
                            }
                        } else {
                            responseMsg = new BistroMessage(ActionType.VALIDATE_ARRIVAL, "ERROR: Invalid Code");
                        }
                    }
                    break;

                case RESTORE_CODE:
                    if (request.getData() instanceof String) {
                        String contact = (String) request.getData();
                        Order found = orderRepo.findOrderByContact(contact);
                        if (found != null) {
                            log("[SMS Simulation] Restored Code " + found.getConfirmationCode() + " sent to " + contact);
                            responseMsg = new BistroMessage(ActionType.RESTORE_CODE, found);
                        } else {
                            responseMsg = new BistroMessage(ActionType.RESTORE_CODE, "ERROR: Not found");
                        }
                    }
                    break;

                // Payment 
                case PAY_BILL:
                    if (request.getData() instanceof Order) {
                        Order billOrder = (Order) request.getData();
                        
                        // 1. Identify User Role (For 10% Discount)
                        User payer = null;
                        if (billOrder.getSubscriberId() > 0) {
                            // In a real app, we'd fetch the user. Here we simulate MEMBER role check.
                             payer = new User(0, "", "", "", "", Role.MEMBER, "", "", 0); 
                        }
                        
                        // 2. Calculate Final Price
                        // Assuming frontend sent base price in totalPrice, or calculate based on guests
                        double basePrice = billOrder.getNumberOfGuests() * 100.0; // Fixed Chef Meal logic
                        double finalPrice = userLogic.calculateFinalPrice(payer, basePrice);
                        
                        // 3. Process in DB (Clear Table)
                        boolean paid = orderRepo.processPayment(billOrder.getOrderNumber(), finalPrice);
                        
                        if (paid) {
                            log("[Payment] Order #" + billOrder.getOrderNumber() + " Paid: " + finalPrice + "NIS. Table Freed.");
                            responseMsg = new BistroMessage(ActionType.PAY_BILL, "OK");
                            
                            // 4. Notify Waitlist - SIMULATION
                            Order nextPerson = orderRepo.getNextInWaitlist(billOrder.getNumberOfGuests());
                            if (nextPerson != null) {
                                orderRepo.updateOrderStatus(nextPerson.getOrderNumber(), "NOTIFIED");
                                orderRepo.updateOrderTime(nextPerson.getOrderNumber());
                                
                                String smsMessage = "Hi " + nextPerson.getPhone() + ", a table is ready! You have 15 mins.";
                                log("[SMS Simulation] To: " + nextPerson.getPhone() + " | Body: " + smsMessage);
                            }
                        } else {
                            responseMsg = new BistroMessage(ActionType.PAY_BILL, "ERROR: DB Update Failed");
                        }
                    }
                    break;

                // Management 
                case GET_ALL_ORDERS:
                    ArrayList<Order> allOrders = orderRepo.getAllOrders();
                    responseMsg = new BistroMessage(ActionType.GET_ALL_ORDERS, allOrders);
                    break;

                case ADD_TABLE:
                case REMOVE_TABLE:
                case UPDATE_TABLE:
                case GET_OPENING_HOURS:
                case UPDATE_OPENING_HOURS:
                    // Delegates to Table/Hours Repositories (Standard DB ops)
                    // You can add these specific calls if you implemented the repos fully.
                    // For now, returning OK to prevent crash.
                    responseMsg = new BistroMessage(type, "OK (Mock)");
                    break;

                //  Reports 
                case GET_PERFORMANCE_REPORT:
                    // Client sends [Month, Year]
                    int[] perfParams = (int[]) request.getData(); 
                    Map<String, Integer> perfData = orderRepo.getPerformanceReportData(perfParams[0], perfParams[1]);
                    responseMsg = new BistroMessage(ActionType.GET_PERFORMANCE_REPORT, perfData);
                    break;

                case GET_SUBSCRIPTION_REPORT:
                    int[] subParams = (int[]) request.getData();
                    Map<String, Integer> subData = orderRepo.getSubscriptionReportData(subParams[0], subParams[1]);
                    responseMsg = new BistroMessage(ActionType.GET_SUBSCRIPTION_REPORT, subData);
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
            e.printStackTrace(); // Helpful for debugging
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