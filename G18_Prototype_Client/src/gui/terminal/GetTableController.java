package gui.terminal;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import client.ChatClient;
import client.ClientUI;
import common.ActionType;
import common.BistroMessage;
import common.Order;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Controller for the "Get Table" (Check-in) screen.
 * * Implements requirements:
 * - Subscriber/Guest enters Confirmation Code to get a table.
 * - System validates the code against the database.
 * - Displays the assigned Table Number returned by the server via the Order object.
 */
public class GetTableController extends AbstractTerminalController implements Initializable {

	@FXML
	private TextField txtCode;
	@FXML
	private Label lblStatus;
	@FXML
	private Label lblInstruction;
	@FXML
	private Button btnCheckIn;
	@FXML
	private Button btnLostCode;
	@FXML
	private Button btnClose;
	@FXML
	private VBox resultBox; // To show the big table number
	@FXML
	private ComboBox<String> cmbMyOrders;

	/**
	 * Initializes the controller class.
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// Customize greeting if member is identified
		if (ChatClient.terminalMember != null) {
			lblInstruction.setText("Hello " + ChatClient.terminalMember.getFirstName() + ",\nSelect an order or enter code:");

			cmbMyOrders.setVisible(true);
			cmbMyOrders.setManaged(true);

			// Trigger the data loading from the server
			loadMemberOrders();

			// Add a listener to fill txtCode when an item is selected
			cmbMyOrders.setOnAction(e -> {
				String selected = cmbMyOrders.getValue();
				if (selected != null && selected.contains("#")) {
					// Extract the code from "Order #1234 - ..."
					String code = selected.split("#")[1].split(" ")[0];
					txtCode.setText(code);
				}
			});
		} else {
			lblInstruction.setText("Please enter your Confirmation Code:");
			cmbMyOrders.setVisible(false);
			cmbMyOrders.setManaged(false);
		}
	}

	/**
	 * Sends GET_RELEVANT_ORDERS to server and populates the ComboBox.
	 */
	private void loadMemberOrders() {
		// Clear current items
		cmbMyOrders.getItems().clear();

		// Request orders using the Member's ID
		ClientUI.chat.accept(new BistroMessage(ActionType.GET_RELEVANT_ORDERS, ChatClient.terminalMember.getUserId()));

		// Sync Wait Loop (Wait for server response to update ChatClient.relevantOrders)
		int attempts = 0;
		while (ChatClient.relevantOrders == null && attempts < 20) {
			try {
				Thread.sleep(100);
				attempts++;
			} catch (InterruptedException e) {
				// Handle interruption
			}
		}

		// Populate the ComboBox with the results
		if (ChatClient.relevantOrders != null && !ChatClient.relevantOrders.isEmpty()) {
			for (Order o : ChatClient.relevantOrders) {
				String item = "Order #" + o.getConfirmationCode() + " - " + o.getNumberOfGuests() + " diners";
				cmbMyOrders.getItems().add(item);
			}
			lblStatus.setText("Found " + ChatClient.relevantOrders.size() + " active orders.");
		} else {
			lblStatus.setText("No active orders found for today.");
		}
	}

	/**
	 * Sends the validation request to the server.
	 * * Logic Flow:
	 * 1. Validates input (numbers only).
	 * 2. Sends VALIDATE_ARRIVAL request with the confirmation code.
	 * 3. Waits for server response.
	 * 4. Expects the server to return the updated Order object containing the assigned Table ID.
	 * * @param event The action event.
	 */
	@FXML
	void checkIn(ActionEvent event) {
		String codeInput = txtCode.getText().trim();

		if (codeInput.isEmpty()) {
			// Using parent abstract method
			setStatus(lblStatus, "Please enter your confirmation code.", false);
			return;
		}

		try {
			int code = Integer.parseInt(codeInput);

			// 1. Send request to server
			System.out.println("Terminal: Validating arrival for code: " + code);
			ClientUI.chat.accept(new BistroMessage(ActionType.VALIDATE_ARRIVAL, code));

			// 2. Handle Response
			if (ChatClient.order != null) {

				// Extract the table ID from the returned order object
				int assignedTableId = ChatClient.order.getAssignedTableId();

				// Check if the ID is valid
				if (assignedTableId > 0) {
					showSuccess(String.valueOf(assignedTableId));
				} else {
					// Valid order but no table assigned (logic error on server or specific status)
					resultBox.setVisible(false);
					setStatus(lblStatus, "Order found, but no table assigned yet.", false);
				}
			} else {
				// Failure: The server returned null (Order not valid/found)
				resultBox.setVisible(false);
				setStatus(lblStatus, "Check-in failed. Verify code or try closer to time.", false);
			}

		} catch (NumberFormatException e) {
			setStatus(lblStatus, "Code must be numbers only.", false);
		}
	}

	/**
	 * Handles the "Lost Code" button.
	 * Logic:
	 * 1. If Member is identified -> Send code immediately to email/SMS.
	 * 2. If Guest -> Open the "Recover Code" popup to ask for Phone/Email.
	 */
	@FXML
	void lostCode(ActionEvent event) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("RecoverCode.fxml"));
			Parent root = loader.load();
			Stage stage = new Stage();
			stage.setTitle("Recover Lost Code");
			stage.setScene(new Scene(root));
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.show();
		} catch (IOException e) {
			e.printStackTrace();
			setStatus(lblStatus, "Error opening recovery screen.", false);
		}
	}

	/**
	 * Displays the success state with the assigned table number.
	 * * @param tableNumber The table number to display.
	 */
	private void showSuccess(String tableNumber) {
		resultBox.setVisible(true);
		setStatus(lblStatus, "Welcome! Please proceed to your table.", true);

		// Update the big number display (assuming it's the second child of the VBox)
		if (resultBox.getChildren().size() > 1 && resultBox.getChildren().get(1) instanceof Label) {
			((Label) resultBox.getChildren().get(1)).setText(tableNumber);
		}

		btnCheckIn.setDisable(true);
		txtCode.setDisable(true);
		btnClose.setText("Done");
	}
}