package gui.utils;

import java.net.URL;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.ResourceBundle;

import client.ChatClient;
import client.ClientUI;
import common.ActionType;
import common.BistroMessage;
import common.OpeningHour;
import common.Order;
import common.Role;
import gui.customer.UserMenuController;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;

/**
 * Controller for the Order Creation Screen.
 *
 * This class implements the Client-Side logic for booking a table.
 * It adheres to the Client-Server architecture by requesting data (Opening Hours, Availability)
 * from the server and rendering the UI based on the response.
 *
 * Key Features:
 * - Visual Feedback: Closed days are rendered in RED in the DatePicker.
 * - Availability Display: All operating hours are shown, but fully booked slots are disabled (grayed out).
 * - Alternative Suggestions: If a user tries to book a full slot, the server suggests alternatives.
 */
public class OrderCreationController implements Initializable {

    //FXML Components
    @FXML private DatePicker dpDate;
    @FXML private ComboBox<String> cmbTime;
    @FXML private TextField txtGuests;

    // Fields for guest details (Walk-in / non-member)
    @FXML private VBox vboxCasualDetails;
    @FXML private TextField txtName;
    @FXML private TextField txtPhone;
    @FXML private TextField txtEmail;

    @FXML private Label lblStatus;
    @FXML private Button btnSubmit;
    @FXML private Button btnBack;

    /**
     * Initializes the controller class.
     * Sets up UI components, event listeners, and fetches initial data from the server.
     *
     * @param location  The location used to resolve relative paths for the root object.
     * @param resources The resources used to localize the root object.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Reset UI components
        btnSubmit.setDisable(true);
        cmbTime.setDisable(true);

        // 2. Request opening hours from the server
        // This is essential for the DatePicker to know which days to mark as closed.
        BistroMessage msg = new BistroMessage(ActionType.GET_OPENING_HOURS, null);
        ClientUI.chat.accept(msg);

        // 3. Adjust UI visibility based on User Role (Member vs Guest)
        if (ChatClient.user != null && ChatClient.user.getRole() == Role.MEMBER) {
            vboxCasualDetails.setVisible(false);
            vboxCasualDetails.setManaged(false);
        } else {
            vboxCasualDetails.setVisible(true);
            vboxCasualDetails.setManaged(true);
        }

        // --- 4. DATE PICKER CONFIGURATION ---
        // Sets a custom DayCellFactory to visually disable and color closed days.
        dpDate.setDayCellFactory(new Callback<DatePicker, DateCell>() {
            @Override
            public DateCell call(final DatePicker datePicker) {
                return new DateCell() {
                    @Override
                    public void updateItem(LocalDate item, boolean empty) {
                        super.updateItem(item, empty);
                        
                        // Reset style to prevent visual artifacts during scrolling
                        setStyle(null);
                        setTooltip(null);
                        setDisable(false); 
                        setTextFill(javafx.scene.paint.Color.BLACK); 

                        if (item == null || empty) {
                            setDisable(true);
                            return;
                        }

                        // Determine if the date is valid
                        boolean isDatePassed = item.isBefore(LocalDate.now());
                        boolean isTooFar = item.isAfter(LocalDate.now().plusMonths(1));
                        boolean isClosedDay = isRestaurantClosed(item);

                        // Disable invalid dates
                        if (isDatePassed || isTooFar || isClosedDay) {
                            setDisable(true);

                            // Apply specific styling for CLOSED days (RED background)
                            if (isClosedDay && !isDatePassed) {
                                // -fx-opacity: 1 forces the background color to be visible even if disabled
                                setStyle("-fx-background-color: #ffcccc; -fx-opacity: 1; -fx-text-fill: #c0392b;");
                                setTooltip(new Tooltip("Restaurant Closed"));
                            }
                        }
                    }
                };
            }
        });

        //5. COMBO BOX CONFIGURATION
        // Sets a custom CellFactory to gray out fully booked time slots.
        cmbTime.setCellFactory(lv -> new ListCell<String>() {
            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setDisable(false);
                    setStyle("");
                } else {
                    setText(item);
                    
                    // Check if this specific time slot is available in the list fetched from the server
                    boolean isAvailable = isTimeSlotAvailable(item);
                    
                    if (!isAvailable) {
                        // Visual cue for fully booked slots
                        setDisable(true); 
                        setStyle("-fx-text-fill: #95a5a6; -fx-opacity: 0.6;"); // Gray color
                        setTooltip(new Tooltip("Fully Booked"));
                    } else {
                        // Standard style for available slots
                        setDisable(false);
                        setStyle("-fx-text-fill: black;");
                        setTooltip(null);
                    }
                }
            }
        });
        
        // Prevent keyboard selection of disabled items
        cmbTime.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !isTimeSlotAvailable(newVal)) {
                 cmbTime.getSelectionModel().clearSelection();
                 lblStatus.setText("Time slot " + newVal + " is fully booked.");
            } else {
                btnSubmit.setDisable(newVal == null || newVal.isEmpty());
                if (newVal != null) lblStatus.setText("");
            }
        });

        // 6. Add Listeners to trigger availability checks on change
        dpDate.valueProperty().addListener((obs, oldVal, newVal) -> loadFullDayWithAvailabilityCheck());
        txtGuests.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) loadFullDayWithAvailabilityCheck();
        });
    }

    /**
     * Helper method to check if a specific time exists in the server's available list.
     * @param timeStr The time string (HH:mm) to check.
     * @return true if available, false otherwise.
     */
    private boolean isTimeSlotAvailable(String timeStr) {
        if (ChatClient.availableTimeSlots == null) return false;
        return ChatClient.availableTimeSlots.contains(timeStr);
    }

    /**
     * Checks if the restaurant is closed on a specific date.
     * Logic includes converting Java's DayOfWeek (Mon=1) to the Database's convention (usually Sun=1),
     * ensuring that days like Saturday are correctly identified as closed.
     *
     * @param date The date to check.
     * @return true if the restaurant is closed, false otherwise.
     */
    private boolean isRestaurantClosed(LocalDate date) {
        if (ChatClient.openingHours == null || ChatClient.openingHours.isEmpty()) return false;
        
        // 1. Check for specific date overrides (e.g., Holidays)
        for (OpeningHour hour : ChatClient.openingHours) {
            if (hour.getSpecificDate() != null) {
                LocalDate specific = new java.sql.Date(hour.getSpecificDate().getTime()).toLocalDate();
                if (specific.isEqual(date)) return hour.isClosed();
            }
        }

        // 2. Check for recurring Day of Week rules
        // JavaFX: Monday=1 ... Saturday=6, Sunday=7
        // DB Standard: Sunday=1 ... Saturday=7
        int javaDay = date.getDayOfWeek().getValue(); 
        int dbDay = (javaDay % 7) + 1; // Conversion formula

        for (OpeningHour hour : ChatClient.openingHours) {
            if (hour.getSpecificDate() == null && hour.getDayOfWeek() == dbDay) {
                if (hour.isClosed()) return true;
            }
        }
        
        return false;
    }

    /**
     * Loads the time slots for the selected day.
     * This method:
     * 1. Generates ALL operating hours locally (e.g., 12:00 to 24:00).
     * 2. Queries the server for AVAILABLE hours.
     * 3. Populates the ComboBox with ALL hours. The CellFactory will disable the unavailable ones.
     */
    private void loadFullDayWithAvailabilityCheck() {
        LocalDate localDate = dpDate.getValue();
        String guestsStr = txtGuests.getText().trim();
        
        // Reset UI state
        btnSubmit.setDisable(true);
        cmbTime.getItems().clear();
        cmbTime.setDisable(true);
        cmbTime.setPromptText("Select Date & Guests...");
        lblStatus.setText("");

        if (localDate == null || guestsStr.isEmpty()) return;
        
        // Early exit if closed
        if (isRestaurantClosed(localDate)) {
            cmbTime.setPromptText("Closed");
            lblStatus.setText("Restaurant is closed on this date.");
            return;
        }

        try {
            int guests = Integer.parseInt(guestsStr);
            if (guests <= 0) { lblStatus.setText("Guests must be at least 1."); return; }

            // Step A: Request available slots from Server
            ChatClient.availableTimeSlots = null;
            java.sql.Date sqlDate = java.sql.Date.valueOf(localDate);
            ArrayList<Object> params = new ArrayList<>();
            params.add(sqlDate);
            params.add(guests);

            BistroMessage msg = new BistroMessage(ActionType.GET_AVAILABLE_TIMES, params);
            ClientUI.chat.accept(msg); 

            if (ChatClient.availableTimeSlots == null) { 
                lblStatus.setText("Server connection error."); 
                return; 
            }

            // Step B: Calculate local Open/Close times for the list generation
            LocalTime openTime = LocalTime.of(12, 0); // Default fallback
            LocalTime closeTime = LocalTime.of(23, 59); // Default fallback
            
            // Logic to find exact opening hours for this specific day
            if (ChatClient.openingHours != null) {
                boolean foundSpecific = false;
                // Check specific date
                for (OpeningHour oh : ChatClient.openingHours) {
                    if (oh.getSpecificDate() != null) {
                        LocalDate dbDate = new java.sql.Date(oh.getSpecificDate().getTime()).toLocalDate();
                        if (dbDate.isEqual(localDate)) {
                            openTime = oh.getOpenTime().toLocalTime();
                            closeTime = oh.getCloseTime().toLocalTime();
                            foundSpecific = true;
                            break;
                        }
                    }
                }
                // Check day of week
                if (!foundSpecific) {
                    int javaDay = localDate.getDayOfWeek().getValue();
                    int dbDay = (javaDay % 7) + 1;
                    
                    for (OpeningHour oh : ChatClient.openingHours) {
                        if (oh.getSpecificDate() == null && oh.getDayOfWeek() == dbDay) {
                            openTime = oh.getOpenTime().toLocalTime();
                            closeTime = oh.getCloseTime().toLocalTime();
                            break;
                        }
                    }
                }
            }

            // Step C: Generate list of ALL slots (Open to Close)
            ArrayList<String> allDaySlots = new ArrayList<>();
            LocalTime current = openTime;
            while (current.isBefore(closeTime) || current.equals(closeTime)) {
                String timeStr = current.format(DateTimeFormatter.ofPattern("HH:mm"));
                allDaySlots.add(timeStr);
                current = current.plusMinutes(30);
                if (current.equals(LocalTime.of(0, 0))) break; // Handle midnight
            }

            // Step D: Populate ComboBox
            cmbTime.setItems(FXCollections.observableArrayList(allDaySlots));
            cmbTime.setDisable(false);
            cmbTime.setPromptText("Select Time");

        } catch (NumberFormatException e) {
            lblStatus.setText("Invalid guest number.");
        }
    }

    /**
     * Handles the Submit button click to create a new order.
     * Sends the order request to the server. If the slot was taken during the process (Race Condition),
     * handles the "Order Alternatives" response by showing a warning and suggesting new times.
     *
     * @param event The ActionEvent triggered by the button.
     */
    @FXML
    public void clickSubmit(ActionEvent event) {
        lblStatus.setText("");
        if (cmbTime.getValue() == null) {
            lblStatus.setText("Please select a time.");
            return;
        }

        try {
            // 1. Parse Input Data
            LocalDate date = dpDate.getValue();
            String timeStr = cmbTime.getValue();
            LocalTime time = LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));
            LocalDateTime requestedDateTime = LocalDateTime.of(date, time);
            int guests = Integer.parseInt(txtGuests.getText());

            String name = "", phone = "", email = "";
            int memberId = 0;

            if (ChatClient.user != null && ChatClient.user.getRole() == Role.MEMBER) {
                memberId = ChatClient.user.getUserId();
                name = ChatClient.user.getFirstName() + " " +  ChatClient.user.getLastName();
                phone = ChatClient.user.getPhone();
                email = ChatClient.user.getEmail();
            } else {
                // Guest handling
                name = txtName.getText().trim();
                phone = txtPhone.getText().trim();
                email = txtEmail.getText().trim();
                if (name.isEmpty() || (phone.isEmpty() && email.isEmpty())) {
                    lblStatus.setText("Please fill in contact details.");
                    return;
                }
            }

            // 2. Create Order Object
            Timestamp ts = Timestamp.valueOf(requestedDateTime);
            Order orderRequest = new Order(ts, guests, name, phone, email);
            if (memberId != 0) orderRequest.setMemberId(memberId);

            // 3. Send Request to Server
            ChatClient.isAlternativeTime = false; // Reset flag before request
            BistroMessage msg = new BistroMessage(ActionType.CREATE_ORDER, orderRequest);
            ClientUI.chat.accept(msg);

            // 4. Handle Response
            
            // Case A: Success
            if (ChatClient.order != null && ChatClient.order.getConfirmationCode() > 0) {
                showSuccessAlert(ChatClient.order);
                clickBack(event);
            } 
            // Case B: Time Full -> Server suggests alternatives
            else if (ChatClient.isAlternativeTime) {
                Alert alert = new Alert(AlertType.WARNING);
                alert.setTitle("Time Unavailable");
                alert.setHeaderText("The requested time is fully booked.");
                alert.setContentText("We found nearby available times for you.\nPlease choose one from the list.");
                alert.showAndWait();

                if (ChatClient.availableTimeSlots != null) {
                    cmbTime.getItems().clear();
                    cmbTime.setItems(FXCollections.observableArrayList(ChatClient.availableTimeSlots));
                    cmbTime.setPromptText("Select a new time");
                    cmbTime.show(); 
                }
                lblStatus.setText("Please pick an alternative time.");
            } 
            // Case C: General Error
            else {
                lblStatus.setText("Table was taken or Server Error. Try refreshing.");
                loadFullDayWithAvailabilityCheck();
            }

        } catch (Exception e) {
            e.printStackTrace();
            lblStatus.setText("Error processing request.");
        }
    }

    /**
     * Shows a success popup with the confirmation code.
     */
    private void showSuccessAlert(Order order) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Booking Confirmed");
        alert.setHeaderText("Table Reserved Successfully!");
        alert.setContentText("Booking Code: " + order.getConfirmationCode() + "\nPlease present this code upon arrival.");
        alert.showAndWait();
    }

    /**
     * Navigates back to the main menu.
     */
    @FXML
    public void clickBack(ActionEvent event) {
        try {
            ((Node) event.getSource()).getScene().getWindow().hide();
            Stage primaryStage = new Stage();
            UserMenuController menu = new UserMenuController();
            menu.start(primaryStage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}