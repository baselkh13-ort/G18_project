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
import javafx.scene.control.ListView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.net.InetAddress;
import server.ServerUI;

/**
 * Controller class for the Server Port Configuration screen.
 *
 * Software Structure:
 * This class works as the Controller in the architecture. It connects the graphical view (FXML)
 * to the logical part of the server (ServerUI). It handles user events like button clicks.
 *
 * UI Components:
 * The class manages the following interface elements:
 * - Buttons: Done (start), Exit, and Reset.
 * - Inputs: Text field for the database password.
 * - Displays: Text area for logs and a List view for connected clients.
 *
 * @author Dana Zablev
 * @version 1.0
 */
public class ServerPortFrameController implements ChatIF {

    /** Button to close and exit the application. */
    @FXML private Button btnExit;

    /** Button to confirm the password and start the server. */
    @FXML private Button btnDone;

    /** Input field for the user to enter the database password. */
    @FXML private TextField passtxt;

    /** Text area that shows system logs and status messages. */
    @FXML private TextArea logArea;

    /** Button to reset the form fields if the connection fails. */
    @FXML private Button btnReset;

    /** List view that displays the information of connected clients. */
    @FXML private ListView<String> listClients;

    /** A list that holds the client data strings for the GUI. */
    private ObservableList<String> clientListItems = FXCollections.observableArrayList();
    
    /**
     * Initializes the controller class.
     * This method runs automatically after the FXML file is loaded.
     * It sets up the screen, hides the reset button, and shows the local IP address.
     */
    @FXML
    public void initialize() {
        if (btnReset != null) {
            btnReset.setVisible(false);
        }
        try {
            InetAddress ip = InetAddress.getLocalHost();
            String myIp = ip.getHostAddress();
            logArea.appendText("Server Console Initialized.\n");
            logArea.appendText("Your IP Address: " + myIp + "\n");
         
        } catch (Exception e) {
            logArea.appendText("Error: Could not get IP address.\n");
        }
        listClients.setItems(clientListItems);
    }

    /**
     * Handles the click event on the Done button.
     * It checks if the password field is not empty and tries to start the server.
     *
     * @param event The event triggered by clicking the button.
     */
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
        
    /**
     * Resets the screen controls so the user can try to connect again.
     * It clears the password field and enables the Done button.
     *
     * @param event The event triggered by clicking the reset button.
     */
    public void resetControls(ActionEvent event) {
            System.out.println("Reset Button Pressed");
            btnDone.setVisible(true);
            btnDone.setDisable(false);
            passtxt.setDisable(false);
            passtxt.clear();
            
            if(btnReset != null) btnReset.setVisible(false);
            
            display("Please try again ");
    }

    /**
     * Displays a message in the log area.
     * This method is an implementation of the ChatIF interface.
     *
     * @param message The string message to display.
     */
    @Override
    public void display(String message) {
        appendToLog(message);
    }

    /**
     * A helper method to add text to the log area safely.
     * It uses Platform.runLater to update the UI from any thread.
     *
     * @param msg The message to add to the log.
     */
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

    /**
     * Loads the FXML file and starts the main application window.
     *
     * @param primaryStage The main stage for the application.
     * @throws Exception If the FXML file cannot be found or loaded.
     */
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/gui/ServerPort.fxml"));
        Scene scene = new Scene(root);

        primaryStage.setTitle("Bistro Server Console");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Handles the click event on the Exit button.
     * Closes the program.
     *
     * @param event The event triggered by clicking the button.
     */
    public void getExitBtn(ActionEvent event) {
        display("Exit Server");
        System.exit(0);
    }

    /**
     * Updates the list of connected clients in the user interface.
     * It removes the old entry for a client and adds the new status.
     *
     * @param ip     The IP address of the client.
     * @param host   The host name of the client.
     * @param status The current connection status (Connected/Disconnected).
     */
    public void updateClientList(String ip, String host, String status) {
        Platform.runLater(() -> {
            clientListItems.removeIf(item -> item.contains(ip));

            String clientInfo = String.format("IP: %s (%s) - Status: %s", ip, host, status);

            clientListItems.add(clientInfo);
        });
    }
}