package gui.staff;

import java.net.URL;
import java.util.ResourceBundle;

import client.ChatClient;
import client.ClientUI;
import common.ActionType;
import common.BistroMessage;
import common.User;
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
 * Controller for the Member Management screen.
 * <p>
 * This screen allows Restaurant Staff (Workers/Managers) to view a list of all
 * registered members in the system. From here, the staff can select a specific
 * member to view their personal details and order history.
 * </p>
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

	/**
	 * Initializes the controller class.
	 * <p>
	 * This method is automatically called after the FXML file has been loaded. It
	 * configures the table columns and loads the member data from the client's
	 * memory.
	 * </p>
	 * 
	 * @param location  The location used to resolve relative paths for the root
	 *                  object.
	 * @param resources The resources used to localize the root object.
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		setupTable();
		loadMembers();
	}

	/**
	 * Configures the TableView columns to bind to the User object properties.
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
	 * <p>
	 * This method retrieves the list of members from the static
	 * {@link ChatClient#allMembers} list, which was pre-fetched by the previous
	 * screen (WorkerMenuController).
	 * </p>
	 */
	private void loadMembers() {
		if (ChatClient.allMembers != null) {
			ObservableList<User> data = FXCollections.observableArrayList(ChatClient.allMembers);
			tblMembers.setItems(data);
		}
	}

	/**
	 * Handles the "View Details & History" button click.
	 * <p>
	 * Logic flow:
	 * <ol>
	 * <li>Checks if a user is selected in the table.</li>
	 * <li>Sends a request to the server to fetch the specific
	 * order history for the selected user ID.</li>
	 * <li>Waits for the server response (handled via {@link ClientUI#chat}).</li>
	 * <li>Loads the {@code MemberDetails.fxml} screen.</li>
	 * <li>Passes the selected User object to the new controller via
	 * {@code initData}.</li>
	 * <li>Opens the details window.</li>
	 * </ol>
	 * </p>
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
			//Request history for the selected user from the server
			BistroMessage msg = new BistroMessage(ActionType.GET_USER_HISTORY, selectedUser.getUserId());
			ClientUI.chat.accept(msg);
			// Execution halts here until the server responds and fills
			// ChatClient.listOfOrders

			//Load the Details Screen
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/staff/MemberDetails.fxml"));
			Parent root = loader.load();

			//Initialize the next controller with the selected user's data
			MemberDetailsController controller = loader.getController();
			controller.initData(selectedUser);

			//Display the new window
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