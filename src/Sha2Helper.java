import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;

public class Sha2Helper {	
	private static int iterationCount ;
	private static byte[] salt;	
	private static MessageDigest digest;
	
	static {
		try {
			iterationCount = 5;
			salt = base64ToByte("MySaltChars");
			digest = MessageDigest.getInstance("SHA-256");
		} catch (IOException ex) {
			System.out.println("Sha2Helper IOException");
			ex.printStackTrace();
		} catch (NoSuchAlgorithmException ex) {
			System.out.println("Sha2Helper NoSuchAlgorithmException");
			ex.printStackTrace();
		}
	}	
	
	public static String encode(String password) {
		String encoded = "";
		try {
			digest.reset();
			digest.update(salt);

			byte[] btPass = digest.digest(password.getBytes("UTF-8"));
			for (int i = 0; i < iterationCount; i++) {
				digest.reset();
				btPass = digest.digest(btPass);
			}
			
			encoded = byteToBase64(btPass);
		} catch(Exception ex) {
			ex.printStackTrace();
		}
		return encoded;
	}
	
	public static boolean verify(String password, String storedPassword)  {
		String encryptedPassword = encode(password);		
		return encryptedPassword.equals(storedPassword);
	}

	private static byte[] base64ToByte(String str) throws IOException {	    	
	    return DatatypeConverter.parseBase64Binary(str);
	}

	private static String byteToBase64(byte[] bt) {		
	    return DatatypeConverter.printBase64Binary(bt);
	}
}
