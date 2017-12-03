/*
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.util;

import android.content.Context;
import android.os.Environment;

import net.gsantner.markor.App;
import net.gsantner.markor.BuildConfig;
import net.gsantner.markor.R;
import net.gsantner.markor.activity.FilesystemListFragment;
import net.gsantner.opoc.util.AppSettingsBase;

import java.io.File;
import java.util.Locale;

@SuppressWarnings("SameParameterValue")
public class AppSettings extends AppSettingsBase {
    public AppSettings(Context _context) {
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

    public boolean isLoadLastDirectoryAtStartup() {
        return getBool(R.string.pref_key__load_last_directory_at_startup, false);
    }

    public boolean isPreviewFirst() {
        return getBool(R.string.pref_key__is_preview_first, false);
    }

    public void setSaveDirectory(String value) {
        setString(R.string.pref_key__notebook_directory, value);
    }


    public File getNotebookDirectory() {
        return new File(getNotebookDirectoryAsStr());
    }

    public String getNotebookDirectoryAsStr() {
        String dir = getString(R.string.pref_key__notebook_directory, "");
        if (dir.isEmpty()) {
            dir = new File(new File(Environment.getExternalStorageDirectory(), "/Documents")
                    , rstr(R.string.app_name).toLowerCase(Locale.ROOT))
                    .getAbsolutePath();
            setSaveDirectory(dir);
        }
        return dir;
    }

    public File getQuickNoteFile() {
        String defaultValue = new File(getNotebookDirectoryAsStr(), rstr(R.string.quicknote_default_filename)).getAbsolutePath();
        return new File(getString(R.string.pref_key__markdown__quicknote_filepath, defaultValue));
    }

    public void setQuickNoteFile(File file) {
        setString(R.string.pref_key__markdown__quicknote_filepath, file.getAbsolutePath());
    }

    public File getTodoTxtFile() {
        String defaultValue = new File(getNotebookDirectoryAsStr(), rstr(R.string.todo_default_filename)).getAbsolutePath();
        return new File(getString(R.string.pref_key__todotxt_filepath, defaultValue));
    }

    public void setTodoFile(File file) {
        setString(R.string.pref_key__todotxt_filepath, file.getAbsolutePath());
    }

    public String getFontFamily() {
        return getString(R.string.pref_key__font_family, rstr(R.string.default_font_family));
    }

    public int getFontSize() {
        return getIntOfStringPref(R.string.pref_key__font_size, 18);
    }

    public boolean isEditor_ShowTextmoduleBar() {
        return getBool(R.string.pref_key__is_show_textmodules_bar, true);
    }

    public boolean isHighlightingEnabled() {
        return getBool(R.string.pref_key__is_highlighting_activated, true);
    }

    public int getMarkdownHighlightingDelay() {
        return getIntOfStringPref(R.string.pref_key__markdown__highlighting_delay, 110);
    }

    public int getHighlightingDelayTodoTxt() {
        return getIntOfStringPref(R.string.pref_key__todotxt__highlighting_delay, 190);
    }

    public String getLastOpenedDirectory() {
        return getString(R.string.pref_key__last_opened_directory, getNotebookDirectoryAsStr());
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

    public boolean isShowSettingsOptionInMainToolbar() {
        return getBool(R.string.pref_key__show_settings_option_in_main_toolbar, true);
    }

    public boolean isHighlightingHexColorEnabled() {
        return getBool(R.string.pref_key__is_highlighting_for_hexcolor_activated, true);
    }

    public boolean isTodoAppendProConOnEndEnabled() {
        return getBool(R.string.pref_key__todotxt__append_contexts_and_projects_on_end_of_task, true);
    }

    public boolean isTodoStartTasksWithTodaysDateEnabled() {
        return getBool(R.string.pref_key__todotxt__start_new_tasks_with_todays_date, true);
    }

    public boolean isAppCurrentVersionFirstStart(boolean doSet) {
        int value = getInt(R.string.pref_key__app_first_start_current_version, -1);
        if (doSet) {
            setInt(R.string.pref_key__app_first_start_current_version, BuildConfig.VERSION_CODE);
        }
        return value != BuildConfig.VERSION_CODE;
    }
}
