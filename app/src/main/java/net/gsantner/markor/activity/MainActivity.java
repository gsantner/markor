package net.gsantner.markor.activity;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.widget.Toast;

import net.gsantner.markor.R;
import net.gsantner.markor.dialog.ConfirmDialog;
import net.gsantner.markor.dialog.CreateFolderDialog;
import net.gsantner.markor.dialog.FilesystemDialog;
import net.gsantner.markor.model.Constants;
import net.gsantner.markor.model.MarkorSingleton;
import net.gsantner.markor.util.AppCast;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ContextUtils;
import net.gsantner.markor.util.CurrentFolderChangedReceiver;
import net.gsantner.markor.util.PermissionChecker;
import net.gsantner.opoc.util.ActivityUtils;
import net.gsantner.opoc.util.FileUtils;

import java.io.File;
import java.util.List;

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

    private FilesystemListFragment _filesystemListFragment;
    private SearchView _searchView;
    private MenuItem _searchItem;

    private BroadcastReceiver _browseToFolderBroadcastReceiver;
    private boolean _doubleBackToExitPressedOnce;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContextUtils.get().setAppLanguage(AppSettings.get().getLanguage());
        if (AppSettings.get().isOverviewStatusBarHidden()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        setContentView(R.layout.main__activity);
        ButterKnife.bind(this);
        setSupportActionBar(_toolbar);

        _filesystemListFragment = new FilesystemListFragment();
        _browseToFolderBroadcastReceiver = new CurrentFolderChangedReceiver(this);

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
        lbm.registerReceiver(_createFolderBroadcastReceiver, AppCast.getLocalBroadcastFilter());
        lbm.registerReceiver(_browseToFolderBroadcastReceiver, AppCast.getLocalBroadcastFilter());
        lbm.registerReceiver(_confirmBroadcastReceiver, AppCast.getLocalBroadcastFilter());

        //IntentFilter ifilterCreateFolderDialog = new IntentFilter();
        //ifilterCreateFolderDialog.addAction(Constants.FRAGMENT_TAG);
        //registerReceiver(_createFolderBroadcastReceiver, ifilterCreateFolderDialog);

        IntentFilter ifilterFsDialog = new IntentFilter();
        ifilterFsDialog.addAction(Constants.FILESYSTEM_IMPORT_DIALOG_TAG);
        ifilterFsDialog.addAction(Constants.FILESYSTEM_MOVE_DIALOG_TAG);
        registerReceiver(_fsBroadcastReceiver, ifilterFsDialog);

        IntentFilter ifilterRenameDialog = new IntentFilter();
        ifilterRenameDialog.addAction(Constants.RENAME_DIALOG_TAG);
        registerReceiver(_renameBroadcastReceiver, ifilterRenameDialog);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.unregisterReceiver(_createFolderBroadcastReceiver);
        unregisterReceiver(_fsBroadcastReceiver);
        unregisterReceiver(_confirmBroadcastReceiver);
        unregisterReceiver(_renameBroadcastReceiver);
        unregisterReceiver(_browseToFolderBroadcastReceiver);
    }


    @OnClick({R.id.main__activity__create_folder_fab, R.id.main__activity__create_note_fab})
    public void onClickFab(View view) {
        if (PermissionChecker.doIfPermissionGranted(this) && PermissionChecker.mkSaveDir(this)) {
            switch (view.getId()) {
                case R.id.main__activity__create_folder_fab: {
                    showFolderNameDialog();
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


    private BroadcastReceiver _renameBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.RENAME_DIALOG_TAG)) {
                String newName = intent.getStringExtra(Constants.RENAME_NEW_NAME);
                File sourceFile = new File(intent.getStringExtra(Constants.SOURCE_FILE));
                File targetFile = new File(sourceFile.getParent(), newName);

                if (targetFile.exists()) {
                    Toast.makeText(context, context.getString(R.string.rename_error_target_already_exists), Toast.LENGTH_LONG).show();
                    _filesystemListFragment.finishActionMode();
                    return;
                }

                if (sourceFile.renameTo(targetFile)) {
                    Toast.makeText(context, context.getString(R.string.rename_success), Toast.LENGTH_LONG).show();
                    _filesystemListFragment.listFilesInDirectory(_filesystemListFragment.getCurrentDir());
                } else {
                    Toast.makeText(context, context.getString(R.string.rename_fail), Toast.LENGTH_LONG).show();
                }
                _filesystemListFragment.finishActionMode();
            }
        }
    };


    private BroadcastReceiver _createFolderBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(AppCast.CREATE_FOLDER.ACTION)) {
                createFolder(new File(intent.getStringExtra(AppCast.CREATE_FOLDER.EXTRA_PATH)));
                _filesystemListFragment.listFilesInDirectory(_filesystemListFragment.getCurrentDir());
            }
        }
    };
    private BroadcastReceiver _fsBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String fileName = intent.getStringExtra(Constants.EXTRA_FILEPATH);
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
        @SuppressWarnings("unchecked")
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(AppCast.CONFIRM.ACTION)) {
                if (intent.getStringExtra(AppCast.CONFIRM.EXTRA_WHAT).equals(ConfirmDialog.WHAT_DELETE)) {
                    List<File> selected = (List<File>) intent.getSerializableExtra(ConfirmDialog.EXTRA_DATA);
                    MarkorSingleton.getInstance().deleteSelectedItems(selected);
                    _filesystemListFragment.listFilesInDirectory(_filesystemListFragment.getCurrentDir());
                    _filesystemListFragment.finishActionMode();
                }
                if (intent.getStringExtra(AppCast.CONFIRM.EXTRA_WHAT).equals(ConfirmDialog.WHAT_OVERWRITE)) {
                    importFileToCurrentDirectory(context, (File) intent.getSerializableExtra(AppCast.CONFIRM.EXTRA_DATA));
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

    private void showFolderNameDialog() {
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
        Bundle args = new Bundle();
        args.putString(Constants.FILESYSTEM_ACTIVITY_ACCESS_TYPE_KEY, Constants.FILESYSTEM_FILE_ACCESS_TYPE);

        FilesystemDialog filesystemDialog = new FilesystemDialog();
        filesystemDialog.setArguments(args);
        filesystemDialog.show(getSupportFragmentManager(), Constants.FILESYSTEM_IMPORT_DIALOG_TAG);
    }

    private void importFile(File file) {
        if (new File(_filesystemListFragment.getCurrentDir().getAbsolutePath(), file.getName()).exists()) {
            askForConfirmation(file);
        } else {
            importFileToCurrentDirectory(this, file);
        }
    }

    private void askForConfirmation(File file) {
        ConfirmDialog d = ConfirmDialog.newInstance(ConfirmDialog.WHAT_OVERWRITE, file);
        d.show(getSupportFragmentManager(), ConfirmDialog.FRAGMENT_TAG);
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
