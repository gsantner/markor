/*#######################################################
 *
 *   Maintained 2017-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.frontend;

import static net.gsantner.markor.format.todotxt.TodoTxtTask.SttTaskSimpleComparator.BY_CONTEXT;
import static net.gsantner.markor.format.todotxt.TodoTxtTask.SttTaskSimpleComparator.BY_CREATION_DATE;
import static net.gsantner.markor.format.todotxt.TodoTxtTask.SttTaskSimpleComparator.BY_DESCRIPTION;
import static net.gsantner.markor.format.todotxt.TodoTxtTask.SttTaskSimpleComparator.BY_DUE_DATE;
import static net.gsantner.markor.format.todotxt.TodoTxtTask.SttTaskSimpleComparator.BY_LINE;
import static net.gsantner.markor.format.todotxt.TodoTxtTask.SttTaskSimpleComparator.BY_PRIORITY;
import static net.gsantner.markor.format.todotxt.TodoTxtTask.SttTaskSimpleComparator.BY_PROJECT;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.os.Build;
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.Spannable;
import android.text.TextUtils;
import android.util.Pair;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import net.gsantner.markor.ApplicationObject;
import net.gsantner.markor.R;
import net.gsantner.markor.format.markdown.MarkdownTextConverter;
import net.gsantner.markor.format.todotxt.TodoTxtBasicSyntaxHighlighter;
import net.gsantner.markor.format.todotxt.TodoTxtFilter;
import net.gsantner.markor.format.todotxt.TodoTxtTask;
import net.gsantner.markor.frontend.filesearch.FileSearchDialog;
import net.gsantner.markor.frontend.filesearch.FileSearchEngine;
import net.gsantner.markor.frontend.filesearch.FileSearchResultSelectorDialog;
import net.gsantner.markor.frontend.textview.SyntaxHighlighterBase;
import net.gsantner.markor.frontend.textview.TextViewUtils;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.opoc.format.GsTextUtils;
import net.gsantner.opoc.frontend.GsSearchOrCustomTextDialog;
import net.gsantner.opoc.frontend.GsSearchOrCustomTextDialog.DialogOptions;
import net.gsantner.opoc.util.GsCollectionUtils;
import net.gsantner.opoc.util.GsContextUtils;
import net.gsantner.opoc.util.GsFileUtils;
import net.gsantner.opoc.wrapper.GsCallback;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MarkorDialogFactory {
    public static AppSettings as() {
        return ApplicationObject.settings();
    }

    public static void showSpecialKeyDialog(Activity activity, GsSearchOrCustomTextDialog.DialogState state, GsCallback.a1<String> callback) {
        DialogOptions dopt = new DialogOptions();
        baseConf(activity, dopt);
        dopt.callback = callback;
        String[] actions = activity.getResources().getStringArray(R.array.textactions_press_key__text);
        dopt.data = new ArrayList<>(Arrays.asList(actions));
        dopt.dialogHeightDp = 530;
        dopt.titleText = R.string.special_key;
        dopt.isSearchEnabled = false;
        dopt.okButtonText = 0;
        GsSearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt, state);
    }

    public static void showAsciidocSpecialKeyDialog(Activity activity, GsCallback.a1<String> callback) {
        GsSearchOrCustomTextDialog.DialogOptions dopt = new GsSearchOrCustomTextDialog.DialogOptions();
        baseConf(activity, dopt);
        dopt.callback = callback;
        // this is the only difference to showSpecialKeyDialog:
        // R.array.asciidoc_textactions_press_key__text is used instead of R.array.textactions_press_key__text
        String[] actions = activity.getResources().getStringArray(R.array.asciidoc_textactions_press_key__text);
        dopt.data = new ArrayList<>(Arrays.asList(actions));

        dopt.dialogHeightDp = 530;
        dopt.titleText = R.string.special_key;
        dopt.isSearchEnabled = false;
        dopt.okButtonText = 0;
        GsSearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    public static void showInsertTableRowDialog(final Activity activity, final boolean isHeader, GsCallback.a2<Integer, Boolean> callback) {
        final DialogOptions dopt = new DialogOptions();
        final String PREF_LAST_USED_TABLE_SIZE = "pref_key_last_used_table_size";
        final int lastUsedTableSize = as().getInt(PREF_LAST_USED_TABLE_SIZE, 3);
        final List<String> availableData = new ArrayList<>();
        for (int i = 2; i <= 5; i++) {
            availableData.add(Integer.toString(i));
        }

        baseConf(activity, dopt);
        dopt.titleText = R.string.table;
        dopt.messageText = activity.getString(R.string.how_much_columns_press_table_button_long_to_start_table) + "\n";
        dopt.messageText += activity.getString(R.string.example_of_a_markdown_table) + ":\n\n";
        dopt.messageText += "" +
                "| id | name | info |\n" +
                "|----| ---- | ---- |\n" +
                "| 1  | John | text |\n" +
                "| 2  | Anna | text |";

        dopt.callback = colsStr -> {
            as().setInt(PREF_LAST_USED_TABLE_SIZE, Integer.parseInt(colsStr));
            callback.callback(Integer.parseInt(colsStr), isHeader);
        };
        dopt.data = availableData;
        dopt.isSoftInputVisible = false;
        dopt.searchInputType = InputType.TYPE_CLASS_NUMBER;
        dopt.highlightData = Collections.singletonList(Integer.toString(lastUsedTableSize));
        dopt.searchHintText = R.string.search_or_custom;
        GsSearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    public static void showSearchFilesDialog(
            final Activity activity,
            final File searchDir,
            final GsCallback.a3<String, Integer, Boolean> callback
    ) {
        if (activity == null || searchDir == null || !searchDir.canRead()) {
            return;
        }

        if (!FileSearchEngine.isSearchExecuting.get()) {
            FileSearchDialog.showDialog(activity, searchOptions -> {
                searchOptions.rootSearchDir = searchDir;
                FileSearchEngine.queueFileSearch(activity, searchOptions, searchResults ->
                        FileSearchResultSelectorDialog.showDialog(activity, searchResults, callback));
            });
        }
    }

    public static void showSttArchiveDialog(final Activity activity, final String lastName, final GsCallback.a1<String> callback) {
        final DialogOptions dopt = new DialogOptions();
        baseConf(activity, dopt);
        dopt.callback = callback;
        final List<String> highlightedData = new ArrayList<>();
        final List<String> availableData = new ArrayList<>();
        availableData.add("todo.archive.txt");
        availableData.add("todo.done.txt");
        availableData.add("archive.txt");
        availableData.add("done.txt");
        if (!TextUtils.isEmpty(lastName)) {
            highlightedData.add(lastName);
            if (!availableData.contains(lastName)) {
                availableData.add(lastName);
            }
            dopt.defaultText = lastName;
        }

        dopt.data = availableData;
        dopt.highlightData = highlightedData;
        dopt.searchHintText = R.string.search_or_custom;
        dopt.messageText = activity.getString(R.string.archive_does_move_done_tasks);
        GsSearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    public static void showSttSortDialogue(Activity activity, final GsCallback.a2<String, Boolean> callback) {
        final DialogOptions dopt = new DialogOptions();
        baseConf(activity, dopt);
        final List<String> availableData = new ArrayList<>();
        final List<Integer> availableDataToIconMap = new ArrayList<>();

        final String o_context = activity.getString(R.string.context);
        final String o_project = activity.getString(R.string.project);
        final String o_prio = activity.getString(R.string.priority);
        final String o_date = activity.getString(R.string.date);
        final String o_textline = activity.getString(R.string.text_lines);
        final String o_description = activity.getString(R.string.description);
        final String o_duedate = activity.getString(R.string.due_date);
        final String d_asc = " (" + activity.getString(R.string.ascending) + ")";
        final String d_desc = " (" + activity.getString(R.string.descending) + ")";
        final String optLastSelected = "showSttSortDialogue.last_selected";

        dopt.callback = arg1 -> {
            as().setString(optLastSelected, arg1);
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

        final GsCallback.a2<String, Integer> addToList = (o_by, iconRes) -> {
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
        dopt.highlightData = Collections.singletonList(as().getString(optLastSelected, o_context + d_desc));
        dopt.iconsForData = availableDataToIconMap;
        dopt.okButtonText = 0;

        dopt.titleText = R.string.sort_tasks_by_selected_order;
        dopt.messageText = "";
        dopt.searchHintText = R.string.search_or_custom;
        dopt.isSearchEnabled = false;
        GsSearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    public static void showSttFilteringDialog(final Activity activity, final EditText text) {
        final DialogOptions dopt = new DialogOptions();
        baseConf(activity, dopt);

        final List<String> options = new ArrayList<>();
        final List<Integer> icons = new ArrayList<>();
        final List<GsCallback.a0> callbacks = new ArrayList<>();

        options.add(activity.getString(R.string.priority));
        icons.add(R.drawable.ic_star_black_24dp);
        callbacks.add(() -> showSttKeySearchDialog(activity, text, R.string.browse_by_priority, false, false, TodoTxtFilter.TYPE.PRIORITY));

        options.add(activity.getString(R.string.due_date));
        icons.add(R.drawable.ic_date_range_black_24dp);
        callbacks.add(() -> showSttKeySearchDialog(activity, text, R.string.browse_by_due_date, false, false, TodoTxtFilter.TYPE.DUE));

        options.add(activity.getString(R.string.project));
        icons.add(R.drawable.ic_new_label_black_24dp);
        callbacks.add(() -> showSttKeySearchDialog(activity, text, R.string.browse_by_project, true, true, TodoTxtFilter.TYPE.PROJECT));

        options.add(activity.getString(R.string.context));
        icons.add(R.drawable.gs_email_sign_black_24dp);
        callbacks.add(() -> showSttKeySearchDialog(activity, text, R.string.browse_by_context, true, true, TodoTxtFilter.TYPE.CONTEXT));

        options.add(activity.getString(R.string.advanced_filtering));
        icons.add(R.drawable.ic_extension_black_24dp);
        callbacks.add(() -> {
            final DialogOptions dopt2 = makeSttLineSelectionDialog(activity, text, t -> true);
            dopt2.titleText = R.string.advanced_filtering;
            dopt2.messageText = Html.fromHtml(activity.getString(R.string.advanced_filtering_help));
            final String[] queryHolder = new String[1];
            dopt2.searchFunction = (query, line) -> {
                queryHolder[0] = query.toString();
                return TodoTxtFilter.isMatchQuery(new TodoTxtTask(line), query);
            };
            addSaveQuery(activity, dopt2, () -> queryHolder[0]);
            GsSearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt2);
        });

        // Add saved views
        final List<Pair<String, String>> savedViews = TodoTxtFilter.loadSavedFilters(activity);
        final List<Integer> indices = GsCollectionUtils.range(savedViews.size());
        Collections.sort(indices, (a, b) -> savedViews.get(a).first.compareTo(savedViews.get(b).first));

        for (final int i : indices) {
            // No icon for the saved searches
            final String title = savedViews.get(i).first;
            final String query = savedViews.get(i).second;
            options.add(title);
            icons.add(R.drawable.empty_blank);
            callbacks.add(() -> {
                final DialogOptions doptView = makeSttLineSelectionDialog(activity, text, t -> TodoTxtFilter.isMatchQuery(t, query));
                setQueryTitle(doptView, title, query);

                // Delete view
                doptView.neutralButtonText = R.string.delete;
                doptView.neutralButtonCallback = viewDialog -> {
                    final DialogOptions confirmDopt = new DialogOptions();
                    baseConf(activity, confirmDopt);
                    confirmDopt.titleText = R.string.confirm_delete;
                    confirmDopt.messageText = title;
                    confirmDopt.isSearchEnabled = false;
                    confirmDopt.callback = (s) -> {
                        viewDialog.dismiss();
                        TodoTxtFilter.deleteFilterIndex(activity, i);
                    };
                    GsSearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, confirmDopt);
                };

                GsSearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, doptView);
            });
        }

        dopt.data = options;
        dopt.iconsForData = icons;
        dopt.positionCallback = (posn) -> callbacks.get(posn.get(0)).callback();
        dopt.isSearchEnabled = false;
        dopt.titleText = R.string.browse_todo;

        GsSearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    /**
     * Filter todos with specified keys.
     * <p>
     * Will display a list of keys. The user can select multiple keys and a list of todos which match the keys will be displayed.
     * The user can then search and select one or more (filtered) todos.
     *
     * @param activity     Context activity
     * @param text         Edit Text with todos
     * @param title        Dialog title
     * @param enableSearch Whether key search is enabled
     * @param enableAnd    Whether 'and' keys makes sense / is enabled
     * @param queryType    Key used with TodoTxtFilter
     */
    public static void showSttKeySearchDialog(
            final Activity activity,
            final EditText text,
            final int title,
            final boolean enableSearch,
            final boolean enableAnd,
            final TodoTxtFilter.TYPE queryType
    ) {

        final DialogOptions dopt = new DialogOptions();
        baseConf(activity, dopt);

        // Populate options
        // -------------------------------------

        final List<TodoTxtTask> allTasks = TodoTxtTask.getAllTasks(text.getText());
        final List<TodoTxtFilter.SttFilterKey> keys = TodoTxtFilter.getKeys(activity, allTasks, queryType);

        // Add other cases
        final List<String> data = new ArrayList<>();
        final List<String> hlData = new ArrayList<>();
        for (final TodoTxtFilter.SttFilterKey k : keys) {
            final String opt = String.format("%s (%d)", k.key, k.count);
            data.add(opt);
            if (k.query == null) {
                hlData.add(opt);
            }
        }
        dopt.data = data;
        dopt.highlightData = hlData;

        // Set up _and_ key
        // -------------------------------------

        final boolean[] useAnd = {false};
        if (enableAnd) {
            dopt.neutralButtonText = R.string.match_any;
            dopt.neutralButtonCallback = (dialog) -> {
                Button neutralButton;
                if (dialog != null && (neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)) != null) {
                    useAnd[0] = !useAnd[0];
                    neutralButton.setText(useAnd[0] ? R.string.match_all : R.string.match_any);
                }
            };
        }

        // Other options
        // -------------------------------------
        dopt.titleText = title;
        dopt.isSearchEnabled = enableSearch;
        dopt.searchHintText = R.string.search;
        dopt.isMultiSelectEnabled = true;

        // Callback to actually show tasks
        // -------------------------------------
        dopt.positionCallback = (keyIndices) -> {

            // Build a query
            final List<String> queryKeys = new ArrayList<>();
            for (final Integer index : keyIndices) {
                queryKeys.add(keys.get(index).query);
            }
            final String query = TodoTxtFilter.makeQuery(queryKeys, useAnd[0], queryType);

            final DialogOptions doptSel = makeSttLineSelectionDialog(activity, text, t -> TodoTxtFilter.isMatchQuery(t, query));
            setQueryTitle(doptSel, activity.getString(title), query);
            addSaveQuery(activity, doptSel, () -> query);

            GsSearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, doptSel);
        };
        GsSearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    private static void setQueryTitle(final DialogOptions dopt, final String subTitle, final String query) {
        // Remove the actual title
        dopt.titleText = 0;
        // Use a message text with 2 lines and a bold name
        dopt.messageText = Html.fromHtml(String.format("<b>%s</b><br><small>%s</small>", subTitle, query));
    }

    // Add the save query dialog
    private static void addSaveQuery(final Activity activity, final DialogOptions dopt, final GsCallback.s0 getQuery) {
        // Callback to save view
        dopt.neutralButtonText = R.string.save;
        dopt.neutralButtonCallback = (dialog) -> {
            final String query = getQuery.callback();
            // Get save name
            final DialogOptions doptSave = new DialogOptions();
            baseConf(activity, doptSave);
            doptSave.titleText = R.string.name;
            doptSave.searchHintText = R.string.empty_string;
            doptSave.callback = saveTitle -> {
                if (!TextUtils.isEmpty(saveTitle)) {
                    TodoTxtFilter.saveFilter(activity, saveTitle, query);
                }
            };
            // Note that we do not dismiss the existing view
            GsSearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, doptSave);
        };
    }

    /**
     * Make a dialog for searching and selecting lines of a todo-txt file
     *
     * @param activity Activity
     * @param text     EditText containing the todo.txt file
     * @param filter   Filter selecting certain todos (by context, project etc etc)
     * @return Dialogoptions for the dialog. Can be further modified by the caller
     */
    public static DialogOptions makeSttLineSelectionDialog(
            final Activity activity,
            final EditText text,
            final GsCallback.b1<TodoTxtTask> filter
    ) {
        final DialogOptions dopt = new DialogOptions();
        baseConf(activity, dopt);
        final List<TodoTxtTask> allTasks = TodoTxtTask.getAllTasks(text.getText());
        final List<String> lines = new ArrayList<>();
        final List<Integer> lineIndices = new ArrayList<>();
        for (int i = 0; i < allTasks.size(); i++) {
            if (filter.callback(allTasks.get(i))) {
                lines.add(allTasks.get(i).getLine());
                lineIndices.add(i);
            }
        }
        dopt.data = lines;
        dopt.titleText = R.string.search;
        dopt.extraFilter = "[^\\s]+"; // Line must have one or more non-whitespace to display
        dopt.isMultiSelectEnabled = true;
        dopt.highlighter = as().isHighlightingEnabled() ? getSttHighlighter() : null;
        dopt.positionCallback = (posns) -> {
            final List<Integer> selIndices = new ArrayList<>();
            for (final Integer p : posns) {
                selIndices.add(lineIndices.get(p));
            }
            TextViewUtils.selectLines(text, selIndices);
        };

        return dopt;
    }

    // Search dialog for todo.txt
    public static void showSttSearchDialog(final Activity activity, final EditText text) {
        final DialogOptions dopt = makeSttLineSelectionDialog(activity, text, t -> true);
        dopt.titleText = R.string.search_documents;
        dopt.neutralButtonText = R.string.search_and_replace;
        dopt.neutralButtonCallback = (dialog) -> {
            dialog.dismiss();
            SearchAndReplaceTextDialog.showSearchReplaceDialog(activity, text.getText(), TextViewUtils.getSelection(text));
        };
        GsSearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    /**
     * Allow to choose between Hexcolor / foreground / background color, pass back stringid
     */
    public static void showColorSelectionModeDialog(Activity activity, GsCallback.a1<Integer> callback) {
        DialogOptions dopt = new DialogOptions();

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
        dopt.okButtonText = 0;
        dopt.messageText = activity.getString(R.string.set_foreground_or_background_color_hexcolor_also_possible);
        GsSearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    /**
     * Shows all checkboxes in the file in a muti select dialog.
     * The multi select can be used to check or uncheck them.
     * <p>
     * Long pressing a line will jump to the line in the file.
     */
    public static void showDocumentChecklistDialog(
            final Activity activity,
            final Editable text,
            final Pattern checkPattern,
            final int checkGroup,
            final String checkedChars,
            final String uncheckedChars,
            final @Nullable GsCallback.a1<Integer> showCallback
    ) {
        final List<String> lines = new ArrayList<>();    // String of each line
        final Set<Integer> checked = new HashSet<>();    // Whether the line is checked
        final List<Integer> indices = new ArrayList<>(); // Indices of the check char in the line
        final Matcher matcher = checkPattern.matcher("");

        GsTextUtils.forEachline(text, (index, start, end) -> {
            final String line = text.subSequence(start, end).toString();
            matcher.reset(line);
            if (matcher.find()) {
                final int cs = matcher.start(checkGroup);
                final char c = line.charAt(cs);
                if (checkedChars.indexOf(c) >= 0) {
                    checked.add(lines.size());
                }
                lines.add(line);
                indices.add(cs + start);
            }
            return true;
        });

        final DialogOptions dopt = new DialogOptions();
        baseConf(activity, dopt);
        dopt.isMultiSelectEnabled = true;
        dopt.data = lines;
        dopt.preSelected = checked;
        dopt.titleText = R.string.check_list;

        final String check = Character.toString(checkedChars.charAt(0));
        final String uncheck = Character.toString(uncheckedChars.charAt(0));
        final TextViewUtils.ChunkedEditable chunked = TextViewUtils.ChunkedEditable.wrap(text);

        dopt.positionCallback = (result) -> {
            // Result has the indices of the checker lines which are selected
            for (final int i : GsCollectionUtils.setDiff(checked, result)) {
                final int cs = indices.get(i);
                chunked.replace(cs, cs + 1, uncheck);
            }

            for (final int i : GsCollectionUtils.setDiff(result, checked)) {
                final int cs = indices.get(i);
                chunked.replace(cs, cs + 1, check);
            }

            chunked.applyChanges();
        };

        if (showCallback != null) {
            dopt.callback = (line) -> {
                final int index = lines.indexOf(line);
                if (index >= 0) {
                    final int cs = indices.get(index);
                    final int end = TextViewUtils.getLineEnd(text, cs);
                    showCallback.callback(end);
                }
            };
        }

        GsSearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    // Insert items
    public static void showInsertItemsDialog(
            final Activity activity,
            final Editable text,
            final Pattern checkPattern,
            final int checkGroup,
            final String checkedChars,
            final String uncheckedChars,
            final @Nullable GsCallback.a1<Integer> showCallback
    ) {
        final List<String> lines = new ArrayList<>();    // String of each line
        final Set<Integer> checked = new HashSet<>();    // Whether the line is checked
        final List<Integer> indices = new ArrayList<>(); // Indices of the check char in the line
        final Matcher matcher = checkPattern.matcher("");
        GsTextUtils.forEachline(text, (index, start, end) -> {
            final String line = text.subSequence(start, end).toString();
            matcher.reset(line);
            if (matcher.find()) {
                final int cs = matcher.start(checkGroup);
                final char c = line.charAt(cs);
                if (checkedChars.indexOf(c) >= 0) {
                    checked.add(lines.size());
                }
                lines.add(line);
                indices.add(cs + start);
            }
            return true;
        });

        final DialogOptions dopt = new DialogOptions();
        baseConf(activity, dopt);
        dopt.isMultiSelectEnabled = true;
        dopt.data = lines;
        dopt.preSelected = checked;
        dopt.titleText = R.string.check_list;

        final String check = Character.toString(checkedChars.charAt(0));
        final String uncheck = Character.toString(uncheckedChars.charAt(0));
        final TextViewUtils.ChunkedEditable chunked = TextViewUtils.ChunkedEditable.wrap(text);

        dopt.positionCallback = (result) -> {
            // Result has the indices of the checker lines which are selected
            for (final int i : GsCollectionUtils.setDiff(checked, result)) {
                final int cs = indices.get(i);
                chunked.replace(cs, cs + 1, uncheck);
            }

            for (final int i : GsCollectionUtils.setDiff(result, checked)) {
                final int cs = indices.get(i);
                chunked.replace(cs, cs + 1, check);
            }

            chunked.applyChanges();
        };

        if (showCallback != null) {
            dopt.callback = (line) -> {
                final int index = lines.indexOf(line);
                if (index >= 0) {
                    final int cs = indices.get(index);
                    final int end = TextViewUtils.getLineEnd(text, cs);
                    showCallback.callback(end);
                }
            };
        }

        GsSearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    public static void showSelectSpecialFileDialog(final Activity activity, final GsCallback.a1<File> callback) {
        GsSearchOrCustomTextDialog.DialogOptions dopt = new GsSearchOrCustomTextDialog.DialogOptions();
        baseConf(activity, dopt);
        dopt.titleText = R.string.special_documents;
        final ArrayList<String> data = new ArrayList<>();
        data.add(activity.getString(R.string.recently_viewed_documents));
        data.add(activity.getString(R.string.popular_documents));
        data.add(activity.getString(R.string.favourites));
        dopt.data = data;
        dopt.isSearchEnabled = false;
        final AppSettings as = ApplicationObject.settings();

        dopt.positionCallback = i -> {
            switch (i.get(0)) {
                default:
                case 0:
                    selectItemDialog(activity, R.string.recently_viewed_documents, as.getRecentFiles(), File::getName, callback);
                    break;
                case 1:
                    selectItemDialog(activity, R.string.popular_documents, as.getPopularFiles(), File::getName, callback);
                    break;
                case 2:
                    selectItemDialog(activity, R.string.favourites, as.getFavouriteFiles(), File::getName, callback);
                    break;
            }
        };

        GsSearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    /* Dialog to select an item from a list of items */
    public static <T> void selectItemDialog(
            final Activity activity,
            final int title,
            final Collection<T> items,
            final GsCallback.s1<T> toString,
            final GsCallback.a1<T> callback
    ) {
        GsSearchOrCustomTextDialog.DialogOptions dopt = new GsSearchOrCustomTextDialog.DialogOptions();
        baseConf(activity, dopt);
        dopt.titleText = title;
        final List<T> data = items instanceof List ? (List<T>) items : new ArrayList<>(items);
        dopt.data = GsCollectionUtils.map(data, toString::callback);
        dopt.positionCallback = i -> callback.callback(data.get(i.get(0)));
        dopt.isSearchEnabled = true;
        GsSearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void showGlobFilesDialog(
            final Activity activity,
            final File searchDir,
            final GsCallback.a1<File> callback
    ) {
        GsSearchOrCustomTextDialog.DialogOptions dopt = new GsSearchOrCustomTextDialog.DialogOptions();
        baseConf(activity, dopt);
        dopt.titleText = R.string.search_documents;
        dopt.isSearchEnabled = true;
        dopt.defaultText = "**/[!.]*.*";
        dopt.callback = (query) -> {
            final List<File> found = GsFileUtils.searchFiles(searchDir, query);
            GsSearchOrCustomTextDialog.DialogOptions dopt2 = new GsSearchOrCustomTextDialog.DialogOptions();
            baseConf(activity, dopt2);
            dopt2.titleText = R.string.select;
            dopt2.isSearchEnabled = true;
            dopt2.data = GsCollectionUtils.map(found, File::getPath);
            dopt2.positionCallback = (result) -> callback.callback(found.get(result.get(0)));
            dopt2.neutralButtonText = R.string.search;
            dopt2.neutralButtonCallback = dialog2 -> {
                dialog2.dismiss();
                showGlobFilesDialog(activity, searchDir, callback);
            };
            GsSearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt2);
        };
        GsSearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    // Insert items
    public static void showUpdateItemsDialog(
            final Activity activity,
            final @StringRes int title,
            final Set<String> allKeys,
            final Set<String> currentKeys,
            final GsCallback.a1<Collection<String>> callback
    ) {
        GsSearchOrCustomTextDialog.DialogOptions dopt = new GsSearchOrCustomTextDialog.DialogOptions();
        baseConf(activity, dopt);
        dopt.data = new ArrayList<>(allKeys);
        dopt.preSelected = GsCollectionUtils.map(currentKeys, s -> dopt.data.indexOf(s));
        dopt.titleText = title;
        dopt.searchHintText = R.string.search_or_custom;
        dopt.isMultiSelectEnabled = true;
        dopt.callback = (str) -> callback.callback(GsCollectionUtils.union(currentKeys, Collections.singleton(str)));
        dopt.positionCallback = (newSel) -> callback.callback(
                GsCollectionUtils.map(newSel, pi -> dopt.data.get(pi).toString()));

        GsSearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    // Get a callback which applies highligting spans to a todo.txt line
    private static GsCallback.a1<Spannable> getSttHighlighter() {
        final SyntaxHighlighterBase h = new TodoTxtBasicSyntaxHighlighter(as()).configure();
        return s -> h.setSpannable(s).recompute().applyAll();
    }

    // Basic search dialog
    public static void showSearchDialog(final Activity activity, final EditText text) {
        final DialogOptions dopt = new DialogOptions();
        baseConf(activity, dopt);
        final Editable edit = text.getText();
        dopt.data = Arrays.asList(edit.toString().split("\n", -1)); // Do not ignore empty lines
        dopt.extraFilter = "[^\\s]+"; // Line must have one or more non-whitespace to display
        dopt.titleText = R.string.search_documents;
        dopt.searchHintText = R.string.search;
        dopt.neutralButtonCallback = (dialog) -> {
            dialog.dismiss();
            SearchAndReplaceTextDialog.showSearchReplaceDialog(activity, edit, TextViewUtils.getSelection(text));
        };
        dopt.neutralButtonText = R.string.search_and_replace;
        dopt.positionCallback = (result) -> TextViewUtils.selectLines(text, result);
        GsSearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    private static class Heading {
        final int level, line;
        final String str;

        Heading(int level, CharSequence str, int line) {
            this.level = level;
            this.str = str.toString();
            this.line = line;
        }
    }

    public static class HeadlineDialogState {
        public Set<Integer> disabledLevels = new HashSet<>();
        public String searchQuery = "";
        public int listPosition = -1;
    }

    /**
     * Show a dialog to select a heading
     *
     * @param activity      Activity
     * @param edit          Editable text
     * @param webView       WebView corresponding to the text
     * @param state         State of the dialog, so it can be restored.
     * @param levelCallback Callback to get the heading level given the text and line start and end
     */
    public static void showHeadlineDialog(
            final Activity activity,
            final EditText edit,
            final WebView webView,
            final HeadlineDialogState state,
            final GsCallback.r3<Integer, CharSequence, Integer, Integer> levelCallback
    ) {
        // Get all headings and their levels
        final CharSequence text = edit.getText();
        final List<Heading> headings = new ArrayList<>();
        GsTextUtils.forEachline(text, (line, start, end) -> {
            final int level = levelCallback.callback(text, start, end);
            if (level > 0) {
                headings.add(new Heading(level, text.subSequence(start, end), line));
            }
            return true;
        });

        // List of levels present in text
        final List<Integer> levels = new ArrayList<>(new TreeSet<>(GsCollectionUtils.map(headings, h -> h.level)));

        // Currently filtered headings
        final List<Integer> filtered = GsCollectionUtils.indices(headings, h -> !state.disabledLevels.contains(h.level));
        final List<String> data = GsCollectionUtils.map(filtered, i -> headings.get(i).str);

        final DialogOptions dopt = new DialogOptions();
        baseConf(activity, dopt);
        dopt.data = data;
        dopt.titleText = R.string.table_of_contents;
        dopt.searchHintText = R.string.search;
        dopt.isSearchEnabled = true;
        dopt.isSoftInputVisible = false;
        dopt.listPosition = state.listPosition;
        dopt.defaultText = state.searchQuery;

        dopt.positionCallback = result -> {
            final int index = filtered.get(result.get(0));
            final int line = headings.get(index).line;

            TextViewUtils.selectLines(edit, line);
            final String jumpJs = "document.querySelector('[line=\"" + line + "\"]').scrollIntoView();";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                webView.evaluateJavascript(jumpJs, null);
            } else {
                webView.loadUrl("javascript:" + jumpJs);
            }
        };

        dopt.neutralButtonText = R.string.filter;
        dopt.neutralButtonCallback = (dialog) -> {
            final DialogOptions dopt2 = new DialogOptions();
            dopt2.preSelected = GsCollectionUtils.indices(levels, l -> !state.disabledLevels.contains(l));
            dopt2.data = GsCollectionUtils.map(levels, l -> "H" + l);
            dopt2.titleText = R.string.filter;
            dopt2.isSearchEnabled = false;
            dopt2.isMultiSelectEnabled = true;
            dopt2.positionCallback = (selected) -> {
                // Update levels so the selected ones are true
                state.disabledLevels.clear();
                state.disabledLevels.addAll(GsCollectionUtils.setDiff(levels, GsCollectionUtils.map(selected, levels::get)));

                // Update selection and data
                filtered.clear();
                filtered.addAll(GsCollectionUtils.indices(headings, h -> !state.disabledLevels.contains(h.level)));

                data.clear();
                data.addAll(GsCollectionUtils.map(filtered, (si, i) -> headings.get(si).str));

                // Refresh
                GsSearchOrCustomTextDialog.getAdapter(dialog).update();
            };
            GsSearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt2);
        };

        dopt.dismissCallback = (d) -> {
            state.listPosition = dopt.listPosition;
            state.searchQuery = dopt.defaultText;
        };

        GsSearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    public static void showIndentSizeDialog(final Activity activity, final int indent, final GsCallback.a1<String> callback) {
        DialogOptions dopt = new DialogOptions();
        baseConf(activity, dopt);
        dopt.callback = callback;
        dopt.data = Arrays.asList("1", "2", "4", "8");
        dopt.highlightData = Collections.singletonList(Integer.toString(indent));
        dopt.isSearchEnabled = false;
        dopt.titleText = R.string.indent;
        dopt.dialogWidthDp = WindowManager.LayoutParams.WRAP_CONTENT;
        GsSearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    public static void showFontSizeDialog(final Activity activity, final int currentSize, final GsCallback.a1<Integer> callback) {
        DialogOptions dopt = new DialogOptions();
        baseConf(activity, dopt);
        dopt.callback = (selectedDialogValueAsString -> callback.callback(Integer.parseInt(selectedDialogValueAsString)));
        final int minFontSize = 5;
        final int maxFontSize = 36;
        final List<String> sizes = new ArrayList<>();
        for (int i = minFontSize; i <= maxFontSize; i++) {
            if (i == currentSize) dopt.listPosition = i - minFontSize - 2;
            sizes.add(Integer.toString(i));
        }
        dopt.data = sizes;
        dopt.highlightData = Collections.singletonList(Integer.toString(currentSize));
        dopt.isSearchEnabled = false;
        dopt.dialogHeightDp = 400;
        dopt.titleText = R.string.font_size;
        dopt.dialogWidthDp = WindowManager.LayoutParams.WRAP_CONTENT;
        GsSearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    public static void showPriorityDialog(Activity activity, char selectedPriority, GsCallback.a1<String> callback) {
        DialogOptions dopt = new DialogOptions();
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
        dopt.okButtonText = 0;
        dopt.messageText = "";
        dopt.isSearchEnabled = false;
        dopt.dialogWidthDp = WindowManager.LayoutParams.WRAP_CONTENT;
        dopt.dialogHeightDp = 475;
        GsSearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    @SuppressLint("StringFormatMatches")
    public static void showCopyMoveConflictDialog(final Activity activity, final String fileName, final String destName, final boolean multiple, final GsCallback.a1<Integer> callback) {
        final DialogOptions dopt = new DialogOptions();
        baseConf(activity, dopt);
        dopt.positionCallback = (result) -> callback.callback(result.get(0));
        final List<String> data = new ArrayList<>();
        // Order of options here should be synchronized with WrMarkorSingleton._moveOrCopySelected
        data.add(activity.getString(R.string.keep_both));
        data.add(activity.getString(R.string.overwrite));
        data.add(activity.getString(R.string.skip));
        if (multiple) {
            data.add(activity.getString(R.string.keep_both_all));
            data.add(activity.getString(R.string.overwrite_all));
            data.add(activity.getString(R.string.skip_all));
        }
        dopt.data = data;
        dopt.isSearchEnabled = false;
        dopt.messageText = activity.getString(R.string.copy_move_conflict_message, fileName, destName);
        dopt.dialogWidthDp = WindowManager.LayoutParams.WRAP_CONTENT;
        GsSearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    public static void showSetPasswordDialog(final Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final DialogOptions dopt = new DialogOptions();
            baseConf(activity, dopt);
            dopt.isSearchEnabled = true;
            dopt.titleText = R.string.file_encryption_password;
            final boolean hasPassword = as().isDefaultPasswordSet();
            dopt.messageText = hasPassword ? activity.getString(R.string.password_already_set_setting_a_new_password_will_overwrite) : "";
            dopt.searchHintText = hasPassword ? R.string.hidden_password : R.string.empty_string;
            dopt.callback = password -> {
                if (!TextUtils.isEmpty(password)) {
                    as().setDefaultPassword(password);
                    Toast.makeText(activity, "✔️", Toast.LENGTH_SHORT).show();
                }
            };
            GsSearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
        }
    }

    public static void showInsertSnippetDialog(final Activity activity, final GsCallback.a1<String> callback) {
        final DialogOptions dopt = new DialogOptions();
        baseConf(activity, dopt);

        final List<Pair<String, File>> snippets = as().getSnippetFiles();

        dopt.data = GsCollectionUtils.map(snippets, p -> p.first);
        dopt.isSearchEnabled = true;
        dopt.titleText = R.string.insert_snippet;
        dopt.messageText = Html.fromHtml("<small><small>" + as().getSnippetsDirectory().getAbsolutePath() + "</small></small>");
        dopt.positionCallback = (ind) -> callback.callback(GsFileUtils.readTextFileFast(snippets.get(ind.get(0)).second).first);
        GsSearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    public static void baseConf(Activity activity, DialogOptions dopt) {
        dopt.isDarkDialog = GsContextUtils.instance.isDarkModeEnabled(activity);
        dopt.clearInputIcon = R.drawable.ic_baseline_clear_24;
        dopt.textColor = ContextCompat.getColor(activity, R.color.primary_text);
        dopt.highlightColor = ContextCompat.getColor(activity, R.color.accent);
        dopt.dialogStyle = R.style.Theme_AppCompat_DayNight_Dialog_Rounded;
    }
}
