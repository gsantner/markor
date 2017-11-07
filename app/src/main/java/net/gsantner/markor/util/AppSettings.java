/*
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.util;

import android.content.Context;
import android.os.Environment;

import net.gsantner.markor.App;
import net.gsantner.markor.R;
import net.gsantner.markor.activity.FilesystemListFragment;
import net.gsantner.opoc.util.AppSettingsBase;

import java.io.File;
import java.util.Locale;

@SuppressWarnings("SameParameterValue")
public class AppSettings extends AppSettingsBase {
    private AppSettings(Context _context) {
        super(_context);
    }

    public static AppSettings get() {
        return new AppSettings(App.get());
    }

    public boolean isDarkThemeEnabled() {
        return getString(R.string.pref_key__app_theme, "").equals(rstr(R.string.app_theme_dark));
    }

    public int getBackgroundColor() {
        return isDarkThemeEnabled() ? rcolor(R.color.dark__background_2) : rcolor(R.color.light__background_2);
    }

    public boolean isRememberLastDirectory() {
        return getBool(R.string.pref_key__remember_last_opened_directory, true);
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
                    , rstr(R.string.app_name).toLowerCase(Locale.ROOT))
                    .getAbsolutePath();
            setSaveDirectory(dir);
        }
        return dir;
    }

    public File getQuickNote() {
        return new File(getSaveDirectory(), "QuickNote.md");
    }

    public String getFontFamily() {
        return getString(R.string.pref_key__font_family, rstr(R.string.default_font_family));
    }

    public int getFontSize() {
        return getIntOfStringPref(R.string.pref_key__font_size, 18);
    }

    public boolean isShowMarkdownShortcuts() {
        return getBool(R.string.pref_key__is_show_markdown_shortcuts, false);
    }

    public boolean isHighlightingEnabled() {
        return getBool(R.string.pref_key__is_highlighting_activated, true);
    }

    public int getHighlightingDelay() {
        return getIntOfStringPref(R.string.pref_key__highlighting_delay, 110);
    }

    public boolean isSmartShortcutsEnabled() {
        return getBool(R.string.pref_key__is_smart_shortcuts_enabled, false);
    }

    public String getLastOpenedDirectory() {
        return getString(R.string.pref_key__last_opened_directory, getSaveDirectory());
    }

    public void setLastOpenedDirectory(String value) {
        setString(R.string.pref_key__last_opened_directory, value);
    }

    public boolean isRenderRtl() {
        return getBool(R.string.pref_key__is_render_rtl, false);
    }

    public boolean isEditorStatusBarHidden() {
        return getBool(R.string.pref_key__is_editor_statusbar_hidden, false);
    }

    public boolean isOverviewStatusBarHidden() {
        return getBool(R.string.pref_key__is_overview_statusbar_hidden, false);
    }

    public String getLanguage() {
        return getString(R.string.pref_key__language, "");
    }

    public void setRecreateMainRequired(boolean value) {
        setBool(R.string.pref_key__is_main_recreate_required, value);
    }

    public boolean isRecreateMainRequired() {
        boolean ret = getBool(R.string.pref_key__is_main_recreate_required, false);
        setBool(R.string.pref_key__is_main_recreate_required, false);
        return ret;
    }

    public void setSortMethod(int value) {
        setInt(R.string.pref_key__sort_method, value);
    }


    public int getSortMethod() {
        return getInt(R.string.pref_key__sort_method, FilesystemListFragment.SORT_BY_NAME);
    }

    public void setSortReverse(boolean value) {
        setBool(R.string.pref_key__sort_reverse, value);
    }

    public boolean isSortReverse() {
        return getBool(R.string.pref_key__sort_reverse, false);
    }
}
