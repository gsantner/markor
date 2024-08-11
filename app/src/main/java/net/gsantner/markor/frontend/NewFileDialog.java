/*#######################################################
 *
 *   Maintained 2018-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 *
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.frontend;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputFilter;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListPopupWindow;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.DialogFragment;

import net.gsantner.markor.ApplicationObject;
import net.gsantner.markor.R;
import net.gsantner.markor.format.FormatRegistry;
import net.gsantner.markor.frontend.textview.HighlightingEditor;
import net.gsantner.markor.frontend.textview.TextViewUtils;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.util.MarkorContextUtils;
import net.gsantner.opoc.util.GsCollectionUtils;
import net.gsantner.opoc.util.GsContextUtils;
import net.gsantner.opoc.util.GsFileUtils;
import net.gsantner.opoc.wrapper.GsAndroidSpinnerOnItemSelectedAdapter;
import net.gsantner.opoc.wrapper.GsCallback;
import net.gsantner.opoc.wrapper.GsTextWatcherAdapter;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import other.de.stanetz.jpencconverter.JavaPasswordbasedCryption;

public class NewFileDialog extends DialogFragment {
    public static final String FRAGMENT_TAG = NewFileDialog.class.getName();
    public static final String EXTRA_DIR = "EXTRA_DIR";
    public static final String EXTRA_ALLOW_CREATE_DIR = "EXTRA_ALLOW_CREATE_DIR";

    public static final int MAX_TITLE_FORMATS = 10;

    private static final List<Integer> NEW_FILE_FORMATS = Arrays.asList(
            FormatRegistry.FORMAT_MARKDOWN,
            FormatRegistry.FORMAT_PLAIN,
            FormatRegistry.FORMAT_TODOTXT,
            FormatRegistry.FORMAT_WIKITEXT,
            FormatRegistry.FORMAT_ASCIIDOC,
            FormatRegistry.FORMAT_ORGMODE,
            FormatRegistry.FORMAT_CSV
    );

    private GsCallback.a1<File> callback;

    public static NewFileDialog newInstance(
            final File sourceFile,
            final boolean allowCreateDir,
            final GsCallback.a1<File> callback
    ) {
        NewFileDialog dialog = new NewFileDialog();
        Bundle args = new Bundle();
        args.putSerializable(EXTRA_DIR, sourceFile);
        args.putSerializable(EXTRA_ALLOW_CREATE_DIR, allowCreateDir);
        dialog.setArguments(args);
        dialog.callback = callback;
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final File file = (File) getArguments().getSerializable(EXTRA_DIR);
        final boolean allowCreateDir = getArguments().getBoolean(EXTRA_ALLOW_CREATE_DIR);

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        AlertDialog.Builder dialogBuilder = makeDialog(file, allowCreateDir, inflater);
        AlertDialog dialog = dialogBuilder.show();
        Window w;
        if ((w = dialog.getWindow()) != null) {
            w.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }
        return dialog;
    }

    @SuppressLint("SetTextI18n")
    private AlertDialog.Builder makeDialog(final File basedir, final boolean allowCreateDir, LayoutInflater inflater) {
        final Activity activity = getActivity();
        final AppSettings appSettings = ApplicationObject.settings();
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(inflater.getContext(), R.style.Theme_AppCompat_DayNight_Dialog_Rounded);
        final View root = inflater.inflate(R.layout.new_file_dialog, null);

        final EditText titleEdit = root.findViewById(R.id.new_file_dialog__name);
        final EditText extEdit = root.findViewById(R.id.new_file_dialog__ext);
        final CheckBox encryptCheckbox = root.findViewById(R.id.new_file_dialog__encrypt);
        final CheckBox utf8BomCheckbox = root.findViewById(R.id.new_file_dialog__utf8_bom);
        final Spinner typeSpinner = root.findViewById(R.id.new_file_dialog__type);
        final Spinner templateSpinner = root.findViewById(R.id.new_file_dialog__template);
        final EditText formatEdit = root.findViewById(R.id.new_file_dialog__name_format);
        final TextView formatSpinner = root.findViewById(R.id.new_file_dialog__name_format_spinner);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && appSettings.isDefaultPasswordSet()) {
            encryptCheckbox.setChecked(appSettings.getNewFileDialogLastUsedEncryption());
        } else {
            encryptCheckbox.setVisibility(View.GONE);
        }

        utf8BomCheckbox.setChecked(appSettings.getNewFileDialogLastUsedUtf8Bom());
        utf8BomCheckbox.setVisibility(appSettings.isExperimentalFeaturesEnabled() ? View.VISIBLE : View.GONE);
        titleEdit.requestFocus();
        new Handler().postDelayed(new GsContextUtils.DoTouchView(titleEdit), 200);

        titleEdit.setFilters(new InputFilter[]{GsContextUtils.instance.makeFilenameInputFilter()});
        extEdit.setFilters(titleEdit.getFilters());

        // Build a list of available formats
        // -----------------------------------------------------------------------------------------
        final List<FormatRegistry.Format> formats = GsCollectionUtils.map(
                NEW_FILE_FORMATS, t -> GsCollectionUtils.selectFirst(FormatRegistry.FORMATS, f -> f.format == t));

        // Setup title format spinner and actions
        // -----------------------------------------------------------------------------------------
        final ArrayAdapter<String> formatAdapter = new ArrayAdapter<>(
                activity, android.R.layout.simple_spinner_dropdown_item);

        formatAdapter.add("");
        formatAdapter.addAll(appSettings.getTitleFormats());

        final ListPopupWindow formatPopup = new ListPopupWindow(activity);
        formatPopup.setAdapter(formatAdapter);
        formatPopup.setAnchorView(formatEdit);
        formatPopup.setOnItemClickListener((parent, view, position, id) -> {
            formatEdit.setText(formatAdapter.getItem(position));
            formatPopup.dismiss();
        });

        formatSpinner.setOnClickListener(v -> formatPopup.show());

        // Setup template spinner and action
        // -----------------------------------------------------------------------------------------
        final List<Pair<String, File>> snippets = appSettings.getSnippetFiles();
        final List<Pair<String, String>> templates = appSettings.getBuiltinTemplates();
        final ArrayAdapter<String> templateAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item);
        templateAdapter.add(activity.getString(R.string.empty_file));
        templateAdapter.addAll(GsCollectionUtils.map(snippets, p -> p.first));
        templateAdapter.addAll(GsCollectionUtils.map(templates, p -> p.first));
        templateSpinner.setAdapter(templateAdapter);

        // Setup type / format spinner and action
        // -----------------------------------------------------------------------------------------
        final ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item);
        typeAdapter.addAll(GsCollectionUtils.map(formats, f -> activity.getString(f.name)));
        typeSpinner.setAdapter(typeAdapter);

        // Set last used extension on first call to the typeSpinner listener
        final String[] lastExt = new String[]{appSettings.getNewFileDialogLastUsedExtension()};

        typeSpinner.setOnItemSelectedListener(new GsAndroidSpinnerOnItemSelectedAdapter(pos -> {
            final FormatRegistry.Format fmt = formats.get(pos);

            if (lastExt[0] != null) {
                extEdit.setText(lastExt[0]);
                lastExt[0] = null;
            } else if (fmt.defaultExtensionWithDot != null) {
                if (encryptCheckbox.isChecked()) {
                    extEdit.setText(fmt.defaultExtensionWithDot + JavaPasswordbasedCryption.DEFAULT_ENCRYPTION_EXTENSION);
                } else {
                    extEdit.setText(fmt.defaultExtensionWithDot);
                }
            }

            final int tpos = templateAdapter.getPosition(appSettings.getTypeTemplate(fmt.format));
            if (tpos >= 0) {
                templateSpinner.setSelection(tpos);
            }
        }));

        // Setup other checkboxes etc
        // -----------------------------------------------------------------------------------------
        encryptCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            final String currentExtention = extEdit.getText().toString();
            if (isChecked) {
                if (!currentExtention.endsWith(JavaPasswordbasedCryption.DEFAULT_ENCRYPTION_EXTENSION)) {
                    extEdit.setText(currentExtention + JavaPasswordbasedCryption.DEFAULT_ENCRYPTION_EXTENSION);
                }
            } else if (currentExtention.endsWith(JavaPasswordbasedCryption.DEFAULT_ENCRYPTION_EXTENSION)) {
                extEdit.setText(currentExtention.replace(JavaPasswordbasedCryption.DEFAULT_ENCRYPTION_EXTENSION, ""));
            }
            appSettings.setNewFileDialogLastUsedEncryption(isChecked);
        });

        utf8BomCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            appSettings.setNewFileDialogLastUsedUtf8Bom(isChecked);
        });

        dialogBuilder.setView(root);

        // Setup button click actions
        // -----------------------------------------------------------------------------------------

        final GsCallback.s0 getTitle = () -> {
            final String title = titleEdit.getText().toString().trim();

            String format = formatEdit.getText().toString().trim();
            if (format.isEmpty() && title.isEmpty()) {
                format = "`yyyy-MM-dd'T'HHmmss`";
            } else if (format.isEmpty()) {
                format = "{{title}}";
            } else if (!title.isEmpty() && !format.contains("{{title}}")) {
                format += "_{{title}}";
            }

            return TextViewUtils.interpolateSnippet(format, title, "").trim();
        };

        final @ColorInt int color = titleEdit.getCurrentTextColor();
        titleEdit.addTextChangedListener(new GsTextWatcherAdapter() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    final String title = getTitle.callback();
                    final String ext = extEdit.getText().toString().trim();
                    final String fn = GsFileUtils.getFilteredFilenameWithoutDisallowedChars(title + ext);
                    if (new File(basedir, fn).exists()) {
                        titleEdit.setTextColor(0xffff0000);
                    } else {
                        titleEdit.setTextColor(color);
                    }
                } catch (Exception ignored) {
                    titleEdit.setTextColor(color);
                }
            }
        });

        final MarkorContextUtils cu = new MarkorContextUtils(getContext());
        dialogBuilder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.dismiss());
        dialogBuilder.setPositiveButton(getString(android.R.string.ok), (dialogInterface, i) -> {
            final FormatRegistry.Format fmt = formats.get(typeSpinner.getSelectedItemPosition());

            final String title = getTitle.callback();
            final String ext = extEdit.getText().toString().trim();
            String fileName = GsFileUtils.getFilteredFilenameWithoutDisallowedChars(title + ext);

            if (fmt.format == FormatRegistry.FORMAT_WIKITEXT) {
                fileName = fileName.replace(" ", "_");
            }

            // Get template string
            // -------------------------------------------------------------------------------------
            final int ti = templateSpinner.getSelectedItemPosition();
            final String template;
            if (ti == 0) {
                template = "";
            } else if (ti <= snippets.size()) {
                template = GsFileUtils.readTextFileFast(snippets.get(ti - 1).second).first;
            } else {
                template = templates.get(ti - snippets.size() - 1).second;
            }

            final Pair<String, Integer> content = getTemplateContent(template, title);
            // -------------------------------------------------------------------------------------

            final File file = new File(basedir, fileName);

            // Most of the logic we want is in the document class so we just reuse it
            final Document document = new Document(file);

            // These are done even if the file isn't created
            final String titleFormat = formatEdit.getText().toString().trim();
            appSettings.setTemplateTitleFormat(templateAdapter.getItem(ti), titleFormat);
            appSettings.setTypeTemplate(fmt.format, (String) templateSpinner.getSelectedItem());
            appSettings.setNewFileDialogLastUsedType(fmt.format);
            appSettings.setNewFileDialogLastUsedExtension(extEdit.getText().toString().trim());

            if (!titleFormat.isEmpty()) {
                appSettings.saveTitleFormat(titleFormat, MAX_TITLE_FORMATS);
            }

            if (!file.exists() || file.length() <= GsContextUtils.TEXTFILE_OVERWRITE_MIN_TEXT_LENGTH) {
                document.saveContent(activity, content.first, cu, true);

                // We only make these changes if the file did not already exist
                appSettings.setDocumentFormat(document.getPath(), fmt.format);
                appSettings.setLastEditPosition(document.getPath(), content.second);

                callback(file);

            } else if (file.canWrite()) {
                callback(file);
            } else {
                Toast.makeText(activity, R.string.failed_to_create_backup, Toast.LENGTH_LONG).show();
            }

            dialogInterface.dismiss();
        });

        dialogBuilder.setNeutralButton(R.string.folder, (dialogInterface, i) -> {

            final String title = getTitle.callback();
            final String dirName = GsFileUtils.getFilteredFilenameWithoutDisallowedChars(title);
            final File f = new File(basedir, dirName);

            final String titleFormat = formatEdit.getText().toString().trim();
            if (!titleFormat.isEmpty()) {
                appSettings.saveTitleFormat(titleFormat, MAX_TITLE_FORMATS);
            }

            if (cu.isUnderStorageAccessFolder(getContext(), f, true)) {
                DocumentFile dof = cu.getDocumentFile(getContext(), f, true);
                if (dof != null && dof.exists()) {
                    callback(f);
                }
            } else if (f.isDirectory() || f.mkdirs()) {
                callback(f);
            }

            dialogInterface.dismiss();
        });

        if (!allowCreateDir) {
            dialogBuilder.setNeutralButton("", null);
        }

        // Initial creation - loop through and set type
        final int lastUsedType = appSettings.getNewFileDialogLastUsedType();
        final List<Integer> indices = GsCollectionUtils.indices(formats, f -> f.format == lastUsedType);
        typeSpinner.setSelection(indices.isEmpty() ? 0 : indices.get(0));

        titleEdit.requestFocus();

        return dialogBuilder;
    }

    private void callback(final File file) {
        try {
            callback.callback(file);
        } catch (Exception ignored) {
        }
    }

    public void setCallback(final GsCallback.a1<File> callback) {
        this.callback = callback;
    }

    private Pair<String, Integer> getTemplateContent(final String template, final String name) {
        String text = TextViewUtils.interpolateSnippet(template, name, "");

        final int startingIndex = template.indexOf(HighlightingEditor.PLACE_CURSOR_HERE_TOKEN);
        text = text.replace(HighlightingEditor.PLACE_CURSOR_HERE_TOKEN, "");

        // Has no utility in a new file
        text = text.replace(HighlightingEditor.INSERT_SELECTION_HERE_TOKEN, "");

        return Pair.create(text, startingIndex);
    }

    public static class ReselectSpinner extends androidx.appcompat.widget.AppCompatSpinner {

        public ReselectSpinner(Context context) {
            super(context);
        }

        public ReselectSpinner(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public ReselectSpinner(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        @Override
        public void setSelection(int position, boolean animate) {
            boolean sameSelected = position == getSelectedItemPosition();
            super.setSelection(position, animate);
            if (sameSelected) {
                getOnItemSelectedListener().onItemSelected(this, getSelectedView(), position, getSelectedItemId());
            }
        }

        @Override
        public void setSelection(int position) {
            boolean sameSelected = position == getSelectedItemPosition();
            super.setSelection(position);
            if (sameSelected) {
                getOnItemSelectedListener().onItemSelected(this, getSelectedView(), position, getSelectedItemId());
            }
        }
    }
}