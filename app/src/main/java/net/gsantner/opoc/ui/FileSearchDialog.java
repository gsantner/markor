package net.gsantner.opoc.ui;

import android.app.Activity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.text.InputType;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import net.gsantner.markor.R;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.opoc.util.Callback;
import net.gsantner.opoc.util.ContextUtils;

public class FileSearchDialog {

    public static void showFileSearchDialog(final Activity activity, final Options dialogOptions) {
        final Initializer initializer = new Initializer(activity, dialogOptions);

        initializer.showDialog();
    }


    public static class Options {
        public Callback.a4<String, Boolean, Boolean, Boolean> callback;

        public Options(final boolean isDarkDialog) {
            _isDarkDialog = isDarkDialog;
        }

        public boolean _isDarkDialog;
    }


    private static class Initializer {
        final private Activity _activity;
        final private Options _dialogOptions;

        private Window _window;
        private AlertDialog _dialog;
        private AppCompatEditText _searchEditText;

        private Initializer(final Activity activity, final Options dialogOptions) {
            _activity = activity;
            _dialogOptions = dialogOptions;

            Initializer.init(this);
        }

        private void showDialog() {
            _window = _dialog.getWindow();
            if (_window != null) {
                _window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            }
            _dialog.show();
            if (_window != null) {
                _window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            }
        }

        private static void init(final Initializer initializer) {
            Components.initDialog(initializer);
        }

        private static Initializer init(final Activity activity, final Options dialogOptions) {
            return new Initializer(activity, dialogOptions);
        }


        private static class Components {

            private static void initDialog(final Initializer initializer) {
                AlertDialog.Builder dialogBuilder = initDialogBuilder(initializer);
                initializer._dialog = dialogBuilder.create();
            }


            private static AlertDialog.Builder initDialogBuilder(final Initializer initializer) {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(initializer._activity, initializer._dialogOptions._isDarkDialog ? R.style.Theme_AppCompat_Dialog : R.style.Theme_AppCompat_Light_Dialog);

                dialogBuilder.setMessage(R.string.recursive_search_in_current_directory);
                dialogBuilder.setTitle(R.string.search);

                LinearLayout dialogContentView = initDialogLayout(initializer);
                dialogBuilder.setView(dialogContentView)
                        .setOnCancelListener(null)
                        .setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> dialogInterface.dismiss());

                dialogBuilder.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    if (initializer._dialogOptions.callback != null && !TextUtils.isEmpty(initializer._searchEditText.getText().toString())) {
                        initializer._dialogOptions.callback.callback(initializer._searchEditText.getText().toString(), AppSettings.get().isSearchQueryUseRegex(), AppSettings.get().isSearchQueryCaseSensitive(), AppSettings.get().isSearchInContent());
                    }
                });

                return dialogBuilder;
            }


            @SuppressWarnings("ConstantConditions")
            private static LinearLayout initDialogLayout(final Initializer initializer) {
                final LinearLayout dialogLayout = new LinearLayout(initializer._activity);
                dialogLayout.setOrientation(LinearLayout.VERTICAL);
                final int dp4px = (int) (new ContextUtils(dialogLayout.getContext()).convertDpToPx(4));
                final int textColor = ContextCompat.getColor(initializer._activity, initializer._dialogOptions._isDarkDialog ? R.color.dark__primary_text : R.color.light__primary_text);

                final LinearLayout.LayoutParams margins = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                margins.setMargins(dp4px * 5, dp4px, dp4px * 5, dp4px);

                final AppCompatEditText searchEditText = new AppCompatEditText(initializer._activity);
                final CheckBox regexCheckBox = new CheckBox(initializer._activity);
                final CheckBox caseSensitivityCheckBox = new CheckBox(initializer._activity);
                final CheckBox searchInContentCheckBox = new CheckBox(initializer._activity);

                final Callback.a0 startSearch = () -> {
                    final String q = searchEditText.getText().toString();
                    if (initializer._dialogOptions.callback != null && !TextUtils.isEmpty(q)) {
                        initializer._dialogOptions.callback.callback(q, regexCheckBox.isChecked(), caseSensitivityCheckBox.isChecked(), searchInContentCheckBox.isChecked());
                    }
                };

                // EdiText: Search query input
                searchEditText.setHint(R.string.search);
                searchEditText.setSingleLine(true);
                searchEditText.setMaxLines(1);
                searchEditText.setTextColor(textColor);
                searchEditText.setHintTextColor((textColor & 0x00FFFFFF) | 0x99000000);
                searchEditText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                searchEditText.setOnKeyListener((keyView, keyCode, keyEvent) -> {
                    if ((keyEvent.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                        if (initializer._dialog != null) {
                            initializer._dialog.dismiss();
                        }
                        startSearch.callback();
                        return true;
                    }
                    return false;
                });


                dialogLayout.addView(searchEditText, margins);
                initializer._searchEditText = searchEditText;

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
                dialogLayout.addView(searchInContentCheckBox, margins);

                return dialogLayout;
            }
        }
    }
}
