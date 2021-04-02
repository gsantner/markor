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

    public static class Options {
        private AppCompatEditText _searchEditText;
        private AlertDialog _dialog;

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


    public static void showFileSearchDialog(final Activity activity, final Options dialogOptions) {

        final AlertDialog dialog = Generation.generateDialog(activity, dialogOptions);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
        dialog.show();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }

        dialogOptions._dialog = dialog;
    }


    private static class Generation {
        private static AlertDialog generateDialog(final Activity activity, final Options dialogOptions) {
            AlertDialog.Builder dialogBuilder = generateDialogBuilder(activity, dialogOptions);

            final AlertDialog dialog = dialogBuilder.create();

            return dialog;
        }

        private static AlertDialog.Builder generateDialogBuilder(final Activity activity, final Options dialogOptions) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity, dialogOptions.isDarkDialog
                    ? R.style.Theme_AppCompat_Dialog
                    : R.style.Theme_AppCompat_Light_Dialog
            );

            if (!TextUtils.isEmpty(dialogOptions.messageText)) {
                dialogBuilder.setMessage(dialogOptions.messageText);
            }

            final LinearLayout dialogLayout = generateDialogLayout(activity, dialogOptions);
            dialogBuilder.setView(dialogLayout)
                    .setOnCancelListener(null)
                    .setNegativeButton(dialogOptions.cancelButtonText, (dialogInterface, i) -> dialogInterface.dismiss());

            if (dialogOptions.titleText != 0) {
                dialogBuilder.setTitle(dialogOptions.titleText);
            }

            dialogBuilder.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                dialogInterface.dismiss();
                if (dialogOptions.callback != null && !TextUtils.isEmpty(dialogOptions._searchEditText.getText().toString())) {
                    dialogOptions.callback.callback(dialogOptions._searchEditText.getText().toString());
                }
            });

            return dialogBuilder;
        }

        private static LinearLayout generateDialogLayout(final Activity activity, final Options dialogOptions) {
            final LinearLayout dialogLayout = new LinearLayout(activity);
            dialogLayout.setOrientation(LinearLayout.VERTICAL);

            OptionsMenuLayout.generate(activity, dialogOptions, dialogLayout);

            generateSearchEditText(activity, dialogOptions, dialogLayout);

            return dialogLayout;
        }

        private static void generateSearchEditText(final Activity activity, final Options dialogOptions, final LinearLayout dialogLayout) {
            final AppCompatEditText searchEditText = new AppCompatEditText(activity);
            searchEditText.setText(dialogOptions.defaultText);
            searchEditText.setSingleLine(true);
            searchEditText.setMaxLines(1);

            final int textColor = ContextCompat.getColor(activity, dialogOptions.isDarkDialog
                    ? R.color.dark__primary_text
                    : R.color.light__primary_text);
            searchEditText.setTextColor(textColor);
            searchEditText.setHintTextColor((textColor & 0x00FFFFFF) | 0x99000000);
            searchEditText.setHint(dialogOptions.searchHintText);
            searchEditText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            searchEditText.setOnKeyListener((keyView, keyCode, keyEvent) -> {
                if ((keyEvent.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    if (dialogOptions._dialog != null) {
                        dialogOptions._dialog.dismiss();
                    }
                    if (dialogOptions.callback != null && !TextUtils.isEmpty(searchEditText.getText().toString())) {
                        dialogOptions.callback.callback(searchEditText.getText().toString());
                    }
                    return true;
                }
                return false;
            });

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            int px = (int) (new ContextUtils(searchEditText.getContext()).convertDpToPx(8));
            lp.setMargins(px, px / 2, px, px / 2);


            dialogLayout.addView(searchEditText, lp);
            dialogOptions._searchEditText = searchEditText;
        }

        private static class OptionsMenuLayout {

            private static void generate(final Activity activity, final Options dialogOptions, final LinearLayout dialogLayout) {
                final LinearLayout menuLayout = new LinearLayout(activity);
                menuLayout.setOrientation(LinearLayout.HORIZONTAL);
                menuLayout.setGravity(Gravity.CENTER);

                generateMenuItems(menuLayout, activity, dialogOptions);

                dialogLayout.addView(menuLayout);
            }

            private static LinearLayout.LayoutParams generateLayoutParams() {
                final int leftMargin = 40;
                final int rightMargin = 40;
                final int topMargin = 40;
                final int bottomMargin = 40;
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                layoutParams.setMargins(leftMargin, topMargin, rightMargin, bottomMargin);

                return layoutParams;
            }

            private static void generateMenuItems(final LinearLayout menuLayout, final Activity activity, final Options dialogOptions) {
                LinearLayout.LayoutParams layoutParams = generateLayoutParams();

                ImageView imgRegex = generateRegexOption(activity, dialogOptions);
                ImageView imgCaseSensitive = generateCaseSensitivityOption(activity, dialogOptions);
                ImageView imgSearchInContent = generateSearchInContentOption(activity, dialogOptions);

                menuLayout.addView(imgRegex, layoutParams);
                menuLayout.addView(imgCaseSensitive, layoutParams);
                menuLayout.addView(imgSearchInContent, layoutParams);
            }


            private static ImageView generateRegexOption(final Activity activity, final Options dialogOptions) {

                ImageView imgRegex = new ImageView(activity);
                imgRegex.setImageResource(R.drawable.ic_regex_black_24dp);
                updateRegexOption(imgRegex, dialogOptions.searchConfigOptions.isRegexQuery, activity);

                imgRegex.setOnClickListener(v -> {
                    dialogOptions.searchConfigOptions.isRegexQuery = !dialogOptions.searchConfigOptions.isRegexQuery;
                    updateRegexOption(imgRegex, dialogOptions.searchConfigOptions.isRegexQuery, activity);
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


            private static ImageView generateCaseSensitivityOption(final Activity activity, final Options dialogOptions) {
                ImageView imgCaseSensitivity = new ImageView(activity);
                imgCaseSensitivity.setImageResource(R.drawable.ic_format_size_black_24dp);
                updateCaseSensitivityOption(imgCaseSensitivity, dialogOptions.searchConfigOptions.isCaseSensitiveQuery, activity);

                imgCaseSensitivity.setOnClickListener(v -> {
                    dialogOptions.searchConfigOptions.isCaseSensitiveQuery = !dialogOptions.searchConfigOptions.isCaseSensitiveQuery;
                    updateCaseSensitivityOption(imgCaseSensitivity, dialogOptions.searchConfigOptions.isCaseSensitiveQuery, activity);
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


            private static ImageView generateSearchInContentOption(final Activity activity, final Options dialogOptions) {
                ImageView imgSearchInContent = new ImageView(activity);
                imgSearchInContent.setImageResource(R.drawable.ic_baseline_plagiarism_24);
                updateSearchInContentOption(imgSearchInContent, dialogOptions.searchConfigOptions.isSearchInContent, activity, dialogOptions);

                imgSearchInContent.setOnClickListener(v -> {
                    dialogOptions.searchConfigOptions.isSearchInContent = !dialogOptions.searchConfigOptions.isSearchInContent;
                    updateSearchInContentOption(imgSearchInContent, dialogOptions.searchConfigOptions.isSearchInContent, activity, dialogOptions);
                });

                return imgSearchInContent;
            }

            private static void updateSearchInContentOption(final ImageView imgCaseSensitivity, final boolean isEnabled,
                                                            final Activity activity, Options dialogOptions) {
                updateIconActivityColor(imgCaseSensitivity, isEnabled, activity);

                final String hintText = activity.getString(isEnabled
                        ? R.string.search_in_content
                        : R.string.search);
                TooltipCompat.setTooltipText(imgCaseSensitivity, hintText);

                if (dialogOptions._dialog != null) {
                    dialogOptions._dialog.setTitle(hintText);
                    dialogOptions._searchEditText.setHint(hintText);
                }
            }

            private static void updateIconActivityColor(final ImageView image, final boolean isEnabled, final Activity activity) {
                final int color = ContextCompat.getColor(activity, isEnabled ? R.color.active_icon : R.color.inactive_icon);
                image.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
            }
        }
    }
}