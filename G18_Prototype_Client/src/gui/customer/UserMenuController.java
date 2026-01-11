package gui.customer;

import java.net.URL;
import java.util.ResourceBundle;

import client.ChatClient;
import client.ClientUI;
import common.ActionType;
import common.BistroMessage;
import common.Role;
import common.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import logic.ScreenMode;

/**
 * UserMenuController controls the main navigation menu for the Bistro
 * application.
 * <p>
 * This controller handles the dashboard presented to the member after a
 * successful login (or guest entry). It adjusts the visibility of buttons based
 * on the user's role (Member vs. Guest).
 * </p>
 */
public class UserMenuController implements Initializable {

	/** Label to display a welcome message with the user's name or "Guest". */
	@FXML
	private Label lblWelcome;

	// visible only for a member
	@FXML
	private Button btnDigitalCard;
	@FXML
	private Button btnExit;

	// visible for members and guests
	@FXML
	private Button btnNewOrder;
	@FXML
	private Button btnCancelOrder;

	/**
	 * Initializes the controller class. This method is automatically called after
	 * the FXML file has been loaded.
	 * <p>
	 * It checks the currently logged-in user's role via {@link ChatClient#user}. If
	 * the user is a Guest (or null), it hides member-specific features (Update
	 * Order, Digital Card).
	 * </p>
	 *
	 * @param location  The location used to resolve relative paths for the root
	 *                  object, or null if unknown.
	 * @param resources The resources used to localize the root object, or null if
	 *                  not found.
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		User user = ChatClient.user;
		if (lblWelcome == null) {
			return;
		}
		
		if (user == null || user.getRole() == Role.GUEST) {
			// GUEST VIEW
			lblWelcome.setText("Welcome, Guest");
			// Hide member-exclusive buttons
			btnDigitalCard.setVisible(false);
			btnDigitalCard.setManaged(false);
			}
		
		else {
			lblWelcome.setText("Welcome," + user.getFirstName() + " " + user.getLastName());
		}
	}

	/**
	 * Entry point to launch the User Menu screen manually (if needed). * @param
	 * primaryStage The primary stage for this view.
	 * 
	 * @throws Exception If loading the FXML fails.
	 */
	public void start(Stage primaryStage) throws Exception {
		Parent root = FXMLLoader.load(getClass().getResource("/gui/customer/UserMenu.fxml"));
		Scene scene = new Scene(root);

		// Link the CSS file
		scene.getStylesheets().add(getClass().getResource("/gui/customer/UserMenu.css").toExternalForm());

		primaryStage.setTitle("Main Menu");
		primaryStage.setScene(scene);
		primaryStage.show();
	}

	/**
	 * Handles the "Book a Table" action.
	 * <p>
	 * Sets the global screen mode to {@link ScreenMode#CREATE} and navigates to the
	 * Order Creation screen.
	 * </p>
	 *
	 * @param event The ActionEvent triggered by clicking the button.
	 * @throws Exception If the FXML file cannot be loaded.
	 */
	@FXML
	public void clickNewOrder(ActionEvent event) throws Exception {
		System.out.println("Selected: New Order");
		ClientUI.currentMode = ScreenMode.CREATE;

		// Hide current window
		((Node) event.getSource()).getScene().getWindow().hide();

		// Load the Order Creation screen
		Stage primaryStage = new Stage();
		Parent root = FXMLLoader.load(getClass().getResource("/gui/utils/OrderCreation.fxml"));
		Scene scene = new Scene(root);

		// Load CSS file
		scene.getStylesheets().add(getClass().getResource("/gui/utils/OrderCreation.css").toExternalForm());

		primaryStage.setTitle("Bistro - Book a Table");
		primaryStage.setScene(scene);
		primaryStage.show();
	}

	/**
	 * Handles the "Cancel Order" action.
	 * <p>
	 * Sets the global screen mode to {@link ScreenMode#CANCEL} and navigates to the
	 * "Insert Order Number" screen.
	 * </p>
	 *
	 * @param event The ActionEvent triggered by clicking the button.
	 * @throws Exception If the FXML file cannot be loaded.
	 */
	@FXML
	public void clickCancelOrder(ActionEvent event) throws Exception {
		System.out.println("Selected: Cancel Order");
		ClientUI.currentMode = ScreenMode.CANCEL;
		// Hide current window
		((Node) event.getSource()).getScene().getWindow().hide();
		Stage primaryStage = new Stage();
		Parent root = FXMLLoader.load(getClass().getResource("/gui/customer/CancelOrder.fxml"));
		Scene scene = new Scene(root);
		scene.getStylesheets().add(getClass().getResource("/gui/customer/CancelOrder.css").toExternalForm());

		primaryStage.setTitle("Bistro - Cancel Order");
		primaryStage.setScene(scene);
		primaryStage.show();
	}

	/**
	 * Opens the Digital Member Card in a new modal window.
	 *
	 * @param event The ActionEvent triggered by clicking the button.
	 */
	@FXML
	public void openDigitalCard(ActionEvent event) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/customer/MemberCard.fxml"));
			Parent root = loader.load();

			MemberCardController controller = loader.getController();
			controller.initData(ChatClient.user);

			Stage stage = new Stage();
			stage.setTitle("Bistro Digital Card");
			stage.setScene(new Scene(root));
			stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
			stage.show();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Handles the "Exit" action.
	 * <p>
	 * Attempts to send a {@link ActionType#CLIENT_QUIT} message to the server to
	 * gracefully close the connection, then terminates the client application.
	 * </p>
	 *
	 * @param event The ActionEvent triggered by clicking the button.
	 */
	@FXML
	public void getExitBtn(ActionEvent event) {
		System.out.println("Exit requested");
		try {
			// Attempt to gracefully disconnect from the server
			if (ClientUI.chat != null && ClientUI.chat.client != null && ClientUI.chat.client.isConnected()) {
				BistroMessage msg = new BistroMessage(ActionType.CLIENT_QUIT, null);
				ClientUI.chat.client.sendToServer(msg);
			}
		} catch (Exception e) {
		} finally {
			System.out.println("Closing application now.");
			System.exit(0);
		}
	}

}