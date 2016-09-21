import java.io.*;
import java.io.File;

import java.net.*;
import java.nio.*;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.FileSystems;

import java.util.*;

public class Server {    
    private static int socketNo = 4444;
    private static Socket socket = null; 
    private static ServerSocket serverSocket = null;
    private static InputStream socketInputStream = null;
    private static OutputStream socketOutputStream = null;
    private static DAL dal = new DAL();

    public static void main(String[] args) throws Exception 
    {
        InitializeServer();

        String stringCommand;
        byte[] bytesCommand;
        boolean serverActive = true;

        try {
            while(serverActive)
            {
                System.out.println("\t.......................................");
                
                bytesCommand = new byte[1536];
                socketInputStream.read(bytesCommand);
                stringCommand = new String(bytesCommand);
                String[] parts = stringCommand.split(":");

                serverActive = TreatCommand(parts);
            }
        }
        catch(Exception ex) {
            System.out.println(ex);
        }

        socketInputStream.close();
        serverSocket.close();
        socket.close();
    }

    private static void InitializeServer() 
    {
        try {
            serverSocket = new ServerSocket(socketNo);
            System.out.println("Server socket " + socketNo + " is open, now, at " + (new Date()).toString());
        } catch (IOException ex) {
            System.out.println("Can't setup server on this port number (" + socketNo + ").");
        }

        try {
            socket = serverSocket.accept();
            System.out.println("Server acceped a new client socket (" + socket.getPort() + ").");
        } catch (IOException ex) {
            System.out.println("Can't accept client connection. ");
        }

        try {
            socketInputStream = new DataInputStream(socket.getInputStream());
            System.out.println("Socket input stream was set");
        } catch (IOException ex) {
            System.out.println("Can't get socket input stream. ");
        }
        
        try {
            socketOutputStream = new DataOutputStream(socket.getOutputStream());
            System.out.println("Socket output stream was set");
        } catch (IOException ex) {
            System.out.println("Can't get socket outpus stream. ");
        }       
    }

    private static boolean TreatCommand(String[] parts) 
    {
        boolean readData;
        
        if(parts[0].equals(CommandTypes.GET.toString()))
        {
            readData = CommandGET(parts[1]);
            System.out.println("GET state: " + readData);
        }
        else if(parts[0].equals(CommandTypes.PUT.toString()))
        {
            readData = CommandPUT(parts[1], Integer.parseInt(parts[2]));
            //System.out.println("PUT state: " + readData);
        }
        else if(parts[0].equals(CommandTypes.RENAME.toString()))
        {
            readData = CommandRENAME(parts[1], parts[2]);
            System.out.println("RENAME state: " + readData);
        }
        else if(parts[0].equals(CommandTypes.DELETE.toString()))
        {
            readData = CommandDELETE(parts[1]);
            System.out.println("DELETE state: " + readData);
        }
        else if(parts[0].equals(CommandTypes.MKDIR.toString()))
        {
            readData = CommandMKDIR(parts[1]);
            System.out.println("MKDIR state: " + readData);
        }
        else if(parts[0].equals(CommandTypes.KILL.toString())) 
        {
            System.out.println("The server was killed. Have a nice day!");
            return false; 
        }       
        return true;
    }
    
    private static boolean CommandGET(String fileName)
    {
        try {
            System.out.println("Geting the file: " + fileName);
            
            int count;
            byte[] bytes = new byte[256*1024];
            fileName = System.getProperty("user.dir") + "/dataSource/" + fileName;
            File sourceFile = new File(fileName);

            if(!sourceFile.exists()) {
                WriteToClient("Error: File doesn't exists!");
            }
            else if(sourceFile.isDirectory()) { 
                WriteToClient("Error: Server can't return a directory yet!");
            }
            else {
                WriteToClient("ACKNOWLEDGE");

                InputStream fileInputStream;
                fileInputStream = new FileInputStream(sourceFile);
                
                while ((count = fileInputStream.read(bytes)) > 0) {
                    socketOutputStream.write(bytes, 0, count);
                }
                
                return true;
            }
        } catch (IOException ex) {
                System.out.println("CommandGET: " + ex);
        }

        return false;
    }

    private static boolean CommandPUT(String fileName, Integer bytesToRead)
    {
        //if(enoughSpaceOnHdd)                                                      //!!!!!!!!!!!!
        WriteToClient("ACKNOWLEDGE");

        boolean readingData = true;
        byte[] buffer = new byte[bytesToRead];
        OutputStream out;
        
        Integer bytesRead = 0;
        Integer bytesLeft = bytesToRead;
        Integer nextPacketSize;
        Integer bufferLength;
        Integer bufferSize = 1536;
        try {
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

            fileName = System.getProperty("user.dir") + "/dataSource/" + fileName;
            out = new FileOutputStream(fileName);
            out.write(buffer, 0, buffer.length);

            WriteToClient("ACKNOWLEDGE");
            byte[] fileHashByte = new byte[2048];
            socketInputStream.read(fileHashByte);
            String fileHashString = new String(fileHashByte);
            String[] parts = fileHashString.split(":");
            if(parts[0].equals("FileHash"))
            {
                dal.InsertNewFileHash(fileName, Helper.getRelativePath(fileName), parts[1]);
                System.out.println("FileHash has been registred successfully.");
            	WriteToClient("ACKNOWLEDGE");
            }
            else
            {
            	WriteToClient("Error: FileHash wasn't sent.");
            	// ??? Delete the file and send again ? Or resend the FileHash
            }

            out.close();            
            return true;
        } catch (IOException ex) {
            System.out.println("CommandPUT: " + ex);
        }
        
        return false;
    }
    
    private static boolean CommandRENAME(String oldFileName, String newFileName)
    {

        String oldFilePath = System.getProperty("user.dir") + "/dataSource/" + oldFileName;
        String newFilePath = System.getProperty("user.dir") + "/dataSource/" + newFileName;

        File oldFile = new File(oldFilePath);
        File newFile = new File(newFilePath);   
        

        if(oldFile.exists())
        {
            WriteToClient("ACKNOWLEDGE");
            
            if(oldFile.renameTo(newFile))
            {
                //WriteToClient("Succes: file " + oldFileName + " renamed to " + newFileName);
                return true;
            }

            WriteToClient("Error: failed to rename the file " + oldFileName + " to " + newFileName);
            return false;
        }
        else if(newFile.exists())
        {
            WriteToClient("Error: Can't rename the file to the new name because a file with the new desired name already exists!");
            return false;
        }
        else
        {
            WriteToClient("File " + oldFileName + " doesn't exists!");
            return false;
        }
    }
    
    private static boolean CommandDELETE(String fileName)
    {
        File fileToDelete = new File(System.getProperty("user.dir") + "/dataSource/", fileName);
                
        try {
            if(fileToDelete.isDirectory())
            {
                boolean directoryDeleted = deleteDirectory(fileToDelete);
                if(directoryDeleted)
                    WriteToClient("ACKNOWLEDGE");
                else
                    WriteToClient("Error: failed to delete directory " + fileToDelete);
                return directoryDeleted;
            }


            boolean fileDeleted = fileToDelete.delete();
            if(fileDeleted)
                WriteToClient("ACKNOWLEDGE");
            else
                WriteToClient("Error: failed to delete file " + fileToDelete);
            return fileDeleted;
        } catch (Exception ex) {
            WriteToClient("Error: " + ex.getMessage());
            return false;
        }
    }

    private static boolean CommandMKDIR(String folderName) 
    {
        boolean directoryCreated = false;

        try{      
            String fullPath = System.getProperty("user.dir") + "/dataSource/" + folderName;
            File file = new File(fullPath);
            directoryCreated = file.mkdir();

            if(directoryCreated)
                WriteToClient("ACKNOWLEDGE");
            else
                WriteToClient("Error: failed to create directory " + folderName);
        }catch(Exception e){
           e.printStackTrace();
        }
        
        return directoryCreated;    
    }
    
    private static boolean deleteDirectory(File path) 
    {
        if( path.exists() ) {
          File[] files = path.listFiles();
          for(int i=0; i<files.length; i++) {
             if(files[i].isDirectory()) {
               deleteDirectory(files[i]);
             }
             else {
               files[i].delete();
             }
          }
        }
        return( path.delete() );
    }

    private static void WriteToClient(String message) 
    {
        try {
            byte[] messageBytes = message.getBytes();
            socketOutputStream.write(messageBytes, 0, messageBytes.length);
        }
        catch(Exception ex){
            System.out.println(ex.getMessage());
        }
    }
}