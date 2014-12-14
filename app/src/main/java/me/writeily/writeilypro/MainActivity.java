package me.writeily.writeilypro;

import android.app.FragmentManager;
import android.app.SearchManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.support.v7.widget.SearchView;
import android.widget.RelativeLayout;

import me.writeily.writeilypro.adapter.DrawerAdapter;
import me.writeily.writeilypro.model.Constants;
import me.writeily.writeilypro.settings.SettingsActivity;

import java.io.File;


public class MainActivity extends ActionBarActivity {

    private String[] drawerArrayList;

    private NotesFragment notesFragment;
    private ArchivesFragment archivesFragment;
    private StarredFragment starredFragment;
    private ImportActivity importActivity;

    private DrawerLayout drawerLayout;
    private ListView drawerListView;
    private RelativeLayout drawerView;

    private ActionBarDrawerToggle drawerToggle;
    private DrawerAdapter drawerAdapter;

    private Toolbar toolbar;
    private Button fab;

    private View frameLayout;

    private SearchView searchView;

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
                } else if (starredFragment.isVisible()) {
                    setToolbarTitle(getString(R.string.starred));
                } else if (archivesFragment.isVisible()) {
                    setToolbarTitle(getString(R.string.archive));
                }

                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View v) {
                setToolbarTitle(getString(R.string.app_name));
                invalidateOptionsMenu();
            }
        };

        drawerLayout.setDrawerListener(drawerToggle);

        fab = (Button) findViewById(R.id.new_note_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, NoteActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_left);
            }
        });

        // Set up the fragments
        notesFragment = new NotesFragment();
        archivesFragment = new ArchivesFragment();
        starredFragment = new StarredFragment();
        importActivity = new ImportActivity();

        // Load initial fragment
        FragmentManager fm = getFragmentManager();
        fm.beginTransaction()
                .replace(R.id.frame, notesFragment)
                .commit();

        setToolbarTitle(getString(R.string.notes));
        initFolders();

        super.onCreate(savedInstanceState);
    }

    /**
     * Create folders, if they don't already exist.
     */
    private void initFolders() {
        File writeilyFolder = new File(Environment.getExternalStorageDirectory() + Constants.WRITEILY_FOLDER);

        boolean writeilyFolderCreated = createFolder(writeilyFolder);

        if (writeilyFolderCreated) {
            File notesFolder = new File(Environment.getExternalStorageDirectory() + Constants.NOTES_FOLDER);
            File starredFolder = new File(Environment.getExternalStorageDirectory() + Constants.STARRED_FOLDER);
            File archivedFolder = new File(Environment.getExternalStorageDirectory() + Constants.ARCHIVED_FOLDER);

            createFolder(notesFolder);
            createFolder(starredFolder);
            createFolder(archivedFolder);
        }
    }

    /**
     * Creates the specified folder if it doesn't already exist.
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
                        else if (archivesFragment.isVisible())
                            archivesFragment.search(query);
                        else if (starredFragment.isVisible())
                            starredFragment.search(query);
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
                        } else if (archivesFragment.isVisible()) {
                            if (newText.equalsIgnoreCase("")) {
                                archivesFragment.clearSearchFilter();
                            } else {
                                archivesFragment.search(newText);
                            }
                        } else if (starredFragment.isVisible()) {
                            if (newText.equalsIgnoreCase("")) {
                                starredFragment.clearSearchFilter();
                            } else {
                                starredFragment.search(newText);
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
                    } else {
                        if (drawerLayout.isDrawerOpen(drawerListView)) {
                            drawerLayout.closeDrawer(drawerListView);
                        }
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
        super.onResume();
        setupAppearancePreferences();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void setupAppearancePreferences() {
        String theme = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.pref_theme_key), "");

        if (!theme.equals("")) {
            if (theme.equals(getString(R.string.theme_dark))) {
                frameLayout.setBackgroundColor(getResources().getColor(R.color.dark_grey));
                drawerListView.setBackgroundColor(getResources().getColor(R.color.dark_grey));
            } else {
                frameLayout.setBackgroundColor(getResources().getColor(android.R.color.white));
                drawerListView.setBackgroundColor(getResources().getColor(android.R.color.white));
            }

            drawerAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Show the SettingsFragment
     */
    private void showSettings() {
        Intent settingsIntent = new Intent(this, SettingsActivity.class);
        startActivity(settingsIntent);
    }

    private void showImportActivity() {
        Intent importIntent = new Intent(this, ImportActivity.class);
        startActivity(importIntent);
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
                if (!starredFragment.isVisible()) {
                    fm.beginTransaction().replace(R.id.frame, starredFragment).commit();
                    setToolbarTitle(getString(R.string.starred));
                }
            } else if (i == 2) {
                if (!archivesFragment.isVisible()) {
                    fm.beginTransaction().replace(R.id.frame, archivesFragment).commit();
                    setToolbarTitle(getString(R.string.archive));
                }
            } else if (i == 3) {
                showImportActivity();
            } else if (i == 4) {
                showSettings();
            }

            // Close the drawer
            drawerLayout.closeDrawer(drawerView);
        }
    }
}
