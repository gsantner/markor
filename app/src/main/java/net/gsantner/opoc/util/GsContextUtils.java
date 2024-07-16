/*#######################################################
 *
 * SPDX-FileCopyrightText: 2016-2024 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2016-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
#########################################################*/
package net.gsantner.opoc.util;

import static android.graphics.Bitmap.CompressFormat;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintJob;
import android.print.PrintManager;
import android.provider.CalendarContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.text.Html;
import android.text.InputFilter;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityManagerCompat;
import androidx.core.app.ShareCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.os.ConfigurationCompat;
import androidx.core.text.TextUtilsCompat;
import androidx.core.view.ViewCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import com.google.android.material.snackbar.Snackbar;

import net.gsantner.opoc.format.GsSimpleMarkdownParser;
import net.gsantner.opoc.format.GsTextUtils;
import net.gsantner.opoc.wrapper.GsCallback;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

@SuppressWarnings({"UnusedReturnValue", "rawtypes", "unused"})
public class GsContextUtils {
    //########################
    //## Constructor
    //########################
    public static final GsContextUtils instance = new GsContextUtils();

    public GsContextUtils() {
    }

    protected <T extends GsContextUtils> T thisp() {
        //noinspection unchecked
        return (T) this;
    }

    //########################
    //## Static fields & members
    //########################
    @SuppressLint("ConstantLocale")
    public final static Locale INITIAL_LOCALE = Locale.getDefault();
    public final static String EXTRA_FILEPATH = "EXTRA_FILEPATH";
    public final static String EXTRA_URI = "EXTRA_URI";
    public final static SimpleDateFormat DATEFORMAT_RFC3339ISH = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss", INITIAL_LOCALE);
    public final static String MIME_TEXT_PLAIN = "text/plain";
    public final static String PREF_KEY__SAF_TREE_URI = "pref_key__saf_tree_uri";
    public final static String CONTENT_RESOLVER_FILE_PROXY_SEGMENT = "CONTENT_RESOLVER_FILE_PROXY_SEGMENT";

    public final static int REQUEST_CAMERA_PICTURE = 50001;
    public final static int REQUEST_PICK_PICTURE = 50002;
    public final static int REQUEST_SAF = 50003;
    public final static int REQUEST_STORAGE_PERMISSION_M = 50004;
    public final static int REQUEST_STORAGE_PERMISSION_R = 50005;
    public final static int REQUEST_RECORD_AUDIO = 50006;
    private final static int BLINK_ANIMATOR_TAG = -1206813720;

    public static int TEXTFILE_OVERWRITE_MIN_TEXT_LENGTH = 2;
    protected static Pair<File, List<Pair<String, String>>> m_cacheLastExtractFileMetadata;
    protected static String _lastCameraPictureFilepath = null;
    protected static WeakReference<GsCallback.a1<String>> _receivePathCallback = null;
    protected static String m_chooserTitle = "➥";


    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //########################
    //## Resources
    //########################
    public enum ResType {
        ID, BOOL, INTEGER, COLOR, STRING, ARRAY, DRAWABLE, PLURALS,
        ANIM, ATTR, DIMEN, LAYOUT, MENU, RAW, STYLE, XML,
    }

    /**
     * Find out the numerical resource id by given {@link ResType}
     *
     * @return A valid id if the id could be found, else 0
     */
    @SuppressLint("DiscouragedApi")
    public int getResId(final Context context, final ResType resType, String name) {
        try {
            name = name.toLowerCase(Locale.ROOT).replace("#", "no").replaceAll("[^A-Za-z0-9_]", "_");
            return context.getResources().getIdentifier(name, resType.name().toLowerCase(Locale.ENGLISH), context.getPackageName());
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Get String by given string resource id (numeric)
     */
    public String rstr(Context context, @StringRes final int strResId) {
        try {
            return context.getString(strResId);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get String by given string resource identifier (textual)
     */
    public String rstr(final Context context, final String strResKey, Object... a0getResKeyAsFallback) {
        try {
            final String s = rstr(context, getResId(context, ResType.STRING, strResKey));
            if (s != null) {
                return s;
            }
        } catch (Exception ignored) {
        }
        return a0getResKeyAsFallback != null && a0getResKeyAsFallback.length > 0 ? strResKey : null;
    }

    /**
     * Get drawable from given resource identifier
     */
    public Drawable rdrawable(final Context context, @DrawableRes final int resId) {
        try {
            return ContextCompat.getDrawable(context, resId);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get color by given color resource id
     */
    @ColorInt
    public int rcolor(final Context context, @ColorRes final int resId) {
        if (context == null || resId == 0) {
            Log.e(getClass().getName(), "GsContextUtils::rcolor: resId is 0!");
            return Color.BLACK;
        }
        return ContextCompat.getColor(context, resId);
    }

    /**
     * Checks if all given (textual) resource ids are available
     *
     * @param resType       A {@link ResType}
     * @param resIdsTextual A (textual) identifier to be awaited at R.restype.resIdsTextual
     * @return True if all given ids are available
     */
    public boolean areResourcesAvailable(final Context context, final ResType resType, final String... resIdsTextual) {
        for (String name : resIdsTextual) {
            if (getResId(context, resType, name) == 0) {
                return false;
            }
        }
        return true;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //########################
    //## App & Device information
    //########################

    public static String getAndroidVersion() {
        return Build.VERSION.RELEASE + " (" + Build.VERSION.SDK_INT + ")";
    }

    public String getAppVersionName(final Context context) {
        final PackageManager manager = context.getPackageManager();
        try {
            PackageInfo info = manager.getPackageInfo(getAppIdFlavorSpecific(context), 0);
            return info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            try {
                PackageInfo info = manager.getPackageInfo(getAppIdUsedAtManifest(context), 0);
                return info.versionName;
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        return "?";
    }

    public String getAppInstallationSource(final Context context) {
        String src = null;
        try {
            src = context.getPackageManager().getInstallerPackageName(getAppIdFlavorSpecific(context));
        } catch (Exception ignored) {
            try {
                src = context.getPackageManager().getInstallerPackageName(getAppIdUsedAtManifest(context));
            } catch (Exception ignored2) {
            }
        }

        src = TextUtils.isEmpty(src) ? "" : src;
        src = src.replaceAll("^\\s*$", "Sideloaded")
                .replaceAll("(?i).*(vending)|(google).*", "Google Play")
                .replaceAll("(?i).*fdroid.*", "F-Droid")
                .replaceAll("(?i).*amazon.*", "Amazon Appstore")
                .replaceAll("(?i).*yalp.*", "Yalp Store").replaceAll("(?i).*aptoide.*", "Aptoide")
                .replaceAll("(?i).*package.*installer.*", "Package Installer");
        return src;
    }

    @SuppressLint("PrivateApi")
    public Application getApplicationObject() {
        try {
            return (Application) Class.forName("android.app.AppGlobals").getMethod("getInitialApplication").invoke(null, (Object[]) null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the apps base packagename, which is equal with all build flavors and variants
     */
    public String getAppIdUsedAtManifest(final Context context) {
        String pkg = rstr(context, "manifest_package_id");
        return !TextUtils.isEmpty(pkg) ? pkg : context.getPackageName();
    }

    /**
     * Get this apps package name, returns the flavor specific package name.
     */
    public String getAppIdFlavorSpecific(final Context context) {
        return context.getPackageName();
    }

    /**
     * Get field from ${applicationId}.BuildConfig
     * May be helpful in libraries, where a access to
     * BuildConfig would only get values of the library
     * rather than the app ones. It awaits a string resource
     * of the package set in manifest (root element).
     * Falls back to applicationId of the app which may differ from manifest.
     */
    public Object getBuildConfigValue(final Context context, final String fieldName) {
        final String pkg = getAppIdUsedAtManifest(context) + ".BuildConfig";
        try {
            Class<?> c = Class.forName(pkg);
            return c.getField(fieldName).get(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<String> getBuildConfigFields(final Context context) {
        final String pkg = getAppIdUsedAtManifest(context) + ".BuildConfig";
        final List<String> fields = new ArrayList<>();
        try {
            for (Field f : Class.forName(pkg).getFields()) {
                fields.add(f.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fields;
    }

    /**
     * Get a BuildConfig bool value
     */
    public Boolean bcbool(final Context context, final String fieldName, final Boolean defaultValue) {
        Object field = getBuildConfigValue(context, fieldName);
        if (field instanceof Boolean) {
            return (Boolean) field;
        }
        return defaultValue;
    }

    /**
     * Get a BuildConfig string value
     */
    public String bcstr(final Context context, final String fieldName, final String defaultValue) {
        Object field = getBuildConfigValue(context, fieldName);
        if (field instanceof String) {
            return (String) field;
        }
        return defaultValue;
    }

    /**
     * Get a BuildConfig string value
     */
    public Integer bcint(final Context context, final String fieldName, final int defaultValue) {
        Object field = getBuildConfigValue(context, fieldName);
        if (field instanceof Integer) {
            return (Integer) field;
        }
        return defaultValue;
    }

    /**
     * Check if this is a gplay build (requires BuildConfig field)
     */
    public boolean isGooglePlayBuild(final Context context) {
        return bcbool(context, "IS_GPLAY_BUILD", true);
    }

    /**
     * Check if this is a foss build (requires BuildConfig field)
     */
    public boolean isFossBuild(final Context context) {
        return bcbool(context, "IS_FOSS_BUILD", false);
    }

    public String readTextfileFromRawRes(final Context context, @RawRes int rawResId, String linePrefix, String linePostfix) {
        final StringBuilder sb = new StringBuilder();

        String line;
        linePrefix = linePrefix == null ? "" : linePrefix;
        linePostfix = linePostfix == null ? "" : linePostfix;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(rawResId)))) {
            while ((line = br.readLine()) != null) {
                sb.append(linePrefix);
                sb.append(line);
                sb.append(linePostfix);
                sb.append("\n");
            }
        } catch (Exception ignored) {
        }
        return sb.toString();
    }

    /**
     * Get internet connection state - the permission ACCESS_NETWORK_STATE is required
     *
     * @return True if internet connection available
     */
    @SuppressLint("MissingPermission")
    public boolean isConnectedToInternet(final Context context) {
        try {
            ConnectivityManager con = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetInfo = con == null ? null : con.getActiveNetworkInfo();
            return activeNetInfo != null && activeNetInfo.isConnectedOrConnecting();
        } catch (Exception ignored) {
            throw new RuntimeException("Error: Developer forgot to declare a permission");
        }
    }

    /**
     * Check if app with given {@code appId} is installed
     */
    public boolean isAppInstalled(final Context context, String appId) {
        try {
            final PackageManager pm = context.getApplicationContext().getPackageManager();
            pm.getPackageInfo(appId, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * Restart the current app. Supply the class to start on startup
     */
    public void restartApp(final Context context, Class classToStart) {
        final Intent intent = new Intent(context, classToStart);
        int flags = PendingIntent.FLAG_CANCEL_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        final PendingIntent pendi = PendingIntent.getActivity(context, 555, intent, flags);
        final AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (context instanceof Activity) {
            ((Activity) context).finish();
        }
        if (mgr != null) {
            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, pendi);
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
        Runtime.getRuntime().exit(0);
    }

    /**
     * Load a markdown file from a {@link RawRes}, prepend each line with {@code prepend} text
     * and convert markdown to html using {@link GsSimpleMarkdownParser}
     */
    public String loadMarkdownForTextViewFromRaw(final Context context, @RawRes int rawMdFile, String prepend) {
        try {
            return new GsSimpleMarkdownParser()
                    .parse(context.getResources().openRawResource(rawMdFile), prepend, GsSimpleMarkdownParser.FILTER_ANDROID_TEXTVIEW)
                    .replaceColor("#000001", rcolor(context, getResId(context, ResType.COLOR, "accent")))
                    .removeMultiNewlines().replaceBulletCharacter("*").getHtml();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Load html into a {@link Spanned} object and set the
     * {@link TextView}'s text using {@link TextView#setText(CharSequence)}
     */
    public void setHtmlToTextView(final TextView textView, final String html) {
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setText(new SpannableString(htmlToSpanned(html)));
    }

    /**
     * Estimate this device's screen diagonal size in inches
     */
    public double getEstimatedScreenSizeInches(final Context context) {
        final DisplayMetrics dm = context.getResources().getDisplayMetrics();

        double calc = dm.density * 160d;
        double x = Math.pow(dm.widthPixels / calc, 2);
        double y = Math.pow(dm.heightPixels / calc, 2);
        calc = Math.sqrt(x + y) * 1.16;  // 1.16 = est. Nav/Statusbar
        return Math.min(12, Math.max(4, calc));
    }

    /**
     * Check if the device is currently in portrait orientation
     */
    public boolean isInPortraitMode(final Context context) {
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    /**
     * Get an {@link Locale} out of a android language code
     * The {@code androidLC} may be in any of the forms: de, en, de-rAt
     */
    public Locale getLocaleByAndroidCode(String androidLC) {
        if (!TextUtils.isEmpty(androidLC)) {
            return androidLC.contains("-r")
                    ? new Locale(androidLC.substring(0, 2), androidLC.substring(4, 6)) // de-rAt
                    : new Locale(androidLC); // de
        }
        return Resources.getSystem().getConfiguration().locale;
    }

    /**
     * Set the apps language
     * {@code androidLC} may be in any of the forms: en, de, de-rAt
     * If given an empty string, the default (system) locale gets loaded
     */
    public <T extends GsContextUtils> T setAppLanguage(final Context context, final String androidLC) {
        Locale locale = getLocaleByAndroidCode(androidLC);
        locale = (locale != null && !androidLC.isEmpty()) ? locale : Resources.getSystem().getConfiguration().locale;
        setAppLocale(context, locale);
        return thisp();
    }

    public <T extends GsContextUtils> T setAppLocale(final Context context, final Locale locale) {
        Configuration config = context.getResources().getConfiguration();
        config.locale = (locale != null ? locale : Resources.getSystem().getConfiguration().locale);
        context.getResources().updateConfiguration(config, null);
        //noinspection ConstantConditions
        Locale.setDefault(locale);
        return thisp();
    }

    /**
     * Send a {@link Intent#ACTION_VIEW} Intent with given parameter
     * If the parameter is an string a browser will get triggered
     */
    public <T extends GsContextUtils> T openWebpageInExternalBrowser(final Context context, final String url) {
        try {
            startActivity(context, new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return thisp();
    }

    /**
     * Try to guess if the color on top of the given {@code colorOnBottomInt}
     * should be light or dark. Returns true if top color should be light
     */
    public boolean shouldColorOnTopBeLight(@ColorInt final int colorOnBottomInt) {
        return 186 > (((0.299 * Color.red(colorOnBottomInt))
                + ((0.587 * Color.green(colorOnBottomInt))
                + (0.114 * Color.blue(colorOnBottomInt)))));
    }

    /**
     * Convert a html string to an android {@link Spanned} object
     */
    public Spanned htmlToSpanned(final String html) {
        Spanned result;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            result = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
        } else {
            result = Html.fromHtml(html);
        }
        return result;
    }

    /**
     * Convert pixel unit do android dp unit
     */
    public float convertPxToDp(final Context context, final float px) {
        return px / context.getResources().getDisplayMetrics().density;
    }

    /**
     * Convert android dp unit to pixel unit
     */
    public int convertDpToPx(final Context context, final float dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    /**
     * Get the private directory for the current package (usually /data/data/package.name/)
     */
    @SuppressWarnings("StatementWithEmptyBody")
    public File getAppDataPrivateDir(final Context context) {
        File filesDir;
        try {
            filesDir = new File(new File(context.getPackageManager().getPackageInfo(getAppIdFlavorSpecific(context), 0).applicationInfo.dataDir), "files");
        } catch (PackageManager.NameNotFoundException e) {
            filesDir = context.getFilesDir();
        }
        if (!filesDir.exists() && filesDir.mkdirs()) ;
        return filesDir;
    }

    /**
     * Get public (accessible) appdata folders
     */
    @SuppressWarnings("StatementWithEmptyBody")
    public List<Pair<File, String>> getAppDataPublicDirs(final Context context, boolean internalStorageFolder, boolean sdcardFolders, boolean storageNameWithoutType) {
        List<Pair<File, String>> dirs = new ArrayList<>();
        for (File externalFileDir : ContextCompat.getExternalFilesDirs(context, null)) {
            if (externalFileDir == null || Environment.getExternalStorageDirectory() == null) {
                continue;
            }
            boolean isInt = externalFileDir.getAbsolutePath().startsWith(Environment.getExternalStorageDirectory().getAbsolutePath());
            boolean add = (internalStorageFolder && isInt) || (sdcardFolders && !isInt);
            if (add) {
                dirs.add(new Pair<>(externalFileDir, getStorageName(externalFileDir, storageNameWithoutType)));
                if (!externalFileDir.exists() && externalFileDir.mkdirs()) ;
            }
        }
        return dirs;
    }

    public String getStorageName(final File externalFileDir, final boolean storageNameWithoutType) {
        boolean isInt = externalFileDir.getAbsolutePath().startsWith(Environment.getExternalStorageDirectory().getAbsolutePath());

        String[] split = externalFileDir.getAbsolutePath().split("/");
        if (split.length > 2) {
            return isInt ? (storageNameWithoutType ? "Internal Storage" : "") : (storageNameWithoutType ? split[2] : ("SD Card (" + split[2] + ")"));
        } else {
            return "Storage";
        }
    }

    public List<Pair<File, String>> getStorages(final Context context, final boolean internalStorageFolder, final boolean sdcardFolders) {
        List<Pair<File, String>> storages = new ArrayList<>();
        for (Pair<File, String> pair : getAppDataPublicDirs(context, internalStorageFolder, sdcardFolders, true)) {
            if (pair.first != null && pair.first.getAbsolutePath().lastIndexOf("/Android/data") > 0) {
                try {
                    storages.add(new Pair<>(new File(pair.first.getCanonicalPath().replaceFirst("/Android/data.*", "")), pair.second));
                } catch (IOException ignored) {
                }
            }
        }
        return storages;
    }

    public File getStorageRootFolder(final Context context, final File file) {
        String filepath;
        try {
            filepath = file.getCanonicalPath();
        } catch (Exception ignored) {
            return null;
        }
        for (Pair<File, String> storage : getStorages(context, false, true)) {
            if (filepath.startsWith(storage.first.getAbsolutePath())) {
                return storage.first;
            }
        }
        return null;
    }

    /**
     * Request the givens paths to be scanned by MediaScanner
     *
     * @param files Files and folders to scan
     */
    public void mediaScannerScanFile(final Context context, final File... files) {
        if (android.os.Build.VERSION.SDK_INT > 19) {
            String[] paths = new String[files.length];
            for (int i = 0; i < files.length; i++) {
                paths[i] = files[i].getAbsolutePath();
            }
            MediaScannerConnection.scanFile(context, paths, null, null);
        } else {
            for (File file : files) {
                context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
            }
        }
    }

    /**
     * Load an image into a {@link ImageView} and apply a color filter
     */
    public static void setDrawableWithColorToImageView(ImageView imageView, @DrawableRes int drawableResId, @ColorRes int colorResId) {
        imageView.setImageResource(drawableResId);
        imageView.setColorFilter(ContextCompat.getColor(imageView.getContext(), colorResId));
    }

    /**
     * Get a {@link Bitmap} out of a {@link Drawable}
     */
    public Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = null;
        if (drawable instanceof VectorDrawableCompat
                || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && drawable instanceof VectorDrawable)
                || ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable instanceof AdaptiveIconDrawable))) {

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                drawable = (DrawableCompat.wrap(drawable)).mutate();
            }

            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        } else if (drawable instanceof BitmapDrawable) {
            bitmap = ((BitmapDrawable) drawable).getBitmap();
        }
        return bitmap;
    }

    /**
     * Get a {@link Bitmap} out of a {@link DrawableRes}
     */
    public Bitmap drawableToBitmap(final Context context, @DrawableRes final int drawableId) {
        try {
            return drawableToBitmap(ContextCompat.getDrawable(context, drawableId));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get a {@link Bitmap} from a given {@code imagePath} on the filesystem
     * Specifying a {@code maxDimen} is also possible and a value below 2000
     * is recommended, otherwise a {@link OutOfMemoryError} may occur
     */
    public Bitmap loadImageFromFilesystem(final File imagePath, final int maxDimen) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath.getAbsolutePath(), options);
        options.inSampleSize = calculateInSampleSize(options, maxDimen);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(imagePath.getAbsolutePath(), options);
    }

    /**
     * Calculates the scaling factor so the bitmap is maximal as big as the maxDimen
     *
     * @param options  Bitmap-options that contain the current dimensions of the bitmap
     * @param maxDimen Max size of the Bitmap (width or height)
     * @return the scaling factor that needs to be applied to the bitmap
     */
    public int calculateInSampleSize(final BitmapFactory.Options options, final int maxDimen) {
        // Raw height and width of image
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (Math.max(height, width) > maxDimen) {
            inSampleSize = Math.round(1f * Math.max(height, width) / maxDimen);
        }
        return inSampleSize;
    }

    /**
     * Scale the bitmap so both dimensions are lower or equal to {@code maxDimen}
     * This keeps the aspect ratio
     */
    public Bitmap scaleBitmap(final Bitmap bitmap, final int maxDimen) {
        int picSize = Math.min(bitmap.getHeight(), bitmap.getWidth());
        float scale = 1.f * maxDimen / picSize;
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    /**
     * Write the given {@link Bitmap} to filesystem
     *
     * @param targetFile The file to be written in
     * @param image      Android {@link Bitmap}
     * @return True if writing was successful
     */
    public <T extends GsContextUtils> T writeImageToFile(final File targetFile, final Bitmap image, GsCallback.a1<Boolean> okCallback, Integer... a0quality) {
        final int quality = (a0quality != null && a0quality.length > 0 && a0quality[0] >= 0 && a0quality[0] <= 100) ? a0quality[0] : 70;
        final String lc = targetFile.getAbsolutePath().toLowerCase(Locale.ROOT);
        final CompressFormat format = lc.endsWith(".webp") ? CompressFormat.WEBP : (lc.endsWith(".png") ? CompressFormat.PNG : CompressFormat.JPEG);

        writeFile(null, targetFile, false, (isOk, outputStream) -> {
            isOk &= image.compress(format, quality, outputStream);
            if (okCallback != null) {
                okCallback.callback(isOk);
            }
            try {
                image.recycle();
            } catch (Exception ignored) {
            }
        });
        return thisp();
    }

    /**
     * Draw text in the center of the given {@link DrawableRes}
     * This may be useful for e.g. badge counts
     */
    public Bitmap drawTextOnDrawable(final Context context, @DrawableRes final int drawableRes, final String text, final int textSize) {
        Resources resources = context.getResources();
        float scale = resources.getDisplayMetrics().density;
        Bitmap bitmap = drawableToBitmap(context, drawableRes);

        bitmap = bitmap.copy(bitmap.getConfig(), true);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.rgb(61, 61, 61));
        paint.setTextSize((int) (textSize * scale));
        paint.setShadowLayer(1f, 0f, 1f, Color.WHITE);

        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        int x = (bitmap.getWidth() - bounds.width()) / 2;
        int y = (bitmap.getHeight() + bounds.height()) / 2;
        canvas.drawText(text, x, y, paint);

        return bitmap;
    }

    /**
     * Try to tint all {@link Menu}s {@link MenuItem}s with given color
     */
    public void tintMenuItems(final Menu menu, final boolean recurse, @ColorInt final int iconColor) {
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            try {
                tintDrawable(item.getIcon(), iconColor);
                if (item.hasSubMenu() && recurse) {
                    //noinspection ConstantConditions
                    tintMenuItems(item.getSubMenu(), recurse, iconColor);
                }
            } catch (Exception ignored) {
                // This should not happen at all, but may in bad menu.xml configuration
            }
        }
    }

    /**
     * Loads {@link Drawable} by given {@link DrawableRes} and applies a color
     */
    public Drawable tintDrawable(final Context context, @DrawableRes final int drawableRes, @ColorInt final int color) {
        return tintDrawable(rdrawable(context, drawableRes), color);
    }

    /**
     * Tint a {@link Drawable} with given {@code color}
     */
    public Drawable tintDrawable(@Nullable Drawable drawable, @ColorInt final int color) {
        if (drawable != null) {
            drawable = DrawableCompat.wrap(drawable);
            DrawableCompat.setTint(drawable.mutate(), color);
        }
        return drawable;
    }

    /**
     * Try to make icons in Toolbar/ActionBars SubMenus visible
     * This may not work on some devices and it maybe won't work on future android updates
     */
    public void setSubMenuIconsVisibility(final Menu menu, final boolean visible) {
        if (TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault()) == ViewCompat.LAYOUT_DIRECTION_RTL) {
            return;
        }
        if (menu.getClass().getSimpleName().equals("MenuBuilder")) {
            try {
                @SuppressLint("PrivateApi") Method m = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);
                m.setAccessible(true);
                m.invoke(menu, visible);
            } catch (Exception ignored) {
                Log.d(getClass().getName(), "Error: 'setSubMenuIconsVisibility' not supported on this device");
            }
        }
    }


    public String getLocalizedDateFormat(final Context context) {
        return ((SimpleDateFormat) android.text.format.DateFormat.getDateFormat(context)).toPattern();
    }

    public String getLocalizedTimeFormat(final Context context) {
        return ((SimpleDateFormat) android.text.format.DateFormat.getTimeFormat(context)).toPattern();
    }

    public String getLocalizedDateTimeFormat(final Context context) {
        return getLocalizedDateFormat(context) + " " + getLocalizedTimeFormat(context);
    }

    /**
     * A {@link InputFilter} for filenames
     */
    @SuppressWarnings({"UnnecessaryLocalVariable", "RedundantSuppression"})
    public InputFilter makeFilenameInputFilter() {
        return (filterSrc, filterStart, filterEnd, filterDest, filterDstart, filterDend) -> {
            if (filterSrc != null && filterSrc.length() > 0) {
                final String newInput = filterSrc.subSequence(filterStart, filterEnd).toString().replace(" ", "");
                final String newInputFiltered = GsFileUtils.getFilteredFilenameWithoutDisallowedChars(newInput);
                if (!newInput.equals(newInputFiltered)) {
                    return "";
                }
            }
            return null;
        };
    }

    /**
     * A simple {@link Runnable} which does a touch event on a view.
     * This pops up e.g. the keyboard on a {@link android.widget.EditText}
     * <p>
     * Example: new Handler().postDelayed(new DoTouchView(editView), 200);
     */
    public static class DoTouchView implements Runnable {
        private View m_view;

        public DoTouchView(View view) {
            m_view = view;
        }

        @Override
        public void run() {
            m_view.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0, 0, 0));
            m_view.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 0, 0, 0));
            m_view = null;
        }
    }


    public String getMimeType(final Context context, final File file) {
        return getMimeType(context, file.getAbsolutePath());
    }

    /**
     * Detect MimeType of given file
     */
    public String getMimeType(final Context context, String uri) {
        String mimeType;
        uri = uri.replaceFirst("\\.jenc$", "");
        if (uri.startsWith(ContentResolver.SCHEME_CONTENT + "://")) {
            ContentResolver cr = context.getContentResolver();
            mimeType = cr.getType(Uri.parse(uri));
        } else {
            String ext = MimeTypeMap.getFileExtensionFromUrl(uri);
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase());
        }

        // Next-best try if other methods fail
        if (GsTextUtils.isNullOrEmpty(mimeType) && new File(uri).exists()) {
            mimeType = GsFileUtils.getMimeType(new File(uri));
        }

        if (GsTextUtils.isNullOrEmpty((mimeType))) {
            mimeType = "*/*";
        }
        return mimeType.toLowerCase(Locale.ROOT);
    }

    /**
     * Parse color hex string, using RGBA (instead of {@link Color#parseColor(String)} which uses ARGB)
     *
     * @param hexcolorString Hex color string in RRGGBB or RRGGBBAA format
     * @return {@link ColorInt}
     */
    public @ColorInt
    Integer parseHexColorString(final String hexcolorString) {
        String h = TextUtils.isEmpty(hexcolorString) ? "" : hexcolorString;
        h = h.replaceAll("[^A-Fa-f0-9]", "").trim();
        if (h.isEmpty() || h.length() > 8) {
            return null;
        }
        try {
            if (h.length() > 6) {
                h = h.substring(6) + (h.length() == 8 ? "" : "0") + h.substring(0, 6);
            }
            return Color.parseColor("#" + h);
        } catch (Exception ignored) {
            return null;
        }
    }

    public boolean isDeviceGoodHardware(final Context context) {
        try {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            return !ActivityManagerCompat.isLowRamDevice(activityManager) &&
                    Runtime.getRuntime().availableProcessors() >= 4 &&
                    activityManager.getMemoryClass() >= 128;
        } catch (Exception ignored) {
            return true;
        }
    }

    // Vibrate device one time by given amount of time, defaulting to 50ms
    // Requires <uses-permission android:name="android.permission.VIBRATE" /> in AndroidManifest to work
    @SuppressWarnings("UnnecessaryReturnStatement")
    @SuppressLint("MissingPermission")
    public void vibrate(final Context context, final int... ms) {
        int ms_v = ms != null && ms.length > 0 ? ms[0] : 50;
        Vibrator vibrator = ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE));
        if (vibrator == null) {
            return;
        } else if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(ms_v, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(ms_v);
        }
    }

    /*
    Check if Wifi is connected. Requires these permissions in AndroidManifest:
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
     */
    @SuppressLint("MissingPermission")
    public boolean isWifiConnected(final Context context, boolean... enabledOnly) {
        final boolean doEnabledCheckOnly = enabledOnly != null && enabledOnly.length > 0 && enabledOnly[0];
        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo wifiInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return wifiInfo != null && (doEnabledCheckOnly ? wifiInfo.isAvailable() : wifiInfo.isConnected());
    }

    // Returns if the device is currently in portrait orientation (landscape=false)
    @SuppressWarnings("deprecation")
    public boolean isDeviceOrientationPortrait(final Context context) {
        final int rotation = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getOrientation();
        return (rotation == Surface.ROTATION_0) || (rotation == Surface.ROTATION_180);
    }

    // Get all of providers of the current app
    public List<ProviderInfo> getProvidersInfos(final Context context) {
        final List<ProviderInfo> providers = new ArrayList<>();
        for (final ProviderInfo info : context.getPackageManager().queryContentProviders(null, 0, 0)) {
            if (info.applicationInfo.uid == context.getApplicationInfo().uid) {
                providers.add(info);
            }
        }
        return providers;
    }

    public String getFileProvider(final Context context) {
        for (final ProviderInfo info : getProvidersInfos(context)) {
            if (info.name.matches("(?i).*fileprovider.*")) {
                return info.authority;
            }
        }
        throw new RuntimeException("Error at GsContextUtils::getFileProviderAuthority(context): No FileProvider authority setup");
    }

    /**
     * Animate to specified Activity
     *
     * @param to                 The class of the activity
     * @param finishFromActivity true: Finish the current activity
     * @param requestCode        Request code for stating the activity, not waiting for result if null
     */
    public <T extends GsContextUtils> T animateToActivity(final Activity context, final Class to, final Boolean finishFromActivity, final Integer requestCode) {
        return animateToActivity(context, new Intent(context, to), finishFromActivity, requestCode);
    }

    /**
     * Animate to Activity specified in intent
     * Requires animation resources
     *
     * @param intent             Intent to open start an activity
     * @param finishFromActivity true: Finish the current activity
     * @param requestCode        Request code for stating the activity, not waiting for result if null
     */
    public <T extends GsContextUtils> T animateToActivity(final Activity context, final Intent intent, final Boolean finishFromActivity, final Integer requestCode) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        if (requestCode != null) {
            context.startActivityForResult(intent, requestCode);
        } else {
            context.startActivity(intent);
        }
        context.overridePendingTransition(getResId(context, ResType.DIMEN, "fadein"), getResId(context, ResType.DIMEN, "fadeout"));
        if (finishFromActivity != null && finishFromActivity) {
            context.finish();
        }
        return thisp();
    }


    public <T extends GsContextUtils> T setChooserTitle(final String title) {
        m_chooserTitle = title;
        return thisp();
    }

    /**
     * Allow to choose a handling app for given intent
     *
     * @param intent      Thing to be shared
     * @param chooserText The title text for the chooser, or null for default
     */
    public void showChooser(final Context context, final Intent intent, final String chooserText) {
        try {
            startActivity(context, Intent.createChooser(intent, chooserText != null ? chooserText : m_chooserTitle));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public <T extends GsContextUtils> T setLauncherActivityEnabled(final Context context, Class activityClass, boolean enable) {
        try {
            ComponentName component = new ComponentName(context, activityClass);
            context.getPackageManager().setComponentEnabledSetting(component, enable ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        } catch (Exception ignored) {
        }
        return thisp();
    }

    public <T extends GsContextUtils> T setLauncherActivityEnabledFromString(final Context context, String activityClass, boolean enable) {
        try {
            ComponentName component = new ComponentName(context, activityClass);
            context.getPackageManager().setComponentEnabledSetting(component, enable ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        } catch (Exception ignored) {
        }
        return thisp();
    }

    /**
     * Try to create a new desktop shortcut on the launcher. Add permissions:
     * <uses-permission android:name="android.permission.INSTALL_SHORTCUT" />
     * <uses-permission android:name="com.android.launcher.permission.INSTALL_SHORTCUT" />
     *
     * @param intent  The intent to be invoked on tap
     * @param iconRes Icon resource for the item
     * @param title   Title of the item
     */
    public void createLauncherDesktopShortcut(final Context context, final Intent intent, @DrawableRes final int iconRes, final String title) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (intent.getAction() == null) {
            intent.setAction(Intent.ACTION_VIEW);
        }

        ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(context, Long.toString(new Random().nextLong()))
                .setIntent(intent)
                .setIcon(IconCompat.createWithResource(context, iconRes))
                .setShortLabel(title)
                .setLongLabel(title)
                .build();
        ShortcutManagerCompat.requestPinShortcut(context, shortcut, null);
    }

    /**
     * Try to create a new desktop shortcut on the launcher. This will not work on Api > 25. Add permissions:
     * <uses-permission android:name="android.permission.INSTALL_SHORTCUT" />
     * <uses-permission android:name="com.android.launcher.permission.INSTALL_SHORTCUT" />
     *
     * @param intent  The intent to be invoked on tap
     * @param iconRes Icon resource for the item
     * @param title   Title of the item
     */
    public void createLauncherDesktopShortcutLegacy(final Context context, final Intent intent, @DrawableRes final int iconRes, final String title) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (intent.getAction() == null) {
            intent.setAction(Intent.ACTION_VIEW);
        }

        Intent creationIntent = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
        creationIntent.putExtra("duplicate", true);
        creationIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);
        creationIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, title);
        creationIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(context, iconRes));
        context.sendBroadcast(creationIntent);
    }

    /**
     * Share text with given mime-type
     *
     * @param text     The text to share
     * @param mimeType MimeType or null (uses text/plain)
     */
    public void shareText(final Context context, final String text, @Nullable final String mimeType) {
        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        intent.setType(mimeType != null ? mimeType : MIME_TEXT_PLAIN);
        showChooser(context, intent, null);
    }

    /**
     * Share the given file as stream with given mime-type
     *
     * @param file     The file to share
     * @param mimeType The files mime type
     */
    public boolean shareStream(final Context context, final File file, final String mimeType) {
        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(EXTRA_FILEPATH, file.getAbsolutePath());
        intent.setType(mimeType);

        try {
            final Uri fileUri = FileProvider.getUriForFile(context, getFileProvider(context), file);
            intent.putExtra(Intent.EXTRA_STREAM, fileUri);
            showChooser(context, intent, null);
            return true;
        } catch (Exception ignored) { // FileUriExposed(API24) / IllegalArgument
            return false;
        }
    }

    /**
     * Share the given files as stream with given mime-type
     *
     * @param files    The files to share
     * @param mimeType The files mime type. Usally * / * is the best option
     */
    public boolean shareStreamMultiple(final Context context, final Collection<File> files, final String mimeType) {
        ArrayList<Uri> uris = new ArrayList<>();
        for (File file : files) {
            File uri = new File(file.toString());
            uris.add(FileProvider.getUriForFile(context, getFileProvider(context), file));
        }

        try {
            final Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            intent.setType(mimeType);
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // = works without Activity context
            showChooser(context, intent, null);
            return true;
        } catch (Exception e) { // FileUriExposed(API24) / IllegalArgument
            return false;
        }
    }

    /**
     * Start calendar application to add new event, with given details prefilled
     */
    public boolean createCalendarAppointment(final Context context, @Nullable final String title, @Nullable final String description, @Nullable final String location, @Nullable final Long... startAndEndTime) {
        final Intent intent = new Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI);
        if (title != null) {
            intent.putExtra(CalendarContract.Events.TITLE, title);
        }
        if (description != null) {
            intent.putExtra(CalendarContract.Events.DESCRIPTION, (description.length() > 800 ? description.substring(0, 800) : description));
        }
        if (location != null) {
            intent.putExtra(CalendarContract.Events.EVENT_LOCATION, location);
        }
        if (startAndEndTime != null) {
            if (startAndEndTime.length > 0 && startAndEndTime[0] > 0) {
                intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startAndEndTime[0]);
            }
            if (startAndEndTime.length > 1 && startAndEndTime[1] > 0) {
                intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, startAndEndTime[1]);
            }
        }

        try {
            startActivity(context, intent);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        }
    }

    /**
     * Start activity specified by Intent. Add FLAG_ACTIVITY_NEW_TASK in case passed context is not a {@link Activity}
     * (when a non-Activity {@link Context} is passed a Exception is thrown otherwise)
     *
     * @param context Context, preferably a Activity
     * @param intent  Intent
     */
    public void startActivity(final Context context, final Intent intent) {
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        context.startActivity(intent);
    }

    /**
     * Open a View intent for given file
     *
     * @param file The file to share
     */
    public boolean viewFileInOtherApp(final Context context, final File file, @Nullable final String type) {
        // On some specific devices the first won't work
        Uri fileUri = null;
        try {
            fileUri = FileProvider.getUriForFile(context, getFileProvider(context), file);
        } catch (Exception ignored) {
            try {
                fileUri = Uri.fromFile(file);
            } catch (Exception ignored2) {
            }
        }

        if (fileUri != null) {
            final Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, (TextUtils.isEmpty(type) ? getMimeType(context, file) : type));
            intent.putExtra(Intent.EXTRA_STREAM, fileUri);
            intent.setClipData(ClipData.newRawUri(file.getName(), fileUri));
            intent.putExtra(EXTRA_FILEPATH, file.getAbsolutePath());
            intent.putExtra(Intent.EXTRA_TITLE, file.getName());
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivity(context, intent);
            return true;
        }
        return false;
    }

    /**
     * Request installation of APK specified by file
     * Permission required:
     * <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
     *
     * @param file The apk file to install
     */
    public boolean requestApkInstallation(final Context context, final File file) {
        if (file == null || !file.getName().toLowerCase().endsWith(".apk")) {
            return false;
        }

        Uri fileUri = null;
        try {
            fileUri = FileProvider.getUriForFile(context, getFileProvider(context), file);
        } catch (Exception ignored) {
            try {
                fileUri = Uri.fromFile(file);
            } catch (Exception ignored2) {
            }
        }

        if (fileUri != null) {
            final String MIME_TYPE_APK = "application/vnd.android.package-archive";

            boolean hasRequestInstallPackagesPermission = true;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.getPackageManager().canRequestPackageInstalls();
                }
            } catch (Exception ignored) {
                hasRequestInstallPackagesPermission = false;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !hasRequestInstallPackagesPermission) {
                shareStream(context, file, MIME_TYPE_APK);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                startActivity(context, new Intent(Intent.ACTION_INSTALL_PACKAGE).setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).setDataAndType(fileUri, MIME_TYPE_APK));
            } else {
                startActivity(context, new Intent(Intent.ACTION_VIEW).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK).setDataAndType(fileUri, MIME_TYPE_APK));
            }
            return true;
        }
        return false;
    }

    /**
     * Share the given bitmap with given format
     *
     * @param bitmap  Image
     * @param quality Quality of the exported image [0-100]
     * @return if success, true
     */
    public <T extends GsContextUtils> T shareImage(final Context context, final Bitmap bitmap, final GsCallback.a1<Boolean> okCallback, final Integer... quality) {
        try {
            File file = new File(context.getCacheDir(), GsFileUtils.getFilenameWithTimestamp());
            if (bitmap != null) {
                writeImageToFile(file, bitmap, (ok) -> {
                    if (ok) {
                        shareStream(context, file, getMimeType(context, file));
                    }
                    if (okCallback != null) {
                        okCallback.callback(ok);
                    }
                }, quality);
                return thisp();
            }
        } catch (Exception ignored) {
        }
        if (okCallback != null) {
            okCallback.callback(false);
        }
        return thisp();
    }

    /**
     * Print a {@link WebView}'s contents, also allows to create a PDF
     *
     * @param webview WebView
     * @param jobName Name of the job (affects PDF name too)
     * @return {{@link PrintJob}} or null
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public PrintJob print(final WebView webview, final String jobName, final boolean... landscape) {
        final Context context = webview.getContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            final PrintDocumentAdapter printAdapter;
            final PrintManager printManager = (PrintManager) context.getSystemService(Context.PRINT_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                printAdapter = webview.createPrintDocumentAdapter(jobName);
            } else {
                printAdapter = webview.createPrintDocumentAdapter();
            }
            final PrintAttributes.Builder attrib = new PrintAttributes.Builder();
            if (landscape != null && landscape.length > 0 && landscape[0]) {
                attrib.setMediaSize(new PrintAttributes.MediaSize("ISO_A4", "android", 11690, 8270));
                attrib.setMinMargins(new PrintAttributes.Margins(0, 0, 0, 0));
            }
            if (printManager != null) {
                try {
                    return printManager.print(jobName, printAdapter, attrib.build());
                } catch (Exception ignored) {
                }
            }
        } else {
            Log.e(getClass().getName(), "ERROR: Method called on too low Android API version");
        }
        return null;
    }


    /**
     * See {@link #print(WebView, String, boolean...)}  print method}
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public PrintJob createPdf(final WebView webview, final String jobName) {
        return print(webview, jobName);
    }


    /**
     * Create a picture out of {@link WebView}'s whole content
     *
     * @param webView The WebView to get contents from
     * @return A {@link Bitmap} or null
     */
    @Nullable
    public Bitmap getBitmapFromWebView(final WebView webView, final boolean... a0fullpage) {
        try {
            //Measure WebView's content
            if (a0fullpage != null && a0fullpage.length > 0 && a0fullpage[0]) {
                int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                webView.measure(widthMeasureSpec, heightMeasureSpec);
                webView.layout(0, 0, webView.getMeasuredWidth(), webView.getMeasuredHeight());
            }

            //Build drawing cache and store its size
            webView.buildDrawingCache();

            //Creates the bitmap and draw WebView's content on in
            Bitmap bitmap = Bitmap.createBitmap(webView.getMeasuredWidth(), webView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawBitmap(bitmap, 0, bitmap.getHeight(), new Paint());

            webView.draw(canvas);
            webView.destroyDrawingCache();

            return bitmap;
        } catch (Exception | OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }
    }


    /***
     * Replace (primary) clipboard contents with given {@code text}
     * @param text Text to be set
     */
    public boolean setClipboard(final Context context, final CharSequence text) {
        try {
            final ClipboardManager cm = ((ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE));
            ClipData clip = ClipData.newPlainText(context.getPackageName(), text);
            cm.setPrimaryClip(clip);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Get clipboard contents, very failsafe and compat to older android versions
     */
    public List<String> getClipboard(final Context context) {
        List<String> clipper = new ArrayList<>();
        ClipboardManager cm = ((ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE));
        if (cm != null && cm.hasPrimaryClip()) {
            ClipData data = cm.getPrimaryClip();
            for (int i = 0; data != null && i < data.getItemCount() && i < data.getItemCount(); i++) {
                ClipData.Item item = data.getItemAt(i);
                if (item != null && !TextUtils.isEmpty(item.getText())) {
                    clipper.add(data.getItemAt(i).getText().toString());
                }
            }
        }
        return clipper;
    }

    /**
     * Draft an email with given data. Unknown data can be supplied as null.
     * This will open a chooser with installed mail clients where the mail can be sent from
     *
     * @param subject Subject (top/title) text to be prefilled in the mail
     * @param body    Body (content) text to be prefilled in the mail
     * @param to      recipients to be prefilled in the mail
     */
    public void draftEmail(final Context context, final String subject, final String body, final String... to) {
        final Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        if (subject != null) {
            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        }
        if (body != null) {
            intent.putExtra(Intent.EXTRA_TEXT, body);
        }
        if (to != null && to.length > 0 && to[0] != null) {
            intent.putExtra(Intent.EXTRA_EMAIL, to);
        }
        showChooser(context, intent, null);
    }

    private static File checkPath(final String path) {
        final File f;
        return (!TextUtils.isEmpty(path) && (f = new File(path)).canRead()) ? f : null;
    }

    /**
     * Try to force extract a absolute filepath from an intent
     *
     * @param receivingIntent The intent from {@link Activity#getIntent()}
     * @return A file or null if extraction did not succeed
     */
    @SuppressWarnings({"ResultOfMethodCallIgnored", "ConstantConditions"})
    public File extractFileFromIntent(final Context context, final Intent receivingIntent) {
        final String action = receivingIntent.getAction();
        final String type = receivingIntent.getType();
        final String extPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        String tmps;
        String fileStr;
        File result = null;

        if ((Intent.ACTION_VIEW.equals(action) || Intent.ACTION_EDIT.equals(action)) || Intent.ACTION_SEND.equals(action)) {

            // Màrkor, SimpleMobileTools FileManager
            if (receivingIntent.hasExtra((tmps = EXTRA_FILEPATH))) {
                result = checkPath(receivingIntent.getStringExtra(tmps));
            }

            // Analyze data/Uri
            Uri fileUri = receivingIntent.getData();
            fileUri = (fileUri != null ? fileUri : receivingIntent.getParcelableExtra(Intent.EXTRA_STREAM));
            if (result == null && fileUri != null && (fileStr = fileUri.toString()) != null) {
                // Uri contains file
                if (fileStr.startsWith("file://")) {
                    result = checkPath(fileUri.getPath());
                }

                if (fileStr.startsWith((tmps = "content://"))) {
                    fileStr = fileStr.substring(tmps.length());
                    String fileProvider = fileStr.substring(0, fileStr.indexOf("/"));
                    fileStr = fileStr.substring(fileProvider.length() + 1);

                    // Some file managers dont add leading slash
                    if (fileStr.startsWith("storage/")) {
                        fileStr = "/" + fileStr;
                    }
                    // Some do add some custom prefix
                    for (String prefix : new String[]{"file", "document", "root_files", "name"}) {
                        if (fileStr.startsWith(prefix)) {
                            fileStr = fileStr.substring(prefix.length());
                        }
                    }

                    // prefix for External storage (/storage/emulated/0  ///  /sdcard/) --> e.g. "content://com.amaze.filemanager/storage_root/file.txt" = "/sdcard/file.txt"
                    for (String prefix : new String[]{"external/", "media/", "storage_root/", "external-path/"}) {
                        if (result == null && fileStr.startsWith((tmps = prefix))) {
                            result = checkPath(Uri.decode(extPath + "/" + fileStr.substring(tmps.length())));
                        }
                    }

                    // Next/OwnCloud Fileprovider
                    for (String fp : new String[]{"org.nextcloud.files", "org.nextcloud.beta.files", "org.owncloud.files"}) {
                        if (result == null && fileProvider.equals(fp) && fileStr.startsWith(tmps = "external_files/")) {
                            result = checkPath(Uri.decode("/storage/" + fileStr.substring(tmps.length()).trim()));
                        }
                    }

                    // AOSP File Manager/Documents
                    if (result == null && fileProvider.equals("com.android.externalstorage.documents") && fileStr.startsWith(tmps = "/primary%3A")) {
                        result = checkPath(Uri.decode(extPath + "/" + fileStr.substring(tmps.length())));
                    }

                    // Mi File Explorer
                    if (result == null && fileProvider.equals("com.mi.android.globalFileexplorer.myprovider") && fileStr.startsWith(tmps = "external_files")) {
                        result = checkPath(Uri.decode(extPath + fileStr.substring(tmps.length())));
                    }

                    if (result == null && fileStr.startsWith(tmps = "external_files/")) {
                        for (String prefix : new String[]{extPath, "/storage", ""}) {
                            if (result == null) {
                                result = checkPath(Uri.decode(prefix + "/" + fileStr.substring(tmps.length())));
                            }
                        }
                    }

                    // URI Encoded paths with full path after content://package/
                    if (result == null && fileStr.startsWith("/") || fileStr.startsWith("%2F")) {
                        result = checkPath(Uri.decode(fileStr));
                        if (result == null) {
                            result = checkPath(fileStr);
                        }
                    }
                }
            }

            fileUri = receivingIntent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (result == null && fileUri != null && !TextUtils.isEmpty(tmps = fileUri.getPath()) && tmps.startsWith("/")) {
                result = checkPath(tmps);
            }

            // Scan MediaStore.MediaColumns
            final String[] sarr = contentColumnData(context, receivingIntent, MediaStore.MediaColumns.DATA, (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? MediaStore.MediaColumns.DATA : null));
            if (result == null && sarr[0] != null) {
                result = checkPath(sarr[0]);
            }

            if (result == null && sarr[1] != null) {
                result = checkPath(Environment.getExternalStorageDirectory() + "/" + sarr[1]);
            }
        }

        // Try build proxy by ContentResolver if no file found
        if (result == null) {
            try {
                // Try detect content file & filename in Intent
                Uri uri = new ShareCompat.IntentReader(context, receivingIntent).getStream();
                uri = (uri != null ? uri : receivingIntent.getData());
                final String[] sarr = contentColumnData(context, receivingIntent, OpenableColumns.DISPLAY_NAME);
                tmps = sarr != null && !TextUtils.isEmpty(sarr[0]) ? sarr[0] : uri.getLastPathSegment();

                // Proxy file to app-private storage (= java.io.File)
                File f = new File(context.getCacheDir(), CONTENT_RESOLVER_FILE_PROXY_SEGMENT + "/" + tmps);
                f.getParentFile().mkdirs();
                byte[] data = GsFileUtils.readCloseBinaryStream(context.getContentResolver().openInputStream(uri));
                GsFileUtils.writeFile(f, data, null);
                f.setReadable(true);
                f.setWritable(true);
                result = checkPath(f.getAbsolutePath());
            } catch (Exception ignored) {
            }
        }

        return result;
    }

    public static String[] contentColumnData(final Context context, final Intent intent, final String... columns) {
        final String[] out = (new String[columns.length]);
        final int INVALID = -1;
        Cursor cursor;
        try {
            cursor = context.getContentResolver().query(intent.getData(), columns, null, null, null);
        } catch (Exception ignored) {
            cursor = null;
        }
        if (cursor != null && cursor.moveToFirst()) {
            for (int i = 0; i < columns.length; i++) {
                final int coli = TextUtils.isEmpty(columns[i]) ? INVALID : cursor.getColumnIndex(columns[i]);
                out[i] = (coli == INVALID ? null : cursor.getString(coli));
            }
        }
        if (cursor != null) {
            cursor.close();
        }
        return out;
    }

    /**
     * Request a picture from gallery
     * Result will be available from {@link Activity}.onActivityResult.
     * It will return the path to the image if locally stored. If retrieved from e.g. a cloud
     * service, the image will get copied to app-cache folder and it's path returned.
     */
    public void requestGalleryPicture(final Activity activity, final GsCallback.a1<String> callback) {
        final Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        try {
            activity.startActivityForResult(intent, REQUEST_PICK_PICTURE);
            setPathCallback(callback);
        } catch (Exception ex) {
            Toast.makeText(activity, "No gallery app installed!", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean requestAudioRecording(final Activity activity, final GsCallback.a1<String> callback) {
        final Intent intent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
        try {
            activity.startActivityForResult(intent, REQUEST_RECORD_AUDIO);
            setPathCallback(callback);
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }

    public String extractFileFromIntentStr(final Context context, final Intent receivingIntent) {
        File f = extractFileFromIntent(context, receivingIntent);
        return f != null ? f.getAbsolutePath() : null;
    }

    /**
     * Request a picture from camera-like apps
     * Result ({@link String}) will be available from {@link Activity}.onActivityResult.
     * It has set resultCode to {@link Activity#RESULT_OK} with same requestCode, if successfully
     * The requested image savepath has to be stored at caller side (not contained in intent),
     * it can be retrieved using {@link #extractResultFromActivityResult(Activity, int, int, Intent)}
     * returns null if an error happened.
     */
    public void requestCameraPicture(final Activity activity, GsCallback.a1<String> callback) {
        try {
            final Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            final File picDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            final String name = GsFileUtils.getFilenameWithTimestamp("IMG", "", ".jpg");
            final File imageTemp = GsFileUtils.findNonConflictingDest(picDir, name);

            if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null && imageTemp.createNewFile()) {
                imageTemp.deleteOnExit();
                // Continue only if the File was successfully created
                final Uri uri;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    uri = FileProvider.getUriForFile(activity, getFileProvider(activity), imageTemp);
                } else {
                    uri = Uri.fromFile(imageTemp);
                }
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri).putExtra(Intent.EXTRA_RETURN_RESULT, true);
                activity.startActivityForResult(takePictureIntent, REQUEST_CAMERA_PICTURE);
                _lastCameraPictureFilepath = imageTemp.getAbsolutePath();
                setPathCallback(callback);
            }
        } catch (IOException ignored) {
        }
    }

    private void setPathCallback(final GsCallback.a1<String> callback) {
        _receivePathCallback = new WeakReference<>(callback);
    }

    private void sendPathCallback(final String path) {
        if (!GsTextUtils.isNullOrEmpty(path) && _receivePathCallback != null) {
            final GsCallback.a1<String> cb = _receivePathCallback.get();
            if (cb != null) {
                cb.callback(path);
            }
        }
        // Send only once and once only
        _receivePathCallback = null;
    }

    /**
     * Extract result data from {@link Activity}.onActivityResult.
     * Forward all arguments from context. Only requestCodes as implemented in {@link GsContextUtils} are analyzed.
     * Also may forward results via callback
     */
    @SuppressLint("ApplySharedPref")
    public void extractResultFromActivityResult(final Activity context, final int requestCode, final int resultCode, final Intent intent) {
        switch (requestCode) {
            case REQUEST_CAMERA_PICTURE: {
                sendPathCallback(resultCode == Activity.RESULT_OK ? _lastCameraPictureFilepath : null);
                break;
            }
            case REQUEST_PICK_PICTURE: {
                if (resultCode == Activity.RESULT_OK && intent != null) {
                    Uri selectedImage = intent.getData();
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};
                    String picturePath = null;

                    Cursor cursor = context.getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        for (String column : filePathColumn) {
                            int curColIndex = cursor.getColumnIndex(column);
                            if (curColIndex == -1) {
                                continue;
                            }
                            picturePath = cursor.getString(curColIndex);
                            if (!TextUtils.isEmpty(picturePath)) {
                                break;
                            }
                        }
                        cursor.close();
                    }

                    // Try to grab via file extraction method
                    intent.setAction(Intent.ACTION_VIEW);
                    picturePath = picturePath != null ? picturePath : extractFileFromIntentStr(context, intent);

                    // Retrieve image from file descriptor / Cloud, e.g.: Google Drive, Picasa
                    if (picturePath == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        try {
                            final ParcelFileDescriptor parcelFileDescriptor = context.getContentResolver().openFileDescriptor(selectedImage, "r");
                            if (parcelFileDescriptor != null) {
                                final FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                                final FileInputStream input = new FileInputStream(fileDescriptor);

                                // Create temporary file in cache directory
                                final File temp = File.createTempFile("image", "tmp", context.getCacheDir());
                                temp.deleteOnExit();
                                picturePath = temp.getAbsolutePath();

                                GsFileUtils.writeFile(new File(picturePath), GsFileUtils.readCloseBinaryStream(input), null);
                            }
                        } catch (IOException ignored) {
                            // nothing we can do here, null value will be handled below
                        }
                    }

                    // Return path to picture on success, else null
                    sendPathCallback(picturePath);
                }
                break;
            }
            case REQUEST_RECORD_AUDIO: {
                if (resultCode == Activity.RESULT_OK && intent != null && intent.getData() != null) {
                    final Uri uri = intent.getData();
                    final String uriPath = uri.getPath();
                    final String ext = uriPath.substring(uriPath.lastIndexOf("."));
                    final String datestr = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss", Locale.ENGLISH).format(new Date());
                    final File temp = new File(context.getCacheDir(), datestr + ext);
                    GsFileUtils.copyUriToFile(context, uri, temp);
                    sendPathCallback(temp.getAbsolutePath());
                }
                break;
            }
            case REQUEST_SAF: {
                if (resultCode == Activity.RESULT_OK && intent != null && intent.getData() != null) {
                    final Uri treeUri = intent.getData();
                    PreferenceManager.getDefaultSharedPreferences(context).edit().putString(PREF_KEY__SAF_TREE_URI, treeUri.toString()).commit();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        final ContentResolver resolver = context.getContentResolver();
                        try {
                            resolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        } catch (SecurityException se) {
                            resolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        }
                    }
                }
                break;
            }
            case REQUEST_STORAGE_PERMISSION_M:
            case REQUEST_STORAGE_PERMISSION_R: {
                checkExternalStoragePermission(context);
                break;
            }
        }
    }

    /**
     * Send a local broadcast (to receive within app), with given action and string-extra+value.
     * This is a convenience method for quickly sending just one thing.
     */
    public void sendLocalBroadcastWithStringExtra(final Context context, final String action, final String extra, final CharSequence value) {
        Intent intent = new Intent(action);
        intent.putExtra(extra, value);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    /**
     * Receive broadcast results via a callback method
     *
     * @param callback       Function to call with received {@link Intent}
     * @param autoUnregister Whether or not to automatically unregister receiver after first match
     * @param filterActions  All {@link IntentFilter} actions to filter for
     * @return The created instance. Has to be unregistered on {@link Activity} lifecycle events.
     */
    public BroadcastReceiver receiveResultFromLocalBroadcast(final Context context, final GsCallback.a2<Intent, BroadcastReceiver> callback, final boolean autoUnregister, final String... filterActions) {
        IntentFilter intentFilter = new IntentFilter();
        for (String filterAction : filterActions) {
            intentFilter.addAction(filterAction);
        }
        final BroadcastReceiver br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null) {
                    if (autoUnregister) {
                        LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
                    }
                    try {
                        callback.callback(intent, this);
                    } catch (Exception ignored) {
                    }
                }
            }
        };
        LocalBroadcastManager.getInstance(context).registerReceiver(br, intentFilter);
        return br;
    }

    /**
     * Request edit of file
     *
     * @param context Context to use to get provider and start activity
     * @param file    File that should be edited
     */
    public void requestFileEdit(final Context context, File file) {
        if (file == null || !file.exists()) {
            return;
        }

        try {
            file = file.getCanonicalFile();
            final Uri uri = FileProvider.getUriForFile(context, getFileProvider(context), file);
            final Intent intent = new Intent(Intent.ACTION_EDIT);
            intent.setDataAndType(uri, GsFileUtils.getMimeType(file));
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            intent.putExtra(EXTRA_FILEPATH, file.getPath());
            startActivity(context, intent);
        } catch (IOException e) {
            Log.e(GsContextUtils.class.getName(), "ERROR: Failed to get canonical file path");
        }
    }

    /**
     * Get content://media/ Uri for given file, or null if not indexed
     *
     * @param file Target file
     * @param mode 1 for picture, 2 for video, anything else for other
     * @return Media URI
     */
    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    public Uri getMediaUri(final Context context, final File file, final int mode) {
        Uri uri = MediaStore.Files.getContentUri("external");
        uri = (mode != 0) ? (mode == 1 ? MediaStore.Images.Media.EXTERNAL_CONTENT_URI : MediaStore.Video.Media.EXTERNAL_CONTENT_URI) : uri;

        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, new String[]{MediaStore.Images.Media._ID}, MediaStore.Images.Media.DATA + "= ?", new String[]{file.getAbsolutePath()}, null);
            if (cursor != null && cursor.moveToFirst()) {
                @SuppressLint("Range")
                int mediaid = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media._ID));
                return Uri.withAppendedPath(uri, mediaid + "");
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * By default Chrome Custom Tabs only uses Chrome Stable to open links
     * There are also other packages (like Chrome Beta, Chromium, Firefox, ..)
     * which implement the Chrome Custom Tab interface. This method changes
     * the customtab intent to use an available compatible browser, if available.
     */
    public void enableChromeCustomTabsForOtherBrowsers(final Context context, final Intent customTabIntent) {
        String[] checkpkgs = new String[]{
                "com.android.chrome", "com.chrome.beta", "com.chrome.dev", "com.google.android.apps.chrome", "org.chromium.chrome",
                "org.mozilla.fennec_fdroid", "org.mozilla.firefox", "org.mozilla.firefox_beta", "org.mozilla.fennec_aurora",
                "org.mozilla.klar", "org.mozilla.focus",
        };

        // Get all intent handlers for web links
        PackageManager pm = context.getPackageManager();
        Intent urlIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.example.com"));
        List<String> browsers = new ArrayList<>();
        for (ResolveInfo ri : pm.queryIntentActivities(urlIntent, 0)) {
            Intent i = new Intent("android.support.customtabs.action.CustomTabsService");
            i.setPackage(ri.activityInfo.packageName);
            if (pm.resolveService(i, 0) != null) {
                browsers.add(ri.activityInfo.packageName);
            }
        }

        // Check if the user has a "default browser" selected
        ResolveInfo ri = pm.resolveActivity(urlIntent, 0);
        String userDefaultBrowser = (ri == null) ? null : ri.activityInfo.packageName;

        // Select which browser to use out of all installed customtab supporting browsers
        String pkg = null;
        if (browsers.size() == 1) {
            pkg = browsers.get(0);
        } else if (!TextUtils.isEmpty(userDefaultBrowser) && browsers.contains(userDefaultBrowser)) {
            pkg = userDefaultBrowser;
        } else if (!browsers.isEmpty()) {
            for (String checkpkg : checkpkgs) {
                if (browsers.contains(checkpkg)) {
                    pkg = checkpkg;
                    break;
                }
            }
            if (pkg == null) {
                pkg = browsers.get(0);
            }
        }
        if (pkg != null && customTabIntent != null) {
            customTabIntent.setPackage(pkg);
        }
    }

    @SuppressWarnings("deprecation")
    public boolean openWebpageInChromeCustomTab(final Context context, final String url) {
        boolean ok = false;
        try {
            // Use a CustomTabsIntent.Builder to configure CustomTabsIntent.
            // Once ready, call CustomTabsIntent.Builder.build() to create a CustomTabsIntent
            // and launch the desired Url with CustomTabsIntent.launchUrl()
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            builder.setToolbarColor(ContextCompat.getColor(context, getResId(context, GsContextUtils.ResType.COLOR, "primary")));
            builder.setSecondaryToolbarColor(ContextCompat.getColor(context, getResId(context, GsContextUtils.ResType.COLOR, "primary_dark")));
            builder.addDefaultShareMenuItem();
            CustomTabsIntent customTabsIntent = builder.build();
            enableChromeCustomTabsForOtherBrowsers(context, customTabsIntent.intent);
            customTabsIntent.launchUrl(context, Uri.parse(url));
            ok = true;
        } catch (Exception ignored) {
        }
        return ok;
    }

    /***
     * Request storage access. The user needs to press "Select storage" at the correct storage.
     * @param context The {@link Activity} which will receive the result from startActivityForResult
     */
    public void requestStorageAccessFramework(final Activity context) {
        if (context != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            );
            context.startActivityForResult(intent, REQUEST_SAF);
        }
    }

    /**
     * Get storage access framework tree uri. The user must have granted access via {@link #requestStorageAccessFramework(Activity)}
     *
     * @return Uri or null if not granted yet
     */
    public Uri getStorageAccessFrameworkTreeUri(final Context context) {
        String treeStr = PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_KEY__SAF_TREE_URI, null);
        if (!TextUtils.isEmpty(treeStr)) {
            try {
                return Uri.parse(treeStr);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /**
     * Get mounted storage folder root (by tree uri). The user must have granted access via {@link #requestStorageAccessFramework(Activity)}
     *
     * @return File or null if SD not mounted
     */
    public File getStorageAccessFolder(final Context context) {
        Uri safUri = getStorageAccessFrameworkTreeUri(context);
        if (safUri != null) {
            String safUriStr = safUri.toString();
            for (Pair<File, String> storage : getStorages(context, false, true)) {
                String storageFolderName = storage.first.getName();
                if (safUriStr.contains(storageFolderName)) {
                    return storage.first;
                }
            }
        }
        return null;
    }

    /**
     * Check whether or not a file is under a storage access folder (external storage / SD)
     *
     * @param file The file object (file/folder)
     * @return Whether or not the file is under storage access folder
     */
    public boolean isUnderStorageAccessFolder(final Context context, final File file, boolean isDir) {
        if (file != null) {
            isDir = isDir || (file.exists() && file.isDirectory());
            // When file writeable as is, it's the fastest way to learn SAF isn't required
            if (canWriteFile(context, file, isDir, false)) {
                return false;
            }
            for (Pair<File, String> storage : getStorages(context, false, true)) {
                if (file.getAbsolutePath().startsWith(storage.first.getAbsolutePath())) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isContentResolverProxyFile(final File file) {
        return file != null && file.getParentFile() != null && CONTENT_RESOLVER_FILE_PROXY_SEGMENT.equals(file.getParentFile().getName());
    }

    public Collection<File> getCacheDirs(final Context context) {
        final Set<File> dirs = new HashSet<>();
        dirs.add(context.getCacheDir());
        dirs.add(context.getExternalCacheDir());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            dirs.addAll(Arrays.asList(context.getExternalCacheDirs()));
        }
        dirs.removeAll(Collections.singleton(null));
        return dirs;
    }

    /**
     * Check whether or not a file can be written.
     * Requires storage access framework permission for external storage (SD)
     *
     * @param file  The file object (file/folder)
     * @param isDir Whether or not the given file parameter is a directory
     * @return Whether or not the file can be written
     */
    public boolean canWriteFile(final Context context, final File file, final boolean isDir, final boolean trySaf) {
        if (file == null) {
            return false;
        }

        // Try direct file access
        if (GsFileUtils.canCreate(file)) {
            return true;
        }

        // Own AppData directories do not require any special permission or handling
        if (GsCollectionUtils.any(getCacheDirs(context), f -> GsFileUtils.isChild(f, file))) {
            return true;
        }

        if (trySaf) {
            final DocumentFile dof = getDocumentFile(context, file, isDir);
            return dof != null && dof.canWrite();
        }

        return false;
    }

    /**
     * Get a {@link DocumentFile} object out of a normal java {@link File}.
     * When used on a external storage (SD), use {@link #requestStorageAccessFramework(Activity)}
     * first to get access. Otherwise this will fail.
     *
     * @param file  The file/folder to convert
     * @param isDir Whether or not file is a directory. For non-existing (to be created) files this info is not known hence required.
     * @return A {@link DocumentFile} object or null if file cannot be converted
     */
    @SuppressWarnings("RegExpRedundantEscape")
    public DocumentFile getDocumentFile(final Context context, final File file, final boolean isDir) {
        // On older versions use fromFile
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            return DocumentFile.fromFile(file);
        }

        // Find storage root folder
        File baseFolderFile = getStorageRootFolder(context, file);
        String baseFolder = baseFolderFile == null ? null : baseFolderFile.getAbsolutePath();
        boolean originalDirectory = false;
        if (baseFolder == null) {
            return null;
        }

        String relPath = null;
        try {
            String fullPath = file.getCanonicalPath();
            if (!baseFolder.equals(fullPath)) {
                relPath = fullPath.substring(baseFolder.length() + 1);
            } else {
                originalDirectory = true;
            }
        } catch (IOException e) {
            return null;
        } catch (Exception ignored) {
            originalDirectory = true;
        }
        Uri treeUri;
        if ((treeUri = getStorageAccessFrameworkTreeUri(context)) == null) {
            return null;
        }
        DocumentFile dof = DocumentFile.fromTreeUri(context, treeUri);
        if (originalDirectory) {
            return dof;
        }
        String[] parts = relPath.split("\\/");
        for (int i = 0; i < parts.length; i++) {
            @SuppressWarnings("ConstantConditions")
            DocumentFile nextDof = dof.findFile(parts[i]);
            if (nextDof == null) {
                try {
                    nextDof = ((i < parts.length - 1) || isDir) ? dof.createDirectory(parts[i]) : dof.createFile("image", parts[i]);
                } catch (Exception ignored) {
                }
            }
            dof = nextDof;
        }
        return dof;
    }

    public void showMountSdDialog(final Activity context, @StringRes final int title, @StringRes final int description, @DrawableRes final int mountDescriptionGraphic) {
        // Image viewer
        ImageView imv = new ImageView(context);
        imv.setImageResource(mountDescriptionGraphic);
        imv.setAdjustViewBounds(true);

        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setView(imv);
        dialog.setTitle(title);
        dialog.setMessage(context.getString(description) + "\n\n");
        dialog.setNegativeButton(android.R.string.cancel, null);
        dialog.setPositiveButton(android.R.string.yes, (dialogInterface, i) -> requestStorageAccessFramework(context));
        AlertDialog dialogi = dialog.create();
        dialogi.show();
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored", "StatementWithEmptyBody"})
    public void writeFile(final Activity context, final File file, final boolean isDirectory, final GsCallback.a2<Boolean, OutputStream> writeFileCallback) {
        try {
            OutputStream fileOutputStream = null;
            ParcelFileDescriptor pfd = null;
            final boolean existingEmptyFile = file.canWrite() && file.length() < TEXTFILE_OVERWRITE_MIN_TEXT_LENGTH;
            final boolean nonExistingCreatableFile = !file.exists() && file.getParentFile() != null && file.getParentFile().canWrite();
            if (isContentResolverProxyFile(file)) {
                // File initially read from Activity, Intent & ContentResolver -> write back to it
                try {
                    Intent intent = context.getIntent();
                    Uri uri = new ShareCompat.IntentReader(context, intent).getStream();
                    uri = (uri != null ? uri : intent.getData());
                    fileOutputStream = context.getContentResolver().openOutputStream(uri, "rwt");
                } catch (Exception ignored) {
                }
            } else if (existingEmptyFile || nonExistingCreatableFile) {
                if (isDirectory) {
                    file.mkdirs();
                } else {
                    fileOutputStream = new FileOutputStream(file);
                }
            } else {
                DocumentFile dof = getDocumentFile(context, file, isDirectory);
                if (dof != null && dof.canWrite()) {
                    if (isDirectory) {
                        // Nothing to do
                    } else {
                        pfd = context.getContentResolver().openFileDescriptor(dof.getUri(), "rwt");
                        fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());
                    }
                }
            }
            if (writeFileCallback != null) {
                writeFileCallback.callback(fileOutputStream != null || (isDirectory && file.exists()), fileOutputStream);
            }
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.flush();
                    fileOutputStream.close();
                } catch (Exception ignored) {
                }
            }
            if (pfd != null) {
                pfd.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Call telephone number.
     * Non direct call, opens up the dialer and pre-sets the telephone number. User needs to press manually.
     * Direct call requires M permission granted, also add permissions to manifest:
     * <uses-permission android:name="android.permission.CALL_PHONE" />
     *
     * @param telNo      The telephone number to call
     * @param directCall Direct call number if possible
     */
    @SuppressWarnings("SimplifiableConditionalExpression")
    public void callTelephoneNumber(final Activity context, String telNo, final boolean... directCall) {
        boolean ldirectCall = (directCall != null && directCall.length > 0) ? directCall[0] : true;
        telNo = telNo.replaceAll("(?i)(tel:?)+", "");

        if (android.os.Build.VERSION.SDK_INT >= 23 && ldirectCall && context != null) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.CALL_PHONE}, 4001);
                ldirectCall = false;
            } else {
                try {
                    Intent callIntent = new Intent(Intent.ACTION_CALL);
                    callIntent.setData(Uri.parse("tel:" + telNo));
                    startActivity(context, callIntent);
                } catch (Exception ignored) {
                    ldirectCall = false;
                }
            }
        }
        // Show dialer up with telephone number pre-inserted
        if (!ldirectCall) {
            startActivity(context, new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", telNo, null)));
        }
    }

    /**
     * @param locale   {@link Locale} locale
     * @param format   {@link String} text which 'll be used as format for {@link SimpleDateFormat}
     * @param datetime {@link Long}   requested time miliseconds
     * @param fallback {@link String} default fallback value. If the format is incorrect and a default is not provided, return the specified format
     * @return formatted string
     */
    public String formatDateTime(@Nullable final Locale locale, @NonNull final String format, @Nullable final Long datetime, @Nullable final String... fallback) {
        try {
            final Locale l = locale != null ? locale : Locale.getDefault();
            final long t = datetime != null ? datetime : System.currentTimeMillis();
            return new SimpleDateFormat(GsTextUtils.unescapeString(format), l).format(t);
        } catch (Exception err) {
            return (fallback != null && fallback.length > 0) ? fallback[0] : format;
        }
    }

    public String formatDateTime(@Nullable final Context context, @NonNull final String format, @Nullable final Long datetime, @Nullable final String... def) {
        Locale locale = null;
        if (context != null) {
            locale = ConfigurationCompat.getLocales(context.getResources().getConfiguration()).get(0);
        }
        return formatDateTime(locale, format, datetime, def);
    }

    public void requestExternalStoragePermission(final Activity activity) {
        final int v = android.os.Build.VERSION.SDK_INT;

        if (v >= Build.VERSION_CODES.R) {
            try {
                final Uri uri = Uri.parse("package:" + getAppIdFlavorSpecific(activity));
                final Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri);
                activity.startActivityForResult(intent, REQUEST_STORAGE_PERMISSION_R);
            } catch (final Exception ex) {
                final Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                activity.startActivityForResult(intent, REQUEST_STORAGE_PERMISSION_R);
            }
        }

        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION_M);
    }

    public void requestExternalStoragePermission(final Activity activity, @StringRes int description) {
        requestExternalStoragePermission(activity, activity.getString(description));
    }

    public void requestExternalStoragePermission(final Activity activity, final String description) {
        final AlertDialog d = new AlertDialog.Builder(activity)
                .setMessage(description)
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> requestExternalStoragePermission(activity))
                .setNegativeButton(android.R.string.no, null)
                .show();
        d.setCanceledOnTouchOutside(false);
    }

    @SuppressWarnings("ConstantConditions")
    public boolean checkExternalStoragePermission(final Context context) {
        final int v = android.os.Build.VERSION.SDK_INT;

        // Android R Manage-All-Files permission
        if (v >= android.os.Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }

        // Android M permissions
        if (v >= android.os.Build.VERSION_CODES.M && v < android.os.Build.VERSION_CODES.R) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }

        // In case unsure, check if anything is writable at external storage
        for (final File f : Environment.getExternalStorageDirectory() != null ? Environment.getExternalStorageDirectory().listFiles() : new File[0]) {
            if (f.canWrite()) {
                return true;
            }
        }

        return false;
    }

    public List<Pair<String, String>> extractFileMetadata(final Context context, File file, boolean withHtml) {
        if (m_cacheLastExtractFileMetadata != null && m_cacheLastExtractFileMetadata.first.equals(file)) {
            return m_cacheLastExtractFileMetadata.second;
        }
        final Uri fileUri = Uri.fromFile(file.getAbsoluteFile());
        final ArrayList<Pair<String, String>> extracted = new ArrayList<>();

        // "Last modified" -> R.string.last_modified
        final GsCallback.a2<String, String> append = (key, value) -> {
            final int resId = getResId(context, GsContextUtils.ResType.STRING, key);
            extracted.add(new Pair<>((resId != 0 ? context.getString(resId) : key), value));
        };

        // java.io.File metadata like name, size, modtime
        append.callback("File", file.getAbsolutePath());
        append.callback("Size", GsFileUtils.getReadableFileSize(file.length(), true));
        append.callback("Last modified", DateUtils.formatDateTime(context, file.lastModified(), (DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_NUMERIC_DATE)));

        // Detect all possible metadata keys from MediaMetadataRetriever as there is no queryAll method
        final List<Pair<Integer, String>> mmrfields = new ArrayList<>();
        for (Field field : MediaMetadataRetriever.class.getDeclaredFields()) {
            String prefix = "METADATA_KEY_";
            String name = field.getName();
            if (name.startsWith(prefix)) {
                prefix = GsTextUtils.toTitleCase(name.replace(prefix, "").replace("_", " ").replaceAll("\\s*(?i)num(ber)?\\s*", " No. "));
                try {
                    mmrfields.add(new Pair<>(field.getInt(null), prefix));
                } catch (Exception ignored) {
                }
            }
        }
        //noinspection ComparatorCombinators
        Collections.sort(mmrfields, (sortO1, sortO2) -> sortO1.first - sortO2.first);

        // Extractor for generic multimedia file metadata like title/artist
        // setDataSource may throw exception on certain files, hence wrap the call
        final MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(context, fileUri);
        } catch (Exception ignored) {
        }

        // Extract Cover& preview if available
        try {
            Bitmap bitmap;
            final byte[] data = withHtml ? mmr.getEmbeddedPicture() : null;
            if (data != null) {
                bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                append.callback("Cover", "<img src='data:image/jpeg;base64,%s' style='max-height: 85vh; max-width: 100%;' />".replace("%s", imageToBase64(bitmap, Bitmap.CompressFormat.JPEG, 50)));
            }
            bitmap = withHtml ? mmr.getFrameAtTime() : null;
            if (bitmap != null) {
                append.callback("Preview", "<img src='data:image/jpeg;base64,%s' style='max-height: 85vh; max-width: 100%;' />".replace("%s", imageToBase64(bitmap, Bitmap.CompressFormat.JPEG, 50)));
            }
        } catch (Exception ignored) {
        }

        // Extract all other detected fields
        for (final Pair<Integer, String> mmrfield : mmrfields) {
            String v = null;
            try {
                v = mmr.extractMetadata(mmrfield.first);
            } catch (Exception ignored) {
            }
            if (!TextUtils.isEmpty(v)) {
                if (mmrfield.first == MediaMetadataRetriever.METADATA_KEY_BITRATE) {
                    v = GsFileUtils.getHumanReadableByteCountSI(Long.parseLong(v)) + "ps";
                    if (v.startsWith("-1 ")) {
                        continue; // invalid / unknown
                    }
                } else if (mmrfield.first == MediaMetadataRetriever.METADATA_KEY_DURATION) {
                    final int[] hms = GsFileUtils.getTimeDiffHMS(Long.parseLong(v), 0);
                    v = String.format("%sh %sm %ss", hms[0], hms[1], hms[2]);
                    if (v.equals("0h 0m 0s")) {
                        continue; // Duration key might be set but no actual duration information
                    }
                }
                append.callback(mmrfield.second, v);
            }
        }

        // free resources
        try {
            mmr.release();
        } catch (Exception ignored) {
        }
        m_cacheLastExtractFileMetadata = new Pair<>(file, extracted);
        return extracted;
    }

    public static String imageToBase64(Bitmap bitmap, Bitmap.CompressFormat format, int q) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(format, q, outputStream);
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT).replaceAll("\\s+", "");
    }


    public Snackbar showSnackBar(final Activity context, @StringRes int stringResId, boolean showLong) {
        final Snackbar s = Snackbar.make(context.findViewById(android.R.id.content), stringResId, (showLong ? Snackbar.LENGTH_LONG : Snackbar.LENGTH_SHORT));
        s.show();
        return s;
    }

    public void showSnackBar(final Activity context, @StringRes int stringResId, boolean showLong, @StringRes int actionResId, View.OnClickListener listener) {
        Snackbar.make(context.findViewById(android.R.id.content), stringResId, (showLong ? Snackbar.LENGTH_LONG : Snackbar.LENGTH_SHORT))
                .setAction(actionResId, listener)
                .show();
    }

    public <T extends GsContextUtils> T showSoftKeyboard(final Activity activity, final boolean show, final View... view) {
        if (activity != null) {
            final InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
            final View focus = (view != null && view.length > 0) ? view[0] : activity.getCurrentFocus();
            final IBinder token = focus != null ? focus.getWindowToken() : null;
            if (imm != null && focus != null) {
                if (show) {
                    imm.showSoftInput(focus, InputMethodManager.SHOW_IMPLICIT);
                } else if (token != null) {
                    imm.hideSoftInputFromWindow(token, InputMethodManager.HIDE_IMPLICIT_ONLY);
                }
            }
        }
        return thisp();
    }

    public void showDialogWithHtmlTextView(final Activity context, @StringRes int resTitleId, String html) {
        showDialogWithHtmlTextView(context, resTitleId, html, true, null);
    }

    public void showDialogWithHtmlTextView(final Activity context, @StringRes int resTitleId, String text, boolean isHtml, DialogInterface.OnDismissListener dismissedListener) {
        ScrollView scroll = new ScrollView(context);
        AppCompatTextView textView = new AppCompatTextView(context);
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, context.getResources().getDisplayMetrics());

        scroll.setPadding(padding, 0, padding, 0);
        scroll.addView(textView);
        textView.setMovementMethod(new LinkMovementMethod());
        textView.setText(isHtml ? new SpannableString(Html.fromHtml(text)) : text);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);

        AlertDialog.Builder dialog = new AlertDialog.Builder(context)
                .setPositiveButton(android.R.string.ok, null).setOnDismissListener(dismissedListener)
                .setView(scroll);
        if (resTitleId != 0) {
            dialog.setTitle(resTitleId);
        }
        dialogFullWidth(dialog.show(), true, false);
    }

    public void showDialogWithRawFileInWebView(final Activity context, String fileInRaw, @StringRes int resTitleId) {
        final WebView wv = new WebView(context);
        wv.loadUrl("file:///android_res/raw/" + fileInRaw);
        AlertDialog.Builder dialog = new AlertDialog.Builder(context)
                .setPositiveButton(android.R.string.ok, null)
                .setTitle(resTitleId)
                .setView(wv);
        dialogFullWidth(dialog.show(), true, false);
    }

    // Toggle with no param, else set visibility according to first bool
    public <T extends GsContextUtils> T toggleStatusbarVisibility(final Activity context, boolean... optionalForceVisible) {
        WindowManager.LayoutParams attrs = context.getWindow().getAttributes();
        int flag = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        if (optionalForceVisible.length == 0) {
            attrs.flags ^= flag;
        } else if (optionalForceVisible.length == 1 && optionalForceVisible[0]) {
            attrs.flags &= ~flag;
        } else {
            attrs.flags |= flag;
        }
        context.getWindow().setAttributes(attrs);
        return thisp();
    }

    public <T extends GsContextUtils> T showGooglePlayEntryForThisApp(final Context context) {
        String pkgId = "details?id=" + context.getPackageName();
        try {
            final Intent gplay = new Intent(Intent.ACTION_VIEW, Uri.parse("market://" + pkgId));
            gplay.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            gplay.addFlags((Build.VERSION.SDK_INT >= 21 ? Intent.FLAG_ACTIVITY_NEW_DOCUMENT : Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET));
            startActivity(context, gplay);
        } catch (ActivityNotFoundException e) {
            openWebpageInExternalBrowser(context, "https://play.google.com/store/apps/" + pkgId);
        }
        return thisp();
    }

    public <T extends GsContextUtils> T setStatusbarColor(final Activity context, int color, boolean... fromRes) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (fromRes != null && fromRes.length > 0 && fromRes[0]) {
                color = ContextCompat.getColor(context, color);
            }

            context.getWindow().setStatusBarColor(color);
        }
        return thisp();
    }

    public boolean isLauncherEnabled(final Context context, final Class activityClass) {
        try {
            ComponentName component = new ComponentName(context, activityClass);
            return context.getPackageManager().getComponentEnabledSetting(component) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        } catch (Exception ignored) {
        }
        return false;
    }

    @ColorInt
    public Integer getCurrentPrimaryColor(final Context context) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(getResId(context, ResType.ATTR, "colorPrimary"), typedValue, true);
        return typedValue.data;
    }

    @ColorInt
    public Integer getCurrentPrimaryDarkColor(final Context context) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(getResId(context, ResType.ATTR, "colorPrimaryDark"), typedValue, true);
        return typedValue.data;
    }

    @ColorInt
    public Integer getCurrentAccentColor(final Context context) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(getResId(context, ResType.ATTR, "colorAccent"), typedValue, true);
        return typedValue.data;
    }

    @ColorInt
    public Integer getActivityBackgroundColor(final Activity activity) {
        TypedArray array = activity.getTheme().obtainStyledAttributes(new int[]{
                android.R.attr.colorBackground,
        });
        int c = array.getColor(0, 0xFF0000);
        array.recycle();
        return c;
    }

    @ColorInt
    public int getListDividerColor(@Nullable final Activity activity) {
        GsContextUtils cu = GsContextUtils.instance;
        final String forBlackBg = "#d1d1d1", forWhiteBg = "#3d3d3d";
        boolean isWhiteBg = true;
        try {
            //noinspection ConstantConditions
            isWhiteBg = shouldColorOnTopBeLight(getActivityBackgroundColor(activity));
        } catch (Exception ignored) {
        }
        return Color.parseColor(isWhiteBg ? forWhiteBg : forBlackBg);
    }

    public <T extends GsContextUtils> T setActivityBackgroundColor(final Activity activity, @ColorInt Integer color) {
        if (color != null) {
            try {
                ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0).setBackgroundColor(color);
            } catch (Exception ignored) {
            }
        }
        return thisp();
    }

    public <T extends GsContextUtils> T setActivityNavigationBarBackgroundColor(final Activity context, @ColorInt Integer color) {
        if (context != null && color != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    final Window window = context.getWindow();
                    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                    window.setNavigationBarColor(color);
                }
            } catch (Exception ignored) {
            }
        }
        return thisp();
    }

    public <T extends GsContextUtils> T startCalendarApp(final Context context) {
        Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();
        builder.appendPath("time");
        builder.appendPath(Long.toString(System.currentTimeMillis()));
        Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        startActivity(context, intent);
        return thisp();
    }

    /**
     * Detect if the activity is currently in splitscreen/multiwindow mode
     */
    public boolean isInSplitScreenMode(final Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return activity.isInMultiWindowMode();
        }
        return false;
    }

    public void setKeepScreenOn(final Activity activity, Boolean keepOn) {
        if (keepOn) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    /**
     * Show dialog in full width / show keyboard
     *
     * @param dialog Get via dialog.show()
     */
    public void dialogFullWidth(AlertDialog dialog, boolean fullWidth, boolean showKeyboard) {
        try {
            Window w;
            if (dialog != null && (w = dialog.getWindow()) != null) {
                if (fullWidth) {
                    w.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
                }
                if (showKeyboard) {
                    w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static void windowAspectRatio(final Window window,
                                         final DisplayMetrics displayMetrics,
                                         float portraitWidthRatio,
                                         float portraitHeightRatio,
                                         float landscapeWidthRatio,
                                         float landscapeHeightRatio) {
        if (window == null) {
            return;
        }

        WindowManager.LayoutParams params = window.getAttributes();
        final int width = displayMetrics.widthPixels;
        final int height = displayMetrics.heightPixels;
        if (width < height) { // Portrait
            params.width = (int) (width * portraitWidthRatio);
            params.height = (int) (height * portraitHeightRatio);
        } else { // Landscape
            params.width = (int) (width * landscapeWidthRatio);
            params.height = (int) (height * landscapeHeightRatio);
        }
        window.setAttributes(params);
    }

    // Make activity/app not show up in the recents history - call before finish / System.exit
    public <T extends GsContextUtils> T removeActivityFromHistory(final Context activity) {
        try {
            ActivityManager am = (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                List<ActivityManager.AppTask> tasks = am.getAppTasks();
                if (tasks != null && !tasks.isEmpty()) {
                    tasks.get(0).setExcludeFromRecents(true);
                }
            }

        } catch (Exception ignored) {
        }
        return thisp();
    }

    /**
     * Set Android day-night theme
     *
     * @param pref one out of system (daynight toggle), auto (daynight hour), autocompat (hour 5-17), light (fixed), dark (fixed)
     */
    @SuppressLint("WrongConstant")
    public void applyDayNightTheme(final String pref) {
        final boolean prefLight = pref.contains("light") || ("autocompat".equals(pref) && isCurrentHourOfDayBetween(9, 17));
        final boolean prefDark = pref.contains("dark") || ("autocompat".equals(pref) && !isCurrentHourOfDayBetween(9, 17));

        if (prefLight) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (prefDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else if ("system".equals(pref)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        } else if ("auto".equals(pref)) {
            //noinspection deprecation
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
        }
    }

    /**
     * A method to determine if current hour is between begin and end.
     * This is especially useful for time-based light/dark mode
     */
    public boolean isCurrentHourOfDayBetween(int begin, int end) {
        begin = (begin >= 23 || begin < 0) ? 0 : begin;
        end = (end >= 23 || end < 0) ? 0 : end;
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        return h >= begin && h <= end;
    }

    @SuppressLint("SwitchIntDef")
    public void nextScreenRotationSetting(final Activity context) {
        String text;
        int nextOrientation;
        switch (context.getRequestedOrientation()) {
            case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE: {
                nextOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                text = "Portrait";
                break;
            }
            case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT: {
                nextOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR;
                text = "Sensor";
                break;
            }
            case ActivityInfo.SCREEN_ORIENTATION_SENSOR: {
                nextOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
                text = "Default";
                break;
            }
            default: {
                nextOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                text = "Landscape";
                break;
            }
        }
        int resId = getResId(context, ResType.STRING, text);
        text = (resId != 0 ? context.getString(resId) : text);
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
        context.setRequestedOrientation(nextOrientation);
    }

    /**
     * Check if the Dark theme mode is enable in this app currently (or at system if system theme is set)
     *
     * @param context {@link Context}
     * @return true if the dark theme/mode is currently enabled in this app
     */
    public boolean isDarkModeEnabled(final Context context) {
        final int state = AppCompatDelegate.getDefaultNightMode();
        if (state == AppCompatDelegate.MODE_NIGHT_YES) {
            return true;
        } else if (state == AppCompatDelegate.MODE_NIGHT_NO) {
            return false;
        } else {
            switch (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) {
                case Configuration.UI_MODE_NIGHT_YES:
                    return true;
                case Configuration.UI_MODE_NIGHT_NO:
                    return false;
            }
        }
        return false;
    }

    public static void blinkView(final View view) {
        if (view == null) {
            return;
        }

        final ObjectAnimator animator = ObjectAnimator
                .ofFloat(view, View.ALPHA, 0.2f, 1.0f)
                .setDuration(500L);

        view.setTag(BLINK_ANIMATOR_TAG, new WeakReference<>(animator));

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                view.setAlpha(1.0f);
                view.setTag(BLINK_ANIMATOR_TAG, null);
            }
        });

        animator.start();
    }

    public static void stopBlinking(final View view) {
        if (view == null) {
            return;
        }

        final Object tagRef = view.getTag(BLINK_ANIMATOR_TAG);
        if (tagRef instanceof WeakReference) {
            final Object tag = ((WeakReference<?>) tagRef).get();
            if (tag instanceof ObjectAnimator) {
                final ObjectAnimator anim = ((ObjectAnimator) tag);
                if (anim.isRunning()) {
                    anim.cancel();
                }
            }
        }
    }

    public static boolean fadeInOut(final View in, final View out, final boolean animate) {
        // Do nothing if we are already in the correct state
        if (in.getVisibility() == View.VISIBLE && out.getVisibility() == View.GONE) {
            return false;
        }

        in.setVisibility(View.VISIBLE);
        if (animate) {
            in.setAlpha(0);
            in.animate().alpha(1).setDuration(200).setListener(null);
            out.animate()
                    .alpha(0)
                    .setDuration(200)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            out.setVisibility(View.GONE);
                        }
                    });
        } else {
            out.setVisibility(View.GONE);
        }

        return true;
    }
}
