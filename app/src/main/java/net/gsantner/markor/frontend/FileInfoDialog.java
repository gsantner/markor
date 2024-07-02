/*#######################################################
 *
 *   Maintained 2017-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.frontend;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import net.gsantner.markor.ApplicationObject;
import net.gsantner.markor.R;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.opoc.util.GsContextUtils;
import net.gsantner.opoc.util.GsFileUtils;

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
        dialogBuilder = new AlertDialog.Builder(inflater.getContext(), R.style.Theme_AppCompat_DayNight_Dialog_Rounded);
        root = inflater.inflate(R.layout.file_info_dialog, null);

        dialogBuilder.setView(root);

        tv(root, R.id.ui__fileinfodialog__name).setText(file.getName());
        tv(root, R.id.ui__fileinfodialog__location).setText(file.getParentFile().getAbsolutePath());
        tv(root, R.id.ui__fileinfodialog__last_modified).setText(DateUtils.formatDateTime(root.getContext(), file.lastModified(), (DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_NUMERIC_DATE)));
        tv(root, R.id.ui__fileinfodialog__last_modified_caption).setText(getString(R.string.last_modified_witharg, "").replace(":", "").trim());
        tv(root, R.id.ui__fileinfodialog__size_description).setText(GsFileUtils.getReadableFileSize(file.length(), false));
        tv(root, R.id.ui__fileinfodialog__mimetype_description).setText(GsFileUtils.getMimeType(file));
        tv(root, R.id.ui__fileinfodialog__location).setOnLongClickListener(v -> {
            GsContextUtils.instance.setClipboard(v.getContext(), file.getAbsolutePath());
            Toast.makeText(v.getContext(), R.string.clipboard, Toast.LENGTH_SHORT).show();
            return true;
        });


        // Number of lines and character count only apply for files.
        root.findViewById(R.id.ui__fileinfodialog__textinfo).setVisibility(View.GONE);
        root.findViewById(R.id.ui__fileinfodialog__fileinfo).setVisibility(file.isFile() ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.ui__fileinfodialog__filesettings).setVisibility(file.isFile() ? View.VISIBLE : View.GONE);
        if (GsFileUtils.isTextFile(file)) {
            root.findViewById(R.id.ui__fileinfodialog__textinfo).setVisibility(View.VISIBLE);
            AtomicInteger valLines = new AtomicInteger(0);
            AtomicInteger valChars = new AtomicInteger(0);
            AtomicInteger valWords = new AtomicInteger(0);
            GsFileUtils.retrieveTextFileSummary(file, valChars, valLines, valWords);

            tv(root, R.id.ui__fileinfodialog__textinfo_caption).setText(getString(R.string.text_lines) + String.format(" / %s / %s", getString(R.string.text_words), getString(R.string.text_characters)).replace("Text ", ""));
            tv(root, R.id.ui__fileinfodialog__textinfo_description).setText(String.format(Locale.ENGLISH, "%d / %d / %d", valLines.intValue(), valWords.intValue(), valChars.intValue()));

        }
        dialogBuilder.setPositiveButton(getString(android.R.string.ok), (dialogInterface, i) -> {
            dialogInterface.dismiss();
        });

        // Hide checkbox
        final AppSettings appSettings = ApplicationObject.settings();
        CheckBox checkHideInRecents = root.findViewById(R.id.ui__fileinfodialog__recents);
        checkHideInRecents.setChecked(appSettings.listFileInRecents(file));
        checkHideInRecents.setOnCheckedChangeListener((buttonView, isChecked) -> appSettings.setListFileInRecents(file, isChecked));

        return dialogBuilder;
    }

    private TextView tv(View root, @IdRes int resId) {
        return root.findViewById(resId);
    }
}
