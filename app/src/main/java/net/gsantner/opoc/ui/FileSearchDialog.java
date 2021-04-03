package net.gsantner.opoc.ui;

import android.app.Activity;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.TooltipCompat;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
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
            this.isDarkDialog = isDarkDialog;
        }

        public String messageText = "";
        public String defaultText = "";
        public boolean isDarkDialog;
        @StringRes
        public int cancelButtonText = android.R.string.cancel;
        @StringRes
        public int titleText = 0;
        @StringRes
        public int searchHintText = android.R.string.search_go;

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

            Initializer.init(this, activity, dialogOptions);
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

        private static void init(final Initializer initializer, final Activity activity, final Options dialogOptions) {
            Components.initDialog(initializer);
        }


        private static class Components {

            private static void initDialog(final Initializer initializer) {
                AlertDialog.Builder dialogBuilder = initDialogBuilder(initializer);
                initializer._dialog = dialogBuilder.create();
            }


            private static AlertDialog.Builder initDialogBuilder(final Initializer initializer) {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(initializer._activity, initializer._dialogOptions.isDarkDialog
                        ? R.style.Theme_AppCompat_Dialog
                        : R.style.Theme_AppCompat_Light_Dialog
                );

                if (!TextUtils.isEmpty(initializer._dialogOptions.messageText)) {
                    dialogBuilder.setMessage(initializer._dialogOptions.messageText);
                }

                initializer._dialogLayout = initDialogLayout(initializer);
                dialogBuilder.setView(initializer._dialogLayout)
                        .setOnCancelListener(null)
                        .setNegativeButton(initializer._dialogOptions.cancelButtonText, (dialogInterface, i) -> dialogInterface.dismiss());

                if (initializer._dialogOptions.titleText != 0) {
                    dialogBuilder.setTitle(initializer._dialogOptions.titleText);
                }

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

                initializer._menuLayout = OptionsMenuLayout.init(initializer, dialogLayout);

                initializer._searchEditText = initSearchEditText(initializer, dialogLayout);

                return dialogLayout;
            }


            private static AppCompatEditText initSearchEditText(final Initializer initializer, final LinearLayout dialogLayout) {
                final AppCompatEditText searchEditText = new AppCompatEditText(initializer._activity);

                searchEditText.setText(initializer._dialogOptions.defaultText);
                searchEditText.setSingleLine(true);
                searchEditText.setMaxLines(1);

                final int textColor = ContextCompat.getColor(initializer._activity, initializer._dialogOptions.isDarkDialog
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
                    menuLayout.setOrientation(LinearLayout.HORIZONTAL);
                    menuLayout.setGravity(Gravity.CENTER);

                    initMenuItems(initializer, menuLayout);

                    dialogLayout.addView(menuLayout);

                    return menuLayout;
                }


                private static LinearLayout.LayoutParams getLayoutParams() {
                    final int leftMargin = 40;
                    final int rightMargin = 40;
                    final int topMargin = 40;
                    final int bottomMargin = 40;
                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    layoutParams.setMargins(leftMargin, topMargin, rightMargin, bottomMargin);

                    return layoutParams;
                }


                private static void initMenuItems(final Initializer initializer, final LinearLayout menuLayout) {
                    LinearLayout.LayoutParams layoutParams = getLayoutParams();

                    ImageView imgRegex = initRegexOption(initializer);
                    ImageView imgCaseSensitive = initCaseSensitivityOption(initializer);
                    ImageView imgSearchInContent = initSearchInContentOption(initializer);

                    menuLayout.addView(imgRegex, layoutParams);
                    menuLayout.addView(imgCaseSensitive, layoutParams);
                    menuLayout.addView(imgSearchInContent, layoutParams);
                }


                private static ImageView initRegexOption(final Initializer initializer) {
                    final Options.SearchConfigOptions searchConfig = initializer._dialogOptions.searchConfigOptions;

                    ImageView imgRegex = new ImageView(initializer._activity);
                    imgRegex.setImageResource(R.drawable.ic_regex_black_24dp);
                    updateRegexOption(imgRegex, searchConfig.isRegexQuery, initializer._activity);

                    imgRegex.setOnClickListener(v -> {
                        searchConfig.isRegexQuery = !searchConfig.isRegexQuery;
                        updateRegexOption(imgRegex, searchConfig.isRegexQuery, initializer._activity);
                    });

                    return imgRegex;
                }

                private static void updateRegexOption(final ImageView imgRegex, final boolean isEnabled, final Activity activity) {
                    updateIconActivityColor(imgRegex, isEnabled, activity);

                    final String hintTextID = activity.getString(isEnabled
                            ? R.string.regex_enabled
                            : R.string.regex_disabled);
                    TooltipCompat.setTooltipText(imgRegex, hintTextID);

                    AppSettings appSettings = new AppSettings(activity);
                    appSettings.setSearchQueryRegexUsing(isEnabled);
                }


                private static ImageView initCaseSensitivityOption(final Initializer initializer) {
                    final Options.SearchConfigOptions searchConfig = initializer._dialogOptions.searchConfigOptions;

                    ImageView imgCaseSensitivity = new ImageView(initializer._activity);
                    imgCaseSensitivity.setImageResource(R.drawable.ic_format_size_black_24dp);
                    updateCaseSensitivityOption(imgCaseSensitivity, searchConfig.isCaseSensitiveQuery, initializer._activity);

                    imgCaseSensitivity.setOnClickListener(v -> {
                        searchConfig.isCaseSensitiveQuery = !searchConfig.isCaseSensitiveQuery;
                        updateCaseSensitivityOption(imgCaseSensitivity, searchConfig.isCaseSensitiveQuery, initializer._activity);
                    });

                    return imgCaseSensitivity;
                }

                private static void updateCaseSensitivityOption(final ImageView imgCaseSensitivity, final boolean isEnabled, final Activity activity) {
                    updateIconActivityColor(imgCaseSensitivity, isEnabled, activity);

                    final String hintText = activity.getString(isEnabled
                            ? R.string.case_sensitivity_enabled
                            : R.string.case_sensitivity_disabled);
                    TooltipCompat.setTooltipText(imgCaseSensitivity, hintText);

                    AppSettings appSettings = new AppSettings(activity);
                    appSettings.setSearchQueryCaseSensitivity(isEnabled);
                }


                private static ImageView initSearchInContentOption(final Initializer initializer) {
                    final Options.SearchConfigOptions searchConfig = initializer._dialogOptions.searchConfigOptions;

                    ImageView imgSearchInContent = new ImageView(initializer._activity);
                    imgSearchInContent.setImageResource(R.drawable.ic_baseline_plagiarism_24);
                    updateSearchInContentOption(imgSearchInContent, searchConfig.isSearchInContent, initializer);

                    imgSearchInContent.setOnClickListener(v -> {
                        searchConfig.isSearchInContent = !searchConfig.isSearchInContent;
                        updateSearchInContentOption(imgSearchInContent, searchConfig.isSearchInContent, initializer);
                    });

                    return imgSearchInContent;
                }

                private static void updateSearchInContentOption(final ImageView imgCaseSensitivity, final boolean isEnabled, final Initializer initializer) {
                    updateIconActivityColor(imgCaseSensitivity, isEnabled, initializer._activity);

                    final String hintText = initializer._activity.getString(isEnabled
                            ? R.string.search_in_content
                            : R.string.search);
                    TooltipCompat.setTooltipText(imgCaseSensitivity, hintText);

                    if (initializer._dialog != null) {
                        initializer._dialog.setTitle(hintText);
                        initializer._searchEditText.setHint(hintText);
                    }
                }


                private static void updateIconActivityColor(final ImageView image, final boolean isEnabled, final Activity activity) {
                    final int color = ContextCompat.getColor(activity, isEnabled ? R.color.active_icon : R.color.inactive_icon);
                    image.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
                }
            }
        }

    }
}