package gui;

import client.ClientController;
import client.ClientUI;
import gui.common.LoginController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label; // Import Label
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ClientConnectFormController {

    @FXML
    private TextField txtServerIP;


    @FXML
    private Label lblError; 

    public void getConnectBtn(ActionEvent event) {
        String ip = txtServerIP.getText();
        
        // Clear previous error messages
        lblError.setText(""); 
        
        if(ip.trim().isEmpty()) {
            lblError.setText("Please enter an IP address");
            return;
        }

        try {
            //Create the ClientController using the IP entered by the user
            ClientUI.chat = new ClientController(ip, 5555);
            
            // Attempt to establish a connection to the server
            ClientUI.chat.client.openConnection();

            // If we reached here, the connection was successful

            //Hide the current connection window
            ((Node)event.getSource()).getScene().getWindow().hide(); 

            //Open the main application window
            Stage primaryStage = new Stage();
            LoginController aFrame = new LoginController(); 
            aFrame.start(primaryStage);
            
        } catch (Exception e) {
            //Error Handling
            
            // Print error to console for debugging
            System.out.println("Connection Failed");
            
            //Display the error to the user (on screen)
            lblError.setText("Connection failed. Check Server.");
        }
        
    }
    
    public void start(Stage primaryStage) throws Exception {    
        Parent root = FXMLLoader.load(getClass().getResource("/gui/ClientConnectForm.fxml"));
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/gui/ClientConnectForm.css").toExternalForm());
        
        primaryStage.setTitle("Client Connection");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}