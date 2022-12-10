/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Environment;
import android.util.Pair;

import androidx.annotation.ColorRes;
import androidx.annotation.IdRes;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;

import net.gsantner.markor.BuildConfig;
import net.gsantner.markor.R;
import net.gsantner.markor.format.FormatRegistry;
import net.gsantner.markor.util.MarkorContextUtils;
import net.gsantner.markor.util.ShortcutUtils;
import net.gsantner.opoc.format.GsTextUtils;
import net.gsantner.opoc.frontend.filebrowser.GsFileBrowserListAdapter;
import net.gsantner.opoc.model.GsSharedPreferencesPropertyBackend;
import net.gsantner.opoc.util.GsContextUtils;
import net.gsantner.opoc.util.GsFileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import other.de.stanetz.jpencconverter.PasswordStore;

@SuppressWarnings({"SameParameterValue", "WeakerAccess", "FieldCanBeLocal"})
public class AppSettings extends GsSharedPreferencesPropertyBackend {
    private SharedPreferences _prefCache;
    private SharedPreferences _prefHistory;
    public static Boolean _isDeviceGoodHardware = null;
    private MarkorContextUtils _cu;

    private static final File LOCAL_TESTFOLDER_FILEPATH = new File("/storage/emulated/0/00_sync/documents/special");

    @Override
    public AppSettings init(final Context context) {
        super.init(context);
        _prefCache = context.getSharedPreferences("cache", Context.MODE_PRIVATE);
        _prefHistory = context.getSharedPreferences("history", Context.MODE_PRIVATE);
        _cu = new MarkorContextUtils(context);
        _isDeviceGoodHardware = _cu.isDeviceGoodHardware(context);

        if (getInt(R.string.pref_key__basic_color_scheme__bg_light, -999) == -999) {
            setEditorBasicColor(true, R.color.white, R.color.dark_grey);
            setEditorBasicColor(false, R.color.dark_grey, R.color.light__background);
        }
        return this;
    }

    public boolean isLoadLastDirectoryAtStartup() {
        return getBool(R.string.pref_key__load_last_directory_at_startup, false);
    }

    public boolean isPreferViewMode() {
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
        if (dir.isEmpty() && LOCAL_TESTFOLDER_FILEPATH.exists() && !BuildConfig.IS_TEST_BUILD) {
            dir = LOCAL_TESTFOLDER_FILEPATH.getParentFile().getParent();
            setSaveDirectory(dir);
        }
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
        if (LOCAL_TESTFOLDER_FILEPATH.exists() && !BuildConfig.IS_TEST_BUILD) {
            defaultValue = new File(LOCAL_TESTFOLDER_FILEPATH, rstr(R.string.quicknote_default_filename)).getAbsolutePath();
        }
        return new File(getString(R.string.pref_key__quicknote_filepath, defaultValue));
    }

    public void setQuickNoteFile(final File file) {
        setString(R.string.pref_key__quicknote_filepath, file.getAbsolutePath());
    }

    public File getTodoFile() {
        String defaultValue = new File(getNotebookDirectoryAsStr(), rstr(R.string.todo_default_filename)).getAbsolutePath();
        if (LOCAL_TESTFOLDER_FILEPATH.exists() && !BuildConfig.IS_TEST_BUILD) {
            defaultValue = new File(LOCAL_TESTFOLDER_FILEPATH, rstr(R.string.todo_default_filename)).getAbsolutePath();
        }
        return new File(getString(R.string.pref_key__todo_filepath, defaultValue));
    }

    public void setTodoFile(File file) {
        setString(R.string.pref_key__todo_filepath, file.getAbsolutePath());
    }

    public File getSnippetsFolder() {
        return new File(getNotebookDirectory(), ".app/snippets");
    }

    public String getFontFamily() {
        return getString(R.string.pref_key__font_family, rstr(R.string.default_font_family));
    }

    public int getFontSize() {
        return getInt(R.string.pref_key__editor_font_size, 15);
    }

    public int getViewFontSize() {
        int size = getInt(R.string.pref_key__view_font_size, -1);
        return size < 2 ? getFontSize() : size;
    }

    public boolean isHighlightingEnabled() {
        return getBool(R.string.pref_key__is_highlighting_activated, true);
    }

    public boolean isDynamicHighlightingEnabled() {
        return getBool(R.string.pref_key__is_dynamic_highlighting_activated, true);
    }

    public int getMarkdownHighlightingDelay() {
        return getInt(R.string.pref_key__markdown__hl_delay_v2, 650);
    }

    public int getAsciidocHighlightingDelay() {
        return getInt(R.string.pref_key__asciidoc__hl_delay, 650);
    }

    public boolean isMarkdownHighlightLineEnding() {
        return getBool(R.string.pref_key__markdown__highlight_lineending_two_or_more_space, false);
    }

    public boolean isAsciidocHighlightLineEnding() {
        return getBool(R.string.pref_key__asciidoc__highlight_lineending_hard_line_break, false);
    }

    public boolean isHighlightCodeMonospaceFont() {
        return getBool(R.string.pref_key__highlight_code_monospace_font, false);
    }

    public boolean isHighlightCodeBlock() {
        return !getBool(R.string.pref_key__highlight_code_block_disabled, false);
    }

    public int getHighlightingDelayTodoTxt() {
        return getInt(R.string.pref_key__todotxt__hl_delay, 870);
    }

    public boolean isRenderRtl() {
        return getBool(R.string.pref_key__is_render_rtl, false);
    }

    public boolean isMarkdownMathEnabled() {
        return getBool(R.string.pref_key__markdown_render_math, false);
    }

    public List<String> getMarkdownShownYamlFrontMatterKeys() {
        String pref = getString(R.string.pref_key__markdown_always_shown_yaml_front_matter_keys, "title,tags,date");
        List<String> keys = new ArrayList<>(Arrays.asList(pref.replace(" ", "").split(",\\s*")));
        keys.removeAll(Arrays.asList("", null));
        return keys;
    }

    public boolean isMarkdownNewlineNewparagraphEnabled() {
        return getBool(R.string.pref_key__markdown_newline_newparagraph, false);
    }

    public boolean isMarkdownTableOfContentsEnabled() {
        return getMarkdownTableOfContentLevels().length > 0;
    }

    public int[] getMarkdownTableOfContentLevels() {
        final List<String> v = getStringSet(R.string.pref_key__markdown_table_of_contents_enabled_levels, Collections.emptyList());
        int[] ret = new int[v.size()];
        for (int i = 0; i < v.size(); i++) {
            ret[i] = Integer.parseInt(v.get(i));
        }
        return ret;
    }

    public boolean isHideSystemStatusbar() {
        return getBool(R.string.pref_key__theming_hide_system_statusbar, false);
    }

    public boolean isSpecialFileLaunchersEnabled() {
        if (BuildConfig.IS_TEST_BUILD) {
            return false;
        }
        return getBool(R.string.pref_key__is_launcher_for_special_files_enabled, false);
    }

    public boolean isKeepScreenOn() {
        return getBool(R.string.pref_key__is_keep_screen_on, !BuildConfig.IS_TEST_BUILD);
    }

    public boolean isDisallowScreenshots() {
        return getBool(R.string.pref_key__is_disallow_screenshots, false);
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

    public String setFileBrowserSortByType(String v) {
        setString(R.string.pref_key__file_browser__sort_by_type, v);
        return v;
    }

    public String getFileBrowserSortByType() {
        return getString(R.string.pref_key__file_browser__sort_by_type, GsFileUtils.SORT_BY_NAME);
    }

    public boolean setFileBrowserSortReverse(boolean value) {
        setBool(R.string.pref_key__sort_reverse, value);
        return value;
    }

    public boolean isFileBrowserSortReverse() {
        return getBool(R.string.pref_key__sort_reverse, false);
    }

    public boolean setFileBrowserFilterShowDotFiles(boolean v) {
        setBool(R.string.pref_key__show_dot_files_v2, v);
        return v;
    }

    public boolean isFileBrowserFilterShowDotFiles() {
        return getBool(R.string.pref_key__show_dot_files_v2, true);
    }

    public boolean isShowSettingsOptionInMainToolbar() {
        return true;//getBool(R.string.pref_key__show_settings_option_in_main_toolbar, true);
    }

    public boolean isHighlightingHexColorEnabled() {
        return getBool(R.string.pref_key__is_highlighting_for_hexcolor_activated, false);
    }

    public boolean isTodoAppendProConOnEndEnabled() {
        return getBool(R.string.pref_key__todotxt__append_contexts_and_projects_on_end_of_task, true);
    }

    public boolean isTodoAddCompletionDateEnabled() {
        return getBool(R.string.pref_key__todotxt__add_completion_date_for_todos, true);
    }

    public boolean isAppCurrentVersionFirstStart(boolean doSet) {
        int value = getInt(R.string.pref_key__app_first_start_current_version, -1);
        if (doSet) {
            setInt(R.string.pref_key__app_first_start_current_version, BuildConfig.VERSION_CODE);
        }
        return value != BuildConfig.VERSION_CODE;
    }

    public float getEditorLineSpacing() {
        return getInt(R.string.pref_key__editor_line_spacing, 100) / 100f;
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

    public int getEditorActionButtonItemPadding() {
        return getInt(R.string.pref_key__editor_textaction_bar_item_padding, 8);
    }

    public boolean isDisableSpellingRedUnderline() {
        return getBool(R.string.pref_key__editor_disable_spelling_red_underline, true);
    }

    public void addRecentDocument(File file) {
        if (!listFileInRecents(file)) {
            return;
        }
        if (!file.equals(getTodoFile()) && !file.equals(getQuickNoteFile())) {
            ArrayList<String> recent = getRecentDocuments();
            recent.add(0, file.getAbsolutePath());
            recent.remove(getTodoFile().getAbsolutePath());
            recent.remove(getQuickNoteFile().getAbsolutePath());
            recent.remove("");
            recent.remove(null);

            setInt(file.getAbsolutePath(), getInt(file.getAbsolutePath(), 0, _prefCache) + 1, _prefCache);
            setRecentDocuments(recent);
        }
        ShortcutUtils.setShortcuts(_context);
    }

    public void toggleFavouriteFile(File file) {
        List<String> list = new ArrayList<>();
        List<File> favourites = getFavouriteFiles();
        for (File f : favourites) {
            if (f != null && (f.exists() || GsFileBrowserListAdapter.isVirtualStorage(f))) {
                list.add(f.getAbsolutePath());
            }
        }
        String abs = file.getAbsolutePath();
        if (list.contains(abs)) {
            list.remove(abs);
        } else {
            list.add(abs);
        }
        setStringList(R.string.pref_key__favourite_files, list);
    }

    private static final String PREF_PREFIX_EDIT_POS_CHAR = "PREF_PREFIX_EDIT_POS_CHAR";
    private static final String PREF_PREFIX_WRAP_STATE = "PREF_PREFIX_WRAP_STATE";
    private static final String PREF_PREFIX_HIGHLIGHT_STATE = "PREF_PREFIX_HIGHLIGHT_STATE";
    private static final String PREF_PREFIX_PREVIEW_STATE = "PREF_PREFIX_PREVIEW_STATE";
    private static final String PREF_PREFIX_INDENT_SIZE = "PREF_PREFIX_INDENT_SIZE";
    private static final String PREF_PREFIX_FONT_SIZE = "PREF_PREFIX_FONT_SIZE";
    private static final String PREF_PREFIX_FILE_FORMAT = "PREF_PREFIX_FILE_FORMAT";
    private static final String PREF_PREFIX_AUTO_FORMAT = "PREF_PREFIX_AUTO_FORMAT";
    private static final String PREF_PREFIX_VIEW_SCROLL_X = "PREF_PREFIX_VIEW_SCROLL_X";
    private static final String PREF_PREFIX_VIEW_SCROLL_Y = "PREF_PREFIX_VIEW_SCROLL_Y";
    private static final String PREF_PREFIX_TODO_DONE_NAME = "PREF_PREFIX_TODO_DONE_NAME";

    public void setLastTodoDoneName(final String path, final String name) {
        if (fexists(path)) {
            setString(PREF_PREFIX_TODO_DONE_NAME + path, name);
        }
    }

    public String getLastTodoDoneName(final String path) {
        final String def = getLastTodoUsedArchiveFilename();
        if (!fexists(path)) {
            return def;
        } else {
            return getString(PREF_PREFIX_TODO_DONE_NAME + path, def);
        }
    }

    public void setLastEditPosition(final String path, final int pos) {
        if (fexists(path)) {
            setInt(PREF_PREFIX_EDIT_POS_CHAR + path, pos);
        }
    }

    public int getLastEditPosition(final String path, final int def) {
        if (!fexists(path)) {
            return def;
        } else {
            return getInt(PREF_PREFIX_EDIT_POS_CHAR + path, def);
        }
    }

    public void setLastViewPosition(File file, int scrollX, int scrollY) {
        if (file == null || !file.exists()) {
            return;
        }
        if (!file.equals(getTodoFile()) && !file.equals(getQuickNoteFile())) {
            setInt(PREF_PREFIX_VIEW_SCROLL_X + file.getAbsolutePath(), scrollX, _prefCache);
            setInt(PREF_PREFIX_VIEW_SCROLL_Y + file.getAbsolutePath(), scrollY, _prefCache);
        }
    }

    public void setDocumentWrapState(final String path, final boolean state) {
        if (fexists(path)) {
            setBool(PREF_PREFIX_WRAP_STATE + path, state);
        }
    }

    public boolean getDocumentWrapState(final String path) {
        final boolean _default = isEditorLineBreakingEnabled();
        if (!fexists(path)) {
            return _default;
        } else {
            return getBool(PREF_PREFIX_WRAP_STATE + path, _default);
        }
    }

    public void setDocumentFormat(final String path, @StringRes final int format) {
        if (fexists(path) && format != FormatRegistry.FORMAT_UNKNOWN) {
            setString(PREF_PREFIX_FILE_FORMAT + path, _context.getString(format));
        }
    }

    @StringRes
    public int getDocumentFormat(final String path, final int _default) {
        if (!fexists(path)) {
            return _default;
        } else {
            final String value = getString(PREF_PREFIX_FILE_FORMAT + path, null);
            final int sid = _cu.getResId(_context, GsContextUtils.ResType.STRING, value);
            // Note TextFormat.FORMAT_UNKNOWN also == 0
            return sid != 0 ? sid : _default;
        }
    }

    public void setDocumentAutoFormatEnabled(final String path, final boolean enabled) {
        if (fexists(path)) {
            setBool(PREF_PREFIX_AUTO_FORMAT + path, enabled);
        }
    }

    public boolean getDocumentAutoFormatEnabled(final String path) {
        final boolean _default = true;
        if (!fexists(path)) {
            return _default;
        } else {
            return getBool(PREF_PREFIX_AUTO_FORMAT + path, _default);
        }
    }

    public void setDocumentFontSize(final String path, final int size) {
        if (fexists(path)) {
            setInt(PREF_PREFIX_FONT_SIZE + path, size);
        }
    }

    public int getDocumentFontSize(final String path) {
        final int _default = getFontSize();
        if (!fexists(path)) {
            return _default;
        } else {
            return getInt(PREF_PREFIX_FONT_SIZE + path, _default);
        }
    }

    public void setDocumentIndentSize(final String path, final int size) {
        if (fexists(path)) {
            setInt(PREF_PREFIX_INDENT_SIZE + path, size);
        }
    }

    public int getDocumentIndentSize(final String path) {
        final int _default = 4;
        if (!fexists(path)) {
            return _default;
        } else {
            return getInt(PREF_PREFIX_INDENT_SIZE + path, _default);
        }
    }

    public void setDocumentPreviewState(final String path, final boolean isViewMode) {
        setBool(PREF_PREFIX_PREVIEW_STATE + path, isViewMode);
    }

    public boolean getDocumentPreviewState(final String path) {
        // Use global setting as default
        final boolean _default = isPreferViewMode();
        if (_default || !fexists(path)) {
            return _default;
        } else {
            return getBool(PREF_PREFIX_PREVIEW_STATE + path, _default);
        }
    }

    public void setDocumentHighlightState(final String path, final boolean state) {
        setBool(PREF_PREFIX_HIGHLIGHT_STATE + path, state);
    }

    public boolean getDocumentHighlightState(final String path, final CharSequence chars) {
        final boolean lengthOk = chars != null && chars.length() < (_isDeviceGoodHardware ? 100000 : 35000);
        return getBool(PREF_PREFIX_HIGHLIGHT_STATE + path, lengthOk && isHighlightingEnabled());
    }

    public int getLastViewPositionX(File file) {
        if (file == null || !file.exists()) {
            return -1;
        }
        return getInt(PREF_PREFIX_VIEW_SCROLL_X + file.getAbsolutePath(), -3, _prefCache);
    }

    public int getLastViewPositionY(File file) {
        if (file == null || !file.exists()) {
            return -1;
        }
        return getInt(PREF_PREFIX_VIEW_SCROLL_Y + file.getAbsolutePath(), -3, _prefCache);
    }

    private List<String> getPopularDocumentsSorted() {
        List<String> popular = getRecentDocuments();
        Collections.sort(popular, (o1, o2) -> Integer.compare(getInt(o1, 0, _prefCache), getInt(o2, 0, _prefCache)));
        return popular;
    }

    public List<String> getPopularDocuments() {
        return getStringList(R.string.pref_key__popular_documents);
    }

    public void setPopularDocuments(List<String> v) {
        limitListTo(v, 20, true);
        setStringList(R.string.pref_key__popular_documents, v, _prefApp);
    }

    public void setRecentDocuments(List<String> v) {
        limitListTo(v, 20, true);
        setStringList(R.string.pref_key__recent_documents, v, _prefApp);
        setPopularDocuments(getPopularDocumentsSorted());
    }

    public ArrayList<String> getRecentDocuments() {
        ArrayList<String> list = getStringList(R.string.pref_key__recent_documents);
        for (int i = 0; i < list.size(); i++) {
            if (!new File(list.get(i)).isFile()) {
                list.remove(i);
                i--;
            }
        }
        return list;
    }

    public ArrayList<File> getAsFileList(List<String> list) {
        ArrayList<File> r = new ArrayList<>();
        for (String f : list) {
            r.add(new File(f));
        }
        return r;
    }

    public ArrayList<File> getFavouriteFiles() {
        ArrayList<File> list = new ArrayList<>();
        for (String fp : getStringList(R.string.pref_key__favourite_files)) {
            File f = new File(fp);
            if (f.exists() || GsFileBrowserListAdapter.isVirtualStorage(f)) {
                list.add(f);
            }
        }
        return list;
    }

    public String getInjectedHeader() {
        return getString(R.string.pref_key__inject_to_head, rstr(R.string.inject_to_head_default));
    }

    public String getInjectedBody() {
        return getString(R.string.pref_key__inject_to_body, "");
    }

    public boolean isEditorHistoryEnabled() {
        return true;//getBool(R.string.pref_key__editor_history_enabled3, true);
    }

    public int getEditorForegroundColor() {
        final boolean night = GsContextUtils.instance.isDarkModeEnabled(_context);
        return getInt(night ? R.string.pref_key__basic_color_scheme__fg_dark : R.string.pref_key__basic_color_scheme__fg_light, rcolor(R.color.primary_text));
    }

    public int getEditorBackgroundColor() {
        final boolean night = GsContextUtils.instance.isDarkModeEnabled(_context);
        int c = getInt(night ? R.string.pref_key__basic_color_scheme__bg_dark : R.string.pref_key__basic_color_scheme__bg_light, rcolor(R.color.background));
        if (getAppThemeName().contains("black")) {
            c = Color.BLACK;
        }
        return c;
    }

    public void applyAppTheme() {
        GsContextUtils.instance.applyDayNightTheme(getString(R.string.pref_key__app_theme, getAppThemeName()));
    }

    public String getAppThemeName() {
        return getString(R.string.pref_key__app_theme, _context.getString(R.string.app_theme_system));
    }

    public void setEditorBasicColor(boolean forDarkMode, @ColorRes int fgColor, @ColorRes int bgColor) {
        int resIdFg = forDarkMode ? R.string.pref_key__basic_color_scheme__fg_dark : R.string.pref_key__basic_color_scheme__fg_light;
        int resIdBg = forDarkMode ? R.string.pref_key__basic_color_scheme__bg_dark : R.string.pref_key__basic_color_scheme__bg_light;
        setInt(resIdFg, rcolor(fgColor));
        setInt(resIdBg, rcolor(bgColor));
    }

    public boolean isMultiWindowEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return getBool(R.string.pref_key__is_multi_window_enabled, true);
        } else {
            return false;
        }
    }

    public boolean isSearchQueryCaseSensitive() {
        return getBool(R.string.pref_key__is_search_query_case_sensitive, false);
    }

    public void setSearchQueryCaseSensitivity(final boolean isQuerySensitive) {
        setBool(R.string.pref_key__is_search_query_case_sensitive, isQuerySensitive);
    }

    public boolean isSearchQueryUseRegex() {
        return getBool(R.string.pref_key__is_search_query_use_regex, false);
    }

    public void setSearchQueryRegexUsing(final boolean isUseRegex) {
        setBool(R.string.pref_key__is_search_query_use_regex, isUseRegex);
    }

    public boolean isSearchInContent() {
        return getBool(R.string.pref_key__is_search_in_content, false);
    }

    public void setSearchInContent(final boolean isSearchInContent) {
        setBool(R.string.pref_key__is_search_in_content, isSearchInContent);
    }

    public boolean isOnlyFirstContentMatch() {
        return getBool(R.string.pref_key__is_only_first_content_match, false);
    }

    public void setOnlyFirstContentMatch(final boolean isOnlyFirstContentMatch) {
        setBool(R.string.pref_key__is_only_first_content_match, isOnlyFirstContentMatch);
    }

    public int getSearchMaxDepth() {
        int depth = getIntOfStringPref(R.string.pref_key__max_search_depth, Integer.MAX_VALUE);

        if (depth == 0) {
            return Integer.MAX_VALUE;
        }

        return depth;
    }

    public List<String> getFileSearchIgnorelist() {
        String pref = getString(R.string.pref_key__filesearch_ignorelist, "");
        return Arrays.asList(pref.replace("\r", "").replace("\n\n", "\n").split("\n"));
    }

    public @IdRes
    int getAppStartupTab() {
        int i = getIntOfStringPref(R.string.pref_key__app_start_tab_v2, R.id.nav_notebook);
        switch (i) {
            case 1:
                return R.id.nav_todo;
            case 2:
                return R.id.nav_quicknote;
        }
        return R.id.nav_notebook;
    }

    public boolean isSwipeToChangeMode() {
        return getBool(R.string.pref_key__swipe_to_change_mode, false);
    }

    public boolean setFileBrowserSortFolderFirst(boolean v) {
        setBool(R.string.pref_key__filesystem_folder_first, v);
        return v;
    }


    public boolean isFileBrowserSortFolderFirst() {
        return getBool(R.string.pref_key__filesystem_folder_first, true);
    }

    public String getNavigationBarColor() {
        return getString(R.string.pref_key__navigationbar_color, "#000000");
    }

    public @IdRes
    Integer getAppStartupFolderMenuId() {
        switch (getString(R.string.pref_key__app_start_folder, "notebook")) {
            case "favourites":
                return R.id.action_go_to_favourite_files;
            case "internal_storage":
                return R.id.action_go_to_storage;
            case "appdata_public":
                return R.id.action_go_to_appdata_public;
            case "appdata_private":
                return R.id.action_go_to_appdata_private;
            case "popular_documents":
                return R.id.action_go_to_popular_files;
            case "recently_viewed_documents":
                return R.id.action_go_to_recent_files;
        }
        return R.id.action_go_to_home;
    }

    public File getFolderToLoadByMenuId(int itemId) {
        List<Pair<File, String>> appDataPublicDirs = _cu.getAppDataPublicDirs(_context, false, true, false);
        switch (itemId) {
            case R.id.action_go_to_home: {
                return getNotebookDirectory();
            }
            case R.id.action_go_to_popular_files: {
                return GsFileBrowserListAdapter.VIRTUAL_STORAGE_POPULAR;
            }
            case R.id.action_go_to_recent_files: {
                return GsFileBrowserListAdapter.VIRTUAL_STORAGE_RECENTS;
            }
            case R.id.action_go_to_favourite_files: {
                return GsFileBrowserListAdapter.VIRTUAL_STORAGE_FAVOURITE;
            }
            case R.id.action_go_to_appdata_private: {
                return _cu.getAppDataPrivateDir(_context);
            }
            case R.id.action_go_to_storage: {
                return Environment.getExternalStorageDirectory();
            }
            case R.id.action_go_to_appdata_sdcard_1: {
                if (appDataPublicDirs.size() > 0) {
                    return appDataPublicDirs.get(0).first;
                }
                return Environment.getExternalStorageDirectory();
            }
            case R.id.action_go_to_appdata_sdcard_2: {
                if (appDataPublicDirs.size() > 1) {
                    return appDataPublicDirs.get(1).first;
                }
                return Environment.getExternalStorageDirectory();
            }
            case R.id.action_go_to_appdata_public: {
                appDataPublicDirs = _cu.getAppDataPublicDirs(_context, true, false, false);
                if (appDataPublicDirs.size() > 0) {
                    return appDataPublicDirs.get(0).first;
                }
                return _cu.getAppDataPrivateDir(_context);
            }
        }
        return getNotebookDirectory();
    }

    public int getTabWidth() {
        return getInt(R.string.pref_key__tab_width_v2, 1);
    }

    public boolean listFileInRecents(File file) {
        return getBool(file.getAbsolutePath() + "_list_in_recents", true);
    }

    public void setListFileInRecents(File file, boolean value) {
        setBool(file.getAbsolutePath() + "_list_in_recents", value);

        if (!value) {
            ArrayList<String> recent = getRecentDocuments();
            if (recent.contains(file.getAbsolutePath())) {
                recent.remove(file.getAbsolutePath());
                setRecentDocuments(recent);
            }
        }
    }

    /*public ArrayList<String> getFilesRatedWith(int rating) {
        return getFilesTaggedWith("rating_" + Integer.toString(rating));
    }

    public ArrayList<String> getFilesTaggedWith(String tag) {
        return getStringList("files_tagged_with" + tag, _prefHistory);
    }*/

    public int getRating(File file) {
        return getInt(file.getAbsolutePath() + "_rating", 0);
    }

    public void setRating(File file, int value) {
        setInt(file.getAbsolutePath() + "_rating", value);
    }

    public boolean isEditorLineBreakingEnabled() {
        return getBool(R.string.pref_key__editor_enable_line_breaking, true);
    }

    private List<String> extSettingCache;

    public synchronized boolean isExtOpenWithThisApp(String ext) {
        if (ext.equals("")) {
            ext = "None";
        }
        if (extSettingCache == null) {
            String pref = getString(R.string.pref_key__exts_to_always_open_in_this_app, "");
            extSettingCache = Arrays.asList(pref.toLowerCase().replace(",,", ",None,").replace(" ", "").split(","));
        }
        return extSettingCache.contains(ext) || extSettingCache.contains(".*");
    }

    public boolean isExperimentalFeaturesEnabled() {
        return getBool(R.string.pref_key__is_enable_experimental_features, BuildConfig.IS_TEST_BUILD);
    }

    public boolean isHighlightBiggerHeadings() {
        return getBool(R.string.pref_key__editor_markdown_bigger_headings_2, false);
    }

    public String getViewModeLinkColor() {
        return GsTextUtils.colorToHexString(getInt(R.string.pref_key__view_mode_link_color, Color.parseColor("#388E3C")));
    }

    public String getUnorderedListCharacter() {
        return getString(R.string.pref_key__editor_unordered_list_character, "-");
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public char[] getDefaultPassword() {
        return new PasswordStore(getContext()).loadKey(R.string.pref_key__default_encryption_password);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean isDefaultPasswordSet() {
        final char[] key = getDefaultPassword();
        return (key != null && key.length > 0);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void setDefaultPassword(String password) {
        new PasswordStore(getContext()).storeKey(password, R.string.pref_key__default_encryption_password);
    }

    public boolean getNewFileDialogLastUsedEncryption() {
        return getBool(R.string.pref_key__new_file_dialog_lastused_encryption, false);
    }

    public void setNewFileDialogLastUsedEncryption(boolean b) {
        setBool(R.string.pref_key__new_file_dialog_lastused_encryption, b);
    }

    public boolean getNewFileDialogLastUsedUtf8Bom() {
        return getBool(R.string.pref_key__new_file_dialog_lastused_encryption, false);
    }

    public void setNewFileDialogLastUsedUtf8Bom(boolean b) {
        setBool(R.string.pref_key__new_file_dialog_lastused_encryption, b);
    }

    public String getNewFileDialogLastUsedExtension() {
        return getString(R.string.pref_key__new_file_dialog_lastused_extension, ".md");
    }

    public void setNewFileDialogLastUsedExtension(String v) {
        setString(R.string.pref_key__new_file_dialog_lastused_extension, v);
    }

    public int getNewFileDialogLastUsedType() {
        return getInt(R.string.pref_key__new_file_dialog_lastused_type, 0);
    }

    public void setNewFileDialogLastUsedType(int i) {
        setInt(R.string.pref_key__new_file_dialog_lastused_type, i);
    }

    public void setFileBrowserLastBrowsedFolder(File f) {
        setString(R.string.pref_key__file_browser_last_browsed_folder, f.getAbsolutePath());
    }

    public File getFileBrowserLastBrowsedFolder() {
        return new File(getString(R.string.pref_key__file_browser_last_browsed_folder, getNotebookDirectoryAsStr()));
    }

    public boolean getSetWebViewFulldrawing(boolean... setValue) {
        final String k = "getSetWebViewFulldrawing";
        if (setValue != null && setValue.length == 1) {
            setBool(k, setValue[0]);
            return setValue[0];
        }
        return getBool(k, false);
    }

    public String getTodotxtAdditionalContextsAndProjects() {
        return getString(R.string.pref_key__todotxt__additional_projects_contexts, "+music +video @home @shop");
    }

    // Not tied to an actual settings. Just moved here for clarity.
    public int getDueDateOffset() {
        return getInt(R.string.pref_key__todotxt__due_date_offset, 3);
    }

    public boolean isWikitextDynamicNotebookRootEnabled() {
        return getBool(R.string.pref_key__wikitext_dynamic_notebook_root, false);
    }

    public boolean isOpenLinksWithChromeCustomTabs() {
        return getBool(R.string.pref_key__open_links_with_chrome_custom_tabs, true);
    }

    public String getShareIntoPrefix() {
        return getString(R.string.pref_key__share_into_format, "\\n----\\n{{text}}");
    }
}
