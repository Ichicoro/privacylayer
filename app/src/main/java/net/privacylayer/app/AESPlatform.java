package net.privacylayer.app;

import android.util.Base64;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AESPlatform {

    // AES-GCM parameters
    public static final int DEFAULT_AES_KEY_SIZE = 128; // in bits
    public static final int GCM_NONCE_LENGTH = 12; // in bytes
    public static final int GCM_TAG_LENGTH = 16; // in bytes

    public static AESMessage encrypt(String inputString, String key) throws
            InvalidKeyException,
            InvalidAlgorithmParameterException,
            IllegalBlockSizeException,
            BadPaddingException,
            UnsupportedEncodingException,
            NoSuchAlgorithmException,
            NoSuchPaddingException {
        return encrypt(inputString, key, DEFAULT_AES_KEY_SIZE);
    }

    public static AESMessage encrypt(String inputString, String keyString, int keySize) throws
            InvalidKeyException,
            InvalidAlgorithmParameterException,
            IllegalBlockSizeException,
            BadPaddingException,
            UnsupportedEncodingException,
            NoSuchAlgorithmException,
            NoSuchPaddingException {
        byte[] input = inputString.getBytes("UTF-8");
        final byte[] nonce = new byte[GCM_NONCE_LENGTH];
        Random random = new Random();
        random.nextBytes(nonce);

        byte[] encrypted = operate(Cipher.ENCRYPT_MODE, input, keyString, keySize, nonce);
        return new AESMessage(encrypted, nonce);
    }

    public static String decrypt(AESMessage message, String keyString) throws
            InvalidKeyException,
            InvalidAlgorithmParameterException,
            IllegalBlockSizeException,
            BadPaddingException,
            UnsupportedEncodingException,
            NoSuchAlgorithmException,
            NoSuchPaddingException {
        return decrypt(message, keyString, DEFAULT_AES_KEY_SIZE);
    }

    public static String decrypt(AESMessage message, String keyString, int keySize) throws
            InvalidKeyException,
            InvalidAlgorithmParameterException,
            IllegalBlockSizeException,
            BadPaddingException,
            UnsupportedEncodingException,
            NoSuchAlgorithmException,
            NoSuchPaddingException {
        byte[] decrypted = operate(Cipher.DECRYPT_MODE, message.content, keyString, keySize, message.nonce);
        return new String(decrypted, "UTF-8");
    }

    private static byte[] operate(int mode, byte[] input, String keyString, int keySize, byte[] nonce)
            throws UnsupportedEncodingException,
            NoSuchAlgorithmException,
            InvalidKeyException,
            InvalidAlgorithmParameterException,
            IllegalBlockSizeException,
            BadPaddingException, NoSuchPaddingException {

        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(keySize);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

        byte[] key = keyString.getBytes("UTF-8");

        MessageDigest sha = MessageDigest.getInstance("SHA-1");
        key = sha.digest(key);
        key = Arrays.copyOf(key, 16); // use only first 128 bit
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, nonce);
        cipher.init(mode, secretKeySpec, spec);

/*
        Todo: add any additional authenticated data
        byte[] aad = "Whatever I like".getBytes("UTF-8");
        cipher.updateAAD(aad);
*/

        return cipher.doFinal(input);
    }


    // Example implementation
    private static void example() throws Exception {
        AESMessage encrypted = AESPlatform.encrypt("We Ichi!", ":3");     // We
        System.out.println(encrypted.toString());
        String decrypted = AESPlatform.decrypt(encrypted, ":3");
        System.out.println(decrypted);
    }
}

class AESMessage {
    byte[] content;
    byte[] nonce;

    public AESMessage(byte[] in_content, byte[] in_nonce) {
        content = in_content;
        nonce = in_nonce;
    }

    public AESMessage(String input) throws UnsupportedEncodingException {
        String[] parts = input.split(":");
        content = parts[0].getBytes("UTF-8");
        nonce = parts[1].getBytes("UTF-8");
    }

    @Override
    public String toString() {
        return new String(Base64.encode(content, Base64.DEFAULT)) + ":"
                + new String(Base64.encode(nonce, Base64.DEFAULT));
    }
}
