/*#######################################################
 *
 *
 *   Maintained 2017-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
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
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
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

public class MainActivity extends MarkorBaseActivity implements GsFileBrowserFragment.FilesystemFragmentOptionsListener, NavigationBarView.OnItemSelectedListener {

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

        try {
            //noinspection ResultOfMethodCallIgnored
            _appSettings.getNotebookDirectory().mkdirs();
        } catch (Exception ignored) {
        }


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
        _viewPager.setAdapter(new SectionsPagerAdapter(getSupportFragmentManager()));
        _viewPager.setOffscreenPageLimit(4);
        _bottomNav.setOnItemSelectedListener(this);
        reduceViewpagerSwipeSensitivity();

        // noinspection PointlessBooleanExpression - Send Test intent
        if (BuildConfig.IS_TEST_BUILD && false) {
            DocumentActivity.launch(this, new File("/sdcard/Documents/mordor/aa-beamer.md"), true, null);
        }

        _cu.applySpecialLaunchersVisibility(this, _appSettings.isSpecialFileLaunchersEnabled());
    }

    @Override
    public void onActivityFirstTimeVisible() {
        super.onActivityFirstTimeVisible();
        // Switch to tab if specific folder _not_ requested, and not recreating from saved instance
        final int startTab = _appSettings.getAppStartupTab();
        if (startTab != R.id.nav_notebook && MarkorContextUtils.getValidIntentFile(getIntent(), null) == null) {
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
        } catch (NullPointerException | IllegalStateException ignored) {
            Log.d(MainActivity.class.getName(), "Child fragments null in onSaveInstanceState()");
        }
    }

    @Override
    public void onRestoreInstanceState(final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState == null) {
            return;
        }

        // Get back references to fragments
        try {
            final FragmentManager manager = getSupportFragmentManager();
            _notebook = (GsFileBrowserFragment) manager.getFragment(savedInstanceState, Integer.toString(R.id.nav_notebook));
            _quicknote = (DocumentEditAndViewFragment) manager.getFragment(savedInstanceState, Integer.toString(R.id.nav_quicknote));
            _todo = (DocumentEditAndViewFragment) manager.getFragment(savedInstanceState, Integer.toString(R.id.nav_todo));
            _more = (MoreFragment) manager.getFragment(savedInstanceState, Integer.toString(R.id.nav_more));

            final NewFileDialog nf = (NewFileDialog) manager.findFragmentByTag(NewFileDialog.FRAGMENT_TAG);
            if (nf != null) {
                nf.setCallback(this::newItemCallback);
            }

        } catch (NullPointerException | IllegalStateException ignored) {
            Log.d(MainActivity.class.getName(), "Child fragment not found in onRestoreInstanceState()");
        }
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
        final File file = MarkorContextUtils.getValidIntentFile(intent, null);
        if (_notebook != null && file != null) {
            _viewPager.setCurrentItem(tabIdToPos(R.id.nav_notebook), false);
            if (file.isDirectory() || GsFileBrowserListAdapter.isVirtualFolder(file)) {
                _notebook.setCurrentFolder(file);
            } else {
                _notebook.getAdapter().showFile(file);
            }
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
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
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
        if (!IntroActivity.isFirstStart(this)) {
            StoragePermissionActivity.requestPermissions(this);
        }

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
            _notebook.getAdapter().setCurrentFolder(_appSettings.getNotebookDirectory());
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

            NewFileDialog.newInstance(_notebook.getCurrentFolder(), true, this::newItemCallback)
                    .show(getSupportFragmentManager(), NewFileDialog.FRAGMENT_TAG);
        }
    }

    private void newItemCallback(final File file) {
        if (file.isFile()) {
            DocumentActivity.launch(MainActivity.this, file, false, null);
        }
        _notebook.getAdapter().showFile(file);
    }

    @Override
    public void onBackPressed() {
        // Exit confirmed with 2xBack
        if (_doubleBackToExitPressedOnce) {
            super.onBackPressed();
            _appSettings.setFileBrowserLastBrowsedFolder(_appSettings.getNotebookDirectory());
            return;
        }

        // Check if fragment handled back press
        final GsFragmentBase<?, ?> frag = getPosFragment(getCurrentPos());
        if (frag != null && frag.onBackPressed()) {
            return;
        }

        // Confirm exit with back / snack bar
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
        if (!_appSettings.getNotebookDirectory().getAbsolutePath().equals(file.getAbsolutePath())) {
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

    public GsFragmentBase<?, ?> getPosFragment(final int pos) {
        if (pos == 0) return _notebook;
        if (pos == 1) return _todo;
        if (pos == 2) return _quicknote;
        if (pos == 3) return _more;
        return null;
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

    public void onViewPagerPageSelected(final int pos) {
        _bottomNav.getMenu().getItem(pos).setChecked(true);

        if (pos == tabIdToPos(R.id.nav_notebook)) {
            _fab.show();
            _cu.showSoftKeyboard(this, false);
        } else {
            _fab.hide();
            restoreDefaultToolbar();
        }

        setTitle(getPosTitle(pos));
    }

    private GsFileBrowserOptions.Options _filesystemDialogOptions = null;

    @Override
    public GsFileBrowserOptions.Options getFilesystemFragmentOptions(GsFileBrowserOptions.Options existingOptions) {
        if (_filesystemDialogOptions == null) {
            _filesystemDialogOptions = MarkorFileBrowserFactory.prepareFsViewerOpts(this, false, new GsFileBrowserOptions.SelectionListenerAdapter() {
                File toShow = null;

                @Override
                public void onFsViewerConfig(GsFileBrowserOptions.Options dopt) {
                    dopt.descModtimeInsteadOfParent = true;
                    dopt.rootFolder = _appSettings.getNotebookDirectory();
                    final File fallback = _appSettings.getFolderToLoadByMenuId(_appSettings.getAppStartupFolderMenuId());
                    final File file = MarkorContextUtils.getValidIntentFile(getIntent(), fallback);
                    if (!GsFileBrowserListAdapter.isVirtualFolder(file) && file.isFile()) {
                        dopt.startFolder = file.getParentFile();
                        toShow = file;
                    } else {
                        dopt.startFolder = file;
                    }
                    toShow = file.isFile() ? file : null;
                    dopt.doSelectMultiple = dopt.doSelectFolder = dopt.doSelectFile = true;
                    dopt.mountedStorageFolder = _cu.getStorageAccessFolder(MainActivity.this);
                }

                @Override
                public void onFsViewerDoUiUpdate(final GsFileBrowserListAdapter adapter) {
                    if (adapter != null && adapter.getCurrentFolder() != null && !TextUtils.isEmpty(adapter.getCurrentFolder().getName())) {
                        _appSettings.setFileBrowserLastBrowsedFolder(adapter.getCurrentFolder());
                        if (getCurrentPos() == tabIdToPos(R.id.nav_notebook)) {
                            setTitle(getFileBrowserTitle());
                        }
                        invalidateOptionsMenu();
                    }

                    if (toShow != null && adapter != null) {
                        adapter.showFile(toShow);
                        toShow = null;
                    }
                }

                @Override
                public void onFsViewerSelected(String request, File file, final Integer lineNumber) {
                    DocumentActivity.launch(MainActivity.this, file, null, lineNumber);
                }
            });
        }
        return _filesystemDialogOptions;
    }

    class SectionsPagerAdapter extends FragmentStateAdapter {

        SectionsPagerAdapter(FragmentManager fragMgr) {
            super(fragMgr, MainActivity.this.getLifecycle());
        }

        @NonNull
        @Override
        public Fragment createFragment(final int pos) {
            final GsFragmentBase<?, ?> frag;
            final int id = tabIdFromPos(pos);
            if (id == R.id.nav_quicknote) {
                frag = _quicknote = DocumentEditAndViewFragment.newInstance(new Document(_appSettings.getQuickNoteFile()), Document.EXTRA_FILE_LINE_NUMBER_LAST, false);
            } else if (id == R.id.nav_todo) {
                frag = _todo = DocumentEditAndViewFragment.newInstance(new Document(_appSettings.getTodoFile()), Document.EXTRA_FILE_LINE_NUMBER_LAST, false);
            } else if (id == R.id.nav_more) {
                frag = _more = MoreFragment.newInstance();
            } else {
                frag = _notebook = GsFileBrowserFragment.newInstance();
            }
            frag.setMenuVisibility(false);
            return frag;
        }

        @Override
        public int getItemCount() {
            return _bottomNav.getMenu().size();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        _cu.extractResultFromActivityResult(this, requestCode, resultCode, data);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onReceiveKeyPress(getPosFragment(getCurrentPos()), keyCode, event) ? true : super.onKeyDown(keyCode, event);
    }
}
