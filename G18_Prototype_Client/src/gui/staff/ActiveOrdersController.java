package gui.staff;

import java.net.URL;
import java.sql.Timestamp;
import java.util.ResourceBundle;

import client.ChatClient;
import client.ClientUI;
import common.ActionType;
import common.BistroMessage;
import common.Order;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

/**
 * Controller for viewing all active orders. This screen displays a list of
 * orders that are currently active or in process. It allows restaurant staff to
 * monitor ongoing orders and refresh the data from the server to get the latest
 * status.
 */
public class ActiveOrdersController implements Initializable {

	@FXML
	private TableView<Order> tblActiveOrders;
	@FXML
	private TableColumn<Order, Integer> colOrderNum;
	@FXML
	private TableColumn<Order, String> colName;
	@FXML
	private TableColumn<Order, Timestamp> colDate;
	@FXML
	private TableColumn<Order, Integer> colGuests;
	@FXML
	private TableColumn<Order, String> colStatus;

	@FXML
	private Button btnRefresh;
	@FXML
	private Button btnClose;

	/**
	 * Initializes the controller class. Called automatically after the FXML file
	 * has been loaded. Sets up the table columns and loads the initial data from
	 * the client memory.
	 * 
	 * @param location  The location used to resolve relative paths for the root
	 *                  object.
	 * @param resources The resources used to localize the root object.
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		setupTable();
		loadData();
	}

	/**
	 * Configures the TableView columns. Maps the table columns to the corresponding
	 * properties in the Order entity using PropertyValueFactory.
	 */
	private void setupTable() {
		colOrderNum.setCellValueFactory(new PropertyValueFactory<>("orderNumber"));
		colName.setCellValueFactory(new PropertyValueFactory<>("CustomerName"));
		colDate.setCellValueFactory(new PropertyValueFactory<>("orderDate"));
		colGuests.setCellValueFactory(new PropertyValueFactory<>("numberOfGuests"));
		colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
	}

	/**
	 * Populates the table with the list of active orders. Retrieves the data from
	 * the static ChatClient.activeOrders list and wraps it in an ObservableList for
	 * display.
	 */
	private void loadData() {
		if (ChatClient.activeOrders != null) {
			ObservableList<Order> data = FXCollections.observableArrayList(ChatClient.activeOrders);
			tblActiveOrders.setItems(data);
		}
	}

	/**
	 * Handles the "Refresh" button click. Clears the local list of active orders
	 * and sends a request to the server to fetch the most up-to-date data.
	 * 
	 * @param event The ActionEvent triggered by clicking the refresh button.
	 */
	@FXML
	void refreshData(ActionEvent event) {
		// Clear old data
		ChatClient.activeOrders.clear();

		// Fetch new data
		BistroMessage msg = new BistroMessage(ActionType.GET_ALL_ACTIVE_ORDERS, null);
		ClientUI.chat.accept(msg);

		// Reload table
		loadData();
	}

	/**
	 * Handles the "Close" button click. Closes the current window and returns to
	 * the previous screen.
	 * 
	 * @param event The ActionEvent triggered by clicking the close button.
	 */
	@FXML
	void closeWindow(ActionEvent event) {
		((Stage) btnClose.getScene().getWindow()).close();
	}
}