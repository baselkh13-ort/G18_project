package gui;

import java.net.URL;
import java.util.ResourceBundle;

import client.ChatClient;
import common.Member;
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

public class MemberListController implements Initializable {

    // FXML Connections
    @FXML private TableView<Member> tblMembers;
    @FXML private TableColumn<Member, String> colMemberId;
    @FXML private TableColumn<Member, String> colQrCode;
    @FXML private TableColumn<Member, String> colUserName;
    @FXML private TableColumn<Member, String> colFullName;
    @FXML private TableColumn<Member, String> colPhone;
    @FXML private TableColumn<Member, String> colEmail;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        // 1. Set up the columns
        // MUST match getters in Member class:
        // memberId -> getMemberId(), qrCode -> getQrCode(), etc.
        colMemberId.setCellValueFactory(new PropertyValueFactory<>("memberId"));
        colQrCode.setCellValueFactory(new PropertyValueFactory<>("qrCode"));
        colUserName.setCellValueFactory(new PropertyValueFactory<>("userName"));
        colFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        // 2. Load data into the table
        if (ChatClient.listOfMembers != null) {
            ObservableList<Member> data = FXCollections.observableArrayList(ChatClient.listOfMembers);
            tblMembers.setItems(data);
        } else {
            System.out.println("Warning: Member list is empty or null.");
        }
    }

    public void getBackBtn(ActionEvent event) {
        try {
            ((Node)event.getSource()).getScene().getWindow().hide();

            Stage primaryStage = new Stage();
            UserMenuController menu = new UserMenuController();
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
