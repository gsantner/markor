package me.writeily.pro;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

import me.writeily.pro.adapter.NotesAdapter;
import me.writeily.pro.dialog.ConfirmDialog;
import me.writeily.pro.dialog.FilesystemDialog;
import me.writeily.pro.model.Constants;
import me.writeily.pro.model.WriteilySingleton;

/**
 * Created by jeff on 2014-04-11.
 */
public class NotesFragment extends Fragment {

    private Context context;

    private View layoutView;

    private ListView filesListView;
    private TextView hintTextView;
    private Button previousDirButton;

    private File rootDir;
    private File currentDir;

    private WriteilySingleton writeilySingleton;

    private ArrayList<File> files;

    private NotesAdapter filesAdapter;
    private ActionMode actionMode;


    public NotesFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        layoutView = inflater.inflate(R.layout.notes_fragment, container, false);
        hintTextView = (TextView) layoutView.findViewById(R.id.empty_hint);

        if (files == null) {
            files = new ArrayList<File>();
            hintTextView.setVisibility(View.VISIBLE);
            hintTextView.setText(getString(R.string.empty_notes_list_hint));
        }

        checkIfDataEmpty();

        context = getActivity().getApplicationContext();
        filesListView = (ListView) layoutView.findViewById(R.id.notes_listview);
        filesAdapter = new NotesAdapter(context, files);

        filesListView.setOnItemClickListener(new NotesItemClickListener());
        filesListView.setMultiChoiceModeListener(new ActionModeCallback());
        filesListView.setAdapter(filesAdapter);

        previousDirButton = (Button) layoutView.findViewById(R.id.import_header_btn);
        previousDirButton.setOnClickListener(new PreviousDirClickListener());

        rootDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + Constants.WRITEILY_FOLDER);

        return layoutView;
    }

    @Override
    public void onResume() {
        writeilySingleton = WriteilySingleton.getInstance();
        retrieveCurrentFolder();
        listFilesInDirectory(currentDir);

        setupAppearancePreferences();
        super.onResume();
    }

    @Override
    public void onPause() {
        saveCurrentFolder();
        super.onPause();
    }

    private void setupAppearancePreferences() {
        String theme = PreferenceManager.getDefaultSharedPreferences(context).getString(getString(R.string.pref_theme_key), "");

        if (theme.equals(getString(R.string.theme_dark))) {
            previousDirButton.setBackgroundColor(getResources().getColor(R.color.grey));
            previousDirButton.setTextColor(getResources().getColor(R.color.lighter_grey));
        } else {
            previousDirButton.setBackgroundColor(getResources().getColor(R.color.lighter_grey));
            previousDirButton.setTextColor(getResources().getColor(R.color.dark_grey));
        }

    }

    private void retrieveCurrentFolder() {
        SharedPreferences pm = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isLastDirStored = pm.getBoolean(getString(R.string.pref_remember_directory_key), false);
        if (isLastDirStored) {
            String rememberedDir = pm.getString(getString(R.string.pref_last_open_directory), null);
            currentDir = (rememberedDir != null) ? new File(rememberedDir) : null;
        }

        // Two-fold check, in case user doesn't have the preference to remember directories enabled
        // This code remembers last directory WITHIN the app (not leaving it)
        if (currentDir == null) {
            currentDir = (writeilySingleton.getNotesLastDirectory() != null) ? writeilySingleton.getNotesLastDirectory() : rootDir;
        }
    }

    private void saveCurrentFolder() {
        SharedPreferences pm = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isLastDirStored = pm.getBoolean(getString(R.string.pref_remember_directory_key), false);

        if (isLastDirStored) {
            String saveDir = (currentDir == null) ? rootDir.getAbsolutePath() : currentDir.getAbsolutePath();
            pm.edit().putString(getString(R.string.pref_last_open_directory), saveDir).apply();
        }

        writeilySingleton.setNotesLastDirectory(currentDir);
    }

    private void confirmDelete() {
        FragmentManager fragManager = getFragmentManager();
        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.show(fragManager, Constants.CONFIRM_DIALOG_TAG);
    }

    private void promptForDirectory() {
        FragmentManager fragManager = getFragmentManager();

        Bundle args = new Bundle();
        args.putString(Constants.FILESYSTEM_ACTIVITY_ACCESS_TYPE_KEY, Constants.FILESYSTEM_FOLDER_ACCESS_TYPE);

        FilesystemDialog filesystemDialog = new FilesystemDialog();
        filesystemDialog.setArguments(args);
        filesystemDialog.show(fragManager, Constants.FILESYSTEM_MOVE_DIALOG_TAG);
    }

    private void checkIfDataEmpty() {
        if (files.isEmpty()) {
            hintTextView.setVisibility(View.VISIBLE);
            hintTextView.setText(getString(R.string.empty_notes_list_hint));
        } else {
            hintTextView.setVisibility(View.INVISIBLE);
        }
    }

    public void listFilesInCurrentDirectory() { listFilesInDirectory(new File(getCurrentDir())); }

    private void listFilesInDirectory(File directory) {
        files = new ArrayList<File>();

        try {
            // Load from SD card
            files = WriteilySingleton.getInstance().addFilesFromDirectory(directory, files);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Refresh the files adapter with the new ArrayList
        if (filesAdapter != null) {
            filesAdapter = new NotesAdapter(context, files);
            filesListView.setAdapter(filesAdapter);
        }

        checkDirectoryStatus();
    }

    private void goToPreviousDir() {
        if (currentDir != null) {
            currentDir = currentDir.getParentFile();
        }

        listFilesInDirectory(currentDir);
    }

    private void checkDirectoryStatus() {
        if (writeilySingleton.isRootDir(currentDir, rootDir)) {
            previousDirButton.setVisibility(View.GONE);
            currentDir = null;
        } else {
            previousDirButton.setVisibility(View.VISIBLE);
        }

        // Check if dir is empty
        if (writeilySingleton.isDirectoryEmpty(files)) {
            hintTextView.setVisibility(View.VISIBLE);
            hintTextView.setText(getString(R.string.empty_directory));
        } else {
            hintTextView.setVisibility(View.INVISIBLE);
        }
    }

    public String getCurrentDir() {
        return (currentDir == null) ? getRootDir() : currentDir.getAbsolutePath();
    }

    public void setCurrentDir(File dir) {
        currentDir = dir;
    }

    public String getRootDir() {
        return rootDir.getAbsolutePath();
    }

    public ListView getFilesListView() {
        return filesListView;
    }

    public NotesAdapter getFilesAdapter() {
        return filesAdapter;
    }

    public void finishActionMode() {
        actionMode.finish();
    }

    /** Search **/
    public void search(CharSequence query) {
        if (query.length() > 0) {
            filesAdapter.getFilter().filter(query);
        }
    }

    public void clearSearchFilter() {
        filesAdapter.getFilter().filter("");

        // Workaround to an (apparently) bug in Android's ArrayAdapter... not pretty
        filesAdapter = new NotesAdapter(context, files);
        filesListView.setAdapter(filesAdapter);
        filesAdapter.notifyDataSetChanged();
    }

    public void clearItemSelection() {
        filesAdapter.notifyDataSetChanged();
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
            actionMode = mode;
            switch (item.getItemId()) {
                case R.id.context_menu_delete:
                    confirmDelete();
                    return true;
                case R.id.context_menu_move:
                    promptForDirectory();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode actionMode, int i, long l, boolean b) {
            final int numSelected = filesListView.getCheckedItemCount();

            switch (numSelected) {
                case 0:
                    actionMode.setSubtitle(null);
                    break;
                case 1:
                    actionMode.setSubtitle(getResources().getString(R.string.one_item_selected));
                    break;
                default:
                    actionMode.setSubtitle(String.format(getResources().getString(R.string.more_items_selected), numSelected));
                    break;
            }
        }
    };

    private class NotesItemClickListener implements android.widget.AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            File file = filesAdapter.getItem(i);

            // Refresh list if directory, else import
            if (file.isDirectory()) {
                currentDir = file;
                listFilesInDirectory(file);
            } else {
                File note = filesAdapter.getItem(i);

                Intent intent = new Intent(context, NoteActivity.class);
                intent.putExtra(Constants.NOTE_SOURCE_DIR, getCurrentDir());
                intent.putExtra(Constants.NOTE_KEY, note);

                startActivity(intent);
                getActivity().overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_left);
            }
        }
    }

    private class PreviousDirClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            goToPreviousDir();
        }
    }
}
