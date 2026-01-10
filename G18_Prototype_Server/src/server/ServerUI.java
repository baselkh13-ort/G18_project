package server;

import javafx.application.Application;
import javafx.stage.Stage;
import gui.ServerPortFrameController;
import common.ChatIF;

 /**
	* Entry point for the Server Application.
	* Handles the initialization of the GUI and starting the server logic.
    */
 
public class ServerUI extends Application {
	
	// Launches the JavaFX application
	public static void main( String args[] ) throws Exception
	   {   
		 launch(args);
	  } // end main
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		// Initializes and displays the Server Port configuration GUI		  		
		ServerPortFrameController aFrame = new ServerPortFrameController(); 
		 
		aFrame.start(primaryStage);
	}
	
	/**
     * Initializes and starts the BistroServer.
     * Configures the database connection password before starting the server.
     * * @param p          The port number as a string.
     * @param dbPassword The password for the MySQL database connection.
     * @param ui         The UI interface for logging messages.
     */
	public static boolean runServer(String p, String dbPassword ,common.ChatIF ui)
	{
		 MySQLConnectionPool.setDBPassword(dbPassword);
		 int port = 0; 

	        try
	        {
	        		// Parses the port number from the provided string
	        		port = Integer.parseInt(p); 
	          
	        }
	        catch(Throwable t)
	        {
	        		if (ui != null) ui.display("ERROR - Could not connect!");
	        		return false;
	        }
	        try {
				MySQLConnectionPool.testConnection(); 
			} catch (Exception e) {
				if (ui != null) ui.display("Error: DB Connection Failed! Check Password.");
				return false; 
			}
	        // Creates a new instance of the server logic with port and UI
			BistroServer sv = new BistroServer(port, ui);

	        
	        try 
	        {
	          // Starts listening for incoming client connections
	          sv.listen(); 
	          return true;
	        } 
	        catch (Exception ex) 
	        {
	        		if (ui != null) ui.display("ERROR - Could not listen for clients!");
	        		return false;
	        }
	}
}