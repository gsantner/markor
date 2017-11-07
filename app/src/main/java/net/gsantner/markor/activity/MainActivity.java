package net.gsantner.markor.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
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
import android.widget.Toast;

import com.pixplicity.generate.OnFeedbackListener;
import com.pixplicity.generate.Rate;

import net.gsantner.markor.R;
import net.gsantner.markor.dialog.ConfirmDialog;
import net.gsantner.markor.dialog.CreateFolderDialog;
import net.gsantner.markor.dialog.FilesystemDialogCreator;
import net.gsantner.markor.model.Constants;
import net.gsantner.markor.model.DocumentLoader;
import net.gsantner.markor.model.MarkorSingleton;
import net.gsantner.markor.ui.BaseFragment;
import net.gsantner.markor.util.AppCast;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ContextUtils;
import net.gsantner.markor.util.PermissionChecker;
import net.gsantner.opoc.ui.FilesystemDialogData;
import net.gsantner.opoc.util.ActivityUtils;
import net.gsantner.opoc.util.FileUtils;

import java.io.File;
import java.io.Serializable;
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


    private FilesystemListFragment _filesystemListFragment; //TODO not populated

    private boolean _doubleBackToExitPressedOnce;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContextUtils.get().setAppLanguage(AppSettings.get().getLanguage());
        if (AppSettings.get().isOverviewStatusBarHidden()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        if (!AppSettings.get().isRememberLastDirectory()) {
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
                .setFeedbackAction(new OnFeedbackListener() {
                    public void onFeedbackTapped() {
                        ContextUtils.get().showRateOnGplayDialog();
                    }
                })
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
            case R.id.action_import: {
                if (PermissionChecker.doIfPermissionGranted(this) && PermissionChecker.mkSaveDir(this)) {
                    showImportDialog();
                }
                return true;
            }
            case R.id.action_create_folder: {
                showCreateFolderDialog();
                return true;
            }
            case R.id.action_preview: {
                // Preview QuickNote
                Intent intent = new Intent(this, DocumentActivity.class);
                intent.putExtra(DocumentActivity.EXTRA_DO_PREVIEW, true);
                intent.putExtra(DocumentLoader.EXTRA_PATH, AppSettings.get().getQuickNote());
                startActivity(intent);
                return true;
            }
        }
        return false;

    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        ContextUtils cu = ContextUtils.get();
        getMenuInflater().inflate(R.menu.main__menu, menu);

        cu.setSubMenuIconsVisiblity(menu, true);
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
            String filepath = intent.getStringExtra(Constants.EXTRA_FILEPATH); // nullable
            String action = intent.getAction();
            switch (action) {
                case AppCast.CREATE_FOLDER.ACTION: {
                    createFolder(new File(intent.getStringExtra(AppCast.CREATE_FOLDER.EXTRA_PATH)));
                    _filesystemListFragment.listFilesInDirectory(_filesystemListFragment.getCurrentDir());
                    return;
                }
                case Constants.FILESYSTEM_IMPORT_DIALOG_TAG: {
                    importFile(new File(filepath));
                    _filesystemListFragment.listFilesInDirectory(_filesystemListFragment.getCurrentDir());
                    return;
                }
                case Constants.FILESYSTEM_MOVE_DIALOG_TAG: {
                    MarkorSingleton.getInstance().moveSelectedNotes(_filesystemListFragment.getSelectedItems(), filepath);
                    _filesystemListFragment.listFilesInDirectory(_filesystemListFragment.getCurrentDir());
                    _filesystemListFragment.finishActionMode();
                    return;
                }
                case AppCast.VIEW_FOLDER_CHANGED.ACTION: {
                    File currentDir = new File(intent.getStringExtra(AppCast.VIEW_FOLDER_CHANGED.EXTRA_PATH));
                    File rootDir = new File(AppSettings.get().getSaveDirectory());
                    if (currentDir.equals(rootDir)) {
                        _toolbar.setTitle(R.string.app_name);
                    } else {
                        _toolbar.setTitle("> " + currentDir.getName());
                    }
                    if (intent.getBooleanExtra(AppCast.VIEW_FOLDER_CHANGED.EXTRA_FORCE_RELOAD, false)) {
                        _filesystemListFragment.listFilesInDirectory(currentDir);
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
                showCreateFolderDialog();
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
                    intent.putExtra(DocumentLoader.EXTRA_PATH, _filesystemListFragment.getCurrentDir());
                    intent.putExtra(DocumentLoader.EXTRA_PATH_IS_FOLDER, true);
                    startActivity(intent);
                    break;
                }
            }
        }
    }

    private void importFileToCurrentDirectory(Context context, File sourceFile) {
        FileUtils.copyFile(sourceFile, new File(_filesystemListFragment.getCurrentDir().getAbsolutePath(), sourceFile.getName()));
        Toast.makeText(context, "Imported to \"" + sourceFile.getName() + "\"",
                Toast.LENGTH_LONG).show();
    }

    private void showCreateFolderDialog() {
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
        _viewPager.getRootView().setBackgroundColor(color);
    }

    private void showImportDialog() {
        FilesystemDialogCreator.showFileDialog(new FilesystemDialogData.SelectionListenerAdapter() {
            @Override
            public void onFsSelected(String request, File file) {
                importFile(file);
                _filesystemListFragment.listFilesInDirectory(_filesystemListFragment.getCurrentDir());
            }

            @Override
            public void onFsMultiSelected(String request, File... files) {
                for (File file : files) {
                    importFile(file);
                }
                _filesystemListFragment.listFilesInDirectory(_filesystemListFragment.getCurrentDir());
            }

            @Override
            public void onFsDialogConfig(FilesystemDialogData.Options opt) {
                opt.titleText = R.string.import_from_device;
                opt.doSelectMultiple = true;
                opt.doSelectFile = true;
                opt.doSelectFolder = true;
            }
        }, getSupportFragmentManager(), this);
    }

    private void importFile(final File file) {
        if (new File(_filesystemListFragment.getCurrentDir().getAbsolutePath(), file.getName()).exists()) {
            String message = getString(R.string.confirm_overwrite_description) + "\n[" + file.getName() + "]";
            // Ask if overwriting is okay
            ConfirmDialog d = ConfirmDialog.newInstance(
                    getString(R.string.confirm_overwrite), message, file,
                    new ConfirmDialog.ConfirmDialogCallback() {
                        @Override
                        public void onConfirmDialogAnswer(boolean confirmed, Serializable data) {
                            if (confirmed) {
                                importFileToCurrentDirectory(MainActivity.this, file);
                            }
                        }
                    });
            d.show(getSupportFragmentManager(), ConfirmDialog.FRAGMENT_TAG);
        } else {
            // Import
            importFileToCurrentDirectory(this, file);
        }
    }

    @Override
    public void onBackPressed() {
        if (_doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }


        BaseFragment frag = _viewPagerAdapter.getCachedFragments().get(_viewPager.getCurrentItem());
        if (frag != null && frag.onBackPressed()) {
            return;
        }


        if (!_filesystemListFragment.onRooDir()) {
            _filesystemListFragment.goToPreviousDir();
        } else {
            _doubleBackToExitPressedOnce = true;
            new ActivityUtils(this).showSnackBar(R.string.press_again_to_exit, false, R.string.exit, new View.OnClickListener() {
                public void onClick(View view) {
                    finish();
                }
            });
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    _doubleBackToExitPressedOnce = false;
                }
            }, 2000);
        }
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
    public void onViewPagerPageSelected(int position) {
        Menu menu = _bottomNav.getMenu();
        for (int i = 0; i < menu.size(); i++) {
            if (i != position) {
                menu.getItem(i).setChecked(false);
            }
        }
        if (position == 1) {
            PermissionChecker.doIfPermissionGranted(this); // cannot prevent bottom tab selection
        }
        menu.getItem(position).setChecked(true);
        _fab.setVisibility(position == 0 ? View.VISIBLE : View.INVISIBLE);

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
                    MainActivity.this._filesystemListFragment = (FilesystemListFragment) fragment;
                    break;
                }
                case R.id.nav_quicknote: {
                    return DocumentEditFragment.newInstance(AppSettings.get().getQuickNote(), false, false);
                }
                case R.id.nav_more: {
                    return MoreFragment.newInstance();
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
