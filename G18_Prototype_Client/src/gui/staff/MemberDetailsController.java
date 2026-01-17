package gui.staff;

import java.net.URL;
import java.sql.Timestamp;
import java.util.ResourceBundle;

import client.ChatClient;
import common.Order;
import common.User;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

/**
 * Controller for the specific Member Details screen. This screen is opened when
 * a staff member selects a user from the management list. It displays: 1.
 * Static personal information (passed via {@link #initData(User)}). 2. Dynamic
 * order history (loaded from {@link ChatClient#listOfOrders})
 */
public class MemberDetailsController implements Initializable {

	// Personal Info Labels
	@FXML
	private Label lblFullName;
	@FXML
	private Label lblId;
	@FXML
	private Label lblPhone;
	@FXML
	private Label lblEmail;

	// History Table Components
	@FXML
	private TableView<Order> tblHistory;

	@FXML
	private TableColumn<Order, Integer> colOrderNum;
	@FXML
	private TableColumn<Order, Timestamp> colDate;
	@FXML
	private TableColumn<Order, Integer> colGuests;

	// The new column for Visit Type
	@FXML
	private TableColumn<Order, String> colVisitType;

	@FXML
	private TableColumn<Order, String> colStatus;
	@FXML
	private TableColumn<Order, Double> colPrice;

	@FXML
	private Button btnClose;

	/**
	 * Initializes the controller class.
	 * This method is automatically called after the FXML file has been loaded. It
	 * configures the table columns and loads the data from the client's memory.
	 *
	 * @param location  The location used to resolve relative paths for the root
	 *                  object.
	 * @param resources The resources used to localize the root object.
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		setupTableColumns();
		loadHistoryFromMemory();
	}

	/**
	 * Receives the selected User object from the previous screen
	 * (MemberManagement). Updates the UI labels with the user's personal
	 * information.
	 *
	 * @param user The User object selected in the previous table.
	 */
	public void initData(User user) {
		if (user != null) {
			lblFullName.setText(user.getFirstName() + " " + user.getLastName());
			lblId.setText(String.valueOf(user.getUserId()));
			lblPhone.setText(user.getPhone());
			lblEmail.setText(user.getEmail());
		}
	}

	/**
	 * Configures the table columns to map to the {@link Order} properties. Includes
	 * custom logic for the 'Visit Type' column.
	 */
	private void setupTableColumns() {
		// Map standard properties directly
		colOrderNum.setCellValueFactory(new PropertyValueFactory<>("orderNumber"));
		colDate.setCellValueFactory(new PropertyValueFactory<>("orderDate"));
		colGuests.setCellValueFactory(new PropertyValueFactory<>("numberOfGuests"));
		colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
		colPrice.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));

		// Custom Logic for Visit Type 
		// Requirement: If Status is "COMPLETED", show "VISIT". Otherwise show "-".
		colVisitType.setCellValueFactory(cellData -> {
			Order order = cellData.getValue();

			if (order.getStatus() != null && order.getStatus().equals("COMPLETED")) {
				return new SimpleStringProperty("VISIT");
			} else {
				return new SimpleStringProperty("-");
			}
		});
	}

	/**
	 * Loads the order history from the static {@link ChatClient#listOfOrders} list.
	 * This list was populated by the server request made in the previous screen.
	 */
	private void loadHistoryFromMemory() {
		if (ChatClient.listOfOrders != null) {
			ObservableList<Order> data = FXCollections.observableArrayList(ChatClient.listOfOrders);
			tblHistory.setItems(data);
		} else {
			System.out.println("Info: No history data found in memory for this user.");
		}
	}

	/**
	 * Closes the current detail window (popup).
	 *
	 * @param event The event triggered by the Close button.
	 */
	@FXML
	void closeWindow(ActionEvent event) {
		((Stage) btnClose.getScene().getWindow()).close();
	}
}