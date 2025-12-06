package gui;

import client.ChatClient;
import client.ClientUI;
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

public class InsertOrderNumberController {

    @FXML private TextField txtOrderNum;
    @FXML private Label lblError;

    /**
     * Handles the search action when the user clicks the "Search" button.
     */
    public void searchOrder(ActionEvent event) {
        String orderNumStr = txtOrderNum.getText();
        
        // Clear previous error messages
        lblError.setText(""); 

        // Validation: Check if the field is empty
        if (orderNumStr.trim().isEmpty()) {
            lblError.setText("Please enter an order number.");
            return;
        }

        try {
            // Parse the input string to an integer
            int orderId = Integer.parseInt(orderNumStr); 

            // Create a message and send request to server
            BistroMessage msg = new BistroMessage(ActionType.READ_ORDER, orderId);
            ClientUI.chat.accept(msg);

            // Check if the order was found (ChatClient.order is updated by the server response)
            if (ChatClient.order == null) {
                lblError.setText("Order Not Found!");
            } else {
                // Order found - Transition to the Order Update screen
                ((Node)event.getSource()).getScene().getWindow().hide(); // Hide current window
                
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/OrderUpdate.fxml"));
                Parent root = loader.load();
                
                // Pass the found order to the next controller
                OrderUpdateController formController = loader.getController();
                formController.loadOrder(ChatClient.order);
                
                Stage stage = new Stage();
                Scene scene = new Scene(root);
                // Load CSS if exists
                scene.getStylesheets().add(getClass().getResource("/gui/OrderUpdate.css").toExternalForm());
                
                stage.setTitle("Edit Order Details");
                stage.setScene(scene);
                stage.show();
            }
        } catch (NumberFormatException e) {
            // Error handling: User entered non-numeric characters
            lblError.setText("Invalid input! Enter numbers only.");
        } catch (Exception e) {
            // General error handling
            e.printStackTrace();
            lblError.setText("An error occurred.");
        }
    }
    
    /**
     * Handles the "Back" button click. 
     * Returns the user to the Main Menu.
     */
    public void getBackBtn(ActionEvent event) throws Exception {
        ((Node)event.getSource()).getScene().getWindow().hide();
        
        Stage primaryStage = new Stage();
        UserMenuController menu = new UserMenuController();
        menu.start(primaryStage);
    }
    
    /**
     * Starts and displays the Insert Order Number screen.
     */
    public void start(Stage primaryStage) throws Exception {    
        Parent root = FXMLLoader.load(getClass().getResource("/gui/InsertOrderNumber.fxml"));
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/gui/InsertOrderNumber.css").toExternalForm());
        
        primaryStage.setTitle("Input Order Number");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}