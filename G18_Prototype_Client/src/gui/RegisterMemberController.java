package gui;

import java.util.UUID;

import client.ChatClient;
import client.ClientUI;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import logic.ActionType;
import logic.BistroMessage;
import logic.Member;

/**
 * Handling the registration of new members.
 * 
 * This controller collects user input, performs client side validation, and sends a registration request to the server.
 *
 * The server is validating uniqueness of username, email, and phone number, and for generating the member ID and QR code.
 */

public class RegisterMemberController {

    @FXML private TextField txtUserName, txtFullName, txtPhone, txtEmail;
    @FXML private Label lblStatus;
    
    /**
     * Handles the submit action when the user clicks the "Submit" button.
     */
    @FXML
    public void clickSubmit(ActionEvent event) {
    	// Reading values
        String userName = txtUserName.getText().trim();
        String fullName = txtFullName.getText().trim();
        String phone    = txtPhone.getText().trim();
        String email    = txtEmail.getText().trim();
        
        // Checking validation
        if (userName.isEmpty() || fullName.isEmpty()) {
            lblStatus.setText("Username and Full Name are required.");
            return;
        }
        
        // Checking phone validation
        if (!phone.isEmpty()) {
        	if (phone.charAt(0) != '0' || phone.charAt(1) != '5') {
        		lblStatus.setText("Phone number must start with '05'");
                return;
        	}
        	for (int i = 0; i < phone.length(); i++) {
                if (!Character.isDigit(phone.charAt(i))) {
                    lblStatus.setText("Phone number must contain digits only");
                    return;
                }
            }
        	if (phone.length() != 10) {
        		lblStatus.setText("Your phone number must be 10 digits.");
                return;
        	}
        	
        }
        
        // Checking email validation
        if (!email.isEmpty()) {
        	if (!email.contains("@") || !email.contains(".")) {
        		System.out.println("EMAIL=[" + email + "]");
        		System.out.println("hasAt=" + email.contains("@") + ", hasDot=" + email.contains("."));
        		lblStatus.setText("Email must contain @ and .");
                return;
        	}
        	
        	if (email.contains(" ")) {
        		lblStatus.setText("Email can't contain space.");
                return;
        	}
        	
        	if (email.indexOf("@") > email.lastIndexOf(".")) {
                lblStatus.setText("Invalid email format");
                return;
            }
        }

        // Create member object to server
        Member req = new Member(null, null, userName, fullName, phone, email);
        
        // Sending to server different thread for not blocking the UI
        new Thread(() -> {
            ClientUI.chat.accept(new BistroMessage(ActionType.REGISTER_MEMBER, req));

            Platform.runLater(() -> {
            	// Display result by chat client
                if (ChatClient.registeredMember != null) {
                    lblStatus.setText(
                        "Registered!\nMember ID:\n" +
                        ChatClient.registeredMember.getMemberId()
                    );
                } 
                else if (ChatClient.registerError != null) {
                	lblStatus.setText("❌ " + ChatClient.registerError.replace("ERROR:", "").trim());
                } else {
                	lblStatus.setText("❌ Registration failed (no response).");
                }
            });


        }).start();
    }

    /**
     * Handles the "Back" button click. 
     * Returns the user to the Main Menu.
     */
    @FXML
    public void clickBack(ActionEvent event) throws Exception {
        ((Node)event.getSource()).getScene().getWindow().hide();

        Stage stage = new Stage();
        Parent root = FXMLLoader.load(getClass().getResource("/gui/UserMenu.fxml"));
        stage.setScene(new Scene(root));
        stage.show();
    }
}
