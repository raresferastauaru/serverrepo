import java.sql.*;

public class DAL
{
	Connection connection;

	String insertUserQuery = "INSERT INTO Users(UserName, UserPassword) VALUES(?, ?)";
	String insertFileHashQuery = "INSERT INTO FileHashes(FullPath, RelativePath, HashCode) VALUES(?, ?, ?)";
	String selectUsersPasswordQuery = "SELECT UserPassword FROM Users WHERE UserName = ?";
	String selectFileHashesQuery = "SELECT * FROM FileHashes";

	Statement statement;
	PreparedStatement insertUserPreparedStatement;
	PreparedStatement insertFileHashPreparedStatement;
	PreparedStatement selectUsersPasswordPreparedStatement;

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

	public void InsertNewFileHash(String fullPath, String relativePath, String hashCode)
	{
		try
		{
			insertFileHashPreparedStatement.setString(1, fullPath);
			insertFileHashPreparedStatement.setString(2, relativePath);
			insertFileHashPreparedStatement.setString(3, hashCode);

			insertFileHashPreparedStatement.execute();
		}
		catch (Exception e)
		{
			System.err.println("Insert new file hash exception: " + e.getMessage());
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

	public ResultSet GetFileHashes()
	{
		try
		{
	  		return statement.executeQuery(selectFileHashesQuery);
		}
		catch (Exception e)
		{
	  		System.err.println("Get FileHashes exception: " + e.getMessage());
		}
		return null;
	}
}
