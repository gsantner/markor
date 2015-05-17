package me.writeily.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

import me.writeily.R;
import me.writeily.adapter.FileAdapter;
import me.writeily.model.Constants;
import me.writeily.model.WriteilySingleton;

/**
 * Created by jeff on 2014-04-11.
 */
public class FilesystemDialog extends DialogFragment {

    public static final String EXTERNAL_STORAGE_PATH = Environment.getExternalStorageDirectory().getPath();
    private ListView filesListView;
    private TextView emptyFolderTextView;

    private ArrayList<File> files;
    private FileAdapter filesAdapter;

    private File rootDir;
    private File currentDir;
    private Button previousDirButton;

    private boolean isMovingFile;
    private boolean isSelectingFolder;
    private boolean isSelectingForWidget;
    private String selectedPath;
    private TextView workingDirectoryText;
    private DialogInterface.OnDismissListener onDismissListener;

    public FilesystemDialog() {
        super();
    }

    public void sendBroadcast(String name) {
        Intent broadcast = new Intent();

        if (isMovingFile) {
            broadcast.setAction(Constants.FILESYSTEM_MOVE_DIALOG_TAG);
            broadcast.putExtra(Constants.FILESYSTEM_FILE_NAME, name);
        } else if (isSelectingFolder || isSelectingForWidget) {
            broadcast.setAction(Constants.FILESYSTEM_SELECT_FOLDER_TAG);
            broadcast.putExtra(Constants.FILESYSTEM_FILE_NAME, name);
        } else {
            broadcast.setAction(Constants.FILESYSTEM_IMPORT_DIALOG_TAG);
            broadcast.putExtra(Constants.FILESYSTEM_FILE_NAME, name);
        }

        getActivity().sendBroadcast(broadcast);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        String theme = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(getString(R.string.pref_theme_key), Constants.DEFAULT_WRITEILY_STORAGE_FOLDER);

        isMovingFile = getArguments().getString(Constants.FILESYSTEM_ACTIVITY_ACCESS_TYPE_KEY).equals(Constants.FILESYSTEM_FOLDER_ACCESS_TYPE);
        isSelectingFolder = getArguments().getString(Constants.FILESYSTEM_ACTIVITY_ACCESS_TYPE_KEY).equals(Constants.FILESYSTEM_SELECT_FOLDER_ACCESS_TYPE);
        isSelectingForWidget = getArguments().getString(Constants.FILESYSTEM_ACTIVITY_ACCESS_TYPE_KEY).equals(Constants.FILESYSTEM_SELECT_FOLDER_FOR_WIDGET_ACCESS_TYPE);

        AlertDialog.Builder dialogBuilder;
        View dialogView;

        if (theme.equals(getString(R.string.theme_dark))) {
            dialogBuilder = new AlertDialog.Builder(getActivity(), R.style.Base_Theme_AppCompat_Dialog);
            dialogView = inflater.inflate(R.layout.filesystem_dialog_dark, null);
        } else {
            dialogBuilder = new AlertDialog.Builder(getActivity(), R.style.Base_Theme_AppCompat_Light_Dialog);
            dialogView = inflater.inflate(R.layout.filesystem_dialog, null);
        }

        dialogBuilder.setView(dialogView);

        if (isSelectingFolder || isSelectingForWidget) {
            dialogBuilder.setTitle(getResources().getString(R.string.select_root_folder));
            dialogBuilder.setPositiveButton(getResources().getString(R.string.select_this_folder), new
                    DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            sendBroadcast(currentDir.getAbsolutePath());
                        }
                    });
        } else if (isMovingFile) {
            dialogBuilder.setTitle(getResources().getString(R.string.select_folder_move));
            dialogBuilder.setPositiveButton(getResources().getString(R.string.move_here), new
                    DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            sendBroadcast(currentDir.getAbsolutePath());
                        }
                    });
        } else {
            dialogBuilder.setTitle(getResources().getString(R.string.import_from_device));
            dialogBuilder.setPositiveButton(getResources().getString(R.string.select), new
                    DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            if (selectedPath != null) {
                                sendBroadcast(selectedPath);
                            } else {
                                Toast.makeText(getActivity(), getResources().getString(R.string.no_selected_file), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }

        dialogBuilder.setNegativeButton(getResources().getString(android.R.string.cancel), new
                DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = dialogBuilder.show();
        emptyFolderTextView = (TextView) dialog.findViewById(R.id.empty_hint);

        if (files == null) {
            files = new ArrayList<File>();
        }

        workingDirectoryText = (TextView) dialog.findViewById(R.id.working_directory);
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
        if (isMovingFile || isSelectingForWidget) {
            workingDirectoryText.setVisibility(View.VISIBLE);
            rootDir = getRootFolderFromPrefsOrDefault();
            listDirectories(rootDir);
            currentDir = rootDir;
        } else if (isSelectingFolder) {
            workingDirectoryText.setVisibility(View.VISIBLE);
            rootDir = new File(EXTERNAL_STORAGE_PATH);
            listFilesInDirectory(rootDir);
            currentDir = rootDir;
        }else {
            workingDirectoryText.setVisibility(View.GONE);
            rootDir = new File(EXTERNAL_STORAGE_PATH);
            listFilesInDirectory(rootDir);
        }

        showCurrentDirectory(rootDir.getAbsolutePath());
        super.onResume();
    }

    private File getRootFolderFromPrefsOrDefault() {
        return new File(PreferenceManager.getDefaultSharedPreferences(this.getActivity()).getString(getString(R.string.pref_root_directory),Constants.DEFAULT_WRITEILY_STORAGE_FOLDER));
    }

    private void showCurrentDirectory(String folder) {
        String currentFolder = folder.substring(folder.lastIndexOf("/") + 1);
        workingDirectoryText.setText(getResources().getString(R.string.current_folder) + " " + currentFolder);
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

        if (isMovingFile) {
            showCurrentDirectory(currentDir.getAbsolutePath());
            listDirectories(currentDir);
        } else {
            listFilesInDirectory(currentDir);
        }
    }

    private void checkDirectoryStatus() {
        WriteilySingleton writeilySingleton = WriteilySingleton.getInstance();

        if (writeilySingleton.isRootDir(currentDir, rootDir)) {
            previousDirButton.setVisibility(View.GONE);
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

    public void setOnDismissListener(DialogInterface.OnDismissListener onDismissListener) {
        this.onDismissListener = onDismissListener;
    }

    private class FilesItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            File file = filesAdapter.getItem(i);

            // Refresh list if directory, else import
            if (file.isDirectory()) {
                currentDir = file;
                selectedPath = null;
                listFilesInDirectory(file);
                showCurrentDirectory(currentDir.getAbsolutePath());
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

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (onDismissListener != null){
            onDismissListener.onDismiss(dialog);
        }
        super.onDismiss(dialog);
    }
}
