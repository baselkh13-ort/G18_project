package gui.utils;

import client.ClientController;
import client.ClientUI;
import gui.common.LoginController;
import gui.terminal.TerminalMainController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label; // Import Label
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controller for the initial Client Connection screen.
 * <p>
 * Handles the input of the Server IP address and initiates the connection to
 * the EchoServer.
 * </p>
 */
public class ClientConnectFormController {

	@FXML
	private TextField txtServerIP;
	@FXML
	private Label lblError;
	@FXML
	private Button btnConnect;
	@FXML
	private CheckBox cbIsTerminal;

	/**
	 * Handles the "Connect" button click (or Enter key in the text field).
	 * <p>
	 * Validates the IP input, attempts to connect to the server using
	 * {@link ClientController}, and if successful, navigates to the Login screen.
	 * </p>
	 * * @param event The event triggered by the user action.
	 */
	public void getConnectBtn(ActionEvent event) {
		String ip = txtServerIP.getText();

		// Clear previous error messages
		lblError.setText("");

		if (ip == null || ip.trim().isEmpty()) {
			lblError.setText("Please enter an IP address");
			return;
		}

		if (btnConnect != null) {
			btnConnect.setDisable(true);
			btnConnect.setText("Connecting...");
		}

		try {
			// Create the ClientController using the IP entered by the user
			ClientUI.chat = new ClientController(ip, 5555);

			// Attempt to establish a connection to the server
			ClientUI.chat.client.openConnection();

			// If we reached here, the connection was successful

			// Hide the current connection window
			((Node) event.getSource()).getScene().getWindow().hide();

			// Open the main application window
			Stage primaryStage = new Stage();
			if (cbIsTerminal.isSelected()) {
                // --- OPTION A: Launch Kiosk / Terminal ---
                openTerminalScreen(primaryStage);
            } else {
                // --- OPTION B: Launch Standard Remote Login ---
                LoginController aFrame = new LoginController();
                aFrame.start(primaryStage);
            }

		} catch (Exception e) {
			// Error Handling

			// Print error to console for debugging
			System.out.println("Connection Failed");

			// Display the error to the user (on screen)

			lblError.setText("Connection failed. Check Server.");
			// Re-enable the button so the user can try again
			if (btnConnect != null) {
				btnConnect.setDisable(false);
				btnConnect.setText("Connect");
			}
		}

	}
	
	/**
     * Helper method to launch the Terminal Main Screen
     */
    private void openTerminalScreen(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/terminal/TerminalMain.fxml"));
            Parent root = loader.load();
            
            // Don't forget the controller might need initialization if you have specific startup logic
            // TerminalMainController controller = loader.getController();
            
            Scene scene = new Scene(root);
            
            // Load Terminal CSS
            String cssPath = "/gui/terminal/Terminal.css";
            if (getClass().getResource(cssPath) != null) {
                 scene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
            } else {
                System.out.println("Error: Terminal.css not found!");
            }

            stage.setTitle("Bistro Terminal System");
            stage.setScene(scene);
            stage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to load Terminal Screen.");
        }
    }

	/**
	 * Entry point to launch this specific screen manually. * @param primaryStage
	 * The stage to display the scene on.
	 * 
	 * @throws Exception If FXML loading fails.
	 */
	public void start(Stage primaryStage) throws Exception {
		Parent root = FXMLLoader.load(getClass().getResource("/gui/utils/ClientConnectForm.fxml"));
		Scene scene = new Scene(root);
		scene.getStylesheets().add(getClass().getResource("/gui/utils/ClientConnectForm.css").toExternalForm());

		primaryStage.setTitle("Client Connection");
		primaryStage.setScene(scene);
		primaryStage.show();
	}
}