import java.io.*;
import java.net.*;
import java.util.*;

public class Server {

    public static void main(String[] args) throws Exception {
    	int socketNo = 4444;
    	ServerSocket serverSocket = null;
    	
        try {
            serverSocket = new ServerSocket(socketNo);
            System.out.println("Aplicatia Server a deschis un SocketServer pe portul " + socketNo + ". (" + (new Date()).toString() + ")");
        } catch (IOException ex) {
            System.out.println("Aplicatia Server nu poate porni un ServerSocket pe portul " + socketNo + ". (" + (new Date()).toString() + ")");
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
				System.out.println("Aplicatia Server a esuat.\nExceptie : " + e.getMessage());
				e.printStackTrace();
			}
		}
    }
}
