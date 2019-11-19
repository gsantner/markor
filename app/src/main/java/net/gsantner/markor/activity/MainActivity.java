/*#######################################################
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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.pixplicity.generate.Rate;

import net.gsantner.markor.BuildConfig;
import net.gsantner.markor.R;
import net.gsantner.markor.format.TextFormat;
import net.gsantner.markor.ui.FilesystemViewerCreator;
import net.gsantner.markor.ui.NewFileDialog;
import net.gsantner.markor.util.ActivityUtils;
import net.gsantner.markor.util.AppCast;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.PermissionChecker;
import net.gsantner.markor.util.ShareUtil;
import net.gsantner.opoc.activity.GsFragmentBase;
import net.gsantner.opoc.format.markdown.SimpleMarkdownParser;
import net.gsantner.opoc.ui.FilesystemViewerAdapter;
import net.gsantner.opoc.ui.FilesystemViewerData;
import net.gsantner.opoc.ui.FilesystemViewerFragment;
import net.gsantner.opoc.util.AndroidSupportMeWrapper;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import butterknife.OnPageChange;

public class MainActivity extends AppActivityBase implements FilesystemViewerFragment.FilesystemFragmentOptionsListener, BottomNavigationView.OnNavigationItemSelectedListener {

    public static boolean IS_DEBUG_ENABLED = false;

    @BindView(R.id.toolbar)
    public Toolbar _toolbar;

    @BindView(R.id.bottom_navigation_bar)
    BottomNavigationView _bottomNav;

    @BindView(R.id.fab_add_new_item)
    FloatingActionButton _fab;

    @BindView(R.id.main__view_pager_container)
    ViewPager _viewPager;
    private SectionsPagerAdapter _viewPagerAdapter;

    private boolean _doubleBackToExitPressedOnce;
    private MenuItem _lastBottomMenuItem;

    private AppSettings _appSettings;
    private ActivityUtils _contextUtils;
    private ShareUtil _shareUtil;

    private String _cachedFolderTitle;

    @SuppressLint("SdCardPath")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setExitTransition(null);
        }
        _appSettings = new AppSettings(this);
        _contextUtils = new ActivityUtils(this);
        _shareUtil = new ShareUtil(this);
        _contextUtils.setAppLanguage(_appSettings.getLanguage());
        if (_appSettings.isOverviewStatusBarHidden()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        if (!_appSettings.isLoadLastDirectoryAtStartup()) {
            _appSettings.setLastOpenedDirectory(null);
        }
        setTheme(_appSettings.isDarkThemeEnabled() ? R.style.AppTheme_Dark : R.style.AppTheme_Light);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main__activity);
        ButterKnife.bind(this);
        setSupportActionBar(_toolbar);

        optShowRate();

        try {
            if (_appSettings.isAppCurrentVersionFirstStart(true)) {
                SimpleMarkdownParser smp = SimpleMarkdownParser.get().setDefaultSmpFilter(SimpleMarkdownParser.FILTER_ANDROID_TEXTVIEW);
                String html = "";
                html += smp.parse(getString(R.string.copyright_license_text_official).replace("\n", "  \n"), "").getHtml();
                html += "<br/><br/><br/><big><big>" + getString(R.string.changelog) + "</big></big><br/>" + smp.parse(getResources().openRawResource(R.raw.changelog), "", SimpleMarkdownParser.FILTER_ANDROID_TEXTVIEW, SimpleMarkdownParser.FILTER_CHANGELOG);
                html += "<br/><br/><br/><big><big>" + getString(R.string.licenses) + "</big></big><br/>" + smp.parse(getResources().openRawResource(R.raw.licenses_3rd_party), "").getHtml();
                ActivityUtils _au = new ActivityUtils(this);
                _au.showDialogWithHtmlTextView(0, html);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        IntroActivity.optStart(this);

        // Setup viewpager
        _viewPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        _viewPager.setAdapter(_viewPagerAdapter);
        _viewPager.setOffscreenPageLimit(4);
        _bottomNav.setOnNavigationItemSelectedListener(this);

        // noinspection PointlessBooleanExpression - Send Test intent
        if (BuildConfig.IS_TEST_BUILD && false) {
            DocumentActivity.launch(this, new File("/sdcard/Documents/mordor/aa-beamer.md"), false, true, null);
        }

        (new ActivityUtils(this)).applySpecialLaunchersVisibility(_appSettings.isSpecialFileLaunchersEnabled());

        _bottomNav.postDelayed(() -> {
            if (_appSettings.getAppStartupTab() != R.id.nav_notebook) {
                _bottomNav.setSelectedItemId(_appSettings.getAppStartupTab());
            }
        }, 1);
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
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        AppSettings as = new AppSettings(this);
        switch (item.getItemId()) {
            case R.id.action_preview: {
                File f = _bottomNav.getSelectedItemId() == R.id.nav_quicknote ? as.getQuickNoteFile() : as.getTodoFile();
                DocumentActivity.launch(MainActivity.this, f, false, true, null);
                return true;
            }
            case R.id.action_settings: {
                new ActivityUtils(this).animateToActivity(SettingsActivity.class, false, null);
                return true;
            }
        }
        return false;

    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.main__menu, menu);

        menu.findItem(R.id.action_settings).setVisible(_appSettings.isShowSettingsOptionInMainToolbar());

        _contextUtils.tintMenuItems(menu, true, Color.WHITE);
        _contextUtils.setSubMenuIconsVisiblity(menu, true);
        return true;
    }

    @Override
    protected void onResume() {
        new AndroidSupportMeWrapper(this).mainOnResume();
        super.onResume();
        IS_DEBUG_ENABLED = BuildConfig.IS_TEST_BUILD || _appSettings.isDebugLogEnabled();
        if (_appSettings.isRecreateMainRequired()) {
            // recreate(); // does not remake fragments
            Intent intent = getIntent();
            overridePendingTransition(0, 0);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            finish();
            overridePendingTransition(0, 0);
            startActivity(intent);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && _appSettings.isMultiWindowEnabled()) {
            setTaskDescription(new ActivityManager.TaskDescription(getString(R.string.app_name)));
        }

        int color = ContextCompat.getColor(this, _appSettings.isDarkThemeEnabled()
                ? R.color.dark__background : R.color.light__background);
        _viewPager.getRootView().setBackgroundColor(color);

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(_localBroadcastReceiver, AppCast.getLocalBroadcastFilter());

        if (_appSettings.isKeepScreenOn()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        cacheCurrentFolder();
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.unregisterReceiver(_localBroadcastReceiver);
    }

    private BroadcastReceiver _localBroadcastReceiver = new BroadcastReceiver() {
        @SuppressWarnings("unchecked")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action == null ? "" : action) {
                case AppCast.VIEW_FOLDER_CHANGED.ACTION: {
                    File currentDir = new File(intent.getStringExtra(AppCast.VIEW_FOLDER_CHANGED.EXTRA_PATH));
                    File rootDir = _appSettings.getNotebookDirectory();
                    if (currentDir.equals(rootDir)) {
                        _toolbar.setTitle(R.string.app_name);
                    } else {
                        _toolbar.setTitle("> " + currentDir.getName());
                    }
                    _cachedFolderTitle = _toolbar.getTitle().toString();
                    return;
                }
            }
        }
    };

    @Override
    @SuppressWarnings("unused")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Determine some results and forward using Local Broadcast
        Object result = _shareUtil.extractResultFromActivityResult(requestCode, resultCode, data, this);


        try {
            FilesystemViewerFragment frag = (FilesystemViewerFragment) _viewPagerAdapter.getFragmentByTag(FilesystemViewerFragment.FRAGMENT_TAG);
            frag.getAdapter().reconfigure();
        } catch (Exception ignored) {
            recreate();
        }
    }

    @OnLongClick({R.id.fab_add_new_item})
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
    @OnClick({R.id.fab_add_new_item})
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
                    if (_shareUtil.isUnderStorageAccessFolder(fsFrag.getCurrentFolder()) && _shareUtil.getStorageAccessFrameworkTreeUri() == null) {
                        _shareUtil.showMountSdDialog(this);
                        return;
                    }

                    if (!fsFrag.getAdapter().isCurrentFolderWriteable()) {
                        return;
                    }

                    NewFileDialog dialog = NewFileDialog.newInstance(fsFrag.getCurrentFolder(), (ok, f) -> {
                        if (ok) {
                            if (f.isFile()) {
                                DocumentActivity.launch(MainActivity.this, f, false, false, null);
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
        updateFabVisibility(item.getItemId() == R.id.nav_notebook);
        PermissionChecker permc = new PermissionChecker(this);

        switch (item.getItemId()) {
            case R.id.nav_notebook: {
                _viewPager.setCurrentItem(0);
                _toolbar.setTitle(_cachedFolderTitle);
                return true;
            }

            case R.id.nav_todo: {
                permc.doIfExtStoragePermissionGranted(); // cannot prevent bottom tab selection
                restoreDefaultToolbar();
                _viewPager.setCurrentItem(1);
                _toolbar.setTitle(R.string.todo);
                return true;
            }
            case R.id.nav_quicknote: {
                permc.doIfExtStoragePermissionGranted(); // cannot prevent bottom tab selection
                restoreDefaultToolbar();
                _viewPager.setCurrentItem(2);
                _toolbar.setTitle(R.string.quicknote);
                return true;
            }
            case R.id.nav_more: {
                restoreDefaultToolbar();
                _viewPager.setCurrentItem(3);
                _toolbar.setTitle(R.string.more);
                return true;
            }
        }
        return false;
    }

    public void updateFabVisibility(boolean visible) {
        if (visible) {
            _fab.show();
        } else {
            _fab.hide();
        }
    }

    @OnPageChange(value = R.id.main__view_pager_container, callback = OnPageChange.Callback.PAGE_SELECTED)
    public void onViewPagerPageSelected(int pos) {
        Menu menu = _bottomNav.getMenu();
        PermissionChecker permc = new PermissionChecker(this);
        (_lastBottomMenuItem != null ? _lastBottomMenuItem : menu.getItem(0)).setChecked(false);
        _lastBottomMenuItem = menu.getItem(pos).setChecked(true);
        updateFabVisibility(pos == 0);
        _toolbar.setTitle(new String[]{_cachedFolderTitle, getString(R.string.todo), getString(R.string.quicknote), getString(R.string.more)}[pos]);

        if (pos > 0 && pos < 3) {
            permc.doIfExtStoragePermissionGranted(); // cannot prevent bottom tab selection
        }
    }

    private FilesystemViewerData.Options _filesystemDialogOptions = null;

    @Override
    public FilesystemViewerData.Options getFilesystemFragmentOptions(FilesystemViewerData.Options existingOptions) {
        if (_filesystemDialogOptions == null) {
            _filesystemDialogOptions = FilesystemViewerCreator.prepareFsViewerOpts(getApplicationContext(), false, new FilesystemViewerData.SelectionListenerAdapter() {
                @Override
                public void onFsViewerConfig(FilesystemViewerData.Options dopt) {
                    dopt.descModtimeInsteadOfParent = true;
                    //opt.rootFolder = _appSettings.getNotebookDirectory();
                    dopt.rootFolder = _appSettings.getFolderToLoadByMenuId(_appSettings.getAppStartupFolderMenuId());
                    dopt.folderFirst = _appSettings.isFilesystemListFolderFirst();
                    dopt.doSelectMultiple = dopt.doSelectFolder = dopt.doSelectFile = true;
                    dopt.mountedStorageFolder = _shareUtil.getStorageAccessFolder();
                    dopt.showDotFiles = _appSettings.isShowDotFiles();
                    dopt.fileComparable = FilesystemViewerFragment.sortFolder(null);
                }

                @Override
                public void onFsViewerDoUiUpdate(FilesystemViewerAdapter adapter) {
                    if (adapter != null && adapter.getCurrentFolder() != null && !TextUtils.isEmpty(adapter.getCurrentFolder().getName())) {
                        cacheCurrentFolder();
                        _toolbar.setTitle(adapter.areItemsSelected() ? "" : _cachedFolderTitle);
                        invalidateOptionsMenu();

                        if (adapter.getCurrentFolder().equals(FilesystemViewerAdapter.VIRTUAL_STORAGE_FAVOURITE)) {
                            adapter.getFsOptions().favouriteFiles = _appSettings.getFavouriteFiles();
                        }
                    }
                }

                @Override
                public void onFsViewerSelected(String request, File file) {
                    if (TextFormat.isTextFile(file)) {
                        DocumentActivity.launch(MainActivity.this, file, false, null, null);
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
        public Fragment getItem(int pos) {
            GsFragmentBase fragment = _fragCache.get(pos);
            switch (_bottomNav.getMenu().getItem(pos).getItemId()) {
                default:
                case R.id.nav_notebook: {
                    if (fragment == null) {
                        fragment = FilesystemViewerFragment.newInstance(getFilesystemFragmentOptions(null));
                    }
                    break;
                }
                case R.id.nav_quicknote: {
                    if (fragment == null) {
                        fragment = DocumentEditFragment.newInstance(_appSettings.getQuickNoteFile(), false, false);
                    }
                    break;
                }
                case R.id.nav_todo: {
                    if (fragment == null) {
                        fragment = DocumentEditFragment.newInstance(_appSettings.getTodoFile(), false, false);
                    }
                    break;
                }
                case R.id.nav_more: {
                    if (fragment == null) {
                        fragment = MoreFragment.newInstance();
                    }
                    break;
                }
            }

            _fragCache.put(pos, fragment);
            return fragment;
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
    protected void onStop() {
        super.onStop();

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

    private void cacheCurrentFolder() {
        FilesystemViewerFragment fragment = (FilesystemViewerFragment) _viewPagerAdapter.getFragmentByTag(FilesystemViewerFragment.FRAGMENT_TAG);
        _cachedFolderTitle = (fragment != null) ? fragment.getCurrentFolder().getName() : getResources().getString(R.string.app_name).toLowerCase();
    }
}
