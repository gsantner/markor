/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;

import net.gsantner.markor.R;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.opoc.util.Callback;
import net.gsantner.opoc.util.ContextUtils;
import net.gsantner.opoc.util.FileUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class NewFileDialog extends DialogFragment {
    public static final String FRAGMENT_TAG = "net.gsantner.markor.ui.NewFileDialog";
    public static final String EXTRA_DIR = "EXTRA_DIR";
    private Callback.a2<Boolean, File> callback;

    public static NewFileDialog newInstance(File sourceFile, Callback.a2<Boolean, File> callback) {
        NewFileDialog dialog = new NewFileDialog();
        Bundle args = new Bundle();
        args.putSerializable(EXTRA_DIR, sourceFile);
        dialog.setArguments(args);
        dialog.callback = callback;
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final File file = (File) getArguments().getSerializable(EXTRA_DIR);

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        AlertDialog.Builder dialogBuilder = makeDialog(file, inflater);
        AlertDialog dialog = dialogBuilder.show();
        Window w;
        if ((w = dialog.getWindow()) != null) {
            w.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
        return dialog;
    }

    private AlertDialog.Builder makeDialog(final File basedir, LayoutInflater inflater) {
        View root;
        AlertDialog.Builder dialogBuilder;
        boolean darkTheme = AppSettings.get().isDarkThemeEnabled();
        dialogBuilder = new AlertDialog.Builder(inflater.getContext(), darkTheme ?
                R.style.Theme_AppCompat_Dialog : R.style.Theme_AppCompat_Light_Dialog);
        root = inflater.inflate(R.layout.new_file_dialog, null);

        final EditText fileNameEdit = root.findViewById(R.id.new_file_dialog__name);
        final EditText fileExtEdit = root.findViewById(R.id.new_file_dialog__ext);
        final Spinner typeSpinner = root.findViewById(R.id.new_file_dialog__type);
        final String[] typeSpinnerToExtension = getResources().getStringArray(R.array.new_file_types__file_extension);

        fileNameEdit.setFilters(new InputFilter[]{ContextUtils.INPUTFILTER_FILENAME});
        fileExtEdit.setFilters(fileNameEdit.getFilters());

        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @SuppressLint({"SimpleDateFormat", "SetTextI18n"})
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String prefix = null;
                String ext = i < typeSpinnerToExtension.length ? typeSpinnerToExtension[i] : "";
                switch (i) {
                    case 4: {
                        prefix = new SimpleDateFormat("YYYY-MM-dd-").format(new Date());
                        break;
                    }
                }

                if (ext != null) {
                    fileExtEdit.setText(ext);
                }
                if (prefix != null && !fileNameEdit.getText().toString().startsWith(prefix)) {
                    fileNameEdit.setText(prefix + fileNameEdit.getText().toString());
                }
                fileNameEdit.setSelection(fileNameEdit.length());
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        dialogBuilder.setView(root);
        fileNameEdit.requestFocus();
        setKb();

        dialogBuilder
                .setPositiveButton(getString(android.R.string.ok), (dialogInterface, i) -> {
                    if (ez(fileNameEdit)) {
                        return;
                    }
                    File f = new File(basedir, fileNameEdit.getText().toString() + fileExtEdit.getText().toString());
                    callback(FileUtils.touch(f), f);
                    dialogInterface.dismiss();
                })
                .setNeutralButton(R.string.folder, (dialogInterface, i) -> {
                    if (ez(fileNameEdit)) {
                        return;
                    }
                    File f = new File(basedir, fileNameEdit.getText().toString());
                    callback(f.mkdirs() || f.exists(), f);
                    dialogInterface.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                });

        return dialogBuilder;
    }

    private boolean ez(EditText et) {
        return et.getText().toString().isEmpty();
    }

    private void callback(boolean ok, File file) {
        try {
            callback.callback(ok, file);
        } catch (Exception ignored) {
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void setKb() {
        try {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        } catch (Exception ignored) {

        }
    }
}
