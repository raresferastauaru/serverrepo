import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static int socketNo = 4444;
    private static ServerSocket serverSocket = null;
	private static List<Socket> connectedSockets = new ArrayList<Socket>();

    public static void main(String[] args) throws Exception {
        try {
            serverSocket = new ServerSocket(socketNo);
            System.out.println("Server socket " + socketNo + " is open, now, at " + (new Date()).toString());
        } catch (IOException ex) {
            System.out.println("Can't setup server on this port number (" + socketNo + ").");
        }

        while (true) {
			try 
			{
				Socket socket = serverSocket.accept();
				
				synchronized (connectedSockets) 
				{
					connectedSockets.add(socket);
				}
				
				//for(int i=0; i<connectedSockets.size(); i++)
				//{
			    //	System.out.println("-- ConnectedSockets: " + connectedSockets.get(i).getPort());
				//}
				
				(new Thread(new TcpClientHandler(socket, connectedSockets))).start();
			}			
			catch (Exception e) 
			{
					System.out.println("Server failed.\nException: " + e.getMessage());		
			}
		}
    }

    private static void CloseServer(){
    	try {
				serverSocket.close();
    	} catch(Exception ex) {
    		System.out.println("Closing server exception: " + ex.getMessage());
    	}
    }
}
