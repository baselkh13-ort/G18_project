package client;
import javafx.application.Application;

import javafx.stage.Stage;
import logic.ScreenMode;
import gui.ClientConnectFormController;

public class ClientUI extends Application {
	// Reference to the ClientController (only one instance allowed)
	public static ClientController chat; 
	// Stores the current operation mode (VIEW or UPDATE)
	public static ScreenMode currentMode;    
	public static void main( String args[] ) throws Exception{ 
		launch(args);  
	} // end main
	 
	@Override
	public void start(Stage primaryStage) throws Exception {
		ClientConnectFormController connectFrame = new ClientConnectFormController(); 
		connectFrame.start(primaryStage);
	}
	
	
}
	