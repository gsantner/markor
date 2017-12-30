/*
 * Copyright (c) 2017-2018 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
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
import net.gsantner.markor.ui.FilesystemDialogCreator;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ContextUtils;
import net.gsantner.markor.util.PermissionChecker;
import net.gsantner.opoc.preference.GsPreferenceFragmentCompat;
import net.gsantner.opoc.ui.FilesystemDialogData;
import net.gsantner.opoc.util.AppSettingsBase;

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

    private AppSettings _appSettings;
    private ContextUtils _contextUtils;

    public void onCreate(Bundle b) {
        // Must be applied before setContentView
        super.onCreate(b);
        _appSettings = new AppSettings(this);
        _contextUtils = new ContextUtils(this);
        _contextUtils.setAppLanguage(_appSettings.getLanguage());
        setTheme(_appSettings.isDarkThemeEnabled() ? R.style.AppTheme_Dark : R.style.AppTheme_Light);

        // Load UI
        setContentView(R.layout.settings__activity);
        ButterKnife.bind(this);

        // Custom code
        iconColor = _contextUtils.color(_appSettings.isDarkThemeEnabled() ? R.color.dark__primary_text : R.color.light__primary_text);
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
        protected AppSettingsBase getAppSettings(Context context) {
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
        public void updateSummaries() {
            updateSummary(R.string.pref_key__notebook_directory, R.drawable.ic_save_black_24dp,
                    getString(R.string.select_storage_folder) + "\n" + AppSettings.get().getNotebookDirectoryAsStr()
            );
            updateSummary(R.string.pref_key__markdown__quicknote_filepath, R.drawable.ic_lightning_white_24dp,
                    getString(R.string.pref_summary__loaded_and_saved_as__plus_name, getString(R.string.quicknote))
                            + "\n" + AppSettings.get().getQuickNoteFile().getAbsolutePath()
            );
            updateSummary(R.string.pref_key__todotxt_filepath, R.drawable.ic_assignment_turned_in_black_24dp,
                    getString(R.string.pref_summary__loaded_and_saved_as__plus_name, getString(R.string.todo))
                            + "\n" + AppSettings.get().getTodoFile().getAbsolutePath()
            );
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
            }
        }

        @Override
        @SuppressWarnings({"ConstantConditions", "ConstantIfStatement", "StatementWithEmptyBody"})
        public Boolean onPreferenceClicked(Preference preference) {
            if (isAdded() && preference.hasKey()) {
                if (false) {
                } else if (eq(preference, R.string.pref_key__notebook_directory)) {
                    if (PermissionChecker.doIfPermissionGranted(getActivity()) && PermissionChecker.mkSaveDir(getActivity())) {
                        FragmentManager fragManager = ((AppCompatActivity) getActivity()).getSupportFragmentManager();
                        FilesystemDialogCreator.showFolderDialog(new FilesystemDialogData.SelectionListenerAdapter() {
                            @Override
                            public void onFsSelected(String request, File file) {
                                AppSettings as = AppSettings.get();
                                as.setSaveDirectory(file.getAbsolutePath());
                                as.setRecreateMainRequired(true);
                                as.setLastOpenedDirectory(as.getNotebookDirectoryAsStr());
                                updateSummaries();
                            }

                            @Override
                            public void onFsDialogConfig(FilesystemDialogData.Options opt) {
                                opt.titleText = R.string.select_storage_folder;
                            }
                        }, fragManager, getActivity());
                        return true;
                    }
                } else if (eq(preference, R.string.pref_key__markdown__quicknote_filepath)) {
                    if (PermissionChecker.doIfPermissionGranted(getActivity()) && PermissionChecker.mkSaveDir(getActivity())) {
                        FragmentManager fragManager = ((AppCompatActivity) getActivity()).getSupportFragmentManager();
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
                        return true;
                    }
                } else if (eq(preference, R.string.pref_key__todotxt_filepath)) {
                    if (PermissionChecker.doIfPermissionGranted(getActivity()) && PermissionChecker.mkSaveDir(getActivity())) {
                        FragmentManager fragManager = ((AppCompatActivity) getActivity()).getSupportFragmentManager();
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
                        return true;
                    }
                }
            }
            return null;
        }
    }
}