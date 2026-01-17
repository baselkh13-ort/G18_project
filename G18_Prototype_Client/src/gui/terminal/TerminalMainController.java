package gui.terminal;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import client.ChatClient;
import client.ClientUI;
import common.ActionType;
import common.BistroMessage;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Main Controller for the Terminal Interface.
 * <p>
 * <b>Functionality:</b>
 * <ul>
 * <li>Manages the main navigation menu.</li>
 * <li>Handles User Identification via Card Reader Simulation.</li>
 * <li>Implements a "Session" model: Guests see the login area, Identified
 * Members see their name and a Logout button.</li>
 * </ul>
 * </p>
 */
public class TerminalMainController implements Initializable {

	// Simulation Area (Visible for Guests)
	@FXML
	private VBox vboxSimulationArea;
	@FXML
	private TextField txtSimulateScan;
	@FXML
	private Label lblError; // Separate label for login errors

	// Member Info Area (Visible for Logged-in Members)
	@FXML
	private VBox vboxMemberInfo;
	@FXML
	private Label lblWelcome;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// Ensure the screen starts in a clean state based on current login status
		refreshScreenState();
	}

	/**
	 * Updates the UI visibility based on whether a user is logged in.
	 * <p>
	 * <b>Logic:</b>
	 * <ul>
	 * <li><b>Guest (ChatClient.terminalMember == null):</b> Shows the "Simulate
	 * Scan" area. Hides the Logout button.</li>
	 * <li><b>Member (ChatClient.terminalMember != null):</b> Hides the scan area.
	 * Shows "Welcome [Name]" and Logout button.</li>
	 * </ul>
	 * </p>
	 */
	private void refreshScreenState() {
		if (ChatClient.terminalMember == null) {
			// GUEST MODE
			vboxSimulationArea.setVisible(true);
			vboxSimulationArea.setManaged(true);

			vboxMemberInfo.setVisible(false);
			vboxMemberInfo.setManaged(false);

			txtSimulateScan.clear();
			lblError.setText(""); // Clear previous errors
		} else {
			//MEMBER MODE
			vboxSimulationArea.setVisible(false);
			vboxSimulationArea.setManaged(false);

			vboxMemberInfo.setVisible(true);
			vboxMemberInfo.setManaged(true);

			lblWelcome.setText("Welcome, " + ChatClient.terminalMember.getFirstName() + " " + ChatClient.terminalMember.getLastName());
		}
	}

	/**
	 * Simulation of swiping a magnetic card or scanning a QR code.
	 * <p>
	 * <b>Process:</b> 1. Reads the Member Code from the input field. 2. Sends an
	 * identification request to the server. 3. If successful, updates the static
	 * {@code ChatClient.terminalMember} and refreshes the UI.
	 * </p>
	 * 
	 * @param event The button click event.
	 */
	@FXML
	void simulateScan(ActionEvent event) {
		String inputCode = txtSimulateScan.getText().trim();

		if (inputCode.isEmpty()) {
			lblError.setText("Error: Please enter a Member Code.");
			lblError.setStyle("-fx-text-fill: #e74c3c;"); // Red
			return;
		}

		try {
			System.out.println("Terminal: Simulating card scan for code: " + inputCode);

			// Send identification request to server
			ClientUI.chat.accept(new BistroMessage(ActionType.IDENTIFY_BY_QR, inputCode));

			// Check if login was successful (Server updates terminalMember)
			if (ChatClient.terminalMember != null) {
				// SUCCESS: Switch UI to Member Mode
				refreshScreenState();
			} else {
				// FAILURE: Show error
				lblError.setText("User not found / Not a member.");
				lblError.setStyle("-fx-text-fill: #e74c3c;");
			}

		} catch (NumberFormatException e) {
			lblError.setText("Error: Code must be numeric.");
			lblError.setStyle("-fx-text-fill: #e74c3c;");
		}
	}

	/**
	 * Logs out the current member.
	 * <p>
	 * <b>Requirement #11 Implementation:</b> Allows switching users without closing
	 * the application.
	 * </p>
	 * 
	 * @param event The button click event.
	 */
	@FXML
	void performLogout(ActionEvent event) {
		System.out.println("Terminal: User requested Logout.");

		ClientUI.chat.accept(new BistroMessage(ActionType.LOGOUT,ChatClient.terminalMember.getUserId() ));

		//Clear local session data
		ChatClient.terminalMember = null;
		ChatClient.relevantOrders.clear(); // Clear any loaded orders

		//Reset UI to Guest Mode
		refreshScreenState();
	}

	// Navigation Methods

	@FXML
	void openGetTable(ActionEvent event) {
		openPopup("GetTable.fxml", "Get Table (Check-in)");
	}

	@FXML
	void openWaitingList(ActionEvent event) {
		openPopup("EnterWaitingList.fxml", "Join Waiting List");
	}

	@FXML
	void openPayment(ActionEvent event) {
		openPopup("TerminalPayment.fxml", "Pay Bill & Check-out");
	}

	@FXML
	void cancelOrder(ActionEvent event) {
		openPopup("TerminalCancel.fxml", "Cancel Reservation");
	}

	@FXML
	void leaveWaitingList(ActionEvent event) {
		openPopup("TerminalLeaveWaitlist.fxml", "Leave Waiting List");
	}

	@FXML
	void recoverLostCode(ActionEvent event) {
		openPopup("RecoverCode.fxml", "Recover Lost Code");
	}

	/**
	 * Helper method to open modal popup windows.
	 * <p>
	 * Keeps the main terminal window open in the background but blocks interaction
	 * until the popup is closed.
	 * </p>
	 * 
	 * @param fxmlName The filename of the FXML to load.
	 * @param title    The title of the popup window.
	 */
	private void openPopup(String fxmlName, String title) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlName));
			Parent root = loader.load();
			Stage stage = new Stage();
			stage.setTitle(title);
			stage.setScene(new Scene(root));

			// Modal window: blocks the parent window until closed
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.showAndWait();

			// Refresh state when returning
			refreshScreenState();

		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Error loading screen: " + fxmlName);
		}
	}

	/**
	 * Exits the entire application. Sends a disconnect signal to the server before
	 * closing.
	 */
	@FXML
	void exitTerminal(ActionEvent event) {
		System.out.println("Exit requested.");
		try {
			if (ClientUI.chat != null && ClientUI.chat.client != null && ClientUI.chat.client.isConnected()) {

				// 1. Notify server logic to update database/lists
				BistroMessage msg = new BistroMessage(ActionType.CLIENT_QUIT, null);
				ClientUI.chat.client.sendToServer(msg);

				// 2. Network: Physically disconnect the socket
				ClientUI.chat.client.quit();
			}
		} catch (Exception e) {
			System.out.println("Error during disconnect: " + e.getMessage());
		} finally {
			System.out.println("Closing application now.");
			System.exit(0);
		}
	}
}