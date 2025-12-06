package client;

import ocsf.client.*;
import common.ChatIF;
import logic.Order;
import logic.ActionType;
import logic.BistroMessage; // Import the wrapper class

import java.io.*;
import java.util.ArrayList;

/**
 * This class overrides some of the methods defined in the abstract
 * superclass in order to give more functionality to the client.
 */
public class ChatClient extends AbstractClient
{
  //Instance variables **********************************************
  
  ChatIF clientUI; 
  // Initialize the list to prevent NullPointerException
  public static ArrayList<Order> listOfOrders = new ArrayList<>();
  public static Order order = null;
  public static boolean awaitResponse = false;

  //Constructors 
  
  public ChatClient(String host, int port, ChatIF clientUI) 
    throws IOException 
  {
    super(host, port); //Call the superclass constructor (AbstractClient)
    this.clientUI = clientUI;
  }

  //Instance methods 
    
  /**
   * This method handles all data that comes in from the server.
   *
   * @param msg The message from the server.
   */
  public void handleMessageFromServer(Object msg) 
  {
      System.out.println("--> handleMessageFromServer");
      
      // Release the waiting flag
      awaitResponse = false;

      // Safety check
      if (msg == null) {
          System.out.println("Received null from server");
          return;
      }

      //Handle the wrapper (BistroMessage)
      if (msg instanceof BistroMessage) {
          BistroMessage bistroMsg = (BistroMessage) msg;
          ActionType type = bistroMsg.getType();
          Object data = bistroMsg.getData();

          System.out.println("Message Type: " + type);

          // Case A: Received an order list (for View All)
          if (data instanceof ArrayList) {
              System.out.println("Got ArrayList inside BistroMessage");
              listOfOrders = (ArrayList<Order>) data;
          }
          
          // Case B: Received a single order (for Update order)
          else if (data instanceof Order) {
              System.out.println("Got Order inside BistroMessage");
              order = (Order) data;
          }
          
          // --- Case C: Received an error message or text ---
          else if (data instanceof String) {
              System.out.println("Got String message: " + data);
              // If it's an error, reset variables so screens know nothing returned
              if (data.toString().contains("Error") || data.toString().contains("not found")) {
                  order = null;
                  listOfOrders.clear(); // Clear the list
              }
          }
          else if (data == null) {
              System.out.println("Data inside BistroMessage is NULL!");
              order = null;
          }
      } else {
    	  System.out.println("WARNING: Received unknown object from server!");
      }
  }

  /**
   * This method handles all data coming from the UI            
   *
   * @param message The message from the UI.    
   */
  
  public void handleMessageFromClientUI(Object message)  
  {
    try
    {
        awaitResponse = true;
        sendToServer(message); // Send the object (BistroMessage)
        
        // Wait for response
        while (awaitResponse) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    catch(IOException e)
    {
        e.printStackTrace();
        clientUI.display("Could not send message to server: Terminating client."+ e);
        quit();
    }
  }

  
  /**
   * This method terminates the client.
   */
  public void quit()
  {
	    try
	    {
	      closeConnection(); // Send disconnection message to server
	    }
	    catch(IOException e) {}
	    
	    // Add a small delay of half a second
	    try {
	        Thread.sleep(500); // Allow time for the message to be sent
	    } catch (InterruptedException e) {
	        e.printStackTrace();
	    }
	    
	    System.exit(0); // Now we can close quietly
  }
}
//End of ChatClient class