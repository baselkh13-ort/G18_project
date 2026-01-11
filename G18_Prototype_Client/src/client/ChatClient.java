package client;

import java.io.IOException;
import java.util.ArrayList;

import common.ActionType;
import common.BistroMessage;
import common.ChatIF;
import common.OpeningHour;
import common.Order;
import common.User;
import ocsf.client.AbstractClient;

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
  public static ArrayList<OpeningHour> openingHours = new ArrayList<>();
  public static ArrayList<String> availableTimeSlots = new ArrayList<>();
  public static Order order = null;
  public static boolean awaitResponse = false;
  public static User user = null;
  public static User registeredUser = null;
  public static boolean operationSuccess = false; // Flag for boolean responses (Cancel)

  //Constructors ****************************************************
  
  public ChatClient(String host, int port, ChatIF clientUI) 
    throws IOException 
  {
    super(host, port); //Call the superclass constructor (AbstractClient)
    this.clientUI = clientUI;
  }

  //Instance methods ************************************************
    
  /**
   * This method handles all data that comes in from the server.
   *
   * @param msg The message from the server.
   */
  public void handleMessageFromServer(Object msg) {
      System.out.println("--> handleMessageFromServer");
      order = null;
      // Release the waiting flag
      awaitResponse = false;
      operationSuccess = false;

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
        	  if (type == ActionType.GET_AVAILABLE_TIMES) { 
                  System.out.println("Got available time slots from server");
                  availableTimeSlots = (ArrayList<String>) data;
             }
        	  else if (type == ActionType.GET_OPENING_HOURS) { 
                  System.out.println("Got OpeningHours list");
                  openingHours = (ArrayList<OpeningHour>) data;
              } 
              else {
                  System.out.println("Got Orders list");
                  listOfOrders = (ArrayList<Order>) data;
              }
          }
          
          // Case B: Received a single order (for Update order)
          else if (data instanceof Order) {
              System.out.println("Got Order inside BistroMessage");
              order = (Order) data;
          }
          
          //Case C:Received an user
          else if (data instanceof User) {
        	  if (type == ActionType.LOGIN) {
                  System.out.println("User logged in.");
                  user = (User) data;
              } 
              else if (type == ActionType.REGISTER_CLEINT) {
                  System.out.println("New client registered.");
                  registeredUser = (User) data;
              }
          }
          
          //Case D: Received an error message or text
          else if (data instanceof String) {
        	  String msgString = (String) data;
              System.out.println("Got String message: " + msgString);
              if (msgString.equals("Success")) {
                  operationSuccess = true;
              }
              // If it's an error, reset variables so screens know nothing returned
              else if (data.toString().contains("Error") || data.toString().contains("not found")) {
                  order = null;
                  listOfOrders.clear(); // Clear the list 
              }
          
          }
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