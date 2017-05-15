import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static int socketNo = 4444;
    private static ServerSocket serverSocket = null;

    public static void main(String[] args) throws Exception {
        try {
            serverSocket = new ServerSocket(socketNo);
            System.out.println("Server socket " + socketNo + " is open. (" + (new Date()).toString() + ")");
        } catch (IOException ex) {
            System.out.println("Can't setup server on port " + socketNo + ".");
        }

        while (true) {
			try {
				Socket socket = serverSocket.accept();
				TcpClientHandler clientHandler = new TcpClientHandler(socket);
				if(clientHandler.Accepted())
					(new Thread(clientHandler)).start();
				else
					socket.close();
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
