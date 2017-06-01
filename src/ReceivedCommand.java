import java.io.*;
import java.util.Collections;
import java.lang.Thread;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class ReceivedCommand {

    private static Gateway gateway = new Gateway();
	
	private InputStream socketInputStream = null;
    private OutputStream socketOutputStream = null;
    
    private String RootPath = Helper.getSyncLocation();
    private String UserDetails;
    private int bufferSize =  8192;
	
    public ReceivedCommand(InputStream socketInputStream, OutputStream socketOutputStream)
  	{
		this.socketInputStream = socketInputStream;
		this.socketOutputStream = socketOutputStream;
  	}
	
	public boolean ValidateConnectedUser(String userName, String userPassword)
	{
		boolean userValid = gateway.ValidateConnectedUser(userName, userPassword);
		if(userValid)
		{
			RootPath += userName + "/";
			
			File dir = new File(RootPath);
			if (!dir.exists()) {
			    try{
			        dir.mkdir();
			    }
			    catch(SecurityException e){
			        e.printStackTrace();
			    }        
			}
			
			UserDetails = userName;
			return true;
		}
		return false;
	}
	public void appendToUserDetails(String detail) 
	{
		UserDetails += detail;
	}

    public boolean CommandGET(String fileName) 
    {
        try
        {
        	System.out.println(UserDetails + "\t- Geting the file: " + fileName);

            int count;
            String filePath = RootPath + fileName;
            File sourceFile = new File(filePath);

            if(!sourceFile.exists()) {
                WriteToClient("Error:File doesn't exists!:");
            }
            else if(sourceFile.isDirectory()) {
                WriteToClient("Error:Server can't return a directory!:");
            }
            else {
				String fileDetails = gateway.GetFileHash(fileName);
				WriteToClient("ACKNOWLEDGE:" + sourceFile.length() + ":" + fileDetails + ":");
				
                InputStream fileInputStream = new FileInputStream(sourceFile);
                byte[] buffer = new byte[bufferSize];
                long startingTime = System.currentTimeMillis();
                while ((count = fileInputStream.read(buffer, 0, buffer.length)) > 0) {
                     socketOutputStream.write(buffer, 0, count);
                }
				WriteToClient(":EOCR:");

                long miliseconds = System.currentTimeMillis() - startingTime;
                if(miliseconds == 0) miliseconds = 1;

                long sentSize = sourceFile.length();
                System.out.println(UserDetails + "\t- File " + fileName +
                                    " was transferred with " + String.valueOf(sentSize / miliseconds) +
                                    " kbps (" + String.valueOf(sentSize) + "/" + String.valueOf(miliseconds) + ").");

                fileInputStream.close();
                return true;
            }
        } catch (IOException ex) {
            System.out.println(UserDetails + "\t- CommandGET - IOException: ");
        	ex.printStackTrace();
		} catch (Exception ex) {
            System.out.println(UserDetails + "\t- CommandGET - Exception: ");
        	ex.printStackTrace();
        }

        return false;
    }
    
	public boolean CommandPUT(String fileName, int fileHashCode, Integer bytesToRead)
	{
		boolean validation = false;
		//if(notEnoughSpaceOnDisk)
		//	WriteToClient("Error:NotEnoughSpaceOnDisk");

		String filePath = RootPath + fileName;
		FileOutputStream fileOutputStream = null;

		System.out.println(UserDetails + "\t- Putting the file " + fileName + " has begun (size " + bytesToRead.toString() + ").");

		try
		{
			fileOutputStream = ValidateFile(filePath);
			
			int storedHashCode = gateway.GetFileHashCode(fileName);
			if(storedHashCode == fileHashCode)
			{
				WriteToClient("Error: the file " + fileName + "(HashCode - " + fileHashCode + ") is up to date already.");
			}
			else
			{
				WriteToClient("ACKNOWLEDGE:");
				
				Integer numberOfBytesRead, bytesLeft = bytesToRead;
				byte[] buffer = new byte[bufferSize];
				boolean readingData = true;

				if(bytesToRead != 0)
				{
					long startingTime = System.currentTimeMillis();
					while(readingData)
					{
						numberOfBytesRead = socketInputStream.read(buffer, 0, buffer.length);
						fileOutputStream.write(buffer, 0, numberOfBytesRead);
						bytesLeft -= numberOfBytesRead;

						if (bytesLeft == 0)
						{
							long miliseconds = System.currentTimeMillis() - startingTime;
							if(miliseconds == 0) miliseconds = 1;

							System.out.println(UserDetails + "\t- Transfer of file " + fileName + " is done (" + String.valueOf(bytesToRead / miliseconds) + " kbps).");

							readingData = false;
						}
						else if(bytesLeft < 0)
						{
							System.out.println(UserDetails + "\t- File " + fileName + " is on bytesLeft < 0. WHY ?");
						}
					}
				}
				else
				{
					Thread.sleep(250);
				}
			
				WriteToClient("ACKNOWLEDGE:");

				byte[] fileHashByte = new byte[2048];
				socketInputStream.read(fileHashByte);
				String fileHashString = new String(fileHashByte);
				String[] parts = fileHashString.split(":");
				if(parts[0].equals("FileHashDetails"))
				{
					FileHashDetails fileHashDetails = new FileHashDetails(fileName, parts[1],parts[2],parts[3],parts[4]);
					gateway.UpdateFileHashCode(fileHashDetails);

					System.out.println(UserDetails + "\t- " + fileHashDetails.toString() + " - has been registred in database successfully.");
					WriteToClient("ACKNOWLEDGE:");
					WriteToClient("EOCR:");
				}
				else
				{
					WriteToClient("Error:FileHash wasn't sent succesfully.:");
				}

				validation = true;
			}
		} catch (IOException ex) {
			System.out.println(UserDetails + "\t- CommandPUT: IOException: " + ex);
		} catch (InterruptedException ex) {
			System.out.println(UserDetails + "\t- CommandPUT: InterruptedException: " + ex);
		} finally {
			try {
				if (fileOutputStream != null)
					fileOutputStream.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

		return validation;
	}

    public boolean CommandRENAME(String oldFileName, String newFileName)
    {
    	try {
			File oldFile = new File(RootPath + oldFileName);
			File newFile = new File(RootPath + newFileName);
			
	        if(newFile.exists())
	        {
	            WriteToClient("Error:Can't rename the file <" + oldFileName + "> to <" + newFileName + "> because a file with the new desired name already exists!:");
	            return false;
	        }
			
			if(oldFile.exists())
	        {
	            WriteToClient("ACKNOWLEDGE:");

	            if(oldFile.renameTo(newFile))
	            {
					if(newFile.isDirectory())
					{
					  gateway.UpdateFileHashRelativePath(oldFileName, newFileName, true);
					  System.out.println(UserDetails + "\t- Folder " + oldFileName + " was successfully renamed to " + newFileName);
					}
					else
					{
					  gateway.UpdateFileHashRelativePath(oldFileName, newFileName, false);
					  System.out.println(UserDetails + "\t- File " + oldFileName + " was successfully renamed to " + newFileName);
					}

					WriteToClient("ACKNOWLEDGE:");
					WriteToClient("EOCR:");
					return true;
	            }

	            WriteToClient("Error:failed to rename the file " + oldFileName + " to " + newFileName + ".:");
	            return false;
	        }
			else
			{
				WriteToClient("Error:can't rename the file <" + oldFileName + "> to <" + newFileName + "> because it doesn't exist.:");
			}
	    } catch (Exception ex) {
            System.out.println(UserDetails + "\t- CommandPUT: " + ex);
	    }
		
		return false;
    }

    public boolean CommandDELETE(String fileName)
    {
        File fileToDelete = new File(RootPath + fileName);

        try
        {
			if(fileToDelete.exists())
			{
				if(fileToDelete.isDirectory())
				{
					boolean directoryDeleted = DeleteDirectory(fileToDelete);
					if(directoryDeleted)
					{
						gateway.DeleteFileHashCode(fileName, true);
						System.out.println(UserDetails + "\t- Succesfully deleted directory: " + fileName + " and all the files that it contained." );
						WriteToClient("ACKNOWLEDGE:");
						//WriteToClient("EOCR:");
					}
					else
					{
						WriteToClient("Error:failed to delete directory " + fileToDelete + ".:");
					}

					return directoryDeleted;
				}
				else
				{
					boolean fileDeleted = fileToDelete.delete();
					if(fileDeleted)
					{
						gateway.DeleteFileHashCode(fileName, false);
						System.out.println(UserDetails + "\t- Succesfully deleted file: " + fileName + "." );
						WriteToClient("ACKNOWLEDGE:");
					}
					else
					{
						WriteToClient("Error:failed to delete file " + fileToDelete + ".:");
					}
					return fileDeleted;
				}
			}
			else 
			{
				WriteToClient("Error: file " + fileToDelete + " is already deleted on server.:");
				return false;
			}
        }
        catch (Exception ex)
        {
            WriteToClient("Error:" + ex.getMessage() + ".:");
            return false;
        }
    }

	public boolean CommandMKDIR(String folderName)
    {
        boolean directoryCreated = false;

        try{
            String fullPath = RootPath + folderName;

            File file = new File(fullPath);
            directoryCreated = file.mkdirs();

            if(directoryCreated)
            {
            	System.out.println(UserDetails + "\t- Directory " + fullPath + " was created successfully.");
                WriteToClient("ACKNOWLEDGE:");
            }
            else
            {
                WriteToClient("Error:Failed to create directory " + folderName + ".:");
            }
        }catch(Exception e){
           e.printStackTrace();
        }

        return directoryCreated;
    }

    public boolean CommandGETFileHashes()
    {
		boolean validation = false;
        try
        {
            System.out.println(UserDetails + "\t- Geting all the FileHashes.");
            String fileHashes = gateway.GetAllFileHashesForUser();
	
			// Does it really send it all ?!
            if(!fileHashes.equals(""))
            {
              	byte[] fileHashesBytes = fileHashes.getBytes();
           		socketOutputStream.write(fileHashesBytes, 0, fileHashesBytes.length);
				
  	            validation = true;
            }
  			else
  			{
                WriteToClient("Error:There are no FileHashes stored on the server.:");
  			}
				
			WriteToClient(":EOCR:");
        } catch (Exception ex) {
            System.out.println(UserDetails + "\t- CommandGETFileHashes: " + ex);
        }

        return validation;
    }

    public void AppendUsedToAssociatedEntities(String userName)
    {
		String hostName;
		String ipAddress;
		String port = "4444";

		try	{
			hostName = 	InetAddress.getLocalHost().getHostName();		
			ipAddress = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException ex) {
			System.out.println("On AppendUsedToAssociatedEntities:");
			ex.printStackTrace();
			return;
		}

    	gateway.AddNewAssociatedEntities(hostName, ipAddress, port, userName);
    }

    public void RemoveUserFromAssociatedEntities(String userName)
    {
		String ipAddress;
		try	{
			ipAddress = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException ex) {
			System.out.println("On RemoveUserFromAssociatedEntities:");
			ex.printStackTrace();
			return;
		}

    	gateway.DeleteAssociatedEntities(ipAddress, userName);
    }

	/// HELPERS ///
	private void WriteToClient(String message)
    {
        try {
        	if(!message.contains("ACKNOWLEDGE") && !message.contains("EOCR"))
        		System.out.println(UserDetails + "\t- " + message);

            byte[] messageBytes = message.getBytes();
            socketOutputStream.write(messageBytes, 0, messageBytes.length);
        }
        catch(Exception ex){
            System.out.println(ex.getMessage());
        }
    }

	private FileOutputStream ValidateFile(String filePath) throws IOException
	{
		File file = new File(filePath);

		File parentDir = file.getParentFile();
		if(!parentDir.exists())
			parentDir.mkdirs();
		else if (file.exists())
			file.delete();

		file.createNewFile();

		return new FileOutputStream(file.getAbsoluteFile(), true);
	}

	private boolean DeleteDirectory(File path)
    {
        if(path.exists())
        {
			File[] files = path.listFiles();
			for(int i=0; i<files.length; i++)
			{
				if(files[i].isDirectory())
					DeleteDirectory(files[i]);
				else
					files[i].delete();
			}
        }
        return path.delete();
    }
}
