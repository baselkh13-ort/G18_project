package gui.staff;

import java.net.URL;
import java.time.LocalTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.ResourceBundle;

import client.ClientUI;
import common.ActionType;
import common.BistroMessage;
import common.OpeningHour;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.stage.Stage;

/**
 * Controller for editing the restaurant's opening hours.
 * Handles two types of updates:
 * 1. Recurring weekly hours (e.g., Every Sunday).
 * 2. Specific date exceptions (e.g., Holidays).
 */
public class EditOpeningHoursController implements Initializable {

    // --- Regular Weekly Hours UI ---
    @FXML private ComboBox<String> cmbDayOfWeek;
    @FXML private ComboBox<String> cmbStartHourRegular;
    @FXML private ComboBox<String> cmbEndHourRegular;
    @FXML private CheckBox chkRegularClosed;
    @FXML private Button btnUpdateRegular;

    // --- Specific Date UI ---
    @FXML private DatePicker dpSpecificDate;
    @FXML private ComboBox<String> cmbStartHourSpecific;
    @FXML private ComboBox<String> cmbEndHourSpecific;
    @FXML private CheckBox chkIsClosed;
    @FXML private Button btnUpdateSpecific;

    @FXML private Button btnClose;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupComboBoxes();
    }

    private void setupComboBoxes() {
        // Populate Days (Sunday = 1, Saturday = 7)
        cmbDayOfWeek.setItems(FXCollections.observableArrayList(
            "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
        ));

        // Populate Hours (00:00 to 23:00)
        ArrayList<String> hours = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            hours.add(String.format("%02d:00", i));
            hours.add(String.format("%02d:30", i)); // Optional: added half hours for precision
        }
        ObservableList<String> hoursObs = FXCollections.observableArrayList(hours);
        
        cmbStartHourRegular.setItems(hoursObs);
        cmbEndHourRegular.setItems(hoursObs);
        cmbStartHourSpecific.setItems(hoursObs);
        cmbEndHourSpecific.setItems(hoursObs);
    }

    /**
     * Toggles regular time fields based on the 'Closed All Day' checkbox.
     */
    @FXML
    void toggleRegularTimeFields(ActionEvent event) {
        boolean isClosed = chkRegularClosed.isSelected();
        cmbStartHourRegular.setDisable(isClosed);
        cmbEndHourRegular.setDisable(isClosed);
    }

    /**
     * Updates the recurring opening hours for a specific day of the week.
     */
    @FXML
    void updateRegularHours(ActionEvent event) {
        String selectedDay = cmbDayOfWeek.getValue();
        boolean isClosed = chkRegularClosed.isSelected();

        if (selectedDay == null) {
            showAlert("Error", "Please select a day of the week.");
            return;
        }

        int dayIndex = cmbDayOfWeek.getSelectionModel().getSelectedIndex() + 1; // 1-7
        OpeningHour newHour;

        if (isClosed) {
            // Case A: Closed all day (Every Saturday for example)
            newHour = new OpeningHour(dayIndex, true);
        } else {
            // Case B: Open with specific hours
            String startStr = cmbStartHourRegular.getValue();
            String endStr = cmbEndHourRegular.getValue();

            if (startStr == null || endStr == null) {
                showAlert("Error", "Please select start and end times.");
                return;
            }

            LocalTime start = LocalTime.parse(startStr);
            LocalTime end = LocalTime.parse(endStr);

            if (start.isAfter(end) || start.equals(end)) {
                showAlert("Invalid Time", "End time must be after start time.");
                return;
            }
            newHour = new OpeningHour(dayIndex, start, end);
        }

        sendUpdateToServer(newHour);
    }

    /**
     * Updates hours for a specific date (overrides regular hours).
     */
    @FXML
    void updateSpecificDate(ActionEvent event) {
        LocalDate date = dpSpecificDate.getValue();
        boolean isClosed = chkIsClosed.isSelected();

        if (date == null) {
            showAlert("Error", "Please select a date.");
            return;
        }
        
        if (date.isBefore(LocalDate.now())) {
             showAlert("Error", "Cannot update past dates.");
             return;
        }

        OpeningHour newHour;
        
        if (isClosed) {
            // Mark as closed for the whole day
            newHour = new OpeningHour(java.sql.Date.valueOf(date), true);
        } else {
            String startStr = cmbStartHourSpecific.getValue();
            String endStr = cmbEndHourSpecific.getValue();

            if (startStr == null || endStr == null) {
                showAlert("Error", "Please select times or mark as 'Closed'.");
                return;
            }
            
            LocalTime start = LocalTime.parse(startStr);
            LocalTime end = LocalTime.parse(endStr);
            
            if (start.isAfter(end)) {
                showAlert("Invalid Time", "End time must be after start time.");
                return;
            }
            
            newHour = new OpeningHour(java.sql.Date.valueOf(date), start, end);
        }

        sendUpdateToServer(newHour);
    }
    
    /**
     * Toggles specific time fields based on the 'Closed' checkbox.
     */
    @FXML
    void toggleSpecificTimeFields(ActionEvent event) {
        boolean isClosed = chkIsClosed.isSelected();
        cmbStartHourSpecific.setDisable(isClosed);
        cmbEndHourSpecific.setDisable(isClosed);
    }

    private void sendUpdateToServer(OpeningHour hour) {
        BistroMessage msg = new BistroMessage(ActionType.UPDATE_OPENING_HOURS, hour);
        ClientUI.chat.accept(msg);
        showAlert("Update Sent", "The request has been processed successfully.");
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    void closeWindow(ActionEvent event) {
        ((Stage) btnClose.getScene().getWindow()).close();
    }
}