package net.gsantner.opoc.ui;

import android.app.Activity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.text.InputType;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import net.gsantner.markor.R;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.opoc.util.Callback;
import net.gsantner.opoc.util.ContextUtils;

import java.util.concurrent.atomic.AtomicReference;


public class FileSearchDialog {

    public static class Options {
        public String query;
        public boolean isRegexQuery;
        public boolean isCaseSensitiveQuery;
        public boolean isSearchInContent;
        public boolean isOnlyFirstContentMatch;

        public Options(final String a_query, final boolean a_isRegexQuery, final boolean a_isCaseSensitiveQuery, final boolean a_isSearchInContent, final boolean a_isOnlyFirstContentMatch) {
            query = a_query;
            isRegexQuery = a_isRegexQuery;
            isCaseSensitiveQuery = a_isCaseSensitiveQuery;
            isSearchInContent = a_isSearchInContent;
            isOnlyFirstContentMatch = a_isOnlyFirstContentMatch;
        }
    }

    public static void showDialog(final Activity activity, final Callback.a1<Options> dialogCallback) {
        final AtomicReference<AlertDialog> dialog = new AtomicReference<>();
        final AlertDialog.Builder dialogBuilder = buildDialog(activity, dialog, dialogCallback);
        dialog.set(dialogBuilder.create());
        Window _window = dialog.get().getWindow();
        if (_window != null) {
            _window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
        dialog.get().show();
        if (_window != null) {
            _window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private static AlertDialog.Builder buildDialog(final Activity activity, final AtomicReference<AlertDialog> dialog, final Callback.a1<Options> dialogCallback) {
        final AppSettings appSettings = new AppSettings(activity);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity, appSettings.isDarkThemeEnabled() ? R.style.Theme_AppCompat_Dialog : R.style.Theme_AppCompat_Light_Dialog);

        final ScrollView scrollView = new ScrollView(activity);
        final LinearLayout dialogLayout = new LinearLayout(activity);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        final int dp4px = (int) (new ContextUtils(dialogLayout.getContext()).convertDpToPx(4));
        final int textColor = ContextCompat.getColor(activity, appSettings.isDarkThemeEnabled() ? R.color.dark__primary_text : R.color.light__primary_text);

        final LinearLayout.LayoutParams margins = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        margins.setMargins(dp4px * 5, dp4px, dp4px * 5, dp4px);

        final LinearLayout.LayoutParams subCheckBoxMargins = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subCheckBoxMargins.setMargins(dp4px * 5 * 2, dp4px, dp4px * 5, dp4px);

        final TextView messageTextView = new TextView(activity);
        final AppCompatEditText searchEditText = new AppCompatEditText(activity);
        Spinner queryHistorySpinner = new Spinner(activity);
        final CheckBox regexCheckBox = new CheckBox(activity);
        final CheckBox caseSensitivityCheckBox = new CheckBox(activity);
        final CheckBox searchInContentCheckBox = new CheckBox(activity);
        final CheckBox onlyFirstContentMatchCheckBox = new CheckBox(activity);

        final Callback.a0 submit = () -> {
            final String query = searchEditText.getText().toString();
            if (dialogCallback != null && !TextUtils.isEmpty(query)) {
                dialogCallback.callback(new FileSearchDialog.Options(query, regexCheckBox.isChecked(), caseSensitivityCheckBox.isChecked(), searchInContentCheckBox.isChecked(), onlyFirstContentMatchCheckBox.isChecked()));
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
        searchEditText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
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
        if (SearchEngine.queryHistory.size() > 0) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, R.layout.list_group_history_item, SearchEngine.queryHistory);
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
        regexCheckBox.setChecked(AppSettings.get().isSearchQueryUseRegex());
        dialogLayout.addView(regexCheckBox, margins);

        // Checkbox: Case sensitive
        caseSensitivityCheckBox.setText(R.string.case_sensitive);
        caseSensitivityCheckBox.setChecked(AppSettings.get().isSearchQueryCaseSensitive());
        dialogLayout.addView(caseSensitivityCheckBox, margins);

        // Checkbox: Search in content
        searchInContentCheckBox.setText(R.string.search_in_content);
        searchInContentCheckBox.setChecked(AppSettings.get().isSearchInContent());
        searchInContentCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            onlyFirstContentMatchCheckBox.setVisibility(isChecked ? View.VISIBLE : View.INVISIBLE);
        });
        dialogLayout.addView(searchInContentCheckBox, margins);

        // Checkbox: Only first content match
        onlyFirstContentMatchCheckBox.setText(R.string.only_first_content_match);
        onlyFirstContentMatchCheckBox.setChecked(AppSettings.get().isOnlyFirstContentMatch());
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

    private static boolean setListViewHeightBasedOnItems(ListView listView, final int maxDisplayedItems) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            return false;
        }

        int numberOfItems = listAdapter.getCount();
        int totalItemsHeight = 0;
        final int maxShowedItems = Math.min(numberOfItems, maxDisplayedItems);
        // Get total height of items.
        for (int itemPos = 0; itemPos < maxShowedItems; itemPos++) {
            View item = listAdapter.getView(itemPos, null, listView);
            item.measure(0, 0);
            totalItemsHeight += item.getMeasuredHeight();
        }

        // Get total height of item dividers.
        int totalDividersHeight = listView.getDividerHeight() * (maxShowedItems - 1);

        // Set list height.
        int height = totalItemsHeight + totalDividersHeight;
        ViewGroup.LayoutParams params = new ListView.LayoutParams(ListView.LayoutParams.MATCH_PARENT, height);
        listView.setLayoutParams(params);
        listView.requestLayout();

        return true;
    }
}
