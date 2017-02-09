import java.io.*;

public class ReceivedCommand {

    private static Gateway gateway = new Gateway();
    private static String RootPath = Helper.getSyncLocation();
	
    private InputStream socketInputStream = null;
    private OutputStream socketOutputStream = null;
	private int bufferSize =  8192; // 1492;
	
    public ReceivedCommand(InputStream socketInputStream, OutputStream socketOutputStream)
  	{
		this.socketInputStream = socketInputStream;
		this.socketOutputStream = socketOutputStream;
  	}



    public boolean CommandGET(String fileName)
    {
        try
        {
        	  System.out.println("Geting the file: " + fileName);

            int count;
            String filePath = RootPath + fileName;
            File sourceFile = new File(filePath);

            if(!sourceFile.exists()) {
                WriteToClient("Error:File doesn't exists!:");
            }
            else if(sourceFile.isDirectory()) {
                WriteToClient("Error:Server can't return a directory! Probably it will never return a directory, just a specific file from a directory:");
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
                long miliseconds = System.currentTimeMillis() - startingTime;
                if(miliseconds == 0) miliseconds = 1;

                long sentSize = sourceFile.length();
                System.out.println("File " + fileName +
                                    " was transferred with " + String.valueOf(sentSize / miliseconds) +
                                    " kbps (" + String.valueOf(sentSize) + "/" + String.valueOf(miliseconds) + ").");

                fileInputStream.close();
                return true;
            }
        } catch (IOException ex) {
            System.out.println("CommandGET: ");
        	ex.printStackTrace();
        }

        return false;
    }

    public boolean CommandGETFileHashes()
    {
        try
        {
            System.out.println("Geting the FileHashes.");
            String fileHashes = gateway.GetAllFileHashesForUser();

            if(!fileHashes.equals(""))
            {
              	byte[] fileHashesBytes = fileHashes.getBytes();
              	socketOutputStream.write(fileHashesBytes, 0, fileHashesBytes.length);

  	            return true;
            }
  			else
  			{
                System.out.println("Error:There are no FileHashes stored on the server.");
                WriteToClient("Error:There are no FileHashes stored on the server.:");
  			}
        } catch (Exception ex) {
            System.out.println("CommandGETFileHashes: " + ex);
        }

        return false;
    }

    public boolean CommandPUT(String fileName, Integer bytesToRead)
    {
        //if(notEnoughSpaceOnDisk)
		    //	WriteToClient("Error:NotEnoughSpaceOnDisk");

        String filePath = RootPath + fileName;
        FileOutputStream fileOutputStream = null;

        System.out.println("Putting the file " + fileName + " has begun.");

        try
        {
        	fileOutputStream = ValidateFile(filePath);

          WriteToClient("ACKNOWLEDGE:");

        	Integer numberOfBytesRead, bytesLeft = bytesToRead;
        	byte[] buffer = new byte[bufferSize];
        	boolean readingData = true;

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

                    System.out.println("File " + fileName +
	                                    " was transferred with " + String.valueOf(bytesToRead / miliseconds) +
	                                    " kbps (" + String.valueOf(bytesToRead) + "/" + String.valueOf(miliseconds) + ").");

                    readingData = false;
                }
                else if(bytesLeft < 0)
                {
                	System.out.println("File " + fileName + " is on bytesLeft < 0. WHY ?");
                }
            }

            WriteToClient("ACKNOWLEDGE:");

            byte[] fileHashByte = new byte[2048];
            socketInputStream.read(fileHashByte);
            String fileHashString = new String(fileHashByte);
            String[] parts = fileHashString.split(":");
            if(parts[0].equals("FileHashDetails"))
            {
            	FileHashDetails fileHashDetails = new FileHashDetails(parts[1],parts[2],parts[3],parts[4]);
            	gateway.UpdateFileHashCode(fileName, fileHashDetails);

                System.out.println(fileHashDetails.toString() + " - has been registred in database successfully.");
            	WriteToClient("ACKNOWLEDGE:");
            }
            else
            {
            	WriteToClient("Error:FileHash wasn't sent succesfully.:");
            }

            return true;
        } catch (IOException ex) {
            System.out.println("CommandPUT: " + ex);
		} finally {
			try {
				if (fileOutputStream != null)
					fileOutputStream.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

        return false;
    }

    public boolean CommandRENAME(String oldFileName, String newFileName)
    {
    	try {
	        String oldFilePath = RootPath + oldFileName;
	        String newFilePath = RootPath + newFileName;

	        File oldFile = new File(oldFilePath);
	        File newFile = new File(newFilePath);

	        if(oldFile.exists())
	        {
	            WriteToClient("ACKNOWLEDGE:");

	            if(oldFile.renameTo(newFile))
	            {
					if(newFile.isDirectory())
					{
					  gateway.UpdateFileHashRelativePath(oldFileName, newFileName, true);
					  System.out.println("Folder " + oldFileName + " was successfully renamed to " + newFileName);
					}
					else
					{
					  gateway.UpdateFileHashRelativePath(oldFileName, newFileName, false);
					  System.out.println("File " + oldFileName + " was successfully renamed to " + newFileName);
					}

					WriteToClient("ACKNOWLEDGE:");
					return true;
	            }

	            WriteToClient("Error:failed to rename the file " + oldFileName + " to " + newFileName + ".:");
	            return false;
	        }
// !!! Nu e chiar corect. Poate ar fi corect textul pt Write-ul de mai sus.
	        else if(newFile.exists())
	        {
	            WriteToClient("Error:Can't rename the file to the new name because a file with the new desired name already exists!:");
	            return false;
	        }
	        else
	        {
	            WriteToClient("Error:File " + oldFileName + " doesn't exists!:");
	            return false;
	        }
	    } catch (Exception ex) {
            System.out.println("CommandPUT: " + ex);
            return false;
	    }
    }

    public boolean CommandDELETE(String fileName)
    {
    	String filePath = RootPath + fileName;
        File fileToDelete = new File(filePath);

        try
        {
            if(fileToDelete.isDirectory())
            {
                boolean directoryDeleted = DeleteDirectory(fileToDelete);
                if(directoryDeleted)
                {
// Cum tratam cazurile de stergere de DIR in DB ?
// Ma gandesc la un WHERE RelativePath LIKE 'relPath%' !!!
// !!! Nu e chiar ok. Nu ar trebuii sa existe posibilitatea de a sterge un director.
//      Asta ar insemna sa stergem toate fisierele+hash-urile din db si de pe hard..
//		Poate cel mai ok e sa se stearga un singur hash odata => SA RETURNAM MESAJ CA SERVERUL NU POATE STERGE UN FOLDER?
// PUTEM lasa si asa, nu cred ca e mare paguba..
// !!! Verificat QUERY-ul pt cazul in care alegem sa nu stergem un folder !!!
                	gateway.DeleteFileHashCode(fileName, true);
                	System.out.println("Succesfully deleted directory: " + fileName + " and all the files that it contained." );
                    WriteToClient("ACKNOWLEDGE:");
                }
                else
                {
                    WriteToClient("Error:failed to delete directory " + fileToDelete + ".:");
                }

                return directoryDeleted;
            }


            boolean fileDeleted = fileToDelete.delete();
            if(fileDeleted)
            {
            	gateway.DeleteFileHashCode(fileName, false);
            	System.out.println("Succesfully deleted file: " + fileName + "." );
                WriteToClient("ACKNOWLEDGE:");
            }
            else
            {
                WriteToClient("Error:failed to delete file " + fileToDelete + ".:");
            }

            return fileDeleted;
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
            System.out.println("fullPath " + fullPath);

            File file = new File(fullPath);
            directoryCreated = file.mkdirs();

            if(directoryCreated)
            {
            	System.out.println("Directory " + fullPath + " was created successfully.");
                WriteToClient("ACKNOWLEDGE:");
            }
            else
            {
            	System.out.println("Directory " + fullPath + " creation failed.");
                WriteToClient("Error:Failed to create directory " + folderName + ".:");
            }
        }catch(Exception e){
           e.printStackTrace();
        }

        return directoryCreated;
    }



	private void WriteToClient(String message)
    {
        try {
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
