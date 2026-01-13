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

/**
 * Controller class for the Server GUI, handling port configuration and logging.
 */
public class ServerPortFrameController implements ChatIF {

    @FXML private Button btnExit;
    @FXML private Button btnDone;
    @FXML private TextField portxt;
    @FXML private TextArea logArea;

    // Helper method to retrieve the port number from the text field
    private String getport() {
        return portxt.getText();
    }

    // Event handler for the "Start" button; validates input and starts the server
    public void Done(ActionEvent event) {
        String p = getport();

        if (p == null || p.trim().isEmpty()) {
            display("You must enter a port number");
            return;
        }

        // Disable controls to prevent double-starting
        btnDone.setDisable(true);
        portxt.setDisable(true);

        display("Starting server on port " + p + "...");

        // Launches the server logic passing the port and this UI for logging
        ServerUI.runServer(p, this);
    }

    /**
     * Automatically called by JavaFX on startup; displays the local IP address.
     */
    @FXML
    public void initialize() {
        try {
            InetAddress ip = InetAddress.getLocalHost();
            String myIp = ip.getHostAddress();
            logArea.appendText("Server Console Initialized.\n");
            logArea.appendText("Your IP Address: " + myIp + "\n");
         
        } catch (Exception e) {
            logArea.appendText("Error: Could not get IP address.\n");
        }
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