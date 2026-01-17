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
import ocsf.client.AbstractClient;

/**
 * This class overrides some of the methods defined in the abstract superclass
 * in order to give more functionality to the client.
 */
public class ChatClient extends AbstractClient {
	// Instance variables **********************************************

	ChatIF clientUI;

	// --- Live Controllers References (For Real-Time Updates) ---
	public static ManageTablesController tablesController = null;

	// --- Static Data Containers ---
	public static ArrayList<Order> listOfOrders = new ArrayList<>();
	public static ArrayList<Order> waitingList = new ArrayList<>();
	public static ArrayList<OpeningHour> openingHours = new ArrayList<>();
	public static ArrayList<String> availableTimeSlots = new ArrayList<>();
	public static ArrayList<User> allMembers = new ArrayList<>();
	public static ArrayList<Order> activeOrders = new ArrayList<>();
	public static ArrayList<Order> activeDiners = new ArrayList<>();
	public static ArrayList<Table> allTables = new ArrayList<>();
	public static ArrayList<Order> relevantOrders = new ArrayList<>();

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

	// Constructors ****************************************************

	public ChatClient(String host, int port, ChatIF clientUI) throws IOException {
		super(host, port);
		this.clientUI = clientUI;
	}

	// Instance methods ************************************************

	/**
	 * This method handles all data that comes in from the server.
	 *
	 * @param msg The message from the server.
	 */
	public void handleMessageFromServer(Object msg) {
		System.out.println("--> handleMessageFromServer");

		// Reset temporary variables
		order = null;
		operationSuccess = false;
		isAlternativeTime = false;

		// Safety check
		if (msg == null) {
			System.out.println("Received null from server");
			awaitResponse = false;
			return;
		}

		// Handle the wrapper (BistroMessage)
		if (msg instanceof BistroMessage) {
			BistroMessage bistroMsg = (BistroMessage) msg;
			ActionType type = bistroMsg.getType();
			Object data = bistroMsg.getData();

			System.out.println("Message Type: " + type);

			// 1. Handle Notifications (Email/SMS/Cancellations)
			if (type == ActionType.SERVER_NOTIFICATION) {
				String notificationText = (String) data;
				System.out.println("Notification received: " + notificationText);

				Platform.runLater(() -> {
					showNotificationAlert(notificationText);
				});
				return;
			}

			// 2. Remove Table Report (TEXT RESPONSE)
			if (type == ActionType.REMOVE_TABLE) {
				String fullMessage = (String) data;

				Platform.runLater(() -> {
					Alert alert = new Alert(AlertType.INFORMATION);
					alert.setTitle("Management Report");
					alert.setHeaderText("Table Removal Summary");

					javafx.scene.control.TextArea area = new javafx.scene.control.TextArea(fullMessage);
					area.setEditable(false);
					area.setWrapText(true);
					area.setMaxWidth(Double.MAX_VALUE);
					area.setMaxHeight(Double.MAX_VALUE);

					alert.getDialogPane().setContent(area);
					alert.showAndWait();

					// Request updated table list from server
					new Thread(() -> {
						this.handleMessageFromClientUI(new BistroMessage(ActionType.GET_ALL_TABLES, null));
					}).start();
				});

				awaitResponse = false;
				return;
			}

			// 3. Update Opening Hours Report (TEXT RESPONSE)
			else if (type == ActionType.UPDATE_OPENING_HOURS) {
				if (data instanceof String) {
					String hoursMsg = (String) data;

					Platform.runLater(() -> {
						Alert alert = new Alert(AlertType.INFORMATION);
						alert.setTitle("Opening Hours Update");
						alert.setHeaderText("Update Summary");

						javafx.scene.control.TextArea area = new javafx.scene.control.TextArea(hoursMsg);
						area.setEditable(false);
						area.setWrapText(true);
						area.setMaxWidth(Double.MAX_VALUE);
						area.setMaxHeight(Double.MAX_VALUE);

						alert.getDialogPane().setContent(area);
						alert.showAndWait();

						new Thread(() -> {
							this.handleMessageFromClientUI(new BistroMessage(ActionType.GET_OPENING_HOURS, null));
						}).start();
					});

					awaitResponse = false;
					return;
				}
			}

			// Case A: Received a List
			if (data instanceof ArrayList) {
				if (type == ActionType.GET_AVAILABLE_TIMES) {
					System.out.println("Got available time slots from server");
					availableTimeSlots = (ArrayList<String>) data;
				} else if (type == ActionType.GET_OPENING_HOURS) {
					System.out.println("Got OpeningHours list");
					openingHours = (ArrayList<OpeningHour>) data;
				} else if (type == ActionType.GET_ALL_ORDERS) {
					System.out.println("Got All Orders list");
					listOfOrders = (ArrayList<Order>) data;
				} else if (type == ActionType.GET_USER_HISTORY) {
					System.out.println("Got Orders and visits list");
					listOfOrders = (ArrayList<Order>) data;
				} else if (type == ActionType.GET_WAITING_LIST) {
					System.out.println("Got waiting list");
					waitingList = (ArrayList<Order>) data;
				} else if (type == ActionType.GET_ALL_MEMBERS) {
					System.out.println("Got all members list");
					allMembers = (ArrayList<User>) data;
				} else if (type == ActionType.GET_ALL_ACTIVE_ORDERS) {
					System.out.println("Got active orders list");
					activeOrders = (ArrayList<Order>) data;
				} else if (type == ActionType.GET_ACTIVE_DINERS) {
					System.out.println("Got active diners list");
					activeDiners = (ArrayList<Order>) data;
				} else if (type == ActionType.ORDER_ALTERNATIVES) {
					System.out.println("Requested time is full. Got alternatives.");
					ArrayList<java.sql.Timestamp> rawTimes = (ArrayList<java.sql.Timestamp>) data;
					availableTimeSlots = new ArrayList<>();
					java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm");
					for (java.sql.Timestamp ts : rawTimes) {
						availableTimeSlots.add(sdf.format(ts));
					}
					isAlternativeTime = true;

				} else if (type == ActionType.GET_RELEVANT_ORDERS) {
					System.out.println("Got relevant orders for Terminal selection");
					relevantOrders = (ArrayList<Order>) data;
				}

				// Handle Tables (Live Update Logic)
				else if (type == ActionType.GET_ALL_TABLES || type == ActionType.ADD_TABLE) {
					System.out.println("Got all tables list (Update)");
					allTables = (ArrayList<Table>) data;

					// If the ManageTables screen is open, refresh it automatically!
					if (tablesController != null) {
						Platform.runLater(() -> {
							tablesController.refreshData();
						});
					}
				}
			}

			// Case B: Received a single order
			else if (data instanceof Order) {
				System.out.println("Got Order inside BistroMessage");
				order = (Order) data;
			}

			// Case C: Received a User
			else if (data instanceof User) {
				if (type == ActionType.LOGIN) {
					System.out.println("User logged in.");
					user = (User) data;
				} else if (type == ActionType.REGISTER_CLEINT) {
					System.out.println("New client registered.");
					registeredUser = (User) data;
				} else if (type == ActionType.IDENTIFY_BY_QR) {
					if (terminalMember != null) {
						System.out.println("Terminal: Member identified: " + ((User) data).getFirstName());
					}
					terminalMember = (User) data;
				}
			}
			// Case D: Reports
			else if (data instanceof Report) {
				Report receivedReport = (Report) data;

				if (type == ActionType.GET_PERFORMANCE_REPORT) {
					System.out.println("ChatClient: Received Performance Report.");
					performanceReport = receivedReport;
				} else if (type == ActionType.GET_SUBSCRIPTION_REPORT) {
					System.out.println("ChatClient: Received Subscription Report.");
					subscriptionReport = receivedReport;
				}
			}
			// Case E: String Messages (Success/Error)
			else if (data instanceof String) {
				String msgString = (String) data;
				System.out.println("Got String message: " + msgString);
				returnMessage = msgString;
				if (returnMessage.equals("Success")) {
					operationSuccess = true;
				}
				// If it's an error, reset variables
				else if (data.toString().contains("Error") || data.toString().contains("not found")) {
					resetClientData();
				}
			}
		}

		// Release the waiting flag ONLY HERE
		awaitResponse = false;
	}

	/**
	 * Helper method to clear data on error.
	 */
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

	/**
	 * Displays a popup notification on the screen (Simulation for SMS/Email). Must
	 * be called inside Platform.runLater.
	 */
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
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			clientUI.display("Could not send message to server: Terminating client." + e);
			quit();
		}
	}

	public void quit() {
		try {
			closeConnection();
		} catch (IOException e) {
		}
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}
}