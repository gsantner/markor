/*
 * Copyright (c) 2014 Jeff Martin
 * Copyright (c) 2015 Pedro Lafuente
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.activity;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.SearchView;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.mobsandgeeks.adapters.Sectionizer;
import com.mobsandgeeks.adapters.SimpleSectionAdapter;

import net.gsantner.markor.R;
import net.gsantner.markor.adapter.NotesAdapter;
import net.gsantner.markor.dialog.ConfirmDialog;
import net.gsantner.markor.dialog.FilesystemDialogCreator;
import net.gsantner.markor.dialog.RenameDialog;
import net.gsantner.markor.model.DocumentLoader;
import net.gsantner.markor.model.MarkorSingleton;
import net.gsantner.markor.ui.BaseFragment;
import net.gsantner.markor.util.AppCast;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ContextUtils;
import net.gsantner.opoc.ui.FilesystemDialogData;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnItemClick;

import static android.content.Context.SEARCH_SERVICE;

public class FilesystemListFragment extends BaseFragment {
    public static final int SORT_BY_DATE = 0;
    public static final int SORT_BY_NAME = 1;
    public static final int SORT_BY_FILESIZE = 2;
    public static final String FRAGMENT_TAG = "FilesystemListFragment";

    @BindView(R.id.filesystemlist__fragment__listview)
    public ListView _filesListView;

    @BindView(R.id.filesystemlist__fragment__background_hint_text)
    public TextView _background_hint_text;

    private NotesAdapter _filesAdapter;


    private SearchView _searchView;
    private MenuItem _searchItem;

    private ArrayList<File> _filesCurrentlyShown = new ArrayList<>();
    private ArrayList<File> _selectedItems = new ArrayList<>();
    private SimpleSectionAdapter<File> _simpleSectionAdapter;
    private MarkorSingleton _markorSingleton;
    private ActionMode _actionMode;
    private File _currentDir;
    private File _rootDir;
    private Sectionizer<File> _sectionizer = new Sectionizer<File>() {
        @Override
        public String getSectionTitleForItem(File instance) {
            return instance.isDirectory() ? getString(R.string.folders) : getString(R.string.files);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ContextUtils.get().setAppLanguage(AppSettings.get().getLanguage());
        View root = inflater.inflate(R.layout.filesystemlist__fragment, container, false);
        ButterKnife.bind(this, root);
        return root;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Context context = getActivity();
        _filesAdapter = new NotesAdapter(context, 0, _filesCurrentlyShown);
        _simpleSectionAdapter = new SimpleSectionAdapter<>(context, _filesAdapter,
                R.layout.ui__text__item,
                R.id.notes_fragment_section_text, _sectionizer);

        _filesListView.setMultiChoiceModeListener(new ActionModeCallback());
        _filesListView.setAdapter(_simpleSectionAdapter);
        _rootDir = getRootFolderFromPrefsOrDefault();
    }

    @Override
    public void onResume() {
        super.onResume();
        _markorSingleton = MarkorSingleton.getInstance();
        File possiblyNewRootDir = getRootFolderFromPrefsOrDefault();
        if (possiblyNewRootDir != _rootDir) {
            _rootDir = possiblyNewRootDir;
            _currentDir = possiblyNewRootDir;
        }
        retrieveCurrentFolder();
        listFilesInDirectory(getCurrentDir());
    }

    private File getRootFolderFromPrefsOrDefault() {
        return new File(AppSettings.get().getSaveDirectory());
    }

    @Override
    public void onPause() {
        super.onPause();
        saveCurrentFolder();
    }

    private void retrieveCurrentFolder() {
        AppSettings appSettings = AppSettings.get();
        String rememberedDir = appSettings.getLastOpenedDirectory();
        _currentDir = (rememberedDir != null) ? new File(rememberedDir) : null;
        // Two-fold check, in case user doesn't have the preference to remember directories enabled
        // This code remembers last directory WITHIN the app (not leaving it)
        if (_currentDir == null) {
            _currentDir = (_markorSingleton.getNotesLastDirectory() != null) ? _markorSingleton.getNotesLastDirectory() : _rootDir;
        }
    }

    private void saveCurrentFolder() {
        AppSettings appSettings = AppSettings.get();
        String saveDir = (_currentDir == null) ? _rootDir.getAbsolutePath() : _currentDir.getAbsolutePath();
        appSettings.setLastOpenedDirectory(saveDir);
        _markorSingleton.setNotesLastDirectory(_currentDir);
    }

    private void confirmDelete() {
        final ArrayList<File> itemsToDelete = new ArrayList<>(_selectedItems);
        String message = String.format(getString(R.string.confirm_delete_description), getResources().getQuantityString(R.plurals.documents, itemsToDelete.size()));
        ConfirmDialog confirmDialog = ConfirmDialog.newInstance(
                getString(R.string.confirm_delete), message, itemsToDelete,
                new ConfirmDialog.ConfirmDialogCallback() {
                    @Override
                    public void onConfirmDialogAnswer(boolean confirmed, Serializable data) {
                        if (confirmed) {
                            MarkorSingleton.getInstance().deleteSelectedItems(itemsToDelete);
                            listFilesInDirectory(getCurrentDir());
                            finishActionMode();
                        }
                    }
                });
        confirmDialog.show(getActivity().getSupportFragmentManager(), ConfirmDialog.FRAGMENT_TAG);
    }

    private void promptForMoveDirectory() {
        final ArrayList<File> filesToMove = new ArrayList<>(_selectedItems);
        FilesystemDialogCreator.showFolderDialog(new FilesystemDialogData.SelectionListenerAdapter() {
            @Override
            public void onFsSelected(String request, File file) {
                super.onFsSelected(request, file);
                MarkorSingleton.getInstance().moveSelectedNotes(filesToMove, file.getAbsolutePath());
                listFilesInDirectory(getCurrentDir());
                finishActionMode();
            }

            @Override
            public void onFsDialogConfig(FilesystemDialogData.Options opt) {
                opt.titleText = R.string.move;
                opt.rootFolder = new File(AppSettings.get().getSaveDirectory());
            }
        }, getActivity().getSupportFragmentManager(), getActivity());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.filesystem__menu, menu);

        _searchItem = menu.findItem(R.id.action_search);
        _searchView = (SearchView) _searchItem.getActionView();

        SearchManager searchManager = (SearchManager) getActivity().getSystemService(SEARCH_SERVICE);
        _searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
        _searchView.setQueryHint(getString(R.string.search_hint));
        if (_searchView != null) {
            _searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    if (query != null) {
                        if (isVisible())
                            search(query);
                    }
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    if (newText != null) {
                        if (isVisible()) {
                            if (newText.equalsIgnoreCase("")) {
                                clearSearchFilter();
                            } else {
                                search(newText);
                            }
                        }
                    }
                    return false;
                }
            });
            _searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    menu.findItem(R.id.action_import).setVisible(hasFocus);
                    if (!hasFocus) {
                        _searchItem.collapseActionView();
                    }
                }
            });
        }
        ContextUtils cu = ContextUtils.get();

        cu.tintMenuItems(menu, true, Color.WHITE);
        cu.setSubMenuIconsVisiblity(menu, true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_sort_by_name: {
                AppSettings.get().setSortMethod(FilesystemListFragment.SORT_BY_NAME);
                sortAdapter();
                return true;
            }
            case R.id.action_sort_by_date: {
                AppSettings.get().setSortMethod(FilesystemListFragment.SORT_BY_DATE);
                sortAdapter();
                return true;
            }
            case R.id.action_sort_by_filesize: {
                AppSettings.get().setSortMethod(FilesystemListFragment.SORT_BY_FILESIZE);
                sortAdapter();
                return true;
            }

            case R.id.action_sort_reverse: {
                item.setChecked(!item.isChecked());
                AppSettings.get().setSortReverse(item.isChecked());
                sortAdapter();
                return true;
            }
        }
        return false;
    }

    public void listFilesInDirectory(File directory) {
        _selectedItems.clear();
        reloadFiles(directory);
        broadcastDirectoryChange(directory);
        showEmptyDirHintIfEmpty();
        sortAdapter();
    }

    private void broadcastDirectoryChange(File directory) {
        AppCast.VIEW_FOLDER_CHANGED.send(getActivity(), directory.getAbsolutePath(), false);
        clearSearchFilter();
    }

    private void reloadFiles(File directory) {

        try {
            // Load from SD card
            _filesCurrentlyShown = MarkorSingleton.getInstance().addMarkdownFilesFromDirectory(directory, new ArrayList<File>());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void reloadAdapter() {
        if (_filesAdapter != null) {
            _filesAdapter = new NotesAdapter(getActivity().getApplicationContext(), 0, _filesCurrentlyShown);
            _simpleSectionAdapter =
                    new SimpleSectionAdapter<>(getActivity().getApplicationContext()
                            , _filesAdapter, R.layout.ui__text__item
                            , R.id.notes_fragment_section_text, _sectionizer);
            _filesListView.setAdapter(_simpleSectionAdapter);
            _simpleSectionAdapter.notifyDataSetChanged();
        }
    }

    public void goToPreviousDir() {
        if (_currentDir != null) {
            _currentDir = _currentDir.getParentFile();
        }

        listFilesInDirectory(getCurrentDir());
    }

    private void showEmptyDirHintIfEmpty() {
        if (_markorSingleton.isDirectoryEmpty(_filesCurrentlyShown)) {
            _background_hint_text.setVisibility(View.VISIBLE);
            _background_hint_text.setText(getString(R.string.empty_directory));
        } else {
            _background_hint_text.setVisibility(View.INVISIBLE);
        }
    }

    public File getCurrentDir() {
        return (_currentDir == null) ? getRootDir() : _currentDir.getAbsoluteFile();
    }

    public File getRootDir() {
        return _rootDir.getAbsoluteFile();
    }

    public void finishActionMode() {
        _actionMode.finish();
        _selectedItems.clear();
    }

    /**
     * Search
     **/
    public void search(CharSequence query) {
        if (query.length() > 0) {
            _filesAdapter.getFilter().filter(query);
            _simpleSectionAdapter.notifyDataSetChanged();
        }
    }

    public void clearSearchFilter() {
        _filesAdapter.getFilter().filter("");
        _simpleSectionAdapter.notifyDataSetChanged();
        reloadAdapter();
    }

    public void sortAdapter() {
        final int sortMethod = AppSettings.get().getSortMethod();
        final boolean sortReverse = AppSettings.get().isSortReverse();
        int count = _filesCurrentlyShown.size();
        int lastFolderIndex = 0;
        for (int i = 0; i < count; i++) {
            if (_filesCurrentlyShown.get(i).isDirectory()) {
                lastFolderIndex++;
            }
        }

        Comparator<File> comparator = new Comparator<File>() {
            @Override
            public int compare(File file, File other) {
                if (sortReverse) {
                    File swap = file;
                    file = other;
                    other = swap;
                }

                switch (sortMethod) {
                    case SORT_BY_NAME:
                        return file.compareTo(other);
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

        Collections.sort(_filesCurrentlyShown.subList(0, lastFolderIndex), comparator);
        Collections.sort(_filesCurrentlyShown.subList(lastFolderIndex, count), comparator);

        reloadAdapter();
    }

    public ArrayList<File> getSelectedItems() {
        return _selectedItems;
    }

    public boolean onRooDir() {
        return _markorSingleton.isRootDir(_currentDir, _rootDir);
    }

    @Override
    public String getFragmentTag() {
        return FRAGMENT_TAG;
    }

    @Override
    public boolean onBackPressed() {
        if (_searchView.isFocused()) {
            _searchView.clearFocus();
            _searchView.setSelected(false);
            return true;
        }
        if (!_searchView.getQuery().toString().isEmpty() || !_searchView.isIconified()) {
            _searchView.setQuery("", false);
            _searchView.setIconified(true);
            _searchItem.collapseActionView();
            return true;
        }

        return false;
    }

    private class ActionModeCallback implements ListView.MultiChoiceModeListener {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.filesystem__context_menu, menu);
            mode.setTitle(getResources().getString(R.string.select_elements));
            return true;
        }


        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            _actionMode = mode;
            switch (item.getItemId()) {
                case R.id.context_menu_delete:
                    confirmDelete();
                    finishActionMode();
                    return true;
                case R.id.context_menu_move:
                    promptForMoveDirectory();
                    finishActionMode();
                    return true;
                case R.id.context_menu_rename:
                    promptForNewName(_selectedItems.get(0));
                    finishActionMode();
                    return true;
                default:
                    return false;
            }
        }

        private void promptForNewName(File file) {
            RenameDialog renameDialog = RenameDialog.newInstance(file);
            renameDialog.show(getFragmentManager(), RenameDialog.FRAGMENT_TAG);
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode actionMode, int i, long l, boolean checked) {

            switch (_filesListView.getCheckedItemCount()) {
                case 0:
                    actionMode.setSubtitle(null);
                    setRenameButtonVisibility(actionMode, false);
                    break;
                case 1:
                    actionMode.setSubtitle(getResources().getString(R.string.one_item_selected));
                    manageClickedVIew(i, checked);
                    setRenameButtonVisibility(actionMode, true);
                    break;
                default:
                    manageClickedVIew(i, checked);
                    actionMode.setSubtitle(String.format(getResources().getString(R.string.more_items_selected), _filesListView.getCheckedItemCount()));
                    setRenameButtonVisibility(actionMode, false);
                    break;
            }
        }

        private void manageClickedVIew(int i, boolean checked) {
            if (checked) {
                _selectedItems.add((File) _simpleSectionAdapter.getItem(i));
            } else {
                _selectedItems.remove((File) _simpleSectionAdapter.getItem(i));
            }
        }

        private void setRenameButtonVisibility(ActionMode actionMode, boolean visible) {
            Menu menu = actionMode.getMenu();
            MenuItem item = menu.findItem(R.id.context_menu_rename);
            item.setVisible(visible);
        }
    } // End: Action Mode callback

    @OnItemClick(R.id.filesystemlist__fragment__listview)
    public void onNotesItemClickListener(AdapterView<?> adapterView, View view, int i, long l) {
        File clickedFile = (File) _simpleSectionAdapter.getItem(i);
        Context context = view.getContext();

        // Refresh list if directory, else import
        if (clickedFile.isDirectory()) {
            _currentDir = clickedFile;
            listFilesInDirectory(clickedFile);
        } else {
            Intent intent = new Intent(context, DocumentActivity.class);
            intent.putExtra(DocumentLoader.EXTRA_PATH, clickedFile);
            intent.putExtra(DocumentLoader.EXTRA_PATH_IS_FOLDER, false);
            startActivity(intent);
        }
    }
}
