package common;

/**
 * This interface defines the contract for classes that handle message display.
 * * It is used to decouple the network layer (Client/Server) from the user interface.
 * Any class implementing this interface must define how to display or log
 * messages received from the communication layer.
 */
public interface ChatIF {

    /**
     * Method that handles the display of a message.
     * * @param message The String value to be displayed or logged to the UI/Console.
     */
    void display(String message);

}