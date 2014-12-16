package me.writeily.writeilypro.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import me.writeily.writeilypro.R;
import me.writeily.writeilypro.model.Constants;

/**
 * Created by jeff on 2014-04-11.
 */
public class FolderDialog extends DialogFragment {

    private EditText folderNameEditText;
    private String currentDir;

    public FolderDialog() {
    }

    public void sendBroadcast(String name) {
        Intent broadcast = new Intent();
        broadcast.setAction(Constants.FOLDER_DIALOG_TAG);
        broadcast.putExtra(Constants.FOLDER_NAME, currentDir + "/" + name);
        getActivity().sendBroadcast(broadcast);
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View dialogView = inflater.inflate(R.layout.folder_dialog, null);
        currentDir = getArguments().getString(Constants.CURRENT_DIRECTORY_DIALOG_KEY, "");

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setTitle(getResources().getString(R.string.create_folder));
        dialogBuilder.setView(dialogView);

        dialogBuilder.setPositiveButton("Create", new
                DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Broadcast result to MainActivity
                        sendBroadcast(folderNameEditText.getText().toString());
                    }
                });

        dialogBuilder.setNegativeButton("Cancel", new
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
