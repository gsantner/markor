/*#######################################################
 *
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
import android.app.ActivityManager;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationBarView;

import net.gsantner.markor.BuildConfig;
import net.gsantner.markor.R;
import net.gsantner.markor.frontend.NewFileDialog;
import net.gsantner.markor.frontend.filebrowser.MarkorFileBrowserFactory;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.util.MarkorContextUtils;
import net.gsantner.opoc.format.GsSimpleMarkdownParser;
import net.gsantner.opoc.frontend.base.GsFragmentBase;
import net.gsantner.opoc.frontend.filebrowser.GsFileBrowserFragment;
import net.gsantner.opoc.frontend.filebrowser.GsFileBrowserListAdapter;
import net.gsantner.opoc.frontend.filebrowser.GsFileBrowserOptions;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import other.writeily.widget.WrMarkorWidgetProvider;

public class MainActivity extends MarkorBaseActivity implements NavigationBarView.OnItemSelectedListener {

    public static boolean IS_DEBUG_ENABLED = false;

    private BottomNavigationView _bottomNav;
    private ViewPager2 _viewPager;
    private GsFileBrowserFragment _notebook;
    private DocumentEditAndViewFragment _quicknote, _todo;
    private MoreFragment _more;
    private FloatingActionButton _fab;

    private boolean _doubleBackToExitPressedOnce;
    private MarkorContextUtils _cu;
    private File _quickSwitchPrevFolder = null;

    @SuppressLint("SdCardPath")
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IS_DEBUG_ENABLED |= BuildConfig.IS_TEST_BUILD;
        _cu = new MarkorContextUtils(this);
        setContentView(R.layout.main__activity);
        _bottomNav = findViewById(R.id.bottom_navigation_bar);
        _viewPager = findViewById(R.id.main__view_pager_container);
        _fab = findViewById(R.id.fab_add_new_item);
        _fab.setOnClickListener(this::onClickFab);
        _fab.setOnLongClickListener(this::onLongClickFab);
        _viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                onViewPagerPageSelected(position);
            }
        });

        setSupportActionBar(findViewById(R.id.toolbar));
        optShowRate();

        // Setup viewpager
        setupFragmentsAndLocations();
        _viewPager.setOffscreenPageLimit(4);
        _bottomNav.setOnItemSelectedListener(this);
        reduceViewpagerSwipeSensitivity();

        // noinspection PointlessBooleanExpression - Send Test intent
        if (BuildConfig.IS_TEST_BUILD && false) {
            DocumentActivity.launch(this, new File("/sdcard/Documents/mordor/aa-beamer.md"), true, null, null);
        }

        _cu.applySpecialLaunchersVisibility(this, _appSettings.isSpecialFileLaunchersEnabled());
    }

    /**
     * 1. Check if any of the standard files or the start folder require permissions
     * 2. Set fallbacks for each that do
     * 3. Ask for permissions
     * 4. If permissions granted re-create with correct files
     * 5. Else tell user that we had to fallback
     */
    private void setupFragmentsAndLocations() {
        // Initialize the adapter with appropriate files
        // -----------------------------------------------------------------------------------------
        final SectionsPagerAdapter adapter = new SectionsPagerAdapter(getSupportFragmentManager());
        adapter._notebookFile = _appSettings.getNotebookFile();
        adapter._qnFile = _appSettings.getQuickNoteFile();
        adapter._todoFile = _appSettings.getTodoFile();

        final boolean fixNb = !adapter._notebookFile.canWrite();
        final boolean fixQn = !adapter._todoFile.canWrite();
        final boolean fixTodo = !adapter._qnFile.canWrite();
        String message = "";
        if (fixNb) {
            message += "\n\n" + getString(R.string.notebook) + ": " + adapter._notebookFile.getPath();
            adapter._notebookFile = _appSettings.getDefaultNotebookFile();
        }
        if (fixQn) {
            message += "\n\n" + getString(R.string.quicknote) + ": " + adapter._qnFile.getPath();
            adapter._qnFile = _appSettings.getDefaultQuickNoteFile();
        }
        if (fixTodo) {
            message += "\n\n" + getString(R.string.todo) + ": " + adapter._todoFile.getPath();
            adapter._todoFile = _appSettings.getDefaultTodoFile();
        }
        _viewPager.setAdapter(adapter);

        // If files need permission, request it
        // -----------------------------------------------------------------------------------------
        if (fixNb || fixQn || fixTodo) {
            MarkorContextUtils.requestFilePermission(this,  getString(R.string.permission_needed_to_access, message),

                    // Permissions granted - set the required folders etc
                    () -> {
                        if (fixNb) {
                            adapter._notebookFile = _appSettings.getNotebookFile();
                            if (_notebook != null) { // Null check as we may not have initialized the fragment yet
                                final GsFileBrowserListAdapter na = _notebook.getAdapter();
                                if (na != null) {
                                    na.getFsOptions().rootFolder = adapter._notebookFile;
                                    na.setCurrentFolder(adapter._notebookFile);
                                }
                            }
                        }

                        if (fixQn) {
                            adapter._qnFile = _appSettings.getQuickNoteFile();
                            if (_quicknote != null) {
                                _quicknote.setFile(adapter._qnFile);
                            }
                        }

                        if (fixTodo) {
                            adapter._todoFile = _appSettings.getTodoFile();
                            if (_todo != null) {
                                _todo.setFile(adapter._todoFile);
                            }
                        }
                    },

                    // Permission not granted - let the user know that we are reverting to default files
                    () -> {
                        String revert = getString(R.string.permission_not_granted) + "\n" + getString(R.string.loading_default_value);
                        if (fixNb) {
                            revert += "\n\n" + getString(R.string.notebook) + ": " + adapter._notebookFile.getPath();
                            _appSettings.setNotebookFile(adapter._notebookFile);
                        }
                        if (fixQn) {
                            revert += "\n\n" + getString(R.string.quicknote) + ": " + adapter._qnFile.getPath();
                            _appSettings.setQuickNoteFile(adapter._qnFile);
                        }
                        if (fixTodo) {
                            revert += "\n\n" + getString(R.string.todo) + ": " + adapter._todoFile.getPath();
                            _appSettings.setTodoFile(adapter._todoFile);
                        }
                        new AlertDialog.Builder(this).setMessage(revert).setCancelable(true).show();
                    }
            );
        }
    }

    @Override
    public void onActivityFirstTimeVisible() {
        super.onActivityFirstTimeVisible();
        // Switch to tab if specific folder _not_ requested, and not recreating from saved instance
        final int startTab = _appSettings.getAppStartupTab();
        if (startTab != R.id.nav_notebook && MarkorContextUtils.getValidIntentDir(getIntent(), null) == null) {
            _viewPager.postDelayed(() -> _viewPager.setCurrentItem(tabIdToPos(startTab)), 100);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save references to fragments
        try {
            final FragmentManager manager = getSupportFragmentManager();
            // Put and get notebook first. Most important for correct operation.
            manager.putFragment(outState, Integer.toString(R.id.nav_notebook), _notebook);
            manager.putFragment(outState, Integer.toString(R.id.nav_quicknote), _quicknote);
            manager.putFragment(outState, Integer.toString(R.id.nav_todo), _todo);
            manager.putFragment(outState, Integer.toString(R.id.nav_more), _more);
        } catch (NullPointerException | IllegalStateException ignored) {}
    }

    @Override
    public void onRestoreInstanceState(final Bundle state) {
        super.onRestoreInstanceState(state);

        if (state == null) {
            return;
        }

        // Get back references to fragments
        try {
            final FragmentManager manager = getSupportFragmentManager();
            _notebook = (GsFileBrowserFragment) manager.getFragment(state, Integer.toString(R.id.nav_notebook));
            _quicknote = (DocumentEditAndViewFragment) manager.getFragment(state, Integer.toString(R.id.nav_quicknote));
            _todo = (DocumentEditAndViewFragment) manager.getFragment(state, Integer.toString(R.id.nav_todo));
            _more = (MoreFragment) manager.getFragment(state, Integer.toString(R.id.nav_more));
        } catch (NullPointerException | IllegalStateException ignored) {}
    }

    // Reduces swipe sensitivity
    // Inspired by https://stackoverflow.com/a/72067439
    private void reduceViewpagerSwipeSensitivity() {
        final int SLOP_MULTIPLIER = 4;
        try {
            final Field ff = ViewPager2.class.getDeclaredField("mRecyclerView");
            ff.setAccessible(true);
            final RecyclerView recyclerView = (RecyclerView) ff.get(_viewPager);
            // Set a constant so we don't continuously reduce this value with every call
            recyclerView.setScrollingTouchSlop(RecyclerView.TOUCH_SLOP_PAGING);
            final Field touchSlopField = RecyclerView.class.getDeclaredField("mTouchSlop");
            touchSlopField.setAccessible(true);
            final int touchSlop = (int) touchSlopField.get(recyclerView);
            touchSlopField.set(recyclerView, touchSlop * SLOP_MULTIPLIER);
        } catch (Exception e) {
            Log.d(MainActivity.class.getName(), e.getMessage());
        }
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        final File dir = MarkorContextUtils.getValidIntentDir(intent, null);
        if (_notebook != null && dir != null) {
            _notebook.post(() -> _notebook.getAdapter().setCurrentFolder(dir));
            _bottomNav.postDelayed(() -> _bottomNav.setSelectedItemId(R.id.nav_notebook), 10);
        }
    }

    private void optShowRate() {
        try {
            new com.pixplicity.generate.Rate.Builder(this)
                    .setTriggerCount(4)
                    .setMinimumInstallTime((int) TimeUnit.MINUTES.toMillis(30))
                    .setFeedbackAction(() -> _cu.showGooglePlayEntryForThisApp(MainActivity.this))
                    .build().count().showRequest();
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        if (item.getItemId() == R.id.action_settings) {
            _cu.animateToActivity(this, SettingsActivity.class, false, null);
            return true;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.main__menu, menu);
        menu.findItem(R.id.action_settings).setVisible(_appSettings.isShowSettingsOptionInMainToolbar());

        _cu.tintMenuItems(menu, true, Color.WHITE);
        _cu.setSubMenuIconsVisibility(menu, true);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (_appSettings.isRecreateMainRequired()) {
            // recreate(); // does not remake fragments
            final Intent intent = getIntent();
            overridePendingTransition(0, 0);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            finish();
            overridePendingTransition(0, 0);
            startActivity(intent);
        }

        _cu.setKeepScreenOn(this, _appSettings.isKeepScreenOn());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && _appSettings.isMultiWindowEnabled()) {
            setTaskDescription(new ActivityManager.TaskDescription(getString(R.string.app_name)));
        }

        // Intro dialog and show changelog etc
        final boolean firstStart = IntroActivity.optStart(this);
        try {
            if (!firstStart && _appSettings.isAppCurrentVersionFirstStart(true)) {
                GsSimpleMarkdownParser smp = GsSimpleMarkdownParser.get().setDefaultSmpFilter(GsSimpleMarkdownParser.FILTER_ANDROID_TEXTVIEW);
                String html = "";
                html += smp.parse(getString(R.string.copyright_license_text_official).replace("\n", "  \n"), "").getHtml();
                html += "<br/><br/><br/><big><big>" + getString(R.string.changelog) + "</big></big><br/>" + smp.parse(getResources().openRawResource(R.raw.changelog), "", GsSimpleMarkdownParser.FILTER_ANDROID_TEXTVIEW);
                html += "<br/><br/><br/><big><big>" + getString(R.string.licenses) + "</big></big><br/>" + smp.parse(getResources().openRawResource(R.raw.licenses_3rd_party), "").getHtml();
                _cu.showDialogWithHtmlTextView(this, 0, html);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void restartMainActivity() {
        getWindow().getDecorView().postDelayed(() -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            finish();
            startActivity(intent);
        }, 1);
    }

    // Cycle between recent, favourite, and current
    public boolean onLongClickFab(View view) {
        if (_notebook != null) {
            final File current = _notebook.getCurrentFolder();
            final File dest;
            if (GsFileBrowserListAdapter.VIRTUAL_STORAGE_RECENTS.equals(current)) {
                dest = GsFileBrowserListAdapter.VIRTUAL_STORAGE_FAVOURITE;
            } else if (GsFileBrowserListAdapter.VIRTUAL_STORAGE_FAVOURITE.equals(current)) {
                if (_quickSwitchPrevFolder != null) {
                    dest = _quickSwitchPrevFolder;
                } else {
                    dest = GsFileBrowserListAdapter.VIRTUAL_STORAGE_RECENTS;
                }
            } else {
                _quickSwitchPrevFolder = current;
                dest = GsFileBrowserListAdapter.VIRTUAL_STORAGE_FAVOURITE;
            }
            _notebook.getAdapter().setCurrentFolder(dest);
        }
        return true;
    }

    public void onClickFab(final View view) {
        if (_notebook == null || _notebook.getAdapter() == null) {
            return;
        }

        if (_notebook.getAdapter().isCurrentFolderVirtual()) {
            _notebook.getAdapter().setCurrentFolder(_appSettings.getNotebookFile());
            return;
        }

        if (view.getId() == R.id.fab_add_new_item) {
            if (_cu.isUnderStorageAccessFolder(this, _notebook.getCurrentFolder(), true) && _cu.getStorageAccessFrameworkTreeUri(this) == null) {
                _cu.showMountSdDialog(this);
                return;
            }

            if (!_notebook.getAdapter().isCurrentFolderWriteable()) {
                return;
            }

            final NewFileDialog dialog = NewFileDialog.newInstance(_notebook.getCurrentFolder(), true, (ok, f) -> {
                if (ok) {
                    if (f.isFile()) {
                        DocumentActivity.launch(MainActivity.this, f, false, null, null);
                    } else if (f.isDirectory()) {
                        _notebook.reloadCurrentFolder();
                    }
                }
            });
            dialog.show(getSupportFragmentManager(), NewFileDialog.FRAGMENT_TAG);
        }
    }

    @Override
    public void onBackPressed() {
        // Exit confirmed with 2xBack
        if (_doubleBackToExitPressedOnce) {
            super.onBackPressed();
            _appSettings.setFileBrowserLastBrowsedFolder(_appSettings.getNotebookFile());
            return;
        }

        // Check if fragment handled back press
        final GsFragmentBase<?, ?> frag = getPosFrament(getCurrentPos());
        if (frag != null && frag.onBackPressed()) {
            return;
        }

        // Confirm exit with back / snackbar
        _doubleBackToExitPressedOnce = true;
        _cu.showSnackBar(this, R.string.press_back_again_to_exit, false, R.string.exit, view -> finish());
        new Handler().postDelayed(() -> _doubleBackToExitPressedOnce = false, 2000);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        _viewPager.setCurrentItem(tabIdToPos(item.getItemId()));
        return true;
    }

    public String getFileBrowserTitle() {
        final File file = _appSettings.getFileBrowserLastBrowsedFolder();
        String title = getString(R.string.app_name);
        if (!_appSettings.getNotebookFile().getAbsolutePath().equals(file.getAbsolutePath())) {
            title = "> " + file.getName();
        }
        return title;
    }

    public int tabIdToPos(final int id) {
        if (id == R.id.nav_notebook) return 0;
        if (id == R.id.nav_todo) return 1;
        if (id == R.id.nav_quicknote) return 2;
        if (id == R.id.nav_more) return 3;
        return 0;
    }

    public int tabIdFromPos(final int pos) {
        return _bottomNav.getMenu().getItem(pos).getItemId();
    }

    public int getCurrentPos() {
        return _viewPager.getCurrentItem();
    }

    public String getPosTitle(final int pos) {
        if (pos == 0) return getFileBrowserTitle();
        if (pos == 1) return getString(R.string.todo);
        if (pos == 2) return getString(R.string.quicknote);
        if (pos == 3) return getString(R.string.more);
        return "";
    }

    public GsFragmentBase<?, ?> getPosFrament(final int pos) {
        if (pos == 0) return _notebook;
        if (pos == 1) return _todo;
        if (pos == 2) return _quicknote;
        if (pos == 3) return _more;
        return null;
    }

    public void onViewPagerPageSelected(int pos) {
        _bottomNav.getMenu().getItem(pos).setChecked(true);

        if (pos == tabIdToPos(R.id.nav_notebook)) {
            _fab.show();
        } else {
            _fab.hide();
        }
        setTitle(getPosTitle(pos));

        if (pos != tabIdToPos(R.id.nav_notebook)) {
            restoreDefaultToolbar();
        }
    }

    class SectionsPagerAdapter extends FragmentStateAdapter {
        File _notebookFile;
        File _qnFile;
        File _todoFile;
        private File _startFolder;

        SectionsPagerAdapter(FragmentManager fragMgr) {
            super(fragMgr, MainActivity.this.getLifecycle());
        }

        @NonNull
        @Override
        public Fragment createFragment(final int pos) {
            final GsFragmentBase<?, ?> frag;
            final int id = tabIdFromPos(pos);
            if (id == R.id.nav_quicknote) {
                frag = _quicknote = DocumentEditAndViewFragment.newInstance(new Document(_qnFile), Document.EXTRA_FILE_LINE_NUMBER_LAST, false);
            } else if (id == R.id.nav_todo) {
                frag = _todo = DocumentEditAndViewFragment.newInstance(new Document(_todoFile), Document.EXTRA_FILE_LINE_NUMBER_LAST, false);
            } else if (id == R.id.nav_more) {
                frag = _more = MoreFragment.newInstance();
            } else {
                frag = _notebook = GsFileBrowserFragment.newInstance(getFilesystemFragmentOptions());
            }
            frag.setMenuVisibility(false);
            return frag;
        }

        @Override
        public int getItemCount() {
            return _bottomNav.getMenu().size();
        }

        public GsFileBrowserOptions.Options getFilesystemFragmentOptions() {
            return MarkorFileBrowserFactory.prepareFsViewerOpts(MainActivity.this, false, new GsFileBrowserOptions.SelectionListenerAdapter() {
                @Override
                public void onFsViewerConfig(GsFileBrowserOptions.Options dopt) {
                    dopt.descModtimeInsteadOfParent = true;
                    dopt.rootFolder = _notebookFile;
                    dopt.startFolder = _startFolder;
                    dopt.doSelectMultiple = dopt.doSelectFolder = dopt.doSelectFile = true;
                    dopt.mountedStorageFolder = _cu.getStorageAccessFolder(MainActivity.this);
                }

                @Override
                public void onFsViewerDoUiUpdate(GsFileBrowserListAdapter adapter) {
                    if (adapter != null && adapter.getCurrentFolder() != null && !TextUtils.isEmpty(adapter.getCurrentFolder().getName())) {
                        _appSettings.setFileBrowserLastBrowsedFolder(adapter.getCurrentFolder());
                        if (getCurrentPos() == tabIdToPos(R.id.nav_notebook)) {
                            setTitle(adapter.areItemsSelected() ? "" : getFileBrowserTitle());
                        }
                        invalidateOptionsMenu();
                    }
                }

                @Override
                public void onFsViewerSelected(String request, File file, final Integer lineNumber) {
                    DocumentActivity.handleFileClick(MainActivity.this, file, lineNumber);
                }
            });
        }
    }

    public GsFileBrowserFragment getNotebook() {
        return _notebook;
    }

    @Override
    protected void onPause() {
        super.onPause();
        WrMarkorWidgetProvider.updateLauncherWidgets();
    }

    @Override
    protected void onStop() {
        super.onStop();
        restoreDefaultToolbar();
    }

    /**
     * Restores the default toolbar. Used when changing the tab or moving to another activity
     * while {@link GsFileBrowserFragment} action mode is active (e.g. when renaming a file)
     */
    private void restoreDefaultToolbar() {
        GsFileBrowserFragment wrFragment = getNotebook();
        if (wrFragment != null) {
            wrFragment.clearSelection();
        }
    }
}
