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
 * Controller for viewing all active orders.
 * <p>
 * Displays orders that are currently 'ACTIVE', 'APPROVED', or in process.
 * Allows refreshing the data from the server.
 * </p>
 */
public class ActiveOrdersController implements Initializable {

	@FXML
	private TableView<Order> tblActiveOrders;
	@FXML
	private TableColumn<Order, Integer> colOrderNum;
	@FXML
	private TableColumn<Order, String> colName;
	@FXML
	private TableColumn<Order, Timestamp> colDate; // או String תלוי במחלקה Order
	@FXML
	private TableColumn<Order, Integer> colGuests;
	@FXML
	private TableColumn<Order, String> colStatus;

	@FXML
	private Button btnRefresh;
	@FXML
	private Button btnClose;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		setupTable();
		loadData();
	}

	private void setupTable() {
		colOrderNum.setCellValueFactory(new PropertyValueFactory<>("orderNumber"));
		colName.setCellValueFactory(new PropertyValueFactory<>("CustomerName")); // ודאי שזה תואם לשדה ב-Order
		colDate.setCellValueFactory(new PropertyValueFactory<>("orderDate"));
		colGuests.setCellValueFactory(new PropertyValueFactory<>("numberOfGuests"));
		colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
	}

	private void loadData() {
		if (ChatClient.activeOrders != null) {
			ObservableList<Order> data = FXCollections.observableArrayList(ChatClient.activeOrders);
			tblActiveOrders.setItems(data);
		}
	}

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

	@FXML
	void closeWindow(ActionEvent event) {
		((Stage) btnClose.getScene().getWindow()).close();
	}
}