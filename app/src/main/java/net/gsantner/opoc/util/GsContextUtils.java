/*#######################################################
 *
 * SPDX-FileCopyrightText: 2016-2022 Gregor Santner <https://gsantner.net/>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2016-2022 by Gregor Santner <https://gsantner.net/>
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
#########################################################*/
package net.gsantner.opoc.util;

import static android.content.Context.VIBRATOR_SERVICE;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.graphics.Bitmap.CompressFormat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
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
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Html;
import android.text.InputFilter;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.annotation.StringRes;
import androidx.core.app.ActivityManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.text.TextUtilsCompat;
import androidx.core.view.ViewCompat;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import net.gsantner.opoc.format.GsSimpleMarkdownParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@SuppressWarnings({"WeakerAccess", "unused", "SameParameterValue", "ObsoleteSdkInt", "deprecation", "SpellCheckingInspection", "TryFinallyCanBeTryWithResources", "UnusedAssignment", "UnusedReturnValue"})
public class GsContextUtils {
    public static final GsContextUtils instance = new GsContextUtils();

    protected <T extends GsContextUtils> T thisp() {
        //noinspection unchecked
        return (T) this;
    }

    //
    // Members, Constructors
    //
    public GsContextUtils() {
    }

    public GsContextUtils(Context context) {
    }

    public void freeContextRef() {
    }

    public <T extends GsContextUtils> T setLauncherActivityEnabled(final Context context, Class activityClass, boolean enable) {
        try {
            ComponentName component = new ComponentName(context, activityClass);
            context.getPackageManager().setComponentEnabledSetting(component, enable ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        } catch (Exception ignored) {
        }
        return thisp();
    }

    //
    // Class Methods
    //
    public enum ResType {
        ID, BOOL, INTEGER, COLOR, STRING, ARRAY, DRAWABLE, PLURALS,
        ANIM, ATTR, DIMEN, LAYOUT, MENU, RAW, STYLE, XML,
    }

    /**
     * Find out the nuermical ressource id by given {@link ResType}
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
     * Get String by given string ressource id (nuermic)
     */
    public String rstr(Context context, @StringRes final int strResId) {
        try {
            return context.getString(strResId);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get String by given string ressource identifier (textual)
     */
    public String rstr(final Context context, final String strResKey, Object... a0getResKeyAsFallback) {
        try {
            return rstr(context, getResId(context, ResType.STRING, strResKey));
        } catch (Resources.NotFoundException e) {
            return a0getResKeyAsFallback != null && a0getResKeyAsFallback.length > 0 ? strResKey : null;
        }
    }

    /**
     * Get drawable from given ressource identifier
     */
    public Drawable rdrawable(final Context context, @DrawableRes final int resId) {
        try {
            return ContextCompat.getDrawable(context, resId);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get color by given color ressource id
     */
    public int rcolor(final Context context, @ColorRes final int resId) {
        if (resId == 0) {
            Log.e(getClass().getName(), "ContextUtils::rcolor: resId is 0!");
            return Color.BLACK;
        }
        return ContextCompat.getColor(context, resId);
    }

    /**
     * Checks if all given (textual) ressource ids are available
     *
     * @param resType       A {@link ResType}
     * @param resIdsTextual A (textual) identifier to be awaited at R.restype.resIdsTextual
     * @return True if all given ids are available
     */
    public boolean areRessourcesAvailable(final Context context, final ResType resType, final String... resIdsTextual) {
        for (String name : resIdsTextual) {
            if (getResId(context, resType, name) == 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Convert an int color to a hex string. Optionally including alpha value.
     *
     * @param intColor  The color coded in int
     * @param withAlpha Optional; Set first bool parameter to true to also include alpha value
     */
    public static String colorToHexString(final int intColor, final boolean... withAlpha) {
        boolean a = withAlpha != null && withAlpha.length >= 1 && withAlpha[0];
        return String.format(a ? "#%08X" : "#%06X", (a ? 0xFFFFFFFF : 0xFFFFFF) & intColor);
    }

    public static String getAndroidVersion() {
        return Build.VERSION.RELEASE + " (" + Build.VERSION.SDK_INT + ")";
    }

    public String getAppVersionName(final Context context) {
        final PackageManager manager = context.getPackageManager();
        try {
            PackageInfo info = manager.getPackageInfo(getPackageIdReal(context), 0);
            return info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            try {
                PackageInfo info = manager.getPackageInfo(getPackageIdManifest(context), 0);
                return info.versionName;
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        return "?";
    }

    public String getAppInstallationSource(final Context context) {
        String src = null;
        try {
            src = context.getPackageManager().getInstallerPackageName(getPackageIdReal(context));
        } catch (Exception ignored) {
            try {
                src = context.getPackageManager().getInstallerPackageName(getPackageIdManifest(context));
            } catch (Exception ignored2) {
            }
        }
        if (src == null || src.trim().isEmpty()) {
            return "Sideloaded";
        } else if (src.toLowerCase(Locale.ENGLISH).contains(".amazon.")) {
            return "Amazon Appstore";
        }
        switch (src) {
            case "com.android.vending":
            case "com.google.android.feedback": {
                return "Google Play";
            }
            case "org.fdroid.fdroid.privileged":
            case "org.fdroid.fdroid": {
                return "F-Droid";
            }
            case "com.github.yeriomin.yalpstore": {
                return "Yalp Store";
            }
            case "cm.aptoide.pt": {
                return "Aptoide";
            }
            case "com.android.packageinstaller": {
                return "Package Installer";
            }
        }
        return src;
    }

    /**
     * Send a {@link Intent#ACTION_VIEW} Intent with given paramter
     * If the parameter is an string a browser will get triggered
     */
    public <T extends GsContextUtils> T openWebpageInExternalBrowser(final Context context, final String url) {
        try {
            Uri uri = Uri.parse(url);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK); // = works without Activity context
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return thisp();
    }

    /**
     * Get the apps base packagename, which is equal with all build flavors and variants
     */
    public String getPackageIdManifest(final Context context) {
        String pkg = rstr(context, "manifest_package_id");
        return !TextUtils.isEmpty(pkg) ? pkg : context.getPackageName();
    }

    /**
     * Get this apps package name, returns the flavor specific package name.
     */
    public String getPackageIdReal(final Context context) {
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
        final String pkg = getPackageIdManifest(context) + ".BuildConfig";
        try {
            Class<?> c = Class.forName(pkg);
            return c.getField(fieldName).get(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<String> getBuildConfigFields(final Context context) {
        final String pkg = getPackageIdManifest(context) + ".BuildConfig";
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

    /**
     * Request a bitcoin donation with given details.
     * All parameters are awaited as string resource ids
     */
    public void showDonateBitcoinRequest(final Context context, @StringRes final int srBitcoinId, @StringRes final int srBitcoinAmount, @StringRes final int srBitcoinMessage, @StringRes final int srAlternativeDonateUrl) {
        if (!isGooglePlayBuild(context)) {
            String btcUri = String.format("bitcoin:%s?amount=%s&label=%s&message=%s",
                    rstr(context, srBitcoinId), rstr(context, srBitcoinAmount),
                    rstr(context, srBitcoinMessage), rstr(context, srBitcoinMessage));
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(btcUri));
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK); // = works without Activity context
            try {
                context.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                openWebpageInExternalBrowser(context, rstr(context, srAlternativeDonateUrl));
            }
        }
    }

    public String readTextfileFromRawRes(final Context context, @RawRes int rawResId, String linePrefix, String linePostfix) {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = null;
        String line;

        linePrefix = linePrefix == null ? "" : linePrefix;
        linePostfix = linePostfix == null ? "" : linePostfix;

        try {
            br = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(rawResId)));
            while ((line = br.readLine()) != null) {
                sb.append(linePrefix);
                sb.append(line);
                sb.append(linePostfix);
                sb.append("\n");
            }
        } catch (Exception ignored) {
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ignored) {
                }
            }
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
     * Check if app with given {@code packageName} is installed
     */
    public boolean isAppInstalled(final Context context, String packageName) {
        try {
            PackageManager pm = context.getApplicationContext().getPackageManager();
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * Restart the current app. Supply the class to start on startup
     */
    @SuppressWarnings("rawtypes")
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
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
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
        DisplayMetrics dm = context.getResources().getDisplayMetrics();

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
        setLocale(context, locale);
        return thisp();
    }

    public <T extends GsContextUtils> T setLocale(final Context context, final Locale locale) {
        Configuration config = context.getResources().getConfiguration();
        config.locale = (locale != null ? locale : Resources.getSystem().getConfiguration().locale);
        context.getResources().updateConfiguration(config, null);
        Locale.setDefault(locale);
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
            filesDir = new File(new File(context.getPackageManager().getPackageInfo(getPackageIdReal(context), 0).applicationInfo.dataDir), "files");
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
    public boolean writeImageToFile(final File targetFile, final Bitmap image, Integer... a0quality) {
        final int quality = (a0quality != null && a0quality.length > 0 && a0quality[0] >= 0 && a0quality[0] <= 100) ? a0quality[0] : 70;
        final String lc = targetFile.getAbsolutePath().toLowerCase(Locale.ROOT);
        final CompressFormat format = lc.endsWith(".webp") ? CompressFormat.WEBP : (lc.endsWith(".png") ? CompressFormat.PNG : CompressFormat.JPEG);

        boolean ok = false;
        File folder = new File(targetFile.getParent());
        if (folder.exists() || folder.mkdirs()) {
            FileOutputStream stream = null;
            try {
                stream = new FileOutputStream(targetFile);
                image.compress(format, quality, stream);
                ok = true;
            } catch (Exception ignored) {
            } finally {
                try {
                    if (stream != null) {
                        stream.close();
                    }
                } catch (IOException ignored) {
                }
            }
        }
        try {
            image.recycle();
        } catch (Exception ignored) {
        }
        return ok;
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
    @SuppressWarnings("ConstantConditions")
    public void tintMenuItems(final Menu menu, final boolean recurse, @ColorInt final int iconColor) {
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            try {
                tintDrawable(item.getIcon(), iconColor);
                if (item.hasSubMenu() && recurse) {
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
    public void setSubMenuIconsVisiblity(final Menu menu, final boolean visible) {
        if (TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault()) == ViewCompat.LAYOUT_DIRECTION_RTL) {
            return;
        }
        if (menu.getClass().getSimpleName().equals("MenuBuilder")) {
            try {
                @SuppressLint("PrivateApi") Method m = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);
                m.setAccessible(true);
                m.invoke(menu, visible);
            } catch (Exception ignored) {
                Log.d(getClass().getName(), "Error: 'setSubMenuIconsVisiblity' not supported on this device");
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
    @SuppressWarnings("Convert2Lambda")
    public static final InputFilter INPUTFILTER_FILENAME = new InputFilter() {
        public CharSequence filter(final CharSequence src, final int start, final int end, final Spanned dest, final int dstart, final int dend) {
            if (src != null && src.length() > 0) {
                for (char disallowed : "|\\?*<\":>[]/'".toCharArray()) {
                    for (int i = start; i < end; i++) {
                        if (src.charAt(i) == disallowed) {
                            return "";
                        }
                    }
                }
            }
            return null;
        }
    };

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
     * Android/Java's own MimeType map is very very small and detection barely works at all
     * Hence use custom map for some file extensions
     */
    public String getMimeType(final Context context, String uri) {
        String mimeType = null;
        uri = uri.replaceFirst("\\.jenc$", "");
        if (uri.startsWith(ContentResolver.SCHEME_CONTENT + "://")) {
            ContentResolver cr = context.getContentResolver();
            mimeType = cr.getType(Uri.parse(uri));
        } else {
            String ext = MimeTypeMap.getFileExtensionFromUrl(uri);
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase());

            // Try to guess if the recommended methods fail
            if (TextUtils.isEmpty(mimeType)) {
                switch (ext) {
                    case "md":
                    case "markdown":
                    case "mkd":
                    case "mdown":
                    case "mkdn":
                    case "mdwn":
                    case "rmd":
                        mimeType = "text/markdown";
                        break;
                    case "yaml":
                    case "yml":
                        mimeType = "text/yaml";
                        break;
                    case "json":
                        mimeType = "text/json";
                        break;
                    case "txt":
                        mimeType = "text/plain";
                        break;
                }
            }
        }

        if (TextUtils.isEmpty(mimeType)) {
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
        Vibrator vibrator = ((Vibrator) context.getSystemService(VIBRATOR_SERVICE));
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
            if (info.name.toLowerCase(Locale.ENGLISH).contains("fileprovider")) {
                return info.authority;
            }
        }
        return null;
    }

    public final static String EXTRA_FILEPATH = "real_file_path_2";
    public final static SimpleDateFormat DATEFORMAT_RFC3339ISH = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss", Locale.getDefault());
    public final static SimpleDateFormat DATEFORMAT_IMG = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()); //20190511-230845
    public final static String MIME_TEXT_PLAIN = "text/plain";
    public final static String PREF_KEY__SAF_TREE_URI = "pref_key__saf_tree_uri";
    public final static String CONTENT_RESOLVER_FILE_PROXY_SEGMENT = "CONTENT_RESOLVER_FILE_PROXY_SEGMENT";

    public final static int REQUEST_CAMERA_PICTURE = 50001;
    public final static int REQUEST_PICK_PICTURE = 50002;
    public final static int REQUEST_SAF = 50003;
    public final static int REQUEST_STORAGE_PERMISSION_M = 50004;
    public final static int REQUEST_STORAGE_PERMISSION_R = 50005;

    public final static int MIN_OVERWRITE_LENGTH = 2;

    protected static String _lastCameraPictureFilepath;
    protected static Pair<File, List<Pair<String, String>>> CACHE_LAST_EXTRACT_FILE_METADATA;
    protected String _chooserTitle = "➥";


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
}


