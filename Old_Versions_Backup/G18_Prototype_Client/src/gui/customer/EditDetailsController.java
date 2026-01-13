package gui.customer;

import java.net.URL;
import java.util.ResourceBundle;

import client.ChatClient;
import client.ClientUI;
import common.ActionType;
import common.BistroMessage;
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
import javafx.stage.Stage;

/**
 * Controller for the "Edit Profile Details" screen.
 * <p>
 * This controller allows <b>Member</b> users to view their personal details and update
 * specific contact information (Email and Phone Number).
 * </p>
 * <p>
 * <b>Business Rules:</b>
 * <ul>
 * <li><b>Read-Only Fields:</b> First Name, Last Name, Member ID (cannot be changed by the user).</li>
 * <li><b>Editable Fields:</b> Email, Phone Number.</li>
 * <li><b>Validation:</b> Fields cannot be left empty.</li>
 * </ul>
 * </p>
 */
public class EditDetailsController implements Initializable {

    //FXML UI Components
    @FXML
    private TextField txtFirstName;
    @FXML
    private TextField txtLastName;
    @FXML
    private TextField txtMemberCode; // Unique ID, must be read-only
    @FXML
    private TextField txtPhone;
    @FXML
    private TextField txtEmail;
    @FXML
    private Label lblMessage; // Used for success/error feedback
    @FXML
    private Button btnSave;
    @FXML
    private Button btnBack;
    
    /** The currently logged-in member */
    private User currentUser;

    /**
     * Initializes the controller class.
     * This method is automatically called after the FXML file has been loaded.
     * It retrieves the logged-in user from ChatClient and populates the form fields.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        lblMessage.setText(""); // Clear previous messages
        this.currentUser = ChatClient.user;

        // Safety check: ensure a user is actually logged in
        if (currentUser == null) {
            showAlert("Error", "No user logged in found.");
            btnSave.setDisable(true);
            return;
        }

        populateFields();
    }
    
    /**
     * Populates the GUI text fields with data from the User object.
     */
    private void populateFields() {
        // Read-Only Data
        txtFirstName.setText(currentUser.getFirstName());
        txtLastName.setText(currentUser.getLastName());
        
        // Using String.valueOf to convert integer ID to String.
        txtMemberCode.setText(String.valueOf(currentUser.getMemberCode())); 

        // Editable Data
        txtPhone.setText(currentUser.getPhone());
        txtEmail.setText(currentUser.getEmail());
    }

    /**
     * Handles the "Save Changes" button click.
     * <p>
     * Validates input, updates the User object, and sends a request to the server
     * to persist changes in the database.
     * </p>
     *
     * @param event The ActionEvent triggered by the button.
     */
    @FXML
    public void clickSave(ActionEvent event) {
        lblMessage.setText(""); // Reset message

        String newPhone = txtPhone.getText().trim();
        String newEmail = txtEmail.getText().trim();

        // 1. Input Validation
        if (newPhone.isEmpty() || newEmail.isEmpty()) {
            showAlert("Validation Error", "Phone number and Email cannot be empty.");
            return;
        }

     // 2. Prepare Data for Update
        // We create a copy or update the existing user object to send to the server.
        // NOTE: Ensure your User class has a constructor or setters for this.
        User updatedUser = new User(
                currentUser.getUserId(),   
                currentUser.getUsername(),  
                currentUser.getPassword(),  
                currentUser.getFirstName(), 
                currentUser.getLastName(),  
                currentUser.getRole(),     
                newPhone,               
                newEmail
        );

        // 3. Send Update Request to Server
        try {
            BistroMessage msg = new BistroMessage(ActionType.UPDATE_USER_INFO, updatedUser);
            ClientUI.chat.accept(msg);

            // 4. Handle Success (Assuming optimistic success or waiting for server response)
            // Ideally, you should wait for a confirmation message from the server here.
            
            // Update the global static user to reflect changes immediately in the app
            ChatClient.user = updatedUser; 
            
            lblMessage.setText("Profile updated successfully!");

        } catch (Exception e) {
            e.printStackTrace();
            lblMessage.setText("Connection failed. Try again.");
        }
    }

    /**
     * Handles the "Back" button click.
     * Closes the current window.
     *
     * @param event The ActionEvent triggered by the button.
     */
    @FXML
    public void clickBack(ActionEvent event) {
        try {
            ((Node) event.getSource()).getScene().getWindow().hide();
            // Optional: Re-open the main menu if needed
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper method to display alert dialogs.
     * * @param title   The title of the alert window.
     * @param content The message body of the alert.
     */
    private void showAlert(String title, String content) {
        Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}



































