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
 * The BistroServer class handles all client-server communications.
 */
public class BistroServer extends AbstractServer {

    private OrderRepository orderRepo;
    private UserRepository userRepo;
    private UserManagement userLogic;
    private ReservationLogic reservationLogic;
    private final OrderScheduler scheduler;
    private TableRepository tableRepo;
    private OpeningHoursRepository hoursRepo;
    private final ChatIF serverUI;

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

    private void log(String s) {
        System.out.println(s);
        if (serverUI != null)
            serverUI.display(s);
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
        String ip = client.getInetAddress().getHostAddress();
        String host = client.getInetAddress().getHostName();
        log("[Client Connected] IP: " + ip);
        updateClientListInUI(ip, host, "Connected");
    }

    @Override
    synchronized protected void clientDisconnected(ConnectionToClient client) {
        String ip = client.getInetAddress().getHostAddress();
        String host = client.getInetAddress().getHostName();
        log("[Client Disconnected] IP: " + ip);
        updateClientListInUI(ip, host, "Disconnected");
    }

    private void updateClientListInUI(String ip, String host, String status) {
        if (serverUI instanceof ServerPortFrameController) {
            ServerPortFrameController screenController = (ServerPortFrameController) serverUI;
            screenController.updateClientList(ip, host, status);
        }
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
                // User Management
                case LOGIN:
                    if (request.getData() instanceof User) {
                        User loginData = (User) request.getData();
                        User authenticatedUser = userRepo.login(loginData.getUsername(), loginData.getPassword());

                        if (authenticatedUser != null) {
                            responseMsg = new BistroMessage(ActionType.LOGIN, authenticatedUser);
                            log("[Login] Success: " + authenticatedUser.getUsername());
                        } else {
                            responseMsg = new BistroMessage(ActionType.LOGIN, "Username or Password incorrect.");
                            log("[Login] Failed attempt for: " + loginData.getUsername());
                        }
                    }
                    break;

                case REGISTER_CLEINT:
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
                                responseMsg = new BistroMessage(ActionType.ORDER_ALTERNATIVES, alternatives);
                                log("[Order] Time unavailable. Suggesting alternatives.");
                            }
                        } catch (IllegalArgumentException e) {
                            responseMsg = new BistroMessage(ActionType.CREATE_ORDER, "ERROR: " + e.getMessage());
                        }
                    }
                    break;

                case CANCEL_ORDER:
                    int codeToCheck = (int) request.getData();
                    boolean canceled = orderRepo.cancelOrderByCode(codeToCheck);
                    if (canceled) {
                        responseMsg = new BistroMessage(ActionType.CANCEL_ORDER, "Success");
                    } else {
                        responseMsg = new BistroMessage(ActionType.CANCEL_ORDER, "Code not found");
                    }
                    break;

                // Reception (Arrival)
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

                // Waitlist
                case ENTER_WAITLIST:
                    if (request.getData() instanceof Order) {
                        Order walkIn = (Order) request.getData();

                        boolean hasEmail = walkIn.getEmail() != null && !walkIn.getEmail().isEmpty();
                        boolean hasPhone = walkIn.getPhone() != null && !walkIn.getPhone().isEmpty();
                        if (!hasEmail && !hasPhone) {
                            responseMsg = new BistroMessage(ActionType.ENTER_WAITLIST, "Error: Must provide Email or Phone");
                            break;
                        }
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
                            int code = (int) (Math.random() * 9000) + 1000;
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
                    if (request.getData() instanceof Integer) {
                        int confirmationCode = (Integer) request.getData();
                        log("[Payment] Request received for Confirmation Code: " + confirmationCode);

                        Order dbOrder = orderRepo.getOrderByCode(confirmationCode);

                        if (dbOrder != null) {

                            if (dbOrder.getStatus().equals("COMPLETED")) {
                                responseMsg = new BistroMessage(ActionType.PAY_BILL, "ERROR: Order already paid");
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

                // Management
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
                    // Here we handle the list parameter correctly!
                    if (request.getData() instanceof ArrayList) {
                        ArrayList<?> paramsList = (ArrayList<?>) request.getData();
                        
                        // Handle potential Date conversion issues safely
                        Object dateObj = paramsList.get(0);
                        java.sql.Date reqDate;
                        if (dateObj instanceof java.sql.Date) {
                            reqDate = (java.sql.Date) dateObj;
                        } else {
                             // Assuming it's a util.Date from client
                             java.util.Date utilDate = (java.util.Date) dateObj;
                             reqDate = new java.sql.Date(utilDate.getTime());
                        }
                        
                        int reqGuests = (int) paramsList.get(1);

                        System.out.println("DEBUG: Calculating available slots for " + reqDate + ", Guests: " + reqGuests);

                        List<String> availableTimes = reservationLogic.getAvailableSlotsForDate(reqDate, reqGuests);
                        responseMsg = new BistroMessage(ActionType.GET_AVAILABLE_TIMES, availableTimes);
                    } else {
                        System.out.println("Error: GET_AVAILABLE_TIMES expected an ArrayList but got " + request.getData().getClass().getName());
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
                        
                        // Use the new safe delete method
                        ArrayList<Order> cancelledList = tableRepo.deleteTableSafely(tableIdToRemove);
                        
                        // If the list is not null, it means the table was found and removed
                        // If the list has items, it means we had overbooking
                        if (cancelledList != null) {
                            
                            if (!cancelledList.isEmpty()) {
                                log("[System] Table removed. Found " + cancelledList.size() + " overbooked orders.");
                                
                                // Notification Simulation Loop
                                for (Order o : cancelledList) {
                                    System.out.println(" [SIMULATION] SMS sent to: " + o.getEmail()); 
                                    System.out.println(" Message: Dear " + o.getCustomerName() + 
                                                       ", reservation #" + o.getOrderNumber() + 
                                                       " cancelled due to restaurant layout changes.");
                                }
                                responseMsg = new BistroMessage(ActionType.REMOVE_TABLE, 
                                        "Success. Table removed. " + cancelledList.size() + " orders cancelled.");
                            } else {
                                log("[Management] Removed Table " + tableIdToRemove + ". No conflicts.");
                                responseMsg = new BistroMessage(ActionType.REMOVE_TABLE, "Success");
                            }
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

                    if (hoursList.isEmpty()) {
                        log("[Warning] Opening hours list is empty!");
                    } else {
                        log("[Server] Sending " + hoursList.size() + " opening hour records.");
                    }

                    responseMsg = new BistroMessage(ActionType.GET_OPENING_HOURS, hoursList);
                    break;

                case UPDATE_OPENING_HOURS:
                    if (request.getData() instanceof OpeningHour) {
                        OpeningHour hourToUpdate = (OpeningHour) request.getData();

                        boolean success = hoursRepo.updateOpeningHour(hourToUpdate);

                        if (success) {
                            log("[Server] Hours updated successfully.");

                            ArrayList<Order> cancelledOrders = orderRepo.cancelConflictingOrders(hourToUpdate);
                            
                            if (!cancelledOrders.isEmpty()) {
       
                                log("[Notification System] Starting conflict resolution...");
                                
                                for (Order o : cancelledOrders) {
                                    String emailBody = "Dear " + o.getCustomerName() + 
                                                     ", due to schedule changes, your reservation #" + 
                                                     o.getOrderNumber() + " has been cancelled.";
                                                     
                                    System.out.println("[SIMULATION] Sending Email to: " + o.getEmail());
                                    System.out.println("Content: " + emailBody);
                                }
                                
                                log("[Notification System] Sent " + cancelledOrders.size() + " cancellation emails.");
                                log("---------------------------------------");
                                
                                responseMsg = new BistroMessage(ActionType.UPDATE_OPENING_HOURS, 
                                    "Success. Updated hours and notified " + cancelledOrders.size() + " customers.");
                            } else {
                                responseMsg = new BistroMessage(ActionType.UPDATE_OPENING_HOURS, "Success. No conflicting orders found.");
                            }
                            
                        } else {
                            log("[Error] Failed to update opening hours.");
                            responseMsg = new BistroMessage(ActionType.UPDATE_OPENING_HOURS, "Error: Update failed");
                        }
                    }
                    break;

                case GET_ORDER_BY_CODE:
                    System.out.println("DEBUG: Request received.");

                    if (request.getData() instanceof Integer) {
                        int code = (Integer) request.getData();
                        common.Order foundOrder = orderRepo.getOrderByCode(code);

                        if (foundOrder != null) {

                            int guests = foundOrder.getNumberOfGuests();
                            System.out.println("DEBUG: Guests from DB: " + guests);

                            User payer = null;
                            if (foundOrder.getMemberId() > 0) {
                                // Dummy user creation using correct constructor with Role enum
                                payer = new User(0, "D", "D", "0", "e", Role.MEMBER, "0", "0", 0);
                            }

                            double basePrice = guests * 100.0;
                            double finalPrice = userLogic.calculateFinalPrice(payer, basePrice);

                            System.out.println("DEBUG: Calculation: " + guests + " * 100 = " + basePrice + " -> Final: " + finalPrice);

                            foundOrder.setTotalPrice(finalPrice);

                            responseMsg = new BistroMessage(ActionType.GET_ORDER_BY_CODE, foundOrder);
                        } else {
                            responseMsg = new BistroMessage(ActionType.GET_ORDER_BY_CODE, null);
                        }
                    }
                    break;

                // Reports
                case GET_PERFORMANCE_REPORT:
                    // Extract parameters (Month and Year) sent by the client
                    int[] perfParams = (int[]) request.getData();
                    int perfMonth = perfParams[0];
                    int perfYear = perfParams[1];

                    //Retrieve raw data from the Database (Returns a simple Map)
                    Map<String, Integer> rawPerfData = orderRepo.getPerformanceReportData(perfMonth, perfYear);

                    // Create the Report object (Shared object located in Common)
                    common.Report perfReport = new common.Report(perfMonth, perfYear, "PERFORMANCE");
                    
                    // Populate the Report object with the raw data
                    if (rawPerfData != null) {
                        for (Map.Entry<String, Integer> entry : rawPerfData.entrySet()) {
                            perfReport.addData(entry.getKey(), entry.getValue());
                        }
                    }

                    // 5. Send the Report object back to the Client
                    responseMsg = new BistroMessage(ActionType.GET_PERFORMANCE_REPORT, perfReport);
                    log("[Reports] Generated Performance Report for " + perfMonth + "/" + perfYear);
                    break;

                case GET_SUBSCRIPTION_REPORT:
                    // Extract parameters
                    int[] subParams = (int[]) request.getData();
                    int subMonth = subParams[0];
                    int subYear = subParams[1];

                    // Retrieve raw data
                    Map<String, Integer> rawSubData = orderRepo.getSubscriptionReportData(subMonth, subYear);

                    //Create the Report object
                    common.Report subReport = new common.Report(subMonth, subYear, "SUBSCRIPTION");
                    
                    // Populate the Report object
                    if (rawSubData != null) {
                        for (Map.Entry<String, Integer> entry : rawSubData.entrySet()) {
                            subReport.addData(entry.getKey(), entry.getValue());
                        }
                    }

                    // Send response
                    responseMsg = new BistroMessage(ActionType.GET_SUBSCRIPTION_REPORT, subReport);
                    log("[Reports] Generated Subscription Report for " + subMonth + "/" + subYear);
                    break;

                case CLIENT_QUIT:
                    log("[Server] Client disconnected.");
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