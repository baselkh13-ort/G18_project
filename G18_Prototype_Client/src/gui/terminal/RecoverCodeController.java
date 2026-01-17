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
import javafx.stage.Stage;

/**
 * Controller for the "Lost Confirmation Code" screen.
 * <p>
 * Implements requirement #45: "If the Confirmation Code is lost... the code
 * will be sent via Email and SMS".
 * </p>
 */
public class RecoverCodeController {

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

        // Handle Response (Simulation)
        if (ChatClient.operationSuccess) {
            lblStatus.setText("Code sent successfully via SMS & Email!");
            lblStatus.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;"); // Green
            btnSend.setDisable(true);
            btnClose.setText("Close");
        } else {
            lblStatus.setText("Error: No active order found for these details.");
            lblStatus.setStyle("-fx-text-fill: #e74c3c;"); // Red
        }
    }

    @FXML
    void closeWindow(ActionEvent event) {
        ((Stage) btnClose.getScene().getWindow()).close();
    }
}