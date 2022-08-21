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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.pixplicity.generate.Rate;

import net.gsantner.markor.BuildConfig;
import net.gsantner.markor.R;
import net.gsantner.markor.format.TextFormat;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.FilesystemViewerCreator;
import net.gsantner.markor.ui.NewFileDialog;
import net.gsantner.markor.util.ActivityUtils;
import net.gsantner.markor.util.PermissionChecker;
import net.gsantner.markor.util.ShareUtil;
import net.gsantner.opoc.activity.GsFragmentBase;
import net.gsantner.opoc.format.markdown.SimpleMarkdownParser;
import net.gsantner.opoc.ui.FilesystemViewerAdapter;
import net.gsantner.opoc.ui.FilesystemViewerData;
import net.gsantner.opoc.ui.FilesystemViewerFragment;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

import other.writeily.widget.WrMarkorWidgetProvider;

public class MainActivity extends MarkorBaseActivity implements FilesystemViewerFragment.FilesystemFragmentOptionsListener, BottomNavigationView.OnNavigationItemSelectedListener {

    public static boolean IS_DEBUG_ENABLED = false;

    private BottomNavigationView _bottomNav;
    private ViewPager _viewPager;
    private SectionsPagerAdapter _viewPagerAdapter;
    private FloatingActionButton _fab;

    private boolean _doubleBackToExitPressedOnce;
    private ShareUtil _shareUtil;
    private int _prevPos = -1;

    @SuppressLint("SdCardPath")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IS_DEBUG_ENABLED |= BuildConfig.IS_TEST_BUILD;
        _shareUtil = new ShareUtil(this);
        setContentView(R.layout.main__activity);
        _bottomNav = findViewById(R.id.bottom_navigation_bar);
        _viewPager = findViewById(R.id.main__view_pager_container);
        _fab = findViewById(R.id.fab_add_new_item);
        _fab.setOnClickListener(this::onClickFab);
        _fab.setOnLongClickListener(this::onLongClickFab);
        _viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                onViewPagerPageSelected(position);
            }
        });

        setSupportActionBar(findViewById(R.id.toolbar));
        optShowRate();

        // Setup viewpager
        _viewPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        _viewPager.setAdapter(_viewPagerAdapter);
        _viewPager.setOffscreenPageLimit(4);
        _bottomNav.setOnNavigationItemSelectedListener(this);
        _prevPos = tabIdToPos(R.id.nav_notebook); // Default

        // noinspection PointlessBooleanExpression - Send Test intent
        if (BuildConfig.IS_TEST_BUILD && false) {
            DocumentActivity.launch(this, new File("/sdcard/Documents/mordor/aa-beamer.md"), true, null, null);
        }

        (new ActivityUtils(this)).applySpecialLaunchersVisibility(_appSettings.isSpecialFileLaunchersEnabled());

        // Switch to tab if specific folder _not_ requested, and not recreating from saved instance
        final int startTab = _appSettings.getAppStartupTab();
        if (startTab != R.id.nav_notebook && savedInstanceState == null && getIntentDir(getIntent(), null) == null) {
            _bottomNav.postDelayed(() -> _bottomNav.setSelectedItemId(startTab), 10);
        }
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);
        final File dir = getIntentDir(intent, null);
        final FilesystemViewerFragment frag = (FilesystemViewerFragment) _viewPagerAdapter.getFragmentByTag(FilesystemViewerFragment.FRAGMENT_TAG);
        if (frag != null && dir != null) {
            frag.getAdapter().setCurrentFolder(dir, false);
            _bottomNav.postDelayed(() -> _bottomNav.setSelectedItemId(R.id.nav_notebook), 10);
        }
    }

    private static File getIntentDir(final Intent intent, final File fallback) {
        if (intent == null) {
            return fallback;
        }

        // By extra path
        final File file = (File) intent.getSerializableExtra(Document.EXTRA_PATH);
        if (file != null && file.isDirectory()) {
            return (File) intent.getSerializableExtra(Document.EXTRA_PATH);
        }

        // By url in data
        try {
            final File dir = new File(intent.getData().getPath());
            if (dir.exists() && dir.isDirectory()) {
                return dir;
            }
        } catch (NullPointerException ignored) {
        }

        return fallback;
    }

    private void optShowRate() {
        new Rate.Builder(this)
                .setTriggerCount(4)
                .setMinimumInstallTime((int) TimeUnit.MINUTES.toMillis(30))
                .setFeedbackAction(() -> new ActivityUtils(this).showGooglePlayEntryForThisApp())
                .build().count().showRequest();
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionChecker permc = new PermissionChecker(this);
        permc.checkPermissionResult(requestCode, permissions, grantResults);

        if (_shareUtil.checkExternalStoragePermission(false)) {
            restartMainActivity();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        if (item.getItemId() == R.id.action_settings) {
            new ActivityUtils(this).animateToActivity(SettingsActivity.class, false, null).freeContextRef();
            return true;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.main__menu, menu);

        menu.findItem(R.id.action_settings).setVisible(_appSettings.isShowSettingsOptionInMainToolbar());

        _activityUtils.tintMenuItems(menu, true, Color.WHITE);
        _activityUtils.setSubMenuIconsVisiblity(menu, true);
        return true;
    }

    @Override
    protected void onResume() {
        //new AndroidSupportMeWrapper(this).mainOnResume();
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && _appSettings.isMultiWindowEnabled()) {
            setTaskDescription(new ActivityManager.TaskDescription(getString(R.string.app_name)));
        }

        if (_appSettings.isKeepScreenOn()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        boolean firstStart = IntroActivity.optStart(this);
        try {
            if (!firstStart && new PermissionChecker(this).doIfExtStoragePermissionGranted() && _appSettings.isAppCurrentVersionFirstStart(true)) {
                SimpleMarkdownParser smp = SimpleMarkdownParser.get().setDefaultSmpFilter(SimpleMarkdownParser.FILTER_ANDROID_TEXTVIEW);
                String html = "";
                html += smp.parse(getString(R.string.copyright_license_text_official).replace("\n", "  \n"), "").getHtml();
                html += "<br/><br/><br/><big><big>" + getString(R.string.changelog) + "</big></big><br/>" + smp.parse(getResources().openRawResource(R.raw.changelog), "", SimpleMarkdownParser.FILTER_ANDROID_TEXTVIEW);
                html += "<br/><br/><br/><big><big>" + getString(R.string.licenses) + "</big></big><br/>" + smp.parse(getResources().openRawResource(R.raw.licenses_3rd_party), "").getHtml();
                ActivityUtils _au = new ActivityUtils(this);
                _au.showDialogWithHtmlTextView(0, html);
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

    @Override
    @SuppressWarnings("unused")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Determine some results and forward using Local Broadcast
        Object result = _shareUtil.extractResultFromActivityResult(requestCode, resultCode, data, this);

        boolean restart = (requestCode == ShareUtil.REQUEST_STORAGE_PERMISSION_R && ((boolean) result));
        restart |= requestCode == IntroActivity.REQ_CODE_APPINTRO && _shareUtil.checkExternalStoragePermission(false);
        if (restart) {
            restartMainActivity();
        }

        try {
            FilesystemViewerFragment frag = (FilesystemViewerFragment) _viewPagerAdapter.getFragmentByTag(FilesystemViewerFragment.FRAGMENT_TAG);
            frag.getAdapter().reconfigure();
        } catch (Exception ignored) {
            recreate();
        }
    }

    public boolean onLongClickFab(View view) {
        PermissionChecker permc = new PermissionChecker(this);
        FilesystemViewerFragment fsFrag = (FilesystemViewerFragment) _viewPagerAdapter.getFragmentByTag(FilesystemViewerFragment.FRAGMENT_TAG);
        if (fsFrag != null && permc.mkdirIfStoragePermissionGranted()) {
            fsFrag.getAdapter().setCurrentFolder(fsFrag.getCurrentFolder().equals(FilesystemViewerAdapter.VIRTUAL_STORAGE_RECENTS)
                            ? FilesystemViewerAdapter.VIRTUAL_STORAGE_FAVOURITE : FilesystemViewerAdapter.VIRTUAL_STORAGE_RECENTS
                    , true);
        }
        return true;
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    public void onClickFab(View view) {
        PermissionChecker permc = new PermissionChecker(this);
        FilesystemViewerFragment fsFrag = (FilesystemViewerFragment) _viewPagerAdapter.getFragmentByTag(FilesystemViewerFragment.FRAGMENT_TAG);
        if (fsFrag == null) {
            return;
        }

        if (fsFrag.getAdapter().isCurrentFolderVirtual()) {
            fsFrag.getAdapter().loadFolder(_appSettings.getNotebookDirectory());
            return;
        }
        if (permc.mkdirIfStoragePermissionGranted()) {
            switch (view.getId()) {
                case R.id.fab_add_new_item: {
                    if (_shareUtil.isUnderStorageAccessFolder(fsFrag.getCurrentFolder(), true) && _shareUtil.getStorageAccessFrameworkTreeUri() == null) {
                        _shareUtil.showMountSdDialog(this);
                        return;
                    }

                    if (!fsFrag.getAdapter().isCurrentFolderWriteable()) {
                        return;
                    }

                    NewFileDialog dialog = NewFileDialog.newInstance(fsFrag.getCurrentFolder(), true, (ok, f) -> {
                        if (ok) {
                            if (f.isFile()) {
                                DocumentActivity.launch(MainActivity.this, f, false, null, null);
                            } else if (f.isDirectory()) {
                                FilesystemViewerFragment wrFragment = (FilesystemViewerFragment) _viewPagerAdapter.getFragmentByTag(FilesystemViewerFragment.FRAGMENT_TAG);
                                if (wrFragment != null) {
                                    wrFragment.reloadCurrentFolder();
                                }
                            }
                        }
                    });
                    dialog.show(getSupportFragmentManager(), NewFileDialog.FRAGMENT_TAG);
                    break;
                }
            }
        }
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
        GsFragmentBase frag = _viewPagerAdapter.getCachedFragments().get(_viewPager.getCurrentItem());
        if (frag != null && frag.onBackPressed()) {
            return;
        }

        // Confirm exit with back / snackbar
        _doubleBackToExitPressedOnce = true;
        new ActivityUtils(this).showSnackBar(R.string.press_back_again_to_exit, false, R.string.exit, view -> finish());
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

        if (_prevPos == tabIdToPos(R.id.nav_quicknote) || _prevPos == tabIdToPos(R.id.nav_todo)) {
            ((DocumentEditFragment) _viewPagerAdapter.getItem(_prevPos)).pause();
        }

        if (pos == tabIdToPos(R.id.nav_quicknote) || pos == tabIdToPos(R.id.nav_todo)) {
            new PermissionChecker(this).doIfExtStoragePermissionGranted();
            ((DocumentEditFragment) _viewPagerAdapter.getItem(pos)).resume();
        }

        _prevPos = pos;
    }

    private FilesystemViewerData.Options _filesystemDialogOptions = null;

    @Override
    public FilesystemViewerData.Options getFilesystemFragmentOptions(FilesystemViewerData.Options existingOptions) {
        if (_filesystemDialogOptions == null) {
            _filesystemDialogOptions = FilesystemViewerCreator.prepareFsViewerOpts(this, false, new FilesystemViewerData.SelectionListenerAdapter() {
                @Override
                public void onFsViewerConfig(FilesystemViewerData.Options dopt) {
                    dopt.descModtimeInsteadOfParent = true;
                    dopt.rootFolder = _appSettings.getNotebookDirectory();
                    dopt.startFolder = getIntentDir(getIntent(), _appSettings.getFolderToLoadByMenuId(_appSettings.getAppStartupFolderMenuId()));
                    dopt.folderFirst = _appSettings.isFilesystemListFolderFirst();
                    dopt.doSelectMultiple = dopt.doSelectFolder = dopt.doSelectFile = true;
                    dopt.mountedStorageFolder = _shareUtil.getStorageAccessFolder();
                    dopt.showDotFiles = _appSettings.isShowDotFiles();
                    dopt.fileComparable = FilesystemViewerFragment.sortFolder(null);

                    dopt.favouriteFiles = _appSettings.getFavouriteFiles();
                    dopt.recentFiles = _appSettings.getAsFileList(_appSettings.getRecentDocuments());
                    dopt.popularFiles = _appSettings.getAsFileList(_appSettings.getPopularDocuments());
                }

                @Override
                public void onFsViewerDoUiUpdate(FilesystemViewerAdapter adapter) {
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
                    if (TextFormat.isTextFile(file)) {
                        DocumentActivity.launch(MainActivity.this, file, null, null, lineNumber);
                    } else if (file.getName().toLowerCase().endsWith(".apk")) {
                        _shareUtil.requestApkInstallation(file);
                    } else {
                        DocumentActivity.askUserIfWantsToOpenFileInThisApp(MainActivity.this, file);
                    }
                }
            });
        }
        return _filesystemDialogOptions;
    }

    class SectionsPagerAdapter extends FragmentPagerAdapter {
        private HashMap<Integer, GsFragmentBase> _fragCache = new LinkedHashMap<>();

        SectionsPagerAdapter(FragmentManager fragMgr) {
            super(fragMgr);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            GsFragmentBase fragment = (GsFragmentBase) super.instantiateItem(container, position);
            _fragCache.put(position, fragment);
            return fragment;
        }

        @Override
        public Fragment getItem(int pos) {
            final Fragment existing = _fragCache.get(pos);
            if (existing != null) {
                return existing;
            }

            switch (_bottomNav.getMenu().getItem(pos).getItemId()) {
                default:
                case R.id.nav_notebook: {
                    return FilesystemViewerFragment.newInstance(getFilesystemFragmentOptions(null));
                }
                case R.id.nav_quicknote: {
                    return DocumentEditFragment.newInstance(_appSettings.getQuickNoteFile(), -1);
                }
                case R.id.nav_todo: {
                    return DocumentEditFragment.newInstance(_appSettings.getTodoFile(), -1);
                }
                case R.id.nav_more: {
                    return MoreFragment.newInstance();
                }
            }
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            super.destroyItem(container, position, object);
            _fragCache.remove(position);
        }

        @Override
        public int getCount() {
            return _bottomNav.getMenu().size();
        }

        public GsFragmentBase getFragmentByTag(String fragmentTag) {
            for (GsFragmentBase frag : _fragCache.values()) {
                if (fragmentTag.equals(frag.getFragmentTag())) {
                    return frag;
                }
            }
            return null;
        }

        public HashMap<Integer, GsFragmentBase> getCachedFragments() {
            return _fragCache;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        ((DocumentEditFragment) _viewPagerAdapter.getItem(tabIdToPos(R.id.nav_todo))).pause();
        ((DocumentEditFragment) _viewPagerAdapter.getItem(tabIdToPos(R.id.nav_quicknote))).pause();
        WrMarkorWidgetProvider.updateLauncherWidgets();
    }

    @Override
    protected void onStop() {
        super.onStop();
        ((DocumentEditFragment) _viewPagerAdapter.getItem(tabIdToPos(R.id.nav_todo))).resume();
        ((DocumentEditFragment) _viewPagerAdapter.getItem(tabIdToPos(R.id.nav_quicknote))).resume();
        restoreDefaultToolbar();
    }

    /**
     * Restores the default toolbar. Used when changing the tab or moving to another activity
     * while {@link FilesystemViewerFragment} action mode is active (e.g. when renaming a file)
     */
    private void restoreDefaultToolbar() {
        FilesystemViewerFragment wrFragment = (FilesystemViewerFragment) _viewPagerAdapter.getFragmentByTag(FilesystemViewerFragment.FRAGMENT_TAG);
        if (wrFragment != null) {
            wrFragment.clearSelection();
        }
    }
}
