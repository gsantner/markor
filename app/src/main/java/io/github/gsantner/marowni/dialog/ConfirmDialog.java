package io.github.gsantner.marowni.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;

import io.github.gsantner.marowni.R;
import io.github.gsantner.marowni.model.Constants;
import io.github.gsantner.marowni.util.AppSettings;

public class ConfirmDialog extends DialogFragment {

    public ConfirmDialog() {
        super();
    }

    public void sendBroadcast() {
        Intent broadcast = new Intent();
        broadcast.setAction(getTag());
        if (getArguments() != null) {
            broadcast.putExtras(getArguments());
        }
        getActivity().sendBroadcast(broadcast);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder dialogBuilder;
        int title = getTitleForTag(getTag());

        if (AppSettings.get().isDarkThemeEnabled()) {
            dialogBuilder = new AlertDialog.Builder(getActivity(), R.style.Base_Theme_AppCompat_Dialog);
        } else {
            dialogBuilder = new AlertDialog.Builder(getActivity(), R.style.Base_Theme_AppCompat_Light_Dialog);
        }


        dialogBuilder.setTitle(getResources().getString(title));

        dialogBuilder.setPositiveButton(getString(android.R.string.ok), new
                DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        sendBroadcast();
                    }
                });

        dialogBuilder.setNegativeButton(getString(android.R.string.cancel), new
                DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        return dialogBuilder.show();
    }

    private int getTitleForTag(String tag) {

        switch (tag) {
            case Constants.CONFIRM_OVERWRITE_DIALOG_TAG:
                return R.string.confirm_overwrite;

            case Constants.CONFIRM_DELETE_DIALOG_TAG:
            default:
                return R.string.confirm_delete;
        }
    }
}
