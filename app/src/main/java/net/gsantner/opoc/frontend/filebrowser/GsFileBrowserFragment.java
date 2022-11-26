/*#######################################################
 *
 * SPDX-FileCopyrightText: 2017-2022 Gregor Santner <https://gsantner.net/>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2018-2022 by Gregor Santner <https://gsantner.net/>
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
#########################################################*/

/*
 * Revision 001 of FilesystemViewerCreator
 * A simple filesystem dialog with file, folder and multiple selection
 * most bits (color, text, images) can be controller using FilesystemViewerData.
 * The data container contains a listener callback for results.
 * Most features are usable without any additional project files and resources
 */
package net.gsantner.opoc.frontend.filebrowser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import net.gsantner.markor.ApplicationObject;
import net.gsantner.markor.R;
import net.gsantner.markor.format.FormatRegistry;
import net.gsantner.markor.frontend.FileInfoDialog;
import net.gsantner.markor.frontend.MarkorDialogFactory;
import net.gsantner.markor.frontend.filebrowser.MarkorFileBrowserFactory;
import net.gsantner.markor.frontend.filesearch.FileSearchEngine;
import net.gsantner.markor.frontend.settings.MarkorPermissionChecker;
import net.gsantner.markor.frontend.textview.TextViewUtils;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.markor.util.MarkorContextUtils;
import net.gsantner.opoc.frontend.base.GsFragmentBase;
import net.gsantner.opoc.model.GsSharedPreferencesPropertyBackend;
import net.gsantner.opoc.util.GsContextUtils;
import net.gsantner.opoc.util.GsFileUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import other.writeily.model.WrMarkorSingleton;
import other.writeily.ui.WrConfirmDialog;
import other.writeily.ui.WrRenameDialog;

public class GsFileBrowserFragment extends GsFragmentBase<GsSharedPreferencesPropertyBackend, GsContextUtils> implements GsFileBrowserOptions.SelectionListener {
    //########################
    //## Static
    //########################
    public static final String FRAGMENT_TAG = "FilesystemViewerFragment";

    public static GsFileBrowserFragment newInstance(GsFileBrowserOptions.Options options) {
        return new GsFileBrowserFragment();
    }

    //########################
    //## Member
    //########################

    private RecyclerView _recyclerList;
    private SwipeRefreshLayout swipe;
    private TextView _emptyHint;

    private GsFileBrowserListAdapter _filesystemViewerAdapter;
    private GsFileBrowserOptions.Options _dopt;
    private GsFileBrowserOptions.SelectionListener _callback;
    private boolean firstResume = true;
    private AppSettings _appSettings;
    private Menu _fragmentMenu;
    private MarkorContextUtils _cu;

    //########################
    //## Methods
    //########################

    public interface FilesystemFragmentOptionsListener {
        GsFileBrowserOptions.Options getFilesystemFragmentOptions(GsFileBrowserOptions.Options existingOptions);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);
        Context context = getContext();
        _recyclerList = root.findViewById(R.id.ui__filesystem_dialog__list);
        swipe = root.findViewById(R.id.pull_to_refresh);
        _emptyHint = root.findViewById(R.id.empty_hint);

        _appSettings = ApplicationObject.settings();
        _cu = new MarkorContextUtils(root.getContext());

        if (!(getActivity() instanceof FilesystemFragmentOptionsListener)) {
            throw new RuntimeException("Error: " + getActivity().getClass().getName() + " doesn't implement FilesystemFragmentOptionsListener");
        }
        setDialogOptions(((FilesystemFragmentOptionsListener) getActivity()).getFilesystemFragmentOptions(_dopt));

        LinearLayoutManager lam = (LinearLayoutManager) _recyclerList.getLayoutManager();
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getActivity(), lam.getOrientation());
        _recyclerList.addItemDecoration(dividerItemDecoration);
        _previousNotebookDirectory = _appSettings.getNotebookDirectoryAsStr();

        _filesystemViewerAdapter = new GsFileBrowserListAdapter(_dopt, context, _recyclerList);
        _recyclerList.setAdapter(_filesystemViewerAdapter);
        _filesystemViewerAdapter.getFilter().filter("");
        onFsViewerDoUiUpdate(_filesystemViewerAdapter);

        swipe.setOnRefreshListener(() -> {
            _filesystemViewerAdapter.reloadCurrentFolder();
            swipe.setRefreshing(false);
        });

        _filesystemViewerAdapter.restoreSavedInstanceState(savedInstanceState);

        if (FileSearchEngine.isSearchExecuting) {
            FileSearchEngine.activity.set(new WeakReference<>(getActivity()));
        }
    }


    @Override
    public String getFragmentTag() {
        return "FilesystemViewerFragment";
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.opoc_filesystem_fragment;
    }

    private void setDialogOptions(GsFileBrowserOptions.Options options) {
        _dopt = options;
        _callback = _dopt.listener;
        if (_callback != null) {
            _callback.onFsViewerConfig(_dopt); // Configure every time
        }
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
    public void onFsViewerSelected(String request, File file, final Integer lineNumber) {
        if (_callback != null) {
            _callback.onFsViewerSelected(_dopt.requestId, file, lineNumber);
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
    public void onFsViewerConfig(GsFileBrowserOptions.Options dopt) {
        if (_callback != null) {
            _callback.onFsViewerConfig(dopt);
        }
    }

    @Override
    public void onFsViewerDoUiUpdate(GsFileBrowserListAdapter adapter) {
        if (_callback != null) {
            _callback.onFsViewerDoUiUpdate(adapter);
        }

        updateMenuItems();
        _emptyHint.postDelayed(() -> _emptyHint.setVisibility(adapter.isCurrentFolderEmpty() ? View.VISIBLE : View.GONE), 200);
        _recyclerList.postDelayed(this::updateMenuItems, 1000);
    }

    private void updateMenuItems() {
        final String curFilepath = (getCurrentFolder() != null ? getCurrentFolder() : new File("/")).getAbsolutePath();
        final boolean selMulti1 = _dopt.doSelectMultiple && _filesystemViewerAdapter.getCurrentSelection().size() == 1;
        final boolean selMultiMore = _dopt.doSelectMultiple && _filesystemViewerAdapter.getCurrentSelection().size() > 1;
        final boolean selFilesOnly = _filesystemViewerAdapter.isFilesOnlySelected();
        final Set<File> selFiles = _filesystemViewerAdapter.getCurrentSelection();

        // Check if is a favourite
        boolean isFavourite = false;
        boolean selTextFilesOnly = true;
        boolean selDirectoriesOnly = true;
        boolean selWritable = (!curFilepath.equals("/storage") && !curFilepath.equals("/storage/emulated"));
        if (selMulti1) {
            for (File favourite : _dopt.favouriteFiles == null ? new ArrayList<File>() : _dopt.favouriteFiles) {
                if (selFiles.contains(favourite)) {
                    isFavourite = true;
                    break;
                }
            }
        }
        for (final File f : _filesystemViewerAdapter.getCurrentSelection()) {
            selTextFilesOnly &= FormatRegistry.isFileSupported(f, true);
            selWritable &= f.canWrite();
            selDirectoriesOnly &= f.isDirectory();
        }

        if (_fragmentMenu != null && _fragmentMenu.findItem(R.id.action_delete_selected_items) != null) {
            _fragmentMenu.findItem(R.id.action_search).setVisible(selFiles.isEmpty() && !_filesystemViewerAdapter.isCurrentFolderVirtual());
            _fragmentMenu.findItem(R.id.action_delete_selected_items).setVisible((selMulti1 || selMultiMore) && selWritable);
            _fragmentMenu.findItem(R.id.action_rename_selected_item).setVisible(selMulti1 && selWritable);
            _fragmentMenu.findItem(R.id.action_info_selected_item).setVisible(selMulti1);
            _fragmentMenu.findItem(R.id.action_move_selected_items).setVisible((selMulti1 || selMultiMore) && selWritable && !_cu.isUnderStorageAccessFolder(getContext(), getCurrentFolder(), true));
            _fragmentMenu.findItem(R.id.action_copy_selected_items).setVisible((selMulti1 || selMultiMore) && selWritable && !_cu.isUnderStorageAccessFolder(getContext(), getCurrentFolder(), true));
            _fragmentMenu.findItem(R.id.action_share_files).setVisible(selFilesOnly && (selMulti1 || selMultiMore) && !_cu.isUnderStorageAccessFolder(getContext(), getCurrentFolder(), true));
            _fragmentMenu.findItem(R.id.action_go_to).setVisible(!_filesystemViewerAdapter.areItemsSelected());
            _fragmentMenu.findItem(R.id.action_sort).setVisible(!_filesystemViewerAdapter.areItemsSelected());
            _fragmentMenu.findItem(R.id.action_import).setVisible(!_filesystemViewerAdapter.areItemsSelected() && !_filesystemViewerAdapter.isCurrentFolderVirtual());
            _fragmentMenu.findItem(R.id.action_settings).setVisible(!_filesystemViewerAdapter.areItemsSelected());
            _fragmentMenu.findItem(R.id.action_favourite).setVisible(selMulti1 && !isFavourite);
            _fragmentMenu.findItem(R.id.action_favourite_remove).setVisible(selMulti1 && isFavourite);
            _fragmentMenu.findItem(R.id.action_fs_copy_to_clipboard).setVisible(selMulti1 && selTextFilesOnly);
            _fragmentMenu.findItem(R.id.action_create_shortcut).setVisible(selMulti1 && (selFilesOnly || selDirectoriesOnly));
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
        if (_filesystemViewerAdapter != null && _filesystemViewerAdapter.canGoUp() && !_filesystemViewerAdapter.isCurrentFolderHome()) {
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
        return _filesystemViewerAdapter != null ? _filesystemViewerAdapter.getCurrentFolder() : null;
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
            _filesystemViewerAdapter.setCurrentFolder(_dopt.rootFolder);
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
        _cu.tintMenuItems(menu, true, Color.WHITE);
        _cu.setSubMenuIconsVisibility(menu, true);

        MenuItem item;
        if ((item = menu.findItem(R.id.action_folder_first)) != null) {
            item.setChecked(_dopt.sortFolderFirst);
        }
        if ((item = menu.findItem(R.id.action_sort_reverse)) != null) {
            item.setChecked(_dopt.sortReverse);
        }
        if ((item = menu.findItem(R.id.action_show_dotfiles)) != null) {
            item.setChecked(_dopt.filterShowDotFiles);
        }

        if ((item = menu.findItem(R.id.action_sort_by_name)) != null && GsFileUtils.SORT_BY_NAME.equals(_dopt.sortByType)) {
            item.setChecked(true);
        } else if ((item = menu.findItem(R.id.action_sort_by_date)) != null && GsFileUtils.SORT_BY_MTIME.equals(_dopt.sortByType)) {
            item.setChecked(true);
        } else if ((item = menu.findItem(R.id.action_sort_by_filesize)) != null && GsFileUtils.SORT_BY_FILESIZE.equals(_dopt.sortByType)) {
            item.setChecked(true);
        } else if ((item = menu.findItem(R.id.action_sort_by_mimetype)) != null && GsFileUtils.SORT_BY_MIMETYPE.equals(_dopt.sortByType)) {
            item.setChecked(true);
        }

        List<Pair<File, String>> sdcardFolders = _cu.getAppDataPublicDirs(getContext(), false, true, true);
        int[] sdcardResIds = {R.id.action_go_to_appdata_sdcard_1, R.id.action_go_to_appdata_sdcard_2};
        for (int i = 0; i < sdcardResIds.length && i < sdcardFolders.size(); i++) {
            item = menu.findItem(sdcardResIds[i]);
            item.setTitle(item.getTitle().toString().replaceFirst("[)]\\s*$", " " + sdcardFolders.get(i).second) + ")");
            item.setVisible(true);
        }
        _fragmentMenu = menu;
        updateMenuItems();
    }

    public GsFileBrowserListAdapter getAdapter() {
        return _filesystemViewerAdapter;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final MarkorPermissionChecker permc = new MarkorPermissionChecker(getActivity());

        final int _id = item.getItemId();

        switch (_id) {
            case R.id.action_create_shortcut: {
                final File file = _filesystemViewerAdapter.getCurrentSelection().iterator().next();
                _cu.createLauncherDesktopShortcut(getContext(), file);
                return true;
            }
            case R.id.action_sort_by_name: {
                item.setChecked(true);
                _dopt.sortByType = _appSettings.setFileBrowserSortByType(GsFileUtils.SORT_BY_NAME);
                sortAdapter();
                return true;
            }
            case R.id.action_sort_by_date: {
                item.setChecked(true);
                _dopt.sortByType = _appSettings.setFileBrowserSortByType(GsFileUtils.SORT_BY_MTIME);
                sortAdapter();
                return true;
            }
            case R.id.action_sort_by_filesize: {
                item.setChecked(true);
                _dopt.sortByType = _appSettings.setFileBrowserSortByType(GsFileUtils.SORT_BY_FILESIZE);
                sortAdapter();
                return true;
            }
            case R.id.action_sort_by_mimetype: {
                item.setChecked(true);
                _dopt.sortByType = _appSettings.setFileBrowserSortByType(GsFileUtils.SORT_BY_MIMETYPE);
                sortAdapter();
                return true;
            }
            case R.id.action_sort_reverse: {
                item.setChecked(!item.isChecked());
                _dopt.sortReverse = _appSettings.setFileBrowserSortReverse(item.isChecked());
                sortAdapter();
                return true;
            }
            case R.id.action_show_dotfiles: {
                item.setChecked(!item.isChecked());
                _dopt.filterShowDotFiles = _appSettings.setFileBrowserFilterShowDotFiles(item.isChecked());
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
                executeSearchAction();
                return true;
            }
            case R.id.action_folder_first: {
                item.setChecked(!item.isChecked());
                _dopt.sortFolderFirst = _appSettings.setFileBrowserSortFolderFirst(item.isChecked());
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
                final File folder = _appSettings.getFolderToLoadByMenuId(_id);
                _filesystemViewerAdapter.setCurrentFolder(folder);
                Toast.makeText(getContext(), folder.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                return true;
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

            case R.id.action_move_selected_items:
            case R.id.action_copy_selected_items: {
                askForMoveOrCopy(_id == R.id.action_move_selected_items);
                return true;
            }

            case R.id.action_share_files: {
                MarkorContextUtils s = new MarkorContextUtils(getContext());
                s.shareStreamMultiple(getContext(), _filesystemViewerAdapter.getCurrentSelection(), "*/*");
                _filesystemViewerAdapter.unselectAll();
                _filesystemViewerAdapter.reloadCurrentFolder();
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
                    if (FormatRegistry.isFileSupported(file, true)) {
                        _cu.setClipboard(getContext(), GsFileUtils.readTextFileFast(file).first);
                        Toast.makeText(getContext(), R.string.clipboard, Toast.LENGTH_SHORT).show();
                        _filesystemViewerAdapter.unselectAll();
                    }
                }
                return true;
            }
        }

        return false;
    }

    private void executeSearchAction() {
        if (new MarkorPermissionChecker(getActivity()).doIfExtStoragePermissionGranted()) {
            final File currentFolder = getCurrentFolder();
            MarkorDialogFactory.showSearchFilesDialog(getActivity(), currentFolder, (relFilePath, lineNumber) -> {
                File load = new File(currentFolder, relFilePath);
                if (load.isDirectory()) {
                    _filesystemViewerAdapter.loadFolder(load);
                } else {
                    onFsViewerSelected("", load, lineNumber);
                }
            });
        }
    }

    public void sortAdapter() {
        reloadCurrentFolder();
    }

    public void clearSelection() {
        if (_filesystemViewerAdapter != null) { // Happens when restoring after rotation etc
            _filesystemViewerAdapter.unselectAll();
        }
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

    private void askForMoveOrCopy(final boolean isMove) {
        final List<File> files = new ArrayList<>(_filesystemViewerAdapter.getCurrentSelection());
        MarkorFileBrowserFactory.showFolderDialog(new GsFileBrowserOptions.SelectionListenerAdapter() {
            private GsFileBrowserOptions.Options _doptMoC;

            @Override
            public void onFsViewerSelected(String request, File file, Integer lineNumber) {
                super.onFsViewerSelected(request, file, null);
                WrMarkorSingleton.getInstance().moveOrCopySelected(files, file, getActivity(), isMove);
                _filesystemViewerAdapter.unselectAll();
                _filesystemViewerAdapter.reloadCurrentFolder();
            }

            @Override
            public void onFsViewerConfig(GsFileBrowserOptions.Options dopt) {
                _doptMoC = dopt;
                _doptMoC.titleText = isMove ? R.string.move : R.string.copy;
                _doptMoC.rootFolder = _appSettings.getNotebookDirectory();
                _doptMoC.startFolder = getCurrentFolder();
                // Directories cannot be moved into themselves. Don't give users the option
                final Set<String> selSet = new HashSet<>();
                for (final File f : files) {
                    selSet.add(f.getAbsolutePath());
                }
                _doptMoC.fileOverallFilter = (context, test) -> !selSet.contains(test.getAbsolutePath());
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onFsViewerDoUiUpdate(GsFileBrowserListAdapter adapter) {
                if (_doptMoC.listener instanceof GsFileBrowserDialog) {
                    final TextView titleView = ((GsFileBrowserDialog) _doptMoC.listener)._dialogTitle;
                    if (titleView != null && adapter.getCurrentFolder() != null) {
                        titleView.setText(String.format("%s → %s", titleView.getContext().getString(isMove ? R.string.move : R.string.copy), adapter.getCurrentFolder().getName()));
                    }
                }
            }
        }, getActivity().getSupportFragmentManager(), getActivity());
    }

    private void showImportDialog() {
        MarkorFileBrowserFactory.showFileDialog(new GsFileBrowserOptions.SelectionListenerAdapter() {
            @Override
            public void onFsViewerSelected(String request, File file, final Integer lineNumber) {
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
            public void onFsViewerConfig(GsFileBrowserOptions.Options dopt) {
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
                    getString(R.string.confirm_overwrite), message, file, (confirmed, data) -> {
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
        GsFileUtils.copyFile(sourceFile, new File(getCurrentFolder().getAbsolutePath(), sourceFile.getName()));
        Toast.makeText(context, getString(R.string.import_) + ": " + sourceFile.getName(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && getCurrentFolder() != null && !TextUtils.isEmpty(getCurrentFolder().getName()) && getToolbar() != null) {
            getToolbar().setTitle(getCurrentFolder().getName());
            reloadCurrentFolder();
        }
    }

    private void showAndBlink(final File file) {
        final int pos = getAdapter().getFilePosition(file);
        final LinearLayoutManager manager = (LinearLayoutManager) _recyclerList.getLayoutManager();
        if (manager != null && pos >= 0) {
            manager.scrollToPositionWithOffset(pos, 1);
            _recyclerList.postDelayed(() -> {
                final RecyclerView.ViewHolder holder = _recyclerList.findViewHolderForLayoutPosition(pos);
                if (holder != null) {
                    TextViewUtils.blinkView(holder.itemView);
                }
            }, 250);
        }
    }

    // Switch to folder and show the file
    public void showPathRelative(final String path) {
        final GsFileBrowserListAdapter adapter = getAdapter();
        if (adapter == null) {
            return;
        }

        final File current = adapter.getCurrentFolder();
        final File file = new File(current, path);
        if (!file.exists()) {
            return;
        }

        final File dir = file.getParentFile();
        if (dir == null) {
            return;
        }

        if (!current.equals(dir)) {
            // Wait up to 2s for the folder to load
            final long init = System.currentTimeMillis();
            adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                @Override
                public void onChanged() {
                    super.onChanged();
                    if ((System.currentTimeMillis() - init) < 2000) {
                        _recyclerList.postDelayed(() -> showAndBlink(file), 250);
                    }
                    adapter.unregisterAdapterDataObserver(this);
                }
            });
            adapter.setCurrentFolder(dir);
        } else {
            showAndBlink(file);
        }
    }
}
