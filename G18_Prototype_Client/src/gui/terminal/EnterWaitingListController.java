package gui.terminal;

import java.net.URL;
import java.sql.Timestamp;
import java.util.ResourceBundle;

import client.ChatClient;
import client.ClientUI;
import common.ActionType;
import common.BistroMessage;
import common.Order;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controller for the "Join Waiting List" screen at the Restaurant Terminal.
 * <p>
 * <b>Requirement Implementation:</b> Allows any customer (Member or Guest) to
 * join the waiting list for a table.
 * </p>
 * <p>
 * <b>Logic Flow:</b>
 * <ul>
 * <li><b>Member:</b> If identified via card swipe (Login), details are
 * auto-filled.</li> Guest:Must manually enter Name, Diners, and at least one
 * identifier (Phone OR Email). Output: Returns a Confirmation Code to the user
 * upon success.
 */
public class EnterWaitingListController implements Initializable {

	@FXML
	private ComboBox<Integer> cmbDiners;
	@FXML
	private TextField txtName;
	@FXML
	private TextField txtPhone;
	@FXML
	private TextField txtEmail;
	@FXML
	private Label lblStatus;
	@FXML
	private Button btnJoin;
	@FXML
	private Button btnClose;

	/**
	 * Initializes the screen. Populates the diner count combo box and pre-fills
	 * data if a member is logged in.
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// Populate diners (1 to 10)
		for (int i = 1; i <= 10; i++) {
			cmbDiners.getItems().add(i);
		}

		// Check if a member is identified (Simulated Card Swipe)
		if (ChatClient.terminalMember != null) {
			// Auto-fill details for better UX
			txtName.setText(ChatClient.terminalMember.getFirstName() + " " + ChatClient.terminalMember.getLastName());
			txtPhone.setText(ChatClient.terminalMember.getPhone());
			txtEmail.setText(ChatClient.terminalMember.getEmail());

			// Lock fields that shouldn't change
			txtName.setEditable(false);

			setStatus("Welcome back, " + ChatClient.terminalMember.getFirstName(), true);
		}
	}

	/**
	 * Sends the request to join the waiting list. Validates that guests provide at
	 * least one contact method (Phone/Email)
	 * 
	 * @param event The button click event.
	 */
	@FXML
	void submitRequest(ActionEvent event) {
		String name = txtName.getText().trim();
		String phone = txtPhone.getText().trim();
		String email = txtEmail.getText().trim();
		Integer diners = cmbDiners.getValue();

		// Validation Logic

		// Requirement: Must specify number of diners
		if (diners == null) {
			setStatus("Error: Please select number of guests.", false);
			return;
		}

		// Requirement: Name is mandatory for the host to call out
		if (name.isEmpty()) {
			setStatus("Error: Please enter your name.", false);
			return;
		}

		// Guest must provide "Phone AND/OR Email" If both are empty, we block the
		// request.
		if (phone.isEmpty() && email.isEmpty()) {
			setStatus("Error: Must provide Phone OR Email.", false);
			return;
		}
		// Validation: Phone Format (starts with 0, total 10 digits)
		if (!phone.isEmpty()) {
			if (!phone.matches("^0\\d{8,9}$")) {
				setStatus("Error: Invalid phone number (e.g., 0501234567).", false);
				return;
			}
		}

		// Validation: Email Format
		if (!email.isEmpty()) {
			if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
				setStatus("Error: Invalid email address format.", false);
				return;
			}
		}

		// Create Order Object
		Order requestOrder = new Order();

		requestOrder.setCustomerName(name);
		requestOrder.setPhone(phone);
		requestOrder.setEmail(email);
		requestOrder.setNumberOfGuests(diners);

		// Set "Now" as the requested time
		requestOrder.setOrderDate(new Timestamp(System.currentTimeMillis()));

		// Link Member ID if logged in (otherwise 0 for Guest)
		if (ChatClient.terminalMember != null) {
			requestOrder.setMemberId(ChatClient.terminalMember.getUserId());
		} else {
			requestOrder.setMemberId(0);
		}

		System.out.println("Terminal: Sending Join-Waitlist request...");

		// Send to Server
		ClientUI.chat.accept(new BistroMessage(ActionType.ENTER_WAITLIST, requestOrder));
		// Handle Response
		// The server is expected to return the Order object with a generated
		// Confirmation Code.
		if (ChatClient.order != null) {
			if ("SEATED".equals(ChatClient.order.getStatus()) && ChatClient.order.getAssignedTableId() != null) {
				// CASE A: Table Available
				int tableId = ChatClient.order.getAssignedTableId();
				setStatus("A table is available! Please proceed to Table:" + tableId, true);
				lblStatus.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold; -fx-font-size: 16px;");
			} else {
				// CASE B: Added to Waiting List
				int code = ChatClient.order.getConfirmationCode();
				setStatus("Success! Your Code: " + code, true);
				lblStatus.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;"); // Green
			}
			// Disable button to prevent double-submit
			btnJoin.setDisable(true);
			btnClose.setText("Done");

		} else {
			// Error handling
			String errorMsg = (ChatClient.returnMessage != null) ? ChatClient.returnMessage : "Failed to join list.";
			setStatus("Error: " + errorMsg, false);
		}
	}

	/**
	 * Updates the status label text and color.
	 * 
	 * @param msg       The message to display.
	 * @param isSuccess True for green text, False for red text.
	 */
	private void setStatus(String msg, boolean isSuccess) {
		lblStatus.setText(msg);
		if (isSuccess) {
			lblStatus.setStyle("-fx-text-fill: #2ecc71;"); // Green
		} else {
			lblStatus.setStyle("-fx-text-fill: #e74c3c;"); // Red
		}
	}

	@FXML
	void closeWindow(ActionEvent event) {
		((Stage) btnClose.getScene().getWindow()).close();
	}
}