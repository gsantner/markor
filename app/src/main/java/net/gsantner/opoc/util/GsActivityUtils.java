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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Build;
import android.provider.CalendarContract;
import android.text.Html;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

import net.gsantner.opoc.model.GsSharedPreferencesPropertyBackend;

import java.util.List;


@SuppressWarnings({"WeakerAccess", "unused", "SameParameterValue", "SpellCheckingInspection", "rawtypes", "UnusedReturnValue"})
public class GsActivityUtils extends GsShareUtil {
    //########################
    //## Members, Constructors
    //########################
    protected Activity _activity;

    public GsActivityUtils(final Activity activity) {
        super(activity);
        _activity = activity;
    }

    @Override
    public void freeContextRef() {
        super.freeContextRef();
        _activity = null;
    }

    //########################
    //##     Methods
    //########################

    /**
     * Animate to specified Activity
     *
     * @param to                 The class of the activity
     * @param finishFromActivity true: Finish the current activity
     * @param requestCode        Request code for stating the activity, not waiting for result if null
     */
    public GsActivityUtils animateToActivity(Class to, Boolean finishFromActivity, Integer requestCode) {
        return animateToActivity(new Intent(_activity, to), finishFromActivity, requestCode);
    }

    /**
     * Animate to Activity specified in intent
     * Requires animation resources
     *
     * @param intent             Intent to open start an activity
     * @param finishFromActivity true: Finish the current activity
     * @param requestCode        Request code for stating the activity, not waiting for result if null
     */
    public GsActivityUtils animateToActivity(Intent intent, Boolean finishFromActivity, Integer requestCode) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        if (requestCode != null) {
            _activity.startActivityForResult(intent, requestCode);
        } else {
            _activity.startActivity(intent);

        }
        _activity.overridePendingTransition(getResId(ResType.DIMEN, "fadein"), getResId(ResType.DIMEN, "fadeout"));
        if (finishFromActivity != null && finishFromActivity) {
            _activity.finish();
        }
        return this;
    }


    public Snackbar showSnackBar(@StringRes int stringResId, boolean showLong) {
        Snackbar s = Snackbar.make(_activity.findViewById(android.R.id.content), stringResId,
                showLong ? Snackbar.LENGTH_LONG : Snackbar.LENGTH_SHORT);
        s.show();
        return s;
    }

    public void showSnackBar(@StringRes int stringResId, boolean showLong, @StringRes int actionResId, View.OnClickListener listener) {
        Snackbar.make(_activity.findViewById(android.R.id.content), stringResId,
                        showLong ? Snackbar.LENGTH_LONG : Snackbar.LENGTH_SHORT)
                .setAction(actionResId, listener)
                .show();
    }

    public GsActivityUtils setSoftKeyboardVisibile(boolean visible, View... editView) {
        final Activity activity = _activity;
        if (activity != null) {
            final View v = (editView != null && editView.length > 0) ? (editView[0]) : (activity.getCurrentFocus() != null && activity.getCurrentFocus().getWindowToken() != null ? activity.getCurrentFocus() : null);
            final InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
            if (v != null && imm != null) {
                Runnable r = () -> {
                    if (visible) {
                        v.requestFocus();
                        imm.showSoftInput(v, InputMethodManager.SHOW_FORCED);
                    } else {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                };
                r.run();
                for (int d : new int[]{100, 350}) {
                    v.postDelayed(r, d);
                }
            }
        }
        return this;
    }

    public GsActivityUtils hideSoftKeyboard() {
        if (_activity != null) {
            InputMethodManager imm = (InputMethodManager) _activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
            if (imm != null && _activity.getCurrentFocus() != null && _activity.getCurrentFocus().getWindowToken() != null) {
                imm.hideSoftInputFromWindow(_activity.getCurrentFocus().getWindowToken(), 0);
            }
        }
        return this;
    }

    public GsActivityUtils showSoftKeyboard() {
        if (_activity != null) {
            InputMethodManager imm = (InputMethodManager) _activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
            if (imm != null && _activity.getCurrentFocus() != null && _activity.getCurrentFocus().getWindowToken() != null) {
                showSoftKeyboard(_activity.getCurrentFocus());
            }
        }
        return this;
    }


    public GsActivityUtils showSoftKeyboard(View textInputView) {
        if (_activity != null) {
            InputMethodManager imm = (InputMethodManager) _activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
            if (imm != null && textInputView != null) {
                imm.showSoftInput(textInputView, InputMethodManager.SHOW_FORCED);
            }
        }
        return this;
    }

    public void showDialogWithHtmlTextView(@StringRes int resTitleId, String html) {
        showDialogWithHtmlTextView(resTitleId, html, true, null);
    }

    public void showDialogWithHtmlTextView(@StringRes int resTitleId, String text, boolean isHtml, DialogInterface.OnDismissListener dismissedListener) {
        ScrollView scroll = new ScrollView(_context);
        AppCompatTextView textView = new AppCompatTextView(_context);
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, _context.getResources().getDisplayMetrics());

        scroll.setPadding(padding, 0, padding, 0);
        scroll.addView(textView);
        textView.setMovementMethod(new LinkMovementMethod());
        textView.setText(isHtml ? new SpannableString(Html.fromHtml(text)) : text);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);

        AlertDialog.Builder dialog = new AlertDialog.Builder(_context)
                .setPositiveButton(android.R.string.ok, null).setOnDismissListener(dismissedListener)
                .setView(scroll);
        if (resTitleId != 0) {
            dialog.setTitle(resTitleId);
        }
        dialogFullWidth(dialog.show(), true, false);
    }

    public void showDialogWithRawFileInWebView(String fileInRaw, @StringRes int resTitleId) {
        WebView wv = new WebView(_context);
        wv.loadUrl("file:///android_res/raw/" + fileInRaw);
        AlertDialog.Builder dialog = new AlertDialog.Builder(_context)
                .setPositiveButton(android.R.string.ok, null)
                .setTitle(resTitleId)
                .setView(wv);
        dialogFullWidth(dialog.show(), true, false);
    }

    // Toggle with no param, else set visibility according to first bool
    public GsActivityUtils toggleStatusbarVisibility(boolean... optionalForceVisible) {
        WindowManager.LayoutParams attrs = _activity.getWindow().getAttributes();
        int flag = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        if (optionalForceVisible.length == 0) {
            attrs.flags ^= flag;
        } else if (optionalForceVisible.length == 1 && optionalForceVisible[0]) {
            attrs.flags &= ~flag;
        } else {
            attrs.flags |= flag;
        }
        _activity.getWindow().setAttributes(attrs);
        return this;
    }

    public GsActivityUtils showGooglePlayEntryForThisApp() {
        String pkgId = "details?id=" + _activity.getPackageName();
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, Uri.parse("market://" + pkgId));
        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                (Build.VERSION.SDK_INT >= 21 ? Intent.FLAG_ACTIVITY_NEW_DOCUMENT : Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET) |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        try {
            _activity.startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            _activity.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/" + pkgId)));
        }
        return this;
    }

    public GsActivityUtils setStatusbarColor(int color, boolean... fromRes) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (fromRes != null && fromRes.length > 0 && fromRes[0]) {
                color = ContextCompat.getColor(_context, color);
            }

            _activity.getWindow().setStatusBarColor(color);
        }
        return this;
    }

    public boolean isLauncherEnabled(Class activityClass) {
        try {
            ComponentName component = new ComponentName(_context, activityClass);
            return _context.getPackageManager().getComponentEnabledSetting(component) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        } catch (Exception ignored) {
        }
        return false;
    }

    @ColorInt
    public Integer getCurrentPrimaryColor() {
        TypedValue typedValue = new TypedValue();
        _context.getTheme().resolveAttribute(getResId(ResType.ATTR, "colorPrimary"), typedValue, true);
        return typedValue.data;
    }

    @ColorInt
    public Integer getCurrentPrimaryDarkColor() {
        TypedValue typedValue = new TypedValue();
        _context.getTheme().resolveAttribute(getResId(ResType.ATTR, "colorPrimaryDark"), typedValue, true);
        return typedValue.data;
    }

    @ColorInt
    public Integer getCurrentAccentColor() {
        TypedValue typedValue = new TypedValue();
        _context.getTheme().resolveAttribute(getResId(ResType.ATTR, "colorAccent"), typedValue, true);
        return typedValue.data;
    }

    @ColorInt
    public Integer getActivityBackgroundColor() {
        TypedArray array = _activity.getTheme().obtainStyledAttributes(new int[]{
                android.R.attr.colorBackground,
        });
        int c = array.getColor(0, 0xFF0000);
        array.recycle();
        return c;
    }

    public GsActivityUtils setActivityBackgroundColor(@ColorInt Integer color) {
        if (color != null) {
            try {
                ((ViewGroup) _activity.findViewById(android.R.id.content)).getChildAt(0).setBackgroundColor(color);
            } catch (Exception ignored) {
            }
        }
        return this;
    }

    public GsActivityUtils setActivityNavigationBarBackgroundColor(@ColorInt Integer color) {
        if (color != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    _activity.getWindow().setNavigationBarColor(color);
                }
            } catch (Exception ignored) {
            }
        }
        return this;
    }

    public GsActivityUtils startCalendarApp() {
        Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();
        builder.appendPath("time");
        builder.appendPath(Long.toString(System.currentTimeMillis()));
        Intent intent = new Intent(Intent.ACTION_VIEW, builder.build());
        _activity.startActivity(intent);
        return this;
    }

    /**
     * Detect if the activity is currently in splitscreen/multiwindow mode
     */
    public boolean isInSplitScreenMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return _activity.isInMultiWindowMode();
        }
        return false;
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

    // Make activity/app not show up in the recents history - call before finish / System.exit
    public GsActivityUtils removeActivityFromHistory() {
        try {
            ActivityManager am = (ActivityManager) _activity.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                List<ActivityManager.AppTask> tasks = am.getAppTasks();
                if (tasks != null && !tasks.isEmpty()) {
                    tasks.get(0).setExcludeFromRecents(true);
                }
            }

        } catch (Exception ignored) {
        }
        return this;
    }

    /**
     * Set Android day-night theme
     *
     * @param pref one out of system (daynight toggle), auto (daynight hour), autocompat (hour 5-17), light (fixed), dark (fixed)
     */
    @SuppressLint("WrongConstant")
    public static void applyDayNightTheme(final String pref) {
        final boolean prefLight = pref.contains("light") || ("autocompat".equals(pref) && GsSharedPreferencesPropertyBackend.isCurrentHourOfDayBetween(9, 17));
        final boolean prefDark = pref.contains("dark") || ("autocompat".equals(pref) && !GsSharedPreferencesPropertyBackend.isCurrentHourOfDayBetween(9, 17));

        if (prefLight) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (prefDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else if ("system".equals(pref)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        } else if ("auto".equals(pref)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
        }
    }


    public void nextScreenRotationSetting() {
        String text = "";
        int nextOrientation;
        switch (_activity.getRequestedOrientation()) {
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
        int resId = getResId(ResType.STRING, text);
        text = (resId != 0 ? _context.getString(resId) : text);
        Toast.makeText(_context, text, Toast.LENGTH_SHORT).show();
        _activity.setRequestedOrientation(nextOrientation);
    }
}
