package me.writeily.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;

import me.writeily.R;
import me.writeily.model.Constants;

/**
 * Created by jeff on 2014-04-11.
 */
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
        String theme = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(getString(R.string.pref_theme_key), "");

        int title = getTitleForTag(getTag());

        if (theme.equals(getString(R.string.theme_dark))) {
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

        switch(tag){
            case Constants.CONFIRM_OVERWRITE_DIALOG_TAG:
                return R.string.confirm_overwrite;

            case Constants.CONFIRM_DELETE_DIALOG_TAG:
            default:
                return R.string.confirm_delete;
        }
    }
}
