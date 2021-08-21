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
import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.InputType;
import android.text.Spannable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import net.gsantner.markor.App;
import net.gsantner.markor.R;
import net.gsantner.markor.format.todotxt.TodoTxtHighlighter;
import net.gsantner.markor.format.todotxt.TodoTxtHighlighterColors;
import net.gsantner.markor.format.todotxt.TodoTxtTask;
import net.gsantner.markor.ui.fsearch.FileSearchDialog;
import net.gsantner.markor.ui.fsearch.FileSearchResultSelectorDialog;
import net.gsantner.markor.ui.fsearch.SearchEngine;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.opoc.ui.SearchOrCustomTextDialog;
import net.gsantner.opoc.util.Callback;
import net.gsantner.opoc.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.gsantner.markor.format.todotxt.TodoTxtTask.SttTaskSimpleComparator.BY_CONTEXT;
import static net.gsantner.markor.format.todotxt.TodoTxtTask.SttTaskSimpleComparator.BY_CREATION_DATE;
import static net.gsantner.markor.format.todotxt.TodoTxtTask.SttTaskSimpleComparator.BY_DESCRIPTION;
import static net.gsantner.markor.format.todotxt.TodoTxtTask.SttTaskSimpleComparator.BY_DUE_DATE;
import static net.gsantner.markor.format.todotxt.TodoTxtTask.SttTaskSimpleComparator.BY_LINE;
import static net.gsantner.markor.format.todotxt.TodoTxtTask.SttTaskSimpleComparator.BY_PRIORITY;
import static net.gsantner.markor.format.todotxt.TodoTxtTask.SttTaskSimpleComparator.BY_PROJECT;

public class SearchOrCustomTextDialogCreator {
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

    public static void showSearchFilesDialog(Activity activity, File searchDir, Callback.a2<String, Integer> callback) {
        if (!SearchEngine.isSearchExecuting) {
            Callback.a1<SearchEngine.SearchOptions> fileSearchDialogCallback = (searchOptions) -> {
                searchOptions.rootSearchDir = searchDir;
                SearchEngine.queueFileSearch(activity, searchOptions, (searchResults) ->
                        FileSearchResultSelectorDialog.showDialog(activity, searchResults, callback));
            };
            FileSearchDialog.showDialog(activity, fileSearchDialogCallback);
        }
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
        String hl = new AppSettings(activity).getLastTodoUsedArchiveFilename();
        if (!TextUtils.isEmpty(hl)) {
            highlightedData.add(hl);
            if (!availableData.contains(hl)) {
                availableData.add(hl);
            }
            dopt.defaultText = hl;
        }

        dopt.data = availableData;
        dopt.highlightData = highlightedData;
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
        String o_context = activity.getString(R.string.context);
        String o_project = activity.getString(R.string.project);
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
            String[] values = arg1
                    .replace(o_context, BY_CONTEXT)
                    .replace(o_project, BY_PROJECT)
                    .replace(o_prio, BY_PRIORITY)
                    .replace(o_date, BY_CREATION_DATE)
                    .replace(o_textline, BY_LINE)
                    .replace(o_description, BY_DESCRIPTION)
                    .replace(o_duedate, BY_DUE_DATE)
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
        addToList.callback(o_project, R.drawable.ic_new_label_black_24dp);
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

    public static void showSttContextDialog(Activity activity, List<String> availableData, Callback.a1<String> callback) {
        SearchOrCustomTextDialog.DialogOptions dopt = new SearchOrCustomTextDialog.DialogOptions();
        baseConf(activity, dopt);
        dopt.data = new ArrayList<>(new TreeSet<>(availableData));
        dopt.callback = callback;
        dopt.titleText = R.string.insert_context;
        dopt.isMultiSelectEnabled = true;
        dopt.positionCallback = (result) -> {
            for (final Integer i : result) {
                callback.callback(dopt.data.get(i).toString());
            }
        };
        SearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    public static void showSttFilteringDialog(final Activity activity, final EditText text) {
        SearchOrCustomTextDialog.DialogOptions dopt = new SearchOrCustomTextDialog.DialogOptions();
        baseConf(activity, dopt);

        final List<String> options = new ArrayList<>();
        final List<Integer> icons = new ArrayList<>();
        final List<Callback.a0> callbacks = new ArrayList<>();

        options.add(activity.getString(R.string.priority));
        icons.add(R.drawable.ic_star_black_24dp);
        callbacks.add(() -> showSttKeySearchDialog(activity, text, R.string.filter_by_priority, false, false, task ->
                task.getPriority() == TodoTxtTask.PRIORITY_NONE ? Collections.emptyList() : Collections.singletonList(Character.toString(task.getPriority()))));

        options.add(activity.getString(R.string.due_date));
        icons.add(R.drawable.ic_date_range_black_24dp);
        final Map<TodoTxtTask.TodoDueState, String> statusMap = new HashMap<>();
        statusMap.put(TodoTxtTask.TodoDueState.TODAY, activity.getString(R.string.due_today));
        statusMap.put(TodoTxtTask.TodoDueState.OVERDUE, activity.getString(R.string.due_overdue));
        statusMap.put(TodoTxtTask.TodoDueState.FUTURE, activity.getString(R.string.due_future));
        callbacks.add(() -> showSttKeySearchDialog(activity, text, R.string.filter_by_due_date, false, false, task ->
                task.getDueStatus() == TodoTxtTask.TodoDueState.NONE ? Collections.emptyList() : Collections.singletonList(statusMap.get(task.getDueStatus()))));

        options.add(activity.getString(R.string.project));
        icons.add(R.drawable.ic_new_label_black_24dp);
        callbacks.add(() -> showSttKeySearchDialog(activity, text, R.string.filter_by_project, true, true, task -> Arrays.asList(task.getProjects())));

        options.add(activity.getString(R.string.context));
        icons.add(R.drawable.gs_email_sign_black_24dp);
        callbacks.add(() -> showSttKeySearchDialog(activity, text, R.string.filter_by_context, true, true, task -> Arrays.asList(task.getContexts())));

        options.add(activity.getString(R.string.completed));
        icons.add(R.drawable.ic_check_black_24dp);
        callbacks.add(() -> {
            final SearchOrCustomTextDialog.DialogOptions dopt2 = makeSttLineSelectionDialog(activity, text, TodoTxtTask::isDone);
            dopt2.neutralButtonCallback = (d) -> showSttFilteringDialog(activity, text);
            dopt2.neutralButtonText = R.string.back_to_filter;
            SearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt2);
        });

        dopt.data = options;
        dopt.iconsForData = icons;
        dopt.positionCallback = (posn) -> callbacks.get(posn.get(0)).callback();
        dopt.isSearchEnabled = false;
        dopt.titleText = R.string.filter_todo;

        SearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    public static SearchOrCustomTextDialog.DialogOptions makeSttLineSelectionDialog(final Activity activity, final EditText text, final Callback.b1<TodoTxtTask> filter) {
        SearchOrCustomTextDialog.DialogOptions dopt = new SearchOrCustomTextDialog.DialogOptions();
        baseConf(activity, dopt);
        final TodoTxtTask[] allTasks = TodoTxtTask.getAllTasks(text.getText());
        final List<String> lines = new ArrayList<>();
        final List<Integer> lineIndices = new ArrayList<>();
        for (int i = 0; i < allTasks.length; i++)  {
            if (filter.callback(allTasks[i])) {
                lines.add(allTasks[i].getLine());
                lineIndices.add(i);
            }
        }
        dopt.data = lines;
        dopt.extraFilter = "[^\\s]+"; // Line must have one or more non-whitespace to display
        dopt.isMultiSelectEnabled = true;
        dopt.highlighter = new AppSettings(activity).isHighlightingEnabled() ? getSttHighlighter(activity) : null;
        dopt.searchHintText = R.string.search;
        dopt.titleText = R.string.search;
        dopt.positionCallback = (posns) -> {
            final List<Integer> selIndices = new ArrayList<>();
            for (final Integer p : posns) {
                selIndices.add(lineIndices.get(p));
            }
            StringUtils.selectLines(text, selIndices);
        };

        return dopt;
    }


    /**
     *
     * Filter todos with specified keys.
     *
     * Will display a list of keys. The user can select multiple keys and a list of todos which match the keys will be displayed.
     * The user can then search and select one or more (filtered) todos.
     *
     * @param activity
     * @param text
     * @param title
     * @param enableSearch
     * @param enableAndToggle
     * @param getKeys
     */
    public static void showSttKeySearchDialog(final Activity activity, final EditText text, final int title, final boolean enableSearch, final boolean enableAndToggle, final Callback.r1<List<String>, TodoTxtTask> getKeys) {
        SearchOrCustomTextDialog.DialogOptions dopt = new SearchOrCustomTextDialog.DialogOptions();
        baseConf(activity, dopt);

        final TodoTxtTask[] allTasks = TodoTxtTask.getAllTasks(text.getText());

        final List<String> keys = new ArrayList<>();
        final int[] noneCount = {0}; // Using an array as we need a final var
        for (final TodoTxtTask task : allTasks) {
            if (!task.isDone()) {
                final List<String> taskKeys = getKeys.callback(task);
                noneCount[0] += (taskKeys.size() == 0) ? 1 : 0;
                keys.addAll(taskKeys);
            }
        }

        final List<String> options = new ArrayList<>(), data = new ArrayList<>();
        final String countFormat = "%s (%d)";

        // Add none case
        final String noneString = "—";
        if (noneCount[0] > 0) {
            final String noneWithCount = String.format(countFormat, noneString, noneCount[0]);
            options.add(noneWithCount);
            dopt.highlightData = Collections.singletonList(noneWithCount);
            data.add(""); // Dummy to make options match data
        }

        // Add other cases
        for (final String k : new TreeSet<>(keys)) {
            options.add(String.format(countFormat, k, Collections.frequency(keys, k)));
            data.add(k);
        }
        dopt.data = options;

        final boolean[] useAnd = {false};
        if (!enableAndToggle) {
            dopt.neutralButtonText = R.string.back_to_filter;
            dopt.neutralButtonCallback = (d) -> showSttFilteringDialog(activity, text);
        } else {
            dopt.neutralButtonText = R.string.match_any;
            dopt.neutralButtonCallback = (dialog) -> {
                Button neutralButton;
                if (dialog != null && (neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)) != null) {
                    useAnd[0] = !useAnd[0];
                    neutralButton.setText(useAnd[0] ? R.string.match_all : R.string.match_any);
                }
            };
        }
        dopt.titleText = title;
        dopt.isSearchEnabled = enableSearch;
        dopt.searchHintText = R.string.search;
        dopt.isMultiSelectEnabled = true;

        dopt.positionCallback = (keyIndices) -> {

            final boolean noneIncluded = (noneCount[0] > 0) && keyIndices.size() > 0 && keyIndices.get(0) == 0;
            final Set<String> searchSet = new LinkedHashSet<>();
            for (int i = noneIncluded ? 1 : 0; i < keyIndices.size(); i++) {
                searchSet.add(data.get(keyIndices.get(i)));
            }

            SearchOrCustomTextDialog.DialogOptions dopt2 = makeSttLineSelectionDialog(activity, text, (task) -> {
                final List<String> taskKeys = getKeys.callback(task);
                if (task.isDone()) {
                    return false;
                } else if (useAnd[0]) {
                    return (taskKeys.isEmpty() && noneIncluded) || taskKeys.containsAll(searchSet);
                } else {
                    return (taskKeys.isEmpty() && noneIncluded) || (!Collections.disjoint(searchSet, taskKeys));
                }
            });

            // Build the message showing the current filter:
            dopt2.messageText = activity.getString(title) + "\n" +
                    (noneIncluded ? (noneString + (searchSet.isEmpty() ? "" : " | ")) : "") +
                    (noneIncluded & useAnd[0] ? "(" : "") +
                    TextUtils.join(useAnd[0] ? " & " : " | ", searchSet) +
                    (noneIncluded & useAnd[0] ? ")" : "");
            dopt2.neutralButtonText = R.string.back_to_filter;
            dopt2.neutralButtonCallback = (d) -> showSttFilteringDialog(activity, text);
            SearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt2);
        };
        SearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
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

    public static void showSttProjectDialog(Activity activity, List<String> availableData, Callback.a1<String> callback) {
        SearchOrCustomTextDialog.DialogOptions dopt = new SearchOrCustomTextDialog.DialogOptions();
        baseConf(activity, dopt);
        dopt.data = new ArrayList<>(new TreeSet<>(availableData));
        dopt.callback = callback;
        dopt.titleText = R.string.insert_project;
        dopt.searchHintText = R.string.search_or_custom;
        dopt.isMultiSelectEnabled = true;
        dopt.positionCallback = (result) -> {
            for (final Integer pi : result) {
                callback.callback(dopt.data.get(pi).toString());
            }
        };
        SearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    private static Callback.a1<Spannable> getSttHighlighter(final Context context) {
        final boolean isDarkTheme = new AppSettings(context).isDarkThemeEnabled();
        return (s) -> TodoTxtHighlighter.basicTodoTxtHighlights(s, true, new TodoTxtHighlighterColors(), isDarkTheme, null);
    }

    public static void showSearchDialog(final Activity activity, final EditText text) {
        SearchOrCustomTextDialog.DialogOptions dopt2 = new SearchOrCustomTextDialog.DialogOptions();
        baseConf(activity, dopt2);
        final Editable edit = text.getText();
        dopt2.data = Arrays.asList(edit.toString().split("\n", -1)); // Do not ignore empty lines
        dopt2.extraFilter = "[^\\s]+"; // Line must have one or more non-whitespace to display
        dopt2.titleText = R.string.search_documents;
        dopt2.searchHintText = R.string.search;
        dopt2.neutralButtonCallback = (d) -> SearchReplaceDialog.showSearchReplaceDialog(activity, edit, StringUtils.getSelection(text));
        dopt2.neutralButtonText = R.string.search_and_replace;
        dopt2.positionCallback = (result) -> StringUtils.selectLines(text, result);
        SearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt2);
    }

    public static void showHeadlineDialog(final String headlineFilterPattern, final Activity activity, final EditText text) {
        SearchOrCustomTextDialog.DialogOptions dopt2 = new SearchOrCustomTextDialog.DialogOptions();
        baseConf(activity, dopt2);
        dopt2.positionCallback = (result) -> StringUtils.selectLines(text, result);
        dopt2.data = Arrays.asList(text.getText().toString().split("\n", -1));
        dopt2.titleText = R.string.table_of_contents;
        dopt2.searchHintText = R.string.search;
        dopt2.extraFilter = headlineFilterPattern;
        dopt2.isSearchEnabled = true;
        dopt2.searchIsRegex = false;
        dopt2.gravity = Gravity.TOP;
        SearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt2);
    }

    public static void showIndentSizeDialog(final Activity activity, final int indent, final Callback.a1<String> callback) {
        SearchOrCustomTextDialog.DialogOptions dopt = new SearchOrCustomTextDialog.DialogOptions();
        baseConf(activity, dopt);
        dopt.callback = callback;
        dopt.data = Arrays.asList("1", "2", "4", "8");
        dopt.highlightData = Collections.singletonList(Integer.toString(indent));
        dopt.isSearchEnabled = false;
        dopt.dialogWidthDp = WindowManager.LayoutParams.WRAP_CONTENT;
        dopt.dialogHeightDp = WindowManager.LayoutParams.WRAP_CONTENT;
        dopt.titleText = R.string.indent;
        SearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    public static void showFontSizeDialog(final Activity activity, final int currentSize, final Callback.a1<Integer> callback) {
        SearchOrCustomTextDialog.DialogOptions dopt = new SearchOrCustomTextDialog.DialogOptions();
        baseConf(activity, dopt);
        dopt.callback = (selectedDialogValueAsString -> callback.callback(Integer.parseInt(selectedDialogValueAsString)));
        final int minFontSize = 1;
        final int maxFontSize = 36;
        final List<String> sizes = new ArrayList<>();
        for (int i = minFontSize; i <= maxFontSize; i++) {
            sizes.add(Integer.toString(i));
        }
        dopt.data = sizes;
        dopt.highlightData = Collections.singletonList(Integer.toString(currentSize));
        dopt.isSearchEnabled = false;
        dopt.dialogWidthDp = WindowManager.LayoutParams.WRAP_CONTENT;
        dopt.dialogHeightDp = 400;
        dopt.titleText = R.string.font_size;
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
        for (int i = 'A'; i <= ((int) 'Z'); i++) {
            availableData.add(Character.toString((char) i));
        }
        highlightedData.add(none);
        if (selectedPriority != TodoTxtTask.PRIORITY_NONE) {
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

    public static void showCopyMoveConflictDialog(final Activity activity, final String fileName, final String destName, final boolean multiple, final Callback.a1<Integer> callback) {
        SearchOrCustomTextDialog.DialogOptions dopt = new SearchOrCustomTextDialog.DialogOptions();
        baseConf(activity, dopt);
        dopt.positionCallback = (result) -> callback.callback(result.get(0));
        List<String> data = new ArrayList<>();
        data.add(activity.getString(R.string.keep_both));
        data.add(activity.getString(R.string.skip));
        data.add(activity.getString(R.string.overwrite));
        if (multiple) {
            data.add(activity.getString(R.string.keep_both_all));
            data.add(activity.getString(R.string.overwrite_all));
            data.add(activity.getString(R.string.skip_all));
        }
        dopt.data = data;
        dopt.isSearchEnabled = false;
        dopt.dialogWidthDp = WindowManager.LayoutParams.WRAP_CONTENT;
        dopt.dialogHeightDp = WindowManager.LayoutParams.WRAP_CONTENT;
        dopt.messageText = activity.getString(R.string.copy_move_conflict_message, fileName, destName);
        dopt.dialogWidthDp = WindowManager.LayoutParams.WRAP_CONTENT;
        dopt.dialogHeightDp = WindowManager.LayoutParams.WRAP_CONTENT;
        SearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    public static void showSetPasswordDialog(final Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final AppSettings as = new AppSettings(activity.getApplicationContext());
            final SearchOrCustomTextDialog.DialogOptions dopt = new SearchOrCustomTextDialog.DialogOptions();
            baseConf(activity, dopt);
            dopt.isSearchEnabled = true;
            dopt.titleText = R.string.file_encryption_password;
            final boolean hasPassword = as.isDefaultPasswordSet();
            dopt.messageText = hasPassword ? activity.getString(R.string.password_already_set_setting_a_new_password_will_overwrite) : "";
            dopt.searchHintText = hasPassword ? R.string.hidden_password : R.string.empty_string;
            dopt.callback = password -> {
                if (!TextUtils.isEmpty(password)) {
                    AppSettings.get().setDefaultPassword(password);
                    Toast.makeText(activity, "✔️", Toast.LENGTH_SHORT).show();
                }
            };
            SearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
        }
    }

    public static void baseConf(Activity activity, SearchOrCustomTextDialog.DialogOptions dopt) {
        AppSettings as = new AppSettings(activity);
        dopt.isDarkDialog = as.isDarkThemeEnabled();
        dopt.clearInputIcon = R.drawable.ic_baseline_clear_24;
        dopt.textColor = ContextCompat.getColor(activity, dopt.isDarkDialog ? R.color.dark__primary_text : R.color.light__primary_text);
        dopt.highlightColor = ContextCompat.getColor(activity, R.color.accent);//0xffF57900;//0xffF57900;
    }
}
