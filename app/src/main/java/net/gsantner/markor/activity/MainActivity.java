package net.gsantner.markor.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.Toast;

import net.gsantner.markor.R;
import net.gsantner.markor.dialog.ConfirmDialog;
import net.gsantner.markor.dialog.CreateFolderDialog;
import net.gsantner.markor.dialog.FilesystemDialog;
import net.gsantner.markor.model.Constants;
import net.gsantner.markor.model.MarkorSingleton;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ContextUtils;
import net.gsantner.markor.util.CurrentFolderChangedReceiver;
import net.gsantner.markor.util.PermissionChecker;
import net.gsantner.markor.util.RenameBroadcastReceiver;
import net.gsantner.markor.util.Utils;
import net.gsantner.opoc.util.ActivityUtils;
import net.gsantner.opoc.util.FileUtils;

import java.io.File;
import java.io.Serializable;

import butterknife.ButterKnife;
import butterknife.OnLongClick;


public class MainActivity extends AppCompatActivity {
    private FilesystemListFragment _filesystemListFragment;
    private Toolbar _toolbar;
    private FloatingActionButton _fabCreateNote;
    private FloatingActionButton _fabCreateFolder;
    private SearchView _searchView;
    private View _frameLayout;
    private MenuItem _searchItem;

    private FragmentManager _fm;
    private RenameBroadcastReceiver _renameBroadcastReceiver;
    private BroadcastReceiver _browseToFolderBroadcastReceiver;
    private boolean _doubleBackToExitPressedOnce;

    private BroadcastReceiver _createFolderBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.CREATE_FOLDER_DIALOG_TAG)) {
                createFolder(new File(intent.getStringExtra(Constants.FOLDER_NAME)));
                _filesystemListFragment.listFilesInDirectory(_filesystemListFragment.getCurrentDir());
            }
        }
    };
    private BroadcastReceiver _fsBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String fileName = intent.getStringExtra(Constants.FILESYSTEM_FILE_NAME);
            if (intent.getAction().equals(Constants.FILESYSTEM_IMPORT_DIALOG_TAG)) {
                importFile(new File(fileName));
                _filesystemListFragment.listFilesInDirectory(_filesystemListFragment.getCurrentDir());
            } else if (intent.getAction().equals(Constants.FILESYSTEM_MOVE_DIALOG_TAG)) {
                MarkorSingleton.getInstance().moveSelectedNotes(_filesystemListFragment.getSelectedItems(), fileName);
                _filesystemListFragment.listFilesInDirectory(_filesystemListFragment.getCurrentDir());
                _filesystemListFragment.finishActionMode();
            }
        }
    };
    private BroadcastReceiver _confirmBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.CONFIRM_DELETE_DIALOG_TAG)) {
                MarkorSingleton.getInstance().deleteSelectedItems(_filesystemListFragment.getSelectedItems());
                _filesystemListFragment.listFilesInDirectory(_filesystemListFragment.getCurrentDir());
                _filesystemListFragment.finishActionMode();
            }
            if (intent.getAction().equals(Constants.CONFIRM_OVERWRITE_DIALOG_TAG)) {
                importFileToCurrentDirectory(context, (File) intent.getSerializableExtra(Constants.SOURCE_FILE));
            }
        }
    };

    private void importFileToCurrentDirectory(Context context, File sourceFile) {
        FileUtils.copyFile(sourceFile, new File(_filesystemListFragment.getCurrentDir().getAbsolutePath(), sourceFile.getName()));
        Toast.makeText(context, "Imported to \"" + sourceFile.getName() + "\"",
                Toast.LENGTH_LONG).show();
    }


    private static final int ANIM_DURATION_TOOLBAR = 150;
    private static final int ANIM_DURATION_FAB = 150;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContextUtils.get().setAppLanguage(AppSettings.get().getLanguage());
        if (AppSettings.get().isOverviewStatusBarHidden()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        setContentView(R.layout.main__activity);
        ButterKnife.bind(this);

        _toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (_toolbar != null) {
            setSupportActionBar(_toolbar);
        }

        _frameLayout = findViewById(R.id.frame);

        _fabCreateNote = (FloatingActionButton) findViewById(R.id.create_note);
        _fabCreateFolder = (FloatingActionButton) findViewById(R.id.create_folder);

        _fabCreateNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createNote();
            }
        });

        _fabCreateFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createFolder();
            }
        });

        // Set up the fragments
        _filesystemListFragment = new FilesystemListFragment();

        _renameBroadcastReceiver = new RenameBroadcastReceiver(_filesystemListFragment);
        _browseToFolderBroadcastReceiver = new CurrentFolderChangedReceiver(this);

        startIntroAnimation();

        initFolders();
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
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
        }
        return false;

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void createNote() {
        if (PermissionChecker.doIfPermissionGranted(this) && PermissionChecker.mkSaveDir(this)) {
            Intent intent = new Intent(MainActivity.this, NoteActivity.class);
            intent.putExtra(Constants.TARGET_DIR, _filesystemListFragment.getCurrentDir().getAbsolutePath());
            startActivity(intent);
        }
    }

    private void createFolder() {
        if (PermissionChecker.doIfPermissionGranted(this) && PermissionChecker.mkSaveDir(this)) {
            showFolderNameDialog();
        }
    }

    private void showFolderNameDialog() {
        FragmentManager fragManager = getSupportFragmentManager();

        Bundle args = new Bundle();
        args.putString(Constants.CURRENT_DIRECTORY_DIALOG_KEY, _filesystemListFragment.getCurrentDir().getAbsolutePath());

        CreateFolderDialog createFolderDialog = new CreateFolderDialog();
        createFolderDialog.setArguments(args);
        createFolderDialog.show(fragManager, Constants.CREATE_FOLDER_DIALOG_TAG);
    }

    /**
     * Create folders, if they don't already exist.
     */
    private void initFolders() {
        File defaultMarkorFolder = new File(AppSettings.get().getSaveDirectory());
        createFolder(defaultMarkorFolder);
    }

    /**
     * Creates the specified folder if it doesn't already exist.
     *
     * @param folder
     * @return
     */
    private boolean createFolder(File folder) {
        boolean success = false;

        if (!folder.exists()) {
            success = folder.mkdir();
        }

        return success;
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        _searchItem = menu.findItem(R.id.action_search);

        _searchView = (SearchView) MenuItemCompat.getActionView(_searchItem);

        SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
        _searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
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

                    menu.findItem(R.id.action_import).setVisible(false);
                    menu.findItem(R.id.action_settings).setVisible(false);

                    if (!hasFocus) {
                        menu.findItem(R.id.action_import).setVisible(true);
                        menu.findItem(R.id.action_settings).setVisible(true);
                        _searchItem.collapseActionView();
                    }
                }
            });

            _searchView.setQueryHint(getString(R.string.search_hint));
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

        IntentFilter ifilterCreateFolderDialog = new IntentFilter();
        ifilterCreateFolderDialog.addAction(Constants.CREATE_FOLDER_DIALOG_TAG);
        registerReceiver(_createFolderBroadcastReceiver, ifilterCreateFolderDialog);

        IntentFilter ifilterFsDialog = new IntentFilter();
        ifilterFsDialog.addAction(Constants.FILESYSTEM_IMPORT_DIALOG_TAG);
        ifilterFsDialog.addAction(Constants.FILESYSTEM_MOVE_DIALOG_TAG);
        registerReceiver(_fsBroadcastReceiver, ifilterFsDialog);

        IntentFilter ifilterConfirmDialog = new IntentFilter();
        ifilterConfirmDialog.addAction(Constants.CONFIRM_DELETE_DIALOG_TAG);
        ifilterConfirmDialog.addAction(Constants.CONFIRM_OVERWRITE_DIALOG_TAG);
        registerReceiver(_confirmBroadcastReceiver, ifilterConfirmDialog);

        IntentFilter ifilterRenameDialog = new IntentFilter();
        ifilterRenameDialog.addAction(Constants.RENAME_DIALOG_TAG);
        registerReceiver(_renameBroadcastReceiver, ifilterRenameDialog);

        IntentFilter ifilterSwitchedFolderFilder = new IntentFilter();
        ifilterSwitchedFolderFilder.addAction(Constants.CURRENT_FOLDER_CHANGED);
        registerReceiver(_browseToFolderBroadcastReceiver, ifilterSwitchedFolderFilder);
    }


    @Override
    protected void onPause() {
        unregisterReceiver(_createFolderBroadcastReceiver);
        unregisterReceiver(_fsBroadcastReceiver);
        unregisterReceiver(_confirmBroadcastReceiver);
        unregisterReceiver(_renameBroadcastReceiver);
        unregisterReceiver(_browseToFolderBroadcastReceiver);
        super.onPause();
    }

    private void setupAppearancePreferences() {
        if (AppSettings.get().isDarkThemeEnabled()) {
            _frameLayout.setBackgroundColor(getResources().getColor(R.color.dark_grey));
            RelativeLayout content = (RelativeLayout) findViewById(R.id.activity_main_content_background);
            content.setBackgroundColor(getResources().getColor(R.color.dark_grey));
        } else {
            _frameLayout.setBackgroundColor(getResources().getColor(android.R.color.white));
            RelativeLayout content = (RelativeLayout) findViewById(R.id.activity_main_content_background);
            content.setBackgroundColor(getResources().getColor(android.R.color.white));
        }
    }

    private void showImportDialog() {
        android.support.v4.app.FragmentManager fragManager = getSupportFragmentManager();

        Bundle args = new Bundle();
        args.putString(Constants.FILESYSTEM_ACTIVITY_ACCESS_TYPE_KEY, Constants.FILESYSTEM_FILE_ACCESS_TYPE);

        FilesystemDialog filesystemDialog = new FilesystemDialog();
        filesystemDialog.setArguments(args);
        filesystemDialog.show(fragManager, Constants.FILESYSTEM_IMPORT_DIALOG_TAG);
    }

    private void importFile(File file) {
        if (new File(_filesystemListFragment.getCurrentDir().getAbsolutePath(), file.getName()).exists()) {
            askForConfirmation(file);
        } else {
            importFileToCurrentDirectory(this, file);
        }
    }

    private void askForConfirmation(Serializable file) {
        FragmentManager fragManager = getSupportFragmentManager();
        ConfirmDialog confirmDialog = new ConfirmDialog();
        Bundle b = new Bundle();
        b.putSerializable(Constants.SOURCE_FILE, file);
        confirmDialog.setArguments(b);
        confirmDialog.show(fragManager, Constants.CONFIRM_OVERWRITE_DIALOG_TAG);

    }

    /**
     * Set the ActionBar title to @title.
     */
    private void setToolbarTitle(String title) {
        _toolbar.setTitle(title);
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

    private void startIntroAnimation() {
        int actionbarSize = Utils.dpToPx(56);
        _toolbar.setTranslationY(-actionbarSize);

        _toolbar.animate()
                .translationY(0)
                .setDuration(ANIM_DURATION_TOOLBAR)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        startContentAnimation();
                    }
                })
                .start();
    }

    private void startContentAnimation() {
        _fm = getSupportFragmentManager();
        _fm.beginTransaction()
                .replace(R.id.frame, _filesystemListFragment)
                .commit();
    }

    @OnLongClick({R.id.create_folder, R.id.create_note})
    public boolean onLongClicked(View view){
        switch (view.getId()){
            case R.id.create_folder: {
                new ActivityUtils(this).showSnackBar(R.string.create_folder, false);
                return true;
            }
            case R.id.create_note: {
                new ActivityUtils(this).showSnackBar(R.string.create_note, false);
                return true;
            }
        }
        return false;
    }
}
