/*
 * ------------------------------------------------------------------------------
 * Gregor Santner <gsantner.github.io> wrote this. You can do whatever you want
 * with it. If we meet some day, and you think it is worth it, you can buy me a
 * coke in return. Provided as is without any kind of warranty. Do not blame or
 * sue me if something goes wrong. No attribution required.    - Gregor Santner
 *
 * License: Creative Commons Zero (CC0 1.0)
 *  http://creativecommons.org/publicdomain/zero/1.0/
 * ----------------------------------------------------------------------------
 */
package io.github.gsantner.opoc.util;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.RawRes;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.webkit.WebView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

@SuppressWarnings({"WeakerAccess", "unused", "SameParameterValue", "SpellCheckingInspection", "deprecation"})
public class Helpers {
    //########################
    //## Members, Constructors
    //########################
    protected Context _context;

    public Helpers(Context context) {
        _context = context;
    }

    //########################
    //##     Methods
    //########################
    public String str(@StringRes int strResId) {
        return _context.getString(strResId);
    }

    static class ResType {
        public static final String DRAWABLE = "drawable";
        public static final String STRING = "string";
        public static final String PLURAL = "plural";
        public static final String COLOR = "color";
        public static final String STYLE = "style";
        public static final String ARRAY = "array";
        public static final String DIMEN = "dimen";
        public static final String MENU = "menu";
        public static final String RAW = "raw";
    }

    public Drawable drawable(@DrawableRes int resId) {
        return ContextCompat.getDrawable(_context, resId);
    }

    public int color(@ColorRes int resId) {
        return ContextCompat.getColor(_context, resId);
    }

    public Context context() {
        return _context;
    }

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

    public void openWebpageInExternalBrowser(final String url) {
        Uri uri = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        _context.startActivity(intent);
    }

    /**
     * Get field from PackageId.BuildConfig
     * May be helpful in libraries, where a access to
     * BuildConfig would only get values of the library
     * rather than the app ones
     */
    public Object getBuildConfigValue(String fieldName) {
        try {
            Class<?> c = Class.forName(_context.getPackageName() + ".BuildConfig");
            return c.getField(fieldName).get(null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean getBuildConfigBoolean(String fieldName, boolean defaultValue) {
        Object field = getBuildConfigValue(fieldName);
        if (field != null && field instanceof Boolean) {
            return (Boolean) field;
        }
        return defaultValue;
    }

    public boolean isGooglePlayBuild() {
        return getBuildConfigBoolean("IS_GPLAY_BUILD", true);
    }

    public boolean isFossBuild() {
        return getBuildConfigBoolean("IS_FOSS_BUILD", false);
    }

    // Requires donate__bitcoin_* resources (see below) to be available as string resource
    public void showDonateBitcoinRequest(@StringRes final int strResBitcoinId, @StringRes final int strResBitcoinAmount, @StringRes final int strResBitcoinMessage, @StringRes final int strResAlternativeDonateUrl) {
        if (!isGooglePlayBuild()) {
            String btcUri = String.format("bitcoin:%s?amount=%s&label=%s&message=%s",
                    str(strResBitcoinId), str(strResBitcoinAmount),
                    str(strResBitcoinMessage), str(strResBitcoinMessage));
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(btcUri));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                _context.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                openWebpageInExternalBrowser(str(strResAlternativeDonateUrl));
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

    public void showDialogWithRawFileInWebView(String fileInRaw, @StringRes int resTitleId) {
        WebView wv = new WebView(_context);
        wv.loadUrl("file:///android_res/raw/" + fileInRaw);
        AlertDialog.Builder dialog = new AlertDialog.Builder(_context)
                .setPositiveButton(android.R.string.ok, null)
                .setTitle(resTitleId)
                .setView(wv);
        dialog.show();
    }

    @SuppressLint("RestrictedApi")
    @SuppressWarnings("RestrictedApi")
    public void setTintColorOfButton(AppCompatButton button, @ColorRes int resColor) {
        button.setSupportBackgroundTintList(ColorStateList.valueOf(
                color(resColor)
        ));
    }

    public boolean isConnectedToInternet() {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                _context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetInfo != null && activeNetInfo.isConnectedOrConnecting();
    }

    public void restartApp(Class classToStartupWith) {
        Intent restartIntent = new Intent(_context, classToStartupWith);
        PendingIntent restartIntentP = PendingIntent.getActivity(_context, 555,
                restartIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager) _context.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, restartIntentP);
        System.exit(0);
    }

    public String loadMarkdownForTextViewFromRaw(@RawRes int rawMdFile, String prepend) {
        try {
            return new SimpleMarkdownParser()
                    .parse(_context.getResources().openRawResource(rawMdFile),
                            prepend, SimpleMarkdownParser.FILTER_ANDROID_TEXTVIEW)
                    .replaceColor("#000001", color(getResId(ResType.COLOR, "accent")))
                    .removeMultiNewlines().replaceBulletCharacter("*").getHtml();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public void setHtmlToTextView(TextView textView, String html) {
        Spanned spanned;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            spanned = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
        } else {
            spanned = Html.fromHtml(html);
        }
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setText(new SpannableString(spanned));
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
    public boolean shouldColorOnTopBeLight(int colorOnBottomInt) {
        return 186 > (((0.299 * Color.red(colorOnBottomInt))
                + ((0.587 * Color.green(colorOnBottomInt))
                + (0.114 * Color.blue(colorOnBottomInt)))));
    }


    public float px2dp(final float px) {
        return px / _context.getResources().getDisplayMetrics().density;
    }

    public float dp2px(final float dp) {
        return dp * _context.getResources().getDisplayMetrics().density;
    }
}
