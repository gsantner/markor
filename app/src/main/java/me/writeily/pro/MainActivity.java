package me.writeily.pro;

import android.app.FragmentManager;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

import java.io.File;

import me.writeily.pro.adapter.DrawerAdapter;
import me.writeily.pro.dialog.FilesystemDialog;
import me.writeily.pro.dialog.FolderDialog;
import me.writeily.pro.model.Constants;
import me.writeily.pro.model.WriteilySingleton;
import me.writeily.pro.settings.SettingsActivity;


public class MainActivity extends ActionBarActivity {

    private String[] drawerArrayList;

    private NotesFragment notesFragment;

    private DrawerLayout drawerLayout;
    private ListView drawerListView;
    private RelativeLayout drawerView;

    private ActionBarDrawerToggle drawerToggle;
    private DrawerAdapter drawerAdapter;

    private Toolbar toolbar;

    private FloatingActionsMenu fabMenu;
    private FloatingActionButton fabCreateNote;
    private FloatingActionButton fabCreateFolder;

    private View frameLayout;

    private SearchView searchView;
    private BroadcastReceiver folderBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.FOLDER_DIALOG_TAG)) {
                createFolder(new File(intent.getStringExtra(Constants.FOLDER_NAME)));
                notesFragment.listFilesInCurrentDirectory();
            }
        }
    };
    private BroadcastReceiver fsBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String fileName = intent.getStringExtra(Constants.FILESYSTEM_FILE_NAME);
            if (intent.getAction().equals(Constants.FILESYSTEM_IMPORT_DIALOG_TAG)) {
                importFile(new File(fileName));
                notesFragment.listFilesInCurrentDirectory();
            } else {
                WriteilySingleton.getInstance().moveSelectedNotes(notesFragment.getFilesListView(), notesFragment.getFilesAdapter(), fileName);
                notesFragment.listFilesInCurrentDirectory();
                notesFragment.finishActionMode();
            }
        }
    };
    private BroadcastReceiver confirmBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.CONFIRM_DIALOG_TAG)) {
                WriteilySingleton.getInstance().deleteSelectedNotes(notesFragment.getFilesListView(), notesFragment.getFilesAdapter());
                notesFragment.listFilesInCurrentDirectory();
                notesFragment.finishActionMode();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        frameLayout = findViewById(R.id.frame);
        drawerArrayList = getResources().getStringArray(R.array.drawer_array);

        // Set the Navigation Drawer up
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerListView = (ListView) findViewById(R.id.drawer_listview);
        drawerView = (RelativeLayout) findViewById(R.id.drawer_view);

        // Set the drawer adapter
        drawerAdapter = new DrawerAdapter(this, drawerArrayList);
        drawerListView.setAdapter(drawerAdapter);
        drawerListView.setOnItemClickListener(new DrawerClickListener());

        // Drawer shadow
        drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        // Drawer toggle
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.open_drawer, R.string.close_drawer) {

            public void onDrawerClosed(View v) {
                if (notesFragment.isVisible()) {
                    setToolbarTitle(getString(R.string.notes));
                }

                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View v) {
                setToolbarTitle(getString(R.string.app_name));
                invalidateOptionsMenu();
            }
        };

        drawerLayout.setDrawerListener(drawerToggle);

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

        // Load initial fragment
        FragmentManager fm = getFragmentManager();
        fm.beginTransaction()
                .replace(R.id.frame, notesFragment)
                .commit();

        setToolbarTitle(getString(R.string.notes));
        initFolders();

        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void createNote() {
        Intent intent = new Intent(MainActivity.this, NoteActivity.class);
        intent.putExtra(Constants.NOTE_SOURCE_DIR, notesFragment.getCurrentDir());
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
        String currentDir = notesFragment.getCurrentDir();
        args.putString(Constants.CURRENT_DIRECTORY_DIALOG_KEY, currentDir);

        FolderDialog folderDialog = new FolderDialog();
        folderDialog.setArguments(args);
        folderDialog.show(fragManager, Constants.FOLDER_DIALOG_TAG);
    }

    /**
     * Create folders, if they don't already exist.
     */
    private void initFolders() {
        File writeilyFolder = new File(Environment.getExternalStorageDirectory() + Constants.WRITEILY_FOLDER);
        createFolder(writeilyFolder);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        final MenuItem searchItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) MenuItemCompat.getActionView(searchItem);

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
                    if (!hasFocus) {
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
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        setupAppearancePreferences();

        IntentFilter ifilterFolderDialog = new IntentFilter();
        ifilterFolderDialog.addAction(Constants.FOLDER_DIALOG_TAG);
        registerReceiver(folderBroadcastReceiver, ifilterFolderDialog);

        IntentFilter ifilterFsDialog = new IntentFilter();
        ifilterFsDialog.addAction(Constants.FILESYSTEM_IMPORT_DIALOG_TAG);
        ifilterFsDialog.addAction(Constants.FILESYSTEM_MOVE_DIALOG_TAG);
        registerReceiver(fsBroadcastReceiver, ifilterFsDialog);

        IntentFilter ifilterConfirmDialog = new IntentFilter();
        ifilterConfirmDialog.addAction(Constants.CONFIRM_DIALOG_TAG);
        registerReceiver(confirmBroadcastReceiver, ifilterConfirmDialog);

        super.onResume();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(folderBroadcastReceiver);
        unregisterReceiver(fsBroadcastReceiver);
        unregisterReceiver(confirmBroadcastReceiver);
        super.onPause();
    }

    private void setupAppearancePreferences() {
        String theme = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.pref_theme_key), "");

        if (theme.equals(getString(R.string.theme_dark))) {
            frameLayout.setBackgroundColor(getResources().getColor(R.color.dark_grey));
            drawerView.setBackgroundColor(getResources().getColor(R.color.dark_grey));
        } else {
            frameLayout.setBackgroundColor(getResources().getColor(android.R.color.white));
            drawerView.setBackgroundColor(getResources().getColor(android.R.color.white));
        }

        drawerAdapter.notifyDataSetChanged();
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
        WriteilySingleton writeilySingleton = WriteilySingleton.getInstance();
        writeilySingleton.copyFile(file, notesFragment.getCurrentDir());
        Toast.makeText(this, "Imported to \"Writeily\"", Toast.LENGTH_LONG).show();
    }

    /**
     * Set the ActionBar title to @title.
     */
    private void setToolbarTitle(String title) {
        toolbar.setTitle(title);
    }

    private class DrawerClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            FragmentManager fm = getFragmentManager();

            if (i == 0) {
                if (!notesFragment.isVisible()) {
                    fm.beginTransaction().replace(R.id.frame, notesFragment).commit();
                    setToolbarTitle(getString(R.string.notes));
                }
            } else if (i == 1) {
                showImportDialog();
            } else if (i == 2) {
                showSettings();
            }

            // Close the drawer
            drawerLayout.closeDrawer(drawerView);
        }
    }
}
