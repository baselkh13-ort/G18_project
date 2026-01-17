package client;

import java.io.IOException;
import java.util.ArrayList;

import common.ActionType;
import common.BistroMessage;
import common.ChatIF;
import common.OpeningHour;
import common.Order;
import common.Report;
import common.Table;
import common.User;
import gui.staff.ManageTablesController;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextArea;
import ocsf.client.AbstractClient;

/**
 * The central class that handles the communication between the client and the server.
 */
public class ChatClient extends AbstractClient {

    // Instance variables
    ChatIF clientUI;

    // Live Controllers References
    public static ManageTablesController tablesController = null;

    // Static Data Containers
    public static ArrayList<Order> listOfOrders = new ArrayList<>();
    public static ArrayList<Order> waitingList = new ArrayList<>();
    public static ArrayList<OpeningHour> openingHours = new ArrayList<>();
    public static ArrayList<String> availableTimeSlots = new ArrayList<>();
    public static ArrayList<User> allMembers = new ArrayList<>();
    public static ArrayList<Order> activeOrders = new ArrayList<>();
    public static ArrayList<Order> activeDiners = new ArrayList<>();
    public static ArrayList<Table> allTables = new ArrayList<>();
    public static ArrayList<Order> relevantOrders = new ArrayList<>();

    // Single Objects & Flags
    public static Order order = null;
    public static boolean awaitResponse = false;
    public static User user = null;
    public static User registeredUser = null;
    public static boolean operationSuccess = false;
    public static Report performanceReport = null;
    public static Report subscriptionReport = null;
    public static User terminalMember = null;
    public static String returnMessage = null;
    public static boolean isAlternativeTime = false;

    // Constructors
    public ChatClient(String host, int port, ChatIF clientUI) throws IOException {
        super(host, port);
        this.clientUI = clientUI;
    }

    // Instance methods

    /**
     * Handles all data that comes in from the server.
     * Parses the message and updates the relevant data or triggers UI alerts.
     *
     * @param msg The message received from the server.
     */
    public void handleMessageFromServer(Object msg) {
        System.out.println("--> handleMessageFromServer");

        // Reset temporary flags
        order = null;
        operationSuccess = false;
        isAlternativeTime = false;

        if (msg == null) {
            System.out.println("Received null from server");
            awaitResponse = false;
            return;
        }

        if (msg instanceof BistroMessage) {
            BistroMessage bistroMsg = (BistroMessage) msg;
            ActionType type = bistroMsg.getType();
            Object data = bistroMsg.getData();

            System.out.println("Message Type: " + type);

            // 1. Critical Notifications
            if (type == ActionType.SERVER_NOTIFICATION) {
                String notificationText = (String) data;
                Platform.runLater(() -> showNotificationAlert(notificationText));
                return;
            }

            // 2. Management Reports
            if (type == ActionType.REMOVE_TABLE) {
                if (data instanceof String) {
                    showReportAlert("Management Report", "Table Removal Summary", (String) data, ActionType.GET_ALL_TABLES);
                    return;
                }
            }
            else if (type == ActionType.UPDATE_TABLE) {
                if (data instanceof String) {
                    showReportAlert("Management Report", "Table Update Summary", (String) data, ActionType.GET_ALL_TABLES);
                    return;
                }
            }
            else if (type == ActionType.UPDATE_OPENING_HOURS) {
                if (data instanceof String) {
                    showReportAlert("Opening Hours Update", "Update Summary", (String) data, ActionType.GET_OPENING_HOURS);
                    return;
                }
            }

            // Case A: Received a List of objects
            if (data instanceof ArrayList) {
                handleListResponse(type, (ArrayList<?>) data);
            }

            // Case B: Single Objects (Order, User, Report)
            else if (data instanceof Order) {
                order = (Order) data;
            }
            else if (data instanceof User) {
                handleUserResponse(type, (User) data);
            }
            else if (data instanceof Report) {
                handleReportResponse(type, (Report) data);
            }
            // Case C: String Messages
            else if (data instanceof String) {
                String msgString = (String) data;
                returnMessage = msgString;
                if (returnMessage.equals("Success")) operationSuccess = true;
                else if (msgString.contains("Error") || msgString.contains("not found")) resetClientData();
            }
        }
        awaitResponse = false;
    }

    /**
     * Helper method to handle list responses to reduce code length in main handler.
     * @param type The action type.
     * @param list The raw list received.
     */
    @SuppressWarnings("unchecked")
    private void handleListResponse(ActionType type, ArrayList<?> list) {
        if (type == ActionType.GET_ALL_TABLES || type == ActionType.ADD_TABLE) {
            allTables = (ArrayList<Table>) list;
            if (tablesController != null) Platform.runLater(() -> tablesController.refreshData());
        } 
        else if (type == ActionType.GET_AVAILABLE_TIMES) availableTimeSlots = (ArrayList<String>) list;
        else if (type == ActionType.GET_OPENING_HOURS) openingHours = (ArrayList<OpeningHour>) list;
        else if (type == ActionType.GET_ALL_ORDERS || type == ActionType.GET_USER_HISTORY) listOfOrders = (ArrayList<Order>) list;
        else if (type == ActionType.GET_WAITING_LIST) waitingList = (ArrayList<Order>) list;
        else if (type == ActionType.GET_ALL_MEMBERS) allMembers = (ArrayList<User>) list;
        else if (type == ActionType.GET_ALL_ACTIVE_ORDERS) activeOrders = (ArrayList<Order>) list;
        else if (type == ActionType.GET_ACTIVE_DINERS) activeDiners = (ArrayList<Order>) list;
        else if (type == ActionType.GET_RELEVANT_ORDERS) relevantOrders = (ArrayList<Order>) list;
        else if (type == ActionType.ORDER_ALTERNATIVES) {
            ArrayList<java.sql.Timestamp> rawTimes = (ArrayList<java.sql.Timestamp>) list;
            availableTimeSlots = new ArrayList<>();
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm");
            for (java.sql.Timestamp ts : rawTimes) availableTimeSlots.add(sdf.format(ts));
            isAlternativeTime = true;
        }
    }
    
    private void handleUserResponse(ActionType type, User userData) {
        if (type == ActionType.LOGIN) user = userData;
        else if (type == ActionType.REGISTER_CLEINT) registeredUser = userData;
        else if (type == ActionType.IDENTIFY_BY_QR) terminalMember = userData;
    }

    private void handleReportResponse(ActionType type, Report reportData) {
        if (type == ActionType.GET_PERFORMANCE_REPORT) performanceReport = reportData;
        else if (type == ActionType.GET_SUBSCRIPTION_REPORT) subscriptionReport = reportData;
    }

    // --- NEW HELPER METHOD ---

    /**
     * Shows a popup Alert with a scrollable TextArea and triggers a data refresh.
     * Use this for management reports like Table Updates or Removal.
     *
     * @param title The title of the Alert window.
     * @param header The header text inside the Alert.
     * @param content The long text content to display.
     * @param refreshAction The ActionType to send to server to refresh data (e.g., GET_ALL_TABLES).
     */
    private void showReportAlert(String title, String header, String content, ActionType refreshAction) {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(header);

            TextArea area = new TextArea(content);
            area.setEditable(false);
            area.setWrapText(true);
            area.setMaxWidth(Double.MAX_VALUE);
            area.setMaxHeight(Double.MAX_VALUE);

            alert.getDialogPane().setContent(area);
            alert.showAndWait();

            // Refresh Logic
            if (refreshAction != null) {
                new Thread(() -> {
                    this.handleMessageFromClientUI(new BistroMessage(refreshAction, null));
                }).start();
            }
        });
        
        awaitResponse = false;
    }

    //Standard Methods

    public static void resetClientData() {
        user = null;
        order = null;
        listOfOrders.clear();
        waitingList.clear();
        allMembers.clear();
        availableTimeSlots.clear();
        activeOrders.clear();
        activeDiners.clear();
        allTables.clear();
        performanceReport = null;
        subscriptionReport = null;
    }

    private void showNotificationAlert(String text) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Message from Bistro System");
        alert.setHeaderText("Simulation: SMS/Email Notification");
        alert.setContentText(text);
        alert.show();
    }

    public void handleMessageFromClientUI(Object message) {
        try {
            awaitResponse = true;
            sendToServer(message);
            while (awaitResponse) {
                try { Thread.sleep(100); } catch (InterruptedException e) { e.printStackTrace(); }
            }
        } catch (IOException e) {
            clientUI.display("Could not send message to server. Terminating client." + e);
            quit();
        }
    }

    public void quit() {
        try { closeConnection(); } catch (IOException e) {}
        System.exit(0);
    }
}