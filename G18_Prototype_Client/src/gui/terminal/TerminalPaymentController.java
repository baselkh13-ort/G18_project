package gui.terminal;

import java.net.URL;
import java.util.ResourceBundle;

import client.ChatClient;
import client.ClientUI;
import common.ActionType;
import common.BistroMessage;
import common.Order;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

/**
 * Controller for the Payment & Checkout screen (Terminal Version).
 * <p>
 * This controller adheres to the logic defined in the remote {@code PaymentController},
 * adapted for the physical Terminal interface.
 * </p>
 * <p>
 * <b>Business Rules:</b>
 * <ul>
 * <li><b>Identification:</b> Guests use Code, Members use List/Code.</li>
 * <li><b>Validation:</b> Payment allowed ONLY if status is 'SEATED'.</li>
 * <li><b>Discount:</b> Members receive 10%.</li>
 * <li><b>Action:</b> Uses {@code PAY_BILL} to close the order and free the table.</li>
 * </ul>
 * </p>
 */
public class TerminalPaymentController implements Initializable {

    // --- Identification Section ---
    @FXML private VBox vboxIdentification;
    @FXML private TextField txtOrderCode;       // Input for Guests
    @FXML private ComboBox<Order> cmbMyOrders;  // Selection for Members
    @FXML private Button btnSearch;

    // --- Bill Details Section ---
    @FXML private VBox vboxBillDetails;
    @FXML private Label lblOrderInfo;
    @FXML private Label lblTotalAmount;
    @FXML private Label lblDiscount;
    @FXML private Label lblFinalToPay;
    @FXML private Button btnPayNow;
    
    @FXML private Label lblStatus;
    
    // Holds the currently selected order object
    private Order currentOrderToPay = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Reset UI State
        vboxBillDetails.setVisible(false);
        vboxBillDetails.setManaged(false);
        lblStatus.setText("");

        // 2. Member vs Guest Setup
        if (ChatClient.terminalMember != null) {
            setupMemberView();
        } else {
            setupGuestView();
        }
    }

    private void setupGuestView() {
        txtOrderCode.setVisible(true);
        cmbMyOrders.setVisible(false);
        cmbMyOrders.setManaged(false);
        lblStatus.setText("Guest: Enter Confirmation Code to pay.");
    }

    private void setupMemberView() {
        txtOrderCode.setVisible(false);
        txtOrderCode.setManaged(false);
        cmbMyOrders.setVisible(true);
        lblStatus.setText("Hello " + ChatClient.terminalMember.getFirstName() + ", select an order to pay:");
        
        // ComboBox setup for displaying Orders
        cmbMyOrders.setConverter(new StringConverter<Order>() {
            @Override
            public String toString(Order order) {
                if (order == null) return null;
                return "Order #" + order.getConfirmationCode() + " (Total: " + order.getTotalPrice() + " NIS)";
            }
            @Override
            public Order fromString(String string) { return null; }
        });

        // Load relevant orders
        ClientUI.chat.accept(new BistroMessage(ActionType.GET_RELEVANT_ORDERS, ChatClient.terminalMember.getUserId()));
        
        if (ChatClient.relevantOrders != null && !ChatClient.relevantOrders.isEmpty()) {
            cmbMyOrders.getItems().addAll(ChatClient.relevantOrders);
        } else {
            lblStatus.setText("No active orders found for today.");
        }
        
        // Handle Selection
        cmbMyOrders.setOnAction(e -> {
            Order selected = cmbMyOrders.getValue();
            if (selected != null) {
                fetchBillDetails(selected.getConfirmationCode());
            }
        });
    }

    @FXML
    void searchOrder(ActionEvent event) {
        lblStatus.setText("");
        String codeStr = txtOrderCode.getText().trim();
        
        if (codeStr.isEmpty()) {
            lblStatus.setText("Please enter a confirmation code.");
            lblStatus.setStyle("-fx-text-fill: #e74c3c;"); // Red
            return;
        }
        
        try {
            fetchBillDetails(Integer.parseInt(codeStr));
        } catch (NumberFormatException e) {
            lblStatus.setText("Code must be numbers only.");
            lblStatus.setStyle("-fx-text-fill: #e74c3c;");
        }
    }

    /**
     * Sends request to server to get order details.
     * <p>
     * <b>Strict Logic Adherence:</b> Matches the status validation of {@code PaymentController}.
     * </p>
     */
    private void fetchBillDetails(int confirmationCode) {
        System.out.println("Terminal: Fetching bill for Code: " + confirmationCode);
        
        // 1. Send Request
        ClientUI.chat.accept(new BistroMessage(ActionType.GET_ORDER_BY_CODE, confirmationCode));
        
        // 2. Check Existence
        if (ChatClient.order == null) {
            lblStatus.setText("Order not found.");
            lblStatus.setStyle("-fx-text-fill: #e74c3c;");
            vboxBillDetails.setVisible(false);
            vboxBillDetails.setManaged(false);
        } else {
            currentOrderToPay = ChatClient.order;
            String status = currentOrderToPay.getStatus();
            
            // 3. Status Validation (Copied from PaymentController)
            if ("SEATED".equals(status)) {
                // Valid status -> Proceed to show bill
                showBill(currentOrderToPay);
            } else {
                // Invalid status handling
                vboxBillDetails.setVisible(false);
                vboxBillDetails.setManaged(false);
                
                if ("COMPLETED".equals(status) || "PAID".equals(status)) {
                    lblStatus.setText("This order has already been paid.");
                } else if ("WAITING".equals(status)) {
                    lblStatus.setText("You are not seated yet. Cannot pay.");
                } else {
                    lblStatus.setText("Cannot pay. Order status: " + status);
                }
                lblStatus.setStyle("-fx-text-fill: #e74c3c;");
            }
        }
    }

    /**
     * Displays the bill details.
     * <p>
     * Provides a visual breakdown of the price and discount.
     * </p>
     */
    private void showBill(Order order) {
        vboxBillDetails.setVisible(true);
        vboxBillDetails.setManaged(true);
        lblStatus.setText(""); 
        
        double total = order.getTotalPrice(); 
        double discount = 0;
        
        // Calculate Discount for Display (Terminal UX)
        if (ChatClient.terminalMember != null) {
            discount = total * 0.10;
        }

        double finalPrice = total - discount;

        // Update Labels
        lblOrderInfo.setText("Order #" + order.getConfirmationCode() + " | Table: " + order.getAssignedTableId());
        lblTotalAmount.setText("Subtotal: " + String.format("%.2f", total) + " NIS");
        
        if (discount > 0) {
            lblDiscount.setText("Member Discount (10%): -" + String.format("%.2f", discount) + " NIS");
            lblDiscount.setStyle("-fx-text-fill: #2ecc71;"); // Green
        } else {
            lblDiscount.setText("Standard Price (No Discount)");
            lblDiscount.setStyle("-fx-text-fill: black;");
        }
        
        lblFinalToPay.setText("TOTAL TO PAY: " + String.format("%.2f", finalPrice) + " NIS");
    }

    /**
     * Processes the payment.
     * <p>
     * Simulates card validation and sends {@code PAY_BILL}.
     * </p>
     */
    @FXML
    void performPayment(ActionEvent event) {
        if (currentOrderToPay == null) return;

        // 1. Simulation: Validate "Credit Card" (In terminal, this is physical)
        System.out.println("Terminal: Simulating Credit Card validation...");
        // (Assuming hardware validation passed)

        // 2. Send Payment Request
        ClientUI.chat.accept(new BistroMessage(ActionType.PAY_BILL, currentOrderToPay.getConfirmationCode()));

        // 3. Handle Result
        if (ChatClient.operationSuccess) {
            lblStatus.setText("Payment Successful! Thank you.");
            lblStatus.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;"); 
            
            btnPayNow.setDisable(true);
            vboxBillDetails.setDisable(true);
            
            // Close window automatically
            new Thread(() -> {
                try { Thread.sleep(2000); } catch (Exception e) {}
                Platform.runLater(() -> ((Stage) btnPayNow.getScene().getWindow()).close());
            }).start();
            
        } else {
            lblStatus.setText("Payment failed. Please try again.");
            lblStatus.setStyle("-fx-text-fill: #e74c3c;");
        }
    }
    
    @FXML
    void closeWindow(ActionEvent event) {
        ((Stage) btnPayNow.getScene().getWindow()).close();
    }
}