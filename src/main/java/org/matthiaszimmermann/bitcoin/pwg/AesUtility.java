package org.matthiaszimmermann.bitcoin.pwg;

import java.io.IOException;
import java.lang.reflect.Field;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class AesUtility {
	
	private static final byte [] SALT = new String("ldsqDQvEWJyWZMCl").getBytes();
	private static final int ITERATION_COUNT = 65536;
	private static final int KEY_STRENGTH = 256;
	
	private Cipher dcipher;
	private SecretKey key;
	private byte[] iv;

	/**
	 * warning: this is a hack, read more about this in the blog post cited below
	 * http://opensourceforgeeks.blogspot.ch/2014/09/how-to-install-java-cryptography.html
	 */
	static {
		try {
			Field field = Class
					.forName("javax.crypto.JceSecurity")
					.getDeclaredField("isRestricted");
			field.setAccessible(true);
			field.set(null, java.lang.Boolean.FALSE);
		} 
		catch (Exception ex) {
			ex.printStackTrace();
		}    	
	}
	
	public static String bytesToBase64(byte [] bytes) {
		return Base64.getEncoder().encodeToString(bytes);
	}
	
	public static byte [] base64ToBytes(String base64EncodedData) throws IOException {
		return Base64.getDecoder().decode(base64EncodedData);
	}			
	
	AesUtility(String passPhrase) throws Exception {
		this(passPhrase, SALT, ITERATION_COUNT, KEY_STRENGTH);
	}
	
	AesUtility(String passPhrase, byte [] salt, int iterationCount, int keyStrength) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidParameterSpecException {
		SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
		KeySpec spec = new PBEKeySpec(passPhrase.toCharArray(), salt, iterationCount, keyStrength);
		SecretKey secret = factory.generateSecret(spec);
		key = new SecretKeySpec(secret.getEncoded(), "AES");
		dcipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
	}
	
	public SecretKey getSecretKey() {
		return key;
	}

	public String encrypt(String data) throws Exception {
		dcipher.init(Cipher.ENCRYPT_MODE, key);
		iv = dcipher.getIV();
		byte[] utf8EncryptedData = dcipher.doFinal(data.getBytes());
		return bytesToBase64(utf8EncryptedData);
	}
	
	public void setIv(String base64encodedIv) throws IOException {
		iv = base64ToBytes(base64encodedIv);
	}
	
	public String getIv() {
		return bytesToBase64(iv);
	}

	public String decrypt(String base64encryptedData, String base64encodedIv) throws Exception {
//		AlgorithmParameters params = dcipher.getParameters();
//		iv = params.getParameterSpec(IvParameterSpec.class).getIV();
		iv = base64ToBytes(base64encodedIv);
		dcipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
		byte[] decryptedData = base64ToBytes(base64encryptedData);
		byte[] utf8 = dcipher.doFinal(decryptedData);
		return new String(utf8, "UTF8");
	}
}
