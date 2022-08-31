package net.gsantner.markor.frontend.filesearch;

import android.app.Activity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.content.ContextCompat;

import net.gsantner.markor.ApplicationObject;
import net.gsantner.markor.R;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.opoc.util.GsContextUtils;
import net.gsantner.opoc.wrapper.GsCallback;

import java.util.concurrent.atomic.AtomicReference;

public class FileSearchDialog {
    public static void showDialog(final Activity activity, final GsCallback.a1<FileSearchEngine.SearchOptions> dialogCallback) {
        final AtomicReference<AlertDialog> dialogRef = new AtomicReference<>();
        dialogRef.set(buildDialog(activity, dialogRef, dialogCallback).create());
        if (dialogRef.get().getWindow() != null) {
            dialogRef.get().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
        dialogRef.get().show();
        if (dialogRef.get().getWindow() != null) {
            dialogRef.get().getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private static AlertDialog.Builder buildDialog(final Activity activity, final AtomicReference<AlertDialog> dialog, final GsCallback.a1<FileSearchEngine.SearchOptions> dialogCallback) {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity, R.style.Theme_AppCompat_DayNight_Dialog);

        final ScrollView scrollView = new ScrollView(activity);
        final LinearLayout dialogLayout = new LinearLayout(activity);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        final int dp4px = GsContextUtils.instance.convertDpToPx(activity, 4);
        final int textColor = ContextCompat.getColor(activity, R.color.primary_text);

        final LinearLayout.LayoutParams margins = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        margins.setMargins(dp4px * 5, dp4px, dp4px * 5, dp4px);

        final LinearLayout.LayoutParams subCheckBoxMargins = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subCheckBoxMargins.setMargins(dp4px * 5 * 2, dp4px, dp4px * 5, dp4px);

        final TextView messageTextView = new TextView(activity);
        final AppCompatEditText searchEditText = new AppCompatEditText(activity);
        final Spinner queryHistorySpinner = new Spinner(activity);
        final CheckBox regexCheckBox = new CheckBox(activity);
        final CheckBox caseSensitivityCheckBox = new CheckBox(activity);
        final CheckBox searchInContentCheckBox = new CheckBox(activity);
        final CheckBox onlyFirstContentMatchCheckBox = new CheckBox(activity);

        final AppSettings appSettings = ApplicationObject.settings();
        final GsCallback.a0 submit = () -> {
            final String query = searchEditText.getText().toString();
            if (dialogCallback != null && !TextUtils.isEmpty(query)) {
                FileSearchEngine.SearchOptions opt = new FileSearchEngine.SearchOptions();
                opt.query = query;
                opt.isRegexQuery = regexCheckBox.isChecked();
                opt.isCaseSensitiveQuery = caseSensitivityCheckBox.isChecked();
                opt.isSearchInContent = searchInContentCheckBox.isChecked();
                opt.isOnlyFirstContentMatch = onlyFirstContentMatchCheckBox.isChecked();
                opt.ignoredDirectories = appSettings.getFileSearchIgnorelist();
                opt.maxSearchDepth = appSettings.getSearchMaxDepth();
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    opt.password = appSettings.getDefaultPassword();
                }
                appSettings.setSearchQueryRegexUsing(opt.isRegexQuery);
                appSettings.setSearchQueryCaseSensitivity(opt.isCaseSensitiveQuery);
                appSettings.setSearchInContent(opt.isSearchInContent);
                appSettings.setOnlyFirstContentMatch(opt.isOnlyFirstContentMatch);
                dialogCallback.callback(opt);
            }
        };

        // TextView
        messageTextView.setText(R.string.recursive_search_in_current_directory);
        dialogLayout.addView(messageTextView, margins);

        // EdiText: Search query input
        searchEditText.setHint(R.string.search);
        searchEditText.setSingleLine(true);
        searchEditText.setMaxLines(1);
        searchEditText.setTextColor(textColor);
        searchEditText.setHintTextColor((textColor & 0x00FFFFFF) | 0x99000000);
        searchEditText.setOnKeyListener((keyView, keyCode, keyEvent) -> {
            if ((keyEvent.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                if (dialog != null && dialog.get() != null) {
                    dialog.get().dismiss();
                }
                submit.callback();
                return true;
            }
            return false;
        });
        dialogLayout.addView(searchEditText, margins);

        // Spinner: History
        if (FileSearchEngine.queryHistory.size() > 0) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, R.layout.list_group_history_item, FileSearchEngine.queryHistory);
            queryHistorySpinner.setAdapter(adapter);

            queryHistorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String query = (String) parent.getItemAtPosition(position);
                    searchEditText.setText(query);
                    searchEditText.selectAll();
                    searchEditText.requestFocus();
                }

                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            dialogLayout.addView(queryHistorySpinner);
        }

        // Checkbox: Regex search
        regexCheckBox.setText(R.string.regex_search);
        regexCheckBox.setChecked(appSettings.isSearchQueryUseRegex());
        dialogLayout.addView(regexCheckBox, margins);

        // Checkbox: Case sensitive
        caseSensitivityCheckBox.setText(R.string.case_sensitive);
        caseSensitivityCheckBox.setChecked(appSettings.isSearchQueryCaseSensitive());
        dialogLayout.addView(caseSensitivityCheckBox, margins);

        // Checkbox: Search in content
        searchInContentCheckBox.setText(R.string.search_in_content);
        searchInContentCheckBox.setChecked(appSettings.isSearchInContent());
        searchInContentCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            onlyFirstContentMatchCheckBox.setVisibility(isChecked ? View.VISIBLE : View.INVISIBLE);
        });
        dialogLayout.addView(searchInContentCheckBox, margins);

        // Checkbox: Only first content match
        onlyFirstContentMatchCheckBox.setText(R.string.stop_search_after_first_match);
        onlyFirstContentMatchCheckBox.setChecked(appSettings.isOnlyFirstContentMatch());
        onlyFirstContentMatchCheckBox.setVisibility(searchInContentCheckBox.isChecked() ? View.VISIBLE : View.INVISIBLE);
        dialogLayout.addView(onlyFirstContentMatchCheckBox, subCheckBoxMargins);

        // ScrollView
        scrollView.addView(dialogLayout);

        // Configure dialog
        dialogBuilder.setTitle(R.string.search)
                .setOnCancelListener(null)
                .setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> dialogInterface.dismiss())
                .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    submit.callback();
                })
                .setView(scrollView);

        return dialogBuilder;
    }
}
