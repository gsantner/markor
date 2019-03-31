/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License: Apache 2.0 / Commercial
 *  https://github.com/gsantner/opoc/#licensing
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
/*
 * Revision 001 of FilesystemDialogCreator
 * A simple filesystem dialog with file, folder and multiple selection
 * most bits (color, text, images) can be controller using FilesystemDialogData.
 * The data container contains a listener callback for results.
 * Most features are usable without any additional project files and resources
 *
 * Required: Butterknife library
 */
package net.gsantner.opoc.ui;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import net.gsantner.markor.R;
import net.gsantner.markor.format.markdown.MarkdownTextConverter;
import net.gsantner.markor.ui.FileInfoDialog;
import net.gsantner.markor.ui.FilesystemDialogCreator;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ContextUtils;
import net.gsantner.markor.util.PermissionChecker;
import net.gsantner.opoc.activity.GsFragmentBase;
import net.gsantner.opoc.util.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import other.writeily.model.WrMarkorSingleton;
import other.writeily.ui.WrConfirmDialog;
import other.writeily.ui.WrRenameDialog;

public class FilesystemFragment extends GsFragmentBase
        implements FilesystemDialogData.SelectionListener {
    //########################
    //## Static
    //########################
    public static final String FRAGMENT_TAG = "FilesystemFragment";

    public static final int SORT_BY_DATE = 0;
    public static final int SORT_BY_NAME = 1;
    public static final int SORT_BY_FILESIZE = 2;

    public static FilesystemFragment newInstance(FilesystemDialogData.Options options) {
        FilesystemFragment f = new FilesystemFragment();
        options.listener.onFsDialogConfig(options);
        return f;
    }

    //########################
    //## Member
    //########################
    @BindView(R.id.ui__filesystem_dialog__list)
    RecyclerView _recyclerList;

    @BindView(R.id.pull_to_refresh)
    public SwipeRefreshLayout swipe;

    private FilesystemDialogAdapter _filesystemDialogAdapter;
    private FilesystemDialogData.Options _dopt;
    private FilesystemDialogData.SelectionListener _callback;
    private File _initialRootFolder = null;
    private boolean firstResume = true;
    private AppSettings _appSettings;
    private ContextUtils _contextUtils;
    private Menu _fragmentMenu;

    //########################
    //## Methods
    //########################

    public interface FilesystemFragmentOptionsListener {
        FilesystemDialogData.Options getFilesystemFragmentOptions(FilesystemDialogData.Options existingOptions);
    }

    @Override
    public void onViewCreated(View root, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);
        Context context = getContext();
        ButterKnife.bind(this, root);
        _appSettings = new AppSettings(root.getContext());
        _contextUtils = new ContextUtils(root.getContext());

        if (!(getActivity() instanceof FilesystemFragmentOptionsListener)) {
            throw new RuntimeException("Error: " + getActivity().getClass().getName() + " doesn't implement FilesystemFragmentOptionsListener");
        }
        setDialogOptions(((FilesystemFragmentOptionsListener) getActivity()).getFilesystemFragmentOptions(_dopt));

        LinearLayoutManager lam = (LinearLayoutManager) _recyclerList.getLayoutManager();
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getActivity(), lam.getOrientation());
        _recyclerList.addItemDecoration(dividerItemDecoration);
        _previousNotebookDirectory = _appSettings.getNotebookDirectoryAsStr();

        _filesystemDialogAdapter = new FilesystemDialogAdapter(_dopt, context, _recyclerList);
        _recyclerList.setAdapter(_filesystemDialogAdapter);
        _filesystemDialogAdapter.getFilter().filter("");
        onFsDoUiUpdate(_filesystemDialogAdapter);

        swipe.setOnRefreshListener(() -> {
            _filesystemDialogAdapter.reloadCurrentFolder();
            swipe.setRefreshing(false);
        });

        _filesystemDialogAdapter.restoreSavedInstanceState(savedInstanceState);
    }

    @Override
    public String getFragmentTag() {
        return "FilesystemFragment";
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.opoc_filesystem_fragment;
    }

    private int rcolor(@ColorRes int colorRes) {
        return ContextCompat.getColor(getActivity(), colorRes);
    }

    private void setDialogOptions(FilesystemDialogData.Options options) {
        _dopt = options;
        _callback = _dopt.listener;
        _dopt.listener = this;
        checkOptions();
    }

    public void onClicked(View view) {
        switch (view.getId()) {
            case R.id.ui__filesystem_dialog__button_ok:
            case R.id.ui__filesystem_dialog__home: {
                _filesystemDialogAdapter.onClick(view);
                break;
            }
            case R.id.ui__filesystem_dialog__button_cancel: {
                onFsNothingSelected(_dopt.requestId);
                break;
            }

        }
    }

    private void checkOptions() {
        if (_dopt.doSelectFile && !_dopt.doSelectMultiple) {
            _dopt.okButtonEnable = false;
        }
    }

    @Override
    public void onFsSelected(String request, File file) {
        if (_callback != null) {
            _callback.onFsSelected(_dopt.requestId, file);
        }
    }

    @Override
    public void onFsMultiSelected(String request, File... files) {
        if (_callback != null) {
            _callback.onFsMultiSelected(_dopt.requestId, files);
        }
    }

    @Override
    public void onFsNothingSelected(String request) {
        if (_callback != null) {
            _callback.onFsNothingSelected(_dopt.requestId);
        }
    }

    @Override
    public void onFsDialogConfig(FilesystemDialogData.Options opt) {
        if (_callback != null) {
            _callback.onFsDialogConfig(opt);
        }
    }

    @Override
    public void onFsDoUiUpdate(FilesystemDialogAdapter adapter) {
        if (_callback != null) {
            _callback.onFsDoUiUpdate(adapter);
        }

        _recyclerList.postDelayed(this::updateMenuItems, 300);
        _recyclerList.postDelayed(this::updateMenuItems, 1000);
        _recyclerList.postDelayed(this::updateMenuItems, 3000);
    }

    private void updateMenuItems(){
        boolean multi1 = _dopt.doSelectMultiple && _filesystemDialogAdapter.getCurrentSelection().size() == 1;
        boolean multiMore = _dopt.doSelectMultiple && _filesystemDialogAdapter.getCurrentSelection().size() > 1;

        if (_fragmentMenu != null && _fragmentMenu.findItem(R.id.action_delete_selected_items) != null) {
            _fragmentMenu.findItem(R.id.action_delete_selected_items).setVisible(multi1 || multiMore);
            _fragmentMenu.findItem(R.id.action_rename_selected_item).setVisible(multi1);
            _fragmentMenu.findItem(R.id.action_info_selected_item).setVisible(multi1);
            _fragmentMenu.findItem(R.id.action_move_selected_items).setVisible(multi1 || multiMore);
            _fragmentMenu.findItem(R.id.action_go_to).setVisible(!_filesystemDialogAdapter.areItemsSelected());
            _fragmentMenu.findItem(R.id.action_sort).setVisible(!_filesystemDialogAdapter.areItemsSelected());
            _fragmentMenu.findItem(R.id.action_import).setVisible(!_filesystemDialogAdapter.areItemsSelected());
            _fragmentMenu.findItem(R.id.action_settings).setVisible(!_filesystemDialogAdapter.areItemsSelected());
        }
    }

    @Override
    public void onFsLongPressed(File file, boolean doSelectMultiple) {
        if (_callback != null) {
            _callback.onFsLongPressed(file, doSelectMultiple);
        }
    }

    @Override
    public boolean onBackPressed() {
        if (_filesystemDialogAdapter.canGoUp() && !_filesystemDialogAdapter.isCurrentFolderHome()) {
            _filesystemDialogAdapter.goUp();
            return true;
        }
        return super.onBackPressed();
    }

    public void reloadCurrentFolder() {
        _filesystemDialogAdapter.unselectAll();
        _filesystemDialogAdapter.reloadCurrentFolder();
        onFsDoUiUpdate(_filesystemDialogAdapter);
    }

    public File getCurrentFolder() {
        return _filesystemDialogAdapter.getCurrentFolder();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState = _filesystemDialogAdapter.saveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    private static String _previousNotebookDirectory;

    @Override
    public void onResume() {
        super.onResume();
        if (!_appSettings.getNotebookDirectoryAsStr().equals(_previousNotebookDirectory)) {
            _dopt.rootFolder = _appSettings.getNotebookDirectory();
            _filesystemDialogAdapter.setCurrentFolder(_dopt.rootFolder, false);
        }

        if (!firstResume) {
            if (_filesystemDialogAdapter.getCurrentFolder() != null) {
                _filesystemDialogAdapter.reloadCurrentFolder();
            }
        }

        onFsDoUiUpdate(_filesystemDialogAdapter);
        firstResume = false;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.filesystem__menu, menu);
        ContextUtils cu = ContextUtils.get();
        cu.tintMenuItems(menu, true, Color.WHITE);
        cu.setSubMenuIconsVisiblity(menu, true);

        MenuItem item;
        if ((item = menu.findItem(R.id.action_folder_first)) != null) {
            item.setChecked(_appSettings.isFilesystemListFolderFirst());
        }

        List<Pair<File, String>> sdcardFolders = _contextUtils.getAppDataPublicDirs(false, true, true);
        int[] sdcardResIds = {R.id.action_go_to_appdata_sdcard_1, R.id.action_go_to_appdata_sdcard_2};
        for (int i = 0; i < sdcardResIds.length && i < sdcardFolders.size(); i++) {
            item = menu.findItem(sdcardResIds[i]);
            item.setTitle(item.getTitle().toString().replaceFirst("[)]\\s*$", " " + sdcardFolders.get(i).second) + ")");
            item.setVisible(true);
        }
        _fragmentMenu = menu;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        PermissionChecker permc = new PermissionChecker(getActivity());
        List<Pair<File, String>> appDataPublicDirs = _contextUtils.getAppDataPublicDirs(false, true, false);

        File folderToLoad = null;

        switch (item.getItemId()) {
            case R.id.action_sort_by_name: {
                _appSettings.setSortMethod(SORT_BY_NAME);
                sortAdapter();
                return true;
            }
            case R.id.action_sort_by_date: {
                _appSettings.setSortMethod(SORT_BY_DATE);
                sortAdapter();
                return true;
            }
            case R.id.action_sort_by_filesize: {
                _appSettings.setSortMethod(SORT_BY_FILESIZE);
                sortAdapter();
                return true;
            }
            case R.id.action_sort_reverse: {
                item.setChecked(!item.isChecked());
                _appSettings.setSortReverse(item.isChecked());
                sortAdapter();
                return true;
            }
            case R.id.action_import: {
                if (permc.mkdirIfStoragePermissionGranted()) {
                    showImportDialog();
                }
                return true;
            }
            case R.id.action_folder_first: {
                item.setChecked(!item.isChecked());
                _appSettings.setFilesystemListFolderFirst(item.isChecked());
                _filesystemDialogAdapter.reloadCurrentFolder();
                sortAdapter();
                return true;
            }
            case R.id.action_go_to_home: {
                folderToLoad = _appSettings.getNotebookDirectory();
                break;
            }
            case R.id.action_go_to_popular_files: {
                folderToLoad = FilesystemDialogAdapter.VIRTUAL_STORAGE_POPULAR;
                break;
            }
            case R.id.action_go_to_recent_files: {
                folderToLoad = FilesystemDialogAdapter.VIRTUAL_STORAGE_RECENTS;
                break;
            }
            case R.id.action_go_to_appdata_private: {
                folderToLoad = _contextUtils.getAppDataPrivateDir();
                break;
            }
            case R.id.action_go_to_storage: {
                folderToLoad = Environment.getExternalStorageDirectory();
                break;
            }
            case R.id.action_go_to_appdata_sdcard_1: {
                if (appDataPublicDirs.size() > 0) {
                    folderToLoad = appDataPublicDirs.get(0).first;
                }
                break;
            }
            case R.id.action_go_to_appdata_sdcard_2: {
                if (appDataPublicDirs.size() > 1) {
                    folderToLoad = appDataPublicDirs.get(1).first;
                }
                break;
            }
            case R.id.action_go_to_appdata_public: {
                appDataPublicDirs = _contextUtils.getAppDataPublicDirs(true, false, false);
                if (appDataPublicDirs.size() > 0) {
                    folderToLoad = appDataPublicDirs.get(0).first;
                }
                break;
            }
            case R.id.action_delete_selected_items: {
                askForDeletingFilesRecursive((confirmed, data) -> {
                    if (confirmed) {
                        WrMarkorSingleton.getInstance().deleteSelectedItems(_filesystemDialogAdapter.getCurrentSelection());
                        _filesystemDialogAdapter.unselectAll();
                        _filesystemDialogAdapter.reloadCurrentFolder();
                    }
                });
                return true;
            }

            case R.id.action_move_selected_items: {
                askForMove();
                return true;
            }

            case R.id.action_info_selected_item: {
                if (_filesystemDialogAdapter.areItemsSelected()) {
                    File file = new ArrayList<>(_filesystemDialogAdapter.getCurrentSelection()).get(0);
                    FileInfoDialog.show(file, getFragmentManager());
                }
                return true;
            }

            case R.id.action_rename_selected_item: {
                if (_filesystemDialogAdapter.areItemsSelected()) {
                    File file = new ArrayList<>(_filesystemDialogAdapter.getCurrentSelection()).get(0);
                    WrRenameDialog renameDialog = WrRenameDialog.newInstance(file, renamedFile -> reloadCurrentFolder());
                    renameDialog.show(getFragmentManager(), WrRenameDialog.FRAGMENT_TAG);
                }
                return true;
            }
        }

        if (folderToLoad != null) {
            _filesystemDialogAdapter.setCurrentFolder(folderToLoad, true);
            Toast.makeText(getContext(), folderToLoad.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            return true;
        }

        return false;
    }

    public void sortAdapter() {
        _dopt.fileComparable = sortFolder(null);
        _dopt.folderFirst = _appSettings.isFilesystemListFolderFirst();
        reloadCurrentFolder();
    }

    public static Comparator<File> sortFolder(List<File> filesToSort) {
        final int sortMethod = AppSettings.get().getSortMethod();
        final boolean sortReverse = AppSettings.get().isSortReverse();

        Comparator<File> comparator = new Comparator<File>() {
            @Override
            public int compare(File file, File other) {
                if (sortReverse) {
                    File swap = file;
                    file = other;
                    other = swap;
                }

                boolean mk1 = MarkdownTextConverter.isTextOrMarkdownFile(file);
                boolean mk2 = MarkdownTextConverter.isTextOrMarkdownFile(other);
                if (mk1 && !mk2) {
                    return 1;
                } else if (!mk1 && mk2) {
                    return -1;
                }

                switch (sortMethod) {
                    case SORT_BY_NAME:
                        return new File(file.getAbsolutePath().toLowerCase()).compareTo(
                                new File(other.getAbsolutePath().toLowerCase()));
                    case SORT_BY_DATE:
                        return Long.valueOf(other.lastModified()).compareTo(file.lastModified());
                    case SORT_BY_FILESIZE:
                        if (file.isDirectory() && other.isDirectory()) {
                            return other.list().length - file.list().length;
                        }
                        return Long.valueOf(other.length()).compareTo(file.length());
                }
                return file.compareTo(other);
            }
        };

        if (filesToSort != null) {
            Collections.sort(filesToSort, comparator);
        }

        return comparator;
    }


    public void clearSelection() {
        _filesystemDialogAdapter.unselectAll();
    }


    ///////////////
    public void askForDeletingFilesRecursive(WrConfirmDialog.ConfirmDialogCallback confirmCallback) {
        final ArrayList<File> itemsToDelete = new ArrayList<>(_filesystemDialogAdapter.getCurrentSelection());
        String message = String.format(getString(R.string.do_you_really_want_to_delete_this_witharg), getResources().getQuantityString(R.plurals.documents, itemsToDelete.size())) + "\n\n";

        for (File f : itemsToDelete) {
            message += "\n" + f.getAbsolutePath();
        }

        WrConfirmDialog confirmDialog = WrConfirmDialog.newInstance(getString(R.string.confirm_delete), message, itemsToDelete, confirmCallback);
        confirmDialog.show(getActivity().getSupportFragmentManager(), WrConfirmDialog.FRAGMENT_TAG);
    }

    private void askForMove() {
        final ArrayList<File> filesToMove = new ArrayList<>(_filesystemDialogAdapter.getCurrentSelection());
        FilesystemDialogCreator.showFolderDialog(new FilesystemDialogData.SelectionListenerAdapter() {
            @Override
            public void onFsSelected(String request, File file) {
                super.onFsSelected(request, file);
                WrMarkorSingleton.getInstance().moveSelectedNotes(filesToMove, file.getAbsolutePath());
            }

            @Override
            public void onFsDialogConfig(FilesystemDialogData.Options opt) {
                opt.titleText = R.string.move;
                opt.rootFolder = _appSettings.getNotebookDirectory();
            }
        }, getActivity().getSupportFragmentManager(), getActivity());
    }

    private void showImportDialog() {
        FilesystemDialogCreator.showFileDialog(new FilesystemDialogData.SelectionListenerAdapter() {
            @Override
            public void onFsSelected(String request, File file) {
                importFile(file);
                reloadCurrentFolder();
            }

            @Override
            public void onFsMultiSelected(String request, File... files) {
                for (File file : files) {
                    importFile(file);
                }
                reloadCurrentFolder();
            }

            @Override
            public void onFsDialogConfig(FilesystemDialogData.Options opt) {
                opt.titleText = R.string.import_from_device;
                opt.doSelectMultiple = true;
                opt.doSelectFile = true;
                opt.doSelectFolder = true;
            }
        }, getFragmentManager(), getActivity(), null);
    }

    private void importFile(final File file) {
        if (new File(getCurrentFolder().getAbsolutePath(), file.getName()).exists()) {
            String message = getString(R.string.file_already_exists_overwerite) + "\n[" + file.getName() + "]";
            // Ask if overwriting is okay
            WrConfirmDialog d = WrConfirmDialog.newInstance(
                    getString(R.string.confirm_overwrite), message, file, (WrConfirmDialog.ConfirmDialogCallback) (confirmed, data) -> {
                        if (confirmed) {
                            importFileToCurrentDirectory(getActivity(), file);
                        }
                    });
            if (getFragmentManager() != null) {
                d.show(getFragmentManager(), WrConfirmDialog.FRAGMENT_TAG);
            }
        } else {
            // Import
            importFileToCurrentDirectory(getActivity(), file);
        }
    }

    private void importFileToCurrentDirectory(Context context, File sourceFile) {
        FileUtils.copyFile(sourceFile, new File(getCurrentFolder().getAbsolutePath(), sourceFile.getName()));
        Toast.makeText(context, getString(R.string.import_) + ": " + sourceFile.getName(), Toast.LENGTH_LONG).show();
    }
}
