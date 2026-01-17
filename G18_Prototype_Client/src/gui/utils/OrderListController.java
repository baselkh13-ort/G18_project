package gui.utils;

import java.net.URL;
import java.sql.Timestamp;
import java.util.ResourceBundle;

import client.ChatClient;
import client.ClientUI;
import common.ActionType;
import common.BistroMessage;
import common.Order;
import gui.staff.WorkerMenuController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

/**
 * Controller class for displaying the list of orders.
 * <p>
 * This screen allows staff to view all orders and refresh the list 
 * to see real-time updates from the database.
 * </p>
 */
public class OrderListController implements Initializable {

    // --- FXML Connections ---
    @FXML
    private TableView<Order> tblOrders;
    @FXML
    private TableColumn<Order, Integer> colOrderNum;
    @FXML
    private TableColumn<Order, Timestamp> colDate;
    @FXML
    private TableColumn<Order, Integer> colGuests;
    @FXML
    private TableColumn<Order, Integer> colConfirmationCode;
    @FXML
    private TableColumn<Order, Integer> colMember;
    @FXML
    private TableColumn<Order, Timestamp> colDateOfPlacingOrder;
    @FXML
    private TableColumn<Order, String> colName;
    @FXML
    private TableColumn<Order, String> colPhone;
    @FXML
    private TableColumn<Order, String> colEmail;
    
    @FXML
    private Button btnRefresh; // Make sure to add this button in SceneBuilder

    /**
     * Initializes the controller class.
     * <p>
     * Sets up the table columns and loads the initial data.
     * </p>
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        
        // 1. Set up the columns
        colOrderNum.setCellValueFactory(new PropertyValueFactory<>("orderNumber"));
        colName.setCellValueFactory(new PropertyValueFactory<>("CustomerName"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("orderDate"));
        colGuests.setCellValueFactory(new PropertyValueFactory<>("numberOfGuests"));
        colConfirmationCode.setCellValueFactory(new PropertyValueFactory<>("confirmationCode"));        
        colMember.setCellValueFactory(new PropertyValueFactory<>("memberId"));
        colDateOfPlacingOrder.setCellValueFactory(new PropertyValueFactory<>("dateOfPlacingOrder"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
    
        // 2. Load data into the table
        loadTableData();
    }
    
    /**
     * Helper method to populate the table with data from ChatClient.
     * <p>
     * This is separated so it can be reused by the Refresh button.
     * </p>
     */
    private void loadTableData() {
        tblOrders.getItems().clear(); // Clear existing items to avoid duplicates
        
        if (ChatClient.listOfOrders != null && !ChatClient.listOfOrders.isEmpty()) {
            ObservableList<Order> data = FXCollections.observableArrayList(ChatClient.listOfOrders);
            tblOrders.setItems(data);
            tblOrders.refresh(); // Force UI refresh
        } else {
            System.out.println("Info: Order list is empty or null.");
        }
    }

    /**
     * Handles the "Refresh" button click.
     * <p>
     * Sends a request to the server to fetch the most up-to-date list of orders
     * and then reloads the table.
     * </p>
     * @param event The ActionEvent triggered by the button.
     */
    @FXML
    public void clickRefresh(ActionEvent event) {
        try {
            // 1. Send request to server to get fresh data
            // NOTE: Change ActionType.GET_ALL_ORDERS to match your specific Enum for this list
            BistroMessage msg = new BistroMessage(ActionType.GET_ALL_ORDERS, null); 
            ClientUI.chat.accept(msg); 
            
            // 2. Reload the table with the new data from ChatClient
            loadTableData();
            
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText("Failed to refresh data from server.");
            alert.show();
        }
    }
    
    /**
     * Handles the "Back" button to return to the main menu.
     */
    public void getBackBtn(ActionEvent event) {
        try {
            // Hide the current window
            ((Node)event.getSource()).getScene().getWindow().hide();
            
            // Open the User Menu
            Stage primaryStage = new Stage();
            WorkerMenuController menu = new WorkerMenuController();
            menu.start(primaryStage); 
            
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText("Could not load the menu screen.");
            alert.show();
        }
    }
}