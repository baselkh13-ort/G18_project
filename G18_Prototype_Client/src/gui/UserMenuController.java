package gui;

import java.io.IOException;

import client.ClientUI;
import common.ActionType;
import common.BistroMessage;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import logic.ScreenMode;

public class UserMenuController {

    /**
     * Button: View All Orders
     * Action: Sends a request to get all orders and switches to the Table View.
     * Sets the mode to "VIEW" (Read-Only).
     */
    @FXML
    public void clickViewAll(ActionEvent event) throws Exception {
        System.out.println("Selected: View All");
        
        // This ensures that if we open an order details screen later, it will be locked.
        ClientUI.currentMode = ScreenMode.VIEW; 

        // 1. Send request to server
        BistroMessage msg = new BistroMessage(ActionType.GET_ALL_ORDERS, null); 
        ClientUI.chat.accept(msg);
        
        // 2. Open the Order List screen (Table)
        ((Node)event.getSource()).getScene().getWindow().hide(); // Close Menu
        
        Stage primaryStage = new Stage();
        Parent root = FXMLLoader.load(getClass().getResource("/gui/OrderList.fxml"));
        Scene scene = new Scene(root);
        
        scene.getStylesheets().add(getClass().getResource("/gui/OrderList.css").toExternalForm());
        
        primaryStage.setTitle("All Orders Table");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    @FXML
    public void clickRegister(ActionEvent event) throws Exception {
        ((Node)event.getSource()).getScene().getWindow().hide();

        Stage stage = new Stage();
        Parent root = FXMLLoader.load(getClass().getResource("/gui/RegisterMember.fxml"));
        Scene scene = new Scene(root);
        stage.setTitle("Register Member");
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    public void clickViewAllMembers(ActionEvent event) {
        System.out.println("Selected: View All Members");

        new Thread(() -> {
            BistroMessage msg = new BistroMessage(ActionType.GET_ALL_MEMBERS, null);
            ClientUI.chat.accept(msg);

            javafx.application.Platform.runLater(() -> {
                try {
                    ((Node) event.getSource()).getScene().getWindow().hide();

                    Stage stage = new Stage();
                    Parent root = FXMLLoader.load(getClass().getResource("/gui/MemberList.fxml"));
                    Scene scene = new Scene(root);
                    stage.setScene(scene);
                    stage.show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }).start();
    }

    /**
     * Button: Update Order
     * Action: Switches to the "Input Order Number" screen.
     * Sets the mode to "UPDATE" (Editable).
     */
    @FXML
    public void clickUpdate(ActionEvent event) throws Exception {
        System.out.println("Selected: Update Order");
        
        // This tells the next screens that editing is allowed.
        ClientUI.currentMode = ScreenMode.UPDATE;
        
        ((Node)event.getSource()).getScene().getWindow().hide(); // Close Menu
        
        Stage primaryStage = new Stage();
        Parent root = FXMLLoader.load(getClass().getResource("/gui/InsertOrderNumber.fxml"));
        Scene scene = new Scene(root);
        
        // Apply CSS
        scene.getStylesheets().add(getClass().getResource("/gui/InsertOrderNumber.css").toExternalForm());
        
        primaryStage.setTitle("Input Order Number");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Button: Exit
     * Action: Sends a quit message to the server and closes the client application.
     */
    @FXML
    public void getExitBtn(ActionEvent event) {
        System.out.println("Exit requested");
        try {
            // Attempt to gracefully disconnect from the server
            if (ClientUI.chat.client != null) {
                BistroMessage msg = new BistroMessage(ActionType.CLIENT_QUIT, null);
                ClientUI.chat.client.sendToServer(msg);
                ClientUI.chat.client.quit(); // Closes connection and calls System.exit(0)
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Entry point to launch the User Menu screen.
     */
    public void start(Stage primaryStage) throws Exception {    
        Parent root = FXMLLoader.load(getClass().getResource("/gui/UserMenu.fxml"));
        Scene scene = new Scene(root);
        
        // Link the CSS file
        scene.getStylesheets().add(getClass().getResource("/gui/UserMenu.css").toExternalForm());
        
        primaryStage.setTitle("Main Menu");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}