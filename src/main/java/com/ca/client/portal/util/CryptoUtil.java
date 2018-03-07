package com.ca.client.portal.util;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ca.client.portal.ex.PortalAPIRuntimeException;

public class CryptoUtil {
	private static final String key = "%nc&,JZA21LV**%jLf$~;nf}^n+{GSBBtm}^?CrT9M$6^]oTy:is7<-|*POSMg&";
	private static final Logger log = LoggerFactory.getLogger(CryptoUtil.class);

	public static void main(String[] args) throws Exception {
		//String key = "%nc&,JZA21LV**%jLf$~;nf}^n+{GSBBtm}^?CrT9M$6^]oTy:is7<-|*POSMg&";
		String clean = "b3e3886";

		String encrypted = encrypt(clean);
		System.out.println("Encrypted password: " + encrypted);
		String decrypted = decrypt(encrypted);
		System.out.println("Decrypted: " + decrypted);
	}

	public static String encrypt(String plainText) {
		byte[] clean = plainText.getBytes();

		// Generating IV.
		int ivSize = 16;
		byte[] iv = new byte[ivSize];
		SecureRandom random = new SecureRandom();
		random.nextBytes(iv);
		IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

		byte[] encrypted;
		try {
			// Hashing key.
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			digest.update(key.getBytes("UTF-8"));
			byte[] keyBytes = new byte[16];
			System.arraycopy(digest.digest(), 0, keyBytes, 0, keyBytes.length);
			SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");

			// Encrypt.
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
			encrypted = cipher.doFinal(clean);
		} catch (InvalidKeyException | NoSuchAlgorithmException | UnsupportedEncodingException | NoSuchPaddingException
				| InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
			log.error("Error encrypting password");
			throw new PortalAPIRuntimeException(e.getMessage());
		}

		// Combine IV and encrypted part.
		byte[] encryptedIVAndText = new byte[ivSize + encrypted.length];
		System.arraycopy(iv, 0, encryptedIVAndText, 0, ivSize);
		System.arraycopy(encrypted, 0, encryptedIVAndText, ivSize, encrypted.length);

		return Base64.encodeBase64String(encryptedIVAndText);
	}

	public static String decrypt(String encryptedPassword) {
		int ivSize = 16;
		int keySize = 16;
		byte[] encryptedIvTextBytes = Base64.decodeBase64(encryptedPassword);
		// Extract IV.
		byte[] iv = new byte[ivSize];
		System.arraycopy(encryptedIvTextBytes, 0, iv, 0, iv.length);
		IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

		// Extract encrypted part.
		int encryptedSize = encryptedIvTextBytes.length - ivSize;
		byte[] encryptedBytes = new byte[encryptedSize];
		System.arraycopy(encryptedIvTextBytes, ivSize, encryptedBytes, 0, encryptedSize);

		byte[] decrypted;
		try {
			// Hash key.
			byte[] keyBytes = new byte[keySize];
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(key.getBytes());
			System.arraycopy(md.digest(), 0, keyBytes, 0, keyBytes.length);
			SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");

			// Decrypt.
			Cipher cipherDecrypt = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipherDecrypt.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
			decrypted = cipherDecrypt.doFinal(encryptedBytes);
		} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
				| InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
			log.error("Error decrypting password");
			throw new PortalAPIRuntimeException(e.getMessage());
		}

		return new String(decrypted);
	}

}
