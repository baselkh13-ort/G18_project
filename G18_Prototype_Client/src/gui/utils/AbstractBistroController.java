package gui.utils;

import client.ChatClient;
import client.ClientUI;
import common.ActionType;
import common.BistroMessage;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

/**
 * Abstract Base Controller.
 * Implements REUSE by centralizing common logic shared across multiple screens,
 * such as Logout functionality and Alert dialogs.
 */
public abstract class AbstractBistroController {

    /**
     * Handles the Logout logic.
     * <p>
     * 1. Updates Server.
     * 2. Clears Client Memory.
     * 3. Redirects to Login Screen.
     * </p>
     * @param event The event triggered by the Logout button.
     */
	public void logout(ActionEvent event) {
        try {
            // 1. Notify Server
            if (ChatClient.user != null) {
                BistroMessage msg = new BistroMessage(ActionType.LOGOUT, ChatClient.user.getUserId());
                ClientUI.chat.accept(msg);
            }

            // 2. Clear Client Memory (Using the helper method in ChatClient)
            ChatClient.resetClientData();

            // 3. Navigate to Login Screen
            ((Node) event.getSource()).getScene().getWindow().hide();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/common/Login.fxml"));
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.setTitle("Bistro - Login");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not return to login screen.");
        }
    }

    /**
     * Reusable helper method to show information alerts.
     * Use this method instead of writing new Alert logic in every controller.
     */
    public void showAlert(String title, String content) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}