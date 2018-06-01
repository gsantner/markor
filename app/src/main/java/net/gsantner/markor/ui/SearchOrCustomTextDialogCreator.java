/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License: Apache 2.0 / Commercial
 *  https://github.com/gsantner/opoc/#licensing
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.ui;

import android.app.Activity;
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
import java.util.List;

public class SearchOrCustomTextDialogCreator {
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
        availableData.add("archive.txt");
        availableData.add("done.txt");
        availableData.add("todo.done.txt");
        availableData.add("todo.archive.txt");
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
        dopt.searchHintText = R.string.search_hint__add_or_custom;
        dopt.messageText = activity.getString(R.string.archive_does_move_done_tasks);
        SearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    public static void showSttContextDialog(Activity activity, List<String> availableData, List<String> highlightedData, Callback.a1<String> callback) {
        SearchOrCustomTextDialog.DialogOptions dopt = new SearchOrCustomTextDialog.DialogOptions();
        baseConf(activity, dopt);
        dopt.callback = callback;
        dopt.data = availableData;
        dopt.highlightData = highlightedData;
        dopt.titleText = R.string.context;
        dopt.searchHintText = R.string.search_hint__add_or_custom;
        dopt.messageText = activity.getString(R.string.browse_somethingsingular_or_add, activity.getString(R.string.context));
        SearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    public static void showSttProjectDialog(Activity activity, List<String> availableData, List<String> highlightedData, Callback.a1<String> callback) {
        SearchOrCustomTextDialog.DialogOptions dopt = new SearchOrCustomTextDialog.DialogOptions();
        baseConf(activity, dopt);
        dopt.callback = callback;
        dopt.data = availableData;
        dopt.highlightData = highlightedData;
        dopt.titleText = R.string.project;
        dopt.searchHintText = R.string.search_hint__add_or_custom;
        dopt.messageText = activity.getString(R.string.browse_somethingsingular_or_add, activity.getString(R.string.project));
        SearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
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
        dopt.searchHintText = R.string.search_hint__add_or_custom;
        dopt.messageText = "";
        dopt.isSearchEnabled = false;
        SearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    private static void baseConf(Activity activity, SearchOrCustomTextDialog.DialogOptions dopt) {
        AppSettings as = new AppSettings(activity);
        dopt.isDarkDialog = as.isDarkThemeEnabled();
        dopt.textColor = ContextCompat.getColor(activity, dopt.isDarkDialog ? R.color.dark__primary_text : R.color.light__primary_text);
        dopt.highlightColor = 0xffF57900;
    }
}
