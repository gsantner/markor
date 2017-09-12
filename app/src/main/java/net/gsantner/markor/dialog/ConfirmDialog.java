package net.gsantner.markor.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import net.gsantner.markor.R;
import net.gsantner.markor.util.AppCast;
import net.gsantner.markor.util.AppSettings;

import java.io.Serializable;

public class ConfirmDialog extends DialogFragment {
    public static final String FRAGMENT_TAG = "ConfirmDialog";
    public static final String EXTRA_WHAT = "EXTRA_WHAT";
    public static final String WHAT_DELETE = "WHAT_DELETE";
    public static final String WHAT_OVERWRITE = "WHAT_OVERWRITE";
    public static final String EXTRA_DATA = "EXTRA_DATA";

    private String _what;
    private Serializable _data;

    public static ConfirmDialog newInstance(String action, Serializable data) {
        ConfirmDialog confirmDialog = new ConfirmDialog();
        Bundle args = new Bundle();
        args.putString(EXTRA_WHAT, action);
        args.putSerializable(EXTRA_DATA, data);
        confirmDialog.setArguments(args);
        return confirmDialog;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        _what = getArguments().getString(EXTRA_WHAT);
        _data = getArguments().getSerializable(EXTRA_DATA);

        AlertDialog.Builder dialogBuilder;
        int title = getTitleResIdForWhat(_what);
        boolean darkTheme = AppSettings.get().isDarkThemeEnabled();
        dialogBuilder = new AlertDialog.Builder(getActivity(), darkTheme ?
                R.style.Theme_AppCompat_Dialog : R.style.Theme_AppCompat_Light_Dialog);


        dialogBuilder.setTitle(getResources().getString(title));

        dialogBuilder.setPositiveButton(getString(android.R.string.ok), new
                DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        AppCast.CONFIRM.send(getActivity(), _what, _data);
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

    private int getTitleResIdForWhat(String tag) {
        switch (tag) {
            case WHAT_OVERWRITE: {
                return R.string.confirm_overwrite;
            }
            case WHAT_DELETE:
            default: {
                return R.string.confirm_delete;
            }
        }
    }
}
