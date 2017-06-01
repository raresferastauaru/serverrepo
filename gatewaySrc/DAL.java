import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DAL {
	Connection connection;
	PreparedStatement preparedStatement;

	public DAL()
	{
	    try
	    {
			String DB_Driver = "org.mariadb.jdbc.Driver";
			String DB_Url = "jdbc:mysql://localhost/MyCloudDB";
			String DB_User = "root";
			String DB_Password = "";

			Class.forName(DB_Driver);
			connection = DriverManager.getConnection(DB_Url, DB_User, DB_Password);
	    }
	    catch (Exception e)
	    {
			System.err.println("DAL setup exception: " + e.getMessage());
	    }
	}

	public ResultSet RunQueryReturnRs(String query)
    {
		ResultSet rs = null;
		try {
			preparedStatement = connection.prepareStatement(query);
	        rs = preparedStatement.executeQuery();
		} catch (Exception e) {
			System.out.println("Exception on RunQueryReturnRs: " + query);
			e.printStackTrace();
		}

        return rs;
    }
}
