package gui.customer;

import java.net.URL;
import java.util.ResourceBundle;

import client.ChatClient;
import client.ClientUI;
import common.ActionType;
import common.BistroMessage;
import common.Order;
import common.Role;
import common.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Controller for the Order Cancellation Screen.
 * <p>
 * This controller handles the cancellation process for existing orders. It
 * supports two modes of operation based on the user's login status:
 * <ul>
 * <li><b>Registered Members:</b> Automatically identified. Must only provide
 * the Order Confirmation Code. Security validation ensures the order belongs to
 * the logged-in member.</li>
 * <li><b>Guests:</b> Must provide the Order Confirmation Code AND the
 * identification detail (Phone Number or Email) used during booking to verify
 * ownership.</li>
 * </ul>
 * </p>
 */
public class CancelOrderController implements Initializable {

	@FXML
	private Label lblTitle;
	@FXML
	private Label lblInstruction;

	/** Input field for the unique Confirmation Code received upon booking. */
	@FXML
	private TextField txtConfirmationCode;
	@FXML
	private Label lblError;
	/** Input field for Guest identification (Phone or Email). */
	@FXML
	private Button btnAction;
	@FXML
	private TextField txtIdentification;
	/** Container for the identification field. Hidden for logged-in members. */
	@FXML
	private VBox vboxIdent;

	/**
	 * Initializes the controller class.
	 * <p>
	 * Sets up the UI components based on the currently logged-in user. If a Member
	 * is logged in, the additional identification field is hidden because the
	 * system already knows their identity. If a Guest is using the system, the
	 * identification field is shown to allow verification.
	 * </p>
	 *
	 * @param location  The location used to resolve relative paths for the root
	 *                  object.
	 * @param resources The resources used to localize the root object.
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		lblTitle.setText("Cancel Order");
		btnAction.setText("Cancel Order");

		User user = ChatClient.user;

		// Adjust UI based on Role
		if (user != null && user.getRole() == Role.MEMBER) {
			// Member Mode: Hide identification field
			vboxIdent.setVisible(false);
			vboxIdent.setManaged(false);
			lblInstruction.setText("Enter Confirmation Code:");
		} else {
			// Guest Mode: Show identification field
			vboxIdent.setVisible(true);
			vboxIdent.setManaged(true);
			lblInstruction.setText("Enter Code & Identification:");
		}
	}

	/**
	 * Handles the "Cancel Order" button click event.
	 * <p>
	 * The process flow is as follows: 1. Validates that input fields are not empty.
	 * 2. Sends a request to the server to fetch the order details by Confirmation
	 * Code. 3. Performs a security check (Authorization) to ensure the user owns
	 * the order. 4. If authorized, sends a cancellation request to the server.
	 * </p>
	 *
	 * @param event The event triggered by clicking the button.
	 */
	@FXML
	public void clickCancel(ActionEvent event) {
		lblError.setText("");
		// field for the confirmation code
		String codeInput = txtConfirmationCode.getText().trim();
		String identInput = txtIdentification.getText().trim();

		// Basic Validation
		if (codeInput.isEmpty()) {
			lblError.setText("Please enter the confirmation code.");
			return;
		}
		// Guest Validation: If user is Guest (null) AND identification field is empty -> Error
		if (ChatClient.user == null && identInput.isEmpty()) {
			lblError.setText("Please enter Phone OR Email used for booking.");
			return;
		}
		
		try {
			int confirmationCode = Integer.parseInt(codeInput);

			// 2. Fetch Order Details from Server
			BistroMessage fetchMsg = new BistroMessage(ActionType.GET_ORDER_BY_CODE, confirmationCode);
			ClientUI.chat.accept(fetchMsg);

			Order targetOrder = ChatClient.order; // Assuming server updates this static field

			if (targetOrder == null) {
				lblError.setText("Order not found.");
				return;
			}
			// Security/Authorization Check
			if (!isUserAuthorized(targetOrder, identInput)) {
				lblError.setText("Identification failed! Details do not match.");
				return;
			}
			// Execute Cancellation
			BistroMessage cancelMsg = new BistroMessage(ActionType.CANCEL_ORDER, targetOrder.getConfirmationCode());
			ClientUI.chat.accept(cancelMsg);
			if (ChatClient.operationSuccess) {
				showSuccessAlert(targetOrder.getConfirmationCode());
				clickBack(event);
			} else {
				lblError.setText("Cancellation failed. Please try again.");
			}
		} catch (NumberFormatException e) {
			lblError.setText("Invalid code format. Numbers only.");
		} catch (Exception e) {
			e.printStackTrace();
			lblError.setText("System error.");
		}
	}

	/**
	 * Verifies if the current user is authorized to cancel the specific order.
	 * <p>
	 * <b>Logic:</b>
	 * <ul>
	 * <li>If User is a <b>Member</b>: The Order's memberID must match the User's
	 * ID.</li>
	 * <li>If User is a <b>Guest</b>: The provided input (Phone or Email) must match
	 * the contact details stored in the order.</li>
	 * </ul>
	 * </p>
	 *
	 * @param order          The order object fetched from the server.
	 * @param identification The phone number or email entered by the user (can be
	 *                       null for members).
	 * @return {@code true} if authorized, {@code false} otherwise.
	 */
	private boolean isUserAuthorized(Order order, String identification) {
		User currentUser = ChatClient.user;

		// Case A: Logged-in Member
		if (currentUser != null && currentUser.getRole() == Role.MEMBER) {
			return order.getMemberId() == currentUser.getUserId();
		}

		// Case B: Guest
		// Check if the input matches either the phone OR the email in the order
		if (identification != null) {
			boolean matchPhone = order.getPhone() != null && identification.equals(order.getPhone());
			boolean matchEmail = order.getEmail() != null && identification.equalsIgnoreCase(order.getEmail());

			return matchPhone || matchEmail;
		}

		return false;
	}

	/**
	 * Displays a success information alert.
	 *
	 * @param orderId The ID of the cancelled order.
	 */
	private void showSuccessAlert(int orderId) {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("Success");
		alert.setHeaderText(null);
		alert.setContentText("Order #" + orderId + " has been cancelled successfully.");
		alert.showAndWait();
	}

	/**
	 * Navigates back to the main User Menu.
	 * 
	 * @param event The event triggered by the back button.
	 */
	public void clickBack(ActionEvent event) {
		try {
			((Node) event.getSource()).getScene().getWindow().hide();
			Stage primaryStage = new Stage();
			UserMenuController menu = new UserMenuController();
			menu.start(primaryStage);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}