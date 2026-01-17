package gui.terminal;

import java.net.URL;
import java.util.ResourceBundle;

import client.ChatClient;
import client.ClientUI;
import common.ActionType;
import common.BistroMessage;
import common.Order;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Controller for the Payment and Checkout screen running on the Terminal. *
 * This screen allows customers (both Members and Guests) to pay their bill
 * directly from the terminal using their Order Confirmation Code. * Key
 * Features: - Identification via Confirmation Code. - Validation that payment
 * is only allowed for SEATED or BILLED orders. - Automatic 10% discount
 * calculation for identified Members. - Simulation of the payment transaction
 * and table release.
 */
public class TerminalPaymentController extends AbstractTerminalController implements Initializable {

	@FXML
	private VBox vboxIdentification;
	@FXML
	private TextField txtOrderCode;
	@FXML
	private Button btnSearch;

	@FXML
	private VBox vboxBillDetails;
	@FXML
	private Label lblOrderInfo;
	@FXML
	private Label lblTotalAmount;
	@FXML
	private Label lblDiscount;
	@FXML
	private Label lblFinalToPay;
	@FXML
	private Button btnPayNow;
	@FXML
	private Label lblStatus;

	private Order currentOrderToPay = null;

	/**
	 * Initializes the controller class. Sets the initial UI state by hiding the
	 * bill details section. If a member has identified via the main terminal
	 * screen, a personalized greeting is displayed.
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// Reset UI to initial state
		vboxBillDetails.setVisible(false);
		vboxBillDetails.setManaged(false);

		if (ChatClient.terminalMember != null) {
			lblStatus.setText("Welcome " + ChatClient.terminalMember.getFirstName() + ". Enter code to pay:");
		} else {
			lblStatus.setText("Please enter your Confirmation Code to pay:");
		}
	}

	/**
	 * Searches for the order by the code entered in the TextField. Performs basic
	 * validation to ensure the input is numeric before triggering the server fetch.
	 * * @param event The action event triggered by the Search button.
	 */
	@FXML
	void searchOrder(ActionEvent event) {
		lblStatus.setText("");
		String codeStr = txtOrderCode.getText().trim();

		if (codeStr.isEmpty()) {
			setStatus(lblStatus, "Please enter a confirmation code.", false);
			return;
		}

		try {
			int code = Integer.parseInt(codeStr);
			fetchBillDetails(code);
		} catch (NumberFormatException e) {
			setStatus(lblStatus, "Code must be numbers only.", false);
		}
	}

	/**
	 * Sends a request to the server to fetch order details. Uses a synchronized
	 * wait loop to pause execution until the server responds with the Order object.
	 * * @param confirmationCode The unique code of the order to fetch.
	 */
	private void fetchBillDetails(int confirmationCode) {
		// Reset client data to ensure fresh results
		ChatClient.order = null;
		ChatClient.returnMessage = null;

		// Request order from server
		ClientUI.chat.accept(new BistroMessage(ActionType.GET_ORDER_BY_CODE, confirmationCode));

		// Synchronized wait for response (Max 2 seconds)
		int attempts = 0;
		while (ChatClient.order == null && ChatClient.returnMessage == null && attempts < 20) {
			try {
				Thread.sleep(100);
				attempts++;
			} catch (Exception e) {
			}
		}

		if (ChatClient.order == null) {
			setStatus(lblStatus, "Order not found or invalid code.", false);
			vboxBillDetails.setVisible(false);
		} else {
			currentOrderToPay = ChatClient.order;
			validateAndShowBill();
		}
	}

	/**
	 * Validates if the order status allows payment and displays the bill. *
	 * Business Rule: Payment is strictly allowed only if the order status is SEATED
	 * or BILLED. If the order is already PAID or waiting, an appropriate error is
	 * shown.
	 */
	private void validateAndShowBill() {
		String status = currentOrderToPay.getStatus();

		// Using equalsIgnoreCase to prevent errors if server sends "Billed" vs "BILLED"
		if ("SEATED".equalsIgnoreCase(status) || "BILLED".equalsIgnoreCase(status)) {
			showBillCalculation();
		} else {
			vboxBillDetails.setVisible(false);
			if ("COMPLETED".equalsIgnoreCase(status) || "PAID".equalsIgnoreCase(status)) {
				setStatus(lblStatus, "This bill has already been settled.", false);
			} else {
				setStatus(lblStatus, "Cannot pay now. Current status: " + status, false);
			}
		}
	}

	/**
	 * Calculates the final amount and updates the UI. * Logic: Checks if the user
	 * operating the terminal is a recognized Member. [cite_start]If yes, applies a
	 * 10% discount to the total price[cite: 51].
	 */
	private void showBillCalculation() {
		vboxBillDetails.setVisible(true);
		vboxBillDetails.setManaged(true);

		double finalAmount = currentOrderToPay.getTotalPrice();

		// Apply 10% discount ONLY if a member is logged into the terminal
		if (ChatClient.terminalMember != null) {
			lblDiscount.setText("Member Discount (10%): -" + String.format("%.2f", finalAmount * 0.10) + " NIS");
			lblDiscount.setStyle("-fx-text-fill: #2ecc71;");
			// Adjust final price for display if logic requires showing discounted total
			finalAmount = finalAmount * 0.90;
		} else {
			lblDiscount.setText("No Discount Applied");
			lblDiscount.setStyle("-fx-text-fill: #7f8c8d;");
		}

		lblOrderInfo.setText("Order #" + currentOrderToPay.getConfirmationCode() + " | Table: "
				+ currentOrderToPay.getAssignedTableId());
		lblTotalAmount.setText("Subtotal: " + String.format("%.2f", currentOrderToPay.getTotalPrice()) + " NIS");
		lblFinalToPay.setText("TOTAL: " + String.format("%.2f", finalAmount) + " NIS");
	}

	/**
	 * Executes the payment transaction. Sends a PAY_BILL request to the server and
	 * waits for confirmation. On success, it simulates freeing the table and closes
	 * the window. * @param event The action event triggered by the Pay button.
	 */
	@FXML
	void performPayment(ActionEvent event) {
		if (currentOrderToPay == null)
			return;

		// Reset flag before request
		ChatClient.operationSuccess = false;

		// Send payment action to server
		ClientUI.chat.accept(new BistroMessage(ActionType.PAY_BILL, currentOrderToPay.getConfirmationCode()));

		// Synchronized wait for response
		int attempts = 0;
		// Wait until success flag turns true OR timeout
		while (!ChatClient.operationSuccess && attempts < 20) {
			try {
				Thread.sleep(100);
				attempts++;
			} catch (Exception e) {
			}
		}

		if (ChatClient.operationSuccess) {
			setStatus(lblStatus, "Payment Successful! Table is now free.", true);
			btnPayNow.setDisable(true);
			txtOrderCode.setDisable(true);

			// Auto-close after 2 seconds
			new Thread(() -> {
				try {
					Thread.sleep(2000);
				} catch (Exception e) {
				}
				Platform.runLater(() -> super.closeWindow(lblStatus));
			}).start();
		} else {
			setStatus(lblStatus, "Payment failed. Please try again.", false);
		}
	}

}