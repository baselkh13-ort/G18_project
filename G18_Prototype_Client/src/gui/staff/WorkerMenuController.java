package gui.staff;

import java.net.URL;
import java.util.ResourceBundle;

import client.ChatClient;
import client.ClientUI;
import common.ActionType;
import common.BistroMessage;
import common.Role;
import common.User;
import gui.utils.AbstractBistroController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import logic.ScreenMode;

/**
 * Controller class for the main menu used by Staff members (Workers and
 * Managers).
 * <p>
 * This controller handles navigation to different functional areas of the
 * system, such as registering clients, viewing orders, and managing reports
 * (Manager only).
 * </p>
 */
public class WorkerMenuController extends AbstractBistroController implements Initializable {

	// FXML Components

	/** Label to display the welcome message with the user's name. */
	@FXML
	private Label lblWelcome;

	/** Button to navigate to the Client Registration screen. */
	@FXML
	private Button btnRegisterClient;

	/** Button to view all orders in the system (Read-Only mode). */
	@FXML
	private Button btnViewAll;

	/** Button to view the waiting list. */
	@FXML
	private Button btnWaitingList;
	@FXML
	private Button btnMemberManagment;
	@FXML
	private Button btnEditHours;
	@FXML
	private Button btnActiveOrders;
	@FXML
	private Button btnActiveDiners;
	@FXML
	private Button btnManageTables;
	
	
	/** Button to view monthly reports (Visible only to Managers). */
	@FXML
	private Button btnViewReports;

	/** Button to exit the application. */
	@FXML
	private Button btnExit;

	// Initialization

	/**
	 * Initializes the controller class.
	 * <p>
	 * Sets the welcome message based on the logged-in user and toggles visibility
	 * of Manager-only buttons based on the user's role.
	 * </p>
	 *
	 * @param location  The location used to resolve relative paths for the root
	 *                  object.
	 * @param resources The resources used to localize the root object.
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		User user = ChatClient.user;

		// Safety check to prevent NullPointerException if loaded without login
		if (user == null) {
			System.err.println("Warning: No user logged in.");
			return;
		}

		// Set personalized welcome message
		if (lblWelcome != null) {
			lblWelcome.setText("Hello, " + user.getFirstName() + " (" + user.getRole() + ")");
		}

		// Handle Role-Based Access Control (RBAC)
		if (btnViewReports != null) {
			if (user.getRole() == Role.WORKER) {
				// WORKER VIEW - Hide sensitive buttons
				btnViewReports.setVisible(false);
				btnViewReports.setManaged(false); // Removes the space it occupied
			} else {
				// MANAGER VIEW - Show everything
				btnViewReports.setVisible(true);
				btnViewReports.setManaged(true);
			}
		}
	}

	/**
	 * Helper method to start/display this screen manually. * @param primaryStage
	 * The main stage of the application.
	 * 
	 * @throws Exception If the FXML file cannot be loaded.
	 */
	public void start(Stage primaryStage) throws Exception {
		Parent root = FXMLLoader.load(getClass().getResource("/gui/staff/WorkerMenu.fxml"));
		Scene scene = new Scene(root);
		// Assuming the CSS is in the same folder
		scene.getStylesheets().add(getClass().getResource("/gui/staff/WorkerMenu.css").toExternalForm());

		primaryStage.setTitle("Bistro - Staff Menu");
		primaryStage.setScene(scene);
		primaryStage.show();
	}

	// Action Handlers (Navigation)

	/**
	 * Navigates to the "Register New Client" screen. * @param event The event
	 * triggered by clicking the register button.
	 */
	@FXML
	void register(ActionEvent event) {
		try {
			// 1. Load the FXML
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/staff/RegisterClient.fxml"));
			Parent root = loader.load();

			// 2. Prepare the Stage
			Stage stage = new Stage();
			stage.setTitle("Register New Client");
			stage.setScene(new Scene(root));

			// 3. Show new window and hide the current one
			stage.show();
			((Node) event.getSource()).getScene().getWindow().hide();

		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error loading RegisterClient.fxml");
		}
	}

	/**
	 * Navigates to the "View All Orders" screen.
	 * <p>
	 * Sends a request to the server to fetch all orders (GET_ALL_ORDERS) and sets
	 * the application mode to VIEW (Read-Only).
	 * </p>
	 * * @param event The event triggered by clicking the View All button.
	 */
	@FXML
	void clickViewAll(ActionEvent event) {
		System.out.println("Selected: View All Orders");

		try {
			// 1. Logic Setup
			ClientUI.currentMode = ScreenMode.VIEW;
			BistroMessage msg = new BistroMessage(ActionType.GET_ALL_ORDERS, null);
			ClientUI.chat.accept(msg);

			// 2. UI Loading
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/utils/OrderList.fxml"));
			Parent root = loader.load();
			Scene scene = new Scene(root);
			scene.getStylesheets().add(getClass().getResource("/gui/utils/OrderList.css").toExternalForm());

			Stage stage = new Stage();
			stage.setTitle("All Orders Table");
			stage.setScene(scene);

			// 3. Show and Hide
			stage.show();
			((Node) event.getSource()).getScene().getWindow().hide();

		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error loading OrderList.fxml");
		}
	}

	/**
	 * Opens the Waiting List Management screen.
	 * <p>
	 * Loads the 'ShowWaitingList' FXML and displays it in a new window.
	 * </p>
	 * * @param event The event triggered by clicking the Waiting List button.
	 */
	@FXML
	void openWaitingList(ActionEvent event) {
		try {
			BistroMessage msg = new BistroMessage(ActionType.GET_WAITING_LIST, null);
			ClientUI.chat.accept(msg);
			// Load the FXML
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/staff/ShowWaitingList.fxml"));
			Parent root = loader.load();
			Scene scene = new Scene(root);

			scene.getStylesheets().add(getClass().getResource("/gui/staff/ShowWaitingList.css").toExternalForm());

			Stage stage = new Stage();
			stage.setTitle("Waiting List Management");
			stage.setScene(scene);

			// Show and Hide
			stage.show();

			((Node) event.getSource()).getScene().getWindow().hide();

		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error loading ShowWaitingList.fxml");
		}
	}

	/**
	 * Opens the Member Management screen.
	 * <p>
	 * Fetches the list of all members from the server BEFORE opening the window.
	 * </p>
	 */
	@FXML
	void openMembersManagement(ActionEvent event) {
		System.out.println("Selected: Members Management");
		try {
			// Fetch all members
			BistroMessage msg = new BistroMessage(ActionType.GET_ALL_MEMBERS, null);
			ClientUI.chat.accept(msg);

			// Open the Member List Screen
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/staff/MemberManagement.fxml"));
			Parent root = loader.load();
			Scene scene = new Scene(root);

			Stage stage = new Stage();
			stage.setTitle("Bistro - Member Management");
			stage.setScene(scene);
			stage.show();

		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error loading MemberManagement.fxml");
		}
	}

	/**
	 * Opens the Opening Hours Management screen.
	 * <p>
	 * This screen allows both Representatives and Managers to update: 1. Regular
	 * weekly hours. 2. Specific dates (exceptions/holidays).
	 * </p>
	 * 
	 * @param event The event triggered by clicking the "Manage Opening Hours"
	 *              button.
	 */

	@FXML
	void openEditOpeningHours(ActionEvent event) {
		try {
			// Load the FXML file for editing opening hours
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/staff/EditOpeningHours.fxml"));
			Parent root = loader.load();

			// Create and show the new stage (window)
			Stage stage = new Stage();
			stage.setTitle("Bistro - Manage Opening Hours");
			stage.setScene(new Scene(root));
			stage.show(); // show() allows opening multiple windows, showAndWait() blocks the main one.

		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error loading EditOpeningHours.fxml");
		}
	}

	/**
	 * Fetches all active orders and opens the Active Orders management screen.
	 * <p>
	 * Sends a GET_ALL_ACTIVE_ORDERS request to the server using Eager Loading.
	 * </p>
	 * 
	 * @param event The event triggered by the "Active Orders" button.
	 */
	@FXML
	void openActiveOrders(ActionEvent event) {
		try {
			// 1. Reset list to avoid stale data
			ChatClient.activeOrders.clear();

			// 2. Request data from server
			BistroMessage msg = new BistroMessage(ActionType.GET_ALL_ACTIVE_ORDERS, null);
			ClientUI.chat.accept(msg);

			// 3. Load the new screen
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/staff/ActiveOrders.fxml"));
			Parent root = loader.load();

			Stage stage = new Stage();
			stage.setTitle("Bistro - Active Orders");
			stage.setScene(new Scene(root));
			stage.show();

		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error loading ActiveOrders.fxml");
		}
	}

	/**
	 * Fetches the list of currently seated diners and opens the Active Diners
	 * screen.
	 * <p>
	 * Sends a GET_ACTIVE_DINERS request to the server using Eager Loading.
	 * </p>
	 * 
	 * @param event The event triggered by the "Active Diners" button.
	 */
	@FXML
	void openActiveDiners(ActionEvent event) {
		try {
			// 1. Reset list to avoid stale data
			ChatClient.activeDiners.clear();

			// 2. Request data from server
			BistroMessage msg = new BistroMessage(ActionType.GET_ACTIVE_DINERS, null);
			ClientUI.chat.accept(msg);

			// 3. Load the new screen
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/staff/ActiveDiners.fxml"));
			Parent root = loader.load();

			Stage stage = new Stage();
			stage.setTitle("Bistro - Current Diners");
			stage.setScene(new Scene(root));
			stage.show();

		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error loading ActiveDiners.fxml");
		}
	}
	
	/**
     * Opens the Table Management screen.
     * <p>
     * Fetches the current list of tables from the server using Eager Loading
     * before displaying the management interface.
     * </p>
     * @param event The event triggered by the "Manage Tables" button.
     */
    @FXML
    void openManageTables(ActionEvent event) {
        try {
            // 1. Clear old data to prevent stale views
            ChatClient.allTables.clear();
            
            // 2. Request data from server
            BistroMessage msg = new BistroMessage(ActionType.GET_ALL_TABLES, null);
            ClientUI.chat.accept(msg);

            // 3. Load and show the screen
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/staff/ManageTables.fxml"));
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.setTitle("Bistro - Manage Tables");
            stage.setScene(new Scene(root));
            stage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error loading ManageTables.fxml");
        }
    }
    
    /**
     * Opens the Manager's Monthly Reports screen.
     * <p>
     * This action is restricted to users with the {@code MANAGER} role only.
     * The screen displays automated statistical reports regarding restaurant performance
     * (arrival times) and subscription data (orders trend).
     * </p>
     *
     * @param event The event triggered by clicking the "View Reports" button.
     */
    @FXML
    void openReportsScreen(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/staff/ViewReports.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Bistro - Monthly Reports");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }
    
    /**
     * Triggered by the "Logout" button in FXML.
     * Uses the REUSED logic from AbstractBistroController.
     */
    @FXML
    public void clickLogout(ActionEvent event) {
		// call to the function in the AbstractBistroController class
        super.logout(event); 
    }
}