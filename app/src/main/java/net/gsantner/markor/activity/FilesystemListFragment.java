package net.gsantner.markor.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
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
import net.gsantner.markor.model.Constants;
import net.gsantner.markor.model.MarkorSingleton;
import net.gsantner.markor.util.AppCast;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ContextUtils;
import net.gsantner.opoc.ui.FilesystemDialogData;

import java.io.File;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnItemClick;

public class FilesystemListFragment extends Fragment {

    public static final int RENAME_CONTEXT_BUTTON_ID = 103;

    @BindView(R.id.filesystemlist__fragment__listview)
    public ListView _filesListView;

    @BindView(R.id.filesystemlist__fragment__background_hint_text)
    public TextView _background_hint_text;

    private NotesAdapter _filesAdapter;


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
        if (appSettings.isRememberLastDirectory()) {
            String rememberedDir = appSettings.getLastOpenedDirectory();
            _currentDir = (rememberedDir != null) ? new File(rememberedDir) : null;
        }

        // Two-fold check, in case user doesn't have the preference to remember directories enabled
        // This code remembers last directory WITHIN the app (not leaving it)
        if (_currentDir == null) {
            _currentDir = (_markorSingleton.getNotesLastDirectory() != null) ? _markorSingleton.getNotesLastDirectory() : _rootDir;
        }
    }

    private void saveCurrentFolder() {
        AppSettings appSettings = AppSettings.get();
        SharedPreferences pm = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        if (appSettings.isRememberLastDirectory()) {
            String saveDir = (_currentDir == null) ? _rootDir.getAbsolutePath() : _currentDir.getAbsolutePath();
            appSettings.setLastOpenedDirectory(saveDir);
        }

        _markorSingleton.setNotesLastDirectory(_currentDir);
    }

    private void confirmDelete() {
        ConfirmDialog confirmDialog = ConfirmDialog.newInstance(ConfirmDialog.WHAT_DELETE, getSelectedItems());
        confirmDialog.show(getFragmentManager(), ConfirmDialog.FRAGMENT_TAG);
    }

    private void promptForMoveDirectory() {
        FilesystemDialogCreator.showFolderDialog(new FilesystemDialogData.SelectionAdapter() {
            @Override
            public void onFsSelected(String request, File file) {
                super.onFsSelected(request, file);
                MarkorSingleton.getInstance().moveSelectedNotes(getSelectedItems(), file.getAbsolutePath());
                listFilesInDirectory(getCurrentDir());
                finishActionMode();
            }

            @Override
            public void onFsDialogConfig(FilesystemDialogData.Options opt) {
                opt.titleText = R.string.select_folder_move;
                opt.rootFolder = new File(AppSettings.get().getSaveDirectory());
            }
        }, getActivity().getSupportFragmentManager(), getActivity());
    }


    public void listFilesInDirectory(File directory) {
        reloadFiles(directory);
        broadcastDirectoryChange(directory, _rootDir);
        showEmptyDirHintIfEmpty();
        reloadAdapter();
    }

    private void broadcastDirectoryChange(File directory, File rootDir) {
        AppCast.CURRENT_FOLDER_CHANGED.send(getActivity(), directory.getAbsolutePath(), rootDir.getAbsolutePath());
        clearSearchFilter();
    }

    private void reloadFiles(File directory) {

        try {
            // Load from SD card
            _filesCurrentlyShown = MarkorSingleton.getInstance().addFilesFromDirectory(directory, new ArrayList<File>());
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

    public ArrayList<File> getSelectedItems() {
        return _selectedItems;
    }

    public boolean onRooDir() {
        return _markorSingleton.isRootDir(_currentDir, _rootDir);
    }

    private class ActionModeCallback implements ListView.MultiChoiceModeListener {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.notes_context_menu, menu);
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
                    return true;
                case R.id.context_menu_move:
                    promptForMoveDirectory();
                    return true;
                case RENAME_CONTEXT_BUTTON_ID:
                    promptForNewName(_selectedItems.get(0));
                    return true;
                default:
                    return false;
            }
        }

        private void promptForNewName(File renameable) {
            FragmentManager fragManager = getFragmentManager();
            Bundle args = new Bundle();
            args.putString(Constants.SOURCE_FILE, renameable.getAbsolutePath());
            RenameDialog renameDialog = new RenameDialog();
            renameDialog.setArguments(args);
            renameDialog.show(fragManager, Constants.RENAME_DIALOG_TAG);
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode actionMode, int i, long l, boolean checked) {

            switch (_filesListView.getCheckedItemCount()) {
                case 0:
                    actionMode.setSubtitle(null);
                    hideRenameButton(actionMode);
                    break;
                case 1:
                    actionMode.setSubtitle(getResources().getString(R.string.one_item_selected));
                    manageClickedVIew(i, checked);
                    showRenameButton(actionMode);
                    break;
                default:
                    manageClickedVIew(i, checked);
                    actionMode.setSubtitle(String.format(getResources().getString(R.string.more_items_selected), _filesListView.getCheckedItemCount()));
                    hideRenameButton(actionMode);
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

        private void hideRenameButton(ActionMode actionMode) {
            showRenameContextButton(actionMode.getMenu(), false);
        }

        private void showRenameButton(ActionMode actionMode) {
            showRenameContextButton(actionMode.getMenu(), true);
        }

        private void showRenameContextButton(Menu menu, boolean show) {
            if (show) {
                menu.add(Menu.FIRST + 1, RENAME_CONTEXT_BUTTON_ID, Menu.FIRST, R.string.rename)
                        .setIcon(R.drawable.ic_edit_light);

            } else {
                menu.setGroupVisible(1, false);
                menu.removeItem(RENAME_CONTEXT_BUTTON_ID);
            }
            ;
        }
    }

    @OnItemClick(R.id.filesystemlist__fragment__listview)
    public void onNotesItemClickListener(AdapterView<?> adapterView, View view, int i, long l) {
        File file = (File) _simpleSectionAdapter.getItem(i);
        Context context = view.getContext();

        // Refresh list if directory, else import
        if (file.isDirectory()) {
            _currentDir = file;
            listFilesInDirectory(file);
        } else {

            File note = (File) _simpleSectionAdapter.getItem(i);
            Intent intent;

            if (AppSettings.get().isPreviewFirst()) {
                intent = new Intent(context, PreviewActivity.class);

                // .replace is a workaround for Markdown lists requiring two \n characters
                if (note != null) {
                    Uri uriBase = null;
                    if (note.getParentFile() != null) {
                        uriBase = Uri.parse(note.getParentFile().toURI().toString());
                    }

                    intent.putExtra(Constants.MD_PREVIEW_BASE, uriBase.toString());
                }

                Uri noteUri = Uri.parse(note.toURI().toString());
                String content = MarkorSingleton.getInstance().readFileUri(noteUri, context);
                intent.putExtra(Constants.MD_PREVIEW_KEY, content.replace("\n-", "\n\n-"));
            } else {
                intent = new Intent(context, NoteActivity.class);
            }
            intent.putExtra(Constants.NOTE_KEY, note);

            startActivity(intent);
            getActivity().overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_left);
        }
    }

    private class PreviousDirClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            goToPreviousDir();
        }
    }
}
