/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;

import net.gsantner.markor.R;
import net.gsantner.markor.activity.openeditor.OpenEditorLinkBoxActivity;
import net.gsantner.markor.activity.openeditor.OpenEditorQuickNoteActivity;
import net.gsantner.markor.activity.openeditor.OpenEditorTodoActivity;
import net.gsantner.markor.ui.FilesystemDialogCreator;
import net.gsantner.markor.util.ActivityUtils;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ContextUtils;
import net.gsantner.markor.util.PermissionChecker;
import net.gsantner.opoc.preference.GsPreferenceFragmentCompat;
import net.gsantner.opoc.preference.SharedPreferencesPropertyBackend;
import net.gsantner.opoc.ui.FilesystemDialogData;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SettingsActivity extends AppCompatActivity {
    static class RESULT {
        static final int NOCHANGE = -1;
        static final int CHANGED = 1;
        static final int RESTART_REQ = 2;
    }

    public static int activityRetVal = RESULT.NOCHANGE;
    private static int iconColor = Color.WHITE;

    @BindView(R.id.toolbar)
    protected Toolbar toolbar;

    public void onCreate(Bundle b) {
        // Must be applied before setContentView
        super.onCreate(b);
        AppSettings appSettings = new AppSettings(this);
        ContextUtils contextUtils = new ContextUtils(this);
        contextUtils.setAppLanguage(appSettings.getLanguage());
        setTheme(appSettings.isDarkThemeEnabled() ? R.style.AppTheme_Dark : R.style.AppTheme_Light);
/*
        ActivityUtils au = new ActivityUtils(this);
        boolean extraLaunchersEnabled = appSettings.isSpecialFileLaunchersEnabled();
        au.setLauncherActivityEnabled(OpenEditorLinkBoxActivity.class, extraLaunchersEnabled);
        au.setLauncherActivityEnabled(OpenEditorQuickNoteActivity.class, extraLaunchersEnabled);
        au.setLauncherActivityEnabled(OpenEditorTodoActivity.class, extraLaunchersEnabled);*/

        // Load UI
        setContentView(R.layout.settings__activity);
        ButterKnife.bind(this);

        // Custom code
        iconColor = contextUtils.rcolor(appSettings.isDarkThemeEnabled() ? R.color.dark__primary_text : R.color.light__primary_text);
        toolbar.setTitle(R.string.settings);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp));
        toolbar.setNavigationOnClickListener(view -> SettingsActivity.this.onBackPressed());
        showFragment(SettingsFragmentMaster.TAG, false);
    }

    protected void showFragment(String tag, boolean addToBackStack) {
        String toolbarTitle = getString(R.string.settings);
        GsPreferenceFragmentCompat prefFrag = (GsPreferenceFragmentCompat) getSupportFragmentManager().findFragmentByTag(tag);
        if (prefFrag == null) {
            switch (tag) {
                case SettingsFragmentMaster.TAG:
                default: {
                    prefFrag = new SettingsFragmentMaster();
                    toolbarTitle = prefFrag.getTitleOrDefault(toolbarTitle);
                    break;
                }
            }
        }
        toolbar.setTitle(toolbarTitle);
        FragmentTransaction t = getSupportFragmentManager().beginTransaction();
        if (addToBackStack) {
            t.addToBackStack(tag);
        }
        t.replace(R.id.settings__activity__fragment_placeholder, prefFrag, tag).commit();
    }

    @Override
    protected void onStop() {
        setResult(activityRetVal);
        super.onStop();
    }

    public static abstract class MarkorSettingsFragment extends GsPreferenceFragmentCompat {
        protected AppSettings _as;

        @Override
        protected SharedPreferencesPropertyBackend getAppSettings(Context context) {
            if (_as == null) {
                _as = new AppSettings(context);
            }
            return _as;
        }

        @Override
        public Integer getIconTintColor() {
            return iconColor;
        }

        @Override
        protected void onPreferenceChanged(SharedPreferences prefs, String key) {
            activityRetVal = RESULT.CHANGED;
        }

        @Override
        protected void onPreferenceScreenChanged(PreferenceFragmentCompat preferenceFragmentCompat, PreferenceScreen preferenceScreen) {
            super.onPreferenceScreenChanged(preferenceFragmentCompat, preferenceScreen);
            if (!TextUtils.isEmpty(preferenceScreen.getTitle())) {
                SettingsActivity a = (SettingsActivity) getActivity();
                if (a != null) {
                    a.toolbar.setTitle(preferenceScreen.getTitle());
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        GsPreferenceFragmentCompat prefFrag = (GsPreferenceFragmentCompat) getSupportFragmentManager().findFragmentByTag(SettingsFragmentMaster.TAG);
        if (prefFrag != null && prefFrag.canGoBack()) {
            prefFrag.goBack();
            return;
        }
        super.onBackPressed();
    }

    public static class SettingsFragmentMaster extends MarkorSettingsFragment {
        public static final String TAG = "SettingsFragmentMaster";

        @Override
        public int getPreferenceResourceForInflation() {
            return R.xml.preferences_master;
        }

        @Override
        public String getFragmentTag() {
            return TAG;
        }

        @Override
        public void doUpdatePreferences() {
            String remove = "/storage/emulated/0/";
            updateSummary(R.string.pref_key__notebook_directory,
                    _cu.htmlToSpanned("<small><small>" + AppSettings.get().getNotebookDirectoryAsStr().replace(remove, "") + "</small></small>")
            );
            updateSummary(R.string.pref_key__quicknote_filepath,
                    _cu.htmlToSpanned("<small><small>" + _as.getQuickNoteFile().getAbsolutePath().replace(remove, "") + "</small></small>")
            );
            updateSummary(R.string.pref_key__todo_filepath,
                    _cu.htmlToSpanned("<small><small>" + _as.getTodoFile().getAbsolutePath().replace(remove, "") + "</small></small>")
            );
            updateSummary(R.string.pref_key__linkbox_filepath,
                    _cu.htmlToSpanned("<small><small>" + _as.getLinkBoxFile().getAbsolutePath().replace(remove, "") + "</small></small>")
            );
            updateSummary(R.string.pref_key__app_theme,
                    getString(_as.isDarkThemeEnabled() ? R.string.dark : R.string.light)
            );
            updatePreference(R.string.pref_key__is_launcher_for_special_files_enabled, null,
                    ("Launcher (" + getString(R.string.special_documents) + ")"),
                    getString(R.string.app_drawer_launcher_special_files_description), true
            );
            updateSummary(R.string.pref_key__todotxt__alternative_naming_context_project,
                    getString(R.string.category_to_context_project_to_tag, getString(R.string.context), getString(R.string.category), getString(R.string.project), getString(R.string.tag)));

        }

        @Override
        protected void onPreferenceChanged(SharedPreferences prefs, String key) {
            super.onPreferenceChanged(prefs, key);
            if (eq(key, R.string.pref_key__language)) {
                activityRetVal = RESULT.RESTART_REQ;
                _as.setRecreateMainRequired(true);
            } else if (eq(key, R.string.pref_key__app_theme)) {
                restartActivity();
                _as.setRecreateMainRequired(true);
            } else if (eq(key, R.string.pref_key__is_overview_statusbar_hidden)) {
                activityRetVal = RESULT.RESTART_REQ;
                _as.setRecreateMainRequired(true);
            } else if (eq(key, R.string.pref_key__is_launcher_for_special_files_enabled)) {
                boolean extraLaunchersEnabled = prefs.getBoolean(key, false);
                ActivityUtils au = new ActivityUtils(getActivity());
                au.setLauncherActivityEnabled(OpenEditorLinkBoxActivity.class, extraLaunchersEnabled);
                au.setLauncherActivityEnabled(OpenEditorQuickNoteActivity.class, extraLaunchersEnabled);
                au.setLauncherActivityEnabled(OpenEditorTodoActivity.class, extraLaunchersEnabled);
            }
        }

        @Override
        @SuppressWarnings({"ConstantConditions", "ConstantIfStatement", "StatementWithEmptyBody"})
        public Boolean onPreferenceClicked(Preference preference) {
            PermissionChecker permc = new PermissionChecker(getActivity());
            if (isAdded() && preference.hasKey()) {
                if (false) {
                } else if (eq(preference, R.string.pref_key__notebook_directory)) {
                    if (permc.doIfExtStoragePermissionGranted()) {
                        FragmentManager fragManager = getActivity().getSupportFragmentManager();
                        FilesystemDialogCreator.showFolderDialog(new FilesystemDialogData.SelectionListenerAdapter() {
                            @Override
                            public void onFsSelected(String request, File file) {
                                AppSettings as = AppSettings.get();
                                as.setSaveDirectory(file.getAbsolutePath());
                                as.setRecreateMainRequired(true);
                                as.setLastOpenedDirectory(as.getNotebookDirectoryAsStr());
                                doUpdatePreferences();
                            }

                            @Override
                            public void onFsDialogConfig(FilesystemDialogData.Options opt) {
                                opt.titleText = R.string.select_storage_folder;
                                if (!permc.mkdirIfStoragePermissionGranted()) {
                                    opt.rootFolder = Environment.getExternalStorageDirectory();
                                }
                            }
                        }, fragManager, getActivity());
                    }
                    return true;
                } else if (eq(preference, R.string.pref_key__quicknote_filepath)) {
                    if (permc.doIfExtStoragePermissionGranted()) {
                        FragmentManager fragManager = getActivity().getSupportFragmentManager();
                        FilesystemDialogCreator.showFileDialog(new FilesystemDialogData.SelectionListenerAdapter() {
                            @Override
                            public void onFsSelected(String request, File file) {
                                AppSettings as = AppSettings.get();
                                as.setQuickNoteFile(file);
                                as.setRecreateMainRequired(true);
                            }

                            @Override
                            public void onFsDialogConfig(FilesystemDialogData.Options opt) {
                                opt.titleText = R.string.quicknote;
                                opt.rootFolder = Environment.getExternalStorageDirectory();
                            }
                        }, fragManager, getActivity());
                    }
                    return true;
                } else if (eq(preference, R.string.pref_key__todo_filepath)) {
                    if (permc.doIfExtStoragePermissionGranted()) {
                        FragmentManager fragManager = getActivity().getSupportFragmentManager();
                        FilesystemDialogCreator.showFileDialog(new FilesystemDialogData.SelectionListenerAdapter() {
                            @Override
                            public void onFsSelected(String request, File file) {
                                AppSettings as = AppSettings.get();
                                as.setTodoFile(file);
                                as.setRecreateMainRequired(true);
                            }

                            @Override
                            public void onFsDialogConfig(FilesystemDialogData.Options opt) {
                                opt.titleText = R.string.todo;
                                opt.rootFolder = Environment.getExternalStorageDirectory();
                            }
                        }, fragManager, getActivity());
                    }
                    return true;
                } else if (eq(preference, R.string.pref_key__linkbox_filepath)) {
                    if (permc.doIfExtStoragePermissionGranted()) {
                        FragmentManager fragManager = getActivity().getSupportFragmentManager();
                        FilesystemDialogCreator.showFileDialog(new FilesystemDialogData.SelectionListenerAdapter() {
                            @Override
                            public void onFsSelected(String request, File file) {
                                AppSettings as = AppSettings.get();
                                as.setLinkBoxFile(file);
                                as.setRecreateMainRequired(true);
                            }

                            @Override
                            public void onFsDialogConfig(FilesystemDialogData.Options opt) {
                                opt.titleText = R.string.linkbox;
                                opt.rootFolder = Environment.getExternalStorageDirectory();
                            }
                        }, fragManager, getActivity());
                    }
                    return true;
                }

                else if (eq(preference, R.string.pref_key__editor_basic_color_scheme_markor)) {
                    _as.setEditorBasicColor(true, R.color.white, R.color.dark_grey);
                    _as.setEditorBasicColor(false, R.color.dark_grey, R.color.light__background);
                }
                else if (eq(preference, R.string.pref_key__editor_basic_color_scheme_blackorwhite)) {
                    _as.setEditorBasicColor(true, R.color.white, R.color.black);
                    _as.setEditorBasicColor(false, R.color.black, R.color.white);
                }
                else if (eq(preference, R.string.pref_key__editor_basic_color_scheme_solarized)) {
                    _as.setEditorBasicColor(true, R.color.solarized_fg, R.color.solarized_bg_dark);
                    _as.setEditorBasicColor(false, R.color.solarized_fg, R.color.solarized_bg_light);
                }
                else if (eq(preference, R.string.pref_key__editor_basic_color_scheme_gruvbox)) {
                    _as.setEditorBasicColor(true, R.color.gruvbox_fg_dark, R.color.gruvbox_bg_dark);
                    _as.setEditorBasicColor(false, R.color.gruvbox_fg_light, R.color.gruvbox_bg_light);
                }

                if (preference.getKey().startsWith("pref_key__editor_basic_color_scheme")){
                    _as.setRecreateMainRequired(true);
                    restartActivity();
                }
            }
            return null;
        }
    }
}
