package net.gsantner.markor.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import net.gsantner.markor.R;
import net.gsantner.markor.util.AppSettings;

import java.io.File;

public class FileInfoDialog extends DialogFragment {
    public static final String EXTRA_FILEPATH = "EXTRA_FILEPATH";
    public static final String FRAGMENT_TAG = "fileInfoDialog";

    public static FileInfoDialog newInstance(File sourceFile) {
        FileInfoDialog dialog = new FileInfoDialog();
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
        root = inflater.inflate(R.layout.ui__file_info_dialog, null);

        dialogBuilder.setTitle(getResources().getString(R.string.info));
        dialogBuilder.setView(root);

        TextView textView = root.findViewById(R.id.ui__filesystem_item__description);
        textView.setText(file.getName());

        TextView locationView = root.findViewById(R.id.ui__filesystem_item__location_description);
        locationView.setText(file.getAbsolutePath());

        TextView sizeView = root.findViewById(R.id.ui__filesystem_item__size_description);
        long totalSizeBytes = file.getTotalSpace();
        String humanReadableSize = android.text.format.Formatter.formatShortFileSize(root.getContext(), totalSizeBytes);
        sizeView.setText(humanReadableSize + "(" + Long.toString(file.getTotalSpace()) + ")");

        dialogBuilder.setPositiveButton(getString(android.R.string.ok), (dialogInterface, i) -> {
            dialogInterface.dismiss();

        });

        return dialogBuilder;
    }

}
