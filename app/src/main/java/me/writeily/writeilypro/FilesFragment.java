package me.writeily.writeilypro;

import android.app.Fragment;
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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import me.writeily.writeilypro.adapter.FilesAdapter;
import me.writeily.writeilypro.model.Constants;
import me.writeily.writeilypro.model.WriteilySingleton;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by jeff on 2014-04-11.
 */
public class FilesFragment extends Fragment {

    private Context context;

    private View layoutView;
    private ListView filesListView;
    private TextView hintTextView;
    private TextView previousDirTextView;

    private File previousDir;
    private ArrayList<File> files;
    private FilesAdapter filesAdapter;
    private File rootDir;

    public FilesFragment() {
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

        rootDir = new File(Environment.getExternalStorageDirectory() + Constants.WRITEILY_FOLDER);
        context = getActivity().getApplicationContext();

        // Previous dir button to help navigate the directories
        previousDirTextView = (TextView) inflater.inflate(R.layout.previous_directory_layout, null);

        filesListView = (ListView) layoutView.findViewById(R.id.notes_listview);
        filesAdapter = new FilesAdapter(context, files);

        filesListView.setOnItemClickListener(new NotesItemClickListener());
        filesListView.setMultiChoiceModeListener(new ActionModeCallback());
        filesListView.setAdapter(filesAdapter);

        return layoutView;
    }

    @Override
    public void onResume() {
        listFilesInDirectory(rootDir);
        super.onResume();
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
            filesAdapter = new FilesAdapter(context, files);
            filesListView.setAdapter(filesAdapter);
        }

        checkHidePreviousDirButton();
        checkIfDirectoryEmpty();
    }

    /**
     * Hide the header when getting to the external dir so the app doesn't show too much.
     */
    public File checkHidePreviousDirButton() {
        File compareDir = new File(Environment.getExternalStorageDirectory().getPath());

        if (previousDir == null || previousDir.getPath().equalsIgnoreCase(compareDir.getAbsolutePath())) {
            filesListView.removeHeaderView(previousDirTextView);
            previousDir = null;
        } else {
            if (filesListView.getHeaderViewsCount() == 0) {
                filesListView.addHeaderView(previousDirTextView);
            }
        }

        return previousDir;
    }

    public void checkIfDirectoryEmpty() {
        if (files == null || files.isEmpty()) {
            hintTextView.setVisibility(View.VISIBLE);
            hintTextView.setText(context.getString(R.string.empty_directory));
        } else {
            hintTextView.setVisibility(View.INVISIBLE);
        }
    }

    private void goToPreviousDir() {
        if (previousDir != null) {
            previousDir = previousDir.getParentFile();
        }

        listFilesInDirectory(previousDir);
    }

    private void openFile(int i) {
        File file = filesAdapter.getItem(i);

        // Refresh list if directory, else import
        if (file.isDirectory()) {
            previousDir = file;
            listFilesInDirectory(file);
        } else {
            // Only open text files
            if (!file.getName().endsWith(Constants.TXT_EXT)) {
                Toast.makeText(context, "Writeily can only edit text files", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(context, NoteActivity.class);
            intent.putExtra(Constants.NOTE_KEY, file);

            startActivity(intent);
            getActivity().overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_left);
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
        filesAdapter = new FilesAdapter(context, files);
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
                case R.id.context_menu_star:
                    WriteilySingleton.getInstance().moveSelectedNotes(filesListView, filesAdapter, Constants.STARRED_FOLDER);
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
            // Note: need to decrement i by 1 because of the header view
            i = filesListView.getHeaderViewsCount() > 0 ? i - 1 : i;

            if (i < 0) {
                goToPreviousDir();
            } else {
                openFile(i);
            }
        }
    }
}
