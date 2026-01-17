package gui.terminal;

import client.ChatClient;
import client.ClientUI;
import common.ActionType;
import common.BistroMessage;
import common.Order;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Controller for canceling an order via the Terminal (Kiosk).
 * <p>
 * <b>Requirement Implementation:</b>
 * Implements requirement #32: "The customer can cancel the order at any time...
 * at one of the terminals".
 * </p>
 * <p>
 * <b>Logic Flow:</b>
 * <ul>
 * <li><b>Identified Member:</b> If the user has already identified (card swipe/login) 
 * at the main terminal screen, they only need to enter the Order Code. Verification 
 * is done automatically against their Member ID.</li>
 * * <li><b>Guest / Unidentified:</b> Must enter the Order Code AND a verification detail.
 * According to requirements, a guest provides a Phone Number OR Email during booking.
 * Therefore, they must enter one of these to verify ownership of the order.</li>
 * </ul>
 * </p>
 */
public class TerminalCancelController {

    @FXML private TextField txtOrderCode;
    
    /**
     * Container for the identification input field. 
     * Hidden if the user is already logged in as a member.
     */
    @FXML private VBox vboxIdentification;
    
    /**
     * Input field for Guest verification. 
     * Accepts either Phone Number or Email Address.
     * Note: Ensure fx:id in FXML matches this name.
     */
    @FXML private TextField txtIdentification; 
    
    @FXML private Label lblStatus;
    @FXML private Button btnClose;

    /**
     * Initializes the controller.
     * Detected whether the user is a logged-in member or a guest.
     * Adjusts the UI visibility accordingly.
     */
    @FXML
    public void initialize() {
        // Check if a member scanned their card or logged in at TerminalMain
        if (ChatClient.terminalMember != null) {
            // Case A: Member is identified
            // Hide the identification input because we already know who they are.
            vboxIdentification.setVisible(false);
            vboxIdentification.setManaged(false);
            lblStatus.setText("Hello " + ChatClient.terminalMember.getFirstName() + ", enter Order Code to cancel.");
        } else {
            // Case B: Guest / Unidentified User
            // Show the input field and prompt for Phone OR Email.
            vboxIdentification.setVisible(true);
            vboxIdentification.setManaged(true);
            lblStatus.setText("Enter Order Code and Phone/Email for verification.");
        }
    }

    /**
     * Handles the cancellation request when the user clicks "Cancel Order".
     * <p>
     * <b>Steps:</b>
     * <ol>
     * <li>Validates that inputs are not empty.</li>
     * <li>Fetches the Order object from the server using the Order Code.</li>
     * <li>Verifies authorization (matches Member ID or Phone/Email).</li>
     * <li>Sends the cancellation command to the server.</li>
     * </ol>
     * </p>
     * @param event The button click event.
     */
    @FXML
    void submitCancellation(ActionEvent event) {
        String codeInput = txtOrderCode.getText().trim();
        String identificationInput = txtIdentification.getText().trim();

        // 1. Basic Validation - Order Code is always required
        if (codeInput.isEmpty()) {
            lblStatus.setText("Error: Order Code is required.");
            lblStatus.setStyle("-fx-text-fill: #e74c3c;"); // Red color
            return;
        }

        // If guest (not identified), the identification field is mandatory
        if (ChatClient.terminalMember == null && identificationInput.isEmpty()) {
            lblStatus.setText("Error: Phone or Email is required for verification.");
            lblStatus.setStyle("-fx-text-fill: #e74c3c;");
            return;
        }

        try {
            int orderCode = Integer.parseInt(codeInput);

            // 2. Fetch Order Details from Server
            ClientUI.chat.accept(new BistroMessage(ActionType.GET_ORDER_BY_CODE, orderCode));
            Order targetOrder = ChatClient.order; // The static field updated by ChatClient

            if (targetOrder == null) {
                lblStatus.setText("Error: Order not found.");
                lblStatus.setStyle("-fx-text-fill: #e74c3c;");
                return;
            }

            // 3. Security Check (Authorization)
            // Verify that this order belongs to the person standing at the terminal
            if (!isAuthorized(targetOrder, identificationInput)) {
                lblStatus.setText("Error: Identification failed. Details do not match.");
                lblStatus.setStyle("-fx-text-fill: #e74c3c;");
                return;
            }

            // 4. Send Cancellation Request
            ClientUI.chat.accept(new BistroMessage(ActionType.CANCEL_ORDER, orderCode));
            
            if (ChatClient.operationSuccess) {
                lblStatus.setText("Success! Order #" + orderCode + " cancelled.");
                lblStatus.setStyle("-fx-text-fill: #2ecc71;"); // Green color
                txtOrderCode.setDisable(true);      // Disable input to prevent double submission
                txtIdentification.setDisable(true); 
            } else {
                lblStatus.setText("Error: Could not cancel order (Server Error).");
                lblStatus.setStyle("-fx-text-fill: #e74c3c;");
            }

        } catch (NumberFormatException e) {
            lblStatus.setText("Error: Order Code must be numbers only.");
            lblStatus.setStyle("-fx-text-fill: #e74c3c;");
        } catch (Exception e) {
            e.printStackTrace();
            lblStatus.setText("System Error: " + e.getMessage());
            lblStatus.setStyle("-fx-text-fill: #e74c3c;");
        }
    }

    /**
     * Verifies if the current user is authorized to cancel the specific order.
     * <p>
     * Logic:
     * <ul>
     * <li>If the user is a <b>Member</b>: Checks if the Order's MemberID matches the logged-in UserID.</li>
     * <li>If the user is a <b>Guest</b>: Checks if the input string matches EITHER the Order's phone number OR the Order's email.</li>
     * </ul>
     * </p>
     * * @param order The order object fetched from the server.
     * @param inputIdentifier The string entered by the user (Phone or Email).
     * @return true if the user owns the order, false otherwise.
     */
    private boolean isAuthorized(Order order, String inputIdentifier) {
        // Case A: Identified Member (Logged in / Card Scanned)
        if (ChatClient.terminalMember != null) {
            // Check if the order belongs to this member ID
            return order.getMemberId() == ChatClient.terminalMember.getUserId();
        }

        // Case B: Guest / Unidentified (Manual Entry)
        // Check if the input matches the phone OR the email stored in the order
        if (inputIdentifier != null && !inputIdentifier.isEmpty()) {
            boolean matchPhone = order.getPhone() != null && inputIdentifier.equals(order.getPhone());
            boolean matchEmail = order.getEmail() != null && inputIdentifier.equals(order.getEmail());
            
            return matchPhone || matchEmail;
        }
        
        return false;
    }

    /**
     * Closes the popup window.
     * @param event The action event.
     */
    @FXML
    void closeWindow(ActionEvent event) {
        ((Stage) btnClose.getScene().getWindow()).close();
    }
}