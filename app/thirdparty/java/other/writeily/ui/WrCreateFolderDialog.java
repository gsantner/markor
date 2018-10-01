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
import android.widget.EditText;
import android.widget.TextView;

import net.gsantner.markor.R;
import net.gsantner.markor.util.AppCast;
import net.gsantner.markor.util.AppSettings;

public class WrCreateFolderDialog extends DialogFragment {
    public static final String FRAGMENT_TAG = "create_folder_dialog_tag";
    // ----- KEYS -----
    public static final String CURRENT_DIRECTORY_DIALOG_KEY = "current_dir_folder_key";

    private EditText folderNameEditText;
    private String currentDir;

    public WrCreateFolderDialog() {
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        currentDir = getArguments().getString(CURRENT_DIRECTORY_DIALOG_KEY, "");

        View root;
        AlertDialog.Builder dialogBuilder;

        boolean darkTheme = AppSettings.get().isDarkThemeEnabled();
        root = inflater.inflate(R.layout.create_folder__dialog, null);
        dialogBuilder = new AlertDialog.Builder(getActivity(), darkTheme ?
                R.style.Theme_AppCompat_Dialog : R.style.Theme_AppCompat_Light_Dialog);

        dialogBuilder.setTitle(getResources().getString(R.string.create_folder));
        dialogBuilder.setView(root);

        TextView tv = root.findViewById(R.id.create_folder_dialog__folder_name);
        tv.setTextColor(ContextCompat.getColor(root.getContext(),
                darkTheme ? R.color.dark__primary_text : R.color.light__primary_text));


        dialogBuilder.setPositiveButton(getResources().getString(R.string.create), (dialog, which) -> {
            String folder = currentDir + "/" + folderNameEditText.getText().toString();
            AppCast.CREATE_FOLDER.send(getActivity(), folder);
        });

        dialogBuilder.setNegativeButton(getResources().getString(android.R.string.cancel), (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = dialogBuilder.show();
        folderNameEditText = dialog.findViewById(R.id.create_folder_dialog__folder_name);

        return dialog;
    }

}
