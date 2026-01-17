package gui.staff;

import client.ChatClient;
import client.ClientUI;
import common.ActionType;
import common.BistroMessage;
import common.Role;
import common.User;
import gui.customer.MemberCardController; 

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
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Controller class for the Client Registration screen.
 * <p>
 * Handles the registration flow: Validates input, registers user, 
 * shows simulation SMS, displays the Digital Card, and redirects back to the menu.
 * </p>
 */
public class RegisterClientController {

    @FXML private TextField txtUserName;
    @FXML private TextField txtPassword;
    @FXML private TextField txtUserID;
    @FXML private TextField txtFirstName;
    @FXML private TextField txtLastName;
    @FXML private TextField txtPhone;
    @FXML private TextField txtEmail;
    @FXML private Label lblStatus;

    /**
     * Handles the registration submission.
     * <p>
     * If registration is successful:
     * 1. Shows SMS simulation alert.
     * 2. Opens the Digital Card popup (and waits for it to close).
     * 3. Navigates back to the main menu automatically.
     * </p>
     * @param event The button click event.
     */
    @FXML
    public void clickSubmit(ActionEvent event) {
        
        // --- Input Gathering & Validation ---
        String userName = txtUserName.getText().trim();
        String password = txtPassword.getText().trim();
        String idStr = txtUserID.getText().trim();
        String firstName = txtFirstName.getText().trim();
        String lastName = txtLastName.getText().trim();
        String phone = txtPhone.getText().trim();
        String email = txtEmail.getText().trim();

        if (userName.isEmpty() || password.isEmpty() || idStr.isEmpty() || 
            firstName.isEmpty() || lastName.isEmpty() || phone.isEmpty() || email.isEmpty()) {
            setError("All fields are required.");
            return;
        }
        
        if (!idStr.matches("\\d+")) { 
            setError("User ID must contain only digits.");
            return;
        }
        
        if (phone.length() != 10 || !phone.startsWith("05") || !phone.matches("\\d+")) {
            setError("Phone must start with '05' and be 10 digits.");
            return;
        }

        if (!email.contains("@") || !email.contains(".")) {
            setError("Invalid email format.");
            return;
        }

        // --- Server Communication ---
        int userID = Integer.parseInt(idStr);
        ChatClient.registeredUser = null; 
        
        User newUser = new User(userID, userName, password, firstName, lastName, Role.MEMBER, phone, email); 
        BistroMessage msg = new BistroMessage(ActionType.REGISTER_CLEINT, newUser);
        ClientUI.chat.accept(msg);

        // --- Response Handling ---
        if (ChatClient.registeredUser != null) {
            User savedUser = ChatClient.registeredUser;
            
            // 1. Show SMS Simulation
            showSimulationAlert(savedUser.getFirstName(), savedUser.getPhone(), savedUser.getMemberCode());
            
            // 2. Open Digital Card (BLOCKING call - code pauses here until card is closed)
            openMemberCardWindow(savedUser);
            
            // 3. Return to Main Menu (This runs only after the card is closed)
            clickBack(event);
            
        } else {
            setError("Server failed to register client (returned null).");
        }
    }
    
    /**
     * Opens the Digital Member Card in a modal window.
     * <p>
     * Uses {@code showAndWait()} to block execution until the user closes the card.
     * </p>
     * @param user The registered user details.
     */
    private void openMemberCardWindow(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/customer/MemberCard.fxml"));
            Parent root = loader.load();

            MemberCardController cardController = loader.getController();
            cardController.initData(user); 

            Stage stage = new Stage();
            stage.setTitle("Digital Member Card");
            stage.setScene(new Scene(root));
            
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.initModality(Modality.APPLICATION_MODAL); 

            // Allow dragging
            root.setOnMousePressed(event -> {
                root.setOnMouseDragged(e -> {
                    stage.setX(e.getScreenX() - event.getSceneX());
                    stage.setY(e.getScreenY() - event.getSceneY());
                });
            });

            // IMPORTANT: showAndWait() blocks the flow until this window is closed.
            stage.showAndWait(); 

        } catch (Exception e) {
            e.printStackTrace();
            setError("Error opening Member Card window.");
        }
    }

    private void setError(String msg) {
        lblStatus.setText(msg);
        lblStatus.setTextFill(Color.RED);
    }

    private void showSimulationAlert(String name, String phone, int memberCode) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Registration Successful");
        alert.setHeaderText("Client Added Successfully!");
        alert.setContentText("Hello " + name + ",\n" 
                + "User ID: " + txtUserID.getText() + "\n"
                + "Generated Member Code: " + memberCode + "\n\n" 
                + "SMS sent to: " + phone);
        alert.showAndWait();
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