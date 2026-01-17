package common;

import java.io.Serializable;

/**
 * Represents the standard communication envelope used between the Client and the Server.
 * * This class encapsulates the protocol for all data exchange.
 * It contains an ActionType (the command) and an Object (the data payload).
 * By implementing Serializable, instances of this class can be sent over the network.
 */
public class BistroMessage implements Serializable {
    
    /**
     * A unique identifier for serialization interoperability.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The specific command or action to be performed (e.g., LOGIN, UPDATE_TABLE).
     */
    private ActionType type; 
    
    /**
     * The data payload associated with the action.
     * This can be any Serializable object (e.g., User, Order, ArrayList, String).
     */
    private Object data;    

    /**
     * Constructs a new BistroMessage with a specific action type and data.
     * * @param type The ActionType enum constant representing the command.
     * @param data The object containing the data required for the command.
     */
    public BistroMessage(ActionType type, Object data) {
        this.type = type;
        this.data = data;
    }
    
    /**
     * Retrieves the action type of this message.
     * @return The ActionType enum constant.
     */
    public ActionType getType() { return type; }
    
    /**
     * Retrieves the data payload of this message.
     * @return The data object.
     */
    public Object getData() { return data; }
}