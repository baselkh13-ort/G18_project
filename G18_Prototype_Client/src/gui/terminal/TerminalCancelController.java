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

/**
 * Controller for canceling an order via the Terminal.
 *
 * Requirement Implementation:
 * The customer can cancel the order at any time at one of the terminals.
 *
 * Logic Flow:
 * - Identified Member: If the user has already identified (card swipe/login) 
 * at the main terminal screen, they only need to enter the Order Code. Verification 
 * is done automatically against their Member ID.
 * - Guest / Unidentified: Must enter the Order Code AND a verification detail.
 * According to requirements, a guest provides a Phone Number OR Email during booking.
 * Therefore, they must enter one of these to verify ownership of the order.
 */
public class TerminalCancelController extends AbstractTerminalController {

    @FXML private TextField txtOrderCode;
    
    /**
     * Container for the identification input field. 
     * Hidden if the user is already logged in as a member.
     */
    @FXML private VBox vboxIdentification;
    
    /**
     * Input field for Guest verification. 
     * Accepts either Phone Number or Email Address.
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
     *
     * Steps:
     * 1. Validates that inputs are not empty.
     * 2. Fetches the Order object from the server using the Order Code.
     * 3. Verifies authorization (matches Member ID or Phone/Email).
     * 4. Sends the cancellation command to the server.
     *
     * @param event The button click event.
     */
    @FXML
    void submitCancellation(ActionEvent event) {
        String codeInput = txtOrderCode.getText().trim();
        String identificationInput = txtIdentification.getText().trim();

        // 1. Basic Validation - Order Code is always required
        if (codeInput.isEmpty()) {
            setStatus(lblStatus, "Error: Order Code is required.", false);
            return;
        }

        // If guest (not identified), the identification field is mandatory
        if (ChatClient.terminalMember == null && identificationInput.isEmpty()) {
            setStatus(lblStatus, "Error: Phone or Email is required for verification.", false);
            return;
        }

        try {
            int orderCode = Integer.parseInt(codeInput);

            // 2. Fetch Order Details from Server
            ClientUI.chat.accept(new BistroMessage(ActionType.GET_ORDER_BY_CODE, orderCode));
            Order targetOrder = ChatClient.order; // The static field updated by ChatClient

            if (targetOrder == null) {
                setStatus(lblStatus, "Error: Order can not be cancelled.", false);
                return;
            }

            // 3. Security Check (Authorization)
            // Verify that this order belongs to the person standing at the terminal
            if (!isAuthorized(targetOrder, identificationInput)) {
                setStatus(lblStatus, "Error: Identification failed. Details do not match.", false);
                return;
            }

            // 4. Send Cancellation Request
            ClientUI.chat.accept(new BistroMessage(ActionType.CANCEL_ORDER, orderCode));
            
            if (ChatClient.operationSuccess) {
                setStatus(lblStatus, "Success! Order " + orderCode + " cancelled.", true);
                txtOrderCode.setDisable(true);      // Disable input to prevent double submission
                txtIdentification.setDisable(true); 
            } else {
                setStatus(lblStatus, "Error: Could not cancel order (Server Error).", false);
            }

        } catch (NumberFormatException e) {
            setStatus(lblStatus, "Error: Order Code must be numbers only.", false);
        } catch (Exception e) {
            e.printStackTrace();
            setStatus(lblStatus, "System Error: " + e.getMessage(), false);
        }
    }

    /**
     * Verifies if the current user is authorized to cancel the specific order.
     *
     * Logic:
     * - If the user is a Member: Checks if the Order's MemberID matches the logged-in UserID.
     * - If the user is a Guest: Checks if the input string matches EITHER the Order's phone number OR the Order's email.
     *
     * @param order The order object fetched from the server.
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
}