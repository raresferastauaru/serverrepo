import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.Thread;

public class TcpClientHandler implements Runnable {
    private static Socket socket = null;
    private static InputStream socketInputStream = null;
    private static OutputStream socketOutputStream = null;

    private static ReceivedCommand receivedCommand;

    public TcpClientHandler(Socket acceptedSocket) {
        socket = acceptedSocket;
        System.out.println("Server acceped a new client socket (" + socket.getPort() + ").");

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

    public void run() {
        String stringCommand;
        byte[] bytesCommand;
        boolean connectionActive = true;

        try
        {
            while(connectionActive)
            {
                System.out.println("\t.......................................");

                bytesCommand = new byte[1536];
                socketInputStream.read(bytesCommand);
                stringCommand = new String(bytesCommand);
                String[] parts = stringCommand.split(":");

                connectionActive = TreatCommand(parts);
            }
        }
		catch(SocketException ex) {
			//socket.close(); socketInputStream.close(); socketOutputStream.close();
			// socket = null; socketInputStream = null; socketOutputStream = null;
			System.out.println("Client connected on socket " + socket.getPort() + " was disconnected.");
		}
        catch(Exception ex) {
            System.out.println("Reading command exception:" +
                       "\n\tMessage: " + ex.getMessage() +
                       "\n\tToString: " + ex.toString() +
                       "\n\tStackTrace: " + ex.getStackTrace() +
					   "\n\tExceptionClassName: " + ex.getClass().getName());
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
			System.out.println("Client connected on socket " + socket.getPort() + " was disconnected.");
            return false;
        }

        return true;
    }
}
