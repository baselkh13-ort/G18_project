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
 * Controller for the "Lost Confirmation Code" screen.
 * Implements requirement #45: "If the Confirmation Code is lost... the code
 * will be sent via Email and SMS".
 */
public class RecoverCodeController extends AbstractTerminalController  {

    @FXML private VBox vboxInput;
    @FXML private TextField txtIdentifier; // Phone or Email
    @FXML private Label lblStatus;
    @FXML private Button btnSend;
    @FXML private Button btnClose;

    /**
     * Initializes the controller.
     * Checks if we already know the user (Member).
     */
    @FXML
    public void initialize() {
        if (ChatClient.terminalMember != null) {
            // Identified Member: Auto-fill their phone/email
            txtIdentifier.setText(ChatClient.terminalMember.getPhone());
            lblStatus.setText("Hello " + ChatClient.terminalMember.getFirstName() + ", click 'Send' to receive your code.");
            
        } else {
            // Guest: Ask for details
            lblStatus.setText("Enter the Phone Number or Email used for booking:");
        }
    }

    /**
     * Sends the restore request to the server.
     * Action: RESTORE_CODE
     */
    @FXML
    void sendRecoveryRequest(ActionEvent event) {
        String input = txtIdentifier.getText().trim();

        if (input.isEmpty()) {
            lblStatus.setText("Error: Please enter Phone or Email.");
            lblStatus.setStyle("-fx-text-fill: #e74c3c;"); // Red
            return;
        }

        System.out.println("Terminal: Requesting code recovery for: " + input);

        // Send to Server
        ClientUI.chat.accept(new BistroMessage(ActionType.RESTORE_CODE, input));

     // 5. Handle Response
        if (ChatClient.order != null) {
            // Success: Extract the code from the returned Order object            
            setStatus(lblStatus,"Success! Code sent to your Email/SMS.", true);
            
            // UI Cleanup
            btnSend.setDisable(true);
            txtIdentifier.setDisable(true);
            btnClose.setText("Done");
            
        } else {
            // Failure: Server couldn't find an order or returned an error message
            String errorMsg = (ChatClient.returnMessage != null) ? ChatClient.returnMessage : "No active order found for these details.";
            setStatus(lblStatus,"Error: " + errorMsg, false);
        }
    }


}