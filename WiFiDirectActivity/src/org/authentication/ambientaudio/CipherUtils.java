package org.authentication.ambientaudio;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class CipherUtils {
	/*
	 * Encrypt a file using AES
	 * The key is hashed with SHA-256
	 */
	public static void encrypt(String fileIn, String fileOut, byte key[]) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
	    // Here you read the cleartext.
	    FileInputStream fis = new FileInputStream(fileIn);
	    // This stream write the encrypted text. This stream will be wrapped by another stream.
	    FileOutputStream fos = new FileOutputStream(fileOut);

	    // Length is 32 bytes
	    //byte key[] = "1010110101010101010100101111001001001001001001011110111100000011010110101010101010100101111001001001001001001011110111100000011010110101010101010100101111001001001001001001011110111100000011010110101010101010100101111001001001001001001011110111100000011010110101010101010100101111001001001001001001011110111100000011010110101010101010100101111001001001001001001011110111100000011010110101010101010100101111001001001001001001011110111100000011010110101010101010100101111001001001001001001011110111100001111000011".getBytes("UTF-8");
	    MessageDigest sha = MessageDigest.getInstance("SHA-256");
	    key = sha.digest(key);
	    SecretKeySpec sks = new SecretKeySpec(key, "AES");
	    // Create cipher
	    Cipher cipher = Cipher.getInstance("AES");
	    cipher.init(Cipher.ENCRYPT_MODE, sks);
	    // Wrap the output stream
	    CipherOutputStream cos = new CipherOutputStream(fos, cipher);
	    // Write bytes
	    int b;
	    byte[] d = new byte[8];
	    while((b = fis.read(d)) != -1) {
	        cos.write(d, 0, b);
	    }
	    // Flush and close streams.
	    cos.flush();
	    cos.close();
	    fis.close();
	}
	
	/*
	 * Decrypt a file using AES
	 * The key is hashed with SHA-256
	 */
	public static void decrypt(String fileIn, String fileOut, byte key[]) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
	    FileInputStream fis = new FileInputStream(fileIn);
	    FileOutputStream fos = new FileOutputStream(fileOut);
	    
	 // Length is 32 bytes
	    //byte key[] = "1010110101010101010100101111001001001001001001011110111100000011010110101010101010100101111001001001001001001011110111100000011010110101010101010100101111001001001001001001011110111100000011010110101010101010100101111001001001001001001011110111100000011010110101010101010100101111001001001001001001011110111100000011010110101010101010100101111001001001001001001011110111100000011010110101010101010100101111001001001001001001011110111100000011010110101010101010100101111001001001001001001011110111100001111000011".getBytes("UTF-8");
	    MessageDigest sha = MessageDigest.getInstance("SHA-256");
	    key = sha.digest(key);
	    SecretKeySpec sks = new SecretKeySpec(key, "AES");
	    Cipher cipher = Cipher.getInstance("AES");
	    cipher.init(Cipher.DECRYPT_MODE, sks);
	    
	    CipherInputStream cis = new CipherInputStream(fis, cipher);
	    int b;
	    byte[] d = new byte[8];
	    while((b = cis.read(d)) != -1) {
	        fos.write(d, 0, b);
	    }
	    
	    fos.flush();
	    fos.close();
	    cis.close();
	}
	
	public static byte[] HashSHA256(byte data[]) throws NoSuchAlgorithmException {
		MessageDigest sha = MessageDigest.getInstance("SHA-256");
	    return sha.digest(data);
	}
}
