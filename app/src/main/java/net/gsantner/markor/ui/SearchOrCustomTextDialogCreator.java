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
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

import net.gsantner.markor.R;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.opoc.format.todotxt.SttTask;
import net.gsantner.opoc.ui.SearchOrCustomTextDialog;
import net.gsantner.opoc.util.Callback;

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
        String[] actions = activity.getResources().getStringArray(R.array.textmoduleactions_press_key__text);
        dopt.data = new ArrayList<>(Arrays.asList(actions));

        List<String> highlightedData = new ArrayList<>();
        highlightedData.add(activity.getString(R.string.key_pos_1_document));
        highlightedData.add(activity.getString(R.string.key_pos_end_document));

        dopt.highlightData = highlightedData;
        dopt.titleText = R.string.special_key;
        dopt.isSearchEnabled = false;
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
            sb.setSpan(boldSpan, document.lastIndexOf("/"), document.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            if (document.startsWith((tmps = appSettings.getNotebookDirectoryAsStr()))) {
                sb.setSpan(sizeSpan, 0, tmps.length() + 1, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            } else if (document.startsWith((tmps = Environment.getExternalStorageDirectory().toString()))) {
                sb.setSpan(sizeSpan, 0, tmps.length() + 1, Spanned.SPAN_INCLUSIVE_INCLUSIVE);

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
        dopt.searchHintText = R.string.serach_or_custom;
        dopt.messageText = activity.getString(R.string.archive_does_move_done_tasks);
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
        dopt.searchHintText = R.string.serach_or_custom;
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
        sortUniqNonEmpty(availableData, "music", "video", "download");
        baseConf(activity, dopt);
        dopt.callback = callback;
        dopt.data = availableData;
        dopt.highlightData = highlightedData;
        dopt.titleText = isTodoTxtAlternativeNaming(activity) ? R.string.tag : R.string.project;
        dopt.searchHintText = R.string.serach_or_custom;
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
        List<String> empty = new ArrayList<>();
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

        dopt.data = availableData;
        dopt.highlightData = highlightedData;
        dopt.titleText = R.string.priority;
        dopt.searchHintText = R.string.serach_or_custom;
        dopt.messageText = "";
        dopt.isSearchEnabled = false;
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
