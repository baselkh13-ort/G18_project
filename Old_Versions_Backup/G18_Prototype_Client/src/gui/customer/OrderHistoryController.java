package gui.customer;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

import client.ChatClient;
import client.ClientUI;
import common.ActionType;
import common.BistroMessage;
import common.Order;
import common.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

/**
 * Controller for the "Orders and Visits History" screen.
 * <p>
 * This screen is accessible ONLY to members. It displays a table of all past
 * orders and visits. A "Visit" is simply an Order with a status of COMPLETED or
 * PAID.
 * </p>
 */
public class OrderHistoryController implements Initializable {

	@FXML
	private Label lblTitle;

	// TableView Components
	@FXML
	private TableView<Order> tblHistory;

	@FXML
	private TableColumn<Order, Integer> colOrderNumber; // Order ID / Code
	@FXML
	private TableColumn<Order, String> colDate; // Date
	@FXML
	private TableColumn<Order, String> colTime; // Time
	@FXML
	private TableColumn<Order, Integer> colGuests; // Num of Diners
	@FXML
	private TableColumn<Order, String> colStatus; // Status (The key to distinguish visits)
	@FXML
	private TableColumn<Order, Double> colPrice; // Total Price

	@FXML
	private Button btnBack;

	/**
	 * Initializes the controller class. Sets up the table columns and fetches the
	 * data from the server.
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		User user = ChatClient.user;
		if (user != null) {
			lblTitle.setText("History for " + user.getFirstName());
		}
		// Setup Table Columns (Mapping fields from Order class)
		colOrderNumber.setCellValueFactory(new PropertyValueFactory<>("userId")); 
		colDate.setCellValueFactory(new PropertyValueFactory<>("orderDate"));
		colTime.setCellValueFactory(new PropertyValueFactory<>("orderTime"));
		colGuests.setCellValueFactory(new PropertyValueFactory<>("numberOfGuests"));
		colStatus.setCellValueFactory(new PropertyValueFactory<>("status")); // e.g., COMPLETED, CANCELED
		colPrice.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));

		// 2. Load Data
		loadOrderHistory();
	}

	/**
     * Fetches the order history for the current user from the server.
     */
    private void loadOrderHistory() {
        try {
            // Request user's orders (Server should handle the query by memberCode)
            BistroMessage msg = new BistroMessage(ActionType.GET_USER_HISTORY, ChatClient.user.getUserId());
            ClientUI.chat.accept(msg);

            // Assume the server returns an ArrayList<Order> in ChatClient.orderList
            ArrayList<Order> orders = ChatClient.listOfOrders; 
            
            if (orders != null) {
                ObservableList<Order> data = FXCollections.observableArrayList(orders);
                tblHistory.setItems(data);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error fetching history");
        }
    }

	/**
	 * Navigates back to the main User Menu.
	 */
	@FXML
	public void clickBack(ActionEvent event) {
		try {
			((Node) event.getSource()).getScene().getWindow().hide();
			Stage primaryStage = new Stage();
			UserMenuController menu = new UserMenuController();
			menu.start(primaryStage);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}