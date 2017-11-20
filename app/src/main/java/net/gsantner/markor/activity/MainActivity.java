package net.gsantner.markor.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
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
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.pixplicity.generate.Rate;

import net.gsantner.markor.R;
import net.gsantner.markor.ui.BaseFragment;
import net.gsantner.markor.util.ActivityUtils;
import net.gsantner.markor.util.AppCast;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ContextUtils;
import net.gsantner.markor.util.DocumentIO;
import net.gsantner.markor.util.PermissionChecker;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import butterknife.OnPageChange;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContextUtils.get().setAppLanguage(AppSettings.get().getLanguage());
        if (AppSettings.get().isOverviewStatusBarHidden()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        if (!AppSettings.get().isLoadLastDirectoryAtStartup()) {
            AppSettings.get().setLastOpenedDirectory(null);
        }
        setContentView(R.layout.main__activity);
        ButterKnife.bind(this);
        setSupportActionBar(_toolbar);

        optShowRate();


        // Setup viewpager
        _viewPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        _viewPager.setAdapter(_viewPagerAdapter);
        _bottomNav.setOnNavigationItemSelectedListener(this);
    }

    private void optShowRate() {
        new Rate.Builder(this)
                .setTriggerCount(4)
                .setMinimumInstallTime((int) TimeUnit.MINUTES.toMillis(30))
                .setFeedbackAction(() -> new ActivityUtils(MainActivity.this).showRateOnGplayDialog())
                .build().count().showRequest();
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionChecker.checkPermissionResult(this, requestCode, permissions, grantResults);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.action_preview: {
                // Preview QuickNote
                Intent intent = new Intent(this, DocumentActivity.class);
                intent.putExtra(DocumentActivity.EXTRA_DO_PREVIEW, true);
                intent.putExtra(DocumentIO.EXTRA_PATH, AppSettings.get().getQuickNoteFile());
                startActivity(intent);
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
        ContextUtils cu = ContextUtils.get();
        AppSettings as = AppSettings.get();
        getMenuInflater().inflate(R.menu.main__menu, menu);

        menu.findItem(R.id.action_settings).setVisible(as.isShowSettingsOptionInMainToolbar());

        cu.tintMenuItems(menu, true, Color.WHITE);
        cu.setSubMenuIconsVisiblity(menu, true);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (AppSettings.get().isRecreateMainRequired()) {
            // recreate(); // does not remake fragments
            Intent intent = getIntent();
            overridePendingTransition(0, 0);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            finish();
            overridePendingTransition(0, 0);
            startActivity(intent);
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

    private BroadcastReceiver _localBroadcastReceiver = new BroadcastReceiver() {
        @SuppressWarnings("unchecked")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action == null ? "" : action) {
                case AppCast.VIEW_FOLDER_CHANGED.ACTION: {
                    File currentDir = new File(intent.getStringExtra(AppCast.VIEW_FOLDER_CHANGED.EXTRA_PATH));
                    File rootDir = AppSettings.get().getNotebookDirectory();
                    if (currentDir.equals(rootDir)) {
                        _toolbar.setTitle(R.string.app_name);
                    } else {
                        _toolbar.setTitle("> " + currentDir.getName());
                    }
                    return;
                }
            }
        }
    };

    @OnLongClick({R.id.fab_add_new_item})
    public boolean onLongClickedFab(View view) {
        switch (view.getId()) {
            case R.id.fab_add_new_item: {
                if (_viewPagerAdapter.getFragmentByTag(FilesystemListFragment.FRAGMENT_TAG) != null) {
                    ((FilesystemListFragment) _viewPagerAdapter.getFragmentByTag(FilesystemListFragment.FRAGMENT_TAG))
                            .showCreateFolderDialog();
                }
                return true;
            }
        }
        return false;
    }

    @OnClick({R.id.fab_add_new_item})
    public void onClickFab(View view) {
        if (PermissionChecker.doIfPermissionGranted(this) && PermissionChecker.mkSaveDir(this)) {
            switch (view.getId()) {
                case R.id.fab_add_new_item: {
                    Intent intent = new Intent(this, DocumentActivity.class);
                    if (_viewPagerAdapter.getFragmentByTag(FilesystemListFragment.FRAGMENT_TAG) != null) {
                        File path = ((FilesystemListFragment) _viewPagerAdapter.getFragmentByTag(FilesystemListFragment.FRAGMENT_TAG))
                                .getCurrentDir();
                        intent.putExtra(DocumentIO.EXTRA_PATH, path);
                    } else {
                        intent.putExtra(DocumentIO.EXTRA_PATH, AppSettings.get().getNotebookDirectory());
                    }
                    intent.putExtra(DocumentIO.EXTRA_PATH_IS_FOLDER, true);
                    startActivity(intent);
                    break;
                }
            }
        }
    }

    private void setupAppearancePreferences() {
        int color = ContextCompat.getColor(this, AppSettings.get().isDarkThemeEnabled()
                ? R.color.dark__background : R.color.light__background);
        _viewPager.getRootView().setBackgroundColor(color);
    }

    @Override
    public void onBackPressed() {
        // Exit confirmed with 2xBack
        if (_doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        // Check if fragment handled back press
        BaseFragment frag = _viewPagerAdapter.getCachedFragments().get(_viewPager.getCurrentItem());
        if (frag != null && frag.onBackPressed()) {
            return;
        }

        // Confirm exit with back / snackbar
        _doubleBackToExitPressedOnce = true;
        new ActivityUtils(this).showSnackBar(R.string.press_again_to_exit, false, R.string.exit, view -> finish());
        new Handler().postDelayed(() -> _doubleBackToExitPressedOnce = false, 2000);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        _fab.setVisibility(item.getItemId() == R.id.nav_notebook ? View.VISIBLE : View.INVISIBLE);
        switch (item.getItemId()) {
            case R.id.nav_notebook: {
                _viewPager.setCurrentItem(0);
                return true;
            }
            case R.id.nav_quicknote: {
                PermissionChecker.doIfPermissionGranted(this); // cannot prevent bottom tab selection
                _viewPager.setCurrentItem(1);
                return true;
            }
            case R.id.nav_more: {
                _viewPager.setCurrentItem(2);
                return true;
            }
        }
        return false;
    }

    @OnPageChange(value = R.id.main__view_pager_container, callback = OnPageChange.Callback.PAGE_SELECTED)
    public void onViewPagerPageSelected(int pos) {
        Menu menu = _bottomNav.getMenu();
        (_lastBottomMenuItem != null ? _lastBottomMenuItem : menu.getItem(0)).setChecked(false);
        _lastBottomMenuItem = menu.getItem(pos).setChecked(true);
        _fab.setVisibility(pos == 0 ? View.VISIBLE : View.INVISIBLE);

        if (pos == 1) {
            PermissionChecker.doIfPermissionGranted(this); // cannot prevent bottom tab selection
        }
    }


    class SectionsPagerAdapter extends FragmentPagerAdapter {
        private HashMap<Integer, BaseFragment> _fragCache = new LinkedHashMap<>();

        SectionsPagerAdapter(FragmentManager fragMgr) {
            super(fragMgr);
        }

        @Override
        public Fragment getItem(int pos) {
            BaseFragment fragment = null;
            switch (_bottomNav.getMenu().getItem(pos).getItemId()) {
                default:
                case R.id.nav_notebook: {
                    fragment = new FilesystemListFragment();
                    break;
                }
                case R.id.nav_quicknote: {
                    fragment = DocumentEditFragment.newInstance(AppSettings.get().getQuickNoteFile(), false, false);
                    break;
                }
                case R.id.nav_more: {
                    fragment = MoreFragment.newInstance();
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

        public BaseFragment getFragmentByTag(String fragmentTag) {
            for (BaseFragment frag : _fragCache.values()) {
                if (fragmentTag.equals(frag.getFragmentTag())) {
                    return frag;
                }
            }
            return null;
        }

        public HashMap<Integer, BaseFragment> getCachedFragments() {
            return _fragCache;
        }
    }
}
