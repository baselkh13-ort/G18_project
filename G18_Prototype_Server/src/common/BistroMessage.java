package logic;
import java.io.Serializable;

public class BistroMessage implements Serializable{
	private static final long serialVersionUID = 1L;

    private ActionType type; 
    private Object data;    

    public BistroMessage(ActionType type, Object data) {
        this.type = type;
        this.data = data;
    }
    
    public ActionType getType() { return type; }
    public Object getData() { return data; }
}
