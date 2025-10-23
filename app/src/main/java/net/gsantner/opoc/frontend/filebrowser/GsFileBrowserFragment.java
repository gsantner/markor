/*#######################################################
 *
 * SPDX-FileCopyrightText: 2017-2025 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2018-2025 by Gregor Santner <gsantner AT mailbox DOT org>
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
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import net.gsantner.markor.R;
import net.gsantner.markor.format.FormatRegistry;
import net.gsantner.markor.frontend.FileInfoDialog;
import net.gsantner.markor.frontend.MarkorDialogFactory;
import net.gsantner.markor.frontend.filebrowser.MarkorFileBrowserFactory;
import net.gsantner.markor.frontend.filesearch.FileSearchEngine;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.markor.util.MarkorContextUtils;
import net.gsantner.opoc.frontend.GsSearchOrCustomTextDialog;
import net.gsantner.opoc.frontend.base.GsFragmentBase;
import net.gsantner.opoc.model.GsSharedPreferencesPropertyBackend;
import net.gsantner.opoc.util.GsCollectionUtils;
import net.gsantner.opoc.util.GsContextUtils;
import net.gsantner.opoc.util.GsFileUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import other.writeily.model.WrMarkorSingleton;
import other.writeily.ui.WrRenameDialog;

public class GsFileBrowserFragment extends GsFragmentBase<GsSharedPreferencesPropertyBackend, GsContextUtils> implements GsFileBrowserOptions.SelectionListener {
    //########################
    //## Static
    //########################
    public static final String FRAGMENT_TAG = "FilesystemViewerFragment";

    public static GsFileBrowserFragment newInstance() {
        return new GsFileBrowserFragment();
    }

    //########################
    //## Member
    //########################

    private RecyclerView _recyclerList;
    private SwipeRefreshLayout _swipe;
    private TextView _emptyHint;

    private GsFileBrowserListAdapter _filesystemViewerAdapter;
    private GsFileBrowserOptions.Options _dopt;
    private GsFileBrowserOptions.SelectionListener _callback;
    private AppSettings _appSettings;
    private Menu _fragmentMenu;
    private MarkorContextUtils _cu;
    private Toolbar _toolbar;
    private boolean _reloadRequiredOnResume = true;

    //########################
    //## Methods
    //########################

    public interface FilesystemFragmentOptionsListener {
        GsFileBrowserOptions.Options getFilesystemFragmentOptions(GsFileBrowserOptions.Options existingOptions);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);
        final Activity activity = getActivity();
        _recyclerList = root.findViewById(R.id.ui__filesystem_dialog__list);
        _swipe = root.findViewById(R.id.pull_to_refresh);
        _emptyHint = root.findViewById(R.id.empty_hint);
        _cu = new MarkorContextUtils(activity);
        _appSettings = AppSettings.get(activity);

        if (!(getActivity() instanceof FilesystemFragmentOptionsListener)) {
            throw new RuntimeException("Error: " + activity.getClass().getName() + " doesn't implement FilesystemFragmentOptionsListener");
        }
        setDialogOptions(((FilesystemFragmentOptionsListener) activity).getFilesystemFragmentOptions(_dopt));

        LinearLayoutManager lam = (LinearLayoutManager) _recyclerList.getLayoutManager();
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(activity, lam.getOrientation());
        _recyclerList.addItemDecoration(dividerItemDecoration);

        _filesystemViewerAdapter = new GsFileBrowserListAdapter(_dopt, activity);
        _recyclerList.setAdapter(_filesystemViewerAdapter);
        setReloadRequiredOnResume(false); // setAdapter will trigger a load

        _swipe.setOnRefreshListener(() -> {
            _filesystemViewerAdapter.reloadCurrentFolder();
            _swipe.setRefreshing(false);
        });

        if (FileSearchEngine.isSearchExecuting.get()) {
            FileSearchEngine.activity.set(new WeakReference<>(activity));
        }

        _toolbar = activity.findViewById(R.id.toolbar);
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
                onFsViewerCancel(_dopt.requestId);
                break;
            }

        }
    }

    @Override
    protected void onToolbarClicked(View v) {
        executeFilterNotebookAction();
    }

    private void checkOptions() {
        if (_dopt.doSelectFile && !_dopt.doSelectMultiple) {
            _dopt.okButtonEnable = false;
        }
    }

    @Override
    public void onFsViewerFolderLoad(final File newFolder) {
        if (_callback != null) {
            _callback.onFsViewerFolderLoad(newFolder);
        }

        _dopt.sortOrder = _appSettings.getFolderSortOrder(newFolder);
        _dopt.favouriteFiles = _appSettings.getFavouriteFiles();
        _dopt.recentFiles = _appSettings.getRecentFiles();
        _dopt.popularFiles = _appSettings.getPopularFiles();
        _dopt.descriptionFormat = _appSettings.getString(R.string.pref_key__file_description_format, "");
    }

    @Override
    public void onFsViewerSelected(String request, File file, final Integer lineNumber) {
        if (_callback != null) {
            _filesystemViewerAdapter.showFileAfterNextLoad(file);
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
    public void onFsViewerCancel(String request) {
        if (_callback != null) {
            _callback.onFsViewerCancel(_dopt.requestId);
        }
    }

    @Override
    public void onFsViewerConfig(GsFileBrowserOptions.Options dopt) {
        if (_callback != null) {
            _callback.onFsViewerConfig(dopt);
        }
    }

    @Override
    @SuppressLint("SetTextI18n")
    public void onFsViewerDoUiUpdate(GsFileBrowserListAdapter adapter) {
        if (_callback != null) {
            _callback.onFsViewerDoUiUpdate(adapter);
        }

        updateMenuItems();
        _emptyHint.setVisibility(adapter.isCurrentFolderEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onFsViewerNeutralButtonPressed(final File currentFolder) {
        if (_callback != null) {
            _callback.onFsViewerNeutralButtonPressed(currentFolder);
        }
    }

    private void updateMenuItems() {
        final Set<File> selFiles = _filesystemViewerAdapter.getCurrentSelection();
        final int selCount = selFiles.size();
        final int totalCount = _filesystemViewerAdapter.getItemCount() - 1;   // Account for ".."
        final boolean selMulti1 = _dopt.doSelectMultiple && selCount == 1;
        final boolean selMultiMore = _dopt.doSelectMultiple && selCount > 1;
        final boolean selMultiAny = selMultiMore || selMulti1;
        final boolean selFilesOnly = _filesystemViewerAdapter.isFilesOnlySelected();
        final boolean selInVirtualDirectory = _filesystemViewerAdapter.isCurrentFolderVirtual();

        // Check if is a favourite
        boolean selTextFilesOnly = true;
        boolean selDirectoriesOnly = true;
        boolean selWritable = true;
        boolean allSelectedFav = true;
        final Collection<File> favFiles = _dopt.favouriteFiles != null ? _dopt.favouriteFiles : Collections.emptySet();
        for (final File f : selFiles) {
            selTextFilesOnly &= FormatRegistry.isFileSupported(f, true);
            selWritable &= f.canWrite();
            selDirectoriesOnly &= f.isDirectory();
            allSelectedFav &= favFiles.contains(f);
        }

        if (_fragmentMenu != null && _fragmentMenu.findItem(R.id.action_delete_selected_items) != null) {
            _fragmentMenu.findItem(R.id.action_search).setVisible(selFiles.isEmpty() && !_filesystemViewerAdapter.isCurrentFolderVirtual());
            _fragmentMenu.findItem(R.id.action_delete_selected_items).setVisible((selMulti1 || selMultiMore) && selWritable);
            _fragmentMenu.findItem(R.id.action_rename_selected_item).setVisible(selMulti1 && selWritable & !selInVirtualDirectory);
            _fragmentMenu.findItem(R.id.action_info_selected_item).setVisible(selMulti1);
            _fragmentMenu.findItem(R.id.action_move_selected_items).setVisible((selMulti1 || selMultiMore) && selWritable && !selInVirtualDirectory && !_cu.isUnderStorageAccessFolder(getContext(), getCurrentFolder(), true));
            _fragmentMenu.findItem(R.id.action_copy_selected_items).setVisible((selMulti1 || selMultiMore) && selWritable && !_cu.isUnderStorageAccessFolder(getContext(), getCurrentFolder(), true));
            _fragmentMenu.findItem(R.id.action_share_files).setVisible(selFilesOnly && (selMulti1 || selMultiMore) && !_cu.isUnderStorageAccessFolder(getContext(), getCurrentFolder(), true));
            _fragmentMenu.findItem(R.id.action_go_to).setVisible(!_filesystemViewerAdapter.areItemsSelected());
            _fragmentMenu.findItem(R.id.action_sort).setVisible(_filesystemViewerAdapter.isCurrentFolderSortable() && !_filesystemViewerAdapter.areItemsSelected());
            _fragmentMenu.findItem(R.id.action_import).setVisible(!_filesystemViewerAdapter.areItemsSelected() && !_filesystemViewerAdapter.isCurrentFolderVirtual());
            _fragmentMenu.findItem(R.id.action_settings).setVisible(!_filesystemViewerAdapter.areItemsSelected());
            _fragmentMenu.findItem(R.id.action_favourite).setVisible(selMultiAny && !allSelectedFav);
            _fragmentMenu.findItem(R.id.action_favourite_remove).setVisible(selMultiAny && allSelectedFav);
            _fragmentMenu.findItem(R.id.action_fs_copy_to_clipboard).setVisible(selMulti1 && selTextFilesOnly);
            _fragmentMenu.findItem(R.id.action_create_shortcut).setVisible(selMulti1 && (selFilesOnly || selDirectoriesOnly));
            _fragmentMenu.findItem(R.id.action_check_all).setVisible(_filesystemViewerAdapter.areItemsSelected() && selCount < totalCount);
            _fragmentMenu.findItem(R.id.action_clear_selection).setVisible(_filesystemViewerAdapter.areItemsSelected());

            final MenuItem sortItem = _fragmentMenu.findItem(R.id.action_sort);
            if (sortItem != null) {
                _cu.tintDrawable(sortItem.getIcon(), _dopt.sortOrder.isFolderLocal ? GsFileBrowserListAdapter.FAVOURITE_COLOR : Color.WHITE);
            }
        }

        // Update subtitle with count
        if (_toolbar != null) {
            if (_filesystemViewerAdapter.areItemsSelected()) {
                _toolbar.setSubtitle(String.format("(%d / %d)", selCount, totalCount));
            } else {
                _toolbar.setSubtitle("");
            }
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
        if (_filesystemViewerAdapter != null && _filesystemViewerAdapter.goBack()) {
            return true;
        }
        return super.onBackPressed();
    }

    public void reloadCurrentFolder() {
        _filesystemViewerAdapter.reloadCurrentFolder();
    }

    public File getCurrentFolder() {
        return _filesystemViewerAdapter != null ? _filesystemViewerAdapter.getCurrentFolder() : null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        _filesystemViewerAdapter.saveInstanceState(outState);
    }

    @Override
    public void onViewStateRestored(final Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        _filesystemViewerAdapter.restoreSavedInstanceState(savedInstanceState);
    }

    public void setReloadRequiredOnResume(boolean reloadRequiredOnResume) {
        _reloadRequiredOnResume = reloadRequiredOnResume;
    }

    @Override
    public void onResume() {
        super.onResume();
        _dopt.listener.onFsViewerConfig(_dopt);
        final File folder = getCurrentFolder();
        final Activity activity = getActivity();
        if (_reloadRequiredOnResume && isVisible() && folder != null && activity != null) {
            reloadCurrentFolder();
        }
        _reloadRequiredOnResume = true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.filesystem__menu, menu);
        _cu.tintMenuItems(menu, true, Color.WHITE);
        _cu.setSubMenuIconsVisibility(menu, true);

        List<Pair<File, String>> sdcardFolders = _cu.getAppDataPublicDirs(getContext(), false, true, true);
        int[] sdcardResIds = {};
        for (int i = 0; i < sdcardResIds.length && i < sdcardFolders.size(); i++) {
            final MenuItem item = menu.findItem(sdcardResIds[i]);
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
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int _id = item.getItemId();
        final Set<File> currentSelection = _filesystemViewerAdapter.getCurrentSelection();

        switch (_id) {
            case R.id.action_create_shortcut: {
                final File file = currentSelection.iterator().next();
                _cu.createLauncherDesktopShortcut(getContext(), file);
                return true;
            }
            case R.id.action_sort: {
                updateSortSettings();
                return true;
            }
            case R.id.action_import: {
                showImportDialog();
                return true;
            }
            case R.id.action_search: {
                executeSearchAction();
                return true;
            }
            case R.id.action_go_to: {
                final File folder = new File("/storage");
                _filesystemViewerAdapter.setCurrentFolder(folder);
                return true;
            }
            case R.id.action_favourite: {
                if (_filesystemViewerAdapter.areItemsSelected()) {
                    _dopt.favouriteFiles = GsCollectionUtils.union(_dopt.favouriteFiles, currentSelection);
                    _appSettings.setFavouriteFiles(_dopt.favouriteFiles);
                    updateMenuItems();
                }
                return true;
            }
            case R.id.action_favourite_remove: {
                if (_filesystemViewerAdapter.areItemsSelected()) {
                    _dopt.favouriteFiles = GsCollectionUtils.setDiff(_dopt.favouriteFiles, currentSelection);
                    _appSettings.setFavouriteFiles(_dopt.favouriteFiles);
                    updateMenuItems();
                }
                return true;
            }
            case R.id.action_delete_selected_items: {
                MarkorDialogFactory.showConfirmDialog(
                        getActivity(),
                        R.string.confirm_delete,
                        null,
                        GsCollectionUtils.map(_filesystemViewerAdapter.getCurrentSelection(), File::getName),
                        () -> new Thread(() -> {
                            WrMarkorSingleton.getInstance().deleteSelectedItems(currentSelection, getContext());
                            _recyclerList.post(() -> _filesystemViewerAdapter.reloadCurrentFolder());
                        }).start()
                );
                return true;
            }
            case R.id.action_move_selected_items:
            case R.id.action_copy_selected_items: {
                askForMoveOrCopy(_id == R.id.action_move_selected_items);
                return true;
            }
            case R.id.action_check_all: {
                _filesystemViewerAdapter.selectAll();
                return true;
            }
            case R.id.action_clear_selection: {
                _filesystemViewerAdapter.unselectAll();
                return true;
            }
            case R.id.action_share_files: {
                MarkorContextUtils s = new MarkorContextUtils(getContext());
                s.shareStreamMultiple(getContext(), currentSelection, "*/*");
                _filesystemViewerAdapter.reloadCurrentFolder();
                return true;
            }
            case R.id.action_info_selected_item: {
                if (_filesystemViewerAdapter.areItemsSelected()) {
                    File file = new ArrayList<>(currentSelection).get(0);
                    FileInfoDialog.show(file, getChildFragmentManager());
                }
                return true;
            }
            case R.id.action_rename_selected_item: {
                if (_filesystemViewerAdapter.areItemsSelected()) {
                    final File file = currentSelection.iterator().next();
                    final WrRenameDialog renameDialog = WrRenameDialog.newInstance(file, renamedFile -> reloadCurrentFolder());
                    renameDialog.show(getChildFragmentManager(), WrRenameDialog.FRAGMENT_TAG);
                }
                return true;
            }
            case R.id.action_fs_copy_to_clipboard: {
                if (_filesystemViewerAdapter.areItemsSelected()) {
                    final File file = new ArrayList<>(currentSelection).get(0);
                    if (FormatRegistry.isFileSupported(file, true)) {
                        _cu.setClipboard(getContext(), GsFileUtils.readTextFileFast(file).first);
                        Toast.makeText(getContext(), R.string.clipboard, Toast.LENGTH_SHORT).show();
                    }
                }
                return true;
            }
        }

        return false;
    }

    private void searchCallback(final File load, final Integer lineNumber, final boolean longPress) {
        if (!longPress) {
            if (load.isDirectory()) {
                _filesystemViewerAdapter.setCurrentFolder(load);
            } else {
                onFsViewerSelected("", load, lineNumber);
            }
        } else {
            _filesystemViewerAdapter.showFile(load);
        }
    }

    private void executeSearchAction() {
        MarkorDialogFactory.showSearchFilesDialog(getActivity(), getCurrentFolder(), this::searchCallback);
    }

    final GsSearchOrCustomTextDialog.DialogState _filterDialogState = new GsSearchOrCustomTextDialog.DialogState();

    private void executeFilterNotebookAction() {
        MarkorDialogFactory.showNotebookFilterDialog(getActivity(), _filterDialogState, null,
                (file, show) -> {
                    if (show) {
                        _filesystemViewerAdapter.showFile(file);
                    } else {
                        searchCallback(file, null, false);
                    }
                });
    }

    public void clearSelection() {
        if (_filesystemViewerAdapter != null) { // Happens when restoring after rotation etc
            _filesystemViewerAdapter.unselectAll();
        }
    }


    ///////////////

    private void askForMoveOrCopy(final boolean isMove) {
        final List<File> files = new ArrayList<>(_filesystemViewerAdapter.getCurrentSelection());
        MarkorFileBrowserFactory.showFolderDialog(new GsFileBrowserOptions.SelectionListenerAdapter() {

            @Override
            public void onFsViewerSelected(String request, File file, Integer lineNumber) {
                super.onFsViewerSelected(request, file, null);
                WrMarkorSingleton.getInstance().moveOrCopySelected(files, file, getActivity(), isMove);
                _filesystemViewerAdapter.unselectAll();
                _filesystemViewerAdapter.reloadCurrentFolder();
            }

            @Override
            public void onFsViewerConfig(GsFileBrowserOptions.Options dopt) {
                dopt.titleText = isMove ? R.string.move : R.string.copy;
                dopt.rootFolder = GsFileBrowserListAdapter.VIRTUAL_STORAGE_ROOT;
                dopt.startFolder = getCurrentFolder();
                // Directories cannot be moved into themselves. Don't give users the option
                final Set<String> selSet = new HashSet<>();
                for (final File f : files) {
                    selSet.add(f.getAbsolutePath());
                }
                dopt.fileOverallFilter = (context, test) -> !selSet.contains(test.getAbsolutePath());
            }

            @Override
            public void onFsViewerCancel(final String request) {
                super.onFsViewerCancel(request);
                _filesystemViewerAdapter.reloadCurrentFolder(); // May be new folders
            }
        }, getChildFragmentManager(), getActivity());
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
        }, getChildFragmentManager(), getActivity(), null);
    }

    private void importFile(final File file) {
        final Activity activity = getActivity();
        if (new File(getCurrentFolder().getAbsolutePath(), file.getName()).exists()) {
            // Ask if overwriting is okay
            MarkorDialogFactory.showConfirmDialog(
                    activity,
                    R.string.confirm_overwrite,
                    getString(R.string.file_already_exists_overwerite) + "\n[" + file.getName() + "]",
                    null,
                    () -> importFileToCurrentDirectory(activity, file)
            );
        } else {
            // Import
            importFileToCurrentDirectory(activity, file);
        }
    }

    private void importFileToCurrentDirectory(Context context, File sourceFile) {
        GsFileUtils.copyFile(sourceFile, new File(getCurrentFolder().getAbsolutePath(), sourceFile.getName()));
        Toast.makeText(context, getString(R.string.import_) + ": " + sourceFile.getName(), Toast.LENGTH_LONG).show();
    }

    public GsFileBrowserOptions.Options getOptions() {
        return _dopt;
    }

    private void updateSortSettings() {
        final GsFileUtils.SortOrder globalOrder = _appSettings.getFolderSortOrder(null);
        MarkorDialogFactory.showFolderSortDialog(getActivity(), _dopt.sortOrder, globalOrder,
                (order) -> {
                    // Erase local sort order if local is unset
                    final File currentFolder = getCurrentFolder();
                    if (_dopt.sortOrder.isFolderLocal && !order.isFolderLocal) {
                        _appSettings.setFolderSortOrder(currentFolder, null);
                    }

                    // Set new sort order to folder or global as needed
                    _dopt.sortOrder = order;
                    _appSettings.setFolderSortOrder(order.isFolderLocal ? currentFolder : null, _dopt.sortOrder);

                    // Ui will be updated by onFsViewerDoUiUpdate after the load
                    reloadCurrentFolder();
                });
    }

    @Override
    public AppSettings createAppSettingsInstance(Context context) {
        return AppSettings.get(context);
    }

    @Override
    public MarkorContextUtils createContextUtilsInstance(Context context) {
        return new MarkorContextUtils(context);
    }
}
