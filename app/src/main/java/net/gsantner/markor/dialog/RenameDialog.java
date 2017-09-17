package net.gsantner.markor.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import net.gsantner.markor.R;
import net.gsantner.markor.util.AppCast;
import net.gsantner.markor.util.AppSettings;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class RenameDialog extends DialogFragment {
    public static final String EXTRA_FILEPATH = "EXTRA_FILEPATH";
    public static final String FRAGMENT_TAG = "RenameDialog";

    public static RenameDialog newInstance(File sourceFile) {
        RenameDialog dialog = new RenameDialog();
        Bundle args = new Bundle();
        args.putSerializable(EXTRA_FILEPATH, sourceFile);
        dialog.setArguments(args);
        return dialog;
    }


    private EditText _newNameField;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final File file = (File) getArguments().getSerializable(EXTRA_FILEPATH);

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        AlertDialog.Builder dialogBuilder = setUpDialog(file, inflater);
        AlertDialog dialog = dialogBuilder.show();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        _newNameField = dialog.findViewById(R.id.new_name);
        return dialog;
    }

    private AlertDialog.Builder setUpDialog(final File file, LayoutInflater inflater) {
        View dialogView;
        AlertDialog.Builder dialogBuilder;
        boolean darkTheme = AppSettings.get().isDarkThemeEnabled();
        dialogBuilder = new AlertDialog.Builder(getActivity(), darkTheme ?
                R.style.Theme_AppCompat_Dialog : R.style.Theme_AppCompat_Light_Dialog);
        dialogView = inflater.inflate(R.layout.ui__rename__dialog, null);

        dialogBuilder.setTitle(getResources().getString(R.string.rename));
        dialogBuilder.setView(dialogView);

        ((EditText) dialogView.findViewById(R.id.new_name)).setText(file.getName());

        dialogBuilder.setPositiveButton(getString(android.R.string.ok), new
                DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (renameFileInSameFolder(file, _newNameField.getText().toString())) {
                            AppCast.VIEW_FOLDER_CHANGED.send(getContext(), file.getParent(), true);
                        }
                    }
                });

        dialogBuilder.setNegativeButton(getString(android.R.string.cancel), new
                DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        return dialogBuilder;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private boolean renameFileInSameFolder(File srcFile, String destFilename) {
        File destFile = new File(srcFile.getParent(), destFilename);
        File cacheFile = new File(getActivity().getCacheDir(), "rename.md");
        try {
            FileUtils.moveFile(srcFile, cacheFile);
            FileUtils.moveFile(cacheFile, destFile);
            return true;
        } catch (IOException ex) {
            return false;
        } finally {
            if (cacheFile.exists()) {
                cacheFile.delete();
            }
        }
    }
}
