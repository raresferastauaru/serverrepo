import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.Thread;

public class TcpClientHandler implements Runnable {
    private ConnectedUser CurrentConnectedUser = null;
    private InputStream socketInputStream = null;
    private OutputStream socketOutputStream = null;
    private ReceivedCommand receivedCommand;
	
    public TcpClientHandler(Socket acceptedSocket) {
		CurrentConnectedUser = new ConnectedUser(acceptedSocket);
		
        try {
            socketInputStream = new DataInputStream(CurrentConnectedUser.getSocket().getInputStream());
            System.out.println("Socket input stream was set");
        } catch (IOException ex) {
            System.out.println("Can't get socket input stream. ");
        }

        try {
            socketOutputStream = new DataOutputStream(CurrentConnectedUser.getSocket().getOutputStream());
            System.out.println("Socket output stream was set");
        } catch (IOException ex) {
            System.out.println("Can't get socket outpus stream. ");
        }

        receivedCommand = new ReceivedCommand(socketInputStream, socketOutputStream);
    }
	
	public boolean Accepted() {
		byte[] buffer = new byte[1024];
		try {
			socketInputStream.read(buffer);
			String credentials = new String(buffer);
			String[] credentialParts = credentials.split(":");
			
			boolean accepted = receivedCommand.ValidateConnectedUser(credentialParts[0], credentialParts[1]);			
			if(accepted) {			
				ManageConnectedSocket(credentialParts[0]);
			} else {
		        System.out.println("Server rejected user " + credentialParts[0] + " (invalid credentials).");
				WriteToClient("Error:Entered user doesn't exist or the password is incorect:");
			}			
			return accepted;
		} catch(IOException ex) {
			ex.printStackTrace();
			return false;
		}
	}

    public void run() {
        String stringCommand;
        byte[] bytesCommand;
        boolean connectionActive = true;

        try {
			while(connectionActive) {
                System.out.println("\t.......................................");

				bytesCommand = new byte[1536];
                socketInputStream.read(bytesCommand);
                stringCommand = new String(bytesCommand);
                String[] parts = stringCommand.split(":");

                connectionActive = TreatCommand(parts);
            }
        }
		catch(SocketException ex) {
			ManageDisconnectedSocket();
		}
        catch(Exception ex) {
            System.out.println("Reading command exception:" +
                       "\n\tMessage: " + ex.getMessage() +
                       "\n\tToString: " + ex.toString() +
                       "\n\tStackTrace: " + ex.getStackTrace() +
					   "\n\tExceptionClassName: " + ex.getClass().getName());
        }
    }
	
    private boolean TreatCommand(String[] parts) {
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
            readData = receivedCommand.CommandPUT(parts[1], Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
            System.out.println("PUT state: " + readData);
			if(readData)
				SendPushNotification("CHANGED", parts[1]);			
            break;
          case RENAME:
            readData = receivedCommand.CommandRENAME(parts[1], parts[2]);
            System.out.println("RENAME state: " + readData);
			if(readData)
				SendPushNotification("RENAMED", parts[1], parts[2]);
            break;
          case DELETE:
            readData = receivedCommand.CommandDELETE(parts[1]);
            System.out.println("DELETE state: " + readData);
			if(readData)
				SendPushNotification("DELETED", parts[1]);
            break;
          case MKDIR:
            readData = receivedCommand.CommandMKDIR(parts[1]);
            System.out.println("MKDIR state: " + readData);
            break;
          case KILL:
			ManageDisconnectedSocket();
            return false;
        }

        return true;
    }
	
	private void SendPushNotification(String command, String value) {
		try {
			String pushCommand = "PUSHNOTIFICATION:" + command + ":" + value + ":EOCR:";
			for(int i=0; i<Helper.ConnectedUsers.size(); i++) 
			{				
				ConnectedUser connectedUserTo = Helper.ConnectedUsers.get(i);
				if(CurrentConnectedUser.getSocketPort() != connectedUserTo.getSocketPort()	
					&& CurrentConnectedUser.getUserName().equals(connectedUserTo.getUserName()))
				{
					System.out.println("Push notification (" + command + 
						") sent to client " + connectedUserTo.getUserName() + 
						" on socket port " + connectedUserTo.getSocketPort()	+ ".");
					WriteToClientOnSocket(connectedUserTo.getSocket(), pushCommand);
				}
			}
		}
		catch(Exception ex) {
			System.out.println("Send push notification exception: ");
			ex.printStackTrace();
		}
	}
	
	private void SendPushNotification(String command, String value1, String value2) {
		SendPushNotification(command, value1 + ":" + value2);
	}
	
	private void ManageConnectedSocket(String userName) {	
		System.out.println("Server acceped user " + CurrentConnectedUser.getUserName() + " on socket port: " + CurrentConnectedUser.getSocket().getPort() + ".");
		WriteToClient("AKNOWLEDGE:");
		
		CurrentConnectedUser.setUserName(userName);
		synchronized (Helper.ConnectedUsers)  {
			Helper.ConnectedUsers.add(CurrentConnectedUser);
		}
	}
	
	private void ManageDisconnectedSocket() {
		System.out.println("Client connected on socket " + CurrentConnectedUser.getSocket().getPort() + " was disconnected.");
					
		System.out.println("\t- Removing: " + CurrentConnectedUser.getSocket().getPort());
		synchronized (Helper.ConnectedUsers) {
			Helper.ConnectedUsers.remove(CurrentConnectedUser);
		}
		for(int i = 0;i<Helper.ConnectedUsers.size(); i++) {
			System.out.println("\t> Remained: " + Helper.ConnectedUsers.get(i).getSocketPort());
		}
	}
	
	private void WriteToClient(String message) {
        try {
            byte[] messageBytes = message.getBytes();
            socketOutputStream.write(messageBytes, 0, messageBytes.length);
        }
        catch(Exception ex){
            System.out.println(ex.getMessage());
        }
    }
	
	private void WriteToClientOnSocket(Socket socketTo, String message) {
        try {			
			OutputStream outputStreamTo = socketTo.getOutputStream();
            byte[] messageBytes = message.getBytes();
            outputStreamTo.write(messageBytes, 0, messageBytes.length);
        }
        catch(Exception ex){
            System.out.println(ex.getMessage());
        }
    }	
}
