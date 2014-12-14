package me.writeily.writeilypro;

import android.app.Fragment;
import android.content.Context;
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

import java.io.File;
import java.util.ArrayList;

import me.writeily.writeilypro.adapter.FileAdapter;
import me.writeily.writeilypro.model.WriteilySingleton;

/**
 * Created by jeff on 2014-04-11.
 */
public class ImportFragment extends Fragment {

    private Context context;

    private View layoutView;
    private ListView filesListView;
    private TextView hintTextView;

    private ArrayList<File> files;
    private FileAdapter filesAdapter;

    private File previousDir;
    private TextView importHeader;

    public ImportFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        layoutView = inflater.inflate(R.layout.notes_fragment, container, false);
        hintTextView = (TextView) layoutView.findViewById(R.id.empty_hint);

        if (files == null) {
            files = new ArrayList<File>();
            hintTextView.setVisibility(View.VISIBLE);
            hintTextView.setText(getString(R.string.empty_directory));
        }

        context = getActivity().getApplicationContext();
        filesListView = (ListView) layoutView.findViewById(R.id.notes_listview);
        filesAdapter = new FileAdapter(context, files);

        filesListView.setOnItemClickListener(new FilesItemClickListener());
        filesListView.setMultiChoiceModeListener(new ActionModeCallback());
        filesListView.setAdapter(filesAdapter);

        // ListView header to help navigate the directories
        importHeader = (TextView) inflater.inflate(R.layout.import_header_view, null);

        File dir = new File(Environment.getExternalStorageDirectory().getPath());
        listFilesInDirectory(dir);

        return layoutView;
    }

    @Override
    public void onResume() {
        File dir = new File(Environment.getExternalStorageDirectory().getPath());
        listFilesInDirectory(dir);
        super.onResume();
    }

    private void checkIfDataEmpty() {
        if (files.isEmpty()) {
            hintTextView.setVisibility(View.VISIBLE);
            hintTextView.setText(getString(R.string.empty_directory));
        } else {
            hintTextView.setVisibility(View.INVISIBLE);
        }
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
            filesAdapter = new FileAdapter(context, files);
            filesListView.setAdapter(filesAdapter);
        }

        checkHideHeader();
        checkIfDataEmpty();
    }

    /**
     * Hide the header when getting to the external dir so the app doesn't show too much.
     */
    private void checkHideHeader() {
        File dir = new File(Environment.getExternalStorageDirectory().getPath());
        if (previousDir == null || previousDir.getPath().equalsIgnoreCase(dir.getAbsolutePath())) {
            filesListView.removeHeaderView(importHeader);
            previousDir = null;
        } else {
            if (filesListView.getHeaderViewsCount() <= 0) {
                filesListView.addHeaderView(importHeader);
            }
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
        filesAdapter = new FileAdapter(context, files);
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
            inflater.inflate(R.menu.import_context_menu, menu);
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
                // TODO multiple import files
                case R.id.context_menu_import:
//                    WriteilySingleton.getInstance().moveSelectedNotes(filesListView, filesAdapter, Constants.NOTES_FOLDER, Constants.ARCHIVED_FOLDER);
//                    mode.finish();
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
                    actionMode.setSubtitle("One file selected");
                    break;
                default:
                    actionMode.setSubtitle(numSelected + " files selected");
                    break;
            }
        }
    };

    private class FilesItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            // Note: need to decrement i by 1 because of the header view
            i = filesListView.getHeaderViewsCount() > 0 ? i - 1 : i;

            if (i < 0) {
                if (previousDir != null) {
                    previousDir = previousDir.getParentFile();
                }

                listFilesInDirectory(previousDir);
            } else {
                File file = filesAdapter.getItem(i);

                // Refresh list if directory, else import
                if (file.isDirectory()) {
                    previousDir = file;
                    listFilesInDirectory(file);
                } else {
                    // TODO save file in writeily folder?
                }
            }
        }
    }
}
