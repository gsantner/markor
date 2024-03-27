package other.de.stanetz.jpencconverter;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Objects;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Class which store the smd-password.<br>
 * How safe is the store? The store is as safe as the keystore of android.
 * <p>
 * Quote: The Android Keystore system lets you store cryptographic keys in a container to make it
 * more difficult to extract from the device.
 * Once keys are in the keystore, they can be used for cryptographic operations with the key material
 * remaining non-exportable.
 * Moreover, it offers facilities to restrict when and how keys can be used,
 * such as requiring user authentication for key use or restricting keys to be used only in certain
 * cryptographic modes.
 */
@SuppressWarnings("CharsetObjectCanBeUsed")
@RequiresApi(api = Build.VERSION_CODES.M)
public class PasswordStore {

    private static final String LOG_TAG_NAME = "SecurityStore";
    private static final String CIPHER_MODE = "AES/GCM/NoPadding";

    // DONT CHANGE THE VALUE!
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String KEY_SUFFIX_IV = ".iv";
    private static final String KEY_SUFFIX_KEY = ".key";

    // Exists PIN, Pattern or Fingerprint or something else.
    private final boolean _deviceIsProtected;

    private final SharedPreferences _preferences;
    private final Context _context;

    /**
     * Initialize this object.
     *
     * @param context the context of the android app. Can be get at Cordova-Plugin via <br>
     *                <code>this.cordova.getActivity().getApplicationContext();</code> otherwise
     *                each Activity is a valid context.
     */
    @SuppressLint("HardwareIds")
    public PasswordStore(Context context) {
        this._context = Objects.requireNonNull(context, "The context must be set.");
        final KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE); //api 23+
        _deviceIsProtected = keyguardManager.isDeviceSecure();
        this._preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    /**
     * Store the given password in a secure way. If the unencryptedKey is null or empty it removes the stored key.
     *
     * @param unencryptedKey the password unencrypted.
     * @param keyname        the name of the key under which it will be store in preferences.
     * @param securityMode   the grade of security.
     * @return <code>true</code> if the operation was successful.
     */
    public boolean storeKey(String unencryptedKey, String keyname, SecurityMode securityMode) {
        if (unencryptedKey == null || unencryptedKey.isEmpty()) {
            return clearAllKeys(keyname);
        }
        try {
            final SecurityMode usedSecurityMode;
            if (SecurityMode.AUTHENTICATION.equals(securityMode) && !_deviceIsProtected) {
                final String warningText = "Downgrade security mode to none, " +
                        "because there is no screenprotection set!" +
                        "Secure lock screen isn't set up.\n" +
                        "Go to 'Settings -> Security -> Screen lock' to set up a lock screen";
                Toast.makeText(_context, warningText,
                        Toast.LENGTH_LONG).show();
                Log.w(LOG_TAG_NAME, warningText);
                usedSecurityMode = SecurityMode.NONE;
            } else {
                usedSecurityMode = securityMode;
            }
            final Cipher cipher = Cipher.getInstance(CIPHER_MODE);
            cipher.init(Cipher.ENCRYPT_MODE, createSecretKey(keyname, usedSecurityMode));
            final byte[] iv = cipher.getIV();
            final byte[] crypteKey = cipher.doFinal(unencryptedKey.getBytes("UTF-8"));

            final SharedPreferences.Editor editor = _preferences.edit();
            editor.putString(keyname + KEY_SUFFIX_KEY, Base64.encodeToString(crypteKey, Base64.DEFAULT));
            editor.putString(keyname + KEY_SUFFIX_IV, Base64.encodeToString(iv, Base64.DEFAULT));
            editor.putString(keyname, "***");
            editor.apply();
            return true;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                 BadPaddingException | IllegalBlockSizeException | NoSuchProviderException e) {
            Log.e(LOG_TAG_NAME, "Wrong encryption parameter", e);
        } catch (InvalidAlgorithmParameterException e) {
            if (SecurityMode.AUTHENTICATION.equals(securityMode) && !_deviceIsProtected) {
                Log.e(LOG_TAG_NAME, "The device must be proteced by pin or pattern or fingerprint.", e);
            } else {
                Log.e(LOG_TAG_NAME, "Wrong encryption parameter", e);
            }
        } catch (UnsupportedEncodingException e) {
            Log.e(LOG_TAG_NAME, "Unkown encoding", e);
        }
        clearAllKeys(keyname);
        return false;
    }

    private boolean clearAllKeys(String keyname) {
        final SharedPreferences.Editor editor = _preferences.edit();
        editor.remove(keyname + KEY_SUFFIX_KEY);
        editor.remove(keyname + KEY_SUFFIX_IV);
        editor.remove(keyname);
        editor.apply();
        return true;
    }

    /**
     * Store the given password in a secure way. If the unencryptedKey is null or empty it removes the stored key.
     *
     * @param unencryptedKey the password unencrypted.
     * @param keyAsResId     the name of the key under which it will be store in preferences as resource-id.
     * @return <code>true</code> if the operation was successful.
     */
    public boolean storeKey(String unencryptedKey, @StringRes int keyAsResId) {
        return storeKey(unencryptedKey, _context.getString(keyAsResId), SecurityMode.NONE);
    }


    /**
     * Loads a saved key or <code>null</code> if no key is found.
     *
     * @param keyAsResId the Resource-Id for the key
     * @return the saved key or <code>null</code> if no key is found.
     */
    public char[] loadKey(@StringRes int keyAsResId) {
        return loadKey(_context.getString(keyAsResId));
    }

    /**
     * Loads a saved key or <code>null</code> if no key is found.
     *
     * @param keyname name under which the key is saved.
     * @return the saved key or <code>null</code> if no key is found.
     */
    @SuppressWarnings("WeakerAccess")
    public char[] loadKey(String keyname) {
        final SecretKey secretKey = getSecretKey(keyname);
        if (_preferences.contains(keyname + KEY_SUFFIX_KEY) && secretKey != null) {
            final byte[] storedKey = Base64.decode(_preferences.getString(keyname + KEY_SUFFIX_KEY, null), Base64.DEFAULT);
            final byte[] encryptionIv = Base64.decode(_preferences.getString(keyname + KEY_SUFFIX_IV, null), Base64.DEFAULT);
            final Cipher cipher;
            try {
                cipher = Cipher.getInstance(CIPHER_MODE);
                final GCMParameterSpec spec = new GCMParameterSpec(128, encryptionIv);
                cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
                final byte[] byteArray = cipher.doFinal(storedKey);
                Charset utf8Charset = Charset.forName("UTF-8");
                CharBuffer charBuffer = utf8Charset.decode(ByteBuffer.wrap(byteArray));
                return charBuffer.array();
            } catch (NoSuchAlgorithmException | NoSuchPaddingException |
                     InvalidAlgorithmParameterException | InvalidKeyException |
                     IllegalBlockSizeException | BadPaddingException e) {
                Log.e(LOG_TAG_NAME, "Wrong decryption parameter", e);
            }
        }
        return null;
    }

    @NonNull
    private SecretKey createSecretKey(String keyname, SecurityMode securityMode) throws NoSuchAlgorithmException,
            NoSuchProviderException, InvalidAlgorithmParameterException {

        final KeyGenerator keyGenerator = KeyGenerator
                .getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);

        keyGenerator.init(new KeyGenParameterSpec.Builder(keyname,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(SecurityMode.AUTHENTICATION.equals(securityMode))
                .setUserAuthenticationValidityDurationSeconds(300)
                .build());
        return keyGenerator.generateKey();
    }

    private SecretKey getSecretKey(String keyname) {
        try {
            final KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            final KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) keyStore.getEntry(keyname, null);
            return entry == null ? null : entry.getSecretKey();
        } catch (KeyStoreException e) {
            Log.e(LOG_TAG_NAME, "No keystore-provider is founded or can't load key from keystore.", e);
        } catch (CertificateException | IOException e) {
            Log.e(LOG_TAG_NAME, "Can't load keystore.", e);
        } catch (NoSuchAlgorithmException e) {
            Log.e(LOG_TAG_NAME, "Can't load keystore or can't load key from keystore.", e);
        } catch (UnrecoverableEntryException e) {
            Log.e(LOG_TAG_NAME, "Can't load key from keystore.", e);
        }
        return null;
    }


    public enum SecurityMode {
        NONE,
        /**
         * User authentication authorizes the use of keys for a duration of time.
         * All keys in this mode are authorized for use as soon as the user unlocks the secure lock
         * screen or confirms their secure lock screen credential using the
         * KeyguardManager.createConfirmDeviceCredentialIntent flow.
         * The duration for which the authorization remains valid is specific to each key,
         * as specified using setUserAuthenticationValidityDurationSeconds during key generation or import.
         * Such keys can only be generated or imported if the secure lock screen is enabled
         * (see KeyguardManager.isDeviceSecure()).
         * These keys become permanently invalidated once the secure lock screen is disabled
         * (reconfigured to None, Swipe or other mode which does not authenticate the user)
         * or forcibly reset (e.g. by a Device Administrator).
         * <p>
         * The time is set to 5 minutes.
         * <p>
         * See https://stackoverflow.com/questions/36043912/error-after-fingerprint-touched-on-samsung-phones-android-security-keystoreexce
         * seems not a good option, because it ends in difficult error-reports.
         * See https://github.com/googlesamples/android-ConfirmCredential/blob/master/Application/src/main/java/com/example/android/confirmcredential/MainActivity.java
         * for an example implementation how to do it right.
         */
        AUTHENTICATION
    }

}
