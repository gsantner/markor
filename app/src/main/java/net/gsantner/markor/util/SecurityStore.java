package net.gsantner.markor.util;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;

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
public class SecurityStore {

    private static final String LOG_TAG_NAME = "SecurityStore";
    private static final String CIPHER_MODE = "AES/GCM/NoPadding";

    // DONT CHANGE THE VALUE!
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String KEY_SUFFIX_IV = ".iv";
    private static final String KEY_SUFFIX_KEY = ".key";

    // Exists PIN, Pattern or Fingerprint or something else.
    private final boolean deviceIsProtected;

    private final SharedPreferences preferences;
    private final Context context;


    /**
     * Initialize this object.
     *
     * @param context the context of the android app. Can be get at Cordova-Plugin via <br>
     *                <code>this.cordova.getActivity().getApplicationContext();</code> otherwise
     *                each Activity is a valid context.
     */
    @SuppressLint("HardwareIds")
    public SecurityStore(Context context) {
        this.context = context;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE); //api 23+
            deviceIsProtected = keyguardManager.isDeviceSecure();
        } else {
            deviceIsProtected = false;
        }
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public boolean storeKey(String unencryptedKey, String keyname, SecurityMode securityMode) {
        try {
            final SecurityMode usedSecurityMode;
            if (SecurityMode.AUTHENTICATION.equals(securityMode) && !deviceIsProtected) {
                final String warningText = "Downgrade security mode to none, " +
                        "because there is no screenprotection set!" +
                        "Secure lock screen isn't set up.\n" +
                        "Go to 'Settings -> Security -> Screen lock' to set up a lock screen";
                Toast.makeText(context, warningText,
                        Toast.LENGTH_LONG).show();
                Log.w(LOG_TAG_NAME, warningText);
                usedSecurityMode = SecurityMode.NONE;
            } else {
                usedSecurityMode = securityMode;
            }
            final Cipher cipher = Cipher.getInstance(CIPHER_MODE);
            final byte[] iv;
            final byte[] crypteKey;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cipher.init(Cipher.ENCRYPT_MODE, createSecretKey(keyname, usedSecurityMode));
                iv = cipher.getIV();

            } else {
                iv = new byte[128];
                new SecureRandom().nextBytes(iv);
                final String warningText = "You have an old Android-Device. The password will be saved unsafe.";
                Toast.makeText(context, warningText, Toast.LENGTH_LONG).show();
                Log.w(LOG_TAG_NAME, warningText);
            }
            crypteKey = cipher.doFinal(unencryptedKey.getBytes("UTF-8"));

            final SharedPreferences.Editor editor = preferences.edit();
            editor.putString(keyname + KEY_SUFFIX_KEY, Base64.encodeToString(crypteKey, Base64.DEFAULT));
            editor.putString(keyname + KEY_SUFFIX_IV, Base64.encodeToString(iv, Base64.DEFAULT));
            editor.apply();
            return true;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                BadPaddingException | IllegalBlockSizeException | NoSuchProviderException e) {
            Log.e(LOG_TAG_NAME, "Wrong encryption parameter", e);
        } catch (InvalidAlgorithmParameterException e) {
            if (SecurityMode.AUTHENTICATION.equals(securityMode) && !deviceIsProtected) {
                Log.e(LOG_TAG_NAME, "The device must be proteced by pin or pattern or fingerprint.", e);
            } else {
                Log.e(LOG_TAG_NAME, "Wrong encryption parameter", e);
            }
        } catch (UnsupportedEncodingException e) {
            Log.e(LOG_TAG_NAME, "Unkown encoding", e);
        }
        preferences.edit().clear().apply();
        return false;
    }

    public boolean storeKey(String unencryptedKey) {
        return storeKey(unencryptedKey, "SMD-Password", SecurityMode.NONE);
    }


    /**
     * Loads a saved key (under Keyword SMD-Password) or <code>null</code> if no key is found.
     *
     * @return the saved key or <code>null</code> if no key is found.
     */

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public String loadKey() {
        return loadKey("SMD-Password");
    }

    /**
     * Loads a saved key or <code>null</code> if no key is found.
     *
     * @param keyname name under which the key is saved.
     * @return the saved key or <code>null</code> if no key is found.
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public String loadKey(String keyname) {
        final SecretKey secretKey = getSecretKey(keyname);
        if (preferences.contains(keyname + KEY_SUFFIX_KEY) && secretKey != null) {
            final byte[] storedKey = Base64.decode(preferences.getString(keyname + KEY_SUFFIX_KEY, null), Base64.DEFAULT);
            final byte[] encryptionIv = Base64.decode(preferences.getString(keyname + KEY_SUFFIX_IV, null), Base64.DEFAULT);
            final Cipher cipher;
            try {
                cipher = Cipher.getInstance(CIPHER_MODE);
                final GCMParameterSpec spec = new GCMParameterSpec(128, encryptionIv);
                cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
                return new String(cipher.doFinal(storedKey), "UTF-8");
            } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | UnsupportedEncodingException e) {
                Log.e(LOG_TAG_NAME, "Wrong decryption parameter", e);
            }
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
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
