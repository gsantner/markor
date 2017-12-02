/*
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.activity;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import net.gsantner.markor.R;
import net.gsantner.markor.ui.FilesystemDialogCreator;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ContextUtils;
import net.gsantner.markor.util.PermissionChecker;
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

    @BindView(R.id.toolbar)
    protected Toolbar toolbar;

    public static int activityRetVal = RESULT.NOCHANGE;

    public void onCreate(Bundle b) {
        super.onCreate(b);
        ContextUtils.get().setAppLanguage(AppSettings.get().getLanguage());
        if (AppSettings.get().isDarkThemeEnabled()) {
            setTheme(R.style.AppTheme_Dark);
        }
        setContentView(R.layout.settings__activity);
        ButterKnife.bind(this);
        toolbar.setTitle(R.string.action_settings);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp));
        toolbar.setNavigationOnClickListener(view -> SettingsActivity.this.onBackPressed());
        showFragment(SettingsFragmentMaster.TAG, false);
    }

    protected void showFragment(String tag, boolean addToBackStack) {
        PreferenceFragment fragment = (PreferenceFragment) getFragmentManager().findFragmentByTag(tag);
        if (fragment == null) {
            switch (tag) {
                case SettingsFragmentMaster.TAG:
                default:
                    fragment = new SettingsFragmentMaster();
                    toolbar.setTitle(R.string.action_settings);
                    break;
            }
        }
        FragmentTransaction t = getFragmentManager().beginTransaction();
        if (addToBackStack) {
            t.addToBackStack(tag);
        }
        t.replace(R.id.settings__activity__fragment_placeholder, fragment, tag).commit();
    }

    @Override
    protected void onStop() {
        setResult(activityRetVal);
        super.onStop();
    }


    public void restartActivity() {
        // Restart settings activity to reflect theme changes
        Intent intent = getIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        overridePendingTransition(0, 0);
        finish();

        overridePendingTransition(0, 0);
        startActivity(intent);
    }

    public static class SettingsFragmentMaster extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        public static final String TAG = "SettingsFragmentMaster";

        AppSettings _appSettings;

        public void onCreate(Bundle savedInstances) {
            super.onCreate(savedInstances);
            _appSettings = AppSettings.get();
            getPreferenceManager().setSharedPreferencesName("app");
            addPreferencesFromResource(R.xml.preferences);
        }

        @Override
        public void onResume() {
            super.onResume();
            _appSettings.registerPreferenceChangedListener(this);
            updateSummaries();
        }

        @Override
        public void onPause() {
            super.onPause();
            _appSettings.unregisterPreferenceChangedListener(this);
        }

        public void updateSummaries() {
            Preference pref = findPreference(getString(R.string.pref_key__notebook_directory));
            pref.setSummary(getString(R.string.select_storage_folder) + "\n" + AppSettings.get().getNotebookDirectoryAsStr());
            pref = findPreference(getString(R.string.pref_key__markdown__quicknote_filepath));
            pref.setSummary(getString(R.string.pref_summary__loaded_and_saved_as__plus_name, getString(R.string.quicknote))
                    + "\n" + AppSettings.get().getQuickNoteFile().getAbsolutePath());
            pref = findPreference(getString(R.string.pref_key__todotxt_filepath));
            pref.setSummary(getString(R.string.pref_summary__loaded_and_saved_as__plus_name, getString(R.string.todo))
                    + "\n" + AppSettings.get().getTodoTxtFile().getAbsolutePath());
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            activityRetVal = RESULT.CHANGED;
            if (key.equals(getString(R.string.pref_key__language))) {
                activityRetVal = RESULT.RESTART_REQ;
                _appSettings.setRecreateMainRequired(true);
            } else if (key.equals(getString(R.string.pref_key__app_theme))) {
                ((SettingsActivity) getActivity()).restartActivity();
                _appSettings.setRecreateMainRequired(true);
            } else if (key.equals(getString(R.string.pref_key__is_overview_statusbar_hidden))) {
                activityRetVal = RESULT.RESTART_REQ;
                _appSettings.setRecreateMainRequired(true);
            }
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen screen, Preference preference) {
            if (isAdded() && preference.hasKey()) {
                switch (preference.getTitleRes()) {
                    case R.string.notebook_directory: {
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
                    }

                    case R.string.quicknote: {
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
                    }

                    case R.string.todo: {
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
            }
            return super.onPreferenceTreeClick(screen, preference);
        }
    }
}