package gui.customer;

import java.net.URL;
import java.util.ResourceBundle;

import client.ChatClient;
import client.ClientUI;
import common.ActionType;
import common.BistroMessage;
import common.Role;
import common.User;
import gui.utils.AbstractBistroController;
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

/**
 * UserMenuController controls the main navigation menu for the Bistro application.
 * This controller handles the dashboard presented to the member after a
 * successful login (or guest entry). It adjusts the visibility of buttons based
 * on the user's role (Member vs. Guest).
 */
public class UserMenuController extends AbstractBistroController implements Initializable {

	/** Label to display a welcome message with the user's name or "Guest". */
	@FXML
	private Label lblWelcome;

	// visible only for a member
	@FXML
	private Button btnDigitalCard;
	@FXML
	private Button btnHistory;
	@FXML
	private Button btnEditProfile;

	// visible for members and guests
	@FXML
	private Button btnNewOrder;
	@FXML
	private Button btnCancelOrder;
	@FXML
	private Button btnExitWaitList;
	@FXML
	private Button btnPayBill;
	@FXML
	private Button btnExit;

	/**
	 * Initializes the controller class. This method is automatically called after
	 * the FXML file has been loaded.
	 * It checks the currently logged-in user's role via {@link ChatClient#user}. If
	 * the user is a Guest (or null), it hides member-specific features (Update
	 * Order, Digital Card).
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
			btnHistory.setVisible(false);
			btnHistory.setManaged(false);

			btnDigitalCard.setVisible(false);
			btnDigitalCard.setManaged(false);

			btnEditProfile.setVisible(false);
			btnEditProfile.setManaged(false);
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
	 * Order Creation screen.
	 * </p>
	 *
	 * @param event The ActionEvent triggered by clicking the button.
	 * @throws Exception If the FXML file cannot be loaded.
	 */
	@FXML
	public void clickNewOrder(ActionEvent event) throws Exception {
		System.out.println("Selected: New Order");

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
	 * "Insert Order Number" screen.
	 * </p>
	 *
	 * @param event The ActionEvent triggered by clicking the button.
	 * @throws Exception If the FXML file cannot be loaded.
	 */
	@FXML
	public void clickCancelOrder(ActionEvent event) throws Exception {
		System.out.println("Selected: Cancel Order");
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
	 * Handles the "Exit Waitlist" button click event.
	 *
	 * This method facilitates the navigation from the main Customer Menu to the
	 * "Exit Waitlist" screen. It performs the following actions:
	 * - Hides the current window (Customer Menu).
	 * - Loads the FXML layout for the Exit Waitlist screen.
	 * - Applies the corresponding CSS stylesheet.
	 * - Displays the new screen in a new Stage.
	 *
	 * @param event The ActionEvent triggered by clicking the button; used to
	 * retrieve the current window.
	 */
	@FXML
	public void clickExitWaitList(ActionEvent event) {
		try {
			System.out.println("Selected: Exit Waitlist");

			// Hide the current window to transition to the next screen
			((Node) event.getSource()).getScene().getWindow().hide();

			// Initialize the new Stage and load the FXML layout file
			Stage primaryStage = new Stage();
			Parent root = FXMLLoader.load(getClass().getResource("/gui/customer/ExitWaitlist.fxml"));
			Scene scene = new Scene(root);

			// Load and apply the CSS
			scene.getStylesheets().add(getClass().getResource("/gui/customer/ExitWaitlist.css").toExternalForm());

			// Configure the stage title and display the window
			primaryStage.setTitle("Bistro - Exit Waitlist");
			primaryStage.setScene(scene);
			primaryStage.show();

		} catch (Exception e) {
			// Handle any errors that occur during FXML loading
			e.printStackTrace();
			System.out.println("Error loading ExitWaitlist screen");
		}
	}

	/**
	 * Handles the "Pay Bill" button click event.
	 * Opens the Payment screen where customers (both Members and Guests) can pay
	 * their bill remotely using their order confirmation code.
	 *
	 * @param event The ActionEvent triggered by the button.
	 */
	@FXML
	public void clickPayBill(ActionEvent event) {
		try {
			System.out.println("Selected: Pay Bill");
			// Hide the current window
			((Node) event.getSource()).getScene().getWindow().hide();

			// Load the Payment screen FXML
			Stage primaryStage = new Stage();
			Parent root = FXMLLoader.load(getClass().getResource("/gui/customer/Payment.fxml"));
			Scene scene = new Scene(root);

			// Load the Payment CSS
			scene.getStylesheets().add(getClass().getResource("/gui/customer/Payment.css").toExternalForm());

			primaryStage.setTitle("Bistro - Pay Bill");
			primaryStage.setScene(scene);
			primaryStage.show();

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error loading Payment screen");
		}
	}

	@FXML
	public void clickHistory(ActionEvent event) {
		System.out.println("Selected: Order History");
		try {
			if (ChatClient.user != null) {
				BistroMessage msg = new BistroMessage(ActionType.GET_USER_HISTORY, ChatClient.user.getUserId());
																																																			
			}

			((Node) event.getSource()).getScene().getWindow().hide();
			Stage primaryStage = new Stage();
			Parent root = FXMLLoader.load(getClass().getResource("/gui/customer/OrderHistory.fxml"));
			Scene scene = new Scene(root);
			scene.getStylesheets().add(getClass().getResource("/gui/customer/OrderHistory.css").toExternalForm());
			primaryStage.setTitle("Bistro - Order History");
			primaryStage.setScene(scene);
			primaryStage.show();

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error loading Order History");
		}
	}

	@FXML
	public void clickEditProfile(ActionEvent event) {
		try {
			// Load the FXML file
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/customer/EditDetails.fxml"));
			Parent root = loader.load();

			// Create the Stage (Window)
			Stage stage = new Stage();
			stage.setTitle("Update Personal Details");
			stage.setScene(new Scene(root));

			// Show the window
			stage.show();

		} catch (Exception e) {
			System.out.println("Error loading EditProfile screen");
			e.printStackTrace();
		}
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
	 * Triggered by the "Logout" button in FXML. Uses the REUSED logic from
	 * AbstractBistroController.
	 */
	@FXML
	public void clickLogout(ActionEvent event) {
		// call to the function in the AbstractBistroController class
		super.logout(event);
	}
}
