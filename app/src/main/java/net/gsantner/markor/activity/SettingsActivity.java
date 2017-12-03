/*
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.activity;

import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
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
import net.gsantner.opoc.activity.GsPreferenceFragment;
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

    private AppSettings _as;
    private ContextUtils _cu;

    public void onCreate(Bundle b) {
        // Must be applied before setContentView
        super.onCreate(b);
        _as = new AppSettings(this);
        _cu = new ContextUtils(this);
        _cu.setAppLanguage(AppSettings.get().getLanguage());
        PreferenceFragment _curPrefFragment;
        setTheme(_as.isDarkThemeEnabled() ? R.style.AppTheme_Dark : R.style.AppTheme_Light);

        // Load UI
        setContentView(R.layout.settings__activity);
        ButterKnife.bind(this);

        // Custom code
        iconColor = _cu.color(_as.isDarkThemeEnabled() ? R.color.dark__primary_text : R.color.light__primary_text);
        toolbar.setTitle(R.string.action_settings);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp));
        toolbar.setNavigationOnClickListener(view -> SettingsActivity.this.onBackPressed());
        showFragment(SettingsFragmentMaster.TAG, false);
    }

    protected void showFragment(String tag, boolean addToBackStack) {
        String toolbarTitle = getString(R.string.action_settings);
        GsPreferenceFragment prefFrag = (GsPreferenceFragment) getFragmentManager().findFragmentByTag(tag);
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
        FragmentTransaction t = getFragmentManager().beginTransaction();
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

    public static class SettingsFragmentMaster extends GsPreferenceFragment {
        public static final String TAG = "SettingsFragmentMaster";

        private AppSettings _as;

        @Override
        protected void afterOnCreate(Bundle savedInstances, Context context) {
            super.afterOnCreate(savedInstances, context);
            _as = new AppSettings(context);
        }

        @Override
        public int getPreferenceResourceForInflation() {
            return R.xml.preferences_master;
        }

        @Override
        public String getFragmentTag() {
            return TAG;
        }

        @Override
        public Integer getIconTintColor() {
            return iconColor;
        }

        @Override
        protected AppSettingsBase getAppSettings(Context context) {
            return new AppSettings(context);
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
                            + "\n" + AppSettings.get().getTodoTxtFile().getAbsolutePath()
            );
        }


        @Override
        protected void onPreferenceChanged(SharedPreferences prefs, String key) {
            activityRetVal = RESULT.CHANGED;
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
        public Boolean onPreferenceClicked(PreferenceScreen screen, Preference preference) {
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