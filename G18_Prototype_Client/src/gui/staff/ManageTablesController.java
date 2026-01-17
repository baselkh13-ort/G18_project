package gui.staff;

import java.net.URL;
import java.util.ResourceBundle;

import client.ChatClient;
import client.ClientUI;
import common.ActionType;
import common.BistroMessage;
import common.Table;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

/**
 * Controller for the Table Management Screen.
 *
 * This class allows the Restaurant Manager to:
 * - Add new tables to the restaurant layout.
 * - Update the capacity of existing tables.
 * - Remove tables from the restaurant.
 *
 * Note: According to the Best Fit algorithm, any modification here
 * triggers a server-side inventory check. If the capacity decreases and future
 * reservations cannot be accommodated, the server is responsible for canceling
 * those orders and notifying the customers.
 */
public class ManageTablesController implements Initializable {

	// FXML Table Components
	@FXML
	private TableView<Table> tblTables;
	@FXML
	private TableColumn<Table, Integer> colTableId;
	@FXML
	private TableColumn<Table, Integer> colCapacity;
	@FXML
	private TableColumn<Table, String> colStatus;

	// FXML Input Components
	@FXML
	private TextField txtTableId;
	@FXML
	private TextField txtCapacity;

	// FXML Buttons
	@FXML
	private Button btnAdd;
	@FXML
	private Button btnUpdate;
	@FXML
	private Button btnRemove;
	@FXML
	private Button btnClose;

	/**
	 * Initializes the controller class.
	 * Sets up the table columns, loads initial data from ChatClient memory, and
	 * adds a listener to the table selection to populate input fields
	 * automatically.
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		setupTable();
		loadData();

		// Listener: When a row is selected, populate the text fields for easier editing
		tblTables.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
			if (newSelection != null) {
				txtTableId.setText(String.valueOf(newSelection.getTableId()));
				txtCapacity.setText(String.valueOf(newSelection.getCapacity()));
				// Lock ID editing when a table is selected
				txtTableId.setEditable(false);
			} else {
				clearFields();
				txtTableId.setEditable(true);
			}
		});
	}

	/**
	 * Configures the TableView columns to map to the {@link Table} entity
	 * properties.
	 */
	private void setupTable() {
		colTableId.setCellValueFactory(new PropertyValueFactory<>("tableId"));
		colCapacity.setCellValueFactory(new PropertyValueFactory<>("capacity"));
		colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
	}

	/**
	 * Loads the table list from the static {@link ChatClient#allTables}.
	 */
	private void loadData() {
		if (ChatClient.allTables != null) {
			ObservableList<Table> data = FXCollections.observableArrayList(ChatClient.allTables);
			tblTables.setItems(data);
		}
	}

	/**
	 * Handles the request to ADD a new table.
	 * Validates input (positive numbers) and checks for duplicate IDs locally
	 * before sending an {@code ADD_TABLE} request to the server.
	 * 
	 * @param event The action event.
	 */
	@FXML
	void clickAdd(ActionEvent event) {
		try {
			String idStr = txtTableId.getText();
			String capStr = txtCapacity.getText();

			if (idStr.isEmpty() || capStr.isEmpty()) {
				showAlert("Missing Input", "Please enter Table ID and Capacity.");
				return;
			}

			int id = Integer.parseInt(idStr);
			int capacity = Integer.parseInt(capStr);

			if (capacity <= 0 || id <= 0) {
				showAlert("Invalid Input", "ID and Capacity must be positive numbers.");
				return;
			}

			// New Validation: Check if ID already exists in local memory
			if (ChatClient.allTables != null) {
				for (Table t : ChatClient.allTables) {
					if (t.getTableId() == id) {
						showAlert("Duplicate Error", "Table ID " + id + " already exists in the system.");
						return; // Stop here, do not send to server
					}
				}
			}

			// If we reached here, the ID is available (locally)
			Table newTable = new Table(id, capacity, "AVAILABLE");

			BistroMessage msg = new BistroMessage(ActionType.ADD_TABLE, newTable);
			ClientUI.chat.accept(msg);

			showAlert("Request Sent", "Table addition request sent to server.");

			refreshData();

		} catch (NumberFormatException e) {
			showAlert("Invalid Input", "Table ID and Capacity must be valid integers.");
		}
	}

	/**
	 * Handles the request to UPDATE an existing table's capacity.
	 * Sends an {@code UPDATE_TABLE} request. The server will check if reducing
	 * capacity conflicts with existing future reservations.
	 * 
	 * @param event The action event.
	 */
	@FXML
	void clickUpdate(ActionEvent event) {
		Table selected = tblTables.getSelectionModel().getSelectedItem();
		if (selected == null) {
			showAlert("Selection Error", "Please select a table from the list to update.");
			return;
		}

		try {

			// Removing an occupied table would leave active orders without a table association.
			if ("OCCUPIED".equalsIgnoreCase(selected.getStatus())) {
				showAlert("Action Denied", "Cannot update an occupied table.\nPlease close the active order first.");
				return;
			}
			int newCapacity = Integer.parseInt(txtCapacity.getText());
			if (newCapacity <= 0) {
				showAlert("Invalid Input", "Capacity must be positive.");
				return;
			}

			// We create a copy or update the object to send to server
			selected.setCapacity(newCapacity);

			BistroMessage msg = new BistroMessage(ActionType.UPDATE_TABLE, selected);
			ClientUI.chat.accept(msg);

			showAlert("Update Sent",
					"Update request sent.\n\nNote: If capacity was reduced, the server will automatically check for conflicts and cancel reservations if necessary.");
			refreshData();

		} catch (NumberFormatException e) {
			showAlert("Invalid Input", "Capacity must be a number.");
		}
	}

	/**
	 * Handles the request to REMOVE a table.
	 * Sends a {@code REMOVE_TABLE} request to the server. 
	 * validation to prevent deleting occupied tables.
	 * 
	 * @param event The action event.
	 */
	@FXML
	void clickRemove(ActionEvent event) {
		Table selected = tblTables.getSelectionModel().getSelectedItem();

		// 1. Validation: Check if a table is selected
		if (selected == null) {
			showAlert("Selection Error", "Please select a table to remove.");
			return;
		}

		// 2. Validation: Prevent removal of occupied tables
		// Removing an occupied table would leave active orders without a table
		// association.
		if ("OCCUPIED".equalsIgnoreCase(selected.getStatus())) {
			showAlert("Action Denied", "Cannot remove an occupied table.\nPlease close the active order first.");
			return;
		}

		// 3. Send Request: Send only the Table ID to the server
		int tableIdToRemove = selected.getTableId();
		BistroMessage msg = new BistroMessage(ActionType.REMOVE_TABLE, tableIdToRemove);
		ClientUI.chat.accept(msg);

		showAlert("Request Sent", "Remove request sent for Table ID: " + tableIdToRemove
				+ ".\n\nNote: Future reservations might be affected.");

		refreshData();
	}

	/**
	 * Clears the input fields and resets the selection state.
	 */
	@FXML
	void clickClearSelection(ActionEvent event) {
		tblTables.getSelectionModel().clearSelection();
		clearFields();
		txtTableId.setEditable(true);
	}

	/**
	 * Refreshes the table data by requesting the updated list from the server.
	 */
	public void refreshData() {
		// Clear selection and inputs
		clickClearSelection(null);

		// Re-fetch data
		BistroMessage msg = new BistroMessage(ActionType.GET_ALL_TABLES, null);
		ClientUI.chat.accept(msg);

		// Reload UI
		loadData();
	}

	private void clearFields() {
		txtTableId.clear();
		txtCapacity.clear();
	}

	private void showAlert(String title, String content) {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle(title);
		alert.setContentText(content);
		alert.showAndWait();
	}

	@FXML
	void closeWindow(ActionEvent event) {
		((Stage) btnClose.getScene().getWindow()).close();
	}
}