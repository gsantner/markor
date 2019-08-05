/*#######################################################
 * Copyright (c) 2014 Jeff Martin
 * Copyright (c) 2015 Pedro Lafuente
 * Copyright (c) 2017-2019 Gregor Santner
 *
 * Licensed under the MIT license.
 * You can get a copy of the license text here:
 *   https://opensource.org/licenses/MIT
###########################################################*/
package other.writeily.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.text.TextWatcher;
import android.text.Editable;

import net.gsantner.markor.R;
import net.gsantner.markor.util.AppCast;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ShareUtil;
import net.gsantner.opoc.util.Callback;
import net.gsantner.opoc.util.FileUtils;

import java.io.File;

public class WrRenameDialog extends DialogFragment {
    public static final String EXTRA_FILEPATH = "EXTRA_FILEPATH";
    public static final String FRAGMENT_TAG = "WrRenameDialog";

    public static WrRenameDialog newInstance(File sourceFile) {
        return newInstance(sourceFile, null);
    }


    public static WrRenameDialog newInstance(File sourceFile, Callback.a1<File> callback) {
        WrRenameDialog dialog = new WrRenameDialog();
        Bundle args = new Bundle();
        args.putSerializable(EXTRA_FILEPATH, sourceFile);
        dialog.setArguments(args);
        dialog._callback = callback;
        return dialog;
    }


    private EditText _newNameField;
    private Callback.a1<File> _callback;
    private boolean _filenameClash;
    private boolean _filenameChanged;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final File file = (File) getArguments().getSerializable(EXTRA_FILEPATH);

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        AlertDialog.Builder dialogBuilder = setUpDialog(file, inflater);
        AlertDialog dialog = dialogBuilder.show();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        _newNameField = dialog.findViewById(R.id.new_name);
        _newNameField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                _filenameChanged = true;
                if (_filenameClash) {
                    ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                    ((TextView) dialog.findViewById(R.id.dialog_message)).setText("");
                    _filenameClash = false;
                }
            }
        });

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            View root = inflater.inflate(R.layout.rename__dialog, null);
            String newFileName = _newNameField.getText().toString();
            ShareUtil shareUtil = new ShareUtil(root.getContext());
            boolean renamed = false;
            if (shareUtil.isUnderStorageAccessFolder(file)) {
                DocumentFile dof = shareUtil.getDocumentFile(file, file.isDirectory());
                if (dof != null) {
                    renamed = dof.renameTo(newFileName);
                    renamed = renamed || (file.getParentFile() != null && new File(file.getParentFile(), newFileName).exists());
                }
            } else {
                File newFile = new File(file.getParent(), newFileName);
                if (_filenameChanged && newFile.exists()) {
                    //dialog.setMessage("File already exists! Please enter a different filename");
                    ((TextView) dialog.findViewById(R.id.dialog_message)).setText("File already exists! Please enter a different filename");
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    _filenameClash = true;
                } else {
                    renamed = FileUtils.renameFile(file, newFile);
                }
            }

            if (renamed || !_filenameChanged) {
                AppCast.VIEW_FOLDER_CHANGED.send(getContext(), file.getParent(), true);
                if (_callback != null) {
                    _callback.callback(file);
                }
                shareUtil.freeContextRef();
                dialog.dismiss();
            }
        });

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(view -> dialog.dismiss());

        return dialog;
    }

    private AlertDialog.Builder setUpDialog(final File file, LayoutInflater inflater) {
        View root;
        AlertDialog.Builder dialogBuilder;
        boolean darkTheme = AppSettings.get().isDarkThemeEnabled();
        dialogBuilder = new AlertDialog.Builder(getActivity(), darkTheme ?
                R.style.Theme_AppCompat_Dialog : R.style.Theme_AppCompat_Light_Dialog);
        root = inflater.inflate(R.layout.rename__dialog, null);

        dialogBuilder.setTitle(getResources().getString(R.string.rename));
        dialogBuilder.setView(root);

        EditText editText = root.findViewById(R.id.new_name);
        editText.requestFocus();
        editText.setText(file.getName());
        editText.setTextColor(ContextCompat.getColor(root.getContext(),
                darkTheme ? R.color.dark__primary_text : R.color.light__primary_text));

        dialogBuilder.setPositiveButton(getString(android.R.string.ok), null);
        dialogBuilder.setNegativeButton(getString(R.string.cancel), null);

        return dialogBuilder;
    }
}
