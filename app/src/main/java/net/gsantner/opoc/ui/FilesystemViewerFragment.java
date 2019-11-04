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
 * Revision 001 of FilesystemViewerCreator
 * A simple filesystem dialog with file, folder and multiple selection
 * most bits (color, text, images) can be controller using FilesystemViewerData.
 * The data container contains a listener callback for results.
 * Most features are usable without any additional project files and resources
 *
 * Required: Butterknife library
 */
package net.gsantner.opoc.ui;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import net.gsantner.markor.R;
import net.gsantner.markor.format.TextFormat;
import net.gsantner.markor.ui.FileInfoDialog;
import net.gsantner.markor.ui.FilesystemViewerCreator;
import net.gsantner.markor.ui.SearchOrCustomTextDialogCreator;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ContextUtils;
import net.gsantner.markor.util.PermissionChecker;
import net.gsantner.markor.util.ShareUtil;
import net.gsantner.opoc.activity.GsFragmentBase;
import net.gsantner.opoc.util.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import other.writeily.model.WrMarkorSingleton;
import other.writeily.ui.WrConfirmDialog;
import other.writeily.ui.WrRenameDialog;

public class FilesystemViewerFragment extends GsFragmentBase
        implements FilesystemViewerData.SelectionListener {
    //########################
    //## Static
    //########################
    public static final String FRAGMENT_TAG = "FilesystemViewerFragment";

    public static final int SORT_BY_NAME = 0;
    public static final int SORT_BY_DATE = 1;
    public static final int SORT_BY_FILESIZE = 2;

    public static FilesystemViewerFragment newInstance(FilesystemViewerData.Options options) {
        FilesystemViewerFragment f = new FilesystemViewerFragment();
        options.listener.onFsViewerConfig(options);
        return f;
    }

    //########################
    //## Member
    //########################
    @BindView(R.id.ui__filesystem_dialog__list)
    RecyclerView _recyclerList;

    @BindView(R.id.pull_to_refresh)
    public SwipeRefreshLayout swipe;

    @BindView(R.id.empty_hint)
    public TextView _emptyHint;

    private FilesystemViewerAdapter _filesystemViewerAdapter;
    private FilesystemViewerData.Options _dopt;
    private FilesystemViewerData.SelectionListener _callback;
    private boolean firstResume = true;
    private AppSettings _appSettings;
    private ContextUtils _contextUtils;
    private Menu _fragmentMenu;
    private ShareUtil _shareUtil;

    //########################
    //## Methods
    //########################

    public interface FilesystemFragmentOptionsListener {
        FilesystemViewerData.Options getFilesystemFragmentOptions(FilesystemViewerData.Options existingOptions);
    }

    @Override
    public void onViewCreated(View root, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);
        Context context = getContext();
        ButterKnife.bind(this, root);
        _appSettings = new AppSettings(root.getContext());
        _contextUtils = new ContextUtils(root.getContext());
        _shareUtil = new ShareUtil(root.getContext());

        if (!(getActivity() instanceof FilesystemFragmentOptionsListener)) {
            throw new RuntimeException("Error: " + getActivity().getClass().getName() + " doesn't implement FilesystemFragmentOptionsListener");
        }
        setDialogOptions(((FilesystemFragmentOptionsListener) getActivity()).getFilesystemFragmentOptions(_dopt));

        LinearLayoutManager lam = (LinearLayoutManager) _recyclerList.getLayoutManager();
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getActivity(), lam.getOrientation());
        _recyclerList.addItemDecoration(dividerItemDecoration);
        _previousNotebookDirectory = _appSettings.getNotebookDirectoryAsStr();

        _filesystemViewerAdapter = new FilesystemViewerAdapter(_dopt, context, _recyclerList);
        _recyclerList.setAdapter(_filesystemViewerAdapter);
        _filesystemViewerAdapter.getFilter().filter("");
        onFsViewerDoUiUpdate(_filesystemViewerAdapter);

        swipe.setOnRefreshListener(() -> {
            _filesystemViewerAdapter.reloadCurrentFolder();
            swipe.setRefreshing(false);
        });

        _filesystemViewerAdapter.restoreSavedInstanceState(savedInstanceState);
    }


    @Override
    public String getFragmentTag() {
        return "FilesystemViewerFragment";
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.opoc_filesystem_fragment;
    }

    private void setDialogOptions(FilesystemViewerData.Options options) {
        _dopt = options;
        _callback = _dopt.listener;
        _dopt.listener = this;
        checkOptions();
    }

    public void onClicked(View view) {
        switch (view.getId()) {
            case R.id.ui__filesystem_dialog__button_ok:
            case R.id.ui__filesystem_dialog__home: {
                _filesystemViewerAdapter.onClick(view);
                break;
            }
            case R.id.ui__filesystem_dialog__button_cancel: {
                onFsViewerNothingSelected(_dopt.requestId);
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
    public void onFsViewerSelected(String request, File file) {
        if (_callback != null) {
            _callback.onFsViewerSelected(_dopt.requestId, file);
        }
    }

    @Override
    public void onFsViewerMultiSelected(String request, File... files) {
        if (_callback != null) {
            _callback.onFsViewerMultiSelected(_dopt.requestId, files);
        }
    }

    @Override
    public void onFsViewerNothingSelected(String request) {
        if (_callback != null) {
            _callback.onFsViewerNothingSelected(_dopt.requestId);
        }
    }

    @Override
    public void onFsViewerConfig(FilesystemViewerData.Options dopt) {
        if (_callback != null) {
            _callback.onFsViewerConfig(dopt);
        }
    }

    @Override
    public void onFsViewerDoUiUpdate(FilesystemViewerAdapter adapter) {
        if (_callback != null) {
            _callback.onFsViewerDoUiUpdate(adapter);
        }

        updateMenuItems();
        _emptyHint.postDelayed(() -> _emptyHint.setVisibility(adapter.isCurrentFolderEmpty() ? View.VISIBLE : View.GONE), 200);
        _recyclerList.postDelayed(this::updateMenuItems, 1000);
    }

    private void updateMenuItems() {
        final boolean selMulti1 = _dopt.doSelectMultiple && _filesystemViewerAdapter.getCurrentSelection().size() == 1;
        final boolean selMultiMore = _dopt.doSelectMultiple && _filesystemViewerAdapter.getCurrentSelection().size() > 1;
        final boolean selFilesOnly = _filesystemViewerAdapter.isFilesOnlySelected();
        final Set<File> selFiles = _filesystemViewerAdapter.getCurrentSelection();

        // Check if is a favourite
        boolean isFavourite = false;
        boolean selTextFilesOnly = false;
        if (selMulti1) {
            for (File favourite : _dopt.favouriteFiles == null ? new ArrayList<File>() : _dopt.favouriteFiles) {
                if (selFiles.contains(favourite)) {
                    isFavourite = true;
                    break;
                }
            }
        }
        for (File f : _filesystemViewerAdapter.getCurrentSelection()) {
            if (TextFormat.isTextFile(f)) {
                selTextFilesOnly = true;
            } else {
                selTextFilesOnly = false;
                break;
            }
        }

        if (_fragmentMenu != null && _fragmentMenu.findItem(R.id.action_delete_selected_items) != null) {
            _fragmentMenu.findItem(R.id.action_search).setVisible(selFiles.isEmpty());
            _fragmentMenu.findItem(R.id.action_delete_selected_items).setVisible(selMulti1 || selMultiMore);
            _fragmentMenu.findItem(R.id.action_rename_selected_item).setVisible(selMulti1);
            _fragmentMenu.findItem(R.id.action_info_selected_item).setVisible(selMulti1);
            _fragmentMenu.findItem(R.id.action_move_selected_items).setVisible((selMulti1 || selMultiMore) && !_shareUtil.isUnderStorageAccessFolder(getCurrentFolder()));
            _fragmentMenu.findItem(R.id.action_share_files).setVisible(selFilesOnly && (selMulti1 || selMultiMore) && !_shareUtil.isUnderStorageAccessFolder(getCurrentFolder()));
            _fragmentMenu.findItem(R.id.action_go_to).setVisible(!_filesystemViewerAdapter.areItemsSelected());
            _fragmentMenu.findItem(R.id.action_sort).setVisible(!_filesystemViewerAdapter.areItemsSelected());
            _fragmentMenu.findItem(R.id.action_import).setVisible(!_filesystemViewerAdapter.areItemsSelected());
            _fragmentMenu.findItem(R.id.action_settings).setVisible(!_filesystemViewerAdapter.areItemsSelected());
            _fragmentMenu.findItem(R.id.action_favourite).setVisible(selMulti1 && !isFavourite);
            _fragmentMenu.findItem(R.id.action_favourite_remove).setVisible(selMulti1 && isFavourite);
            _fragmentMenu.findItem(R.id.action_fs_copy_to_clipboard).setVisible(selMulti1 && selTextFilesOnly);
        }
    }

    @Override
    public void onFsViewerItemLongPressed(File file, boolean doSelectMultiple) {
        if (_callback != null) {
            _callback.onFsViewerItemLongPressed(file, doSelectMultiple);
        }
    }

    @Override
    public boolean onBackPressed() {
        if (_filesystemViewerAdapter.canGoUp() && !_filesystemViewerAdapter.isCurrentFolderHome()) {
            _filesystemViewerAdapter.goUp();
            return true;
        }
        return super.onBackPressed();
    }

    public void reloadCurrentFolder() {
        _filesystemViewerAdapter.unselectAll();
        _filesystemViewerAdapter.reloadCurrentFolder();
        onFsViewerDoUiUpdate(_filesystemViewerAdapter);
    }

    public File getCurrentFolder() {
        return _filesystemViewerAdapter.getCurrentFolder();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState = _filesystemViewerAdapter.saveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    private static String _previousNotebookDirectory;

    @Override
    public void onResume() {
        super.onResume();
        if (!_appSettings.getNotebookDirectoryAsStr().equals(_previousNotebookDirectory)) {
            _dopt.rootFolder = _appSettings.getNotebookDirectory();
            _filesystemViewerAdapter.setCurrentFolder(_dopt.rootFolder, false);
        }

        if (!firstResume) {
            if (_filesystemViewerAdapter.getCurrentFolder() != null) {
                _filesystemViewerAdapter.reloadCurrentFolder();
            }
        }

        onFsViewerDoUiUpdate(_filesystemViewerAdapter);
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
        if ((item = menu.findItem(R.id.action_sort_reverse)) != null) {
            item.setChecked(_appSettings.isSortReverse());
        }
        if ((item = menu.findItem(R.id.action_show_dotfiles)) != null) {
            item.setChecked(_appSettings.isShowDotFiles());
        }
        MenuItem[] sortBy = new MenuItem[]{menu.findItem(R.id.action_sort_by_name), menu.findItem(R.id.action_sort_by_date), menu.findItem(R.id.action_sort_by_filesize),};
        for (int i = 0; i < sortBy.length; i++) {
            if (sortBy[i] != null) {
                if (_appSettings.getSortMethod() == i) {
                    sortBy[i].setChecked(true);
                }
            }
        }


        List<Pair<File, String>> sdcardFolders = _contextUtils.getAppDataPublicDirs(false, true, true);
        int[] sdcardResIds = {R.id.action_go_to_appdata_sdcard_1, R.id.action_go_to_appdata_sdcard_2};
        for (int i = 0; i < sdcardResIds.length && i < sdcardFolders.size(); i++) {
            item = menu.findItem(sdcardResIds[i]);
            item.setTitle(item.getTitle().toString().replaceFirst("[)]\\s*$", " " + sdcardFolders.get(i).second) + ")");
            item.setVisible(true);
        }
        _fragmentMenu = menu;
        updateMenuItems();
    }

    public FilesystemViewerAdapter getAdapter() {
        return _filesystemViewerAdapter;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        PermissionChecker permc = new PermissionChecker(getActivity());
        List<Pair<File, String>> appDataPublicDirs = _contextUtils.getAppDataPublicDirs(false, true, false);

        File folderToLoad = null;

        switch (item.getItemId()) {
            case R.id.action_sort_by_name: {
                item.setChecked(true);
                _appSettings.setSortMethod(SORT_BY_NAME);
                sortAdapter();
                return true;
            }
            case R.id.action_sort_by_date: {
                item.setChecked(true);
                _appSettings.setSortMethod(SORT_BY_DATE);
                sortAdapter();
                return true;
            }
            case R.id.action_sort_by_filesize: {
                item.setChecked(true);
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
            case R.id.action_show_dotfiles: {
                item.setChecked(!item.isChecked());
                _appSettings.setShowDotFiles(item.isChecked());
                _dopt.showDotFiles = item.isChecked();
                reloadCurrentFolder();
                return true;
            }
            case R.id.action_import: {
                if (permc.mkdirIfStoragePermissionGranted()) {
                    showImportDialog();
                }
                return true;
            }
            case R.id.action_search: {
                final File currentFolder = getCurrentFolder();
                SearchOrCustomTextDialogCreator.showSearchFilesDialog(getActivity(), currentFolder, relFilePath -> {
                    File load = new File(currentFolder, relFilePath);
                    if (load.isDirectory()) {
                        _filesystemViewerAdapter.loadFolder(load);
                    } else {
                        onFsViewerSelected("", load);
                    }
                });
                return true;
            }
            case R.id.action_folder_first: {
                item.setChecked(!item.isChecked());
                _appSettings.setFilesystemListFolderFirst(item.isChecked());
                _filesystemViewerAdapter.reloadCurrentFolder();
                sortAdapter();
                return true;
            }
            case R.id.action_go_to_home:
            case R.id.action_go_to_popular_files:
            case R.id.action_go_to_recent_files:
            case R.id.action_go_to_favourite_files:
            case R.id.action_go_to_appdata_private:
            case R.id.action_go_to_storage:
            case R.id.action_go_to_appdata_sdcard_1:
            case R.id.action_go_to_appdata_sdcard_2:
            case R.id.action_go_to_appdata_public: {
                folderToLoad = _appSettings.getFolderToLoadByMenuId(item.getItemId());
                break;
            }
            case R.id.action_favourite:
            case R.id.action_favourite_remove: {
                if (_filesystemViewerAdapter.areItemsSelected()) {
                    _appSettings.toggleFavouriteFile(new ArrayList<>(_filesystemViewerAdapter.getCurrentSelection()).get(0));
                    _dopt.favouriteFiles = _appSettings.getFavouriteFiles();
                    updateMenuItems();
                }
                return true;
            }
            case R.id.action_delete_selected_items: {
                askForDeletingFilesRecursive((confirmed, data) -> {
                    if (confirmed) {
                        Runnable deleter = () -> {
                            WrMarkorSingleton.getInstance().deleteSelectedItems(_filesystemViewerAdapter.getCurrentSelection(), getContext());
                            _recyclerList.post(() -> {
                                _filesystemViewerAdapter.unselectAll();
                                _filesystemViewerAdapter.reloadCurrentFolder();
                            });
                        };
                        new Thread(deleter).start();
                    }
                });
                return true;
            }

            case R.id.action_move_selected_items: {
                askForMove();
                return true;
            }

            case R.id.action_share_files: {
                ShareUtil s = new ShareUtil(getContext());
                s.shareStreamMultiple(_filesystemViewerAdapter.getCurrentSelection(), "*/*");
                _filesystemViewerAdapter.unselectAll();
                _filesystemViewerAdapter.reloadCurrentFolder();
                s.freeContextRef();
                return true;
            }

            case R.id.action_info_selected_item: {
                if (_filesystemViewerAdapter.areItemsSelected()) {
                    File file = new ArrayList<>(_filesystemViewerAdapter.getCurrentSelection()).get(0);
                    FileInfoDialog.show(file, getFragmentManager());
                }
                return true;
            }

            case R.id.action_rename_selected_item: {
                if (_filesystemViewerAdapter.areItemsSelected()) {
                    File file = new ArrayList<>(_filesystemViewerAdapter.getCurrentSelection()).get(0);
                    WrRenameDialog renameDialog = WrRenameDialog.newInstance(file, renamedFile -> reloadCurrentFolder());
                    renameDialog.show(getFragmentManager(), WrRenameDialog.FRAGMENT_TAG);
                }
                return true;
            }

            case R.id.action_fs_copy_to_clipboard: {
                if (_filesystemViewerAdapter.areItemsSelected()) {
                    File file = new ArrayList<>(_filesystemViewerAdapter.getCurrentSelection()).get(0);
                    if (TextFormat.isTextFile(file, file.getAbsolutePath())) {
                        _shareUtil.setClipboard(FileUtils.readTextFileFast(file));
                        Toast.makeText(getContext(), R.string.clipboard, Toast.LENGTH_SHORT).show();
                        _filesystemViewerAdapter.unselectAll();
                    }
                }
                return true;
            }
        }

        if (folderToLoad != null) {
            _filesystemViewerAdapter.setCurrentFolder(folderToLoad, true);
            Toast.makeText(getContext(), folderToLoad.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            return true;
        }

        return false;
    }

    public static Comparator<File> sortFolder(List<File> filesToSort) {
        final int sortMethod = AppSettings.get().getSortMethod();
        final boolean sortReverse = AppSettings.get().isSortReverse();

        final Comparator<File> comparator = (current, other) -> {
            if (sortReverse) {
                File swap = current;
                current = other;
                other = swap;
            }

            switch (sortMethod) {
                case SORT_BY_NAME:
                    return new File(current.getAbsolutePath().toLowerCase()).compareTo(
                            new File(other.getAbsolutePath().toLowerCase()));
                case SORT_BY_DATE:
                    return Long.compare(other.lastModified(), current.lastModified());
                case SORT_BY_FILESIZE:
                    if (current.isDirectory() && other.isDirectory()) {
                        return other.list().length - current.list().length;
                    }
                    return Long.compare(other.length(), current.length());
            }
            return current.compareTo(other);
        };

        if (filesToSort != null) {
            try {
                Collections.sort(filesToSort, comparator);
            } catch (Exception ignored) {
            }
        }

        return comparator;
    }

    public void sortAdapter() {
        _dopt.fileComparable = sortFolder(null);
        _dopt.folderFirst = _appSettings.isFilesystemListFolderFirst();
        reloadCurrentFolder();
    }

    public void clearSelection() {
        _filesystemViewerAdapter.unselectAll();
    }


    ///////////////
    public void askForDeletingFilesRecursive(WrConfirmDialog.ConfirmDialogCallback confirmCallback) {
        final ArrayList<File> itemsToDelete = new ArrayList<>(_filesystemViewerAdapter.getCurrentSelection());
        StringBuilder message = new StringBuilder(String.format(getString(R.string.do_you_really_want_to_delete_this_witharg), getResources().getQuantityString(R.plurals.documents, itemsToDelete.size())) + "\n\n");

        for (File f : itemsToDelete) {
            message.append("\n").append(f.getAbsolutePath());
        }

        WrConfirmDialog confirmDialog = WrConfirmDialog.newInstance(getString(R.string.confirm_delete), message.toString(), itemsToDelete, confirmCallback);
        confirmDialog.show(getActivity().getSupportFragmentManager(), WrConfirmDialog.FRAGMENT_TAG);
    }

    private void askForMove() {
        final ArrayList<File> filesToMove = new ArrayList<>(_filesystemViewerAdapter.getCurrentSelection());
        FilesystemViewerCreator.showFolderDialog(new FilesystemViewerData.SelectionListenerAdapter() {
            @Override
            public void onFsViewerSelected(String request, File file) {
                super.onFsViewerSelected(request, file);
                WrMarkorSingleton.getInstance().moveSelectedNotes(filesToMove, file.getAbsolutePath(), getContext());
                _filesystemViewerAdapter.unselectAll();
                _filesystemViewerAdapter.reloadCurrentFolder();
            }

            @Override
            public void onFsViewerConfig(FilesystemViewerData.Options dopt) {
                dopt.titleText = R.string.move;
                dopt.rootFolder = _appSettings.getNotebookDirectory();
            }
        }, getActivity().getSupportFragmentManager(), getActivity());
    }

    private void showImportDialog() {
        FilesystemViewerCreator.showFileDialog(new FilesystemViewerData.SelectionListenerAdapter() {
            @Override
            public void onFsViewerSelected(String request, File file) {
                importFile(file);
                reloadCurrentFolder();
            }

            @Override
            public void onFsViewerMultiSelected(String request, File... files) {
                for (File file : files) {
                    importFile(file);
                }
                reloadCurrentFolder();
            }

            @Override
            public void onFsViewerConfig(FilesystemViewerData.Options dopt) {
                dopt.titleText = R.string.import_from_device;
                dopt.doSelectMultiple = true;
                dopt.doSelectFile = true;
                dopt.doSelectFolder = true;
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
