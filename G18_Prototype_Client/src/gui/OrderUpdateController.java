package gui;

import java.sql.Date;
import client.ClientUI;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import logic.ActionType;
import logic.BistroMessage;
import logic.Order;
import logic.ScreenMode;

public class OrderUpdateController {

    @FXML private TextField txtOrderNum;
    @FXML private TextField txtDate;
    @FXML private TextField txtGuests;
    @FXML private TextField txtSubscriber;
    @FXML private TextField txtConfirmationCode; 
    @FXML private TextField txtDateOfPlacingOrder;
    @FXML private Button btnSave;
    @FXML private Label lblMessage;

    private Order currentOrder;

    /**
     * Loads ALL order details into the text fields.
     * System fields are permanently locked.
     * Editable fields are locked/unlocked based on the User Mode.
     */
    public void loadOrder(Order order) {
        this.currentOrder = order;

        //Populate data
        txtOrderNum.setText(String.valueOf(order.getOrderNumber()));
        txtDate.setText(order.getOrderDate().toString());
        txtGuests.setText(String.valueOf(order.getNumberOfGuests()));
        txtConfirmationCode.setText(String.valueOf(order.getConfirmationCode()));
        txtSubscriber.setText(String.valueOf(order.getSubscriberID()));
        txtDateOfPlacingOrder.setText(order.getDateOfPlacingOrder().toString());

        //Lock constant fields
        txtOrderNum.setEditable(false);
        txtSubscriber.setEditable(false);
        txtConfirmationCode.setEditable(false);
        txtDateOfPlacingOrder.setEditable(false);
        
        // 3. Manage permissions based on Mode (View vs Update)
        if (ClientUI.currentMode == ScreenMode.VIEW) {
            // View Mode: Lock everything and hide save button
            txtDate.setEditable(false);
            txtGuests.setEditable(false);
            btnSave.setVisible(false); 
        } 
        else {
            // Update Mode: Allow editing of specific fields
            txtDate.setEditable(true);
            txtGuests.setEditable(true);
            btnSave.setVisible(true);
        }
    }

    /**
     * Saves the changes made to the order.
     */
    public void saveChanges(ActionEvent event) {
        // Clear previous styles and text
        lblMessage.setText(""); 
        lblMessage.getStyleClass().removeAll("success", "error"); 

        try {
            // Validate and Parse input
            String dateStr = txtDate.getText();
            int guests = Integer.parseInt(txtGuests.getText());
            
            // Convert String to SQL Date
            Date sqlDate = Date.valueOf(dateStr); 
            
            // Update the Logic Object
            currentOrder.setOrderDate(sqlDate);
            currentOrder.setNumberOfGuests(guests);
            
            // Create message and send to server
            BistroMessage msg = new BistroMessage(ActionType.UPDATE_ORDER, currentOrder);
            ClientUI.chat.accept(msg);

            // Show Success Message (Apply CSS class 'success')
            lblMessage.setText("Order updated successfully!");
            lblMessage.getStyleClass().add("success");
            
        } catch (IllegalArgumentException e) {
            // Date format error
            lblMessage.setText("Error: Date must be YYYY-MM-DD");
            lblMessage.getStyleClass().add("error");
            
        } catch (Exception e) {
            // General error
            lblMessage.setText("Error: Invalid input");
            lblMessage.getStyleClass().add("error");
            e.printStackTrace();
        }
    }

    /**
     * Closes the current window and returns to the main menu.
     */
    public void closeWindow(ActionEvent event) {
        ((Node)event.getSource()).getScene().getWindow().hide();
        try {
            Stage primaryStage = new Stage();
            UserMenuController menu = new UserMenuController();
            menu.start(primaryStage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}