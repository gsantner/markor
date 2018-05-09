/*
 * Copyright (c) 2017-2018 Gregor Santner
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
import net.gsantner.opoc.preference.SharedPreferencesPropertyBackend;

import java.io.File;
import java.util.Locale;

@SuppressWarnings("SameParameterValue")
public class AppSettings extends SharedPreferencesPropertyBackend {
    private final String _themeDarkResStr;
    private final String _themeLightResStr;

    public AppSettings(Context _context) {
        super(_context);
        _themeDarkResStr = rstr(R.string.app_theme_dark);
        _themeLightResStr = rstr(R.string.app_theme_light);
    }

    public static AppSettings get() {
        return new AppSettings(App.get());
    }

    public boolean isDarkThemeEnabled() {
        return getString(R.string.pref_key__app_theme, _themeLightResStr).equals(_themeDarkResStr);
    }

    public int getBackgroundColor() {
        return isDarkThemeEnabled() ? rcolor(R.color.dark__background) : rcolor(R.color.light__background);
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
        return new File(getString(R.string.pref_key__quicknote_filepath, defaultValue));
    }

    public void setQuickNoteFile(File file) {
        setString(R.string.pref_key__quicknote_filepath, file.getAbsolutePath());
    }

    public File getTodoFile() {
        String defaultValue = new File(getNotebookDirectoryAsStr(), rstr(R.string.todo_default_filename)).getAbsolutePath();
        return new File(getString(R.string.pref_key__todo_filepath, defaultValue));
    }

    public void setTodoFile(File file) {
        setString(R.string.pref_key__todo_filepath, file.getAbsolutePath());
    }

    public File getLinkBoxFile() {
        String defaultValue = new File(getNotebookDirectoryAsStr(), rstr(R.string.linkbox_default_filename)).getAbsolutePath();
        return new File(getString(R.string.pref_key__linkbox_filepath, defaultValue));
    }

    public void setLinkBoxFile(File file) {
        setString(R.string.pref_key__linkbox_filepath, file.getAbsolutePath());
    }

    public String getFontFamily() {
        return getString(R.string.pref_key__font_family, rstr(R.string.default_font_family));
    }

    public int getFontSize() {
        return getInt(R.string.pref_key__editor_font_size, 18);
    }

    public boolean isEditor_ShowTextmoduleBar() {
        return getBool(R.string.pref_key__is_show_textmodules_bar, true);
    }

    public boolean isHighlightingEnabled() {
        return getBool(R.string.pref_key__is_highlighting_activated, true);
    }

    public int getMarkdownHighlightingDelay() {
        return getInt(R.string.pref_key__markdown__hl_delay, 270);
    }


    public boolean isMarkdownHighlightLineEnding() {
        return getBool(R.string.pref_key__markdown__highlight_lineending_two_or_more_space, false);
    }

    public boolean isMarkdownHighlightCodeFontMonospaceAllowed() {
        return getBool(R.string.pref_key__markdown__monospace_some_parts, false);
    }


    public int getHighlightingDelayTodoTxt() {
        return getInt(R.string.pref_key__todotxt__hl_delay, 870);
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
        return getBool(R.string.pref_key__is_highlighting_for_hexcolor_activated, false);
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

    public float getEditorLineSpacing() {
        return getInt(R.string.pref_key__editor_line_spacing, 90) / 100f;
    }

    public void setLastTodoUsedArchiveFilename(String value) {
        setString(R.string.pref_key__todotxt__last_used_archive_filename, value);
    }

    public String getLastTodoUsedArchiveFilename() {
        return getString(R.string.pref_key__todotxt__last_used_archive_filename, "todo.archive.txt");
    }

    public boolean isEditorStartOnBotttom() {
        return getBool(R.string.pref_key__editor_start_editing_on_bottom, true);
    }

    public boolean isEditorStartEditingInCenter() {
        return getBool(R.string.pref_key__editor_start_editing_in_center, false);
    }

    public int getEditorTextmoduleBarItemPadding() {
        return getInt(R.string.pref_key__editor_textmodule_bar_item_padding, 8);
    }

    public boolean isDisableSpellingRedUnderline() {
        return getBool(R.string.pref_key__editor_disable_spelling_red_underline, true);
    }
}
