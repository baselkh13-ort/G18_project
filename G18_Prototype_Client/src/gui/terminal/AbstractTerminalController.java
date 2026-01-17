package gui.terminal;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.stage.Stage;

/**
 * Abstract base controller for all Terminal screens.
 */
public abstract class AbstractTerminalController {

    /**
     * Closes the window triggered by a Button click (FXML Action).
     * @param event The ActionEvent.
     */
    @FXML
    public void closeWindow(ActionEvent event) {
        try {
            if (event != null && event.getSource() instanceof Node) {
                closeWindow((Node) event.getSource());
            }
        } catch (Exception e) {
            System.err.println("Error closing window from event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Closes the window associated with a specific UI component.
     * Use this method when closing the window programmatically (not from a button click).
     * @param node Any UI element currently displayed in the window (e.g., a Label or Button).
     */
    protected void closeWindow(Node node) {
        try {
            if (node != null && node.getScene() != null) {
                Stage stage = (Stage) node.getScene().getWindow();
                stage.close();
            }
        } catch (Exception e) {
            System.err.println("Error closing window from node: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Updates the status label.
     */
    protected void setStatus(Label label, String msg, boolean isSuccess) {
        if (label == null) return;
        label.setText(msg);
        if (isSuccess) {
            label.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;"); 
        } else {
            label.setStyle("-fx-text-fill: #e74c3c;"); 
        }
    }
}