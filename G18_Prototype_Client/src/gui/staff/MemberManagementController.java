package gui.staff;

import java.net.URL;
import java.util.ResourceBundle;

import client.ChatClient;
import client.ClientUI;
import common.ActionType;
import common.BistroMessage;
import common.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

/**
 * Controller for the Table Management Screen.
 *
 * This class allows the Restaurant Manager to:
 * - Add new tables to the restaurant layout.
 * - Update the capacity of existing tables.
 * - Remove tables from the restaurant.
 * If the capacity decreases and future reservations cannot be accommodated, the server is responsible for canceling
 * those orders and notifying the customers.
 */
public class MemberManagementController implements Initializable {

	// FXML Table Components
	@FXML
	private TableView<User> tblMembers;
	@FXML
	private TableColumn<User, Integer> colId;
	@FXML
	private TableColumn<User, String> colFirstName;
	@FXML
	private TableColumn<User, String> colLastName;
	@FXML
	private TableColumn<User, String> colPhone;
	@FXML
	private TableColumn<User, String> colEmail;

	// FXML Buttons
	@FXML
	private Button btnViewDetails;
	@FXML
	private Button btnBack;
	@FXML
	private Button btnRefresh;

	/**
	 * Initializes the controller class.
	 * This method is automatically called after the FXML file has been loaded. It
	 * configures the table columns and loads the initial member data from the
	 * client's memory.
	 * * @param location The location used to resolve relative paths for the root
	 * object.
	 * 
	 * @param resources The resources used to localize the root object.
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		setupTable();
		loadMembers();
	}

	/**
	 * Configures the TableView columns to bind to the User object properties. Uses
	 * PropertyValueFactory to map the fields.
	 */
	private void setupTable() {
		colId.setCellValueFactory(new PropertyValueFactory<>("userId"));
		colFirstName.setCellValueFactory(new PropertyValueFactory<>("firstName"));
		colLastName.setCellValueFactory(new PropertyValueFactory<>("lastName"));
		colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
		colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
	}

	/**
	 * Populates the table with member data.
	 * Retrieves the list of members from the static {@link ChatClient#allMembers}
	 * list. Wraps the list in an ObservableList for the JavaFX TableView.
	 */
	private void loadMembers() {
		if (ChatClient.allMembers != null) {
			ObservableList<User> data = FXCollections.observableArrayList(ChatClient.allMembers);
			tblMembers.setItems(data);
			tblMembers.refresh(); // Refreshes the visual elements of the table
		}
	}

	/**
	 * Handles the "Refresh" button click.
	 * Sends a request to the server to fetch the most up-to-date list of members.
	 * This runs on a separate thread to avoid freezing the UI while waiting for the
	 * server.
	 * * @param event The ActionEvent triggered by clicking the refresh button.
	 */
	@FXML
	void refreshData(ActionEvent event) {
		System.out.println("Refreshing member list...");

		// 1. Clear local cache to ensure we get fresh data
		ChatClient.allMembers = null;

		// 2. Send request to Server
		ClientUI.chat.accept(new BistroMessage(ActionType.GET_ALL_MEMBERS, null));

		// 3. Create a background thread for waiting
		new Thread(() -> {
			int attempts = 0;
			// Wait loop: checks if the server response has populated 'allMembers'
			while (ChatClient.allMembers == null && attempts < 20) {
				try {
					Thread.sleep(100); // Wait 100ms between checks
					attempts++;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			// 4. Update the UI on the JavaFX Application Thread
			Platform.runLater(() -> {
				if (ChatClient.allMembers != null) {
					loadMembers(); // Reload data into the table
					System.out.println("GUI: Refresh complete.");
				} else {
					// Handle server timeout or error
					Alert alert = new Alert(AlertType.ERROR);
					alert.setTitle("Connection Error");
					alert.setContentText("Failed to retrieve data from server. Please try again.");
					alert.show();
				}
			});
		}).start();
	}

	/**
	 * Handles the "View Details & History" button click.
	 *
	 * Logic flow:
	 * 1. Validates that a row is selected.
	 * 2. Requests the order history for the selected user from the server.
	 * 3. Waits for the server response (Synchronized wait loop).
	 * 4. Opens the MemberDetails.fxml screen with the populated data.
	 *
	 * @param event The ActionEvent triggered by clicking the button.
	 */
	@FXML
	void viewDetails(ActionEvent event) {
		User selectedUser = tblMembers.getSelectionModel().getSelectedItem();

		// Validation: Ensure a row is selected
		if (selectedUser == null) {
			Alert alert = new Alert(AlertType.WARNING);
			alert.setTitle("No Selection");
			alert.setContentText("Please select a member from the list first.");
			alert.showAndWait();
			return;
		}

		try {
			// Reset the order history cache before requesting new data
			ChatClient.listOfOrders = null;

			// Request history for the selected user
			BistroMessage msg = new BistroMessage(ActionType.GET_USER_HISTORY, selectedUser.getUserId());
			ClientUI.chat.accept(msg);

			// Synchronized Wait Loop: Wait for server response
			int attempts = 0;
			while (ChatClient.listOfOrders == null && attempts < 20) {
				try {
					Thread.sleep(100);
					attempts++;
				} catch (Exception e) {
				}
			}

			// Load the Details Screen FXML
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/staff/MemberDetails.fxml"));
			Parent root = loader.load();

			// Initialize the next controller with the selected user's data
			MemberDetailsController controller = loader.getController();
			controller.initData(selectedUser);

			// Open the new stage
			Stage stage = new Stage();
			stage.setTitle("Member Details: " + selectedUser.getFirstName());
			stage.setScene(new Scene(root));
			stage.show();

		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error opening Member Details screen.");
		}
	}

	/**
	 * Closes the current Member Management window. * @param event The ActionEvent
	 * triggered by clicking the back button.
	 */
	@FXML
	void goBack(ActionEvent event) {
		((Stage) btnBack.getScene().getWindow()).close();
	}
}