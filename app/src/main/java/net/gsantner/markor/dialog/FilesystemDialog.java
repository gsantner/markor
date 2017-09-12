package net.gsantner.markor.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import net.gsantner.markor.R;
import net.gsantner.markor.adapter.FileAdapter;
import net.gsantner.markor.model.Constants;
import net.gsantner.markor.model.MarkorSingleton;
import net.gsantner.markor.util.AppSettings;

import java.io.File;
import java.util.ArrayList;

public class FilesystemDialog extends DialogFragment {
    public static final String EXTRA_ACCESS_TYPE = "EXTRA_ACCESS_TYPE";

    public static final String AT_FOLDER_SELECT_WIDGET = "FILESYSTEM_SELECT_FOLDER_FOR_WIDGET_ACCESS_TYPE";
    public static final String AT_FOLDER_SELECT = "FILESYSTEM_SELECT_FOLDER_ACCESS_TYPE";
    public static final String AT_FOLDER = "FILESYSTEM_FOLDER_ACCESS_TYPE";
    public static final String AT_FILE = "FILESYSTEM_FILE_ACCESS_TYPE";

    private ListView _filesListView;
    private TextView _emptyFolderTextView;

    private ArrayList<File> _files;
    private FileAdapter _filesAdapter;

    private File _rootDir;
    private File _currentDir;
    private Button _previousDirButton;

    private boolean _isMovingFile;
    private boolean _isSelectingFolder;
    private boolean _isSelectingForWidget;
    private boolean _isImportingFile;

    private String _selectedPath;
    private TextView _workingDirectoryText;
    private DialogInterface.OnDismissListener _onDismissListener;


    public void sendBroadcast(String name) {
        Intent broadcast = new Intent();

        if (_isMovingFile) {
            broadcast.setAction(Constants.FILESYSTEM_MOVE_DIALOG_TAG);
            broadcast.putExtra(Constants.EXTRA_FILEPATH, name);
        } else if (_isSelectingFolder || _isSelectingForWidget) {
            broadcast.setAction(Constants.FILESYSTEM_SELECT_FOLDER_TAG);
            broadcast.putExtra(Constants.EXTRA_FILEPATH, name);
        } else {
            broadcast.setAction(Constants.FILESYSTEM_IMPORT_DIALOG_TAG);
            broadcast.putExtra(Constants.EXTRA_FILEPATH, name);
        }

        getActivity().sendBroadcast(broadcast);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());

        _isMovingFile = getArguments().getString(EXTRA_ACCESS_TYPE).equals(AT_FOLDER);
        _isSelectingFolder = getArguments().getString(EXTRA_ACCESS_TYPE).equals(AT_FOLDER_SELECT);
        _isSelectingForWidget = getArguments().getString(EXTRA_ACCESS_TYPE).equals(AT_FOLDER_SELECT_WIDGET);
        _isImportingFile = getArguments().getString(EXTRA_ACCESS_TYPE).equals(AT_FILE);

        AlertDialog.Builder dialogBuilder;
        View dialogView;

        boolean darkTheme = AppSettings.get().isDarkThemeEnabled();
        dialogBuilder = new AlertDialog.Builder(getActivity(), darkTheme ?
                R.style.Theme_AppCompat_Dialog : R.style.Theme_AppCompat_Light_Dialog);
        dialogView = inflater.inflate(R.layout.ui__filesystem__dialog, null);
        dialogBuilder.setView(dialogView);

        if (_isSelectingFolder || _isSelectingForWidget) {
            dialogBuilder.setTitle(getResources().getString(R.string.select_root_folder));
            dialogBuilder.setPositiveButton(getResources().getString(R.string.select_this_folder), new
                    DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            sendBroadcast(_currentDir.getAbsolutePath());
                        }
                    });
        } else if (_isMovingFile) {
            dialogBuilder.setTitle(getResources().getString(R.string.select_folder_move));
            dialogBuilder.setPositiveButton(getResources().getString(R.string.move_here), new
                    DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            sendBroadcast(_currentDir.getAbsolutePath());
                        }
                    });
        } else {
            dialogBuilder.setTitle(getResources().getString(R.string.import_from_device));
            dialogBuilder.setPositiveButton(getResources().getString(R.string.select), new
                    DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            if (_selectedPath != null) {
                                sendBroadcast(_selectedPath);
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
        _emptyFolderTextView = dialog.findViewById(R.id.background_hint_text);

        if (_files == null) {
            _files = new ArrayList<>();
        }

        _workingDirectoryText = dialog.findViewById(R.id.working_directory);
        _filesListView = dialog.findViewById(R.id.notes_listview);
        _filesAdapter = new FileAdapter(getActivity().getApplicationContext(), _files);

        _filesListView.setOnItemClickListener(new FilesItemClickListener());
        _filesListView.setAdapter(_filesAdapter);

        // Previous dir button to help navigate the directories
        _previousDirButton = dialog.findViewById(R.id.import_header_btn);
        _previousDirButton.setOnClickListener(new PreviousDirClickListener());

        _workingDirectoryText.setTextColor(ContextCompat.getColor(_workingDirectoryText.getContext(),
                darkTheme ? R.color.dark__primary_text : R.color.light__primary_text));

        return dialog;
    }

    @Override
    public void onResume() {
        if (_isMovingFile || _isSelectingForWidget) {
            _workingDirectoryText.setVisibility(View.VISIBLE);
            _rootDir = getRootFolderFromPrefsOrDefault();
            listDirectories(_rootDir);
            _currentDir = _rootDir;
        } else if (_isSelectingFolder) {
            _workingDirectoryText.setVisibility(View.VISIBLE);
            _rootDir = new File(Environment.getExternalStorageDirectory().getPath());
            listFilesInDirectory(_rootDir);
            _currentDir = _rootDir;
        } else {
            _workingDirectoryText.setVisibility(View.GONE);
            _rootDir = new File(Environment.getExternalStorageDirectory().getPath());
            listFilesInDirectory(_rootDir);
        }

        showCurrentDirectory(_rootDir.getAbsolutePath());
        super.onResume();
    }

    private File getRootFolderFromPrefsOrDefault() {
        return new File(AppSettings.get().getSaveDirectory());
    }

    private void showCurrentDirectory(String folder) {
        String currentFolder = folder.substring(folder.lastIndexOf("/") + 1);
        _workingDirectoryText.setText(getResources().getString(R.string.current_folder) + " " + currentFolder);
    }

    private void listFilesInDirectory(File directory) {
        _files = new ArrayList<>();

        try {
            // Load from SD card
            _files = MarkorSingleton.getInstance().addFilesFromDirectory(directory, _files);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Refresh the _files adapter with the new ArrayList
        if (_filesAdapter != null) {
            _filesAdapter = new FileAdapter(getActivity().getApplicationContext(), _files);
            _filesListView.setAdapter(_filesAdapter);
        }

        checkDirectoryStatus();
    }

    private void listDirectories(File directory) {
        _files = new ArrayList<>();

        try {
            // Load from SD card
            _files = MarkorSingleton.getInstance().addDirectories(directory, _files);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Refresh the _files adapter with the new ArrayList
        if (_filesAdapter != null) {
            _filesAdapter = new FileAdapter(getActivity().getApplicationContext(), _files);
            _filesListView.setAdapter(_filesAdapter);
        }

        checkDirectoryStatus();
    }

    private void goToPreviousDir() {
        if (_currentDir != null) {
            _currentDir = _currentDir.getParentFile();
        }

        if (_isMovingFile) {
            showCurrentDirectory(_currentDir.getAbsolutePath());
            listDirectories(_currentDir);
        } else {
            listFilesInDirectory(_currentDir);
        }
    }

    private void checkDirectoryStatus() {
        MarkorSingleton markorSingleton = MarkorSingleton.getInstance();

        if (markorSingleton.isRootDir(_currentDir, _rootDir)) {
            _previousDirButton.setVisibility(View.GONE);
        } else {
            _previousDirButton.setVisibility(View.VISIBLE);
        }

        // Check if dir is empty
        if (markorSingleton.isDirectoryEmpty(_files)) {
            _emptyFolderTextView.setVisibility(View.VISIBLE);
            _emptyFolderTextView.setText(getString(R.string.empty_directory));
        } else {
            _emptyFolderTextView.setVisibility(View.INVISIBLE);
        }
    }

    public void setOnDismissListener(DialogInterface.OnDismissListener onDismissListener) {
        this._onDismissListener = onDismissListener;
    }

    private class FilesItemClickListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            File file = _filesAdapter.getItem(i);

            // Refresh list if directory, else import
            if (file.isDirectory()) {
                _currentDir = file;
                _selectedPath = null;
                listFilesInDirectory(file);
                showCurrentDirectory(_currentDir.getAbsolutePath());
            } else {
                _selectedPath = file.getAbsolutePath();
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
        if (_onDismissListener != null) {
            _onDismissListener.onDismiss(dialog);
        }
        super.onDismiss(dialog);
    }
}
