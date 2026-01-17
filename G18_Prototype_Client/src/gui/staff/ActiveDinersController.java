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
 * Controller for viewing currently seated diners (Active Diners).
 * <p>
 * Displays a list of tables currently occupied.
 * Allows refreshing the data from the server.
 * </p>
 */
public class ActiveDinersController implements Initializable {

    @FXML private TableView<Order> tblActiveDiners;
    @FXML private TableColumn<Order, Integer> colOrderNum;
    @FXML private TableColumn<Order, String> colName;
    @FXML private TableColumn<Order, Timestamp> colTimeEntered; // זמן כניסה/הזמנה
    @FXML private TableColumn<Order, Integer> colGuests;
    
    @FXML private Button btnRefresh;
    @FXML private Button btnClose;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        loadData();
    }

    private void setupTable() {
        colOrderNum.setCellValueFactory(new PropertyValueFactory<>("orderNumber"));
        colName.setCellValueFactory(new PropertyValueFactory<>("CustomerName")); 
        colTimeEntered.setCellValueFactory(new PropertyValueFactory<>("orderDate"));
        colGuests.setCellValueFactory(new PropertyValueFactory<>("numberOfGuests"));
    }

    private void loadData() {
        if (ChatClient.activeDiners != null) {
            ObservableList<Order> data = FXCollections.observableArrayList(ChatClient.activeDiners);
            tblActiveDiners.setItems(data);
        }
    }

    @FXML
    void refreshData(ActionEvent event) {
        // Clear old data
        ChatClient.activeDiners.clear();
        
        // Fetch new data
        BistroMessage msg = new BistroMessage(ActionType.GET_ACTIVE_DINERS, null);
        ClientUI.chat.accept(msg);
        
        // Reload table
        loadData();
    }

    @FXML
    void closeWindow(ActionEvent event) {
        ((Stage) btnClose.getScene().getWindow()).close();
    }
}