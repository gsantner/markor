/*#######################################################
 *
 *   Maintained by Gregor Santner, 2016-
 *   https://gsantner.net/
 *
 *   License: Apache 2.0
 *  https://github.com/gsantner/opoc/#licensing
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.opoc.util;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
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
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.support.annotation.StringRes;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import net.gsantner.opoc.format.markdown.SimpleMarkdownParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.Locale;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.graphics.Bitmap.CompressFormat;

@SuppressWarnings({"WeakerAccess", "unused", "SameParameterValue", "ObsoleteSdkInt", "deprecation", "SpellCheckingInspection"})
public class ContextUtils {
    //
    // Members, Constructors
    //
    protected Context _context;

    public ContextUtils(Context context) {
        _context = context;
    }

    public Context context() {
        return _context;
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
    public int getResId(ResType resType, final String name) {
        return _context.getResources().getIdentifier(name, resType.name().toLowerCase(), _context.getPackageName());
    }

    /**
     * Get String by given string ressource id (nuermic)
     */
    public String rstr(@StringRes int strResId) {
        return _context.getString(strResId);
    }

    /**
     * Get String by given string ressource identifier (textual)
     */
    public String rstr(String strResKey) {
        try {
            return rstr(getResId(ResType.STRING, strResKey));
        } catch (Resources.NotFoundException e) {
            return null;
        }
    }

    /**
     * Get drawable from given ressource identifier
     */
    public Drawable rdrawable(@DrawableRes int resId) {
        return ContextCompat.getDrawable(_context, resId);
    }

    /**
     * Get color by given color ressource id
     */
    public int rcolor(@ColorRes int resId) {
        return ContextCompat.getColor(_context, resId);
    }

    /**
     * Checks if all given (textual) ressource ids are available
     *
     * @param resType       A {@link ResType}
     * @param resIdsTextual A (textual) identifier to be awaited at R.restype.resIdsTextual
     * @return True if all given ids are available
     */
    public boolean areRessourcesAvailable(final ResType resType, final String... resIdsTextual) {
        for (String name : resIdsTextual) {
            if (getResId(resType, name) == 0) {
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
    public String colorToHexString(int intColor, boolean... withAlpha) {
        boolean a = withAlpha != null && withAlpha.length >= 1 && withAlpha[0];
        return String.format(a ? "#%08X" : "#%06X", (a ? 0xFFFFFFFF : 0xFFFFFF) & intColor);
    }

    public String getAppVersionName() {
        try {
            PackageManager manager = _context.getPackageManager();
            PackageInfo info = manager.getPackageInfo(getPackageName(), 0);
            return info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "?";
        }
    }

    public String getAppInstallationSource() {
        String src = null;
        try {
            src = _context.getPackageManager().getInstallerPackageName(getPackageName());
        } catch (Exception ignored) {
        }
        if (TextUtils.isEmpty(src)) {
            return "Sideloaded";
        } else if (src.toLowerCase().contains(".amazon.")) {
            return "Amazon Appstore";
        }
        switch (src) {
            case "com.android.vending":
            case "com.google.android.feedback": {
                return "Google Play Store";
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
    public void openWebpageInExternalBrowser(final String url) {
        Uri uri = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        try {
            _context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get this apps package name. The builtin method may fail when used with flavors
     */
    public String getPackageName() {
        String pkg = rstr("manifest_package_id");
        return pkg != null ? pkg : _context.getPackageName();
    }

    /**
     * Get field from ${applicationId}.BuildConfig
     * May be helpful in libraries, where a access to
     * BuildConfig would only get values of the library
     * rather than the app ones. It awaits a string resource
     * of the package set in manifest (root element).
     * Falls back to applicationId of the app which may differ from manifest.
     */
    public Object getBuildConfigValue(String fieldName) {
        String pkg = getPackageName() + ".BuildConfig";
        try {
            Class<?> c = Class.forName(pkg);
            return c.getField(fieldName).get(null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get a BuildConfig bool value
     */
    public Boolean bcbool(String fieldName, Boolean defaultValue) {
        Object field = getBuildConfigValue(fieldName);
        if (field != null && field instanceof Boolean) {
            return (Boolean) field;
        }
        return defaultValue;
    }

    /**
     * Get a BuildConfig string value
     */
    public String bcstr(String fieldName, String defaultValue) {
        Object field = getBuildConfigValue(fieldName);
        if (field != null && field instanceof String) {
            return (String) field;
        }
        return defaultValue;
    }

    /**
     * Get a BuildConfig string value
     */
    public Integer bcint(String fieldName, int defaultValue) {
        Object field = getBuildConfigValue(fieldName);
        if (field != null && field instanceof Integer) {
            return (Integer) field;
        }
        return defaultValue;
    }

    /**
     * Check if this is a gplay build (requires BuildConfig field)
     */
    public boolean isGooglePlayBuild() {
        return bcbool("IS_GPLAY_BUILD", true);
    }

    /**
     * Check if this is a foss build (requires BuildConfig field)
     */
    public boolean isFossBuild() {
        return bcbool("IS_FOSS_BUILD", false);
    }

    /**
     * Request a bitcoin donation with given details.
     * All parameters are awaited as string resource ids
     */
    public void showDonateBitcoinRequest(@StringRes final int srBitcoinId, @StringRes final int srBitcoinAmount, @StringRes final int srBitcoinMessage, @StringRes final int srAlternativeDonateUrl) {
        if (!isGooglePlayBuild()) {
            String btcUri = String.format("bitcoin:%s?amount=%s&label=%s&message=%s",
                    rstr(srBitcoinId), rstr(srBitcoinAmount),
                    rstr(srBitcoinMessage), rstr(srBitcoinMessage));
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(btcUri));
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            try {
                _context.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                openWebpageInExternalBrowser(rstr(srAlternativeDonateUrl));
            }
        }
    }

    public String readTextfileFromRawRes(@RawRes int rawResId, String linePrefix, String linePostfix) {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = null;
        String line;

        linePrefix = linePrefix == null ? "" : linePrefix;
        linePostfix = linePostfix == null ? "" : linePostfix;

        try {
            br = new BufferedReader(new InputStreamReader(_context.getResources().openRawResource(rawResId)));
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
    public boolean isConnectedToInternet() {
        try {
            ConnectivityManager con = (ConnectivityManager) _context.getSystemService(Context.CONNECTIVITY_SERVICE);
            @SuppressLint("MissingPermission") NetworkInfo activeNetInfo =
                    con == null ? null : con.getActiveNetworkInfo();
            return activeNetInfo != null && activeNetInfo.isConnectedOrConnecting();
        } catch (Exception ignored) {
            throw new RuntimeException("Error: Developer forgot to declare a permission");
        }
    }

    /**
     * Check if app with given {@code packageName} is installed
     */
    public boolean isAppInstalled(String packageName) {
        PackageManager pm = _context.getApplicationContext().getPackageManager();
        try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * Restart the current app. Supply the class to start on startup
     */
    public void restartApp(Class classToStart) {
        Intent inte = new Intent(_context, classToStart);
        PendingIntent inteP = PendingIntent.getActivity(_context, 555, inte, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager) _context.getSystemService(Context.ALARM_SERVICE);
        if (mgr != null) {
            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, inteP);
        } else {
            inte.addFlags(FLAG_ACTIVITY_NEW_TASK);
            _context.startActivity(inte);
        }
        Runtime.getRuntime().exit(0);
    }

    /**
     * Load a markdown file from a {@link RawRes}, prepend each line with {@code prepend} text
     * and convert markdown to html using {@link SimpleMarkdownParser}
     */
    public String loadMarkdownForTextViewFromRaw(@RawRes int rawMdFile, String prepend) {
        try {
            return new SimpleMarkdownParser()
                    .parse(_context.getResources().openRawResource(rawMdFile),
                            prepend, SimpleMarkdownParser.FILTER_ANDROID_TEXTVIEW)
                    .replaceColor("#000001", rcolor(getResId(ResType.COLOR, "accent")))
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
    public void setHtmlToTextView(TextView textView, String html) {
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setText(new SpannableString(htmlToSpanned(html)));
    }

    /**
     * Estimate this device's screen diagonal size in inches
     */
    public double getEstimatedScreenSizeInches() {
        DisplayMetrics dm = _context.getResources().getDisplayMetrics();

        double calc = dm.density * 160d;
        double x = Math.pow(dm.widthPixels / calc, 2);
        double y = Math.pow(dm.heightPixels / calc, 2);
        calc = Math.sqrt(x + y) * 1.16;  // 1.16 = est. Nav/Statusbar
        return Math.min(12, Math.max(4, calc));
    }

    /**
     * Check if the device is currently in portrait orientation
     */
    public boolean isInPortraitMode() {
        return _context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
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
    public void setAppLanguage(String androidLC) {
        Locale locale = getLocaleByAndroidCode(androidLC);
        Configuration config = _context.getResources().getConfiguration();
        config.locale = (locale != null && !androidLC.isEmpty())
                ? locale : Resources.getSystem().getConfiguration().locale;
        _context.getResources().updateConfiguration(config, null);
    }

    /**
     * Try to guess if the color on top of the given {@code colorOnBottomInt}
     * should be light or dark. Returns true if top color should be light
     */
    public boolean shouldColorOnTopBeLight(@ColorInt int colorOnBottomInt) {
        return 186 > (((0.299 * Color.red(colorOnBottomInt))
                + ((0.587 * Color.green(colorOnBottomInt))
                + (0.114 * Color.blue(colorOnBottomInt)))));
    }

    /**
     * Convert a html string to an android {@link Spanned} object
     */
    public Spanned htmlToSpanned(String html) {
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
    public float convertPxToDp(final float px) {
        return px / _context.getResources().getDisplayMetrics().density;
    }

    /**
     * Convert android dp unit to pixel unit
     */
    public float convertDpToPx(final float dp) {
        return dp * _context.getResources().getDisplayMetrics().density;
    }

    /**
     * Request the givens paths to be scanned by MediaScanner
     *
     * @param files Files and folders to scan
     */
    public void mediaScannerScanFile(File... files) {
        if (android.os.Build.VERSION.SDK_INT > 19) {
            String[] paths = new String[files.length];
            for (int i = 0; i < files.length; i++) {
                paths[i] = files[i].getAbsolutePath();
            }
            MediaScannerConnection.scanFile(_context, paths, null, null);
        } else {
            for (File file : files) {
                _context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
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

            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
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
    public Bitmap drawableToBitmap(@DrawableRes int drawableId) {
        return drawableToBitmap(ContextCompat.getDrawable(_context, drawableId));
    }

    /**
     * Get a {@link Bitmap} from a given {@code imagePath} on the filesystem
     * Specifying a {@code maxDimen} is also possible and a value below 2000
     * is recommended, otherwise a {@link OutOfMemoryError} may occur
     */
    public Bitmap loadImageFromFilesystem(File imagePath, int maxDimen) {
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
    public int calculateInSampleSize(BitmapFactory.Options options, int maxDimen) {
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
    public Bitmap scaleBitmap(Bitmap bitmap, int maxDimen) {
        int picSize = Math.min(bitmap.getHeight(), bitmap.getWidth());
        float scale = 1.f * maxDimen / picSize;
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    /**
     * Write the given {@link Bitmap} to {@code imageFile}, in {@link CompressFormat#JPEG} format
     */
    public boolean writeImageToFileJpeg(File imageFile, Bitmap image) {
        return writeImageToFile(imageFile, image, Bitmap.CompressFormat.JPEG, 95);
    }

    /**
     * Write the given {@link Bitmap} to filesystem
     *
     * @param targetFile The file to be written in
     * @param image      The image as android {@link Bitmap}
     * @param format     One format of {@link CompressFormat}, null will determine based on filename
     * @param quality    Quality level, defaults to 95
     * @return True if writing was successful
     */
    public boolean writeImageToFile(File targetFile, Bitmap image, CompressFormat format, Integer quality) {
        File folder = new File(targetFile.getParent());
        if (quality == null || quality < 0 || quality > 100) {
            quality = 95;
        }
        if (format == null) {
            format = CompressFormat.JPEG;
            String lc = targetFile.getAbsolutePath().toLowerCase(Locale.ROOT);
            if (lc.endsWith(".png")) {
                format = CompressFormat.PNG;
            }
            if (lc.endsWith(".webp")) {
                format = CompressFormat.WEBP;
            }
        }
        if (folder.exists() || folder.mkdirs()) {
            FileOutputStream stream = null;
            try {
                stream = new FileOutputStream(targetFile); // overwrites this image every time
                image.compress(format, quality, stream);
                return true;
            } catch (FileNotFoundException ignored) {
            } finally {
                try {
                    if (stream != null) {
                        stream.close();
                    }
                } catch (IOException ignored) {
                }
            }
        }
        return false;
    }

    /**
     * Draw text in the center of the given {@link DrawableRes}
     * This may be useful for e.g. badge counts
     */
    public Bitmap drawTextOnDrawable(@DrawableRes int drawableRes, String text, int textSize) {
        Resources resources = _context.getResources();
        float scale = resources.getDisplayMetrics().density;
        Bitmap bitmap = drawableToBitmap(drawableRes);

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
    public void tintMenuItems(Menu menu, boolean recurse, @ColorInt int iconColor) {
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            tintDrawable(item.getIcon(), iconColor);
            if (item.hasSubMenu() && recurse) {
                tintMenuItems(item.getSubMenu(), recurse, iconColor);
            }
        }
    }

    /**
     * Loads {@link Drawable} by given {@link DrawableRes} and applies a color
     */
    public Drawable tintDrawable(@DrawableRes int drawableRes, @ColorInt int color) {
        return tintDrawable(rdrawable(drawableRes), color);
    }

    /**
     * Tint a {@link Drawable} with given {@code color}
     */
    public Drawable tintDrawable(@Nullable Drawable drawable, @ColorInt int color) {
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
    public void setSubMenuIconsVisiblity(Menu menu, boolean visible) {
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
}
