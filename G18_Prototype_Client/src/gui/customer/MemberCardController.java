package gui.customer;

import common.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class MemberCardController {

    @FXML private Label lblFullName;
    @FXML private Label lblMemberID;
    @FXML private Label lblMemberCode;


    public void initData(User user) {
        if (user != null) {
            lblFullName.setText(user.getFirstName() + " " + user.getLastName());
            lblMemberID.setText("ID: " + user.getUserId());
            
            lblMemberCode.setText(String.valueOf(user.getMemberCode()));
        }
    }

    @FXML
    public void closeCard(ActionEvent event) {
        ((Stage)((Node)event.getSource()).getScene().getWindow()).close();
    }
}