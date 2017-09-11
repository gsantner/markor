package net.gsantner.markor.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import net.gsantner.markor.R;
import net.gsantner.markor.model.Constants;
import net.gsantner.markor.util.AppSettings;

import java.io.File;

public class RenameDialog extends DialogFragment {

    private EditText newNameField;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final File file = new File(getArguments().getString(Constants.SOURCE_FILE));

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        AlertDialog.Builder dialogBuilder = setUpDialog(file, inflater);
        AlertDialog dialog = dialogBuilder.show();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        newNameField = (EditText) dialog.findViewById(R.id.new_name);
        return dialog;
    }

    private AlertDialog.Builder setUpDialog(final File file, LayoutInflater inflater) {

        View dialogView;
        AlertDialog.Builder dialogBuilder;
        boolean darkTheme = AppSettings.get().isDarkThemeEnabled();
        dialogBuilder = new AlertDialog.Builder(getActivity(), darkTheme ?
                R.style.Theme_AppCompat_Dialog : R.style.Theme_AppCompat_Light_Dialog);
        dialogView = inflater.inflate(R.layout.rename_dialog, null);

        dialogBuilder.setTitle(getResources().getString(R.string.rename));
        dialogBuilder.setView(dialogView);

        ((EditText) dialogView.findViewById(R.id.new_name)).setText(file.getName());

        dialogBuilder.setPositiveButton(getString(android.R.string.ok), new
                DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        sendBroadcast(newNameField.getText().toString(), file);
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

    private void sendBroadcast(String name, File file) {
        Intent broadcast = new Intent();
        broadcast.setAction(Constants.RENAME_DIALOG_TAG);
        broadcast.putExtra(Constants.RENAME_NEW_NAME, name);
        broadcast.putExtra(Constants.SOURCE_FILE, file.getAbsolutePath());
        getActivity().sendBroadcast(broadcast);
    }
}
