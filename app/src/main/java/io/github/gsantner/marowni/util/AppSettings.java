package io.github.gsantner.marowni.util;

import android.content.Context;
import android.os.Environment;

import java.io.File;

import io.github.gsantner.marowni.App;
import io.github.gsantner.marowni.R;
import io.github.gsantner.opoc.util.AppSettingsBase;

public class AppSettings extends AppSettingsBase {
    private AppSettings(Context _context) {
        super(_context);
    }

    public static AppSettings get() {
        return new AppSettings(App.get());
    }

    public boolean isDarkThemeEnabled() {
        return getString(R.string.pref_key__app_theme, "").equals(rstr(R.string.theme_dark));
    }

    public int getBackgroundColor() {
        return isDarkThemeEnabled() ? rcolor(R.color.dark__background) : rcolor(R.color.light__background);
    }

    public void setLockType(String value) {
        setString(R.string.pref_key__lock_type, value);
    }

    public void setLockAuthPinOrPassword(String value) {
        setString(R.string.pref_key__user_pin, value);
    }

    public void setLockType(int value) {
        if (value >= 0 && value < 3) {
            setString(R.string.pref_key__lock_type, Integer.toString(value));
        }
    }

    public int getLockType() {
        return getIntOfStringPref(R.string.pref_key__lock_type, 0);
    }

    public boolean isRememberLastDirectory() {
        return getBool(R.string.pref_key__remember_last_opened_directory, false);
    }

    public boolean isPreviewFirst() {
        return getBool(R.string.pref_key__is_preview_first, false);
    }

    public void setSaveDirectory(String value) {
        setString(R.string.pref_key__save_directory, value);
    }

    public String getSaveDirectory() {
        String dir = getString(R.string.pref_key__save_directory, "");
        if (dir.isEmpty()) {
            dir = new File(new File(Environment.getExternalStorageDirectory(), "/Documents")
                    , rstr(R.string.app_name).toLowerCase())
                    .getAbsolutePath();
            setSaveDirectory(dir);
        }
        return dir;
    }

    public String getFontFamily() {
        return getString(R.string.pref_key__font_family, "sans-serif-light");
    }

    public int getFontSize() {
        return getIntOfStringPref(R.string.pref_key__font_size, 21);
    }

    public boolean isShowMarkdownShortcuts() {
        return getBool(R.string.pref_key__is_show_markdown_shortcuts, false);
    }

    public boolean isHighlightingEnabled() {
        return getBool(R.string.pref_key__is_highlighting_activated, true);
    }

    public int getHighlightingDelay() {
        return getIntOfStringPref(R.string.pref_key__highlighting_delay, 500);
    }

    public boolean isSmartShortcutsEnabled() {
        return getBool(R.string.pref_key__is_smart_shortcuts_enabled, false);
    }

    public String getLastOpenedDirectory() {
        return getString(R.string.pref_key__last_opened_directory, getSaveDirectory());
    }

    public void setLastOpenedDirectory(String value){
        setString(R.string.pref_key__last_opened_directory, value);
    }

    public boolean isRenderRtl() {
        return getBool(R.string.pref_key__is_render_rtl, false);
    }
}
