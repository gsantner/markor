/*
 * Copyright (c) 2014 Jeff Martin
 * Copyright (c) 2015 Pedro Lafuente
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.activity;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.gsantner.markor.R;
import net.gsantner.markor.dialog.ConfirmDialog;
import net.gsantner.markor.dialog.CreateFolderDialog;
import net.gsantner.markor.dialog.FilesystemDialogCreator;
import net.gsantner.markor.model.Constants;
import net.gsantner.markor.model.MarkorSingleton;
import net.gsantner.markor.util.AppCast;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ContextUtils;
import net.gsantner.markor.util.PermissionChecker;
import net.gsantner.opoc.ui.FilesystemDialogData;
import net.gsantner.opoc.util.ActivityUtils;
import net.gsantner.opoc.util.FileUtils;

import java.io.File;
import java.io.Serializable;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;


public class MainActivity extends AppCompatActivity {

    @BindView(R.id.toolbar)
    public Toolbar _toolbar;

    @BindView(R.id.main__activity__fragment_placeholder)
    public View _frameLayout;

    @BindView(R.id.main__activity__content_background)
    public RelativeLayout _contentRoot;

    @BindView(R.id.main__activity__breadcrumbs)
    public TextView _breadcrumbs;

    private FilesystemListFragment _filesystemListFragment;
    private SearchView _searchView;
    private MenuItem _searchItem;

    private boolean _doubleBackToExitPressedOnce;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContextUtils.get().setAppLanguage(AppSettings.get().getLanguage());
        if (AppSettings.get().isOverviewStatusBarHidden()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        if(!AppSettings.get().isRememberLastDirectory()){
            AppSettings.get().setLastOpenedDirectory(null);
        }
        setContentView(R.layout.main__activity);
        ButterKnife.bind(this);
        setSupportActionBar(_toolbar);

        _filesystemListFragment = new FilesystemListFragment();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main__activity__fragment_placeholder, _filesystemListFragment)
                .commit();
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionChecker.checkPermissionResult(this, requestCode, permissions, grantResults);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.action_settings: {
                new ActivityUtils(this).animateToActivity(SettingsActivity.class, false, 124);
                return true;
            }
            case R.id.action_import: {
                if (PermissionChecker.doIfPermissionGranted(this) && PermissionChecker.mkSaveDir(this)) {
                    showImportDialog();
                }
                return true;
            }
            case R.id.action_about: {
                new ActivityUtils(this).animateToActivity(AboutActivity.class, false, 123);
                return true;
            }
            case R.id.action_sort_by_date : {
                _filesystemListFragment.sortAdapter(1);
                return true;
            }
            case R.id.action_sort_by_name :{
                _filesystemListFragment.sortAdapter(0);
                return true;
            }
            case R.id.action_sort_by_size :{
                _filesystemListFragment.sortAdapter(2);
                return true;
            }
        }
        return false;

    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        _searchItem = menu.findItem(R.id.action_search);
        _searchView = (SearchView) _searchItem.getActionView();

        SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
        _searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        _searchView.setQueryHint(getString(R.string.search_hint));
        if (_searchView != null) {
            _searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    if (query != null) {
                        if (_filesystemListFragment.isVisible())
                            _filesystemListFragment.search(query);
                    }
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    if (newText != null) {
                        if (_filesystemListFragment.isVisible()) {
                            if (newText.equalsIgnoreCase("")) {
                                _filesystemListFragment.clearSearchFilter();
                            } else {
                                _filesystemListFragment.search(newText);
                            }
                        }
                    }
                    return false;
                }
            });
            _searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    menu.findItem(R.id.action_import).setVisible(hasFocus);
                    menu.findItem(R.id.action_settings).setVisible(hasFocus);
                    if (!hasFocus) {
                        _searchItem.collapseActionView();
                    }
                }
            });
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (AppSettings.get().isRecreateMainRequired()) {
            recreate();
        }

        setupAppearancePreferences();
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(_localBroadcastReceiver, AppCast.getLocalBroadcastFilter());
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.unregisterReceiver(_localBroadcastReceiver);
    }


    @OnClick({R.id.main__activity__create_folder_fab, R.id.main__activity__create_note_fab})
    public void onClickFab(View view) {
        if (PermissionChecker.doIfPermissionGranted(this) && PermissionChecker.mkSaveDir(this)) {
            switch (view.getId()) {
                case R.id.main__activity__create_folder_fab: {
                    showCreateFolderDialog();
                    break;
                }
                case R.id.main__activity__create_note_fab: {
                    Intent intent = new Intent(MainActivity.this, NoteActivity.class);
                    intent.putExtra(Constants.TARGET_DIR, _filesystemListFragment.getCurrentDir().getAbsolutePath());
                    startActivity(intent);
                    break;
                }
            }
        }
    }

    private BroadcastReceiver _localBroadcastReceiver = new BroadcastReceiver() {
        @SuppressWarnings("unchecked")
        @Override
        public void onReceive(Context context, Intent intent) {
            String filepath = intent.getStringExtra(Constants.EXTRA_FILEPATH); // nullable
            String action = intent.getAction();
            switch (action) {
                case AppCast.CREATE_FOLDER.ACTION: {
                    createFolder(new File(intent.getStringExtra(AppCast.CREATE_FOLDER.EXTRA_PATH)));
                    _filesystemListFragment.listFilesInDirectory(_filesystemListFragment.getCurrentDir());
                    return;
                }
                case Constants.FILESYSTEM_IMPORT_DIALOG_TAG: {
                    importFile(new File(filepath));
                    _filesystemListFragment.listFilesInDirectory(_filesystemListFragment.getCurrentDir());
                    return;
                }
                case Constants.FILESYSTEM_MOVE_DIALOG_TAG: {
                    MarkorSingleton.getInstance().moveSelectedNotes(_filesystemListFragment.getSelectedItems(), filepath);
                    _filesystemListFragment.listFilesInDirectory(_filesystemListFragment.getCurrentDir());
                    _filesystemListFragment.finishActionMode();
                    return;
                }
                case AppCast.VIEW_FOLDER_CHANGED.ACTION: {
                    File currentDir = new File(intent.getStringExtra(AppCast.VIEW_FOLDER_CHANGED.EXTRA_PATH));
                    File rootDir = new File(AppSettings.get().getSaveDirectory());
                    if (currentDir.equals(rootDir)) {
                        _breadcrumbs.setVisibility(View.GONE);
                    } else {
                        String text = currentDir.getParentFile().equals(rootDir)
                                ? (" > " + currentDir.getName())
                                : ("... > " + currentDir.getParentFile().getName() + " > " + currentDir.getName()
                        );
                        _breadcrumbs.setText(text);
                        _breadcrumbs.setVisibility(View.VISIBLE);
                    }
                    if (intent.getBooleanExtra(AppCast.VIEW_FOLDER_CHANGED.EXTRA_FORCE_RELOAD, false)) {
                        _filesystemListFragment.listFilesInDirectory(currentDir);
                    }
                    return;
                }
            }
        }
    };

    @OnLongClick({R.id.main__activity__create_folder_fab, R.id.main__activity__create_note_fab})
    public boolean onLongClickedFab(View view) {
        switch (view.getId()) {
            case R.id.main__activity__create_folder_fab: {
                new ActivityUtils(this).showSnackBar(R.string.create_folder, false);
                return true;
            }
            case R.id.main__activity__create_note_fab: {
                new ActivityUtils(this).showSnackBar(R.string.create_note, false);
                return true;
            }
        }
        return false;
    }

    private void importFileToCurrentDirectory(Context context, File sourceFile) {
        FileUtils.copyFile(sourceFile, new File(_filesystemListFragment.getCurrentDir().getAbsolutePath(), sourceFile.getName()));
        Toast.makeText(context, "Imported to \"" + sourceFile.getName() + "\"",
                Toast.LENGTH_LONG).show();
    }

    private void showCreateFolderDialog() {
        FragmentManager fragManager = getSupportFragmentManager();

        Bundle args = new Bundle();
        args.putString(Constants.CURRENT_DIRECTORY_DIALOG_KEY, _filesystemListFragment.getCurrentDir().getAbsolutePath());

        CreateFolderDialog createFolderDialog = new CreateFolderDialog();
        createFolderDialog.setArguments(args);
        createFolderDialog.show(fragManager, CreateFolderDialog.FRAGMENT_TAG);
    }

    private boolean createFolder(File folder) {
        return !folder.exists() && folder.mkdirs();
    }

    private void setupAppearancePreferences() {
        int color = ContextCompat.getColor(this, AppSettings.get().isDarkThemeEnabled()
                ? R.color.dark__background : R.color.light__background);
        _frameLayout.setBackgroundColor(color);
        _contentRoot.setBackgroundColor(color);
    }

    private void showImportDialog() {
        FilesystemDialogCreator.showFileDialog(new FilesystemDialogData.SelectionListenerAdapter() {
            @Override
            public void onFsSelected(String request, File file) {
                importFile(file);
                _filesystemListFragment.listFilesInDirectory(_filesystemListFragment.getCurrentDir());
            }

            @Override
            public void onFsMultiSelected(String request, File... files) {
                for (File file : files) {
                    importFile(file);
                }
                _filesystemListFragment.listFilesInDirectory(_filesystemListFragment.getCurrentDir());
            }

            @Override
            public void onFsDialogConfig(FilesystemDialogData.Options opt) {
                opt.titleText = R.string.import_from_device;
                opt.doSelectMultiple = true;
                opt.doSelectFile = true;
                opt.doSelectFolder = true;
            }
        }, getSupportFragmentManager(), this);
    }

    private void importFile(final File file) {
        if (new File(_filesystemListFragment.getCurrentDir().getAbsolutePath(), file.getName()).exists()) {
            // Ask if overwriting is okay
            ConfirmDialog d = ConfirmDialog.newInstance(R.string.confirm_overwrite, file,
                    new ConfirmDialog.ConfirmDialogCallback() {
                        @Override
                        public void onConfirmDialogAnswer(boolean confirmed, Serializable data) {
                            if (confirmed) {
                                importFileToCurrentDirectory(MainActivity.this, file);
                            }
                        }
                    });
            d.show(getSupportFragmentManager(), ConfirmDialog.FRAGMENT_TAG);
        } else {
            // Import
            importFileToCurrentDirectory(this, file);
        }
    }

    @Override
    public void onBackPressed() {
        if (_doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        if (_searchView.isFocused()) {
            _searchView.clearFocus();
            _searchView.setSelected(false);
            return;
        }
        if (!_searchView.getQuery().toString().isEmpty() || !_searchView.isIconified()) {
            _searchView.setQuery("", false);
            _searchView.setIconified(true);
            _searchItem.collapseActionView();
            return;
        }


        if (!_filesystemListFragment.onRooDir()) {
            _filesystemListFragment.goToPreviousDir();
        } else {
            this._doubleBackToExitPressedOnce = true;
            Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    _doubleBackToExitPressedOnce = false;
                }
            }, 2000);
        }
    }

}
