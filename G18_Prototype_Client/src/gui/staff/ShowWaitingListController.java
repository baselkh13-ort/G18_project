package gui.staff;

import java.net.URL;
import java.sql.Timestamp;
import java.util.ResourceBundle;

import javafx.scene.Node;
import client.ChatClient;
import client.ClientUI;
import common.BistroMessage;
import common.ActionType;
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
 * Controller class for the ShowWaitingList screen. This screen allows the
 * Restaurant Representative to view and manage the list of customers currently
 * waiting for a table.
 * 
 */
public class ShowWaitingListController implements Initializable {

	// FXML Component Injections
	@FXML
	private TableView<Order> waitingListTable;

	@FXML
	private TableColumn<Order, Integer> colOrderId;

	@FXML
	private TableColumn<Order, String> colCustomerName;

	@FXML
	private TableColumn<Order, String> colPhone;

	@FXML
	private TableColumn<Order, String> colEmail;
	@FXML
	private TableColumn<Order, Integer> colSize;

	@FXML
	private TableColumn<Order, Timestamp> colTime;

	@FXML
	private TableColumn<Order, String> colStatus;

	@FXML
	private Button refreshBtn;

	@FXML
	private Button backBtn;

	/**
	 * ObservableList to hold the data for the table view.
	 */
	private ObservableList<Order> waitingList = FXCollections.observableArrayList();

	/**
	 * Initializes the controller class. Sets up the table columns and sends a
	 * request to fetch the initial data. * @param location The location used to
	 * resolve relative paths for the root object.
	 * 
	 * @param resources The resources used to localize the root object.
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		setupTableColumns();
		loadData();
	}
	/**
	 * Configures the table columns to bind to the specific properties of the Order
	 * entity.
	 */
	private void setupTableColumns() {
		colOrderId.setCellValueFactory(new PropertyValueFactory<>("orderNumber"));
		colCustomerName.setCellValueFactory(new PropertyValueFactory<>("CustomerName"));
		colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
		colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
		colSize.setCellValueFactory(new PropertyValueFactory<>("numberOfGuests"));
		colTime.setCellValueFactory(new PropertyValueFactory<>("orderDate"));
		colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

		waitingListTable.setItems(waitingList);
	}
	
	/**
     * Loads the waiting list data from the client's local memory into the table view.
     * <p>
     * This method assumes that the data was previously fetched from the server
     * and stored in the static {@link ChatClient#waitingList}.
     * It clears the current table items and re-populates them to ensure the view
     * is up-to-date.
     * </p>
     */
	private void loadData() {
		waitingList.clear();
        if (ChatClient.waitingList != null) {
            waitingList.addAll(ChatClient.waitingList);
        }
    }
	
	/**
	 * Sends a request to the server to fetch all orders with status 'WAITING'. Uses
	 * the BistroMessage class for communication. * @param event The event triggered
	 * by the refresh button.
	 */
	@FXML
	void refreshData(ActionEvent event) {
		// Create a new message requesting the waiting list
		BistroMessage msg = new BistroMessage(ActionType.GET_WAITING_LIST, null);

		// Send the message to the server via the client controller
		ClientUI.chat.accept(msg);
		loadData();
	}



	/**
     * Handles the "Back" button action.
     * <p>
     * Navigates the user back to the main Worker Menu by hiding the current
     * Waiting List screen and opening the {@link WorkerMenuController}.
     * </p>
     *
     * @param event The event triggered by clicking the back button.
     */
    @FXML
    void goBack(ActionEvent event) {
        try {
            // 1. Hide the current window (Waiting List)
            ((Node)event.getSource()).getScene().getWindow().hide();

            // 2. Initialize and open the Worker Menu
            Stage primaryStage = new Stage();
            WorkerMenuController workerMenu = new WorkerMenuController();
            workerMenu.start(primaryStage);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error returning to Worker Menu");
        }
    }
}