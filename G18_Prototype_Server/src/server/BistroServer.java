package server;

import common.*;
import common.Table;
import logic.ReservationLogic;
import logic.UserManagement;
import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.sql.Timestamp;
import gui.ServerPortFrameController;
import common.Role;

/**
 * The main Server class that handles all communication with the clients.
 *
 * Software Structure:
 * This class acts as the central hub of the Server Layer. It extends the OCSF AbstractServer.
 * It receives messages from clients (Client Layer), processes them using the Logic Layer
 * (UserManagement, ReservationLogic), and accesses data via the Database Layer (Repositories).
 * It also manages the background Scheduler for automated tasks.
 *
 * UI Components:
 * This class connects to the Server GUI to display logs, errors, and the list of connected clients.
 *
 * @author Dana Zablev
 * @version 1.0
 */
public class BistroServer extends AbstractServer {

    /** Repository for handling Order database operations. */
    private OrderRepository orderRepo;
    /** Repository for handling User database operations. */
    private UserRepository userRepo;
    /** Logic class for user-related business rules. */
    private UserManagement userLogic;
    /** Logic class for reservation algorithms. */
    private ReservationLogic reservationLogic;
    /** Background task runner for automated checks. */
    private final OrderScheduler scheduler;
    /** Repository for handling Table database operations. */
    private TableRepository tableRepo;
    /** Repository for handling Opening Hours database operations. */
    private OpeningHoursRepository hoursRepo;
    /** Interface for sending logs to the Server GUI. */
    private final ChatIF serverUI;

    /**
     * Constructor. Initializes all repositories, logic classes, and the scheduler.
     *
     * @param port The port number to listen on.
     * @param serverUI The GUI controller to display logs.
     */
    public BistroServer(int port, ChatIF serverUI) {
        super(port);
        this.serverUI = serverUI;
        this.orderRepo = new OrderRepository();
        this.userRepo = new UserRepository();
        this.userLogic = new UserManagement();
        this.reservationLogic = new ReservationLogic();
        this.tableRepo = new TableRepository();
        this.hoursRepo = new OpeningHoursRepository();

        // Start background tasks
        this.scheduler = new OrderScheduler(orderRepo, serverUI, this);
        this.scheduler.start();

        log("[Server] Order Scheduler initialized and started.");
    }

    /**
     * Helper method to display messages on the Server GUI.
     * @param s The message to display.
     */
    private void log(String s) {
        System.out.println(s);
        if (serverUI != null)
            serverUI.display(s);
    }

    /**
     * Called when the server successfully starts listening for connections.
     */
    @Override
    protected void serverStarted() {
    		userRepo.resetAllLoginStatus();
        log("[Server] Bistro Server Listening on port " + getPort());
    }

    /**
     * Called when the server stops listening.
     */
    @Override
    protected void serverStopped() {
        log("[Server] Server stopped.");
    }

    /**
     * Called when a new client connects to the server.
     * Updates the connected clients list in the UI.
     * @param client The connection object of the new client.
     */
    @Override
    protected void clientConnected(ConnectionToClient client) {
        String ip = client.getInetAddress().getHostAddress();
        String host = client.getInetAddress().getHostName();
        log("[Client Connected] IP: " + ip);
        updateClientListInUI(ip, host, "Connected");
    }

    /**
     * Called when a client disconnects.
     * Updates the connected clients list in the UI.
     * @param client The connection object of the disconnected client.
     */
    @Override
    synchronized protected void clientDisconnected(ConnectionToClient client) {
        String ip = "Unknown";
        String host = "Unknown";

        try {
            if (client.getInetAddress() != null) {
                ip = client.getInetAddress().getHostAddress();
                host = client.getInetAddress().getHostName();
            }
        } catch (Exception e) {
        }

        log("[Client Disconnected] IP: " + ip);

        Object userIdObj = client.getInfo("userId");
        if (userIdObj != null) {
            int userId = (int) userIdObj;
            userRepo.logoutUser(userId); 
            log("[Auto-Logout] User ID " + userId + " released.");
        }

        updateClientListInUI(ip, host, "Disconnected");
    }

    /**
     * Sends the client status update to the GUI controller.
     *
     * @param ip The client's IP.
     * @param host The client's Host name.
     * @param status The status (Connected/Disconnected).
     */
    private void updateClientListInUI(String ip, String host, String status) {
        if (serverUI instanceof ServerPortFrameController) {
            ServerPortFrameController screenController = (ServerPortFrameController) serverUI;
            screenController.updateClientList(ip, host, status);
        }
    }

    /**
     * The main method that handles all requests coming from clients.
     * It uses a Switch-Case structure to determine the ActionType and calls
     * the appropriate methods in the Repositories or Logic classes.
     *
     * @param msg The message object sent by the client.
     * @param client The connection to the client.
     */
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
                // User Management & Authentication
                
                case LOGIN:
                    // Authenticates a user with username and password
                    if (request.getData() instanceof User) {
                        User loginData = (User) request.getData();
                        User authenticatedUser = userRepo.login(loginData.getUsername(), loginData.getPassword());

                        if (authenticatedUser != null) {
                        	    client.setInfo("userId", authenticatedUser.getUserId());
                            responseMsg = new BistroMessage(ActionType.LOGIN, authenticatedUser);
                            log("[Login] Success: " + authenticatedUser.getUsername());
                        } else {
                            responseMsg = new BistroMessage(ActionType.LOGIN, "Username or Password incorrect.");
                            log("[Login] Failed attempt for: " + loginData.getUsername());
                        }
                    }
                    break;

                case REGISTER_CLEINT:
                    // Registers a new user and generates a member ID
                    if (request.getData() instanceof User) {
                        User newUser = (User) request.getData();
                        int memberCode = userRepo.registerUser(newUser);
                        if (memberCode != -1) {
                            newUser.setMemberCode(memberCode);
                            responseMsg = new BistroMessage(ActionType.REGISTER_CLEINT, newUser);
                        } else {
                            responseMsg = new BistroMessage(ActionType.REGISTER_CLEINT, "Registration failed.");
                        }
                    }
                    break;

                case UPDATE_USER_INFO:
                    // Updates phone or email for an existing user
                    if (request.getData() instanceof User) {
                        User userToUpdate = (User) request.getData();
                        boolean updated = userRepo.updateUserInfo(userToUpdate.getUserId(), userToUpdate.getPhone(), userToUpdate.getEmail());
                        if (updated) {
                            responseMsg = new BistroMessage(ActionType.UPDATE_USER_INFO, "Success");
                            log("[User Mgmt] Updated info for User ID: " + userToUpdate.getUserId());
                        } else {
                            responseMsg = new BistroMessage(ActionType.UPDATE_USER_INFO, "Failed");
                        }
                    }
                    break;

                case IDENTIFY_BY_QR:
                    // Finds a user by scanning their unique QR code string
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
                    // Returns the list of past orders for a specific member
                    if (request.getData() instanceof Integer) {
                        int subscriberId = (Integer) request.getData();
                        ArrayList<Order> history = orderRepo.getMemberHistory(subscriberId);
                        responseMsg = new BistroMessage(ActionType.GET_USER_HISTORY, history);
                    }
                    break;

                case GET_ALL_MEMBERS:
                    // Returns a list of all registered members (for Management screen)
                    ArrayList<User> allMembers = userRepo.getAllMembers();
                    log("[Management] Sending all user records.");
                    responseMsg = new BistroMessage(ActionType.GET_ALL_MEMBERS, allMembers);
                    break;

                //  Reservations & Order Handling
                
                case CREATE_ORDER:
                    // Logic to create a new reservation. Checks availability first.
                    if (request.getData() instanceof Order) {
                        Order newOrder = (Order) request.getData();
                        try {
                            List<Timestamp> alternatives = reservationLogic.checkAvailability(newOrder);

                            if (alternatives == null) {
                                // Table is available - Proceed to book
                                int code; 
                                do {
                                    code = (int) (Math.random() * 9000) + 1000;
                                } while (orderRepo.isCodeExists(code));
                                
                                newOrder.setConfirmationCode(code);
                                newOrder.setStatus("PENDING");

                                int orderId = orderRepo.createOrder(newOrder);

                                if (orderId != -1) {
                                    newOrder.setOrderNumber(orderId);
                                    responseMsg = new BistroMessage(ActionType.CREATE_ORDER, newOrder);
                                    log("[Order] Approved. ID: " + orderId + ", Code: " + code);
                                } else {
                                    log("[Error] Failed to save order to DB!");
                                    responseMsg = new BistroMessage(ActionType.CREATE_ORDER, "ERROR: Database Save Failed (Check Server Logs)");
                                }
                            } else {
                                // Table unavailable - Return alternatives
                                responseMsg = new BistroMessage(ActionType.ORDER_ALTERNATIVES, alternatives);
                                log("[Order] Time unavailable. Suggesting alternatives.");
                            }
                        } catch (IllegalArgumentException e) {
                            responseMsg = new BistroMessage(ActionType.CREATE_ORDER, "ERROR: " + e.getMessage());
                        }
                    }
                    break;

                case CANCEL_ORDER:
                    // Cancels a pending order
                    int codeToCheck = (int) request.getData();
                    boolean canceled = orderRepo.cancelOrderByCode(codeToCheck);
                    if (canceled) {
                        responseMsg = new BistroMessage(ActionType.CANCEL_ORDER, "Success");
                    } else {
                        responseMsg = new BistroMessage(ActionType.CANCEL_ORDER, "Code not found");
                    }
                    break;

                // Reception, Arrival & Seating

                case VALIDATE_ARRIVAL:
                    // Checks in a customer. If table available -> SEATED. Else -> WAITING.
                    if (request.getData() instanceof Integer) {
                        
                        // Check if the restaurant is currently OPEN before processing arrival
                        Timestamp currentTime = new Timestamp(System.currentTimeMillis());
                        if (!hoursRepo.isOpen(currentTime)) {
                            responseMsg = new BistroMessage(ActionType.VALIDATE_ARRIVAL, "Error: Restaurant is CLOSED at this time.");
                            break; 
                        }

                        int code = (Integer) request.getData();
                        Order order = orderRepo.getOrderByCode(code);

                        if (order != null) {
                            int assignedTable = orderRepo.assignFreeTable(order.getOrderNumber(), order.getNumberOfGuests());

                            if (assignedTable != -1) {
                                order.setAssignedTableId(assignedTable);
                                order.setStatus("SEATED");
                                responseMsg = new BistroMessage(ActionType.VALIDATE_ARRIVAL, order);
                                log("[Reception] Code " + code + " Seated at Table " + assignedTable);
                            } else {
                                orderRepo.updateOrderStatus(order.getOrderNumber(), "WAITING");
                                
                                responseMsg = new BistroMessage(ActionType.VALIDATE_ARRIVAL, "WAIT: No table ready yet. You are now in the waiting list.");
                                log("[Reception] Code " + code + " moved to WAITING list (Physical Arrival).");
                            }
                        } else {
                            responseMsg = new BistroMessage(ActionType.VALIDATE_ARRIVAL, "ERROR: Invalid Code");
                        }
                    }
                    break;

                //Waitlist Management (Walk-ins)

                case ENTER_WAITLIST:
                    // Adds a walk-in customer to the system (Seated or Waiting)
                    if (request.getData() instanceof Order) {
                        Order walkIn = (Order) request.getData();

                        boolean hasEmail = walkIn.getEmail() != null && !walkIn.getEmail().isEmpty();
                        boolean hasPhone = walkIn.getPhone() != null && !walkIn.getPhone().isEmpty();
                        
                        if (!hasEmail && !hasPhone) {
                            responseMsg = new BistroMessage(ActionType.ENTER_WAITLIST, "Error: Must provide Email or Phone");
                            break;
                        }

                        if (orderRepo.hasActiveOrder(walkIn.getPhone(), walkIn.getEmail())) {
                            responseMsg = new BistroMessage(ActionType.ENTER_WAITLIST, "Error: You are already in the system (Seated or Waiting).");
                            log("[Waitlist] Blocked duplicate entry for " + walkIn.getCustomerName());
                            break; 
                        }

                        if (walkIn.getOrderDate() == null) {
                            walkIn.setOrderDate(new java.sql.Timestamp(System.currentTimeMillis()));
                        }

                        //  Check opening hours before allowing entry
                        if (!hoursRepo.isOpen(walkIn.getOrderDate())) {
                             responseMsg = new BistroMessage(ActionType.ENTER_WAITLIST, "Error: Restaurant is CLOSED at this time.");
                             break;
                        }

                        int code;
                        do {
                            code = (int) (Math.random() * 9000) + 1000;
                        } while (orderRepo.isCodeExists(code));
                        
                        walkIn.setConfirmationCode(code); 

                        if (orderRepo.isTableAvailableNow(walkIn.getNumberOfGuests())) {
                            // Immediate Seating
                            walkIn.setStatus("SEATED"); 
                            int id = orderRepo.createOrder(walkIn);
                            
                            if (id != -1) {
                                walkIn.setOrderNumber(id);
                                int tableId = orderRepo.assignFreeTable(id, walkIn.getNumberOfGuests());
                                walkIn.setAssignedTableId(tableId);

                                log("[Waitlist] Walk-in Seated immediately at Table " + tableId);
                                responseMsg = new BistroMessage(ActionType.ENTER_WAITLIST, walkIn);
                            } else {
                                responseMsg = new BistroMessage(ActionType.ENTER_WAITLIST, "Error: DB Creation Failed");
                            }

                        } else {
                            // Add to Waitlist
                            walkIn.setStatus("WAITING");
                            int id = orderRepo.createOrder(walkIn);
                            
                            if (id != -1) {
                                walkIn.setOrderNumber(id);
                                log("[Waitlist] Added to queue. Code: " + code);
                                responseMsg = new BistroMessage(ActionType.ENTER_WAITLIST, walkIn);
                            } else {
                                responseMsg = new BistroMessage(ActionType.ENTER_WAITLIST, "Error: DB Creation Failed");
                            }
                        }
                    }
                    break;

                case LEAVE_WAITLIST:
                    // Removes a customer from the waiting list
                    if (request.getData() instanceof Integer) {
                        int confirmationCode = (Integer) request.getData();
                        boolean success = orderRepo.cancelOrderByCode(confirmationCode);
                        
                        if (success) {
                            responseMsg = new BistroMessage(ActionType.LEAVE_WAITLIST, "Success");
                            log("[Waitlist] Customer with code " + confirmationCode + " left the queue.");
                        } else {
                            responseMsg = new BistroMessage(ActionType.LEAVE_WAITLIST, "Error: Code not found");
                        }
                    }
                    break;

                case RESTORE_CODE:
                    // Recovers a lost confirmation code via contact info
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

                // Billing & Checkout

                case PAY_BILL:
                    // Handles payment, calculates final price, and releases the table
                    if (request.getData() instanceof Integer) {
                        int confirmationCode = (Integer) request.getData();
                        log("[Payment] Request received for Confirmation Code: " + confirmationCode);

                        Order dbOrder = orderRepo.getOrderByCode(confirmationCode);

                        if (dbOrder != null) {
                            if (dbOrder.getStatus().equals("COMPLETED")) {
                                responseMsg = new BistroMessage(ActionType.PAY_BILL, "ERROR: Order already paid");
                                break;
                            }
                            if (!dbOrder.getStatus().equals("SEATED") && !dbOrder.getStatus().equals("BILLED")) {
                                responseMsg = new BistroMessage(ActionType.PAY_BILL, "ERROR: You cannot pay before being seated!");
                                log("[Payment] Blocked payment for order in status: " + dbOrder.getStatus());
                                break;
                            }

                            User payer = null;
                            if (dbOrder.getMemberId() > 0) {
                                payer = new User();
                                payer.setRole(Role.MEMBER);
                                log("[Payment] Subscriber identified. Applying discount.");
                            }

                            double basePrice = dbOrder.getNumberOfGuests() * 100.0;
                            double finalPrice = userLogic.calculateFinalPrice(payer, basePrice);

                            int freedTableCapacity = 0;
                            if (dbOrder.getAssignedTableId() != null) {
                                freedTableCapacity = tableRepo.getTableCapacity(dbOrder.getAssignedTableId());
                            }

                            boolean paid = orderRepo.processPayment(dbOrder.getOrderNumber(), finalPrice);

                            if (paid) {
                                log("[Payment] Code " + confirmationCode + " Paid: " + finalPrice + "NIS.");
                                responseMsg = new BistroMessage(ActionType.PAY_BILL, "Success");

                                // Notify next in line if table was freed
                                if (freedTableCapacity > 0) {
                                    Order nextPerson = orderRepo.getNextInWaitlist(freedTableCapacity);
                                    if (nextPerson != null) {
                                        orderRepo.updateOrderStatus(nextPerson.getOrderNumber(), "NOTIFIED");
                                        orderRepo.updateOrderTime(nextPerson.getOrderNumber());

                                        String smsMessage = "Hi " + nextPerson.getPhone() + ", table for " +
                                                nextPerson.getNumberOfGuests() + " is ready! 15 mins to arrive.";

                                        BistroMessage notifyMsg = new BistroMessage(ActionType.SERVER_NOTIFICATION, smsMessage);
                                        sendToAllClients(notifyMsg);
                                    }
                                }
                            } else {
                                responseMsg = new BistroMessage(ActionType.PAY_BILL, "ERROR: DB Update Failed");
                            }
                        } else {
                            responseMsg = new BistroMessage(ActionType.PAY_BILL, "ERROR: Invalid Code");
                        }
                    }
                    break;

                // Management & Configuration

                case GET_ALL_ORDERS:
                    ArrayList<Order> allOrders = orderRepo.getAllOrders();
                    responseMsg = new BistroMessage(ActionType.GET_ALL_ORDERS, allOrders);
                    break;
                    
                case GET_WAITING_LIST:
                    ArrayList<Order> waitlist = orderRepo.getLiveWaitingList();
                    responseMsg = new BistroMessage(ActionType.GET_WAITING_LIST, waitlist);
                    break;
                    
                case GET_ACTIVE_DINERS:
                    ArrayList<Order> activeDiners = orderRepo.getActiveDiners();
                    responseMsg = new BistroMessage(ActionType.GET_ACTIVE_DINERS, activeDiners);
                    break;
                
                case GET_ALL_ACTIVE_ORDERS:
                    ArrayList<Order> allActiveOrders = orderRepo.getAllActiveOrdersForToday();
                    responseMsg = new BistroMessage(ActionType.GET_ALL_ACTIVE_ORDERS, allActiveOrders);
                    break;

                case GET_AVAILABLE_TIMES:
                    // Calculates available time slots for a given date and group size
                    if (request.getData() instanceof ArrayList) {
                        ArrayList<?> paramsList = (ArrayList<?>) request.getData();
                        
                        Object dateObj = paramsList.get(0);
                        java.sql.Date reqDate;
                        if (dateObj instanceof java.sql.Date) {
                            reqDate = (java.sql.Date) dateObj;
                        } else {
                             java.util.Date utilDate = (java.util.Date) dateObj;
                             reqDate = new java.sql.Date(utilDate.getTime());
                        }
                        
                        int reqGuests = (int) paramsList.get(1);
                        System.out.println("DEBUG: Calculating available slots for " + reqDate + ", Guests: " + reqGuests);

                        List<String> availableTimes = reservationLogic.getAvailableSlotsForDate(reqDate, reqGuests);
                        responseMsg = new BistroMessage(ActionType.GET_AVAILABLE_TIMES, availableTimes);
                    } else {
                        responseMsg = new BistroMessage(ActionType.GET_AVAILABLE_TIMES, null);
                    }
                    break;

                case ADD_TABLE:
                    if (request.getData() instanceof Table) {
                        Table newTable = (Table) request.getData();
                        boolean success = tableRepo.addTable(newTable);
                        if (success) {
                            log("[Management] Added Table " + newTable.getTableId());
                            responseMsg = new BistroMessage(ActionType.ADD_TABLE, "Success");
                        } else {
                            responseMsg = new BistroMessage(ActionType.ADD_TABLE, "Failed: ID might exist");
                        }
                    }
                    break;

                case REMOVE_TABLE:
                    if (request.getData() instanceof Integer) {
                        int tableIdToRemove = (Integer) request.getData();
                        
                        //  Delete table and get list of cancelled orders
                        ArrayList<Order> cancelledList = tableRepo.deleteTableSafely(tableIdToRemove);

                        if (cancelledList != null) {
                            //  Prepare the detailed report for the client (Manager)
                            StringBuilder clientResponse = new StringBuilder();
                            clientResponse.append("Success. Table ").append(tableIdToRemove).append(" removed.\n");

                            if (cancelledList.isEmpty()) {
                                clientResponse.append("No future orders were affected.");
                                log("[Management] Removed Table " + tableIdToRemove + ". No conflicts.");
                            } else {
                                clientResponse.append("WARNING: ").append(cancelledList.size()).append(" orders were cancelled!\n\n");
                                clientResponse.append("--- Notifications Sent ---\n");
                                
                                log("[System] Table removed. Found " + cancelledList.size() + " overbooked orders.");

                                for (Order o : cancelledList) {
                                    // Create the message string
                                    String logMsg = "SMS sent to: " + o.getEmail() + 
                                                  " (Customer: " + o.getCustomerName() + 
                                                  ", Order #" + o.getOrderNumber() + ")";

                                    // Log to Server GUI/Console
                                    log("[SIMULATION] " + logMsg);

                                    // Append to the response sent to the Manager
                                    clientResponse.append("- ").append(logMsg).append("\n");
                                }
                            }
                            
                            //  Send the full report string back to the client
                            responseMsg = new BistroMessage(ActionType.REMOVE_TABLE, clientResponse.toString());
                            
                        } else {
                            responseMsg = new BistroMessage(ActionType.REMOVE_TABLE, "Failed: Table ID not found");
                        }
                    }
                    break;
                case UPDATE_TABLE:
                    if (request.getData() instanceof Table) {
                        Table tableToUpdate = (Table) request.getData();
                        boolean success = tableRepo.updateTable(tableToUpdate);
                        if (success) {
                            log("[Management] Updated Table #" + tableToUpdate.getTableId());
                            responseMsg = new BistroMessage(ActionType.UPDATE_TABLE, "Success");
                        } else {
                            responseMsg = new BistroMessage(ActionType.UPDATE_TABLE, "Failed");
                        }
                    }
                    break;

                case GET_OPENING_HOURS:
                    ArrayList<OpeningHour> hoursList = hoursRepo.getAllOpeningHours();
                    responseMsg = new BistroMessage(ActionType.GET_OPENING_HOURS, hoursList);
                    break;

                case UPDATE_OPENING_HOURS:
                    // Updates restaurant schedule and handles conflicts
                    if (request.getData() instanceof OpeningHour) {
                        OpeningHour hourToUpdate = (OpeningHour) request.getData();
                        boolean success = hoursRepo.updateOpeningHour(hourToUpdate);

                        if (success) {
                            // 1. Check for conflicts
                            ArrayList<Order> cancelledOrders = orderRepo.cancelConflictingOrders(hourToUpdate);
                            
                            // 2. Build the detailed report
                            StringBuilder clientResponse = new StringBuilder();
                            clientResponse.append("Hours updated successfully.\n");

                            if (cancelledOrders.isEmpty()) {
                                clientResponse.append("No existing orders were affected.");
                                log("[Server] Hours updated. No conflicts found.");
                            } else {
                                clientResponse.append("WARNING: ").append(cancelledOrders.size()).append(" orders cancelled due to new hours!\n\n");
                                clientResponse.append("--- Notifications Sent ---\n");
                                
                                log("[Notification System] Starting conflict resolution...");
                                
                                for (Order o : cancelledOrders) {
                                    // Create the message string
                                    String logMsg = "SMS sent to: " + o.getEmail() + 
                                                  " (Customer: " + o.getCustomerName() + 
                                                  ", Order #" + o.getOrderNumber() + ")";
                                    
                                    // Log to Server GUI
                                    log("[SIMULATION] " + logMsg);
                                    
                                    // Append to Client Report
                                    clientResponse.append("- ").append(logMsg).append("\n");
                                }
                            }
                            
                            // 3. Send the full report back to the client
                            responseMsg = new BistroMessage(ActionType.UPDATE_OPENING_HOURS, clientResponse.toString());
                            
                        } else {
                            log("[Error] Failed to update opening hours.");
                            responseMsg = new BistroMessage(ActionType.UPDATE_OPENING_HOURS, "Error: Update failed");
                        }
                    }
                    break;

                case GET_ORDER_BY_CODE:
                    if (request.getData() instanceof Integer) {
                        int code = (Integer) request.getData();
                        common.Order foundOrder = orderRepo.getOrderByCode(code);

                        if (foundOrder != null) {
                            int guests = foundOrder.getNumberOfGuests();
                            User payer = null;
                            if (foundOrder.getMemberId() > 0) {
                                payer = new User(0, "D", "D", "0", "e", Role.MEMBER, "0", "0", 0);
                            }
                            double basePrice = guests * 100.0;
                            double finalPrice = userLogic.calculateFinalPrice(payer, basePrice);
                            foundOrder.setTotalPrice(finalPrice);
                            responseMsg = new BistroMessage(ActionType.GET_ORDER_BY_CODE, foundOrder);
                        } else {
                            responseMsg = new BistroMessage(ActionType.GET_ORDER_BY_CODE, null);
                        }
                    }
                    break;
                    
                case GET_ALL_TABLES:
                    ArrayList<Table> allTables = tableRepo.getAllTables();
                    log("[Management] Fetched " + allTables.size() + " tables from DB.");
                    responseMsg = new BistroMessage(ActionType.GET_ALL_TABLES, allTables);
                    break;

                // Report Generation

                case GET_PERFORMANCE_REPORT:
                    int[] perfParams = (int[]) request.getData();
                    int perfMonth = perfParams[0];
                    int perfYear = perfParams[1];

                    Map<String, Integer> rawPerfData = orderRepo.getPerformanceReportData(perfMonth, perfYear);
                    common.Report perfReport = new common.Report(perfMonth, perfYear, "PERFORMANCE");
                    
                    if (rawPerfData != null) {
                        for (Map.Entry<String, Integer> entry : rawPerfData.entrySet()) {
                            perfReport.addData(entry.getKey(), entry.getValue());
                        }
                    }
                    responseMsg = new BistroMessage(ActionType.GET_PERFORMANCE_REPORT, perfReport);
                    log("[Reports] Generated Performance Report for " + perfMonth + "/" + perfYear);
                    break;

                case GET_SUBSCRIPTION_REPORT:
                    int[] subParams = (int[]) request.getData();
                    int subMonth = subParams[0];
                    int subYear = subParams[1];

                    Map<String, Integer> rawSubData = orderRepo.getSubscriptionReportData(subMonth, subYear);
                    common.Report subReport = new common.Report(subMonth, subYear, "SUBSCRIPTION");
                    
                    if (rawSubData != null) {
                        for (Map.Entry<String, Integer> entry : rawSubData.entrySet()) {
                            subReport.addData(entry.getKey(), entry.getValue());
                        }
                    }
                    responseMsg = new BistroMessage(ActionType.GET_SUBSCRIPTION_REPORT, subReport);
                    log("[Reports] Generated Subscription Report for " + subMonth + "/" + subYear);
                    break;
                    
                case LOGOUT:
                    // Handles the logout request to allow switching users.
                    if (request.getData() instanceof Integer) {
                        int userId = (Integer) request.getData();
                        
                        //  Update database status to Offline
                        userRepo.logoutUser(userId); 
                        
                        //  Log the event on the server console
                        log(" User ID " + userId + " logged out.");
                        
                        //  Send success message back to client so it can switch scenes
                        responseMsg = new BistroMessage(ActionType.LOGOUT, "Success");
                    }
                    break;

                case CLIENT_QUIT:
                    log("[Server] Client disconnected.");
                    String ip = client.getInetAddress().getHostAddress();
                    String host = client.getInetAddress().getHostName();
                    updateClientListInUI(ip, host, "Disconnected");
                    try {
                        client.close(); 
                    } catch (IOException e) {}
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