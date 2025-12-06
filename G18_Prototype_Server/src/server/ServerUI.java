package server;

import javafx.application.Application;
import javafx.stage.Stage;
import gui.ServerPortFrameController;


 //Main entry point for the Server application using JavaFX.
 
public class ServerUI extends Application {
	final public static int DEFAULT_PORT = 5555;
	
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
	 * Static method to initialize and start the BistroServer.
	 */
	public static void runServer(String p ,common.ChatIF ui)
	{
		 int port = DEFAULT_PORT; //Port to listen on

	        try
	        {
	        		// Parses the port number from the provided string
	        		port = Integer.parseInt(p); 
	          
	        }
	        catch(Throwable t)
	        {
	        		if (ui != null) ui.display("ERROR - Could not connect!");
	        }
	    	
	        // Creates a new instance of the server logic with port and UI
	        BistroServer sv = new BistroServer(port ,ui);
	        
	        try 
	        {
	          // Starts listening for incoming client connections
	          sv.listen(); 
	        } 
	        catch (Exception ex) 
	        {
	        	if (ui != null) ui.display("ERROR - Could not listen for clients!");
	        }
	}
}