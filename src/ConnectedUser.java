import java.net.*;

public class ConnectedUser {
	private String userName;
	private Socket socket;
	
	public ConnectedUser(Socket socket) {
		this.socket = socket;
	}


	public void setUserName(String userName) {
		this.userName = userName;
	}


	public String getUserName() {
		return this.userName;
	}

	public Socket getSocket() {
		return this.socket;
	}
	public int getSocketPort() {
		return this.socket.getPort();
	}
}
