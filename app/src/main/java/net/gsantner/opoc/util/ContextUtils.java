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
import android.content.ClipData;
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
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.graphics.Bitmap.CompressFormat;

@SuppressWarnings({"WeakerAccess", "unused", "SameParameterValue", "ObsoleteSdkInt", "deprecation", "SpellCheckingInspection"})
public class ContextUtils {
    //########################
    //## Members, Constructors
    //########################
    protected Context _context;

    public ContextUtils(Context context) {
        _context = context;
    }

    public Context context() {
        return _context;
    }

    //########################
    //##    Resources
    //########################
    static class ResType {
        public static final String DRAWABLE = "drawable";
        public static final String STRING = "string";
        public static final String PLURAL = "plural";
        public static final String COLOR = "color";
        public static final String STYLE = "style";
        public static final String ARRAY = "array";
        public static final String DIMEN = "dimen";
        public static final String MENU = "menu";
        public static final String BOOL = "bool";
        public static final String RAW = "raw";
    }

    public String rstr(@StringRes int strResId) {
        return _context.getString(strResId);
    }

    public String rstr(String strResKey) {
        return rstr(getResId(ResType.STRING, strResKey));
    }

    public Drawable rdrawable(@DrawableRes int resId) {
        return ContextCompat.getDrawable(_context, resId);
    }

    public int rcolor(@ColorRes int resId) {
        return ContextCompat.getColor(_context, resId);
    }

    public int getResId(final String type, final String name) {
        return _context.getResources().getIdentifier(name, type, _context.getPackageName());
    }

    public boolean areResIdsAvailable(final String type, final String... names) {
        for (String name : names) {
            if (getResId(type, name) == 0) {
                return false;
            }
        }
        return true;
    }

    //########################
    //##    Methods
    //########################

    public String colorToHexString(int intColor) {
        return String.format("#%06X", 0xFFFFFF & intColor);
    }

    public String getAppVersionName() {
        try {
            PackageManager manager = _context.getPackageManager();
            PackageInfo info = manager.getPackageInfo(_context.getPackageName(), 0);
            return info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "?";
        }
    }

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
     * Get field from ${applicationId}.BuildConfig
     * May be helpful in libraries, where a access to
     * BuildConfig would only get values of the library
     * rather than the app ones. It awaits a string resource
     * of the package set in manifest (root element).
     * Falls back to applicationId of the app which may differ from manifest.
     */
    public Object getBuildConfigValue(String fieldName) {
        String pkg;
        try {
            pkg = rstr("manifest_package_id");
        } catch (Resources.NotFoundException ex) {
            pkg = _context.getPackageName();
        }
        pkg += ".BuildConfig";
        try {
            Class<?> c = Class.forName(pkg);
            return c.getField(fieldName).get(null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean bcbool(String fieldName, boolean defaultValue) {
        Object field = getBuildConfigValue(fieldName);
        if (field != null && field instanceof Boolean) {
            return (Boolean) field;
        }
        return defaultValue;
    }

    public String bcstr(String fieldName, String defaultValue) {
        Object field = getBuildConfigValue(fieldName);
        if (field != null && field instanceof String) {
            return (String) field;
        }
        return defaultValue;
    }

    public boolean isGooglePlayBuild() {
        return bcbool("IS_GPLAY_BUILD", true);
    }

    public boolean isFossBuild() {
        return bcbool("IS_FOSS_BUILD", false);
    }

    // Requires donate__bitcoin_* resources (see below) to be available as string resource
    public void showDonateBitcoinRequest(@StringRes final int strResBitcoinId, @StringRes final int strResBitcoinAmount, @StringRes final int strResBitcoinMessage, @StringRes final int strResAlternativeDonateUrl) {
        if (!isGooglePlayBuild()) {
            String btcUri = String.format("bitcoin:%s?amount=%s&label=%s&message=%s",
                    rstr(strResBitcoinId), rstr(strResBitcoinAmount),
                    rstr(strResBitcoinMessage), rstr(strResBitcoinMessage));
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(btcUri));
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            try {
                _context.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                openWebpageInExternalBrowser(rstr(strResAlternativeDonateUrl));
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

    public boolean isAppInstalled(String packageName) {
        PackageManager pm = _context.getApplicationContext().getPackageManager();
        try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

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

    public void setHtmlToTextView(TextView textView, String html) {
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setText(new SpannableString(htmlToSpanned(html)));
    }

    public double getEstimatedScreenSizeInches() {
        DisplayMetrics dm = _context.getResources().getDisplayMetrics();

        double density = dm.density * 160;
        double x = Math.pow(dm.widthPixels / density, 2);
        double y = Math.pow(dm.heightPixels / density, 2);
        double screenInches = Math.sqrt(x + y) * 1.16;  // 1.16 = est. Nav/Statusbar
        screenInches = screenInches < 4.0 ? 4.0 : screenInches;
        screenInches = screenInches > 12.0 ? 12.0 : screenInches;
        return screenInches;
    }

    public boolean isInPortraitMode() {
        return _context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    public Locale getLocaleByAndroidCode(String code) {
        if (!TextUtils.isEmpty(code)) {
            return code.contains("-r")
                    ? new Locale(code.substring(0, 2), code.substring(4, 6)) // de-rAt
                    : new Locale(code); // de
        }
        return Resources.getSystem().getConfiguration().locale;
    }

    //  en/de/de-rAt ; Empty string -> default locale
    public void setAppLanguage(String androidLocaleString) {
        Locale locale = getLocaleByAndroidCode(androidLocaleString);
        Configuration config = _context.getResources().getConfiguration();
        config.locale = (locale != null && !androidLocaleString.isEmpty())
                ? locale : Resources.getSystem().getConfiguration().locale;
        _context.getResources().updateConfiguration(config, null);
    }

    // Find out if color above the given color should be light or dark. true if light
    public boolean shouldColorOnTopBeLight(@ColorInt int colorOnBottomInt) {
        return 186 > (((0.299 * Color.red(colorOnBottomInt))
                + ((0.587 * Color.green(colorOnBottomInt))
                + (0.114 * Color.blue(colorOnBottomInt)))));
    }

    public Spanned htmlToSpanned(String html) {
        Spanned result;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            result = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
        } else {
            result = Html.fromHtml(html);
        }
        return result;
    }

    public boolean setClipboard(String text) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager cm = ((android.text.ClipboardManager) _context.getSystemService(Context.CLIPBOARD_SERVICE));
            if (cm != null) {
                cm.setText(text);
                return true;
            }
        } else {
            android.content.ClipboardManager cm = ((android.content.ClipboardManager) _context.getSystemService(Context.CLIPBOARD_SERVICE));
            if (cm != null) {
                ClipData clip = ClipData.newPlainText(_context.getPackageName(), text);
                cm.setPrimaryClip(clip);
                return true;
            }
        }
        return false;
    }

    public List<String> getClipboard() {
        List<String> clipper = new ArrayList<>();
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager cm = ((android.text.ClipboardManager) _context.getSystemService(Context.CLIPBOARD_SERVICE));
            if (cm != null) {
                clipper.add(cm.getText().toString());
            }

        } else {
            android.content.ClipboardManager cm = ((android.content.ClipboardManager) _context.getSystemService(Context.CLIPBOARD_SERVICE));
            if (cm != null && cm.hasPrimaryClip()) {
                ClipData data = cm.getPrimaryClip();
                for (int i = 0; i < data.getItemCount() && i < data.getItemCount(); i++) {
                    clipper.add(data.getItemAt(i).getText().toString());
                }
            }

        }
        return clipper;
    }

    public float convertPxToDp(final float px) {
        return px / _context.getResources().getDisplayMetrics().density;
    }

    public float convertDpToPx(final float dp) {
        return dp * _context.getResources().getDisplayMetrics().density;
    }

    public static void setDrawableWithColorToImageView(ImageView imageView, @DrawableRes int drawableResId, @ColorRes int colorResId) {
        imageView.setImageResource(drawableResId);
        imageView.setColorFilter(ContextCompat.getColor(imageView.getContext(), colorResId));
    }

    public Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && (drawable instanceof VectorDrawable || drawable instanceof VectorDrawableCompat)) {
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

    public Bitmap scaleBitmap(Bitmap bitmap, int maxDimen) {
        int picSize = Math.min(bitmap.getHeight(), bitmap.getWidth());
        float scale = 1.f * maxDimen / picSize;
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public boolean writeImageToFileJpeg(File imageFile, Bitmap image) {
        return writeImageToFile(imageFile, image, Bitmap.CompressFormat.JPEG, 95);
    }

    /**
     * Write bitmap to the filesystem
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

    public Bitmap drawTextOnDrawable(@DrawableRes int resId, String text, int textSize) {
        Resources resources = _context.getResources();
        float scale = resources.getDisplayMetrics().density;
        Bitmap bitmap = bitmapToDrawable(resId);

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

    public Bitmap bitmapToDrawable(@DrawableRes int drawableId) {
        Bitmap bitmap = null;
        Drawable drawable = ContextCompat.getDrawable(_context, drawableId);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && (drawable instanceof VectorDrawable || drawable instanceof VectorDrawableCompat)) {
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

    public Drawable tintDrawable(@DrawableRes int drawableRes, @ColorInt int color) {
        return tintDrawable(_context.getResources().getDrawable(drawableRes), color);
    }

    public Drawable tintDrawable(@Nullable Drawable drawable, @ColorInt int color) {
        if (drawable != null) {
            drawable = DrawableCompat.wrap(drawable);
            DrawableCompat.setTint(drawable.mutate(), color);
        }
        return drawable;
    }

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
