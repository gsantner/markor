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

import me.writeily.writeilypro.adapter.NotesAdapter;
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
    private ListView notesListView;
    private TextView hintTextView;

    private ArrayList<File> notes;
    private NotesAdapter notesAdapter;

    public NotesFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        layoutView = inflater.inflate(R.layout.notes_fragment, container, false);
        hintTextView = (TextView) layoutView.findViewById(R.id.empty_hint);

        if (notes == null) {
            notes = new ArrayList<File>();
            hintTextView.setVisibility(View.VISIBLE);
            hintTextView.setText(getString(R.string.empty_notes_list_hint));
        }

        checkIfDataEmpty();

        context = getActivity().getApplicationContext();
        notesListView = (ListView) layoutView.findViewById(R.id.notes_listview);
        notesAdapter = new NotesAdapter(context, notes);

        notesListView.setOnItemClickListener(new NotesItemClickListener());
        notesListView.setMultiChoiceModeListener(new ActionModeCallback());
        notesListView.setAdapter(notesAdapter);

        return layoutView;
    }

    @Override
    public void onResume() {
        listNotes();
        super.onResume();
    }

    private void checkIfDataEmpty() {
        if (notes.isEmpty()) {
            hintTextView.setVisibility(View.VISIBLE);
            hintTextView.setText(getString(R.string.empty_notes_list_hint));
        } else {
            hintTextView.setVisibility(View.INVISIBLE);
        }
    }

    private void listNotes() {
        notes = new ArrayList<File>();

        try {
            // Load from SD card
            File dir = new File(Environment.getExternalStorageDirectory() + Constants.WRITEILY_FOLDER);
            notes = WriteilySingleton.getInstance().addTextFilesFromDirectory(dir, notes);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Refresh the notes adapter with the new ArrayList
        if (notesAdapter != null) {
            notesAdapter = new NotesAdapter(context, notes);
            notesListView.setAdapter(notesAdapter);
        }

        checkIfDataEmpty();
    }

    /** Search **/
    public void search(CharSequence query) {
        if (query.length() > 0) {
            notesAdapter.getFilter().filter(query);
        }
    }

    public void clearSearchFilter() {
        notesAdapter.getFilter().filter("");

        // Workaround to an (apparently) bug in Android's ArrayAdapter... not pretty
        notesAdapter = new NotesAdapter(context, notes);
        notesListView.setAdapter(notesAdapter);
        notesAdapter.notifyDataSetChanged();
    }

    public void clearItemSelection() {
        notesAdapter.notifyDataSetChanged();
    }

    private class ActionModeCallback implements ListView.MultiChoiceModeListener {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.notes_context_menu, menu);
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
                case R.id.context_menu_archive:
                    WriteilySingleton.getInstance().moveSelectedNotes(notesListView, notesAdapter, Constants.ARCHIVED_FOLDER);
                    listNotes();
                    mode.finish();
                    return true;
                case R.id.context_menu_star:
                    WriteilySingleton.getInstance().moveSelectedNotes(notesListView, notesAdapter, Constants.STARRED_FOLDER);
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
            final int numSelected = notesListView.getCheckedItemCount();

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
            File note = notesAdapter.getItem(i);

            Intent intent = new Intent(context, NoteActivity.class);
            intent.putExtra(Constants.NOTE_KEY, note);

            startActivity(intent);
            getActivity().overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_left);
        }
    }
}
