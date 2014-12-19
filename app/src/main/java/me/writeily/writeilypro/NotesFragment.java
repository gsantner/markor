package me.writeily.writeilypro;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
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

import me.writeily.writeilypro.adapter.NotesAdapter;
import me.writeily.writeilypro.dialog.FilesystemDialog;
import me.writeily.writeilypro.model.Constants;
import me.writeily.writeilypro.model.WriteilySingleton;

import java.io.File;
import java.util.ArrayList;

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
        listFilesInDirectory(rootDir);
        super.onResume();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == Constants.FILESYSTEM_ACTIVITY_FOLDER_REQUEST_CODE) {
                String folderPath = data.getStringExtra(Constants.FILESYSTEM_FOLDER_PATH);
                WriteilySingleton.getInstance().moveSelectedNotes(filesListView, filesAdapter, folderPath);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
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

    public void listFilesInDirectory() {
        listFilesInDirectory(new File(getCurrentDir()));
    }

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

    public String getCurrentDir() {
        return (currentDir == null) ? getRootDir() : currentDir.getAbsolutePath();
    }

    public String getRootDir() {
        return rootDir.getAbsolutePath();
    }

    private class ActionModeCallback implements ListView.MultiChoiceModeListener {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.notes_context_menu, menu);
            mode.setTitle("Select files");
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.context_menu_archive:
                    WriteilySingleton.getInstance().moveSelectedNotes(filesListView, filesAdapter, Constants.ARCHIVED_FOLDER);
                    listFilesInDirectory(rootDir);
                    mode.finish();
                    return true;
                case R.id.context_menu_move:
                    promptForDirectory();
                    mode.finish();
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
                    actionMode.setSubtitle("One item selected");
                    break;
                default:
                    actionMode.setSubtitle(numSelected + " items selected");
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
