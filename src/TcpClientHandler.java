import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.Thread;

public class TcpClientHandler implements Runnable {
	private ConnectedUser CurrentConnectedUser = null;
	private InputStream _socketInputStream = null;
	private OutputStream _socketOutputStream = null;
	private ReceivedCommand _receivedCommand;

	public TcpClientHandler(Socket acceptedSocket) {
		CurrentConnectedUser = new ConnectedUser(acceptedSocket);
		
		try {
			_socketInputStream = new DataInputStream(CurrentConnectedUser.getSocket().getInputStream());
		} catch (IOException ex) {
			System.out.println("Stream-ul de intrare prin socket neinitializat. ");
		}

		try {
			_socketOutputStream = new DataOutputStream(CurrentConnectedUser.getSocket().getOutputStream());
		} catch (IOException ex) {
			System.out.println("Stream-ul de iesire prin socket neinitializat. ");
		}

		_receivedCommand = new ReceivedCommand(_socketInputStream, _socketOutputStream);
	}
	
	public boolean Accepted() {
		byte[] buffer = new byte[1024];
		try {
			_socketInputStream.read(buffer);
			String credentials = new String(buffer);
			String[] credentialParts = credentials.split(":");
			
			boolean accepted = _receivedCommand.ValidateConnectedUser(credentialParts[0], credentialParts[1]);			
			if(accepted) {			
				ManageConnectedSocket(credentialParts[0]);
			} else {
		        System.out.println("Serverul a respins clientul " + credentialParts[0] + " (date de autentificare gresite).");
				WriteToClient("Error:Utilizatorul introdus nu exista sau parola e gresita:");
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
				_socketInputStream.read(bytesCommand);
				stringCommand = new String(bytesCommand);
				String[] parts = stringCommand.split(":");

				connectionActive = TreatCommand(parts);
			}
		}
		catch(SocketException ex) {
			ManageDisconnectedSocket();
		} catch(Exception ex) {
		System.out.println("ManipulantClientTCP: Run Exception:" +
			"\n\tMesaj: " + ex.getMessage() +
			"\n\tToString: " + ex.toString() +
			"\n\tStackTrace: " + ex.getStackTrace() +
			"\n\tNumeleExceptiei: " + ex.getClass().getName());
		}
	}

	private boolean TreatCommand(String[] parts) {
		boolean result;
		CommandTypes cmd = CommandTypes.valueOf(parts[0]);
		switch(cmd)
		{
			case GET:
				result = _receivedCommand.CommandGET(parts[1]);
				break;
			case GETFileHashes:
				result = _receivedCommand.CommandGETFileHashes();
				break;
			case PUT:
				result = _receivedCommand.CommandPUT(parts[1], Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
				if(result)
					SendPushNotification("CHANGED", parts[1]);
				break;
			case RENAME:
				result = _receivedCommand.CommandRENAME(parts[1], parts[2]);
				if(result)
					SendPushNotification("RENAMED", parts[1], parts[2]);
				break;
			case DELETE:
				result = _receivedCommand.CommandDELETE(parts[1]);
				if(result)
					SendPushNotification("DELETED", parts[1]);
				break;
			case MKDIR:
				result = _receivedCommand.CommandMKDIR(parts[1]);
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
					System.out.println("PushNotification(" + command + 
						") fisierul <" + value +
						"> a fost transmit catre clientul " + connectedUserTo.getUserName() + 
						"@" + connectedUserTo.getSocketPort() + ".");
					WriteToClientOnSocket(connectedUserTo.getSocket(), pushCommand);
				}
			}
		}
		catch(Exception ex) {
			System.out.println("PushNotification - Exception: ");
			ex.printStackTrace();
		}
	}
	
	private void SendPushNotification(String command, String value1, String value2) {
		SendPushNotification(command, value1 + ":" + value2);
	}

	private void ManageConnectedSocket(String userName) {
		CurrentConnectedUser.setUserName(userName);
		_receivedCommand.appendToUserDetails("@" + Integer.toString(CurrentConnectedUser.getSocketPort()));

		synchronized (Helper.ConnectedUsers) {
			Helper.ConnectedUsers.add(CurrentConnectedUser);
		}

		_receivedCommand.AppendUserToAssociatedEntities(CurrentConnectedUser.getUserName());

		System.out.println("Serverul a acceptat clientul " + CurrentConnectedUser.getUserName() + "@" + CurrentConnectedUser.getSocket().getPort() + ". (Streamurile IO au fost initializate)");
		WriteToClient("AKNOWLEDGE:");
	}
	
	private void ManageDisconnectedSocket() {		
		System.out.println("Clientul " + CurrentConnectedUser.getUserName() + "@" + CurrentConnectedUser.getSocket().getPort() + " s-a deconectat.");
		
		System.out.println("\t- Sters: " + CurrentConnectedUser.getSocket().getPort());
		synchronized (Helper.ConnectedUsers) {
			Helper.ConnectedUsers.remove(CurrentConnectedUser);
		}

		_receivedCommand.RemoveUserFromAssociatedEntities(CurrentConnectedUser.getUserName());

		for(int i = 0;i<Helper.ConnectedUsers.size(); i++) {
			System.out.println("\t> Ramas: " + Helper.ConnectedUsers.get(i).getSocketPort());
		}
	}

	private void WriteToClient(String message) {
		try {
			byte[] messageBytes = message.getBytes();
			_socketOutputStream.write(messageBytes, 0, messageBytes.length);
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
