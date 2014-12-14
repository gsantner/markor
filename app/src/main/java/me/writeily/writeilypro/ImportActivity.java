package me.writeily.writeilypro;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

import me.writeily.writeilypro.adapter.FileAdapter;
import me.writeily.writeilypro.model.WriteilySingleton;

/**
 * Created by jeff on 2014-04-11.
 */
public class ImportActivity extends ActionBarActivity {

    private ListView filesListView;
    private TextView emptyFolderTextView;

    private ArrayList<File> files;
    private FileAdapter filesAdapter;

    private File previousDir;
    private Button previousDirButton;
    private Toolbar toolbar;

    public ImportActivity() {
        super();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_import);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        emptyFolderTextView = (TextView) findViewById(R.id.empty_hint);
        emptyFolderTextView.setText(getString(R.string.empty_directory));

        if (files== null) {
            files = new ArrayList<File>();
        }

        filesListView = (ListView) findViewById(R.id.notes_listview);
        filesAdapter = new FileAdapter(this, files);

        filesListView.setOnItemClickListener(new FilesItemClickListener());
        filesListView.setMultiChoiceModeListener(new ActionModeCallback());
        filesListView.setAdapter(filesAdapter);

        // Previous dir button to help navigate the directories
        previousDirButton = (Button) findViewById(R.id.import_header_btn);
        previousDirButton.setOnClickListener(new PreviousDirClickListener());

        File dir = new File(Environment.getExternalStorageDirectory().getPath());
        listFilesInDirectory(dir);
        checkIfDirectoryEmpty();

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        File dir = new File(Environment.getExternalStorageDirectory().getPath());
        listFilesInDirectory(dir);
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        if (previousDir != null) {
            goToPreviousDir();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                overridePendingTransition(R.anim.anim_slide_out_right, R.anim.anim_slide_in_right);
                return true;
            default:
                return super.onOptionsItemSelected(item);
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
            filesAdapter = new FileAdapter(this, files);
            filesListView.setAdapter(filesAdapter);
        }

        checkHidePreviousDirButton();
    }

    private void checkIfDirectoryEmpty() {
        if (files.isEmpty()) {
            emptyFolderTextView.setVisibility(View.VISIBLE);
            emptyFolderTextView.setText(getString(R.string.empty_notes_list_hint));
        } else {
            emptyFolderTextView.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Hide the header when getting to the external dir so the app doesn't show too much.
     */
    private void checkHidePreviousDirButton() {
        File compareRootDir = new File(Environment.getExternalStorageDirectory().getPath());
        if (previousDir == null || previousDir.getPath().equalsIgnoreCase(compareRootDir.getAbsolutePath())) {
            previousDirButton.setVisibility(View.GONE);
            previousDir = null;
        } else {
            previousDirButton.setVisibility(View.VISIBLE);
        }
    }

    private void goToPreviousDir() {
        if (previousDir != null) {
            previousDir = previousDir.getParentFile();
        }

        listFilesInDirectory(previousDir);
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

    private class PreviousDirClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            goToPreviousDir();
        }
    }
}
