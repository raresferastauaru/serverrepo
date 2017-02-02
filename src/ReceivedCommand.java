import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ReceivedCommand {

    private static Gateway gateway = new Gateway();
    private static String RootPath = Helper.getSyncLocation();
    private InputStream socketInputStream = null;
    private OutputStream socketOutputStream = null;
    
	public ReceivedCommand(InputStream socketInputStream, OutputStream socketOutputStream)
	{
		this.socketInputStream = socketInputStream;
		this.socketOutputStream = socketOutputStream;
	}

	public boolean CommandGETFileHashes()
    {
        try {
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
	
	public boolean CommandGET(String fileName)
    {
        try {
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
            	// append from db: creationTime - Ticks?,  lastWriteTime - Ticks ?, isReadOnly
            	WriteToClient("ACKNOWLEDGE:" + sourceFile.length() + ":" + fileDetails + ":");   // check if fileDetails contains those details
                  
                InputStream fileInputStream = new FileInputStream(sourceFile);
                byte[] bytes = new byte[1024];
                while ((count = fileInputStream.read(bytes, 0, 1024)) > 0) {
                     socketOutputStream.write(bytes, 0, count);
                }

                fileInputStream.close();
                return true;
            }
        } catch (IOException ex) {
            System.out.println("CommandGET: ");
        	ex.printStackTrace();
        }

        return false;
    }

	public boolean CommandPUT(String fileName, Integer bytesToRead)
    {
        //if(enoughSpaceOnHdd)//!!!!!!!!!!!!

        WriteToClient("ACKNOWLEDGE:");
        byte[] buffer = new byte[bytesToRead];

        try {
        	boolean readingData = true;
        	Integer bytesRead = 0, bytesLeft = bytesToRead, nextPacketSize;
        	Integer bufferLength, bufferSize = 1536;
            long startingTime = System.currentTimeMillis();

            while(readingData)
            {
                nextPacketSize = (bytesLeft > bufferSize) ? bufferSize : bytesLeft;

                bufferLength = socketInputStream.read(buffer, bytesRead, nextPacketSize);
                bytesRead += bufferLength;
                bytesLeft -= bufferLength;

                if (bytesLeft <= 0)
                {
                    long ms = System.currentTimeMillis() - startingTime;

                    if(ms == 0) ms = 1;

                    System.out.println("File " + fileName +
                                        " was transferred with " + String.valueOf(bytesRead / ms) +
                                        "(" + String.valueOf(bytesRead) + "/" + String.valueOf(ms) + ") bps.");

                    readingData = false;
                }
            }

            fileName = RootPath + fileName;
            OutputStream out = new FileOutputStream(fileName);
            out.write(buffer, 0, buffer.length);

            WriteToClient("ACKNOWLEDGE:");
            
            byte[] fileHashByte = new byte[2048];
            socketInputStream.read(fileHashByte);
            String fileHashString = new String(fileHashByte);
            String[] parts = fileHashString.split(":");
            if(parts[0].equals("FileHashDetails"))
            {
            	FileHashDetails fileHashDetails = new FileHashDetails(parts[1],parts[2],parts[3],parts[4]);
            	gateway.UpdateFileHashCode(Helper.getRelativePath(fileName), fileHashDetails);
            	
                System.out.println("FileHash(" + Helper.getRelativePath(fileName) + ", " + parts[1] + ") has been registred successfully.");
            	WriteToClient("ACKNOWLEDGE:");
            }
            else
            {
            	WriteToClient("Error:FileHash wasn't sent.:");
            }

            out.close();
            return true;
        } catch (IOException ex) {
            System.out.println("CommandPUT: " + ex);
        }

        return false;
    }

	public boolean CommandRENAME(String oldFileName, String newFileName)
    {
    	try {
	        String oldFilePath = RootPath + "/" +  oldFileName;
	        String newFilePath = RootPath + "/" + newFileName;
	        
	        File oldFile = new File(oldFilePath);
	        File newFile = new File(newFilePath);

	        if(oldFile.exists())
	        {
	            WriteToClient("ACKNOWLEDGE:");

	            if(oldFile.renameTo(newFile))
	            {
                  Thread.sleep(10);

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

        try {
            if(fileToDelete.isDirectory())
            {
                boolean directoryDeleted = DeleteDirectory(fileToDelete);
                if(directoryDeleted) {
                	gateway.DeleteFileHashCode(fileName, true); 				// Cum tratam cazurile de stergere de DIR in DB ?
                																// Ma gandesc la un WHERE RelativePath LIKE 'relPath%' !!!
                	System.out.println("Succesfully deleted directory: " + fileName + " and all the files that it contained." );
                    WriteToClient("ACKNOWLEDGE:");                    
                }
                else
                    WriteToClient("Error:failed to delete directory " + fileToDelete + ".:");
                return directoryDeleted;
            }


            boolean fileDeleted = fileToDelete.delete();
            if(fileDeleted) {
            	gateway.DeleteFileHashCode(fileName, false);
            	System.out.println("Succesfully deleted file: " + fileName + "." );
                WriteToClient("ACKNOWLEDGE:");
            }
            else
                WriteToClient("Error:failed to delete file " + fileToDelete + ".:");
            return fileDeleted;
        } catch (Exception ex) {
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
            directoryCreated = file.mkdir();

            if(directoryCreated)
                WriteToClient("ACKNOWLEDGE:");
            else
                WriteToClient("Error:Failed to create directory " + folderName + ".:");
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

	private boolean DeleteDirectory(File path)
    {
        if( path.exists() ) {
          File[] files = path.listFiles();
          for(int i=0; i<files.length; i++) {
             if(files[i].isDirectory()) {
               DeleteDirectory(files[i]);
             }
             else {
               files[i].delete();
             }
          }
        }
        return( path.delete() );
    }
}
