package me.writeily.pro.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import me.writeily.pro.R;
import me.writeily.pro.model.Constants;

/**
 * Created by jeff on 2014-04-11.
 */
public class ConfirmDialog extends DialogFragment {

    public ConfirmDialog() {
        super();
    }

    public void sendBroadcast() {
        Intent broadcast = new Intent();
        broadcast.setAction(Constants.CONFIRM_DIALOG_TAG);
        getActivity().sendBroadcast(broadcast);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setTitle(getResources().getString(R.string.confirm_delete));

        dialogBuilder.setPositiveButton("Confirm", new
                DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        sendBroadcast();
                    }
                });

        dialogBuilder.setNegativeButton("Cancel", new
                DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        return dialogBuilder.show();
    }
}
