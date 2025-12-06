package logic;

import java.io.Serializable;

// Implements Serializable so this object can be sent over the network
public class BistroMessage implements Serializable {

    // Unique ID to ensure the client and server class versions match
    private static final long serialVersionUID = 1L;

    // Defines the operation/command
    private ActionType type;

    //Uses 'Object' to allow sending any type of data
    private Object data;   

    // Constructor to create a new message
    public BistroMessage(ActionType type, Object data) {
        this.type = type;
        this.data = data;
    }
  
    public ActionType getType() { 
        return type; 
    }
  
    public Object getData() {
        return data; 	
    }
}