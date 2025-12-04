// Ilya Zeldner
package server;
import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;


public class BistroServer extends AbstractServer {
    private OrderRepository orderRepo;
    public BistroServer(int port) {
        super(port);
        this.orderRepo = new OrderRepository();
    }
    @Override
    protected void serverStarted()
    {
    		System.out.println("Server listening for connections on port " + getPort());
    }
    @Override
    protected void serverStopped()
    {
    		System.out.println("Server has stopped listening for connections.");
    }
    @Override
    protected void clientConnected(ConnectionToClient client) {
        try {
            String clientIp = client.getInetAddress().getHostAddress();
            String clientHost = client.getInetAddress().getHostName();
            System.out.println("Client Connected | Status=Connected | IP=" + clientIp + " | Host=" + clientHost);
        } catch (Exception e) {
            System.out.println("Client Connected | Status=Connected");
        }
    }

    @Override
    protected void clientDisconnected(ConnectionToClient client) {
        try {
            String clientIp = client.getInetAddress().getHostAddress();
            String clientHost = client.getInetAddress().getHostName();
            System.out.println("Client Disconnected | Status=Disconnected | IP=" + clientIp + " | Host=" + clientHost);
        } catch (Exception e) {
            System.out.println("Client Disconnected | Status=Disconnected");
        }
    }
    
    
    @Override
    protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
    		String request;
        	if (msg == null) request = "";
        	else request = msg.toString();
        String response;

        	if (request.equals("GET_ALL_ORDERS")) {
                response = orderRepo.getAllOrders();
                
        }else if (request.startsWith("UPDATE_ORDER,")) {
                try {
                    
                    int p1 = request.indexOf(',');
                    int p2 = request.indexOf(',', p1 + 1);
                    int p3 = request.indexOf(',', p2 + 1);

                    if (p1 < 0 || p2 < 0 || p3 < 0) {
                        response = "ERROR: Use UPDATE_ORDER,order_number,yyyy-mm-dd,number_of_guests";
                    } else {
                        String sOrder = request.substring(p1 + 1, p2);
                        String sDate  = request.substring(p2 + 1, p3);
                        String sGuests = request.substring(p3 + 1);

                        int orderNumber = Integer.parseInt(sOrder);
                        java.sql.Date orderDate = java.sql.Date.valueOf(sDate); // yyyy-mm-dd
                        int guests = Integer.parseInt(sGuests);

                        response = orderRepo.updateOrder(orderNumber, orderDate, guests);
                    }
                } catch (Exception e) {
                    response = "ERROR: " + e.getMessage();
                }
    		  	} else {
                response = "ERROR: Unknown command";
            }

        		try {
        			client.sendToClient(response);
        		} catch (Exception e) {
        			e.printStackTrace();
        		}
    }    
    public static void main(String[] args) {
        try {
            new BistroServer(5555).listen();
        } catch (Exception e) { e.printStackTrace(); }
    }
}


