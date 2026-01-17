package gui.customer;

import common.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import java.util.Random;

/**
 * Controller class for the Digital Member Card screen.
 * <p>
 * Displays member details including Username, Phone, Email, and generates a QR simulation.
 * </p>
 */
public class MemberCardController {

    // --- FXML Components ---
    
    @FXML private Label lblFullName;
    @FXML private Label lblUsername; // New
    @FXML private Label lblMemberID;
    @FXML private Label lblPhone;    // New
    @FXML private Label lblEmail;    // New
    @FXML private Label lblMemberCode;
    @FXML private Canvas qrCanvas;

    /**
     * Initializes the member card with the provided user data.
     * @param user The {@link User} object containing the member's details.
     */
    public void initData(User user) {
        if (user != null) {
            // Basic Info
            lblFullName.setText(user.getFirstName() + " " + user.getLastName());
            lblUsername.setText("(" + user.getUsername() + ")"); // Display username
            
            // Contact & ID Info
            lblMemberID.setText(String.valueOf(user.getUserId()));
            lblPhone.setText(user.getPhone());
            lblEmail.setText(user.getEmail());

            // Member Code Logic
            if (user.getMemberCode() == 0) {
                // Handle case where code hasn't synced from server yet
                lblMemberCode.setText("Pending...");
            } else {
                lblMemberCode.setText(String.valueOf(user.getMemberCode()));
            }

            // Generate the visual QR simulation
            drawSimulationQR();
        }
    }

    /**
     * Draws a random pattern on the canvas to simulate a QR code.
     */
    private void drawSimulationQR() {
        GraphicsContext gc = qrCanvas.getGraphicsContext2D();
        double width = qrCanvas.getWidth();
        double height = qrCanvas.getHeight();
        
        // Clear background
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, width, height);
        
        // Draw random "pixels"
        gc.setFill(Color.BLACK);
        int pixelSize = 6;
        Random rand = new Random();

        for (int y = 0; y < height; y += pixelSize) {
            for (int x = 0; x < width; x += pixelSize) {
                if (rand.nextBoolean()) {
                    gc.fillRect(x, y, pixelSize, pixelSize);
                }
            }
        }
        
        // Draw Finder Patterns
        drawFinderPattern(gc, 2, 2, pixelSize); 
        drawFinderPattern(gc, (int)(width) - (pixelSize * 7) - 2, 2, pixelSize); 
        drawFinderPattern(gc, 2, (int)(height) - (pixelSize * 7) - 2, pixelSize); 
    }
    
    private void drawFinderPattern(GraphicsContext gc, int x, int y, int size) {
        int patternSize = size * 7;
        gc.setFill(Color.BLACK);
        gc.fillRect(x, y, patternSize, patternSize);
        gc.setFill(Color.WHITE);
        gc.fillRect(x + size, y + size, patternSize - (2 * size), patternSize - (2 * size));
        gc.setFill(Color.BLACK);
        gc.fillRect(x + (2 * size), y + (2 * size), patternSize - (4 * size), patternSize - (4 * size));
    }

    @FXML
    public void closeCard(ActionEvent event) {
        ((Stage)((Node)event.getSource()).getScene().getWindow()).close();
    }
}