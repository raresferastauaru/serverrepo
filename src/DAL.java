import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DAL {
	private Connection _connection;
	private PreparedStatement _preparedStatement;
	private CallableStatement _callableStatement;

	public DAL()
	{
	    try
	    {
			String DB_Driver = "org.mariadb.jdbc.Driver";
			String DB_Url = "jdbc:mysql://localhost/MyCloudDB";
			String DB_User = "root";
			String DB_Password = "";

			Class.forName(DB_Driver);
			_connection = DriverManager.getConnection(DB_Url, DB_User, DB_Password);
	    }
	    catch (Exception e)
	    {
			System.err.println("DAL: Exceptie la instantiere: " + e.getMessage());
	    }
	}
	
	public String Close()
	{
		try
		{
			_connection.close();
			return "DAL inchis cu succes";
		}
		catch(Exception e)
		{
			return "DAL: Exceptie la Inchidere: " + e.getMessage();
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
			_preparedStatement = _connection.prepareStatement(query);

			if (sqlParams != null) {
	        	for(int i=1; i<=sqlParams.length; i++) {
	        		switch(sqlParams[i-1].getType()) 
	        		{
		        		case "String":
		        			_preparedStatement.setString(i, sqlParams[i-1].getValue());
		        			break;
		        		case "Integer":
		        			_preparedStatement.setInt(i, Integer.parseInt(sqlParams[i-1].getValue()));
		        			break;
		        		default:
		        			System.out.println("DAL: RuleazaCerereReturneazaRs - Tipul " + sqlParams[i-1].getType() + " nu este implementat inca.");
		        			break;
	        		}
	        	}
	        }
	        
	        rs = _preparedStatement.executeQuery();
		} catch (Exception e) {
			System.out.println("DAL: Exceptie la RuleazaCerereReturneazaRs: " + query);
			e.printStackTrace();
		}

        return rs;
    }
	
	
	public ResultSet RunSpReturnRs(String storedProcedure)
	{
		return RunSpReturnRs(storedProcedure, new SqlParam[0]);
	}
	public ResultSet RunSpReturnRs(String storedProcedure,  SqlParam[] sqlParams)
	{
		ResultSet rs = null;
		try {
			_callableStatement = _connection.prepareCall(storedProcedure);
	        
			if (sqlParams != null) {
	        	for(int i=1; i<=sqlParams.length; i++) {
	        		switch(sqlParams[i-1].getType()) 
	        		{
		        		case "String":
		        			_callableStatement.setString(i, sqlParams[i-1].getValue());
		        			break;
		        		case "Integer":
		        			_callableStatement.setInt(i, Integer.parseInt(sqlParams[i-1].getValue()));
		        			break;
		        		default:
		        			System.out.println("DAL: RuleazaProceduraStocataReturneazaRs - Tipul " + sqlParams[i-1].getType() + " nu este implementat inca.");
		        			break;
	        		}
	        	}
	        }
	        
	        rs = _callableStatement.executeQuery();
		} catch (Exception e) {
			System.out.println("DAL: Exceptie la RuleazaProceduraStocataReturneazaRs: " + storedProcedure);
			e.printStackTrace();
		}

        return rs;
    }
}
