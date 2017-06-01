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
		} catch (IOException ex) {
			System.out.println("Can't get socket input stream. ");
		}

		try {
			socketOutputStream = new DataOutputStream(CurrentConnectedUser.getSocket().getOutputStream());
		} catch (IOException ex) {
			System.out.println("Can't get socket output stream. ");
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
		boolean connectionActive = true;
		byte[] bytesCommand;
		String stringCommand;

		try {
			while(connectionActive) {
				bytesCommand = new byte[1536];
				socketInputStream.read(bytesCommand);
				stringCommand = new String(bytesCommand);
				String[] parts = stringCommand.split(":");

				connectionActive = TreatCommand(parts);
			}
		}
		catch(SocketException ex) {
			ManageDisconnectedSocket();
		} catch(Exception ex) {
		System.out.println("TcpClientHandler: Reading command exception:" +
			"\n\tMessage: " + ex.getMessage() +
			"\n\tToString: " + ex.toString() +
			"\n\tStackTrace: " + ex.getStackTrace() +
			"\n\tExceptionClassName: " + ex.getClass().getName());
		}
	}

	private boolean TreatCommand(String[] parts) {
		boolean result;
		CommandTypes cmd = CommandTypes.valueOf(parts[0]);
		switch(cmd)
		{
			case GET:
				result = receivedCommand.CommandGET(parts[1]);
				break;
			case GETFileHashes:
				result = receivedCommand.CommandGETFileHashes();
				break;
			case PUT:
				result = receivedCommand.CommandPUT(parts[1], Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
				if(result)
					SendPushNotification("CHANGED", parts[1]);
				break;
			case RENAME:
				result = receivedCommand.CommandRENAME(parts[1], parts[2]);
				if(result)
					SendPushNotification("RENAMED", parts[1], parts[2]);
				break;
			case DELETE:
				result = receivedCommand.CommandDELETE(parts[1]);
				if(result)
					SendPushNotification("DELETED", parts[1]);
				break;
			case MKDIR:
				result = receivedCommand.CommandMKDIR(parts[1]);
				if(result)
					SendPushNotification("MKDIR", parts[1]);
				break;
			case KILL:
				ManageDisconnectedSocket();
				return false;
		}

		return true;
	}

	private void SendPushNotification(String command, String value) {
		try {
			Thread.sleep(1500);
			String pushCommand = ":PUSHNOTIFICATION:" + command + ":" + value + ":EOCR:";  /// BEFORE: "PUSHNOTIFICATION:"
			for(int i=0; i<Helper.ConnectedUsers.size(); i++)
			{
				ConnectedUser connectedUserTo = Helper.ConnectedUsers.get(i);
				if(CurrentConnectedUser.getSocketPort() != connectedUserTo.getSocketPort()	
					&& CurrentConnectedUser.getUserName().equals(connectedUserTo.getUserName()))
				{
					System.out.println("Push notification (" + command + 
						" the file: " + value +
						") sent to client " + connectedUserTo.getUserName() + 
						"@" + connectedUserTo.getSocketPort() + ".");
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
		CurrentConnectedUser.setUserName(userName);
		receivedCommand.appendToUserDetails("@" + Integer.toString(CurrentConnectedUser.getSocketPort()));

		synchronized (Helper.ConnectedUsers) {
			Helper.ConnectedUsers.add(CurrentConnectedUser);
		}

		receivedCommand.AppendUsedToAssociatedEntities(CurrentConnectedUser.getUserName());

		System.out.println("Server acceped client " + CurrentConnectedUser.getUserName() + "@" + CurrentConnectedUser.getSocket().getPort() + ". (IO streams were set)");
		WriteToClient("AKNOWLEDGE:");
	}
	
	private void ManageDisconnectedSocket() {		
		System.out.println("Client " + CurrentConnectedUser.getUserName() + "@" + CurrentConnectedUser.getSocket().getPort() + " was disconnected.");
		
		System.out.println("\t- Removing: " + CurrentConnectedUser.getSocket().getPort());
		synchronized (Helper.ConnectedUsers) {
			Helper.ConnectedUsers.remove(CurrentConnectedUser);
		}

		receivedCommand.RemoveUserFromAssociatedEntities(CurrentConnectedUser.getUserName());

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
