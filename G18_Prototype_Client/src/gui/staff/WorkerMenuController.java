package gui.staff;

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

public class WorkerMenuController implements Initializable{
	// Common Buttons (For Worker AND Manager)
	@FXML
	private Label lblWelcome;
	@FXML
	private Button btnRegisterClient; // register a new member
	@FXML
	private Button btnViewAll;
	// אני עוד צריכה להוסיף פה כפתורים

	// Manager Only Buttons
	@FXML
	private Button btnViewReports;
	@FXML
	private Button btnExit;
	// אני עוד צריכה להוסיף פה כפתורים

	/**
     * 
     */
	@Override
    public void initialize(URL location, ResourceBundle resources) {
		User user = ChatClient.user;
		if (user == null || lblWelcome == null || btnViewReports == null) {
            return; 
        }
		lblWelcome.setText("Hello, " + user.getFirstName() + " (" + user.getRole() + ")");
		if (user.getRole() == Role.WORKER) {
            // WORKER VIEW - Hide sensitive buttons
			btnViewReports.setVisible(false);
		} else {
			// MANAGER VIEW - Show everything
            btnViewReports.setVisible(true);
        }
	}
		
	public void start(Stage primaryStage) throws Exception {
		
		Parent root = FXMLLoader.load(getClass().getResource("/gui/staff/WorkerMenu.fxml"));
        Scene scene = new Scene(root);
        
        scene.getStylesheets().add(getClass().getResource("/gui/staff/WorkerMenu.css").toExternalForm());
        primaryStage.setTitle("Bistro - Staff Menu");
        primaryStage.setScene(scene);
        primaryStage.show();
	
	}
	@FXML
    public void register(ActionEvent event) throws Exception {
		
        ((Node)event.getSource()).getScene().getWindow().hide(); 
        
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/staff/RegisterClient.fxml"));
        Parent root = loader.load();
        
        Stage stage = new Stage();
        stage.setTitle("Register New Client");
        stage.setScene(new Scene(root));
        stage.show();
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
