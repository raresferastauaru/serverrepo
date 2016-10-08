import java.sql.*;

public class DAL
{
	private int USERID = 1;
	Connection connection;

	String selectFileHashesQuery = "SELECT * FROM FileHashes";

	Statement statement;

	String insertUserQuery = "INSERT INTO Users(UserName, UserPassword) VALUES(?, ?)";
	PreparedStatement insertUserPreparedStatement;

	String insertFileHashQuery = "INSERT INTO FileHashes(RelativePath, HashCode) VALUES(?, ?)";
	PreparedStatement insertFileHashPreparedStatement;

	String selectUsersPasswordQuery = "SELECT UserPassword FROM Users WHERE UserName = ?";
	PreparedStatement selectUsersPasswordPreparedStatement;

	String updateFileHashCodeQuery = "{CALL UpdateFileHashCode(?, ?, ?)}";
  String updateFileHashRelativePathQuery = "{CALL UpdateFileHashRelativePath(?, ?)}";
	CallableStatement callableStatement;

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

			statement = connection.createStatement();

			insertUserPreparedStatement = connection.prepareStatement(insertUserQuery);
			insertFileHashPreparedStatement = connection.prepareStatement(insertFileHashQuery);
			selectUsersPasswordPreparedStatement = connection.prepareStatement(selectUsersPasswordQuery);
	    }
	    catch (Exception e)
	    {
			System.err.println("DAL setup exception: " + e.getMessage());
	    }
	}

	public String Close()
	{
		try
		{
			connection.close();
			return "Dal succesfully closed";
		}
		catch(Exception e)
		{
			return "DAL exception: " + e.getMessage();
		}
	}

	public void InsertNewUser(String userName, String userPassword)
	{
		try
		{
			insertUserPreparedStatement.setString(1, userName);
			insertUserPreparedStatement.setString(2, userPassword);
			insertUserPreparedStatement.execute();
		}
		catch (Exception e)
		{
		  	System.err.println("Insert new user exception exception: " + e.getMessage());
		}
	}

	public void UpdateFileHashCode(String relativePath, String hashCode)
	{
		int hashCodeInt = Integer.parseInt(hashCode);

		try
		{
	    callableStatement = connection.prepareCall(updateFileHashCodeQuery);
			callableStatement.setString(1, relativePath);
			callableStatement.setInt(2, hashCodeInt);
			callableStatement.setInt(3, USERID);

			callableStatement.execute();
		} catch (Exception e) {
			System.err.println("Update file hash exception: " + e.getMessage());
		}
	}
 
  public void UpdateFileHashRelativePath(String oldRelativePath, String newRelativePath)
	{
		try
		{
	    callableStatement = connection.prepareCall(updateFileHashRelativePathQuery);
			callableStatement.setString(1, oldRelativePath);
			callableStatement.setString(2, newRelativePath);

			callableStatement.execute();
		} catch (Exception e) {
			System.err.println("Update file hash exception: " + e.getMessage());
		}
	}

	public void GetUsersPassword(String userName)
	{
		try
		{
			selectUsersPasswordPreparedStatement.setString(1, userName);
			selectUsersPasswordPreparedStatement.executeQuery();
		}
		catch (Exception e)
		{
			System.err.println("Get user's password exception: " + e.getMessage());
		}
	}

	public String GetFileHashes()
	{
		try
		{
            String fileHashes = "";

	  		ResultSet rs = statement.executeQuery(selectFileHashesQuery);
			while (rs.next())
			{
				fileHashes += rs.getString("RelativePath") + ":" + rs.getString("HashCode") + ":" + rs.getString("OldHashCode") + "|";
			}

			return fileHashes;
		}
		catch (Exception e)
		{
	  		System.err.println("Get FileHashes exception: " + e.getMessage());
		}
		return null;
	}
}
