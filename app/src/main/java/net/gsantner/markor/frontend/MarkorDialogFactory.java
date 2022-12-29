/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
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
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import net.gsantner.markor.ApplicationObject;
import net.gsantner.markor.R;
import net.gsantner.markor.format.FormatRegistry;
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
import net.gsantner.opoc.util.GsContextUtils;
import net.gsantner.opoc.util.GsFileUtils;
import net.gsantner.opoc.wrapper.GsCallback;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

public class MarkorDialogFactory {
    public static AppSettings as() {
        return ApplicationObject.settings();
    }

    public static void showSpecialKeyDialog(Activity activity, GsCallback.a1<String> callback) {
        DialogOptions dopt = new DialogOptions();
        baseConf(activity, dopt);
        dopt.callback = callback;
        String[] actions = activity.getResources().getStringArray(R.array.textactions_press_key__text);
        dopt.data = new ArrayList<>(Arrays.asList(actions));

        dopt.dialogHeightDp = 530;
        dopt.titleText = R.string.special_key;
        dopt.isSearchEnabled = false;
        dopt.okButtonText = 0;
        GsSearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
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

    public static void showAttachSomethingDialog(final Activity activity, final GsCallback.a1<Integer> userCallback) {
        final List<String> availableData = new ArrayList<>();
        final List<Integer> availableDataToActionMap = new ArrayList<>();
        final List<Integer> availableDataToIconMap = new ArrayList<>();
        final GsCallback.a3<Integer, Integer, Integer> addToList = (strRes, actionRes, iconRes) -> {
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

        DialogOptions dopt = new DialogOptions();
        baseConf(activity, dopt);
        dopt.callback = str -> userCallback.callback(availableDataToActionMap.get(availableData.indexOf(str)));
        dopt.data = availableData;
        dopt.iconsForData = availableDataToIconMap;
        dopt.isSearchEnabled = false;
        dopt.okButtonText = 0;
        dopt.titleText = 0;
        dopt.dialogWidthDp = WindowManager.LayoutParams.WRAP_CONTENT;
        dopt.gravity = Gravity.BOTTOM | Gravity.END;
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
        dopt.messageText = activity.getString(R.string.how_much_columns_press_table_button_long_to_start_table);
        dopt.messageText += activity.getString(R.string.example_of_a_markdown_table) + ":\n\n";
        dopt.messageText += "| id | name | info |\n|-----|-----------|--------|\n| 1  | John   | text |\n| 2  | Anna   | text |\n";

        dopt.callback = colsStr -> {
            as().setInt(PREF_LAST_USED_TABLE_SIZE, Integer.parseInt(colsStr));
            callback.callback(Integer.parseInt(colsStr), isHeader);
        };
        dopt.data = availableData;
        dopt.searchInputType = InputType.TYPE_CLASS_NUMBER;
        dopt.highlightData = Collections.singletonList(Integer.toString(lastUsedTableSize));
        dopt.searchHintText = R.string.search_or_custom;
        GsSearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    public static void showSearchFilesDialog(Activity activity, File searchDir, GsCallback.a2<String, Integer> callback) {
        if (!FileSearchEngine.isSearchExecuting) {
            GsCallback.a1<FileSearchEngine.SearchOptions> fileSearchDialogCallback = (searchOptions) -> {
                searchOptions.rootSearchDir = searchDir;
                FileSearchEngine.queueFileSearch(activity, searchOptions, (searchResults) ->
                        FileSearchResultSelectorDialog.showDialog(activity, searchResults, callback));
            };
            FileSearchDialog.showDialog(activity, fileSearchDialogCallback);
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
        dopt.dialogWidthDp = WindowManager.LayoutParams.WRAP_CONTENT;
        dopt.dialogHeightDp = 530;
        dopt.gravity = Gravity.BOTTOM | Gravity.END;
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

        final Boolean showIme = TextViewUtils.isImeOpen(text);

        final List<String> options = new ArrayList<>();
        final List<Integer> icons = new ArrayList<>();
        final List<GsCallback.a0> callbacks = new ArrayList<>();

        options.add(activity.getString(R.string.priority));
        icons.add(R.drawable.ic_star_black_24dp);
        callbacks.add(() -> showSttKeySearchDialog(activity, text, R.string.browse_by_priority, false, false, showIme, TodoTxtFilter.TYPE.PRIORITY));

        options.add(activity.getString(R.string.due_date));
        icons.add(R.drawable.ic_date_range_black_24dp);
        callbacks.add(() -> showSttKeySearchDialog(activity, text, R.string.browse_by_due_date, false, false, showIme, TodoTxtFilter.TYPE.DUE));

        options.add(activity.getString(R.string.project));
        icons.add(R.drawable.ic_new_label_black_24dp);
        callbacks.add(() -> showSttKeySearchDialog(activity, text, R.string.browse_by_project, true, true, showIme, TodoTxtFilter.TYPE.PROJECT));

        options.add(activity.getString(R.string.context));
        icons.add(R.drawable.gs_email_sign_black_24dp);
        callbacks.add(() -> showSttKeySearchDialog(activity, text, R.string.browse_by_context, true, true, showIme, TodoTxtFilter.TYPE.CONTEXT));

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
            addRestoreKeyboard(activity, dopt2, text, showIme);
            GsSearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt2);
        });

        // Add saved views
        final List<Pair<String, String>> savedViews = TodoTxtFilter.loadSavedFilters(activity);
        final List<Integer> indices = GsTextUtils.range(savedViews.size());
        Collections.sort(indices, (a, b) -> savedViews.get(a).first.compareTo(savedViews.get(b).first));

        for (final int i : indices) {
            // No icon for the saved searches
            final String title = savedViews.get(i).first;
            final String query = savedViews.get(i).second;
            options.add(title);
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
        dopt.dialogWidthDp = WindowManager.LayoutParams.MATCH_PARENT;

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
     * @param showIme      Whether to show IME when done (if == true)
     * @param queryType    Key used with TodoTxtFilter
     */
    public static void showSttKeySearchDialog(
            final Activity activity,
            final EditText text,
            final int title,
            final boolean enableSearch,
            final boolean enableAnd,
            final Boolean showIme,
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
        addRestoreKeyboard(activity, dopt, text, showIme);

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
            addRestoreKeyboard(activity, doptSel, text, showIme);

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
        DialogOptions dopt = new DialogOptions();
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
        addRestoreKeyboard(activity, dopt, text);
        GsSearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    // Restore the keyboard when dialog closes if keyboard is open when dialog was called
    private static void addRestoreKeyboard(final Activity activity, final DialogOptions options, final EditText edit) {
        addRestoreKeyboard(activity, options, edit, TextViewUtils.isImeOpen(edit));
    }

    // Restore the keyboard when dialog closes if required
    private static void addRestoreKeyboard(final Activity activity, final DialogOptions options, final EditText edit, final Boolean restore) {
        if (restore != null && restore) {
            options.dismissCallback = (d) -> GsContextUtils.instance.setSoftKeyboardVisible(activity, true, edit);
        }
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

    // Insert items
    public static void showInsertItemsDialog(
            final Activity activity,
            final @StringRes int title,
            final List<String> data,
            final @Nullable EditText text,              // Passed in here for keyboard restore
            final GsCallback.a1<String> insertCallback
    ) {
        GsSearchOrCustomTextDialog.DialogOptions dopt = new GsSearchOrCustomTextDialog.DialogOptions();
        baseConf(activity, dopt);
        dopt.data = new ArrayList<>(new TreeSet<>(data));
        dopt.callback = insertCallback;
        dopt.titleText = title;
        dopt.searchHintText = R.string.search_or_custom;
        dopt.isMultiSelectEnabled = true;
        dopt.positionCallback = (result) -> {
            for (final Integer pi : result) {
                insertCallback.callback(dopt.data.get(pi).toString());
            }
        };
        if (text != null) {
            addRestoreKeyboard(activity, dopt, text);
        }
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
        addRestoreKeyboard(activity, dopt, text);
        GsSearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    public static void showHeadlineDialog(final String headlineFilterPattern, final Activity activity, final EditText text) {
        final DialogOptions dopt = new DialogOptions();
        baseConf(activity, dopt);
        dopt.positionCallback = (result) -> TextViewUtils.selectLines(text, result);
        dopt.data = Arrays.asList(text.getText().toString().split("\n", -1));
        dopt.titleText = R.string.table_of_contents;
        dopt.searchHintText = R.string.search;
        dopt.extraFilter = headlineFilterPattern;
        dopt.isSearchEnabled = true;
        dopt.gravity = Gravity.TOP;
        addRestoreKeyboard(activity, dopt, text);
        GsSearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    public static void showIndentSizeDialog(final Activity activity, final int indent, final GsCallback.a1<String> callback) {
        DialogOptions dopt = new DialogOptions();
        baseConf(activity, dopt);
        dopt.callback = callback;
        dopt.data = Arrays.asList("1", "2", "4", "8");
        dopt.highlightData = Collections.singletonList(Integer.toString(indent));
        dopt.isSearchEnabled = false;
        dopt.dialogWidthDp = WindowManager.LayoutParams.WRAP_CONTENT;
        dopt.dialogHeightDp = WindowManager.LayoutParams.WRAP_CONTENT;
        dopt.titleText = R.string.indent;
        GsSearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    public static void showFontSizeDialog(final Activity activity, final int currentSize, final GsCallback.a1<Integer> callback) {
        DialogOptions dopt = new DialogOptions();
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
        dopt.dialogWidthDp = WindowManager.LayoutParams.WRAP_CONTENT;
        dopt.dialogHeightDp = WindowManager.LayoutParams.WRAP_CONTENT;
        dopt.messageText = activity.getString(R.string.copy_move_conflict_message, fileName, destName);
        dopt.dialogWidthDp = WindowManager.LayoutParams.WRAP_CONTENT;
        dopt.dialogHeightDp = WindowManager.LayoutParams.WRAP_CONTENT;
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

    // Read all files in snippets folder with appropriate extension
    // Create a map of snippet title -> text
    public static Map<String, File> getSnippets(final AppSettings as) {
        final Map<String, File> texts = new TreeMap<>();
        final File folder = new File(as.getNotebookDirectory(), ".app/snippets");
        if ((!folder.exists() || !folder.isDirectory() || !folder.canRead())) {
            if (!folder.mkdirs()) {
                return texts;
            }
        }

        // Read all files in snippets folder with appropriate extension
        // Create a map of snippet title -> text
        for (final File f : GsFileUtils.replaceFilesWithCachedVariants(folder.listFiles())) {
            if (f.exists() && f.canRead() && FormatRegistry.isFileSupported(f, true)) {
                texts.put(f.getName(), f);
            }
        }
        return texts;
    }

    public static void showInsertSnippetDialog(final Activity activity, @Nullable final EditText edit, final GsCallback.a1<String> callback) {
        final DialogOptions dopt = new DialogOptions();
        baseConf(activity, dopt);

        final Map<String, File> texts = getSnippets(as());
        final Boolean showIme = edit != null ? TextViewUtils.isImeOpen(edit) : null;

        final List<String> data = new ArrayList<>(texts.keySet());
        dopt.data = data;
        dopt.isSearchEnabled = true;
        dopt.titleText = R.string.insert_snippet;
        dopt.messageText = Html.fromHtml("<small><small>" + as().getSnippetsFolder().getAbsolutePath() + "</small></small>");
        dopt.positionCallback = (ind) -> callback.callback(GsFileUtils.readTextFileFast(texts.get(data.get(ind.get(0)))).first);
        addRestoreKeyboard(activity, dopt, edit, showIme);
        GsSearchOrCustomTextDialog.showMultiChoiceDialogWithSearchFilterUI(activity, dopt);
    }

    public static void baseConf(Activity activity, DialogOptions dopt) {
        dopt.isDarkDialog = GsContextUtils.instance.isDarkModeEnabled(activity);
        dopt.clearInputIcon = R.drawable.ic_baseline_clear_24;
        dopt.textColor = ContextCompat.getColor(activity, R.color.primary_text);
        dopt.highlightColor = ContextCompat.getColor(activity, R.color.accent);
    }
}
