package me.writeily.writeilypro.dropbox;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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

import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFile;
import com.dropbox.sync.android.DbxFileInfo;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;

import java.io.IOException;
import java.util.ArrayList;

import me.writeily.writeilypro.R;
import me.writeily.writeilypro.adapter.DropboxNotesAdapter;
import me.writeily.writeilypro.model.Constants;

/**
 * Created by jeff on 14-12-20.
 */
public class DropboxFragment extends Fragment {

    private Context context;

    private ListView dbxFilesListView;
    private TextView hintTextView;
    private Button previousDirButton;

    private DbxFileInfo currentDir;

    private DropboxSingleton dropboxSingleton;

    private ArrayList<DbxFileInfo> dbxFiles;

    private DropboxNotesAdapter dbxFilesAdapter;
    private ActionMode actionMode;

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        View layoutView = inflater.inflate(R.layout.files_list_fragment, container, false);

        hintTextView = (TextView) layoutView.findViewById(R.id.empty_hint);
        dropboxSingleton = DropboxSingleton.getInstance();

        context = getActivity().getApplicationContext();
        dropboxSingleton.setDbxAccountManager(DbxAccountManager.getInstance(context, Constants.DBX_KEY, Constants.DBX_SECRET));

        hintTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!dropboxSingleton.getDbxAccountManager().hasLinkedAccount()) {
                    dropboxSingleton.getDbxAccountManager().startLink(getActivity(), Constants.DBX_REQUEST_LINK_CODE);
                }
            }
        });

        if (dbxFiles == null) {
            dbxFiles = new ArrayList<DbxFileInfo>();
            hintTextView.setVisibility(View.VISIBLE);
            hintTextView.setText(getString(R.string.empty_notes_list_hint));
        }

        dbxFilesListView = (ListView) layoutView.findViewById(R.id.notes_listview);
        dbxFilesAdapter = new DropboxNotesAdapter(context, dbxFiles);


        dbxFilesListView.setOnItemClickListener(new DbxNotesItemClickListener());
        dbxFilesListView.setMultiChoiceModeListener(new ActionModeCallback());
        dbxFilesListView.setAdapter(dbxFilesAdapter);

        previousDirButton = (Button) layoutView.findViewById(R.id.import_header_btn);
        previousDirButton.setOnClickListener(new PreviousDirClickListener());

        checkIfDataEmpty();

        try {
            if (dropboxSingleton.getDbxAccountManager().hasLinkedAccount()) {
                dropboxSingleton.setDbxFileSystem(DbxFileSystem.forAccount(dropboxSingleton.getDbxAccountManager().getLinkedAccount()));
                hintTextView.setText(getString(R.string.empty_directory));

                listDirectory(Constants.DBX_ROOT_DIR);
            } else {
                hintTextView.setText(getString(R.string.sync_dropbox_required));
            }
        } catch (DbxException.Unauthorized unauthorized) {
            // TODO Toast that Dropbox access is unauthorized?
        }

        return layoutView;
    }

    @Override
    public void onResume() {
        dropboxSingleton = DropboxSingleton.getInstance();
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void listDirectory(String dir) {
        dbxFiles = new ArrayList<DbxFileInfo>();
        DbxPath dbxPath = new DbxPath(dir);

        try {
            for (DbxFileInfo f : dropboxSingleton.getDbxFileSystem().listFolder(dbxPath)) {
                if (!f.toString().startsWith(".")) {
                    dbxFiles.add(f);
                }
            }
        } catch (DbxException e) {
            e.printStackTrace();
        }

        if (dbxFilesAdapter != null) {
            dbxFilesAdapter = new DropboxNotesAdapter(context, dbxFiles);
            dbxFilesListView.setAdapter(dbxFilesAdapter);
        }

        checkDirectoryStatus();
    }

    private void checkDirectoryStatus() {
        if (currentDir != null && currentDir.toString().equalsIgnoreCase(Constants.DBX_ROOT_DIR)) {
            previousDirButton.setVisibility(View.GONE);
            currentDir = null;
        } else {
            previousDirButton.setVisibility(View.VISIBLE);
        }

        // Check if dir is empty
        if (dbxFiles.isEmpty()) {
            hintTextView.setVisibility(View.VISIBLE);
            hintTextView.setText(getString(R.string.empty_directory));
        } else {
            hintTextView.setVisibility(View.INVISIBLE);
        }
    }

    private void checkIfDataEmpty() {
        if (dbxFiles.isEmpty()) {
            hintTextView.setVisibility(View.VISIBLE);
            hintTextView.setText(getString(R.string.empty_notes_list_hint));
        } else {
            hintTextView.setVisibility(View.INVISIBLE);
        }
    }

    /** Search **/
    public void search(CharSequence query) {
        if (query.length() > 0) {
            dbxFilesAdapter.getFilter().filter(query);
        }
    }

    public void clearSearchFilter() {
        dbxFilesAdapter.getFilter().filter("");

        // Workaround to an (apparently) bug in Android's ArrayAdapter... not pretty
        dbxFilesAdapter = new DropboxNotesAdapter(context, dbxFiles);
        dbxFilesListView.setAdapter(dbxFilesAdapter);
        dbxFilesAdapter.notifyDataSetChanged();
    }

    public void clearItemSelection() {
        dbxFilesAdapter.notifyDataSetChanged();
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
            actionMode = mode;
            switch (item.getItemId()) {
                case R.id.context_menu_delete:
//                    confirmDelete();
                    return true;
                case R.id.context_menu_move:
//                    promptForDirectory();
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
            final int numSelected = dbxFilesListView.getCheckedItemCount();

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

    private class DbxNotesItemClickListener implements android.widget.AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            DbxFileInfo file = dbxFilesAdapter.getItem(i);

            // Refresh list if directory, else import
            if (file.isFolder) {
                currentDir = file;
                listDirectory(Constants.DBX_ROOT_DIR);
            } else {
                DbxFileInfo dbxFileInfo = dbxFilesAdapter.getItem(i);
                DbxPath notePath = dbxFileInfo.path;
                DbxFile note = null;

                try {
                    note = dropboxSingleton.getDbxFileSystem().open(notePath);
                    String noteContent = note.readString();

                    Intent intent = new Intent(context, DropboxNoteActivity.class);
                    intent.putExtra(Constants.DBX_NOTE_PATH, notePath.toString());
                    intent.putExtra(Constants.DBX_NOTE_CONTENT, noteContent);

                    startActivity(intent);
                    getActivity().overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_left);
                } catch (DbxException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (note != null) {
                        note.close();
                    }
                }
            }
        }
    }

    private class PreviousDirClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            //goToPreviousDir();
        }
    }
}
