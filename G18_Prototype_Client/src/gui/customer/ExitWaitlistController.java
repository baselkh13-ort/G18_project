package gui.customer;

import java.net.URL;
import java.util.ResourceBundle;

import client.ChatClient;
import client.ClientUI;
import common.ActionType;
import common.BistroMessage;
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
 * Controller for the "Exit Waiting List" screen.
 * <p>
 * This class handles the logic for removing a customer from the restaurant's
 * waiting list. It supports two identification methods based on the user's
 * login status:
 * <ul>
 * <li><b>Members:</b> Automatically identified by their member code. No input
 * required.</li>
 * <li><b>Guests:</b> Must manually provide the Phone Number or Email address
 * they used when joining the list.</li>
 * </ul>
 * </p>
 */

public class ExitWaitlistController implements Initializable {

	// UI Components
	@FXML
	private Label lblTitle;
	@FXML
	private Label lblInstruction;
	/** Input field for Guest identification (Phone or Email). */
	@FXML
	private TextField txtIdentification;

	/** Box for the input field (used to hide it form the members). */
	@FXML
	private VBox vboxGuest;

	@FXML
	private Label lblError;
	@FXML
	private Button btnExitWaitlist;
	@FXML
	private Button btnBack;

	/**
	 * Initializes the controller class.
	 * <p>
	 * This method is called automatically after the FXML file has been loaded. It
	 * adapts the UI visibility and instruction text based on whether the current
	 * user is a registered Member or a Guest.
	 * </p>
	 *
	 * @param location  The location used to resolve relative paths for the root
	 *                  object.
	 * @param resources The resources used to localize the root object.
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		lblTitle.setText("Exit Waiting List");
		btnExitWaitlist.setText("Confirm Exit");
		User user = ChatClient.user;

		// Check if the user is a Member
		if (user != null && user.getRole() == Role.MEMBER) {
			// Member View - hide the input field since from the member
			vboxGuest.setVisible(false);
			vboxGuest.setManaged(false);
			lblInstruction.setText("Click confirm to remove yourself from the list.");
		} else {
			// Guest View - show the input field for manual identification.
			vboxGuest.setVisible(true);
			vboxGuest.setManaged(true);

			lblInstruction.setText("Enter the Phone or Email you used to join the list:");
		}
	}

	/**
     * Handles the "Confirm Exit" button click event.
     * <p>
     * Sends a {@link ActionType#LEAVE_WAITLIST} request to the server.
     * The payload sent depends on the user role:
     * <ul>
     * <li><b>Member:</b> Sends the integer member code.</li>
     * <li><b>Guest:</b> Sends the String input (Phone or Email).</li>
     * </ul>
     * </p>
     *
     * @param event The ActionEvent triggered by clicking the button.
     */
    @FXML
    public void clickExitList(ActionEvent event) {
        lblError.setText("");
        User user = ChatClient.user; // Access the static user instance
        Object identificationData = null;

        // Determine the identification data to send
        if (user != null && user.getRole() == Role.MEMBER) {
        	// Member: memberCode
            identificationData = user.getMemberCode();
        } else {
            // Guest: input (String)
            String input = txtIdentification.getText().trim();
            
            if (input.isEmpty()) {
                lblError.setText("Please enter your Phone or Email.");
                return;
            }
            identificationData = input; 
        }
        try {
            //Send request to server
            BistroMessage msg = new BistroMessage(ActionType.LEAVE_WAITLIST, identificationData);
            ClientUI.chat.accept(msg);

            // Check server response (using the boolean flag in ChatClient)
            if (ChatClient.operationSuccess) {
                showSuccessAlert();
                clickBack(event); // Return to main menu on success
            } else {
                lblError.setText("Failed. Details not found in the waiting list.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            lblError.setText("System error. Please try again.");
        }
    }  
    
    /**
     * Displays a success information alert to the customer.
     */
    private void showSuccessAlert() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText("You have been removed from the waiting list.");
        alert.showAndWait();
    }
            
    /**
     * Navigates back to the main User Menu.
     * @param event The event triggered by the back button.
     */
    @FXML
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
