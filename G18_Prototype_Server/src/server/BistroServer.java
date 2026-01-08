package server;
import common.ActionType;
import common.BistroMessage;
import common.ChatIF;
import common.Member;
import common.Order;
import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;
import java.util.ArrayList;
import java.io.IOException;

// client connections and handles database operations via OrderRepository.
public class BistroServer extends AbstractServer {

    private OrderRepository orderRepo; // Handles database queries
    private final ChatIF ui;           // Interface for GUI logging
    private MemberRepository memberRepo = new MemberRepository();

    // Initializes the server on a specific port and sets up the DB repository.
    public BistroServer(int port, ChatIF ui) {
        super(port);
        this.orderRepo = new OrderRepository();
        this.ui = ui;
    }

    // Helper method to display messages on both Console and Server GUI
    private void log(String s) {
        System.out.println(s);
        if (ui != null) ui.display(s);
    }

    @Override
    protected void serverStarted() {
        log("[Server] Listening on port " + getPort());
    }

    @Override
    protected void serverStopped() {
        log("[Server] Stopped listening.");
    }

    // Triggered when a new client connects to the server
    @Override
    protected void clientConnected(ConnectionToClient client) {
        try {
            String ip = client.getInetAddress().getHostAddress();
            String host = client.getInetAddress().getHostName();
            client.setInfo("SavedIP", ip);
            client.setInfo("SavedHost", host);
            log("[Client Connected] Status=Connected | IP=" + ip + " | Host=" + host);
        } catch (Exception e) {
            log("[Client Connected] Error saving client info.");
        }
    }

    // Triggered when a client disconnects
    @Override
    protected void clientDisconnected(ConnectionToClient client) {
        String ip = (String) client.getInfo("SavedIP");
        String host = (String) client.getInfo("SavedHost");
        if (ip == null) ip = "Unknown";
        if (host == null) host = "Unknown";
        log("[Client Disconnected] Status=Disconnected | IP=" + ip + " | Host=" + host);
    }

    @Override
    protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
        log("-------------------------------");
        log("[Debug] Message received from Client: " + client.getInetAddress());

        // Ensure message is not null
        if (msg == null) {
            log("[Error] Message is NULL!");
            return;
        }
        
        log("[Debug] Message Class Type: " + msg.getClass().getSimpleName());

        // Ensure message is of type BistroMessage
        if (msg instanceof BistroMessage) {
            BistroMessage request = (BistroMessage) msg;
            ActionType type = request.getType(); 
            log("[Debug] ActionType received: " + type);
            if (type == ActionType.CLIENT_QUIT) {
                try {
                    log("Client requested disconnect. Closing connection...");
                    client.close();
                } catch (IOException e) {
                    log("[Error] closing connection: " + e.getMessage());
                }
                return; 
            }
            BistroMessage responseMsg = null; 

            // Request Processing based on ActionType
            switch (type) {
                case GET_ALL_ORDERS:
                    log("[Debug] Processing GET_ALL_ORDERS...");
                    try {
                        ArrayList<Order> list = orderRepo.getAllOrders();

                        if (list != null) {
                            log("[Debug] DB returned list with size: " + list.size());
                        } else {
                            log("[Error] DB returned NULL list!");
                        }
                        responseMsg = new BistroMessage(ActionType.GET_ALL_ORDERS, list);
                    } catch (Exception e) {
                        log("[Error] Failed during GET_ALL_ORDERS: " + e.getMessage());
                        e.printStackTrace();
                    }
                    break;

                case UPDATE_ORDER:
                    log("[Debug] Processing UPDATE_ORDER...");
                    Object data = request.getData();
                    log("[Debug] Data payload: " + (data != null ? data.toString() : "NULL"));

                    if (data instanceof Order) {
                        Order order = (Order) data;
                        log("[Debug] Updating Order #" + order.getOrderNumber());

                        String result = orderRepo.updateOrder(order.getOrderNumber(), order.getOrderDate(), order.getNumberOfGuests());
                        log("[Debug] DB Update Result: " + result);

                        responseMsg = new BistroMessage(ActionType.UPDATE_ORDER, result);
                    } else {
                        log("[Error] Data provided is NOT an Order object!");
                        responseMsg = new BistroMessage(ActionType.UPDATE_ORDER, "ERROR: Invalid Data");
                    }
                    break;

                case READ_ORDER:
                    log("[Debug] Processing READ_ORDER...");
                    Object idData = request.getData();
                    log("[Debug] Searching for ID: " + idData);

                    if (idData instanceof Integer) {
                        int id = (Integer) idData;
                        Order found = orderRepo.getOrderById(id);

                        if (found != null) {
                            log("[Debug] Order found!");
                        } else {
                            log("[Debug] Order NOT found (null).");
                        }
                        responseMsg = new BistroMessage(ActionType.READ_ORDER, found);
                    } else {
                        log("[Error] ID provided is not an Integer!");
                    }
                    break;
                
                case GET_ALL_MEMBERS:
                	log("[Debug] Processing GET_ALL_MEMBERS...");
                    ArrayList<Member> members = memberRepo.getAllMembers();
                    log("[Debug] members list = " + (members == null ? "NULL" : ("size=" + members.size())));
                    responseMsg = new BistroMessage(ActionType.GET_ALL_MEMBERS, members);
                    break;

                case GET_MEMBER_BY_ID:
                    Object idObj = request.getData();
                    if (idObj instanceof String) {
                        Member m = memberRepo.getMemberById((String) idObj);
                        responseMsg = new BistroMessage(ActionType.GET_MEMBER_BY_ID, m);
                    } else {
                        responseMsg = new BistroMessage(ActionType.GET_MEMBER_BY_ID, "ERROR: Invalid memberId");
                    }
                    break;

                case REGISTER_MEMBER: {
                    log("[Debug] Processing REGISTER_MEMBER...");
                    
                    // Taking the data that the client sent
                    Object mObj = request.getData();
                    // if its not kind of member it's an error
                    if (!(mObj instanceof Member)) {
                        responseMsg = new BistroMessage(ActionType.REGISTER_MEMBER, "ERROR: Invalid data");
                        break;
                    }
                    
                    Member m = (Member) mObj;
                    
                    // Trim to clean spaces
                    String username = (m.getUserName() == null) ? "" : m.getUserName().trim();
                    String fullName = (m.getFullName() == null) ? "" : m.getFullName().trim();
                    String phone = (m.getPhone() == null) ? "" : m.getPhone().trim();
                    String email = (m.getEmail() == null) ? "" : m.getEmail().trim();
                    
                    // If statements
                    if (username.isEmpty() || fullName.isEmpty()) {
                        responseMsg = new BistroMessage(ActionType.REGISTER_MEMBER, "ERROR: Username and Full Name are required");
                        break;
                    }

                  
                    if (memberRepo.usernameExists(username)) {
                        responseMsg = new BistroMessage(ActionType.REGISTER_MEMBER, "ERROR: Username already exists");
                        break;
                    }                  
                    

                    if (!email.isEmpty() && memberRepo.emailExists(email)) {
                        responseMsg = new BistroMessage(ActionType.REGISTER_MEMBER, "ERROR: Email already exists");
                        break;
                    }

                    if (!phone.isEmpty() && memberRepo.phoneExists(phone)) {
                        responseMsg = new BistroMessage(ActionType.REGISTER_MEMBER, "ERROR: Phone already exists");
                        break;
                    }
                    
                    // Create unique id's
                    String memberId = java.util.UUID.randomUUID().toString();
                    String qrCode   = java.util.UUID.randomUUID().toString();
                    
                    // Insert the details to DB
                    String res = memberRepo.insertMember(memberId, qrCode, username, fullName, m.getPhone(), m.getEmail());

                    if (res != null && !res.toUpperCase().startsWith("ERROR")) {
                        Member created = new Member(memberId, qrCode, username, fullName, m.getPhone(), m.getEmail());
                        responseMsg = new BistroMessage(ActionType.REGISTER_MEMBER, created);
                    } else {
                        responseMsg = new BistroMessage(ActionType.REGISTER_MEMBER, "ERROR: " + res);
                    }
                    break;
                }


                case UPDATE_MEMBER_CONTACT:
                    // הכי פשוט: שולחים Member (עם memberId + phone/email חדשים)
                    Object mObj2 = request.getData();
                    if (mObj2 instanceof Member) {
                        Member m = (Member) mObj2;
                        String res = memberRepo.updateContact(m.getMemberId(), m.getPhone(), m.getEmail());
                        responseMsg = new BistroMessage(ActionType.UPDATE_MEMBER_CONTACT, res);
                    } else {
                        responseMsg = new BistroMessage(ActionType.UPDATE_MEMBER_CONTACT, "ERROR: Invalid data");
                    }
                    break;
                    
                default:
                    log("[Error] Unknown ActionType received: " + type);
                    break;
            }

            // Send Response back to Client
            if (responseMsg != null) {
                try {
                    log("[Debug] Sending response to client...");
                    client.sendToClient(responseMsg);
                    log("[Debug] Response sent successfully.");
                } catch (Exception e) {
                    log("[Error] Failed to send response: " + e.getMessage());
                }
            } else {
                log("[Error] responseMsg is null, nothing sent.");
            }

        } else {
            log("[Error] Received object is NOT an instance of BistroMessage.");
            log("[Error] It is: " + msg.getClass().getName());
        }
        log("---------------------------");
    }
}

