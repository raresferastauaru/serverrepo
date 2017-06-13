import java.io.*;
import java.net.*;
import java.util.*;
import java.sql.SQLException;
import java.sql.ResultSet;

public class Gateway {

    private static DAL _dal;
    private static OdroidNode _userAlreadyConnectedOn;
    private static OdroidNode _lowestLoadedOdroid;

    public static void main(String[] args) throws Exception {

		int socketNo = 4445;
		ServerSocket serverSocket = null;
		InputStream socketInputStream = null;
		OutputStream socketOutputStream = null;

        try {
            serverSocket = new ServerSocket(socketNo);
            System.out.println("Aplicatia Gateway a deschis un ServerSocket pe portul " + socketNo + ". (" + (new Date()).toString() + ")");
        } catch (IOException ex) {
            System.out.println("Aplicatia Gateway nu poate porni un ServerSocket pe portul " + socketNo + ". (" + (new Date()).toString() + ")");
        }

        _dal = new DAL();

        byte[] buffer;
        while (true) {
			try {
				Socket socket = serverSocket.accept();

                socketInputStream = new DataInputStream(socket.getInputStream());
                socketOutputStream = new DataOutputStream(socket.getOutputStream());

                buffer = new byte[128];
                int readBytes = socketInputStream.read(buffer);
                String userName = new String(buffer).substring(0, readBytes);

                OdroidNode selectedNode = GetAvailableOdroidNode(userName);
                socketOutputStream.write(selectedNode.getOdroidNodeInfos(), 0, selectedNode.getOdroidNodeInfos().length);
                
        		System.out.println("Utilizatorul <" + userName + "> a fost asociat cu nodul <" +  selectedNode.getName() + ">.");
			}			
			catch (Exception ex) 
			{
                ex.printStackTrace();
			}
		}
    }

    private static OdroidNode GetAvailableOdroidNode(String userName) {
        RefreshAssociatedEntitiesInfos(userName);
        
        if(_userAlreadyConnectedOn != null)
			return _userAlreadyConnectedOn;
        else 
	        return _lowestLoadedOdroid;
    }

    private static void RefreshAssociatedEntitiesInfos(String userName)
    {
    	String userAlreadyConnectedQuery = "SELECT OdroidName, OdroidIP, OdroidPort FROM AssociatedEntities WHERE UserName='" + userName +  "' LIMIT 1";
    	String lowestLoadedOdroidQuery = "SELECT OdroidName, OdroidIP, OdroidPort, Count(*) FROM AssociatedEntities GROUP BY OdroidName ORDER BY Count(*) ASC LIMIT 1";
        ResultSet rs;

        try {
            rs = _dal.RunQueryReturnRs(userAlreadyConnectedQuery);
            if(rs.first())
            {
				_userAlreadyConnectedOn = new OdroidNode(rs.getString("OdroidName"), rs.getString("OdroidIP"), rs.getString("OdroidPort"));
			}
			else 
			{
				_userAlreadyConnectedOn = null;
			}

			rs = _dal.RunQueryReturnRs(lowestLoadedOdroidQuery);
			if(rs.first())
            {
            	_lowestLoadedOdroid = new OdroidNode(rs.getString("OdroidName"), rs.getString("OdroidIP"), rs.getString("OdroidPort"));
			}
			else 
			{
				_lowestLoadedOdroid = new OdroidNode("Odroid1", "192.168.100.31", "4444");
			}
        } catch (SQLException e) {
            System.out.println("Exceptie SQL la ReincarcaEntitatileAsociate: procesarea ResultSet-ului");
            e.printStackTrace();
        }
    }
}
