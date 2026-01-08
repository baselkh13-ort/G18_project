package gui.staff;

import client.ChatClient;
import client.ClientUI;
import common.ActionType;
import common.BistroMessage;
import common.Member;
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
public class RegisterMemberController {

	@FXML
	private TextField txtUserName;
	@FXML
	private TextField txtName;
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

		lblStatus.setText("");
		lblStatus.setStyle("-fx-text-fill: black;");

		String userName = txtUserName.getText().trim();
		String name = txtName.getText().trim();
		String phone = txtPhone.getText().trim();
		String email = txtEmail.getText().trim();

		if (userName.isEmpty()) {
			lblStatus.setText("Please enter a user-name.");
			lblStatus.setStyle("-fx-text-fill: red;");
			return; // we will stop here
		}

		if (name.isEmpty()) {
			lblStatus.setText("Please enter a name.");
			lblStatus.setStyle("-fx-text-fill: red;");

		}

		if (phone.isEmpty()) {
			lblStatus.setText("Please enter a phone-number.");
			lblStatus.setStyle("-fx-text-fill: red;");
			return;
		}

		if (phone.length() != 10 || !phone.startsWith("05") || !phone.matches("\\d+")) {
			lblStatus.setText("Phone must start with '05' and be 10 digits.");
			lblStatus.setStyle("-fx-text-fill: red;");
			return;
		}

		if (email.isEmpty()) {
			lblStatus.setText("Please enter an Email Address.");
			lblStatus.setStyle("-fx-text-fill: red;");
			return;
		}

		if (!email.contains("@") || !email.contains(".")) {
			lblStatus.setText("Invalid email format (must contain @ and .).");
			lblStatus.setStyle("-fx-text-fill: red;");
			return;
		}

		// if we are here it means that all the fields are valid
		Member req = new Member(null, null, userName, name, phone, email);
		ChatClient.registeredMember = null;
		ChatClient.registerError = null;

		new Thread(() -> {
			ClientUI.chat.accept(new BistroMessage(ActionType.REGISTER_NEW_MEMBER, req));
			Platform.runLater(() -> {
				if (ChatClient.registeredMember != null) {
					String newId = ChatClient.registeredMember.getMemberId();
					lblStatus.setText("Success! Member ID: " + newId);
					lblStatus.setStyle("-fx-text-fill: green;");

					showSimulationAlert(name, phone, newId);

					clearFields();
				} else if (ChatClient.registerError != null) {
					lblStatus.setText("❌ " + ChatClient.registerError);
					lblStatus.setStyle("-fx-text-fill: red;");
				} else {
					lblStatus.setText("❌ Server did not respond.");
					lblStatus.setStyle("-fx-text-fill: red;");
				}
			});

		}).start();
	}

	private void showSimulationAlert(String name, String phone, String code) {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("Simulation: Message Sent");
		alert.setHeaderText("Simulating SMS to: " + phone);
		alert.setContentText("Hello " + name + ",\n" + "Welcome to Bistro!\n" + "Your Member Code is: " + code + "\n\n"
				+ "(Please save this code to identify)");
		alert.showAndWait();
	}

	private void clearFields() {
		txtUserName.clear();
		txtName.clear();
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
