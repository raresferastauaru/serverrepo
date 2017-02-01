import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DAL {
	Connection connection;

	 
	PreparedStatement preparedStatement;
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

	
	public ResultSet RunQueryReturnRs(String query)
	{
		return RunQueryReturnRs(query, new SqlParam[0]);
	}
	
	public ResultSet RunQueryReturnRs(String query,  SqlParam[] sqlParams)
    {
		ResultSet rs = null;
		try {
			preparedStatement = connection.prepareStatement(query);

			if (sqlParams != null) {
	        	for(int i=1; i<=sqlParams.length; i++) {
	        		switch(sqlParams[i-1].getType()) 
	        		{
		        		case "String":
		        			preparedStatement.setString(i, sqlParams[i-1].getValue());
		        			break;
		        		case "Integer":
		        			preparedStatement.setInt(i, Integer.parseInt(sqlParams[i-1].getValue()));
		        			break;
		        		default:
		        			System.out.println("RunSpReturnRs - Type " + sqlParams[i-1].getType() + " does not have support yet.");
		        			break;
	        		}
	        	}
	        }
	        
	        rs = preparedStatement.executeQuery();
		} catch (Exception e) {
			System.out.println("Exception on RunQueryReturnRs: " + query);
			e.printStackTrace();
		}

        return rs;
    }
	
	public ResultSet RunSpReturnRs(String storedProcedure,  SqlParam[] sqlParams)
    {
		ResultSet rs = null;
		try {
			callableStatement = connection.prepareCall(storedProcedure);
	        
			if (sqlParams != null) {
	        	for(int i=1; i<=sqlParams.length; i++) {
	        		switch(sqlParams[i-1].getType()) 
	        		{
		        		case "String":
		        			callableStatement.setString(i, sqlParams[i-1].getValue());
		        			break;
		        		case "Integer":
		        			callableStatement.setInt(i, Integer.parseInt(sqlParams[i-1].getValue()));
		        			break;
		        		default:
		        			System.out.println("RunSpReturnRs - Type " + sqlParams[i-1].getType() + " does not have support yet.");
		        			break;
	        		}
	        	}
	        }
	        
	        rs = callableStatement.executeQuery();
		} catch (Exception e) {
			System.out.println("Exception on RunSpReturnRs: " + storedProcedure);
			e.printStackTrace();
		}

        return rs;
    }
}
