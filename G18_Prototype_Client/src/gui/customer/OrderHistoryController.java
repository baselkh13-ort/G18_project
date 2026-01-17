package gui.customer;

import java.net.URL;
import java.sql.Timestamp;
import java.util.ResourceBundle;

import client.ChatClient;
import common.Order;
import common.User;
import javafx.beans.property.SimpleStringProperty; // Import needed for the custom column
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
 * This screen displays a table of all past orders.
 * It includes a calculated column to distinguish actual "Visits" (Completed orders)
 * from other order types.
 */
public class OrderHistoryController implements Initializable {

    @FXML
    private Label lblTitle;

    //TableView Components
    @FXML
    private TableView<Order> tblHistory;

    @FXML
    private TableColumn<Order, Integer> colOrderNumber; 
    @FXML
    private TableColumn<Order, Timestamp> colDate; 
    @FXML
    private TableColumn<Order, Integer> colGuests; 
    @FXML
    private TableColumn<Order, String> colStatus; 
    @FXML
    private TableColumn<Order, Double> colPrice; 
    
    /** * New Column: Indicates if this order counts as a physical "Visit".
     * Logic: If status is COMPLETED, then Visit = Yes.
     */
    @FXML
    private TableColumn<Order, String> colIsVisit; 

    @FXML
    private Button btnBack;

    /**
     * Initializes the controller class.
     * Sets up the table columns and generates the "Is Visit" data on the fly.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        User user = ChatClient.user;
        if (user != null) {
            lblTitle.setText("History for " + user.getFirstName());
        }

        //Setup Table Columns
        
        // Standard columns mapping directly to Order properties
        colOrderNumber.setCellValueFactory(new PropertyValueFactory<>("orderNumber"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("orderDate"));
        colGuests.setCellValueFactory(new PropertyValueFactory<>("numberOfGuests"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status")); 
        colPrice.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));

        //Custom Logic for "Visited?" Column 
        // This creates a value based on the Status, without changing the Order class.
        colIsVisit.setCellValueFactory(cellData -> {
            Order order = cellData.getValue();
            String status = order.getStatus(); 
            
            // Check if the status implies a completed visit
            if (status != null && status.equals("COMPLETED")) {
                return new SimpleStringProperty("Yes"); // It was a visit
            } else {
                return new SimpleStringProperty("No");  // Just an order (or canceled)
            }
        });

        // Load Data
        if (ChatClient.listOfOrders != null) {
            ObservableList<Order> data = FXCollections.observableArrayList(ChatClient.listOfOrders);
            tblHistory.setItems(data);
        } else {
            System.out.println("No history found in memory.");
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