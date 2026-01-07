package logic;
//Defines the command types sent between Client and Server.

public enum ActionType {
    READ_ORDER,      // Request to fetch a specific order by ID sdfafdfds
    UPDATE_ORDER,    // Request to update an existing order
    GET_ALL_ORDERS,   // Request to retrieve all orders from the DB
    CLIENT_QUIT 	// Indicates that the client is requesting a graceful disconnection from the server
}