/*
 * Copyright (c) 2017-2018 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.ui;

import android.app.Activity;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;

import net.gsantner.markor.R;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.opoc.format.todotxt.SttTask;
import net.gsantner.opoc.ui.SearchOrCustomTextDialog;
import net.gsantner.opoc.util.Callback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SearchOrCustomTextDialogCreator {
    public static void showTextNavigationDialog(Activity activity, Callback.a1<String> callback) {
        SearchOrCustomTextDialog.DialogOptions dopt = new SearchOrCustomTextDialog.DialogOptions();
        baseConf(activity, dopt);
        dopt.callback = callback;
        String[] actions = activity.getResources().getStringArray(R.array.textmoduleactions_navigation);
        dopt.data = new ArrayList<>(Arrays.asList(actions));
        dopt.highlightData = new ArrayList<>();
        dopt.titleText = R.string.text_navigation;
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
