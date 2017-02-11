import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static int socketNo = 4444;
    private static ServerSocket serverSocket = null;

    public static void main(String[] args) throws Exception {
        try {
            serverSocket = new ServerSocket(socketNo);
            System.out.println("Server socket " + socketNo + " is open, now, at " + (new Date()).toString());
        } catch (IOException ex) {
            System.out.println("Can't setup server on this port number (" + socketNo + ").");
        }

        while (true) {
			try {
					(new Thread(new TcpClientHandler(serverSocket.accept()))).start();
			} catch (Exception e) {
					System.out.println("Server failed.\nException: " + e.getMessage());						
					//System.out.println(Thread.currentThread().getName() + " ");
					//CloseServer();
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
