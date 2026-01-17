package gui.terminal;

import client.ChatClient;
import client.ClientUI;
import common.ActionType;
import common.BistroMessage;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Controller for the "Leave Waiting List" screen.
 * This screen allows any customer to remove their entry from the waiting list
 * by providing the unique Confirmation Code generated during registration.
 */
public class TerminalLeaveWaitlistController extends AbstractTerminalController {

    @FXML private VBox vboxIdentification;

    /** TextField for entering the numeric Confirmation Code */
    @FXML private TextField txtIdentification;

    @FXML private Label lblStatus;
    @FXML private Button btnConfirm;
    @FXML private Button btnClose;

    /**
     * Initializes the controller.
     * Sets context-aware instructions for members or guests.
     */
    @FXML
    public void initialize() {
        if (ChatClient.terminalMember != null) {
            lblStatus.setText("Hello " + ChatClient.terminalMember.getFirstName()
                    + ",\nPlease provide the code for the entry you wish to remove.");
        } else {
            lblStatus.setText("Please enter the numeric code provided in your confirmation.");
        }
    }

    /**
     * Processes the request to leave the waiting list.
     * Logic:
     * 1. Validates that the input is not empty and contains only digits.
     * 2. Sends the numeric code to the server via ActionType.LEAVE_WAITLIST.
     * 3. Displays success or error based on server response.
     * @param event The triggered ActionEvent.
     */
    @FXML
    void confirmLeave(ActionEvent event) {
        String codeInput = txtIdentification.getText().trim();

        // 1. Check for empty input
        if (codeInput.isEmpty()) {
            setStatus(lblStatus, "Error: Confirmation code is required.", false);
            return;
        }
        
        Integer codeToSend = null;
        // 2. Validate numeric format (Confirmation codes are integers)
        try {
        	codeToSend = Integer.parseInt(codeInput);
        } catch (NumberFormatException e) {
            setStatus(lblStatus, "Error: Code must consist of numbers only.", false);
            return;
        }

        System.out.println("Terminal: Sending Leave-Waitlist request for Code: " + codeToSend);

        // 3. Communicate with server
        ClientUI.chat.accept(new BistroMessage(ActionType.LEAVE_WAITLIST, codeToSend));
        System.out.println(ChatClient.operationSuccess);
        
        // 4. Handle Response
        if (ChatClient.operationSuccess) {
            setStatus(lblStatus, "Success: You have been removed from the waiting list.", true);
            
            // Lock inputs to prevent double submission
            btnConfirm.setDisable(true);
            txtIdentification.setDisable(true);
            btnClose.setText("Done");
        } else {
            // Display server-side error or generic message
            String errorMsg = (ChatClient.returnMessage != null) ? ChatClient.returnMessage : "Error: Invalid or expired code.";
            setStatus(lblStatus, errorMsg, false);
        }
    }
}