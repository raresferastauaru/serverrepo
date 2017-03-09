import java.sql.ResultSet;
import java.sql.SQLException;

public class Gateway {
	private DAL _dal;
	private String USERID = "0";

	public Gateway() {
		_dal= new DAL();
	}
	
	private String selectConnectedUser = "SELECT * FROM Users WHERE UserName = ?";
	public boolean ValidateConnectedUser(String userName, String userPassword) 
	{		
		SqlParam[] sqlParams = new SqlParam[1];
		sqlParams[0] = new SqlParam("String", userName);
		
		ResultSet rs = _dal.RunQueryReturnRs(selectConnectedUser, sqlParams);
		
		try {
			rs.next();
			String encryptedPassword = rs.getString("UserPassword");
			boolean userValid = Sha2Helper.verify(userPassword, encryptedPassword);
			
			if(userValid) {
				USERID = rs.getString("UserId");
				return true;
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
		return false;
	}

	// FILE HASHES //
	private String selectFileHashesQuery = "SELECT * FROM FileHashes WHERE UserId = ?";
	// return Array list of fileHash/fileHashDetails
	public String GetAllFileHashesForUser()
	{
		SqlParam[] sqlParams = new SqlParam[1];
		sqlParams[0] = new SqlParam("Integer", USERID);
		
  		ResultSet rs = _dal.RunQueryReturnRs(selectFileHashesQuery, sqlParams);

        String fileHashesString = "";
		try {
			while (rs.next())
			{
				fileHashesString += rs.getString("RelativePath") + ":" + 
									rs.getString("OldRelativePath") + ":" + 
									rs.getString("HashCode") + ":" + 
									rs.getString("OldHashCode") + ":" +
									rs.getString("IsDeleted") + "|";
			}
		} catch (SQLException e) {
			//e.printStackTrace();
			System.out.println("SQLException on GetFileHashes: progressing the ResultSet");
			System.out.println("Message: " + e.getMessage());
		}

		return fileHashesString;
	}
	
	private String selectFileHashQuery = "SELECT * FROM FileHashes WHERE UserId = ? AND RelativePath = ?";
	// return fileHash/fileHashDetails
	public String GetFileHash(String relativePath) {
		SqlParam[] sqlParams = new SqlParam[2];
		sqlParams[0] = new SqlParam("Integer", USERID);
		sqlParams[1] = new SqlParam("String", relativePath);
		
		ResultSet rs = _dal.RunQueryReturnRs(selectFileHashQuery, sqlParams);

        String fileHashString = "";
		try {
			rs.next();
			
			fileHashString += rs.getString("CreationTime") + ":"
							+ rs.getString("LastWriteTime") + ":"
							+ rs.getString("IsReadOnly");
		
		} catch (SQLException e) {
			//e.printStackTrace();
			System.out.println("SQLException on GetFileHash: progressing the ResultSet");
			System.out.println("Message: " + e.getMessage());
		}

		return fileHashString;
	}
	
	private String selectFileHashCodeQuery = "SELECT * FROM FileHashes WHERE UserId = ? AND RelativePath = ?";
	// return fileHashCode
	public int GetFileHashCode(String relativePath) {
		SqlParam[] sqlParams = new SqlParam[2];
		sqlParams[0] = new SqlParam("Integer", USERID);
		sqlParams[1] = new SqlParam("String", relativePath);
		
		ResultSet rs = _dal.RunQueryReturnRs(selectFileHashCodeQuery, sqlParams);

		try {
			rs.next();
			
			return rs.getInt("HashCode");
		} catch (SQLException e) {
			//e.printStackTrace();
			System.out.println("SQLException on GetFileHashCode: progressing the ResultSet");
			System.out.println("Message: " + e.getMessage());
		}
		return 0;
	}
	
	private String updateFileHashCodeStoredProcedure = "{CALL UpdateFileHashCode(?, ?, ?, ?, ?, ?)}";
	public void UpdateFileHashCode(String relativePath, FileHashDetails fileHashDetails)
	{
		SqlParam[] sqlParams = new SqlParam[6];
 
		sqlParams[0] = new SqlParam("Integer", USERID);
		sqlParams[1] = new SqlParam("String", relativePath);
		sqlParams[2] = new SqlParam("Integer", fileHashDetails.getHashCode());
		sqlParams[3] = new SqlParam("String", fileHashDetails.getCreationTime());
		sqlParams[4] = new SqlParam("String", fileHashDetails.getLastWriteTime());
		sqlParams[5] = new SqlParam("String", fileHashDetails.getIsReadOnly());
		
		_dal.RunSpReturnRs(updateFileHashCodeStoredProcedure, sqlParams);
	}
	
	private String updateFileHashRelativePathStoredProcedure = "{CALL UpdateFileHashRelativePath(?, ?, ?)}";
	private String updateFileHashDirectoryRelativePathStoredProcedure = "{CALL UpdateFileHashDirectoryRelativePath(?, ?, ?)}";
	public void UpdateFileHashRelativePath(String oldRelativePath, String newRelativePath, boolean isDirectory)
	{
		SqlParam[] sqlParams = new SqlParam[3];
		sqlParams[0] = new SqlParam("String", oldRelativePath);
		sqlParams[1] = new SqlParam("String", newRelativePath);
		sqlParams[2] = new SqlParam("Integer", USERID);
		
		if(isDirectory) 
		{
			_dal.RunSpReturnRs(updateFileHashDirectoryRelativePathStoredProcedure, sqlParams);
			System.out.println("Directory " + oldRelativePath + " renamed in database succesfully to " + newRelativePath + ".");
		}
		else
		{			
			_dal.RunSpReturnRs(updateFileHashRelativePathStoredProcedure, sqlParams);
			System.out.println("File " + oldRelativePath + " renamed in database succesfully to " + newRelativePath + ".");
		}
	}
	
	private String deleteFileHashCodeQuery = "UPDATE FileHashes SET IsDeleted=1 WHERE RelativePath = ? AND UserId = ?";
	private String deleteDirectoryHashCodesQuery = "UPDATE FileHashes SET IsDeleted=1 WHERE RelativePath LIKE ? AND UserId = ?";
	public void DeleteFileHashCode(String relativePath, boolean isDirectory) {
		SqlParam[] sqlParams = new SqlParam[2];
		sqlParams[1] = new SqlParam("Integer", USERID);
		
		if(isDirectory)
		{
			sqlParams[0] = new SqlParam("String", relativePath + '%');
			_dal.RunQueryReturnRs(deleteDirectoryHashCodesQuery, sqlParams);
		}
		else
		{
			sqlParams[0] = new SqlParam("String", relativePath);
			_dal.RunQueryReturnRs(deleteFileHashCodeQuery, sqlParams);
		}
	}
	
	

	// USERS //
	private String insertUserQuery = "INSERT INTO Users(UserName, UserPassword) VALUES(?, ?)";
	public void InsertNewUser(String userName, String userPassword)
	{
		SqlParam[] sqlParams = new SqlParam[2];
		sqlParams[0] = new SqlParam("String", userName);
		sqlParams[1] = new SqlParam("String", userPassword);
		
		_dal.RunQueryReturnRs(insertUserQuery, sqlParams);
	}
	private String getUsersPasswordQuery = "SELECT UserPassword FROM Users WHERE UserName = ?";
	public void GetUsersPassword(String userName)
	{
		SqlParam[] sqlParams = new SqlParam[1];
		sqlParams[0] = new SqlParam("String", userName);
		
		_dal.RunQueryReturnRs(getUsersPasswordQuery, sqlParams);
	}
}
