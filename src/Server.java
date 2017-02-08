import java.io.*;
import java.net.*;
import java.util.*;


public class Server {
    private static int socketNo = 4444;
    private static Socket socket = null;
    private static ServerSocket serverSocket = null;
    private static InputStream socketInputStream = null;
    private static OutputStream socketOutputStream = null;
    
    private static ReceivedCommand receivedCommand;

    public static void main(String[] args) throws Exception {
        InitializeServer();
        
        String stringCommand;
        byte[] bytesCommand;
        boolean serverActive = true;

        try 
        {
            while(serverActive)
            {
                System.out.println("\t.......................................");

                bytesCommand = new byte[1536];
                socketInputStream.read(bytesCommand);
                stringCommand = new String(bytesCommand);
                String[] parts = stringCommand.split(":");

                serverActive = TreatCommand(parts);
            }
        }
        catch(Exception ex) {
            System.out.println("Reading command exception:" + 
            				   "\n\tMessage: " + ex.getMessage() + 
            				   "\n\tToString: " + ex.toString() + 
            				   "\n\tStackTrace: " + ex.getStackTrace());
        }

        CloseServer();
    }

    private static void InitializeServer() {
        try {
            serverSocket = new ServerSocket(socketNo);
            System.out.println("Server socket " + socketNo + " is open, now, at " + (new Date()).toString());
        } catch (IOException ex) {
            System.out.println("Can't setup server on this port number (" + socketNo + ").");
        }

        try {
            socket = serverSocket.accept();
            System.out.println("Server acceped a new client socket (" + socket.getPort() + ").");
        } catch (IOException ex) {
            System.out.println("Can't accept client connection. ");
        }

        try {
            socketInputStream = new DataInputStream(socket.getInputStream());
            System.out.println("Socket input stream was set");
        } catch (IOException ex) {
            System.out.println("Can't get socket input stream. ");
        }

        try {
            socketOutputStream = new DataOutputStream(socket.getOutputStream());
            System.out.println("Socket output stream was set");
        } catch (IOException ex) {
            System.out.println("Can't get socket outpus stream. ");
        }
        
        receivedCommand = new ReceivedCommand(socketInputStream, socketOutputStream);
    }

    private static void CloseServer(){
    	try {
    		socketOutputStream.close();
	        socketInputStream.close();
	        socket.close();
	        serverSocket.close();
    	} catch(Exception ex) {
    		System.out.println("Closing server exception: " + ex.getMessage());
    	}
    }
    
    private static boolean TreatCommand(String[] parts) {
        boolean readData;
        CommandTypes cmd = CommandTypes.valueOf(parts[0]);
        switch(cmd)
        {
          case GET:
            readData = receivedCommand.CommandGET(parts[1]);
            System.out.println("GET state: " + readData);
            break;
          case GETFileHashes:
            readData = receivedCommand.CommandGETFileHashes();
            System.out.println("GETFileHashes state: " + readData);
            break;
          case PUT:
            readData = receivedCommand.CommandPUT(parts[1], Integer.parseInt(parts[2]));
            System.out.println("PUT state: " + readData);
            break;
          case RENAME:
            readData = receivedCommand.CommandRENAME(parts[1], parts[2]);
            System.out.println("RENAME state: " + readData);
            break;
          case DELETE:
            readData = receivedCommand.CommandDELETE(parts[1]);
            System.out.println("DELETE state: " + readData);
            break;
          case MKDIR:
            readData = receivedCommand.CommandMKDIR(parts[1]);
            System.out.println("MKDIR state: " + readData);
            break;
          case KILL:
            System.out.println("The server was killed. Have a nice day!");
            return false;
        }

        return true;
    }
}
