package net.privacylayer.app;

import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.util.Base64;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AESPlatform {

    // AES-GCM parameters
    public static final int DEFAULT_AES_KEY_SIZE = 128; // in bits
    public static final int GCM_NONCE_LENGTH = 12; // in bytes
    public static final int GCM_TAG_LENGTH = 16; // in bytes

    public static AESMessage encrypt(@NonNull String inputString, @NonNull String key) throws Exception {
        return encrypt(inputString, key, DEFAULT_AES_KEY_SIZE);
    }

    public static AESMessage encrypt(@NonNull String inputString, @NonNull String keyString, int keySize) throws Exception {
        byte[] input = inputString.getBytes("UTF-8");
        final byte[] nonce = new byte[GCM_NONCE_LENGTH];
        Random random = new Random();
        random.nextBytes(nonce);

        byte[] encrypted = operate(Cipher.ENCRYPT_MODE, input, keyString, keySize, nonce);
        return new AESMessage(encrypted, nonce);
    }

    public static String decrypt(@NonNull AESMessage message, @NonNull String keyString) throws Exception {
        return decrypt(message, keyString, DEFAULT_AES_KEY_SIZE);
    }

    public static String decrypt(@NonNull AESMessage message, @NonNull String keyString, int keySize) throws Exception {
        byte[] decrypted = operate(Cipher.DECRYPT_MODE, message.content, keyString, keySize, message.nonce);
        return new String(decrypted, "UTF-8");
    }

    private static byte[] operate(int mode, byte[] input, String keyString, int keySize, byte[] nonce)
            throws Exception {

        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(keySize);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");

        byte[] key = keyString.getBytes("UTF-8");

        MessageDigest sha = MessageDigest.getInstance("SHA-1");
        key = sha.digest(key);
        key = Arrays.copyOf(key, 16); // use only first 128 bit
        Key AESkey = new SecretKeySpec(key, "AES");

        cipher.init(mode, AESkey, new IvParameterSpec(nonce));
/*
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

    public AESMessage(@NonNull byte[] in_content, @NonNull byte[] in_nonce) {
        content = in_content;
        nonce = in_nonce;
    }

    public AESMessage(@NonNull String input) throws UnsupportedEncodingException {
        String[] parts = input.split(":");
        if (parts.length != 2)
            throw new IllegalArgumentException("Input isn't a properly formatted AES message.");
        content = Base64.decode(parts[0].getBytes("UTF-8"), Base64.DEFAULT);
        nonce = Base64.decode(parts[1].getBytes("UTF-8"), Base64.DEFAULT);
    }

    @Override
    public String toString() {
        return new String(Base64.encode(content, Base64.NO_WRAP)) + ":"
                + new String(Base64.encode(nonce, Base64.NO_WRAP));
    }
}

class DiffieHellman {
    // Based on https://gist.github.com/zcdziura/7652286
    public static byte[] iv = new SecureRandom().generateSeed(16);

    public static void test() throws Exception {
        String plainText = "Look mah, I'm a message!";
        Log.i("We/We", "Original plaintext message: " + plainText);

        // Initialize two key pairs
        KeyPair keyPairA = generateECKeys();
        KeyPair keyPairB = generateECKeys();

        // Create two AES secret keys to encrypt/decrypt the message
        SecretKey secretKeyA = generateSharedSecret(keyPairA.getPrivate(),
                keyPairB.getPublic());
        SecretKey secretKeyB = generateSharedSecret(keyPairB.getPrivate(),
                keyPairA.getPublic());

        // Encrypt the message using 'secretKeyA'
        String cipherText = encryptString(secretKeyA, plainText);
        Log.i("We/We", "Encrypted cipher text: " + cipherText);

        // Decrypt the message using 'secretKeyB'
        String decryptedPlainText = decryptString(secretKeyB, cipherText);
        Log.i("We/We", "Decrypted cipher text: " + decryptedPlainText);
    }

    public static PrivateKey loadPrivateKey(String key64) throws GeneralSecurityException {
        byte[] clear = Base64.decode(key64, Base64.NO_WRAP);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(clear);
        KeyFactory fact = KeyFactory.getInstance("DH");
        PrivateKey priv = fact.generatePrivate(keySpec);
        Arrays.fill(clear, (byte) 0);
        return priv;
    }


    public static PublicKey loadPublicKey(String stored) throws GeneralSecurityException {
        byte[] data = Base64.decode(stored, Base64.NO_WRAP);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(data);
        KeyFactory fact = KeyFactory.getInstance("DH");
        return fact.generatePublic(spec);
    }

    public static String savePrivateKey(PrivateKey priv) throws GeneralSecurityException {
        KeyFactory fact = KeyFactory.getInstance("DH");
        PKCS8EncodedKeySpec spec = fact.getKeySpec(priv,
                PKCS8EncodedKeySpec.class);
        byte[] packed = spec.getEncoded();
        String key64 = Base64.encodeToString(packed, Base64.NO_WRAP);

        Arrays.fill(packed, (byte) 0);
        return key64;
    }


    public static String savePublicKey(PublicKey publ) throws GeneralSecurityException {
        KeyFactory fact = KeyFactory.getInstance("DH");
        X509EncodedKeySpec spec = fact.getKeySpec(publ,
                X509EncodedKeySpec.class);
        return Base64.encodeToString(spec.getEncoded(), Base64.NO_WRAP);
    }

    public static KeyPair generateECKeys() {
        try {
            /* The factors were self-generated [@CapacitorSet]. Source:
            import java.security.AlgorithmParameterGenerator;
            import java.security.AlgorithmParameters;
            import javax.crypto.spec.DHParameterSpec;
            public class Main {
              public static void main(String[] argv) throws Exception {
                AlgorithmParameterGenerator paramGen = AlgorithmParameterGenerator.getInstance("DH");
                paramGen.init(2048);
                AlgorithmParameters params = paramGen.generateParameters();
                DHParameterSpec dhSpec = params.getParameterSpec(DHParameterSpec.class);
                System.out.println(dhSpec.getG().toString(16));
                System.out.println(dhSpec.getP().toString(16));
              }
            } */
            BigInteger g = new BigInteger(
                    "93445990947cef561f52de0fa07a232b07ba78c6d1b3a09d1b838de4d3c51f843c307427b963b2060fb30d8088e5bc8459cf4201987e5d83c2a9c2b72cee53f7905c92c6425f9f97df71b8c09ea97e8435c30b57d6e84bb134af3aeaacf4047da02716c0b85c1b403dba306569aaaa6fb7b01861c4f692af24ad89f02408762380dbdd7186e36d59edf9d2abd93bfe8f04e4e20a214df66dabd02d1b15e6b943ad73a5695110286d6e3b4d35f8f08ece05728645bfb85d29ec561d6db16ac4bb5f58805eea1298b29161f74bac3ff9003dabfcc5fdc7604fb7bfdbf96e9c6c8ca7b357a74a94f62752a780a451bed793400b56a1a9414fa38458ed797896ca8c",
                    16);
            BigInteger p = new BigInteger(
                    "ab0eab856a13bdc2c35ae735b04b6424f7c8d33beae9f7d28ff58f84a845e727a2cb3d3fcf716ff839e65fbeaa4f9b38eddd3b87c03b1bf4e5dd86f211a7845d67d2a44a64b5126776fc5a210196020e6552930fbb5f98f5f23589d51dee3fbdb9e714989ad966465ee56e3551b216f0e15c257c0aeddbc1e6b394341a4c07a5412e22cda2c052d232ea68c9709d4e1fe359780a9842f7b30130a7bea563c31897e95cc7cff834ac46aa4d56a1f75b5437dd444d7be4e33c069c340020250c713d6219c5b62d252ad348220254ff77cd6ba54cdd0f37ec6d6cc9bd22ea6794b6237f6fb056edfd7132d4a1be3ddc7cfe6fe57b974d5a9d67ac7059cab02b2a7b",
                    16);
            final DHParameterSpec dhSpec = new DHParameterSpec(p, g, 511);
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DH");
            keyGen.initialize(dhSpec);
            return keyGen.generateKeyPair();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static SecretKey generateSharedSecret(PrivateKey privateKey,
                                                 PublicKey publicKey) throws Exception {
        KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");
        keyAgreement.init(privateKey);
        keyAgreement.doPhase(publicKey, true);

        return keyAgreement.generateSecret("AES");
    }

    public static String encryptString(SecretKey key, String plainText) {
        try {
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
            byte[] plainTextBytes = plainText.getBytes("UTF-8");
            byte[] cipherText;

            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
            cipherText = new byte[cipher.getOutputSize(plainTextBytes.length)];
            int encryptLength = cipher.update(plainTextBytes, 0,
                    plainTextBytes.length, cipherText, 0);
            encryptLength += cipher.doFinal(cipherText, encryptLength);

            return bytesToHex(cipherText);
        } catch (NoSuchAlgorithmException | NoSuchProviderException
                | NoSuchPaddingException | InvalidKeyException
                | InvalidAlgorithmParameterException
                | UnsupportedEncodingException | ShortBufferException
                | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String decryptString(SecretKey key, String cipherText) {
        try {
            Key decryptionKey = new SecretKeySpec(key.getEncoded(),
                    key.getAlgorithm());
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
            byte[] cipherTextBytes = hexToBytes(cipherText);
            byte[] plainText;

            cipher.init(Cipher.DECRYPT_MODE, decryptionKey, ivSpec);
            plainText = new byte[cipher.getOutputSize(cipherTextBytes.length)];
            int decryptLength = cipher.update(cipherTextBytes, 0,
                    cipherTextBytes.length, plainText, 0);
            decryptLength += cipher.doFinal(plainText, decryptLength);

            return new String(plainText, "UTF-8");
        } catch (NoSuchAlgorithmException | NoSuchProviderException
                | NoSuchPaddingException | InvalidKeyException
                | InvalidAlgorithmParameterException
                | IllegalBlockSizeException | BadPaddingException
                | ShortBufferException | UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String bytesToHex(byte[] data, int length) {
        String digits = "0123456789ABCDEF";
        StringBuffer buffer = new StringBuffer();

        for (int i = 0; i != length; i++) {
            int v = data[i] & 0xff;

            buffer.append(digits.charAt(v >> 4));
            buffer.append(digits.charAt(v & 0xf));
        }

        return buffer.toString();
    }

    public static String bytesToHex(byte[] data) {
        return bytesToHex(data, data.length);
    }

    public static byte[] hexToBytes(String string) {
        int length = string.length();
        byte[] data = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            data[i / 2] = (byte) ((Character.digit(string.charAt(i), 16) << 4) + Character
                    .digit(string.charAt(i + 1), 16));
        }
        return data;
    }

    public static KeyPair prepareKeyPair(SharedPreferences sharedPrefs) throws GeneralSecurityException {
        final String TAG = MainActivity.TAG;
        // Generate a keypair if it doesn't already exist
        String PubKeyString = sharedPrefs.getString("PublicKey", "");
        String PrivKeyString = sharedPrefs.getString("PrivateKey", "");
        if (!PubKeyString.equals("")) {
            PublicKey pubKey = DiffieHellman.loadPublicKey(PubKeyString);
            PrivateKey privKey = DiffieHellman.loadPrivateKey(PrivKeyString);
            KeyPair myKeypair = new KeyPair(pubKey, privKey);
            Log.i(TAG, "Keypair rebuilt successfully.");
            return myKeypair;
        } else {
            KeyPair kp = DiffieHellman.generateECKeys();
            sharedPrefs.edit()
                    .putString("PublicKey", DiffieHellman.savePublicKey(kp.getPublic()))
                    .putString("PrivateKey", DiffieHellman.savePrivateKey(kp.getPrivate()))
                    .apply();
            Log.i(TAG, "DH keypair generated successfully.");
            return kp;
        }
    }
}