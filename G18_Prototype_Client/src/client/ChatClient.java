package client;
import logic.Member;
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
  // Initialize the lists to prevent NullPointerException
  public static ArrayList<Order> listOfOrders = new ArrayList<>();
  public static Order order = null;
  
  public static ArrayList<Member> listOfMembers = new ArrayList<>();
  public static Member member = null;
  
  public static boolean awaitResponse = false;
  
  // If the register successes or not
  public static Member registeredMember;
  public static String registerError;
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
  public void handleMessageFromServer(Object msg) {
	    System.out.println("--> handleMessageFromServer");

	    if (!(msg instanceof BistroMessage)) {
	        System.out.println("WARNING: unknown object from server");
	        awaitResponse = false;
	        return;
	    }

	    BistroMessage bm = (BistroMessage) msg;
	    ActionType type = bm.getType();
	    Object data = bm.getData();

	    System.out.println("Message Type: " + type);

	    switch (type) {

	        case GET_ALL_ORDERS:
	            listOfOrders = (ArrayList<Order>) data;
	            System.out.println("Saved Orders list, size=" + (listOfOrders == null ? 0 : listOfOrders.size()));
	            break;

	        case READ_ORDER:
	            order = (Order) data;
	            break;

	        case GET_ALL_MEMBERS:
	            if (data instanceof ArrayList) {
	                listOfMembers = (ArrayList<Member>) data;
	                System.out.println("Saved Members list, size=" + (listOfMembers == null ? 0 : listOfMembers.size()));
	            } else {
	                System.out.println("GET_ALL_MEMBERS returned non-list: " + data);
	                listOfMembers = new ArrayList<>();
	            }
	            break;

	        case GET_MEMBER_BY_ID:
	            member = (Member) data;
	            break;

	        case REGISTER_MEMBER:
	        	registeredMember = null;
	        	registerError = null;

	        	if (data instanceof Member) {
	        	    registeredMember = (Member) data;
	        	} 
	        	else if (data instanceof String && ((String) data).startsWith("ERROR")) {
	        	    registerError = (String) data;
	        	}

	            break;


	        case UPDATE_MEMBER_CONTACT:
	            System.out.println("Server says: " + data);
	            break;

	        default:
	            System.out.println("Unhandled type: " + type + ", data=" + data);
	    }

	    awaitResponse = false;
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