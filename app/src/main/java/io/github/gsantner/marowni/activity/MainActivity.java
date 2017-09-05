package io.github.gsantner.marowni.activity;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.FragmentManager;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

import java.io.File;
import java.io.Serializable;

import io.github.gsantner.marowni.R;
import io.github.gsantner.marowni.dialog.ConfirmDialog;
import io.github.gsantner.marowni.dialog.CreateFolderDialog;
import io.github.gsantner.marowni.dialog.FilesystemDialog;
import io.github.gsantner.marowni.model.Constants;
import io.github.gsantner.marowni.model.MarowniSingleton;
import io.github.gsantner.marowni.settings.SettingsActivity;
import io.github.gsantner.marowni.util.AppSettings;
import io.github.gsantner.marowni.util.CurrentFolderChangedReceiver;
import io.github.gsantner.marowni.util.Helpers;
import io.github.gsantner.marowni.util.HelpersA;
import io.github.gsantner.marowni.util.RenameBroadcastReceiver;
import io.github.gsantner.marowni.util.Utils;


public class MainActivity extends AppCompatActivity {

    private MenuItem searchItem;

    private FragmentManager fm;

    private NotesFragment notesFragment;

    private Toolbar toolbar;

    public static FloatingActionsMenu fabMenu;
    private FloatingActionButton fabCreateNote;
    private FloatingActionButton fabCreateFolder;

    private View frameLayout;
    private BroadcastReceiver createFolderBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.CREATE_FOLDER_DIALOG_TAG)) {
                createFolder(new File(intent.getStringExtra(Constants.FOLDER_NAME)));
                notesFragment.listFilesInDirectory(notesFragment.getCurrentDir());
            }
        }
    };
    private BroadcastReceiver fsBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String fileName = intent.getStringExtra(Constants.FILESYSTEM_FILE_NAME);
            if (intent.getAction().equals(Constants.FILESYSTEM_IMPORT_DIALOG_TAG)) {
                importFile(new File(fileName));
                notesFragment.listFilesInDirectory(notesFragment.getCurrentDir());
            } else if (intent.getAction().equals(Constants.FILESYSTEM_MOVE_DIALOG_TAG)) {
                MarowniSingleton.getInstance().moveSelectedNotes(notesFragment.getSelectedItems(), fileName);
                notesFragment.listFilesInDirectory(notesFragment.getCurrentDir());
                notesFragment.finishActionMode();
            }
        }
    };
    private BroadcastReceiver confirmBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.CONFIRM_DELETE_DIALOG_TAG)) {
                MarowniSingleton.getInstance().deleteSelectedItems(notesFragment.getSelectedItems());
                notesFragment.listFilesInDirectory(notesFragment.getCurrentDir());
                notesFragment.finishActionMode();
            }
            if (intent.getAction().equals(Constants.CONFIRM_OVERWRITE_DIALOG_TAG)) {
                importFileToCurrentDirectory(context, (File) intent.getSerializableExtra(Constants.SOURCE_FILE));
            }
        }
    };

    private void importFileToCurrentDirectory(Context context, File sourceFile) {
        Helpers.get().copyFile(sourceFile, new File(notesFragment.getCurrentDir().getAbsolutePath(), sourceFile.getName()));
        Toast.makeText(context, "Imported to \"" + sourceFile.getName() + "\"",
                Toast.LENGTH_LONG).show();
    }

    private RenameBroadcastReceiver renameBroadcastReceiver;
    private BroadcastReceiver browseToFolderBroadcastReceiver;

    private boolean doubleBackToExitPressedOnce;

    private static final int ANIM_DURATION_TOOLBAR = 150;
    private static final int ANIM_DURATION_FAB = 150;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        askForStoragePermission();

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        frameLayout = findViewById(R.id.frame);

        fabMenu = (FloatingActionsMenu) findViewById(R.id.fab);
        fabCreateNote = (FloatingActionButton) findViewById(R.id.create_note);
        fabCreateFolder = (FloatingActionButton) findViewById(R.id.create_folder);

        fabCreateNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createNote();
            }
        });

        fabCreateFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createFolder();
            }
        });

        // Set up the fragments
        notesFragment = new NotesFragment();

        renameBroadcastReceiver = new RenameBroadcastReceiver(notesFragment);
        browseToFolderBroadcastReceiver = new CurrentFolderChangedReceiver(this);

        startIntroAnimation();

        initFolders();
    }

    private void askForStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 314
            );
        }
    }


    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 314 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.action_settings: {
                showSettings();
                return true;
            }
            case R.id.action_import: {
                showImportDialog();
                return true;
            }
            case R.id.action_about: {
                HelpersA.get(this).animateToActivity(AboutActivity.class, false, 123);
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
        Intent intent = new Intent(MainActivity.this, NoteActivity.class);
        intent.putExtra(Constants.TARGET_DIR, notesFragment.getCurrentDir().getAbsolutePath());
        startActivity(intent);
        overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_left);
        fabMenu.collapse();
    }

    private void createFolder() {
        showFolderNameDialog();
        fabMenu.collapse();
    }

    private void showFolderNameDialog() {
        FragmentManager fragManager = getFragmentManager();

        Bundle args = new Bundle();
        args.putString(Constants.CURRENT_DIRECTORY_DIALOG_KEY, notesFragment.getCurrentDir().getAbsolutePath());

        CreateFolderDialog createFolderDialog = new CreateFolderDialog();
        createFolderDialog.setArguments(args);
        createFolderDialog.show(fragManager, Constants.CREATE_FOLDER_DIALOG_TAG);
    }

    /**
     * Create folders, if they don't already exist.
     */
    private void initFolders() {
        File defaultMarowniFolder = new File(AppSettings.get().getSaveDirectory());
        createFolder(defaultMarowniFolder);
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

        searchItem = menu.findItem(R.id.action_search);

        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);

        SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    if (query != null) {
                        if (notesFragment.isVisible())
                            notesFragment.search(query);
                    }
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    if (newText != null) {
                        if (notesFragment.isVisible()) {
                            if (newText.equalsIgnoreCase("")) {
                                notesFragment.clearSearchFilter();
                            } else {
                                notesFragment.search(newText);
                            }
                        }
                    }
                    return false;
                }
            });

            searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {

                    menu.findItem(R.id.action_import).setVisible(false);
                    menu.findItem(R.id.action_settings).setVisible(false);

                    if (!hasFocus) {
                        menu.findItem(R.id.action_import).setVisible(true);
                        menu.findItem(R.id.action_settings).setVisible(true);
                        searchItem.collapseActionView();
                        searchView.setQuery("", false);
                    }
                }
            });

            searchView.setQueryHint(getString(R.string.search_hint));
        }

        return true;
    }

    @Override
    protected void onResume() {
        setupAppearancePreferences();

        IntentFilter ifilterCreateFolderDialog = new IntentFilter();
        ifilterCreateFolderDialog.addAction(Constants.CREATE_FOLDER_DIALOG_TAG);
        registerReceiver(createFolderBroadcastReceiver, ifilterCreateFolderDialog);

        IntentFilter ifilterFsDialog = new IntentFilter();
        ifilterFsDialog.addAction(Constants.FILESYSTEM_IMPORT_DIALOG_TAG);
        ifilterFsDialog.addAction(Constants.FILESYSTEM_MOVE_DIALOG_TAG);
        registerReceiver(fsBroadcastReceiver, ifilterFsDialog);

        IntentFilter ifilterConfirmDialog = new IntentFilter();
        ifilterConfirmDialog.addAction(Constants.CONFIRM_DELETE_DIALOG_TAG);
        ifilterConfirmDialog.addAction(Constants.CONFIRM_OVERWRITE_DIALOG_TAG);
        registerReceiver(confirmBroadcastReceiver, ifilterConfirmDialog);

        IntentFilter ifilterRenameDialog = new IntentFilter();
        ifilterRenameDialog.addAction(Constants.RENAME_DIALOG_TAG);
        registerReceiver(renameBroadcastReceiver, ifilterRenameDialog);

        IntentFilter ifilterSwitchedFolderFilder = new IntentFilter();
        ifilterSwitchedFolderFilder.addAction(Constants.CURRENT_FOLDER_CHANGED);
        registerReceiver(browseToFolderBroadcastReceiver, ifilterSwitchedFolderFilder);

        super.onResume();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(createFolderBroadcastReceiver);
        unregisterReceiver(fsBroadcastReceiver);
        unregisterReceiver(confirmBroadcastReceiver);
        unregisterReceiver(renameBroadcastReceiver);
        unregisterReceiver(browseToFolderBroadcastReceiver);
        super.onPause();
    }

    private void setupAppearancePreferences() {
        if (AppSettings.get().isDarkThemeEnabled()) {
            frameLayout.setBackgroundColor(getResources().getColor(R.color.dark_grey));
            RelativeLayout content = (RelativeLayout) findViewById(R.id.activity_main_content_background);
            content.setBackgroundColor(getResources().getColor(R.color.dark_grey));
        } else {
            frameLayout.setBackgroundColor(getResources().getColor(android.R.color.white));
            RelativeLayout content = (RelativeLayout) findViewById(R.id.activity_main_content_background);
            content.setBackgroundColor(getResources().getColor(android.R.color.white));
        }
    }

    /**
     * Show the SettingsFragment
     */
    private void showSettings() {
        Intent settingsIntent = new Intent(this, SettingsActivity.class);
        startActivity(settingsIntent);
    }

    private void showImportDialog() {
        FragmentManager fragManager = getFragmentManager();

        Bundle args = new Bundle();
        args.putString(Constants.FILESYSTEM_ACTIVITY_ACCESS_TYPE_KEY, Constants.FILESYSTEM_FILE_ACCESS_TYPE);

        FilesystemDialog filesystemDialog = new FilesystemDialog();
        filesystemDialog.setArguments(args);
        filesystemDialog.show(fragManager, Constants.FILESYSTEM_IMPORT_DIALOG_TAG);
    }

    private void importFile(File file) {
        if (new File(notesFragment.getCurrentDir().getAbsolutePath(), file.getName()).exists()) {
            askForConfirmation(file);
        } else {
            importFileToCurrentDirectory(this, file);
        }
    }

    private void askForConfirmation(Serializable file) {
        FragmentManager fragManager = getFragmentManager();
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
        toolbar.setTitle(title);
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        if (!notesFragment.onRooDir()) {
            notesFragment.goToPreviousDir();
        } else {
            this.doubleBackToExitPressedOnce = true;
            Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    doubleBackToExitPressedOnce = false;
                }
            }, 2000);
        }
    }

    private void startIntroAnimation() {
        fabMenu.setTranslationY(2 * getResources().getDimensionPixelOffset(R.dimen.btn_fab_size));
        int actionbarSize = Utils.dpToPx(56);
        toolbar.setTranslationY(-actionbarSize);

        toolbar.animate()
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
        fabMenu.animate()
                .translationY(0)
                .setInterpolator(new OvershootInterpolator(1.f))
                .setDuration(ANIM_DURATION_FAB)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        // Load initial fragment
                    }
                })
                .start();

        fm = getFragmentManager();
        fm.beginTransaction()
                .replace(R.id.frame, notesFragment)
                .commit();
    }
}
