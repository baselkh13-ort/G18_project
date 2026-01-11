package gui.utils;

import java.net.URL;
import java.sql.Date;
import java.util.ResourceBundle;

import client.ChatClient;
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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

public class OrderListController implements Initializable {

    // FXML Connections
    @FXML private TableView<Order> tblOrders;
    @FXML private TableColumn<Order, Integer> colOrderNum;
    @FXML private TableColumn<Order, Date> colDate;
    @FXML private TableColumn<Order, Integer> colGuests;
    @FXML private TableColumn<Order, Integer> colConfirmationCode;
    @FXML private TableColumn<Order, Integer> colSubscriber;
    @FXML private TableColumn<Order, Date> colDateOfPlacingOrder;

    /**
     * Initializes the controller class.
     * This method is automatically called after the FXML file has been loaded.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        
        // 1. Set up the columns
        // The strings inside PropertyValueFactory MUST match the getter names in the Order class.
        // Example: "orderNumber" looks for getOrderNumber()
        colOrderNum.setCellValueFactory(new PropertyValueFactory<>("orderNumber"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("orderDate"));
        colGuests.setCellValueFactory(new PropertyValueFactory<>("numberOfGuests"));
        colConfirmationCode.setCellValueFactory(new PropertyValueFactory<>("confirmationCode"));        
        colSubscriber.setCellValueFactory(new PropertyValueFactory<>("subscriberID"));
        colDateOfPlacingOrder.setCellValueFactory(new PropertyValueFactory<>("dateOfPlacingOrder"));
        
    
        // 2. Load data into the table
        // We check if the list is not null to avoid NullPointerException
        if (ChatClient.listOfOrders != null) {
            ObservableList<Order> data = FXCollections.observableArrayList(ChatClient.listOfOrders);
            tblOrders.setItems(data);
        } else {
            System.out.println("Warning: Order list is empty or null.");
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