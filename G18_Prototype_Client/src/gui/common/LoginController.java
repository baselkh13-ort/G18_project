package gui.common;

import java.net.URL;
import java.util.ResourceBundle;

import client.ChatClient;
import client.ClientUI;
import common.ActionType;
import common.BistroMessage;
import common.User;
import gui.UserMenuController;
import gui.staff.WorkerMenuController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controller class for the Login Screen.
 * <p>
 * This class handles the initial user authentication process. It serves as the
 * entry point for the client application GUI.
 * </p>
 * * Key responsibilities:
 * <ul>
 * <li>Validating user input (username and password).</li>
 * <li>Communicating with the server to authenticate credentials.</li>
 * <li>Navigating to the appropriate main menu based on the user's role (Member,
 * Worker, Manager).</li>
 * <li>Providing a "Guest" login option for limited access.</li>
 * </ul>
 */
public class LoginController {

	// FXML UI components
	@FXML
	private TextField txtUsername;
	@FXML
	private PasswordField txtPassword;
	@FXML
	private Label lblError;
	@FXML
	private Button btnLogin;
	@FXML
	private Button btnGuest;

	/**
	 * * Static reference to the current stage, used if external closing is needed.
	 */
	private static Stage currentStage;

	/**
	 * Launches and displays the Login Screen.
	 * <p>
	 * This method loads the FXML file, sets the scene, and displays the primary
	 * stage. It should be called from the main application class (ClientUI).
	 * </p>
	 *
	 * @param primaryStage The main stage (window) of the JavaFX application.
	 * @throws Exception If the FXML file '/gui/common/Login.fxml' cannot be loaded.
	 */
	public void start(Stage primaryStage) throws Exception {
		Parent root = FXMLLoader.load(getClass().getResource("/gui/common/Login.fxml"));
		Scene scene = new Scene(root);

		currentStage = primaryStage; // Save reference for later closing
		primaryStage.setTitle("Bistro System - Login");
		primaryStage.setScene(scene);
		primaryStage.show();
	}

	/**
	 * Handles the "Login" button click event.
	 * <p>
	 * The process involves: 1. Validating that input fields are not empty. 2.
	 * Creating a temporary User object. 3. Sending a login request to the server
	 * via ChatClient. 4. Waiting for the server's response (blocking wait). 5.
	 * Navigating to the main menu if successful, or displaying an error.
	 * </p>
	 *
	 * @param event The ActionEvent triggered by the button click.
	 */
	@FXML
	public void getLoginBtn(ActionEvent event) {
		String username = txtUsername.getText();
		String password = txtPassword.getText();
		// 1. Input Validation
		if (username.trim().isEmpty() || password.trim().isEmpty()) {
			lblError.setText("Please enter username and password!");
			return;
		}
		// Reset the user before new attempt
		ChatClient.user = null;

		// 2. Create a User object for authentication
		User loginUser = new User(0, username, password, null, null, null, null, null);

		// 3. Create Message and Send to Server
		BistroMessage msg = new BistroMessage(ActionType.LOGIN, loginUser);
		System.out.println("LoginController: Sending login request for user: " + username);

		if (ClientUI.chat != null) {
			ClientUI.chat.accept(msg); // Sends message and waits for response
		} else {
			lblError.setText("Error: No connection to server.");
			return;
		}

		// 4.Check Response from the server
		User loggedInUser = ChatClient.user;

		if (loggedInUser != null) {
			System.out.println("Login Successful! Role: " + loggedInUser.getRole());

			((Node) event.getSource()).getScene().getWindow().hide();
			// Navigate to the next screen
			openMainMenuByRole(loggedInUser);

		} else {
			lblError.setText("Invalid username or password.");
		}
	}

	/**
	 * Handles the "Continue as Guest" button click event.
	 * <p>
	 * Bypasses authentication and opens the User Menu in Guest mode. The
	 * ChatClient.user field will remain null.
	 * </p>
	 *
	 * @param event The ActionEvent triggered by the button click.
	 */
	@FXML
	public void continueAsGuest(ActionEvent event) {
		System.out.println("LoginController: Guest login selected - Moving to Main Menu...");
		try {
			// Hide the current Login window
			((Node) event.getSource()).getScene().getWindow().hide();

			// Open the UserMenuController (Main Menu)
			Stage primaryStage = new Stage();
			UserMenuController userMenu = new UserMenuController();
			userMenu.start(primaryStage);

		} catch (Exception e) {
			e.printStackTrace();
			lblError.setText("Error opening Main Menu");
		}

	}

	/**
	 * Handles the "Exit" button click event.
	 * <p>
	 * Sends a quit message to the server to ensure clean disconnection before
	 * closing the application.
	 * </p>
	 *
	 * @param event The ActionEvent triggered by the button click.
	 */
	@FXML
	public void getExitBtn(ActionEvent event) {
		try {
			if (ClientUI.chat != null) {
				BistroMessage msg = new BistroMessage(ActionType.CLIENT_QUIT, null);
				ClientUI.chat.client.sendToServer(msg);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.exit(0);
	}

	/**
	 * Helper method to route the authenticated user to the correct screen based on
	 * their Role.
	 *
	 * @param user The authenticated User object containing the Role.
	 */
	private void openMainMenuByRole(User user) {
		try {
			Stage stage = new Stage();
			switch (user.getRole()) {

			case MEMBER:
				UserMenuController memberScreen = new UserMenuController();
				memberScreen.start(stage);
				break;

			case WORKER:
			case MANAGER:
				WorkerMenuController workerScreen = new WorkerMenuController();
				workerScreen.start(stage);
				break;

			default:
				System.out.println("Error: Unknown Role");
				lblError.setText("Error: User role not recognized.");
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
			lblError.setText("Error opening main menu.");
		}
	}

}
