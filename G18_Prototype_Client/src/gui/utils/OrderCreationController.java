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
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;

/**
 * Controller for the Order Creation Screen.
 * <p>
 * This class handles the logic for booking a new table. It manages:
 * <ul>
 * <li>Server communication to fetch opening hours and available time
 * slots.</li>
 * <li>UI validation (blocking invalid dates, requiring guest count).</li>
 * <li>Visual blocking of closed dates in the DatePicker.</li>
 * <li>Sending the final booking request to the server.</li>
 * </ul>
 * </p>
 */
public class OrderCreationController implements Initializable {

	@FXML
	private DatePicker dpDate;
	@FXML
	private ComboBox<String> cmbTime;
	@FXML
	private TextField txtGuests;

	// for guest to fill
	@FXML
	private VBox vboxCasualDetails;
	@FXML
	private TextField txtName;
	@FXML
	private TextField txtPhone;
	@FXML
	private TextField txtEmail;

	@FXML
	private Label lblStatus;
	@FXML
	private Button btnSubmit;
	@FXML
	private Button btnBack;

	/**
	 * Initializes the controller class.
	 * <p>
	 * This method: 1. Requests the opening hours from the server to configure the
	 * calendar. 2. Adjusts the UI based on the user's role (Member vs. Guest). 3.
	 * Configures the DatePicker to visually block and disable closed dates. 4. Sets
	 * up listeners for user input changes.
	 * </p>
	 *
	 * @param location  The location used to resolve relative paths for the root
	 *                  object.
	 * @param resources The resources used to localize the root object.
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// Reset UI components
		btnSubmit.setDisable(true);
		cmbTime.setDisable(true);

		// Request opening hours from the server to populate ChatClient.openingHours
		// This is critical for the DatePicker validation logic.
		BistroMessage msg = new BistroMessage(ActionType.GET_OPENING_HOURS, null);
		ClientUI.chat.accept(msg);

		// Adjust UI visibility based on user role
		if (ChatClient.user != null && ChatClient.user.getRole() == Role.MEMBER) {
			vboxCasualDetails.setVisible(false);
			vboxCasualDetails.setManaged(false);
		} else {
			vboxCasualDetails.setVisible(true);
			vboxCasualDetails.setManaged(true);
		}

		// Configure the DatePicker's DayCellFactory to disable invalid dates
		dpDate.setDayCellFactory(new Callback<DatePicker, DateCell>() {
			@Override
			public DateCell call(final DatePicker datePicker) {
				return new DateCell() {
					@Override
					public void updateItem(LocalDate item, boolean empty) {
						super.updateItem(item, empty);

						if (item == null || empty) {
							setDisable(true);
							return;
						}

						// Disable dates in the past or more than a month in the future
						boolean isDatePassed = item.isBefore(LocalDate.now());
						boolean isTooFar = item.isAfter(LocalDate.now().plusMonths(1));

						// Check if the restaurant is closed on this specific date
						boolean isClosedDay = isRestaurantClosed(item);

						if (isDatePassed || isTooFar || isClosedDay) {
							setDisable(true);

							// Style closed future dates in red with a tooltip
							if (isClosedDay && !isDatePassed) {
								setStyle("-fx-background-color: #ffcccc;");
								setTooltip(new Tooltip("Restaurant Closed"));
							}
						}
					}
				};
			}
		});

		// Add listeners to trigger availability checks
		dpDate.valueProperty().addListener((obs, oldVal, newVal) -> loadAvailableHours());

		txtGuests.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
			if (!isNowFocused)
				loadAvailableHours();
		});

		// Enable the submit button only when a time is selected
		cmbTime.valueProperty().addListener((obs, oldVal, newVal) -> {
			btnSubmit.setDisable(newVal == null || newVal.isEmpty());
		});
	}

	/**
	 * Checks if the restaurant is closed on a specific date.
	 * <p>
	 * It checks against the {@code ChatClient.openingHours} list, prioritizing
	 * specific closed dates (e.g., holidays) over general day-of-week rules.
	 * </p>
	 *
	 * @param date The date to check.
	 * @return {@code true} if the restaurant is closed, {@code false} otherwise.
	 */
	private boolean isRestaurantClosed(LocalDate date) {
		// If data hasn't arrived or list is empty, assume open to avoid blocking
		// everything
		if (ChatClient.openingHours == null || ChatClient.openingHours.isEmpty()) {
			return false;
		}

		// 1. Check for specific closed dates (holidays)
		for (OpeningHour hour : ChatClient.openingHours) {
			if (hour.getSpecificDate() != null) {
				LocalDate specific = new java.sql.Date(hour.getSpecificDate().getTime()).toLocalDate();
				if (specific.isEqual(date)) {
					return hour.isClosed();
				}
			}
		}

		// 2. Check for recurring closed days (e.g., Saturday)
		// Assumption: Database uses 1 for Sunday. Adjust calculation if necessary.
		int dayOfWeekDB = (date.getDayOfWeek().getValue() % 7) + 1;

		for (OpeningHour hour : ChatClient.openingHours) {
			if (hour.getSpecificDate() == null && hour.getDayOfWeek() == dayOfWeekDB) {
				if (hour.isClosed())
					return true;
			}
		}
		return false;
	}

	/**
	 * Sends a request to the server to get available time slots for the selected
	 * date and guest count.
	 * <p>
	 * This method handles specific server responses:
	 * <ul>
	 * <li>"CLOSED": The restaurant is closed.</li>
	 * <li>"FULL": The restaurant is fully booked.</li>
	 * <li>List of times: Updates the ComboBox with available slots.</li>
	 * </ul>
	 * </p>
	 */
	private void loadAvailableHours() {
		LocalDate localDate = dpDate.getValue();
		String guestsStr = txtGuests.getText().trim();

		// Reset UI state before request
		btnSubmit.setDisable(true);
		cmbTime.getItems().clear();
		cmbTime.setDisable(true);
		cmbTime.setPromptText("Select Date & Guests...");
		lblStatus.setText("");

		if (localDate == null || guestsStr.isEmpty())
			return;

		try {
			int guests = Integer.parseInt(guestsStr);
			if (guests <= 0) {
				lblStatus.setText("Guests must be at least 1.");
				return;
			}

			// Prepare request parameters
			java.sql.Date sqlDate = java.sql.Date.valueOf(localDate);
			Object[] params = new Object[2];
			params[0] = sqlDate;
			params[1] = guests;

			// Send GET_AVAILABLE_TIMES request
			BistroMessage msg = new BistroMessage(ActionType.GET_AVAILABLE_TIMES, params);
			ClientUI.chat.accept(msg);

			ArrayList<String> slots = ChatClient.availableTimeSlots;

			if (slots == null) {
				lblStatus.setText("Server error.");
				return;
			}

			// Handle "CLOSED" response
			if (!slots.isEmpty() && slots.get(0).equals("CLOSED")) {
				cmbTime.setPromptText("Closed");
				lblStatus.setText("Restaurant is closed on this date.");
				return;
			}

			// Handle "FULL" response or empty list
			if (slots.isEmpty() || (slots.size() == 1 && slots.get(0).equals("FULL"))) {
				cmbTime.setPromptText("Fully Booked");
				lblStatus.setText("Fully booked! Try another date.");
				return;
			}

			// Populate available times
			cmbTime.setItems(FXCollections.observableArrayList(slots));
			cmbTime.setDisable(false);
			cmbTime.setPromptText("Select Time");

		} catch (NumberFormatException e) {
			lblStatus.setText("Invalid guest number.");
		}
	}

	/**
	 * Handles the submit button click.
	 * <p>
	 * Collects all input data, validates it, constructs an {@link Order} object,
	 * and sends a {@code CREATE_ORDER} request to the server.
	 * </p>
	 *
	 * @param event The event triggered by the button click.
	 */
	@FXML
	public void clickSubmit(ActionEvent event) {
		lblStatus.setText("");
		if (cmbTime.getValue() == null) {
			lblStatus.setText("Please select a time.");
			return;
		}

		try {
			// Parse Date and Time
			LocalDate date = dpDate.getValue();
			String timeStr = cmbTime.getValue();
			LocalTime time = LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));
			LocalDateTime requestedDateTime = LocalDateTime.of(date, time);
			int guests = Integer.parseInt(txtGuests.getText());

			String name = "", phone = "", email = "";
			int memberId = 0;

			// Gather user details based on role
			if (ChatClient.user != null && ChatClient.user.getRole() == Role.MEMBER) {
				memberId = ChatClient.user.getUserId();
				name = ChatClient.user.getFirstName() + ChatClient.user.getLastName();
				phone = ChatClient.user.getPhone();
				email = ChatClient.user.getEmail();
			} else {
				name = txtName.getText().trim();
				phone = txtPhone.getText().trim();
				email = txtEmail.getText().trim();
				if (name.isEmpty()) {
					lblStatus.setText("Full Name required.");
					return;
				}
				if (phone.isEmpty() && email.isEmpty()) {
					lblStatus.setText("Contact info required.");
					return;
				}
			}

			// Create Order object
			Timestamp ts = Timestamp.valueOf(requestedDateTime);
			Order orderRequest = new Order(ts, guests, name, phone, email);
			if (memberId != 0)
				orderRequest.setMemberId(memberId);

			// Send CREATE_ORDER request
			BistroMessage msg = new BistroMessage(ActionType.CREATE_ORDER, orderRequest);
			ClientUI.chat.accept(msg);

			// Check for successful creation
			if (ChatClient.order != null && ChatClient.order.getConfirmationCode() > 0) {
				showSuccessAlert(ChatClient.order);
				clickBack(event);
			} else {
				// Handle race condition where the table was taken during the process
				lblStatus.setText("Table was just taken! Refreshing...");
				loadAvailableHours();
			}

		} catch (Exception e) {
			e.printStackTrace();
			lblStatus.setText("Error processing request.");
		}
	}

	/**
	 * Displays a success alert with the booking details.
	 *
	 * @param order The created order object containing the confirmation code.
	 */
	private void showSuccessAlert(Order order) {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("Booking Confirmed");
		alert.setHeaderText("Table Reserved Successfully!");
		alert.setContentText(
				"Booking Code: " + order.getConfirmationCode() + "\n" + "Please present this code upon arrival.");
		alert.showAndWait();
	}

	/**
	 * Navigates back to the main user menu.
	 *
	 * @param event The event triggered by the back button.
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