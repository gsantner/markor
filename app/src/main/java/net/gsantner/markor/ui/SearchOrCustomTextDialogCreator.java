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

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.WindowManager;

import net.gsantner.markor.R;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.opoc.format.todotxt.SttTask;
import net.gsantner.opoc.ui.SearchOrCustomTextDialog;
import net.gsantner.opoc.util.Callback;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SearchOrCustomTextDialogCreator {
    private static boolean isTodoTxtAlternativeNaming(Context context) {
        return new AppSettings(context).isTodoTxtAlternativeNaming();
    }

    public static void showSpecialKeyDialog(Activity activity, Callback.a1<String> callback) {
        SearchOrCustomTextDialog.DialogOptions dopt = new SearchOrCustomTextDialog.DialogOptions();
        baseConf(activity, dopt);
        dopt.callback = callback;
        String[] actions = activity.getResources().getStringArray(R.array.textactions_press_key__text);
        dopt.data = new ArrayList<>(Arrays.asList(actions));

        dopt.dialogHeightDp = 530;
        dopt.titleText = R.string.special_key;
        dopt.isSearchEnabled = false;
        SearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    public static void showAttachSomethingDialog(final Activity activity, final Callback.a1<Integer> userCallback) {
        final List<String> availableData = new ArrayList<>();
        final List<Integer> availableDataToActionMap = new ArrayList<>();
        final List<Integer> availableDataToIconMap = new ArrayList<>();
        final Callback.a3<Integer, Integer, Integer> addToList = (strRes, actionRes, iconRes) -> {
            availableData.add(activity.getString(strRes));
            availableDataToActionMap.add(actionRes);
            availableDataToIconMap.add(iconRes);
        };
        addToList.callback(R.string.color, R.id.action_attach_color, R.drawable.ic_format_color_fill_black_24dp);
        addToList.callback(R.string.insert_link, R.id.action_attach_link, R.drawable.ic_link_black_24dp);
        addToList.callback(R.string.file, R.id.action_attach_file, R.drawable.ic_attach_file_black_24dp);
        addToList.callback(R.string.image, R.id.action_attach_image, R.drawable.ic_image_black_24dp);
        addToList.callback(R.string.audio, R.id.action_attach_audio, R.drawable.ic_keyboard_voice_black_24dp);
        addToList.callback(R.string.date, R.id.action_attach_date, R.drawable.ic_access_time_black_24dp);


        SearchOrCustomTextDialog.DialogOptions dopt = new SearchOrCustomTextDialog.DialogOptions();
        baseConf(activity, dopt);
        dopt.callback = str -> userCallback.callback(availableDataToActionMap.get(availableData.indexOf(str)));
        dopt.data = availableData;
        dopt.iconsForData = availableDataToIconMap;
        dopt.isSearchEnabled = false;
        dopt.titleText = 0;
        dopt.dialogWidthDp = WindowManager.LayoutParams.WRAP_CONTENT;
        dopt.gravity = Gravity.BOTTOM | Gravity.END;
        SearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    public static void showInsertTableRowDialog(final Activity activity, final boolean isHeader, Callback.a2<Integer, Boolean> callback) {
        final SearchOrCustomTextDialog.DialogOptions dopt = new SearchOrCustomTextDialog.DialogOptions();
        final AppSettings as = new AppSettings(activity.getApplicationContext());
        final String PREF_LAST_USED_TABLE_SIZE = "pref_key_last_used_table_size";
        final int lastUsedTableSize = as.getInt(PREF_LAST_USED_TABLE_SIZE, 3);
        final List<String> availableData = new ArrayList<>();
        for (int i = 2; i <= 5; i++) {
            availableData.add(Integer.toString(i));
        }

        baseConf(activity, dopt);
        dopt.titleText = R.string.table;
        dopt.messageText = activity.getString(R.string.how_much_columns_press_table_button_long_to_start_table);
        dopt.messageText += activity.getString(R.string.example_of_a_markdown_table) + ":\n\n";
        dopt.messageText += "| id | name | info |\n|-----|-----------|--------|\n| 1  | John   | text |\n| 2  | Anna   | text |\n";

        dopt.callback = colsStr -> {
            as.setInt(PREF_LAST_USED_TABLE_SIZE, Integer.parseInt(colsStr));
            callback.callback(Integer.parseInt(colsStr), isHeader);
        };
        dopt.data = availableData;
        dopt.searchInputType = InputType.TYPE_CLASS_NUMBER;
        dopt.highlightData = Collections.singletonList(Integer.toString(lastUsedTableSize));
        dopt.searchHintText = R.string.search_or_custom;
        SearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    public static void showSearchFilesDialog(Activity activity, File searchDir, Callback.a1<String> callback) {
        SearchOrCustomTextDialog.DialogOptions dopt = new SearchOrCustomTextDialog.DialogOptions();
        baseConf(activity, dopt);
        dopt.callback = query -> SearchOrCustomTextDialog.recursiveFileSearch(activity, searchDir, query, (Callback.a1<List<String>>) searchResults -> {
            dopt.callback = callback;
            dopt.isSearchEnabled = false;
            dopt.data = searchResults;
            dopt.cancelButtonText = R.string.close;
            dopt.titleText = R.string.select;
            dopt.messageText = null;
            if (dopt.data.isEmpty()) {
                dopt.messageText = "     ¯\\_(ツ)_/¯     ";
            }
            SearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
        });
        dopt.titleText = R.string.search;
        dopt.isSearchEnabled = true;
        dopt.messageText = activity.getString(R.string.recursive_search_in_current_directory);
        dopt.searchHintText = R.string.search;
        SearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }


    public static void showRecentDocumentsDialog(Activity activity, Callback.a1<String> callback) {
        SearchOrCustomTextDialog.DialogOptions dopt = new SearchOrCustomTextDialog.DialogOptions();
        baseConf(activity, dopt);
        dopt.callback = callback;

        final StyleSpan boldSpan = new StyleSpan(android.graphics.Typeface.BOLD);
        final RelativeSizeSpan sizeSpan = new RelativeSizeSpan(0.75f);
        ArrayList<Spannable> spannables = new ArrayList<>();

        AppSettings appSettings = new AppSettings(activity.getApplicationContext());
        String tmps;
        for (String document : appSettings.getRecentDocuments()) {
            final SpannableStringBuilder sb = new SpannableStringBuilder(document);
            try {
                sb.setSpan(boldSpan, document.lastIndexOf("/"), document.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                if (document.startsWith((tmps = appSettings.getNotebookDirectoryAsStr()))) {
                    sb.setSpan(sizeSpan, 0, tmps.length() + 1, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                } else if (document.startsWith((tmps = Environment.getExternalStorageDirectory().toString()))) {
                    sb.setSpan(sizeSpan, 0, tmps.length() + 1, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                }
            } catch (Exception ignored) {
                // setSpan may throw error
            }
            spannables.add(sb);
        }
        dopt.data = spannables;

        dopt.titleText = R.string.recently_viewed_documents;
        dopt.isSearchEnabled = false;
        SearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    public static void showSttArchiveDialog(Activity activity, Callback.a1<String> callback) {
        SearchOrCustomTextDialog.DialogOptions dopt = new SearchOrCustomTextDialog.DialogOptions();
        baseConf(activity, dopt);
        dopt.callback = callback;
        List<String> highlightedData = new ArrayList<>();
        List<String> availableData = new ArrayList<>();
        availableData.add("todo.archive.txt");
        availableData.add("todo.done.txt");
        availableData.add("archive.txt");
        availableData.add("done.txt");
        availableData.add(".todo.archive.txt");
        String hl = new AppSettings(activity).getLastTodoUsedArchiveFilename();
        if (!TextUtils.isEmpty(hl)) {
            highlightedData.add(hl);
            if (!availableData.contains(hl)) {
                availableData.add(hl);
            }
        }

        dopt.data = availableData;
        dopt.highlightData = highlightedData;
        dopt.titleText = R.string.archive;
        dopt.searchHintText = R.string.search_or_custom;
        dopt.messageText = activity.getString(R.string.archive_does_move_done_tasks);
        SearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    public static void showSttSortDialogue(Activity activity, final Callback.a2<String, Boolean> callback) {
        SearchOrCustomTextDialog.DialogOptions dopt = new SearchOrCustomTextDialog.DialogOptions();
        baseConf(activity, dopt);
        final List<String> availableData = new ArrayList<>();
        final List<Integer> availableDataToIconMap = new ArrayList<>();

        AppSettings appSettings = new AppSettings(activity.getApplicationContext());
        String o_context = activity.getString(appSettings.isTodoTxtAlternativeNaming() ? R.string.category : R.string.context);
        String o_project = activity.getString(appSettings.isTodoTxtAlternativeNaming() ? R.string.tag : R.string.project);
        String o_prio = activity.getString(R.string.priority);
        String o_date = activity.getString(R.string.date);
        String o_textline = activity.getString(R.string.text_lines);
        String o_description = activity.getString(R.string.description);
        String o_duedate = activity.getString(R.string.due_date);
        String d_asc = " (" + activity.getString(R.string.ascending) + ")";
        String d_desc = " (" + activity.getString(R.string.descending) + ")";
        String optLastSelected = "showSttSortDialogue.last_selected";

        dopt.callback = arg1 -> {
            appSettings.setString(optLastSelected, arg1);
            String[] values = arg1.replace(o_context, "context").replace(o_project, "project").replace(o_prio, "priority")
                    .replace(o_date, "date").replace(o_textline, "line").replace(o_description, "description").replace(o_duedate, "duedate")
                    .split(" ");
            callback.callback(values[0], values[1].contains(d_desc.replace(" ", "")));
        };

        final Callback.a2<String, Integer> addToList = (o_by, iconRes) -> {
            availableData.add(o_by + d_asc);
            availableData.add(o_by + d_desc);
            availableDataToIconMap.add(iconRes);
            availableDataToIconMap.add(iconRes);
        };
        addToList.callback(o_prio, R.drawable.ic_star_border_black_24dp);
        addToList.callback(o_project, R.drawable.ic_local_offer_black_24dp);
        addToList.callback(o_context, R.drawable.gs_email_sign_black_24dp);
        addToList.callback(o_date, R.drawable.ic_date_range_black_24dp);
        addToList.callback(o_duedate, R.drawable.ic_date_range_black_24dp);
        addToList.callback(o_description, R.drawable.ic_text_fields_black_24dp);
        addToList.callback(o_textline, R.drawable.ic_text_fields_black_24dp);

        dopt.data = availableData;
        dopt.highlightData = Collections.singletonList(appSettings.getString(optLastSelected, o_context + d_desc));
        dopt.iconsForData = availableDataToIconMap;
        dopt.dialogWidthDp = WindowManager.LayoutParams.WRAP_CONTENT;
        dopt.dialogHeightDp = 530;
        dopt.gravity = Gravity.BOTTOM | Gravity.END;

        dopt.titleText = R.string.sort_tasks_by_selected_order;
        dopt.messageText = "";
        dopt.searchHintText = R.string.search_or_custom;
        dopt.isSearchEnabled = false;
        SearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    public static void showSttContextDialog(Activity activity, List<String> availableData, List<String> highlightedData, Callback.a1<String> callback) {
        SearchOrCustomTextDialog.DialogOptions dopt = new SearchOrCustomTextDialog.DialogOptions();
        sortUniqNonEmpty(availableData, "home", "shop");
        baseConf(activity, dopt);
        dopt.callback = callback;
        dopt.data = availableData;
        dopt.highlightData = highlightedData;
        dopt.titleText = isTodoTxtAlternativeNaming(activity) ? R.string.category : R.string.context;
        dopt.searchHintText = R.string.search_or_custom;
        //dopt.messageText = activity.getString(R.string.add_x_or_browse_existing_ones_witharg, activity.getString(R.string.context));
        SearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    public static void showSttContextListDialog(Activity activity, List<String> availableData, List<String> highlightedData, String fullText, Callback.a1<String> userCallback) {
        showSttContextDialog(activity, availableData, highlightedData, callbackValue -> {
            SearchOrCustomTextDialog.DialogOptions dopt = new SearchOrCustomTextDialog.DialogOptions();
            baseConf(activity, dopt);
            dopt.callback = userCallback;
            dopt.data = filterContains(new ArrayList<>(Arrays.asList(fullText.split("\n"))), callbackValue);
            dopt.highlightData = highlightedData;
            dopt.titleText = isTodoTxtAlternativeNaming(activity) ? R.string.category : R.string.context;
            dopt.searchHintText = R.string.search;
            SearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
        });
    }

    private static List<String> filterContains(List<String> values, String text) {
        for (int i = 0; i < values.size(); i++) {
            if (!values.get(i).contains(text)) {
                values.remove(i);
                i--;
            }
        }
        return values;
    }

    private static List<String> filterEmpty(List<String> data) {
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).trim().isEmpty()) {
                data.remove(i);
                i--;

            }
        }
        return data;
    }

    /**
     * Allow to choose between Hexcolor / foreground / background color, pass back stringid
     */
    public static void showColorSelectionModeDialog(Activity activity, Callback.a1<Integer> callback) {
        SearchOrCustomTextDialog.DialogOptions dopt = new SearchOrCustomTextDialog.DialogOptions();

        final String hexcode = activity.getString(R.string.hexcode);
        final String fg = activity.getString(R.string.foreground);
        final String bg = activity.getString(R.string.background);

        baseConf(activity, dopt);
        dopt.callback = arg1 -> {
            int id = R.string.hexcode;
            if (fg.equals(arg1)) {
                id = R.string.foreground;
            } else if (bg.equals(arg1)) {
                id = R.string.background;
            }
            callback.callback(id);
        };

        dopt.data = new ArrayList<>(Arrays.asList(hexcode, fg, bg));
        dopt.titleText = R.string.color;
        dopt.isSearchEnabled = false;
        dopt.messageText = activity.getString(R.string.set_foreground_or_background_color_hexcolor_also_possible);
        SearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    public static void showSttProjectDialog(Activity activity, List<String> availableData, List<String> highlightedData, Callback.a1<String> callback) {
        SearchOrCustomTextDialog.DialogOptions dopt = new SearchOrCustomTextDialog.DialogOptions();
        sortUniqNonEmpty(availableData, "music", "video");
        baseConf(activity, dopt);
        dopt.callback = callback;
        dopt.data = availableData;
        dopt.highlightData = highlightedData;
        dopt.titleText = isTodoTxtAlternativeNaming(activity) ? R.string.tag : R.string.project;
        dopt.searchHintText = R.string.search_or_custom;
        //dopt.messageText = activity.getString(R.string.add_x_or_browse_existing_ones_witharg, activity.getString(R.string.project));
        SearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }


    public static void showSttProjectListDialog(Activity activity, List<String> availableData, List<String> highlightedData, String fullText, Callback.a1<String> userCallback) {
        showSttProjectDialog(activity, availableData, highlightedData, callbackValue -> {
            SearchOrCustomTextDialog.DialogOptions dopt = new SearchOrCustomTextDialog.DialogOptions();
            baseConf(activity, dopt);
            dopt.callback = userCallback;
            dopt.data = filterContains(new ArrayList<>(Arrays.asList(fullText.split("\n"))), callbackValue);
            dopt.highlightData = highlightedData;
            dopt.titleText = isTodoTxtAlternativeNaming(activity) ? R.string.tag : R.string.project;
            dopt.searchHintText = R.string.search;
            SearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
        });
    }


    public static void showSearchDialog(Activity activity, String fullText, Callback.a1<String> userCallback) {
        /*SearchOrCustomTextDialog.DialogOptions dopt = new SearchOrCustomTextDialog.DialogOptions();
        baseConf(activity, dopt);
        dopt.callback = callbackValue -> {*/
        SearchOrCustomTextDialog.DialogOptions dopt2 = new SearchOrCustomTextDialog.DialogOptions();
        baseConf(activity, dopt2);
        dopt2.callback = userCallback;
        dopt2.data = filterEmpty(new ArrayList<>(Arrays.asList(fullText.split("\n"))));
        dopt2.titleText = R.string.search_documents;
        dopt2.searchHintText = R.string.search;
        SearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt2);
        /*};*/
        /*dopt.titleText = R.string.search_documents;
        dopt.searchHintText = R.string.search;
        SearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);*/
    }

    public static void showPriorityDialog(Activity activity, char selectedPriority, Callback.a1<String> callback) {
        SearchOrCustomTextDialog.DialogOptions dopt = new SearchOrCustomTextDialog.DialogOptions();
        baseConf(activity, dopt);
        dopt.callback = callback;

        List<String> availableData = new ArrayList<>();
        List<String> highlightedData = new ArrayList<>();
        String none = activity.getString(R.string.none);
        availableData.add(none);
        for (int i = ((int) 'A'); i <= ((int) 'Z'); i++) {
            availableData.add(Character.toString((char) i));
        }
        highlightedData.add(none);
        if (selectedPriority != SttTask.PRIORITY_NONE) {
            highlightedData.add(Character.toString(selectedPriority));
        }

        final List<Integer> dataIcons = new ArrayList<>();
        dataIcons.add(R.drawable.ic_delete_black_24dp);
        for (int i = 0; i <= 5; i++) {
            dataIcons.add(R.drawable.ic_star_border_black_24dp);
        }

        dopt.iconsForData = dataIcons;
        dopt.data = availableData;
        dopt.highlightData = highlightedData;
        dopt.titleText = R.string.priority;
        dopt.messageText = "";
        dopt.isSearchEnabled = false;
        dopt.dialogWidthDp = WindowManager.LayoutParams.WRAP_CONTENT;
        dopt.dialogHeightDp = 475;
        SearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    private static List<String> sortUniqNonEmpty(List<String> data, String... plus) {
        Set<String> uniq = new HashSet<>(data);
        if (plus != null) {
            uniq.addAll(Arrays.asList(plus));
        }
        data.clear();
        data.addAll(uniq);
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).trim().isEmpty()) {
                data.remove(i);
                i--;
            }
        }
        Collections.sort(data);
        return data;
    }

    private static void baseConf(Activity activity, SearchOrCustomTextDialog.DialogOptions dopt) {
        AppSettings as = new AppSettings(activity);
        dopt.isDarkDialog = as.isDarkThemeEnabled();
        dopt.textColor = ContextCompat.getColor(activity, dopt.isDarkDialog ? R.color.dark__primary_text : R.color.light__primary_text);
        dopt.highlightColor = ContextCompat.getColor(activity, R.color.accent);//0xffF57900;//0xffF57900;
    }
}
