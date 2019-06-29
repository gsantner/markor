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

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import net.gsantner.markor.R;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.opoc.util.FileUtils;
import net.gsantner.opoc.util.ShareUtil;

import java.io.File;
import java.util.Locale;
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

    public static FileInfoDialog show(File sourceFile, FragmentManager fragmentManager) {
        FileInfoDialog dialog = FileInfoDialog.newInstance(sourceFile);
        dialog.show(fragmentManager, FileInfoDialog.FRAGMENT_TAG);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final File file = (File) getArguments().getSerializable(EXTRA_FILEPATH);

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        AlertDialog.Builder dialogBuilder = setUpDialog(file, inflater);
        AlertDialog dialog = dialogBuilder.show();

        Window w;
        if ((w = dialog.getWindow()) != null) {
            w.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }

        return dialog;
    }

    @SuppressLint("SetTextI18n")
    private AlertDialog.Builder setUpDialog(final File file, LayoutInflater inflater) {
        View root;
        AlertDialog.Builder dialogBuilder;
        AppSettings appSettings = new AppSettings(inflater.getContext());
        dialogBuilder = new AlertDialog.Builder(inflater.getContext(), appSettings.isDarkThemeEnabled() ?
                R.style.Theme_AppCompat_Dialog : R.style.Theme_AppCompat_Light_Dialog);
        root = inflater.inflate(R.layout.file_info_dialog, null);

        dialogBuilder.setView(root);

        tv(root, R.id.ui__fileinfodialog__name).setText(file.getName());
        tv(root, R.id.ui__fileinfodialog__location).setText(file.getParentFile().getAbsolutePath());
        tv(root, R.id.ui__fileinfodialog__last_modified).setText(DateUtils.formatDateTime(root.getContext(), file.lastModified(), (DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_NUMERIC_DATE)));
        tv(root, R.id.ui__fileinfodialog__last_modified_caption).setText(getString(R.string.last_modified_witharg, "").replace(":", "").trim());
        tv(root, R.id.ui__fileinfodialog__size_description).setText(FileUtils.getReadableFileSize(file.length(), false));
        tv(root, R.id.ui__fileinfodialog__location).setOnLongClickListener(v -> {
            new ShareUtil(v.getContext()).setClipboard(file.getAbsolutePath());
            Toast.makeText(v.getContext(), R.string.clipboard, Toast.LENGTH_SHORT).show();
            return true;
        });


        // Number of lines and character count only apply for files.
        root.findViewById(R.id.ui__fileinfodialog__textinfo).setVisibility(View.GONE);
        root.findViewById(R.id.ui__fileinfodialog__fileinfo).setVisibility(file.isFile() ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.ui__fileinfodialog__filesettings).setVisibility(file.isFile() ? View.VISIBLE : View.GONE);
        if (FileUtils.isTextFile(file)) {
            root.findViewById(R.id.ui__fileinfodialog__textinfo).setVisibility(View.VISIBLE);
            AtomicInteger valLines = new AtomicInteger(0);
            AtomicInteger valChars = new AtomicInteger(0);
            AtomicInteger valWords = new AtomicInteger(0);
            FileUtils.retrieveTextFileSummary(file, valChars, valLines, valWords);

            tv(root, R.id.ui__fileinfodialog__textinfo_caption).setText(getString(R.string.text_lines) + String.format(" / %s / %s", getString(R.string.text_words), getString(R.string.text_characters)).replace("Text ", ""));
            tv(root, R.id.ui__fileinfodialog__textinfo_description).setText(String.format(Locale.ENGLISH, "%d / %d / %d", valLines.intValue(), valWords.intValue(), valChars.intValue()));

        }
        dialogBuilder.setPositiveButton(getString(android.R.string.ok), (dialogInterface, i) -> {
            dialogInterface.dismiss();
        });

        // Hide checkbox
        CheckBox checkHideInRecents = root.findViewById(R.id.ui__fileinfodialog__recents);
        checkHideInRecents.setChecked(appSettings.listFileInRecents(file));
        checkHideInRecents.setOnCheckedChangeListener((buttonView, isChecked) -> appSettings.setListFileInRecents(file, isChecked));

        return dialogBuilder;
    }

    private TextView tv(View root, @IdRes int resId) {
        return root.findViewById(resId);
    }
}
