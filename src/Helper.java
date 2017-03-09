import java.util.*;

public class Helper
{
	public static List<ConnectedUser> ConnectedUsers;
	
	static {
		ConnectedUsers = new ArrayList<ConnectedUser>();
	}
	
	public static String getSyncLocation()
	{
	    return "/home/rares/SyncRootDirectory/";
	}
	public static String getSpecificPath(String fullPath, String fixedPath)
	{
	    Integer sIndex = fixedPath.length() + 1;
	    return fullPath.substring(sIndex, fullPath.length() - sIndex);
	}
	public static String getRelativePath(String path)
	{
		return path.substring(getSyncLocation().length());
	}
}
