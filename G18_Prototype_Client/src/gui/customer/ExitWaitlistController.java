package gui.customer;

import java.net.URL;
import java.util.ResourceBundle;

import client.ChatClient;
import client.ClientUI;
import common.ActionType;
import common.BistroMessage;
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
 * This class handles the logic for removing a customer from the waiting list.
 * It captures the unique confirmation code from the user, validates that it is a number,
 * sends a cancellation request to the server, and provides feedback based on the response.
 */
public class ExitWaitlistController implements Initializable {

    //FXML Components

    @FXML
    private Label lblTitle;

    @FXML
    private Label lblInstruction;

    /** Input field for the Confirmation Code. */
    @FXML
    private TextField txtIdentification;

    /** Container for the input field. */
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
     * This method is called automatically after the FXML file has been loaded. 
     * It sets up the UI text and ensures the input field is visible for all users,
     * as the server requires a specific confirmation code (Integer) to identify the order.
     *
     * @param location  The location used to resolve relative paths for the root object.
     * @param resources The resources used to localize the root object.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        lblTitle.setText("Exit Waiting List");
        btnExitWaitlist.setText("Confirm Exit");
        lblError.setText("");

        // Make the input field visible to all users (Members and Guests)
        // because the server requires the specific Confirmation Code to identify the order.
        vboxGuest.setVisible(true);
        vboxGuest.setManaged(true);
        
        // Set clear instructions for the user
        lblInstruction.setText("Please enter your Confirmation Code to exit:");
    }

    /**
     * Handles the "Confirm Exit" button click event.
     * This method retrieves the input string, parses it into an Integer, 
     * sends a {@link ActionType#LEAVE_WAITLIST} request with the Integer code to the server.
     * It includes error handling for non-numeric inputs.
     *
     * @param event The event triggered by clicking the confirm button.
     */
    @FXML
    public void clickExitList(ActionEvent event) {
        // Reset error message
        lblError.setText("");
        
        // Step 1: Get the input string
        String inputStr = txtIdentification.getText().trim();
        
        if (inputStr.isEmpty()) {
            lblError.setText("Please enter the Confirmation Code.");
            return;
        }

        // Step 2: Parse the string to an Integer
        Integer codeToSend = null;
        try {
            codeToSend = Integer.parseInt(inputStr);
        } catch (NumberFormatException e) {
            // Handle the case where the user entered non-numeric text
            lblError.setText("Confirmation Code must be a number.");
            return;
        }

        try {
            // Reset client flags before sending the request
            ChatClient.operationSuccess = false; 
            ChatClient.returnMessage = ""; 

            // Step 3: Send the request with the INTEGER object
            BistroMessage msg = new BistroMessage(ActionType.LEAVE_WAITLIST, codeToSend);
            ClientUI.chat.accept(msg);

            // Step 4: Handle the server's response
            if (ChatClient.operationSuccess) {
                // Case: Server processed the request successfully
                showSuccessAlert();
                clickBack(event); // Return to the previous menu
            } else {
                // Case: Server returned an error (Code not found)
                if (ChatClient.returnMessage != null && !ChatClient.returnMessage.isEmpty()) {
                    lblError.setText(ChatClient.returnMessage); 
                } else {
                    lblError.setText("Failed to remove from waitlist.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            lblError.setText("System error. Please try again.");
        }
    }   
    
    /**
     * Displays an information alert indicating success.
     * This is called when the server successfully removes the user from the waiting list.
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