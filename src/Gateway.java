import java.sql.ResultSet;
import java.sql.SQLException;

public class Gateway {
	private DAL _dal;
	private String USERID = "0";

	public Gateway() {
		_dal= new DAL();
	}
	
	
	// FILE HASHES //
	
	// GET ALL FILE HASHES
	private String selectFileHashesQuery = "SELECT * FROM FileHashes WHERE UserId = ?";
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
			System.out.println("PortalOperatiiDB: SQLException la GetFileHashes: procesarea ResultSet-ului");
			e.printStackTrace();
		}

		return fileHashesString;
	}
	
	// GET SPECIFIC FILE HASHE
	private String selectFileHashQuery = "SELECT * FROM FileHashes WHERE UserId = ? AND RelativePath = ?";
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
			System.out.println("PortalOperatiiDB: SQLException la GetFileHash: procesarea ResultSet-ului");
			System.out.println("Mesaj: " + e.getMessage());
		}

		return fileHashString;
	}
	
	// GET FILE HASH CODE
	private String selectFileHashCodeQuery = "SELECT * FROM FileHashes WHERE UserId = ? AND RelativePath = ? AND IsDeleted = 0";
	public int GetFileHashCode(String relativePath) {
		SqlParam[] sqlParams = new SqlParam[2];
		sqlParams[0] = new SqlParam("Integer", USERID);
		sqlParams[1] = new SqlParam("String", relativePath);
		
		ResultSet rs = _dal.RunQueryReturnRs(selectFileHashCodeQuery, sqlParams);
		
		try {		
			if(rs.next())
			{
				return Integer.parseInt(rs.getString("HashCode"));
			}
		} catch (SQLException e) {
			System.out.println("PortalOperatiiDB: SQLException la GetFileHashCode: procesarea ResultSet-ului");
			e.printStackTrace();
		}
		return 0;
	}
	
	// UPDATE FILE HASHCODE
	private String updateFileHashCodeStoredProcedure = "{CALL UpdateFileHashCode(?, ?, ?, ?, ?, ?)}";
	public void UpdateFileHashCode(FileHashDetails fileHashDetails)
	{
		SqlParam[] sqlParams = new SqlParam[6];
 
		sqlParams[0] = new SqlParam("Integer", USERID);
		sqlParams[1] = new SqlParam("String", fileHashDetails.getFileName());
		sqlParams[2] = new SqlParam("Integer", fileHashDetails.getHashCode());
		sqlParams[3] = new SqlParam("String", fileHashDetails.getCreationTime());
		sqlParams[4] = new SqlParam("String", fileHashDetails.getLastWriteTime());
		sqlParams[5] = new SqlParam("String", fileHashDetails.getIsReadOnly());
		
		_dal.RunSpReturnRs(updateFileHashCodeStoredProcedure, sqlParams);
	}
	
	// UPDATE FILE RELATIVE PATH
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
			System.out.println("Directorul " + oldRelativePath + " redenumit cu succes la " + newRelativePath + " in baza de date.");
		}
		else
		{			
			_dal.RunSpReturnRs(updateFileHashRelativePathStoredProcedure, sqlParams);
			System.out.println("Fisierul " + oldRelativePath + " redenumit cu succes la " + newRelativePath + " in baza de date.");
		}
	}
	
	// DELETE FILE HASHCODE
	private String deleteFileHashQuery = "UPDATE FileHashes SET IsDeleted=1 WHERE RelativePath = ? AND UserId = ?";
	private String deleteDirectoryHashQuery = "UPDATE FileHashes SET IsDeleted=1 WHERE RelativePath LIKE ? AND UserId = ?";
	public void DeleteFileHash(String relativePath, boolean isDirectory) {
		SqlParam[] sqlParams = new SqlParam[2];
		sqlParams[1] = new SqlParam("Integer", USERID);
		
		if(isDirectory)
		{
			sqlParams[0] = new SqlParam("String", relativePath + '%');
			_dal.RunQueryReturnRs(deleteDirectoryHashQuery, sqlParams);
		}
		else
		{
			sqlParams[0] = new SqlParam("String", relativePath);
			_dal.RunQueryReturnRs(deleteFileHashQuery, sqlParams);
		}
	}
	
	

	// USERS //
	
	// VALIDATE USER
	private String selectConnectedUser = "SELECT * FROM Users WHERE UserName = ?";
	public boolean ValidateConnectedUser(String userName, String userPassword) 
	{		
		SqlParam[] sqlParams = new SqlParam[1];
		sqlParams[0] = new SqlParam("String", userName);
		
		ResultSet rs = _dal.RunQueryReturnRs(selectConnectedUser, sqlParams);
		
		try {
			if(if(rs.first())
            {
				String encryptedPassword = rs.getString("UserPassword");
				boolean userValid = Sha2Helper.verify(userPassword, encryptedPassword);
				
				if(userValid) {
					USERID = rs.getString("UserId");
					return true;
				}
			}
			else
			{
				return false;
			}			
		} catch (SQLException ex) {
			System.out.println("PortalOperatiiDB: SQLException la ValidateConnectedUser: procesarea ResultSet-ului");
			ex.printStackTrace();
		}
		return false;
	}
	
	private String getUsersPasswordQuery = "SELECT UserPassword FROM Users WHERE UserName = ?";
	public void GetUsersPassword(String userName)
	{
		SqlParam[] sqlParams = new SqlParam[1];
		sqlParams[0] = new SqlParam("String", userName);
		
		_dal.RunQueryReturnRs(getUsersPasswordQuery, sqlParams);
	}



	// ASSOCIATED ENTITIES //

	// Subscribe to list of Associadet Entities
    private String addNewAssociatedEntitiesQuery = "INSERT INTO MyCloudDB.AssociatedEntities(OdroidName, OdroidIP, OdroidPort, UserName) VALUES(?, ?, ?, ?)";
    public void AddNewAssociatedEntities(String odroidName, String odroidIP, String odroidPort, String userName)
    {
        SqlParam[] sqlParams = new SqlParam[4];
        sqlParams[0] = new SqlParam("String", odroidName);
        sqlParams[1] = new SqlParam("String", odroidIP);
        sqlParams[2] = new SqlParam("String", odroidPort);
        sqlParams[3] = new SqlParam("String", userName);

        _dal.RunQueryReturnRs(addNewAssociatedEntitiesQuery, sqlParams);
    }

	// Subscribe from the list of Associadet Entities
	private String deleteAssociatedEntitiesQuery = "DELETE FROM AssociatedEntities WHERE OdroidIP = ? AND UserName = ? LIMIT 1";
	public void DeleteAssociatedEntities(String odroidIP, String userName)
	{
		SqlParam[] sqlParams = new SqlParam[2];
		sqlParams[0] = new SqlParam("String", odroidIP);
		sqlParams[1] = new SqlParam("String", userName);
		
		_dal.RunQueryReturnRs(deleteAssociatedEntitiesQuery, sqlParams);
	}
}
