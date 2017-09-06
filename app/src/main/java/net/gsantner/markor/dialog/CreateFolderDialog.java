package net.gsantner.markor.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import net.gsantner.markor.R;
import net.gsantner.markor.model.Constants;
import net.gsantner.markor.util.AppSettings;

public class CreateFolderDialog extends DialogFragment {

    private EditText folderNameEditText;
    private String currentDir;

    public CreateFolderDialog() {
    }

    public void sendBroadcast(String name) {
        Intent broadcast = new Intent();
        broadcast.setAction(Constants.CREATE_FOLDER_DIALOG_TAG);
        broadcast.putExtra(Constants.FOLDER_NAME, currentDir + "/" + name);
        getActivity().sendBroadcast(broadcast);
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        currentDir = getArguments().getString(Constants.CURRENT_DIRECTORY_DIALOG_KEY, "");

        View dialogView;
        AlertDialog.Builder dialogBuilder;

        if (AppSettings.get().isDarkThemeEnabled()) {
            dialogView = inflater.inflate(R.layout.folder_dialog_dark, null);
            dialogBuilder = new AlertDialog.Builder(getActivity(), R.style.Base_Theme_AppCompat_Dialog);
        } else {
            dialogView = inflater.inflate(R.layout.folder_dialog, null);
            dialogBuilder = new AlertDialog.Builder(getActivity(), R.style.Base_Theme_AppCompat_Light_Dialog);
        }

        dialogBuilder.setTitle(getResources().getString(R.string.create_folder));
        dialogBuilder.setView(dialogView);

        dialogBuilder.setPositiveButton(getResources().getString(R.string.create), new
                DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Broadcast result to MainActivity
                        sendBroadcast(folderNameEditText.getText().toString());
                    }
                });

        dialogBuilder.setNegativeButton(getResources().getString(android.R.string.cancel), new
                DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = dialogBuilder.show();
        folderNameEditText = (EditText) dialog.findViewById(R.id.folder_name);

        return dialog;
    }

}
