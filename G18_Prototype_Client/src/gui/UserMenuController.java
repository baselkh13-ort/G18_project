package gui;

import java.io.IOException;
import java.util.ResourceBundle;

import client.ChatClient;
import client.ClientUI;
import common.ActionType;
import common.BistroMessage;
import common.User;
import logic.ScreenMode;
import java.net.URL;
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

public class UserMenuController implements Initializable {
	@FXML
	private Label lblWelcome;
	@FXML
	private Button btnUpdate; // will be hiden from not members
	@FXML
	private Button btnViewAll;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		User user = ChatClient.user;
		if (lblWelcome == null || btnUpdate == null) {
			return;
		}
		if (user == null) {
			lblWelcome.setText("Welcome, Guest");
			btnUpdate.setVisible(false);
		} else {
			lblWelcome.setText("Welcome, " + user.getFirstName());
			btnUpdate.setVisible(true);

		}

	}

	/**
	 * Button: View All Orders Action: Sends a request to get all orders and
	 * switches to the Table View. Sets the mode to "VIEW" (Read-Only).
	 */
	@FXML
	public void clickViewAll(ActionEvent event) throws Exception {
		System.out.println("Selected: View All");

		// This ensures that if we open an order details screen later, it will be
		// locked.
		ClientUI.currentMode = ScreenMode.VIEW;

		// 1. Send request to server
		BistroMessage msg = new BistroMessage(ActionType.GET_ALL_ORDERS, null);
		ClientUI.chat.accept(msg);

		// 2. Open the Order List screen (Table)
		((Node) event.getSource()).getScene().getWindow().hide(); // Close Menu

		Stage primaryStage = new Stage();
		Parent root = FXMLLoader.load(getClass().getResource("/gui/OrderList.fxml"));
		Scene scene = new Scene(root);

		scene.getStylesheets().add(getClass().getResource("/gui/OrderList.css").toExternalForm());

		primaryStage.setTitle("All Orders Table");
		primaryStage.setScene(scene);
		primaryStage.show();
	}

	/**
	 * Button: Update Order Action: Switches to the "Input Order Number" screen.
	 * Sets the mode to "UPDATE" (Editable).
	 */
	@FXML
	public void clickUpdate(ActionEvent event) throws Exception {
		System.out.println("Selected: Update Order");

		// --- CRITICAL STEP: Set the global mode to UPDATE ---
		// This tells the next screens that editing is allowed.
		ClientUI.currentMode = ScreenMode.UPDATE;

		((Node) event.getSource()).getScene().getWindow().hide(); // Close Menu

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
	 * Button: Exit Action: Sends a quit message to the server and closes the client
	 * application.
	 */
	@FXML
	public void getExitBtn(ActionEvent event) {
		System.out.println("Exit requested");
		try {
			// Attempt to gracefully disconnect from the server
			if (ClientUI.chat != null) {
				BistroMessage msg = new BistroMessage(ActionType.CLIENT_QUIT, null);
				ClientUI.chat.accept(msg);
				if (ClientUI.chat.client != null) {
					ClientUI.chat.client.quit();// Closes connection and calls System.exit(0)
				}
			}
		} catch (Exception e) {
		}
		System.exit(0);
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