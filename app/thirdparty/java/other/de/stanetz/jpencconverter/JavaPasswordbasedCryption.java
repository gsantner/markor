package other.de.stanetz.jpencconverter;


import android.os.Build;

import androidx.annotation.RequiresApi;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Helper-Class which encrypt text or bytes based on a password. The encrypted bytes used following-format:
 *
 * <ul>
 *     <li>4 bytes - define the version.</li>
 *     <li>salt bytes - bytes to salt the password. The length depends on version.</li>
 *     <li>nonce bytes - bytes as nonce for cipher depends. The length  on version.</li>
 *     <li>content bytes - the encrypted content-bytes.</li>
 * </ul>
 * This Class need Android KitKat to run om android devices.
 */
// COPIED FROM https://gitlab.com/opensource21/jpencconverter/-/blob/v0.2.1/src/main/java/de/stanetz/jpencconverter/cryption/JavaPasswordbasedCryption.java
@SuppressWarnings("deprecation")
@RequiresApi(api = Build.VERSION_CODES.M)
public class JavaPasswordbasedCryption {

    /**
     * Recommended extension for encrypted files.
     */
    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB_MR2)
    public static final String DEFAULT_ENCRYPTION_EXTENSION = ".jenc";
    private final Version version;
    private final Random random;

    /**
     * Create a new Instance of the given android api version.
     *
     * @param random     strongest SecureRandom.getInstanceStrong(), which could be very slow.  A compromise could be SecureRandom.getInstance("SHA1PRNG") or new SecureRandom.
     * @param apiVersion the android api-version which is used to search for the best version.
     */
    public JavaPasswordbasedCryption(int apiVersion, Random random) {
        this(getVersionForAndroid(apiVersion), random);
    }

    /**
     * Create a new Instance of the given version.
     *
     * @param random  strongest SecureRandom.getInstanceStrong(), which could be very slow.  A compromise could be SecureRandom.getInstance("SHA1PRNG") or new SecureRandom.
     * @param version the container-version
     */
    public JavaPasswordbasedCryption(Version version, Random random) {
        this.version = version;
        this.random = random;
    }


    private static Version getVersionForAndroid(int apiVersion) {
        if (apiVersion >= 26) {
            return Version.V001;
        } else if (apiVersion >= 23) {
            return Version.U001;
        } else {
            throw new IllegalArgumentException("Minimal API-Version is 23, so " + apiVersion + " isn't supported");
        }
    }

    /**
     * Extract the version from encrypted bytes.
     *
     * @param encryptedText the encrypted bytes.
     * @return the used version.
     */
    public static Version getVersion(byte[] encryptedText) {
        final byte[] versionBytes = Arrays.copyOfRange(encryptedText, 0, Version.NAME_LENGTH);
        return Version.valueOf(new String(versionBytes, StandardCharsets.US_ASCII));
    }

    /**
     * Decrypt the text.
     *
     * @param encryptedText encrypted text as bytes.
     * @param password      the password <b>Warning!</b> the array will be filled with 0!
     * @return decrypted text.
     */
    public static String getDecryptedText(byte[] encryptedText, char[] password) {
        return new JavaPasswordbasedCryption(getVersion(encryptedText), null).decrypt(encryptedText, password);
    }


    /**
     * Encrypt the given text with the password.
     *
     * @param plainText the decrypted text.
     * @param password  the password to create the key <b>Warning!</b> the array will be filled with 0!
     * @return encrypted-bytes.
     * @throws EncryptionFailedException when something goes wrong.
     */
    public byte[] encrypt(String plainText, char[] password) throws EncryptionFailedException {
        return encryptBytes(plainText.getBytes(StandardCharsets.UTF_8), password);
    }

    /**
     * Encrypt the given byte-array with the password.
     *
     * @param decryptedBytes the decrypted text.
     * @param password       the password to create the key <b>Warning!</b> the array will be filled with 0!
     * @return encrypted-bytes.
     * @throws EncryptionFailedException when something goes wrong.
     */
    public byte[] encryptBytes(byte[] decryptedBytes, char[] password) throws EncryptionFailedException {
        try {
            final byte[] salt = getRandomBytes(version.keySaltLength);
            final byte[] nonce = getRandomBytes(version.nonceLenth);
            final SecretKey key = createKeyFromPassword(password, salt);
            final byte[] cryptedBytes = getCipher(key, Cipher.ENCRYPT_MODE, nonce).doFinal(decryptedBytes);
            final byte[] result = new byte[Version.NAME_LENGTH + nonce.length + salt.length + cryptedBytes.length];
            System.arraycopy(version.name().getBytes(StandardCharsets.US_ASCII), 0, result, 0, Version.NAME_LENGTH);
            System.arraycopy(nonce, 0, result, Version.NAME_LENGTH, version.nonceLenth);
            System.arraycopy(salt, 0, result, Version.NAME_LENGTH + version.nonceLenth, version.keySaltLength);
            System.arraycopy(cryptedBytes, 0, result, Version.NAME_LENGTH + version.nonceLenth + version.keySaltLength, cryptedBytes.length);
            return result;
        } catch (Exception e) {
            throw new EncryptionFailedException("Can't encrypt text.", e);
        }
    }


    /**
     * Decrypt the given bytes with the password.
     *
     * @param encryptedText encrypted-bytes with version
     * @param password      the password to create the key. <b>Warning!</b> the array will be filled with 0!!
     * @return the decrypted text.
     * @throws EncryptionFailedException when something goes wrong.
     */
    public String decrypt(byte[] encryptedText, char[] password) throws EncryptionFailedException {
        byte[] decryptedCipherTextBytes = decryptBytes(encryptedText, password);
        return new String(decryptedCipherTextBytes, StandardCharsets.UTF_8);
    }

    /**
     * Decrypt the given bytes with the password.
     *
     * @param encrypted encrypted-bytes with version
     * @param password  the password to create the key <b>Warning!</b> the array will be filled with 0!
     * @return the decrypted bytes.
     * @throws EncryptionFailedException when something goes wrong.
     */
    public byte[] decryptBytes(byte[] encrypted, char[] password) throws EncryptionFailedException {
        try {
            final Version currentVersion = getVersion(encrypted);
            if (currentVersion != version) {
                throw new IllegalArgumentException("The current version " + currentVersion.name() + " differs from configured version " + version.name());
            }
            int from = Version.NAME_LENGTH;
            int to = from + version.nonceLenth;
            final byte[] nonce = Arrays.copyOfRange(encrypted, from, to);
            from = to;
            to = from + version.keySaltLength;
            final byte[] salt = Arrays.copyOfRange(encrypted, from, to);
            from = to;
            to = encrypted.length;
            final byte[] encodedBytes = Arrays.copyOfRange(encrypted, from, to);
            final SecretKey key = createKeyFromPassword(password, salt);
            return getCipher(key, Cipher.DECRYPT_MODE, nonce).doFinal(encodedBytes);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException |
                 InvalidAlgorithmParameterException | InvalidKeyException |
                 IllegalBlockSizeException | BadPaddingException e) {
            throw new EncryptionFailedException("Can't decrypt text.", e);
        }
    }

    private SecretKey createKeyFromPassword(char[] password, byte[] salt) {
        try {
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(version.keyFactory);
            KeySpec passwordBasedEncryptionKeySpec = new PBEKeySpec(password, salt, version.keyIterationCount, version.keyLength);
            Arrays.fill(password, Character.MIN_VALUE);
            SecretKey secretKeyFromPBKDF2 = secretKeyFactory.generateSecret(passwordBasedEncryptionKeySpec);
            return new SecretKeySpec(secretKeyFromPBKDF2.getEncoded(), version.keyAlgorithm);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new EncryptionFailedException("Error creating key from password: " + e.getMessage(), e);
        }
    }

    private Cipher getCipher(SecretKey key, int encryptMode, byte[] nonce) throws
            NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        if (version == Version.V001 || version == Version.U001) {
            Cipher cipher = Cipher.getInstance(version.cipher);
            GCMParameterSpec spec = new GCMParameterSpec(16 * 8, nonce);
            cipher.init(encryptMode, key, spec);
            return cipher;
        }
        throw new IllegalStateException("Unknown version " + version.name());
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

    /**
     * Something which goes wrong at encryption.
     */
    public static final class EncryptionFailedException extends RuntimeException {
        EncryptionFailedException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Version of encryption.
     * Version which starts with an U are Versions which are unsecure compared to a V-Version.
     */
    @SuppressWarnings("DeprecatedIsStillUsed")
    public enum Version {
        V001("PBKDF2WithHmacSHA512", 10000, 256, "AES", 64, "AES/GCM/NoPadding", 32),

        /**
         * Weaker version of V001. Needed for old android-devices.
         *
         * @deprecated please use {@link #V001} if possible.
         */
        @Deprecated
        U001("PBKDF2WithHmacSHA1", 10000, 256, "AES", 64, "AES/GCM/NoPadding", 32);

        /**
         * Define the length of the Versionnames.
         */
        public static final int NAME_LENGTH = 4;
        private final String keyFactory;
        private final int keyIterationCount;
        private final int keyLength;
        private final String keyAlgorithm;
        private final int keySaltLength;
        private final String cipher;
        private final int nonceLenth;

        Version(String keyFactory, int keyIterationCount, int keyLength, String keyAlgorithm, int keySaltLength, String cipher, int nonceLenth) {
            this.keyFactory = keyFactory;
            this.keyIterationCount = keyIterationCount;
            this.keyLength = keyLength;
            this.keyAlgorithm = keyAlgorithm;
            this.keySaltLength = keySaltLength;
            this.cipher = cipher;
            this.nonceLenth = nonceLenth;
        }
    }

}