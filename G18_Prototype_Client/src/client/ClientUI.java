package client;

import gui.utils.ClientConnectFormController;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * The main entry point for the Bistro Client Application.
 * This class extends {@link Application} to manage the JavaFX lifecycle.
 * It is responsible for launching the GUI and initializing the connection
 * screen where the user enters the server IP and port.
 */
public class ClientUI extends Application {

	/**
	 * A static reference to the central controller that handles communication 
	 * between the client UI and the server.
	 * This instance is accessible globally to allow any screen to send messages 
	 * to the server via {@code ClientUI.chat.accept()}.
	 */
	public static ClientController chat; 

	/**
	 * The main method serves as the entry point for the Java application.
	 * It calls {@link #launch(String...)} to start the JavaFX application lifecycle.
	 * * @param args Command line arguments passed to the application.
	 * @throws Exception If an error occurs during the launch process.
	 */
	public static void main( String args[] ) throws Exception { 
		launch(args);  
	} // end main
	 
	/**
	 * The main entry point for the JavaFX application.
	 * This method is called after the system is ready for the application to begin running.
	 * It initializes and displays the {@link ClientConnectFormController}, which serves
	 * as the initial connection window.
	 * * @param primaryStage The primary stage for this application, onto which 
	 * the application scene can be set.
	 * @throws Exception If an error occurs while starting the connection frame.
	 */
	@Override
	public void start(Stage primaryStage) throws Exception {
		ClientConnectFormController connectFrame = new ClientConnectFormController(); 
		connectFrame.start(primaryStage);
	}
}