package net.gsantner.markor.util;

import android.os.Build;
import android.support.annotation.RequiresApi;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Helper-Class which encrypt a text based on a password.
 */
@SuppressWarnings("CharsetObjectCanBeUsed")
class CryptWithPassword {

    private final int saltLength;
    private Random random;

    /**
     * @param random     recommended SecureRandom.getInstanceStrong(), which could be very slow.  A compromise could be SecureRandom.getInstance("SHA1PRNG")
     * @param saltLength recommended 64
     */
    private CryptWithPassword(Random random, int saltLength) {
        this.random = random;
        this.saltLength = saltLength;
    }

    protected CryptWithPassword() {
        this(new SecureRandom(), 64);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    protected byte[] encrypt(String plainText, char[] password) throws EncryptionFailedException {
        try {
            final byte[] salt = getRandomBytes(saltLength);
            final byte[] nonce = getRandomBytes(32);
            final SecretKey key = createKeyFromPassword(password, salt);
            final byte[] cryptedBytes = getCipherGcm(key, Cipher.ENCRYPT_MODE, nonce).doFinal(plainText.getBytes("UTF-8"));
            final byte[] result = new byte[nonce.length + salt.length + cryptedBytes.length];
            System.arraycopy(nonce, 0, result, 0, 32);
            System.arraycopy(salt, 0, result, 32, saltLength);
            System.arraycopy(cryptedBytes, 0, result, 32 + saltLength, cryptedBytes.length);
            return result;
        } catch (Exception e) {
            throw new EncryptionFailedException("Can't encrypt text.", e);
        }

    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    protected String decrypt(byte[] encryptedText, char[] password) throws EncryptionFailedException {
        try {
            final byte[] nonce = Arrays.copyOfRange(encryptedText, 0, 32);
            final byte[] salt = Arrays.copyOfRange(encryptedText, 32, 32 + saltLength);
            final byte[] encodedBytes = Arrays.copyOfRange(encryptedText, 32 + saltLength, encryptedText.length);
            final SecretKey key = createKeyFromPassword(password, salt);
            byte[] decryptedCipherTextBytes = getCipherGcm(key, Cipher.DECRYPT_MODE, nonce).doFinal(encodedBytes);
            return new String(decryptedCipherTextBytes, "UTF-8");
        } catch (Exception e) {
            throw new EncryptionFailedException("Can't encrypt text.", e);
        }
    }

    private SecretKey createKeyFromPassword(char[] password, byte[] salt) throws EncryptionFailedException {
        try {
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
            KeySpec passwordBasedEncryptionKeySpec = new PBEKeySpec(password, salt, 10000, 256);
            Arrays.fill(password, Character.MIN_VALUE);
            SecretKey secretKeyFromPBKDF2 = secretKeyFactory.generateSecret(passwordBasedEncryptionKeySpec);
            return new SecretKeySpec(secretKeyFromPBKDF2.getEncoded(), "AES");
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new EncryptionFailedException("Error creating key from password: " + e.getMessage(), e);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private Cipher getCipherGcm(SecretKey key, int encryptMode, byte[] nonce) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(16 * 8, nonce);
        cipher.init(encryptMode, key, spec);
        return cipher;
    }

    /**
     * Generate a salt
     *
     * @param length recommended value is 64
     * @return a byte array with random values
     */
    private byte[] getRandomBytes(int length) {
        final byte[] salt = new byte[length];
        random.nextBytes(salt);
        return salt;
    }

    protected static final class EncryptionFailedException extends Exception {
        EncryptionFailedException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
