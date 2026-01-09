package gui.staff;

import client.ChatClient;
import client.ClientUI;
import common.ActionType;
import common.BistroMessage;
import common.Role;
import common.User;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Handling the registration of new members.
 */
public class RegisterClientController {

	@FXML
	private TextField txtUserName;
	@FXML
	private TextField txtPassword;
	@FXML
	private TextField txtUserID;
	@FXML
	private TextField txtFirstName;
	@FXML
	private TextField txtLastName;
	@FXML
	private TextField txtPhone;
	@FXML
	private TextField txtEmail;
	@FXML
	private Label lblStatus;

	/**
	 * Handles the submit action when the stuff member clicks the "Register Member""
	 * button.
	 */
	@FXML
	public void clickSubmit(ActionEvent event) {


		String userName = txtUserName.getText().trim();
		String password = txtPassword.getText().trim();
		String idStr = txtUserID.getText().trim();
		String firstName = txtFirstName.getText().trim();
		String lastName = txtLastName.getText().trim();
		String phone = txtPhone.getText().trim();
		String email = txtEmail.getText().trim();

		if (userName.isEmpty()) {
			setError("Please enter a username.");
            return;
		}

		if (password.isEmpty()) {
			setError("Please enter a password.");
			return;
		}
		
		if (idStr.isEmpty()) {
            setError("Please enter User ID.");
            return;
        }
		
		if (!idStr.matches("\\d+")) { 
            setError("User ID must contain only digits.");
            return;
        }
		if (firstName.isEmpty()) {
            setError("Please enter a first name.");
            return;
        }

		if (lastName.isEmpty()) { 
            setError("Please enter a last name.");
            return;
        }
		
		if (phone.isEmpty()) {
			setError("Please enter a phone number.");
			return;
		}

		if (phone.length() != 10 || !phone.startsWith("05") || !phone.matches("\\d+")) {
			setError("Phone must start with '05' and be 10 digits.");
			return;
		}

		if (email.isEmpty()) {
			setError("Please enter an Email Address.");
			return;
		}

		if (!email.contains("@") || !email.contains(".")) {
			setError("Invalid email format.");
			return;
		}
		int userID = Integer.parseInt(idStr);
		
		ChatClient.registeredUser = null;
		// if we are here it means that all the fields are valid
		User newUser = new User(userName, password, userID, firstName, lastName, phone, email);
		newUser.setRole(Role.MEMBER); 
		BistroMessage msg = new BistroMessage(ActionType.REGISTER_CLIENT, newUser);
		ClientUI.chat.accept(msg);
		if (ChatClient.registeredUser != null) {
            User savedUser = ChatClient.registeredUser;
            showSimulationAlert(savedUser.getFirstName(), savedUser.getPhone(), savedUser.getMemberCode());
            clearFields();
        } else {
            setError("Server failed to register client (returned null).");
        }
	}
	
	private void setError(String msg) {
        lblStatus.setText(msg);
    }

	private void showSimulationAlert(String name, String phone, int memberCode) {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("Registration Successful");
		alert.setHeaderText("Client Added Successfully!");
		alert.setContentText("Hello " + name + ",\n" 
                + "User ID: " + txtUserID.getText() + "\n"
                + "Generated Member Code: " + memberCode + "\n\n" 
                + "Simulated SMS sent to: " + phone);
		alert.showAndWait();
	}

	private void clearFields() {
		txtUserName.clear();
        txtPassword.clear();
        txtUserID.clear();
        txtFirstName.clear();
        txtLastName.clear();
        txtPhone.clear();
        txtEmail.clear();
	}

	@FXML
	public void clickBack(ActionEvent event) {
		try {
			((Node) event.getSource()).getScene().getWindow().hide();

			FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/staff/WorkerMenu.fxml"));
			Parent root = loader.load();

			Stage stage = new Stage();
			stage.setScene(new Scene(root));
			stage.setTitle("Bistro - Staff Menu");
			stage.show();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Error loading WorkerMenu.fxml");
		}
	}
}
