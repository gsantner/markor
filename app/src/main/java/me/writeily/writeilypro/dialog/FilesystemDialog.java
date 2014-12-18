package me.writeily.writeilypro.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

import me.writeily.writeilypro.R;
import me.writeily.writeilypro.adapter.FileAdapter;
import me.writeily.writeilypro.model.Constants;
import me.writeily.writeilypro.model.WriteilySingleton;

/**
 * Created by jeff on 2014-04-11.
 */
public class FilesystemDialog extends DialogFragment {

    private ListView filesListView;
    private TextView emptyFolderTextView;

    private ArrayList<File> files;
    private FileAdapter filesAdapter;

    private File rootDir;
    private File currentDir;
    private Button previousDirButton;

    private boolean isMovingFile;
    private String selectedPath;

    public FilesystemDialog() {
        super();
    }

    public void sendBroadcast(String name) {
        Intent broadcast = new Intent();

        if (!isMovingFile) {
            broadcast.setAction(Constants.FILESYSTEM_IMPORT_DIALOG_TAG);
            broadcast.putExtra(Constants.FILESYSTEM_FILE_NAME, name);
        } else {
            broadcast.setAction(Constants.FILESYSTEM_MOVE_DIALOG_TAG);
            broadcast.putExtra(Constants.FILESYSTEM_FILE_NAME, name);
        }

        getActivity().sendBroadcast(broadcast);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View dialogView = inflater.inflate(R.layout.filesystem_dialog, null);

        isMovingFile = getArguments().getString(Constants.FILESYSTEM_ACTIVITY_ACCESS_TYPE_KEY).equals(Constants.FILESYSTEM_FOLDER_ACCESS_TYPE);

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());

        if (isMovingFile) {
            dialogBuilder.setTitle(getResources().getString(R.string.select_folder_move));
        } else {
            dialogBuilder.setTitle(getResources().getString(R.string.import_from_device));
        }

        dialogBuilder.setView(dialogView);
        dialogBuilder.setPositiveButton("Select", new
                DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        sendBroadcast(selectedPath);
                    }
                });

        dialogBuilder.setNegativeButton("Cancel", new
                DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = dialogBuilder.show();
        emptyFolderTextView = (TextView) dialog.findViewById(R.id.empty_hint);

        if (files== null) {
            files = new ArrayList<File>();
        }

        filesListView = (ListView) dialog.findViewById(R.id.notes_listview);
        filesAdapter = new FileAdapter(getActivity().getApplicationContext(), files);

        filesListView.setOnItemClickListener(new FilesItemClickListener());
        filesListView.setAdapter(filesAdapter);

        // Previous dir button to help navigate the directories
        previousDirButton = (Button) dialog.findViewById(R.id.import_header_btn);
        previousDirButton.setOnClickListener(new PreviousDirClickListener());

        return dialog;
    }

    @Override
    public void onResume() {
        rootDir = new File(Environment.getExternalStorageDirectory().getPath());
        if (isMovingFile) {
            listDirectories(rootDir);
        } else {
            listFilesInDirectory(rootDir);
        }
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
            filesAdapter = new FileAdapter(getActivity().getApplicationContext(), files);
            filesListView.setAdapter(filesAdapter);
        }

        checkDirectoryStatus();
    }

    private void listDirectories(File directory) {
        files = new ArrayList<File>();

        try {
            // Load from SD card
            files = WriteilySingleton.getInstance().addDirectories(directory, files);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Refresh the files adapter with the new ArrayList
        if (filesAdapter != null) {
            filesAdapter = new FileAdapter(getActivity().getApplicationContext(), files);
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
        WriteilySingleton writeilySingleton = WriteilySingleton.getInstance();

        if (writeilySingleton.isRootDir(currentDir, rootDir)) {
            previousDirButton.setVisibility(View.GONE);
            currentDir = null;
        } else {
            previousDirButton.setVisibility(View.VISIBLE);
        }

        // Check if dir is empty
        if (writeilySingleton.isDirectoryEmpty(files)) {
            emptyFolderTextView.setVisibility(View.VISIBLE);
            emptyFolderTextView.setText(getString(R.string.empty_directory));
        } else {
            emptyFolderTextView.setVisibility(View.INVISIBLE);
        }
    }

    private class FilesItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            File file = filesAdapter.getItem(i);

            // Refresh list if directory, else import
            if (file.isDirectory()) {
                if (!isMovingFile || (selectedPath != null && selectedPath.equalsIgnoreCase(file.getAbsolutePath()))) {
                    currentDir = file;
                    selectedPath = null;
                    listFilesInDirectory(file);
                } else {
                    selectedPath = file.getAbsolutePath();
                }
            } else {
                selectedPath = file.getAbsolutePath();
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
