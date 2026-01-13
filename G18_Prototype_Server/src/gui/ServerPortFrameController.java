package gui;

import common.ChatIF;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import java.net.InetAddress;
import server.ServerUI;
import javafx.scene.control.ListView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.Iterator;

/**
 * Controller class for the Server GUI, handling port configuration and logging.
 */
public class ServerPortFrameController implements ChatIF {

    @FXML private Button btnExit;
    @FXML private Button btnDone;
    @FXML private TextField passtxt;
    @FXML private TextArea logArea;
    @FXML private Button btnReset;
    
    @FXML private ListView<String> listClients;
    private ObservableList<String> clientListItems = FXCollections.observableArrayList();
    
    @FXML
    public void initialize() {
    		if (btnReset != null) {
            btnReset.setVisible(false);
        }
    		listClients.setItems(clientListItems);
        try {
            InetAddress ip = InetAddress.getLocalHost();
            String myIp = ip.getHostAddress();
            logArea.appendText("Server Console Initialized.\n");
            logArea.appendText("Your IP Address: " + myIp + "\n");
         
        } catch (Exception e) {
            logArea.appendText("Error: Could not get IP address.\n");
        } 
    }

    // Event handler for the "Start" button; validates input and starts the server
    public void Done(ActionEvent event) {
    	String dbPass = passtxt.getText();

        if (dbPass == null || dbPass.trim().isEmpty()) {
            display("You must enter the DB password");
            return;
        }

        // Disable controls to prevent double-starting
        btnDone.setDisable(true);
        passtxt.setDisable(true);
        if(btnReset != null) btnReset.setVisible(false);

        display("Connecting to DB...");
        display("Starting server on port 5555...");

        boolean success = ServerUI.runServer("5555", dbPass, this);
       
        if (!success) {
        	if(btnReset != null)
        		btnDone.setVisible(false);
        		if(btnReset != null) btnReset.setVisible(true);
            display("Server failed to start. Click Reset to try again.");
        }
    }
        
        public void resetControls(ActionEvent event) {
        		System.out.println("Reset Button Pressed");
        		btnDone.setVisible(true);
        		btnDone.setDisable(false);
        		passtxt.setDisable(false);
            passtxt.clear();
            
            if(btnReset != null) btnReset.setVisible(false);
            
            display("Please try again ");
    }
        public void updateClientList(String ip, String host, String status) {
            Platform.runLater(() -> {
            	Iterator<String> iterator = clientListItems.iterator();
            	while (iterator.hasNext()) {
            	    String row = iterator.next();
            	    if (row.contains(ip)) {
            	        iterator.remove();
            	    }
            	}            	
            	String clientInfo = String.format("IP: %s | Host: %s | Status: %s", ip, host, status);
            	clientListItems.add(clientInfo);
            });
        }    
    

    // Implementation of ChatIF to display messages in the log
    @Override
    public void display(String message) {
        appendToLog(message);
    }

    // Safely updates the text area from any thread using Platform.runLater
    public void appendToLog(String msg) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                if (logArea != null) {
                    logArea.appendText(msg + "\n");
                } else {
                    System.out.println(msg);
                }
            }
        });
    }

    // Loads the FXML file and shows the server window
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/gui/ServerPort.fxml"));
        Scene scene = new Scene(root);

        primaryStage.setTitle("Bistro Server Console");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Closes the application when "Exit" is clicked
    public void getExitBtn(ActionEvent event) {
        display("Exit Server");
        System.exit(0);
    }
}