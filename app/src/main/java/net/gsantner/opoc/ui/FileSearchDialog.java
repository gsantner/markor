package net.gsantner.opoc.ui;

import android.app.Activity;
import android.support.annotation.StringRes;
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
        public SearchConfigOptions searchConfigOptions;
        public Callback.a1<String> callback;


        public Options(final boolean isDarkDialog) {
            searchConfigOptions = new SearchConfigOptions();
            _isDarkDialog = isDarkDialog;
        }

        public String messageText = "";
        public String defaultText = "";
        public boolean _isDarkDialog;
        @StringRes
        public int cancelButtonText = android.R.string.cancel;
        @StringRes
        public int titleText = R.string.search;
        @StringRes
        public int searchHintText = R.string.empty_string;

        public static class SearchConfigOptions {
            public boolean isRegexQuery;
            public boolean isCaseSensitiveQuery;
            public boolean isSearchInContent;
        }
    }


    private static class Initializer {
        final private Activity _activity;
        final private Options _dialogOptions;

        private Window _window;
        private AlertDialog _dialog;
        private LinearLayout _dialogLayout;
        private LinearLayout _menuLayout;
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
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(initializer._activity, initializer._dialogOptions._isDarkDialog
                        ? R.style.Theme_AppCompat_Dialog
                        : R.style.Theme_AppCompat_Light_Dialog
                );

                if (!TextUtils.isEmpty(initializer._dialogOptions.messageText)) {
                    dialogBuilder.setMessage(initializer._dialogOptions.messageText);
                }
                dialogBuilder.setTitle(initializer._dialogOptions.titleText);
                dialogBuilder.setIcon(R.drawable.ic_search_black_24dp);

                initializer._dialogLayout = initDialogLayout(initializer);
                dialogBuilder.setView(initializer._dialogLayout)
                        .setOnCancelListener(null)
                        .setNegativeButton(initializer._dialogOptions.cancelButtonText, (dialogInterface, i) -> dialogInterface.dismiss());

                dialogBuilder.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    if (initializer._dialogOptions.callback != null && !TextUtils.isEmpty(initializer._searchEditText.getText().toString())) {
                        initializer._dialogOptions.callback.callback(initializer._searchEditText.getText().toString());
                    }
                });

                return dialogBuilder;
            }


            private static LinearLayout initDialogLayout(final Initializer initializer) {
                final LinearLayout dialogLayout = new LinearLayout(initializer._activity);
                dialogLayout.setOrientation(LinearLayout.VERTICAL);


                initializer._searchEditText = initSearchEditText(initializer, dialogLayout);

                initializer._menuLayout = OptionsMenuLayout.init(initializer, dialogLayout);

                return dialogLayout;
            }


            private static AppCompatEditText initSearchEditText(final Initializer initializer, final LinearLayout dialogLayout) {
                final AppCompatEditText searchEditText = new AppCompatEditText(initializer._activity);

                searchEditText.setText(initializer._dialogOptions.defaultText);
                searchEditText.setSingleLine(true);
                searchEditText.setMaxLines(1);

                final int textColor = ContextCompat.getColor(initializer._activity, initializer._dialogOptions._isDarkDialog
                        ? R.color.dark__primary_text
                        : R.color.light__primary_text);
                searchEditText.setTextColor(textColor);
                searchEditText.setHintTextColor((textColor & 0x00FFFFFF) | 0x99000000);
                searchEditText.setHint(initializer._dialogOptions.searchHintText);
                searchEditText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                searchEditText.setOnKeyListener((keyView, keyCode, keyEvent) -> {
                    if ((keyEvent.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                        if (initializer._dialog != null) {
                            initializer._dialog.dismiss();
                        }
                        if (initializer._dialogOptions.callback != null && !TextUtils.isEmpty(searchEditText.getText().toString())) {
                            initializer._dialogOptions.callback.callback(searchEditText.getText().toString());
                        }
                        return true;
                    }
                    return false;
                });

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                int px = (int) (new ContextUtils(searchEditText.getContext()).convertDpToPx(8));
                lp.setMargins(px, px / 2, px, px / 2);


                dialogLayout.addView(searchEditText, lp);
                return searchEditText;
            }


            private static class OptionsMenuLayout {

                private static LinearLayout init(final Initializer initializer, final LinearLayout dialogLayout) {
                    final LinearLayout menuLayout = new LinearLayout(initializer._activity);
                    menuLayout.setOrientation(LinearLayout.VERTICAL);

                    initMenuItems(initializer, menuLayout);

                    dialogLayout.addView(menuLayout);

                    return menuLayout;
                }


                private static LinearLayout.LayoutParams getLayoutParams() {
                    final int leftMargin = 40;
                    final int rightMargin = 40;
                    final int topMargin = 10;
                    final int bottomMargin = 10;
                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    layoutParams.setMargins(leftMargin, topMargin, rightMargin, bottomMargin);

                    return layoutParams;
                }


                private static void initMenuItems(final Initializer initializer, final LinearLayout menuLayout) {
                    LinearLayout.LayoutParams layoutParams = getLayoutParams();

                    CheckBox regexCheckBox = initRegexOption(initializer);
                    CheckBox caseSensitivityCheckBox = initCaseSensitivityOption(initializer);
                    CheckBox searchInContentCheckBox = initSearchInContentOption(initializer);

                    menuLayout.addView(caseSensitivityCheckBox, layoutParams);
                    menuLayout.addView(searchInContentCheckBox, layoutParams);
                    menuLayout.addView(regexCheckBox, layoutParams);
                }


                private static CheckBox initRegexOption(final Initializer initializer) {
                    final Options.SearchConfigOptions searchConfig = initializer._dialogOptions.searchConfigOptions;

                    CheckBox regexCheckBox = new CheckBox(initializer._activity);
                    regexCheckBox.setText(R.string.regex_search);
                    regexCheckBox.setChecked(searchConfig.isRegexQuery);

                    updateRegexOption(regexCheckBox, searchConfig.isRegexQuery, initializer._activity);

                    regexCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        searchConfig.isRegexQuery = isChecked;
                        updateRegexOption(regexCheckBox, isChecked, initializer._activity);
                    });

                    return regexCheckBox;
                }

                private static void updateRegexOption(final CheckBox regexCheckBox, final boolean isEnabled, final Activity activity) {
                    AppSettings appSettings = new AppSettings(activity);
                    appSettings.setSearchQueryRegexUsing(isEnabled);
                }


                private static CheckBox initCaseSensitivityOption(final Initializer initializer) {
                    final Options.SearchConfigOptions searchConfig = initializer._dialogOptions.searchConfigOptions;

                    CheckBox caseSensitivityCheckBox = new CheckBox(initializer._activity);
                    caseSensitivityCheckBox.setText(R.string.case_sensitive);
                    caseSensitivityCheckBox.setChecked(searchConfig.isCaseSensitiveQuery);

                    updateCaseSensitivityOption(caseSensitivityCheckBox, searchConfig.isCaseSensitiveQuery, initializer._activity);

                    caseSensitivityCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        searchConfig.isCaseSensitiveQuery = isChecked;
                        updateCaseSensitivityOption(caseSensitivityCheckBox, isChecked, initializer._activity);
                    });

                    return caseSensitivityCheckBox;
                }

                private static void updateCaseSensitivityOption(final CheckBox caseSensitivityCheckBox, final boolean isEnabled, final Activity activity) {
                    AppSettings appSettings = new AppSettings(activity);
                    appSettings.setSearchQueryCaseSensitivity(isEnabled);
                }


                private static CheckBox initSearchInContentOption(final Initializer initializer) {
                    final Options.SearchConfigOptions searchConfig = initializer._dialogOptions.searchConfigOptions;

                    CheckBox searchInContentCheckBox = new CheckBox(initializer._activity);
                    searchInContentCheckBox.setText(R.string.search_in_content);
                    searchInContentCheckBox.setChecked(searchConfig.isSearchInContent);

                    updateSearchInContentOption(searchInContentCheckBox, searchConfig.isSearchInContent, initializer._activity);

                    searchInContentCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        searchConfig.isSearchInContent = isChecked;
                        updateSearchInContentOption(searchInContentCheckBox, isChecked, initializer._activity);
                    });

                    return searchInContentCheckBox;
                }

                private static void updateSearchInContentOption(final CheckBox searchInContentCheckBox, final boolean isEnabled, final Activity activity) {
                    AppSettings appSettings = new AppSettings(activity);
                    appSettings.setSearchInContent(isEnabled);
                }
            }
        }
    }
}
