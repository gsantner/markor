/*#######################################################
 * Copyright (c) 2014 Jeff Martin
 * Copyright (c) 2015 Pedro Lafuente
 * Copyright (c) 2017-2024 Gregor Santner
 *
 * Licensed under the MIT license.
 * You can get a copy of the license text here:
 *   https://opensource.org/licenses/MIT
###########################################################*/
package other.writeily.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import net.gsantner.markor.R;

import java.io.Serializable;

public class WrConfirmDialog extends DialogFragment {
    public static final String FRAGMENT_TAG = "WrConfirmDialog";

    private static final String EXTRA_TITLE = "EXTRA_TITLE";
    private static final String EXTRA_MESSAGE = "EXTRA_MESSAGE";
    public static final String EXTRA_DATA = "EXTRA_DATA";

    private Serializable _data;
    private ConfirmDialogCallback[] _callbacks;
    private String _summary;

    public static WrConfirmDialog newInstance(String title, String message,
                                              Serializable data, ConfirmDialogCallback... callbacks) {
        WrConfirmDialog confirmDialog = new WrConfirmDialog();
        Bundle args = new Bundle();
        args.putSerializable(EXTRA_DATA, data);
        args.putString(EXTRA_TITLE, title);
        args.putString(EXTRA_MESSAGE, message);
        confirmDialog.setArguments(args);
        confirmDialog.setCallbacks(callbacks);
        return confirmDialog;
    }

    public void setCallbacks(ConfirmDialogCallback[] callbacks) {
        _callbacks = callbacks;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String title = getArguments().getString(EXTRA_TITLE);
        String message = getArguments().getString(EXTRA_MESSAGE);
        _data = getArguments().getSerializable(EXTRA_DATA);

        AlertDialog.Builder dialogBuilder;
        dialogBuilder = new AlertDialog.Builder(getActivity(), R.style.Theme_AppCompat_DayNight_Dialog_Rounded);

        dialogBuilder.setTitle(title);
        if (!TextUtils.isEmpty(message)) {
            dialogBuilder.setMessage(message);
        }

        dialogBuilder.setPositiveButton(getString(android.R.string.ok), (dialog, which) -> {
            if (_callbacks != null) {
                for (ConfirmDialogCallback cdc : _callbacks) {
                    if (cdc != null) {
                        cdc.onConfirmDialogAnswer(true, _data);
                    }
                }
            }
        });

        dialogBuilder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
            dialog.dismiss();
            for (ConfirmDialogCallback cdc : _callbacks) {
                cdc.onConfirmDialogAnswer(false, _data);
            }
        });

        return dialogBuilder.show();
    }

    public interface ConfirmDialogCallback {
        void onConfirmDialogAnswer(boolean confirmed, Serializable data);
    }
}
