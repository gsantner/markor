package me.writeily.writeilypro;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.SparseBooleanArray;
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

import me.writeily.writeilypro.adapter.NotesAdapter;
import me.writeily.writeilypro.model.Constants;
import me.writeily.writeilypro.model.WriteilySingleton;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by jeff on 2014-04-11.
 */
public class ArchivesFragment extends Fragment {

    private Context context;

    private View layoutView;
    private ListView archivesListView;
    private TextView hintTextView;

    private NotesAdapter archivesAdapter;
    private ArrayList<File> archives;

    public ArchivesFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        layoutView = inflater.inflate(R.layout.notes_fragment, container, false);
        hintTextView = (TextView) layoutView.findViewById(R.id.empty_hint);

        if (archives == null) {
            archives = new ArrayList<File>();
            hintTextView.setVisibility(View.VISIBLE);
            hintTextView.setText(getString(R.string.empty_archives_hint));
        }

        checkIfDataEmpty();

        context = getActivity().getApplicationContext();
        archivesListView = (ListView) layoutView.findViewById(R.id.notes_listview);
        archivesAdapter = new NotesAdapter(context, archives);

        archivesListView.setOnItemClickListener(new NotesItemClickListener());
        archivesListView.setMultiChoiceModeListener(new ActionModeCallback());
        archivesListView.setAdapter(archivesAdapter);

        return layoutView;
    }

    @Override
    public void onResume() {
        listNotes();
        super.onResume();
    }

    private void checkIfDataEmpty() {
        if (archives.isEmpty()) {
            hintTextView.setVisibility(View.VISIBLE);
            hintTextView.setText(getString(R.string.empty_archives_hint));
        } else {
            hintTextView.setVisibility(View.INVISIBLE);
        }
    }

    private void listNotes() {
        archives = new ArrayList<File>();

        try {
            // Load from SD card
            File dir = new File(Environment.getExternalStorageDirectory() + Constants.ARCHIVED_FOLDER);
            archives = WriteilySingleton.getInstance().addTextFilesFromDirectory(dir, archives);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Refresh the notes adapter with the new ArrayList
        if (archivesAdapter != null) {
            archivesAdapter = new NotesAdapter(context, archives);
            archivesListView.setAdapter(archivesAdapter);
        }

        checkIfDataEmpty();
    }

    private void deleteSelectedNotes() {
        SparseBooleanArray checkedIndices = archivesListView.getCheckedItemPositions();
        for (int i = 0; i < checkedIndices.size(); i++) {
            // Delete the file from internal storage
            if (checkedIndices.valueAt(i)) {
                File note = archivesAdapter.getItem(checkedIndices.keyAt(i));
                note.delete();
            }
        }

        listNotes();
    }

    /** Search **/
    public void search(CharSequence query) {
        if (query.length() > 0) {
            archivesAdapter.getFilter().filter(query);
        }
    }

    public void clearSearchFilter() {
        archivesAdapter.getFilter().filter("");

        // Workaround to an (apparently) bug in Android's ArrayAdapter... not pretty, I know
        archivesAdapter = new NotesAdapter(context, archives);
        archivesListView.setAdapter(archivesAdapter);
        archivesAdapter.notifyDataSetChanged();
    }

    private class ActionModeCallback implements ListView.MultiChoiceModeListener {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.archives_context_menu, menu);
            mode.setTitle("Select notes");
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.context_menu_delete:
                    deleteSelectedNotes();
                    listNotes();
                    mode.finish();
                    return true;
                case R.id.context_menu_restore:
                    WriteilySingleton.getInstance().moveSelectedNotes(archivesListView, archivesAdapter, Constants.ARCHIVED_FOLDER, Constants.NOTES_FOLDER);
                    listNotes();
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
            final int numSelected = archivesListView.getCheckedItemCount();

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
            File note = archivesAdapter.getItem(i);

            Intent intent = new Intent(context, NoteActivity.class);
            intent.putExtra(Constants.NOTE_KEY, note);

            startActivity(intent);
            getActivity().overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_left);
        }
    }
}
