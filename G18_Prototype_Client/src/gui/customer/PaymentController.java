package gui.customer;

import java.net.URL;
import java.util.ResourceBundle;

import client.ChatClient;
import client.ClientUI;
import common.ActionType;
import common.BistroMessage;
import common.Order;
import common.Role;
import common.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Controller for the Bill Payment Screen.
 * <p>
 * This screen allows customers (Members and Guests) to pay their bill remotely
 * using the Confirmation Code received during booking or joining to the waiting
 * list.
 * </p>
 * <p>
 * <b>Business Rules Implemented:</b>
 * <ul>
 * <li>Payment is identified by Confirmation Code.</li>
 * <li><b>Members:</b> Receive a 10% discount on the total bill.</li>
 * <li><b>Guests:</b> Pay the full price.</li>
 * <li><b>Table Release:</b> Upon successful payment, the table is freed
 * immediately.</li>
 * </ul>
 * </p>
 */
public class PaymentController implements Initializable {

	// Search Section
	@FXML
	private TextField txtConfirmationCode;
	@FXML
	private Label lblSearchError;
	@FXML
	private Button btnSearchOrder;

	// Bill Details Section (Hidden initially)
	@FXML
	private VBox vboxBillDetails;
	@FXML
	private Label lblOrderNumber;
	@FXML
	private Label lblStatusText; // To display "Discount Included"
	@FXML
	private Label lblTotalToPay;

	// Payment Input Section
	@FXML
	private TextField txtCreditCard;
	@FXML
	private TextField txtCVV;
	@FXML
	private TextField txtExpiryDate;

	@FXML
	private Button btnPay;
	@FXML
	private Button btnBack;

	private Order currentOrder = null;


	/**
	 * Initializes the controller class. Hides the bill details section until a
	 * valid order is found.
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		vboxBillDetails.setVisible(false);
		vboxBillDetails.setManaged(false);
		lblSearchError.setText("");
	}

	/**
	 * Step 1: Search for the order/bill by Confirmation Code.
	 * <p>
	 * Sends a request to the server to fetch the order details based on the code provided.
     * </p>
     * @param event The ActionEvent triggered by clicking the search button.
     */
	@FXML
	public void clickSearchOrder(ActionEvent event) {
		lblSearchError.setText("");
		String codeStr = txtConfirmationCode.getText().trim();
		if (codeStr.isEmpty()) {
			lblSearchError.setText("Please enter a confirmation code.");
			return;
		}

		try {
			Integer confirmationCode = Integer.parseInt(codeStr);

            // Send request to server to get order details
            BistroMessage msg = new BistroMessage(ActionType.GET_ORDER_BY_CODE, confirmationCode);
            ClientUI.chat.accept(msg);
            // Check if order exists
            if (ChatClient.order == null) {
                lblSearchError.setText("Order not found or already paid.");
                vboxBillDetails.setVisible(false);
                vboxBillDetails.setManaged(false);
            } else {
                this.currentOrder = ChatClient.order;
                displayBillDetails();
            }
		
		} catch (NumberFormatException e) {
            lblSearchError.setText("Code must be numbers only.");
        } catch (Exception e) {
            e.printStackTrace();
            lblSearchError.setText("Communication error.");
        }
    }
	
	/**
     * Step 2: Display bill details based on Server Calculation.
     * <p>
     * This method updates the UI with the final price received from the server.
     * It also adds a visual indicator if the customer is a Member, confirming that
     * their discount has been applied.
     * </p>
     */	
	private void displayBillDetails() {
        // Show the bill box
        vboxBillDetails.setVisible(true);
        vboxBillDetails.setManaged(true);	
        //Get the FINAL price directly from the Server Order object
        double finalPrice = currentOrder.getTotalPrice();
		
        lblOrderNumber.setText("Order #" + currentOrder.getConfirmationCode());
        lblTotalToPay.setText(String.format("TOTAL TO PAY: $%.2f", finalPrice));
        
        User currentUser = ChatClient.user;
        if (currentUser != null && currentUser.getRole() == Role.MEMBER) {
            // Member found - Display a message
            lblStatusText.setText("Member Discount (10%) included in total.");
        } else {
            // Guest or not logged in
            lblStatusText.setText("Standard Price");
        }
	}
	
	/**
     * Step 3: Process Payment.
     * <p>
     * Sends a request to the server to close the order.
     * The server will handle the logic of freeing the table immediately.
     * </p>
     *
     * @param event The ActionEvent triggered by clicking 'Pay Now'.
     */	
	@FXML
    public void clickPay(ActionEvent event) {
        // Basic validation (Simulation)
        if (txtCreditCard.getText().isEmpty() || txtCVV.getText().isEmpty() || txtExpiryDate.getText().isEmpty()) {
        	showAlert("Validation Error", "Please fill in all credit card details (Number, CVV, Date).");    
        	return;
        }	
		
        try {
            // Send Payment Request to Server
            BistroMessage msg = new BistroMessage(ActionType.PAY_BILL, currentOrder.getConfirmationCode()); 
            ClientUI.chat.accept(msg);

            if (ChatClient.operationSuccess) {
                showAlert("Payment Successful", "Thank you!");
                clickBack(event);
            } else {
                showAlert("Error", "Payment failed. Please try again.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("System Error", "Communication failed.");
        }
	}
	
	/**
     * Displays an information alert to the user.
     * @param title The title of the alert.
     * @param content The message content.
     */
	private void showAlert(String title, String content) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
	
	/**
     * Navigates back to the main User Menu.
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
