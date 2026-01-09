package gui.staff;

import java.net.URL;
import java.util.ResourceBundle;

import client.ChatClient;  
import common.Role;
import common.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable; 
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class WorkerMenuController implements Initializable{
	// Common Buttons (For Worker AND Manager)
	@FXML
	private Label lblWelcome;
	@FXML
	private Button btnRegisterClient; // register a new member
	// אני עוד צריכה להוסיף פה כפתורים

	// Manager Only Buttons
	@FXML
	private Button btnViewReports;
	// אני עוד צריכה להוסיף פה כפתורים

	/**
     * 
     */
	@Override
    public void initialize(URL location, ResourceBundle resources) {
		User user = ChatClient.user;
		if (user == null || lblWelcome == null || btnViewReports == null) {
            return; 
        }
		lblWelcome.setText("Hello, " + user.getFirstName() + " (" + user.getRole() + ")");
		if (user.getRole() == Role.WORKER) {
			// MANAGER VIEW - Show everything
			btnViewReports.setVisible(true);
		} else {
            // WORKER VIEW - Hide sensitive buttons
            btnViewReports.setVisible(false);
        }
	}
		
	public void start(Stage primaryStage) throws Exception {
		
		Parent root = FXMLLoader.load(getClass().getResource("/gui/staff/WorkerMenu.fxml"));
        Scene scene = new Scene(root);
        
        scene.getStylesheets().add(getClass().getResource("/gui/staff/WorkerMenu.css").toExternalForm());
        primaryStage.setTitle("Bistro - Staff Menu");
        primaryStage.setScene(scene);
        primaryStage.show();
	
	}
	@FXML
    public void register(ActionEvent event) throws Exception {
		
        ((Node)event.getSource()).getScene().getWindow().hide(); 
        
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/staff/RegisterClient.fxml"));
        Parent root = loader.load();
        
        Stage stage = new Stage();
        stage.setTitle("Register New Client");
        stage.setScene(new Scene(root));
        stage.show();
    }
	
	
	
	
}
