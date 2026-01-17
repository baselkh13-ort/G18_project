package gui.staff;

import java.net.URL;
import java.time.LocalDate;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;

import client.ChatClient;
import client.ClientUI;
import common.ActionType;
import common.BistroMessage;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;

/**
 * Controller class for the View Reports screen. This screen allows the Manager
 * to view two types of monthly reports: 1. Performance Report (Bar Chart) -
 * Shows delays, arrival times, and customer lateness. 2. Orders Activity Report
 * (Line Chart) - Shows the number of orders and waitlist entries per day. * The
 * data is fetched from the server based on the selected month and year.
 */
public class ViewReportsController implements Initializable {

	// UI Variables
	@FXML
	private ComboBox<String> cmbReportType;
	@FXML
	private ComboBox<Integer> cmbMonth;
	@FXML
	private ComboBox<Integer> cmbYear;
	@FXML
	private Button btnLoadReport;
	@FXML
	private Label lblStatus;
	@FXML
	private Button btnClose;

	// Charts for Performance Report
	@FXML
	private BarChart<String, Number> barChartPerformance;
	@FXML
	private CategoryAxis xAxisPerf;
	@FXML
	private NumberAxis yAxisPerf;

	// Charts for Activity Report
	@FXML
	private LineChart<String, Number> lineChartSubscriptions;
	@FXML
	private CategoryAxis xAxisSub;
	@FXML
	private NumberAxis yAxisSub;

	/**
	 * Called when the screen is initialized. Sets up the report types in the combo
	 * box and sets the default date to the current month.
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// Add report options
		cmbReportType.getItems().addAll("Performance Report", "Orders Activity Report");
		cmbReportType.setValue("Performance Report");

		// Fill days and years
		for (int i = 1; i <= 12; i++)
			cmbMonth.getItems().add(i);

		int currentYear = LocalDate.now().getYear();
		cmbYear.getItems().addAll(currentYear, currentYear - 1, currentYear - 2);

		// Set default values
		cmbMonth.setValue(LocalDate.now().getMonthValue());
		cmbYear.setValue(currentYear);

		// Hide charts at the beginning
		barChartPerformance.setVisible(false);
		lineChartSubscriptions.setVisible(false);
	}

	/**
	 * Main action when the user clicks 'Generate'. Checks if the date is valid and
	 * calls the relevant method to load the data. * @param event The click event.
	 */
	@FXML
	void loadReport(ActionEvent event) {
		String selectedType = cmbReportType.getValue();
		Integer month = cmbMonth.getValue();
		Integer year = cmbYear.getValue();

		// Validation
		if (month == null || year == null) {
			lblStatus.setText("Error: Please select a valid month and year.");
			lblStatus.setStyle("-fx-text-fill: red;");
			return;
		}

		lblStatus.setText("Fetching Report from Server...");
		lblStatus.setStyle("-fx-text-fill: black;");

		int[] dateParam = { month, year };

		// Decide which report to load
		if (selectedType.equals("Performance Report")) {
			handlePerformanceReport(dateParam);
		} else {
			handleActivityReport(dateParam);
		}
	}

	/**
	 * Handles the logic for the Performance Report. Sends a request to the server
	 * and updates the Bar Chart with the data received. It displays metrics like
	 * Average Delay, Lateness, and Stay Duration. * @param date The month and year
	 * array.
	 */
	private void handlePerformanceReport(int[] date) {
		// Setup UI for Bar Chart
		barChartPerformance.setVisible(true);
		lineChartSubscriptions.setVisible(false);
		barChartPerformance.getData().clear();

		ChatClient.performanceReport = null; // Reset data

		System.out.println("CLIENT DEBUG: Requesting Performance Report for " + date[0] + "/" + date[1]);

		// Request from Server
		ClientUI.chat.accept(new BistroMessage(ActionType.GET_PERFORMANCE_REPORT, date));

		// Check if we got a response
		if (ChatClient.performanceReport != null && ChatClient.performanceReport.getDataMap() != null) {
			Map<String, Number> data = ChatClient.performanceReport.getDataMap();
			System.out.println("CLIENT DEBUG: Performance Data Received: " + data);

			if (data.isEmpty()) {
				lblStatus.setText("No data available for this month.");
				return;
			}

			XYChart.Series<String, Number> series = new XYChart.Series<>();
			series.setName("Monthly Metrics");

			// We use TreeMap to sort the keys alphabetically for better display
			TreeMap<String, Number> sortedData = new TreeMap<>(data);

			// Loop over all data from server and add to the chart
			for (Map.Entry<String, Number> entry : sortedData.entrySet()) {
				String key = entry.getKey();
				Number value = entry.getValue();
				series.getData().add(new XYChart.Data<>(key, value));
			}

			barChartPerformance.getData().add(series);

			// Update UI safely
			Platform.runLater(() -> {
				barChartPerformance.layout();
				lblStatus.setText("Performance report loaded successfully.");
				lblStatus.setStyle("-fx-text-fill: green;");
			});

		} else {
			System.out.println("CLIENT DEBUG: Report object is NULL.");
			lblStatus.setText("No response from server.");
		}
	}

	/**
	 * Handles the logic for the Orders Activity Report. Updates the Line Chart with
	 * two lines: one for Orders and one for Waitlist entries. It iterates through
	 * all days of the month (1-31) to build the graph. * @param date The month and
	 * year array.
	 */
	private void handleActivityReport(int[] date) {
		// Setup UI for Line Chart
		lineChartSubscriptions.setVisible(true);
		barChartPerformance.setVisible(false);
		lineChartSubscriptions.getData().clear();

		ChatClient.subscriptionReport = null;

		System.out.println("CLIENT DEBUG: Requesting Activity Report for " + date[0] + "/" + date[1]);

		// Request from Server
		ClientUI.chat.accept(new BistroMessage(ActionType.GET_SUBSCRIPTION_REPORT, date));

		if (ChatClient.subscriptionReport != null && ChatClient.subscriptionReport.getDataMap() != null) {
			Map<String, Number> data = ChatClient.subscriptionReport.getDataMap();
			System.out.println("CLIENT DEBUG: Activity Data Received: " + data);

			XYChart.Series<String, Number> orderSeries = new XYChart.Series<>();
			orderSeries.setName("Total Orders");

			XYChart.Series<String, Number> waitlistSeries = new XYChart.Series<>();
			waitlistSeries.setName("Waitlist Entries");

			boolean hasData = false;

			// Loop 1 to 31 to ensure the X-Axis is in correct order
			for (int i = 1; i <= 31; i++) {
				String dayKey = String.valueOf(i); // Example: "1"
				String waitKey = "W-" + i; // Example: "W-1" for waitlist data

				// Get value or 0 if empty
				Number ordersCount = data.getOrDefault(dayKey, 0);
				Number waitlistCount = data.getOrDefault(waitKey, 0);

				if (ordersCount.intValue() > 0 || waitlistCount.intValue() > 0) {
					hasData = true;
				}

				// Add points to the graph
				orderSeries.getData().add(new XYChart.Data<>(dayKey, ordersCount));
				waitlistSeries.getData().add(new XYChart.Data<>(dayKey, waitlistCount));
			}

			if (!hasData) {
				lblStatus.setText("No activity found for this month.");
			} else {
				lineChartSubscriptions.getData().addAll(orderSeries, waitlistSeries);

				Platform.runLater(() -> {
					lineChartSubscriptions.layout();
					lblStatus.setText("Activity report loaded successfully.");
					lblStatus.setStyle("-fx-text-fill: green;");
				});
			}
		} else {
			lblStatus.setText("No response from server.");
		}
	}

	/**
	 * Closes the current window.
	 * 
	 * @param event The click event.
	 */
	@FXML
	void closeWindow(ActionEvent event) {
		((Stage) btnClose.getScene().getWindow()).close();
	}
}