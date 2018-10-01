/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import net.gsantner.markor.R;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.opoc.util.FileUtils;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

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

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final File file = (File) getArguments().getSerializable(EXTRA_FILEPATH);

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        AlertDialog.Builder dialogBuilder = setUpDialog(file, inflater);
        AlertDialog dialog = dialogBuilder.show();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        return dialog;
    }

    private AlertDialog.Builder setUpDialog(final File file, LayoutInflater inflater) {
        View root;
        AlertDialog.Builder dialogBuilder;
        boolean darkTheme = AppSettings.get().isDarkThemeEnabled();
        dialogBuilder = new AlertDialog.Builder(inflater.getContext(), darkTheme ?
                R.style.Theme_AppCompat_Dialog : R.style.Theme_AppCompat_Light_Dialog);
        root = inflater.inflate(R.layout.file_info_dialog, null);

        dialogBuilder.setTitle(getResources().getString(R.string.details));
        dialogBuilder.setView(root);

        TextView textView = root.findViewById(R.id.ui__fileinfodialog__description);
        textView.setText(file.getName());

        TextView locationView = root.findViewById(R.id.ui__fileinfodialog__location_description);
        locationView.setText(file.getParentFile().getAbsolutePath());

        TextView sizeView = root.findViewById(R.id.ui__fileinfodialog__size_description);
        sizeView.setText(FileUtils.getReadableFileSize(file.length(), false));

        // Number of lines and character count only apply for files.
        if (FileUtils.isTextFile(file)) {
            TextView textNumLinesView = root.findViewById(R.id.ui__fileinfodialog__numberlines_description);

            AtomicInteger linesCount = new AtomicInteger(0);
            AtomicInteger charactersCount = new AtomicInteger(0);
            FileUtils.retrieveTextFileSummary(file, charactersCount, linesCount);

            textNumLinesView.setText((linesCount.toString()));

            TextView textNumCharactersView = root.findViewById(R.id.ui__fileinfodialog__numbercharacters_description);
            textNumCharactersView.setText(charactersCount.toString());
        } else {
            root.findViewById(R.id.ui__fileinfodialog__textinfo).setVisibility(View.GONE);
        }
        dialogBuilder.setPositiveButton(getString(android.R.string.ok), (dialogInterface, i) -> {
            dialogInterface.dismiss();

        });

        return dialogBuilder;
    }
}
