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
        	System.out.println(UserDetails + "\t- Obtine fisierul: " + fileName);

            int count;
            String filePath = RootPath + fileName;
            File sourceFile = new File(filePath);

            if(!sourceFile.exists()) {
                WriteToClient("Error:Fisierul nu exista!:");
            }
            else if(sourceFile.isDirectory()) {
                WriteToClient("Error:Serverul nu poate returna un director!:");
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
                System.out.println(UserDetails + "\t- Fisierul " + fileName +
                                    " a fost transferat cu " + String.valueOf(sentSize / miliseconds) +
                                    " kbps (" + String.valueOf(sentSize) + "/" + String.valueOf(miliseconds) + ").");

                fileInputStream.close();
                return true;
            }
        } catch (IOException ex) {
            System.out.println(UserDetails + "\t- ComandaOBTINE - IOException: ");
        	ex.printStackTrace();
		} catch (Exception ex) {
            System.out.println(UserDetails + "\t- ComandaOBTINE - Exception: ");
        	ex.printStackTrace();
        }

        return false;
    }
    
	public boolean CommandPUT(String fileName, int fileHashCode, Integer bytesToRead)
	{
		boolean validation = false;
		//if(notEnoughSpaceOnDisk)
		//	WriteToClient("Error:SpatiuInsuficientPeDisc");

		String filePath = RootPath + fileName;
		FileOutputStream fileOutputStream = null;

		System.out.println(UserDetails + "\t- Punerea fisierului " + fileName + " a inceput (dimensiune " + bytesToRead.toString() + ").");

		try
		{
			fileOutputStream = ValidateFile(filePath);
			
			int storedHashCode = gateway.GetFileHashCode(fileName);
			if(storedHashCode == fileHashCode)
			{
				WriteToClient("Error: fisierul " + fileName + "(Cod hash - " + fileHashCode + ") este deja actualizat.");
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

							System.out.println(UserDetails + "\t- Transferul fisierului " + fileName + " a fost incheiat (" + String.valueOf(bytesToRead / miliseconds) + " kbps).");

							readingData = false;
						}
						else if(bytesLeft < 0)
						{
							System.out.println(UserDetails + "\t- Fisierul " + fileName + " are octetiRamasi < 0. De ce ?");
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

					System.out.println(UserDetails + "\t- " + fileHashDetails.toString() + " - a fost inregistrat in baza de date cu succes.");
					WriteToClient("ACKNOWLEDGE:");
					WriteToClient("EOCR:");
				}
				else
				{
					WriteToClient("Error:Detaliile inregistrarilor fisierului nu au putut fi transferate cu succes.:");
				}

				validation = true;
			}
		} catch (IOException ex) {
			System.out.println(UserDetails + "\t- ComandaPUNE: IOException: " + ex);
		} catch (InterruptedException ex) {
			System.out.println(UserDetails + "\t- ComandaPUNE: InterruptedException: " + ex);
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
	            WriteToClient("Error:Fisierul <" + oldFileName + "> nu poate fi redenumit la <" + newFileName + "> pentru ca numele dorit deja exista!:");
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
					  System.out.println(UserDetails + "\t- Directorul " + oldFileName + " a fost redenumit cu succes la " + newFileName);
					}
					else
					{
					  gateway.UpdateFileHashRelativePath(oldFileName, newFileName, false);
					  System.out.println(UserDetails + "\t- Fisierul " + oldFileName + " a fost redenumit cu succes la " + newFileName);
					}

					WriteToClient("ACKNOWLEDGE:");
					WriteToClient("EOCR:");
					return true;
	            }

	            WriteToClient("Error:Esec la redenumirea fisierului " + oldFileName + " la " + newFileName + ".:");
	            return false;
	        }
			else
			{
				WriteToClient("Error:Fisierul <" + oldFileName + "> nu poate fi redenumit la <" + newFileName + "> pentru ca nu exista.:");
			}
	    } catch (Exception ex) {
            System.out.println(UserDetails + "\t- ComandaREDENUMESTE - Exception: ");
            ex.printStackTrace();
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
						gateway.DeleteFileHash(fileName, true);
						System.out.println(UserDetails + "\t- Directorul " + fileName + " a fost sters cu succes (inclusiv continutul acestuia)." );
						WriteToClient("ACKNOWLEDGE:");
						//WriteToClient("EOCR:");
					}
					else
					{
						WriteToClient("Error:Esec la stergerea directorului " + fileToDelete + ".:");
					}

					return directoryDeleted;
				}
				else
				{
					boolean fileDeleted = fileToDelete.delete();
					if(fileDeleted)
					{
						gateway.DeleteFileHash(fileName, false);
						System.out.println(UserDetails + "\t- Fisierul " + fileName + " a fost sters cu succes." );
						WriteToClient("ACKNOWLEDGE:");
					}
					else
					{
						WriteToClient("Error:Esec la stergerea fisierului " + fileToDelete + ".:");
					}
					return fileDeleted;
				}
			}
			else 
			{
				WriteToClient("Error:Fisierul " + fileToDelete + " este deja sters pe server.:");
				return false;
			}
        }
        catch (Exception ex)
        {
            WriteToClient("Error:" + ex.getMessage() + ".:");
            System.out.println(UserDetails + "\t- CommandSTERGE - Exception: ");
            ex.printStackTrace();
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
            	System.out.println(UserDetails + "\t- Directorul " + fullPath + " a fost creat cu succes.");
                WriteToClient("ACKNOWLEDGE:");
            }
            else
            {
                WriteToClient("Error:Esec la crearea directorului " + folderName + ".:");
            }
        }catch(Exception e){
            System.out.println(UserDetails + "\t- CRDIRECTOR - Exception: ");
			e.printStackTrace();
        }

        return directoryCreated;
    }

    public boolean CommandGETFileHashes()
    {
		boolean validation = false;
        try
        {
            System.out.println(UserDetails + "\t- Obtine toate InregistrarileFisierelor pentru sincronizarea initiala.");
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
                WriteToClient("Error:Nu exista fisiere inregistrate deocamdata.:");
  			}
				
			WriteToClient(":EOCR:");
        } catch (Exception ex) {
            System.out.println(UserDetails + "\t- CommandOBTINEInregistrarileFisierelor - Exception: ");
            ex.printStackTrace();
        }

        return validation;
    }

    public void AppendUserToAssociatedEntities(String userName)
    {
		String hostName;
		String ipAddress;
		String port = "4444";

		try	{
			hostName = 	InetAddress.getLocalHost().getHostName();		
			ipAddress = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException ex) {
			System.out.println(UserDetails + "\t - AdaugaUtilizatorLaEntitatileAsociate - UnknownHostException:");
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
			System.out.println(UserDetails + "\t - StergeUtilizatorDinEntitatileAsociate - UnknownHostException:");
			ex.printStackTrace();
			return;
		}

    	gateway.DeleteAssociatedEntities(ipAddress, userName);
    }

	/// HELPERS ///
	private void WriteToClient(String message)
    {
        try {
        	if(message.contains("Error:"))
        		message.replace("Error:", "");
        	
        	if(!message.contains("ACKNOWLEDGE") && !message.contains("EOCR"))
        		System.out.println(UserDetails + "\t- " + message);

            byte[] messageBytes = message.getBytes();
            socketOutputStream.write(messageBytes, 0, messageBytes.length);
        }
        catch(Exception ex){
			System.out.println(UserDetails + "\t - ScrieCatreClient - Exception:");
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
