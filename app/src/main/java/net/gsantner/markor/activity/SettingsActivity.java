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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.widget.RemoteViews;

import net.gsantner.markor.R;
import net.gsantner.markor.ui.FilesystemViewerCreator;
import net.gsantner.markor.util.ActivityUtils;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ContextUtils;
import net.gsantner.markor.util.PermissionChecker;
import net.gsantner.opoc.preference.FontPreferenceCompat;
import net.gsantner.opoc.preference.GsPreferenceFragmentCompat;
import net.gsantner.opoc.preference.SharedPreferencesPropertyBackend;
import net.gsantner.opoc.ui.FilesystemViewerData;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import other.de.stanetz.jpencconverter.PasswordStore;
import other.writeily.widget.WrMarkorWidgetProvider;

public class SettingsActivity extends AppActivityBase {

    @SuppressWarnings("WeakerAccess")
    public static class RESULT {
        public static final int NOCHANGE = -1;
        public static final int CHANGED = 1;
        public static final int RESTART_REQ = 2;
    }

    public static int activityRetVal = RESULT.NOCHANGE;
    private static int iconColor = Color.WHITE;

    @BindView(R.id.toolbar)
    protected Toolbar toolbar;

    public void onCreate(Bundle b) {
        // Must be applied before setContentView
        AppSettings appSettings = new AppSettings(this);
        ContextUtils contextUtils = new ContextUtils(this);
        contextUtils.setAppLanguage(appSettings.getLanguage());
        setTheme(appSettings.isDarkThemeEnabled() ? R.style.AppTheme_Dark : R.style.AppTheme_Light);
        super.onCreate(b);
/*
        ActivityUtils au = new ActivityUtils(this);
        boolean extraLaunchersEnabled = appSettings.isSpecialFileLaunchersEnabled();
        au.setLauncherActivityEnabled(OpenEditorQuickNoteActivity.class, extraLaunchersEnabled);
        au.setLauncherActivityEnabled(OpenEditorTodoActivity.class, extraLaunchersEnabled);*/

        // Load UI
        setContentView(R.layout.settings__activity);
        ButterKnife.bind(this);

        // Custom code
        FontPreferenceCompat.additionalyCheckedFolder = new File(appSettings.getNotebookDirectory(), ".app/fonts");
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
            updatePreference(R.string.pref_key__is_launcher_for_special_files_enabled, null,
                    ("Launcher (" + getString(R.string.special_documents) + ")"),
                    getString(R.string.app_drawer_launcher_special_files_description), true
            );
            updateSummary(R.string.pref_key__exts_to_always_open_in_this_app, _appSettings.getString(R.string.pref_key__exts_to_always_open_in_this_app, ""));
            updateSummary(R.string.pref_key__todotxt__alternative_naming_context_project,
                    getString(R.string.category_to_context_project_to_tag, getString(R.string.context), getString(R.string.category), getString(R.string.project), getString(R.string.tag)));

            setPreferenceVisible(R.string.pref_key__is_multi_window_enabled, Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
            setPreferenceVisible(R.string.pref_key__default_encryption_password, Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && _as.hasPasswordBeenSetOnce()) {
                updateSummary(R.string.pref_key__default_encryption_password, "****");
                setDialogMessage(R.string.pref_key__default_encryption_password, getString(R.string.password_already_set_setting_a_new_password_will_overwrite));
            }


            final int[] experimentalKeys = new int[]{
                    R.string.pref_key__swipe_to_change_mode,
                    R.string.pref_key__todotxt__hl_delay,
                    R.string.pref_key__markdown__hl_delay_v2,
                    R.string.pref_key__is_editor_statusbar_hidden,
                    R.string.pref_key__tab_width_v2,
                    R.string.pref_key__editor_line_spacing,
                    R.string.pref_key__todotxt__start_new_tasks_with_huuid_v3,
                    R.string.pref_key__default_encryption_password,
            };
            for (final int keyId : experimentalKeys) {
                setPreferenceVisible(keyId, _as.isExperimentalFeaturesEnabled());
            }
        }

        @SuppressLint("ApplySharedPref")
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
                au.applySpecialLaunchersVisibility(extraLaunchersEnabled);
            } else if (eq(key, R.string.pref_key__default_encryption_password) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && !TextUtils.isEmpty(prefs.getString(key, null))) {
                new PasswordStore(getActivity()).storeKey(prefs.getString(key, null), key, PasswordStore.SecurityMode.NONE);
                // Never delete the password, otherwise you will remove the password in PasswordStore too!
                // Never remove this line, otherwise the password will be stored unencrypted forever.
                // Using commit and while to ensure that the asterisk-pw is definitely written.
                prefs.edit().remove(key).commit();
                ((EditTextPreference) findPreference(key)).setText("");
                _as.setPasswordHasBeenSetOnce(true);
            }
        }

        @Override
        @SuppressWarnings({"ConstantConditions", "ConstantIfStatement", "StatementWithEmptyBody"})
        public Boolean onPreferenceClicked(Preference preference, String key, int keyResId) {
            PermissionChecker permc = new PermissionChecker(getActivity());
            switch (keyResId) {

                case R.string.pref_key__notebook_directory: {
                    if (permc.doIfExtStoragePermissionGranted()) {
                        FragmentManager fragManager = getActivity().getSupportFragmentManager();
                        FilesystemViewerCreator.showFolderDialog(new FilesystemViewerData.SelectionListenerAdapter() {
                            @Override
                            public void onFsViewerSelected(String request, File file) {
                                AppSettings as = AppSettings.get();
                                as.setSaveDirectory(file.getAbsolutePath());
                                as.setRecreateMainRequired(true);
                                as.setLastOpenedDirectory(as.getNotebookDirectoryAsStr());
                                doUpdatePreferences();
                            }

                            @Override
                            public void onFsViewerConfig(FilesystemViewerData.Options dopt) {
                                dopt.titleText = R.string.select_storage_folder;
                                if (!permc.mkdirIfStoragePermissionGranted()) {
                                    dopt.rootFolder = Environment.getExternalStorageDirectory();
                                }
                            }
                        }, fragManager, getActivity());
                    }
                    return true;
                }
                case R.string.pref_key__quicknote_filepath: {
                    if (permc.doIfExtStoragePermissionGranted()) {
                        FragmentManager fragManager = getActivity().getSupportFragmentManager();
                        FilesystemViewerCreator.showFileDialog(new FilesystemViewerData.SelectionListenerAdapter() {
                            @Override
                            public void onFsViewerSelected(String request, File file) {
                                AppSettings as = AppSettings.get();
                                as.setQuickNoteFile(file);
                                as.setRecreateMainRequired(true);
                                doUpdatePreferences();
                            }

                            @Override
                            public void onFsViewerConfig(FilesystemViewerData.Options dopt) {
                                dopt.titleText = R.string.quicknote;
                                dopt.rootFolder = Environment.getExternalStorageDirectory();
                            }
                        }, fragManager, getActivity(), FilesystemViewerCreator.IsMimeText);
                    }
                    return true;
                }
                case R.string.pref_key__todo_filepath: {
                    if (permc.doIfExtStoragePermissionGranted()) {
                        FragmentManager fragManager = getActivity().getSupportFragmentManager();
                        FilesystemViewerCreator.showFileDialog(new FilesystemViewerData.SelectionListenerAdapter() {
                            @Override
                            public void onFsViewerSelected(String request, File file) {
                                AppSettings as = AppSettings.get();
                                as.setTodoFile(file);
                                as.setRecreateMainRequired(true);
                                doUpdatePreferences();
                            }

                            @Override
                            public void onFsViewerConfig(FilesystemViewerData.Options dopt) {
                                dopt.titleText = R.string.todo;
                                dopt.rootFolder = Environment.getExternalStorageDirectory();
                            }
                        }, fragManager, getActivity(), FilesystemViewerCreator.IsMimeText);
                    }
                    return true;
                }
                case R.string.pref_key__editor_basic_color_scheme_markor: {
                    _as.setEditorBasicColor(true, R.color.white, R.color.dark_grey);
                    _as.setEditorBasicColor(false, R.color.dark_grey, R.color.light__background);
                    break;
                }
                case R.string.pref_key__editor_basic_color_scheme_blackorwhite: {
                    _as.setEditorBasicColor(true, R.color.white, R.color.black);
                    _as.setEditorBasicColor(false, R.color.black, R.color.white);
                    break;
                }
                case R.string.pref_key__editor_basic_color_scheme_amoled: {
                    _as.setEditorBasicColor(true, R.color.white, R.color.black);
                    _as.setEditorBasicColor(false, R.color.black, R.color.white);
                    _as.setDarkThemeEnabled(true);
                    break;
                }
                case R.string.pref_key__editor_basic_color_scheme_solarized: {
                    _as.setEditorBasicColor(true, R.color.solarized_fg, R.color.solarized_bg_dark);
                    _as.setEditorBasicColor(false, R.color.solarized_fg, R.color.solarized_bg_light);
                    break;
                }
                case R.string.pref_key__editor_basic_color_scheme_gruvbox: {
                    _as.setEditorBasicColor(true, R.color.gruvbox_fg_dark, R.color.gruvbox_bg_dark);
                    _as.setEditorBasicColor(false, R.color.gruvbox_fg_light, R.color.gruvbox_bg_light);
                    break;
                }
                case R.string.pref_key__editor_basic_color_scheme_greenscale: {
                    _as.setEditorBasicColor(true, R.color.green_dark, R.color.black);
                    _as.setEditorBasicColor(false, R.color.green_light, R.color.white);
                    break;
                }
                case R.string.pref_key__editor_basic_color_scheme_sepia: {
                    _as.setEditorBasicColor(true, R.color.sepia_bg_light__fg_dark, R.color.sepia_fg_light__bg_dark);
                    _as.setEditorBasicColor(false, R.color.sepia_fg_light__bg_dark, R.color.sepia_bg_light__fg_dark);
                    break;
                }
                case R.string.pref_key__plaintext__reorder_actions:
                case R.string.pref_key__markdown__reorder_actions:
                case R.string.pref_key__todotxt__reorder_actions: {
                    Intent intent = new Intent(getActivity(), ActionOrderActivity.class);
                    intent.putExtra(ActionOrderActivity.EXTRA_FORMAT_KEY, (keyResId == R.string.pref_key__markdown__reorder_actions) ? R.id.action_format_markdown : (keyResId == R.string.pref_key__todotxt__reorder_actions ? R.id.action_format_todotxt : R.id.action_format_plaintext));
                    startActivity(intent);
                    break;
                }
            }

            // Handling widget color scheme
            WrMarkorWidgetProvider.handleWidgetScheme(
                    getContext(),
                    new RemoteViews(getContext().getPackageName(), R.layout.widget_layout),
                    new AppSettings(getContext()).isDarkThemeEnabled());

            if (key.startsWith("pref_key__editor_basic_color_scheme") && !key.contains("_fg_") && !key.contains("_bg_")) {
                _as.setRecreateMainRequired(true);
                restartActivity();
            }
            return null;
        }

        @Override
        public boolean isDividerVisible() {
            return true;
        }

        @Override
        public void onPause() {
            super.onPause();
            // Reset Password to ensure it's not stored as plaintext.
            _as.getDefaultPreferencesEditor().remove(getContext().getString(R.string.pref_key__default_encryption_password)).commit();
        }
    }
}
