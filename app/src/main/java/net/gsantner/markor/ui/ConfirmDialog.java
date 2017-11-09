/*
 * Copyright (c) 2014 Jeff Martin
 * Copyright (c) 2015 Pedro Lafuente
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;

import net.gsantner.markor.R;
import net.gsantner.markor.util.AppSettings;

import java.io.Serializable;

public class ConfirmDialog extends DialogFragment {
    public static final String FRAGMENT_TAG = "ConfirmDialog";

    private static final String EXTRA_TITLE = "EXTRA_TITLE";
    private static final String EXTRA_MESSAGE = "EXTRA_MESSAGE";
    public static final String EXTRA_DATA = "EXTRA_DATA";

    private Serializable _data;
    private ConfirmDialogCallback[] _callbacks;
    private String _summary;

    public static ConfirmDialog newInstance(String title, String message,
                                            Serializable data, ConfirmDialogCallback... callbacks) {
        ConfirmDialog confirmDialog = new ConfirmDialog();
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
        boolean darkTheme = AppSettings.get().isDarkThemeEnabled();
        dialogBuilder = new AlertDialog.Builder(getActivity(), darkTheme ?
                R.style.Theme_AppCompat_Dialog : R.style.Theme_AppCompat_Light_Dialog);

        dialogBuilder.setTitle(title);
        if (!TextUtils.isEmpty(message)) {
            dialogBuilder.setMessage(message);
        }

        dialogBuilder.setPositiveButton(getString(android.R.string.ok), (dialog, which) -> {
            for (ConfirmDialogCallback cdc : _callbacks) {
                cdc.onConfirmDialogAnswer(true, _data);
            }
        });

        dialogBuilder.setNegativeButton(getString(android.R.string.cancel), (dialog, which) -> {
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
