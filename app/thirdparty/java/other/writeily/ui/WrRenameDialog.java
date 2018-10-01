/*#######################################################
 * Copyright (c) 2014 Jeff Martin
 * Copyright (c) 2015 Pedro Lafuente
 * Copyright (c) 2017-2018 Gregor Santner
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
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import net.gsantner.markor.R;
import net.gsantner.markor.util.AppCast;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.opoc.util.FileUtils;

import java.io.File;

public class WrRenameDialog extends DialogFragment {
    public static final String EXTRA_FILEPATH = "EXTRA_FILEPATH";
    public static final String FRAGMENT_TAG = "WrRenameDialog";

    public static WrRenameDialog newInstance(File sourceFile) {
        WrRenameDialog dialog = new WrRenameDialog();
        Bundle args = new Bundle();
        args.putSerializable(EXTRA_FILEPATH, sourceFile);
        dialog.setArguments(args);
        return dialog;
    }


    private EditText _newNameField;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final File file = (File) getArguments().getSerializable(EXTRA_FILEPATH);

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        AlertDialog.Builder dialogBuilder = setUpDialog(file, inflater);
        AlertDialog dialog = dialogBuilder.show();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        _newNameField = dialog.findViewById(R.id.new_name);
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
        editText.setText(file.getName());
        editText.setTextColor(ContextCompat.getColor(root.getContext(),
                darkTheme ? R.color.dark__primary_text : R.color.light__primary_text));


        dialogBuilder.setPositiveButton(getString(android.R.string.ok), (dialog, which) -> {
            if (FileUtils.renameFileInSameFolder(file, _newNameField.getText().toString())) {
                AppCast.VIEW_FOLDER_CHANGED.send(getContext(), file.getParent(), true);
            }
        });

        dialogBuilder.setNegativeButton(getString(android.R.string.cancel), (dialog, which) -> dialog.dismiss());

        return dialogBuilder;
    }


}
