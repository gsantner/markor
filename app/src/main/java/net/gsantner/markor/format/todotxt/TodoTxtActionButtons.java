/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.todotxt;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import net.gsantner.markor.R;
import net.gsantner.markor.format.ActionButtonBase;
import net.gsantner.markor.frontend.MarkorDialogFactory;
import net.gsantner.markor.frontend.textview.TextViewUtils;
import net.gsantner.markor.model.Document;
import net.gsantner.opoc.util.GsFileUtils;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Pattern;

public class TodoTxtActionButtons extends ActionButtonBase {

    private static final String LAST_SORT_ORDER_KEY = TodoTxtActionButtons.class.getCanonicalName() + "_last_sort_order_key";

    public TodoTxtActionButtons(@NonNull Context context, Document document) {
        super(context, document);
    }

    @Override
    public List<ActionItem> getActiveActionList() {

        final ActionItem[] TMA_ACTIONS = {
                new ActionItem(R.string.abid_todotxt_toggle_done, R.drawable.ic_check_box_black_24dp, R.string.toggle_done),
                new ActionItem(R.string.abid_todotxt_add_context, R.drawable.gs_email_sign_black_24dp, R.string.add_context),
                new ActionItem(R.string.abid_todotxt_add_project, R.drawable.ic_new_label_black_24dp, R.string.add_project),
                new ActionItem(R.string.abid_todotxt_priority, R.drawable.ic_star_border_black_24dp, R.string.priority),
                new ActionItem(R.string.abid_common_delete_lines, R.drawable.ic_delete_black_24dp, R.string.delete_lines),
                new ActionItem(R.string.abid_common_open_link_browser, R.drawable.ic_open_in_browser_black_24dp, R.string.open_link),
                new ActionItem(R.string.abid_common_attach_something, R.drawable.ic_attach_file_black_24dp, R.string.attach),
                new ActionItem(R.string.abid_common_special_key, R.drawable.ic_keyboard_black_24dp, R.string.special_key),
                new ActionItem(R.string.abid_todotxt_archive_done_tasks, R.drawable.ic_archive_black_24dp, R.string.archive_completed_tasks),
                new ActionItem(R.string.abid_todotxt_sort_todo, R.drawable.ic_sort_by_alpha_black_24dp, R.string.sort_by),
                new ActionItem(R.string.abid_todotxt_current_date, R.drawable.ic_date_range_black_24dp, R.string.current_date),
                new ActionItem(R.string.abid_common_new_line_below, R.drawable.ic_baseline_keyboard_return_24, R.string.start_new_line_below),
                new ActionItem(R.string.abid_common_move_text_one_line_up, R.drawable.ic_baseline_arrow_upward_24, R.string.move_text_one_line_up),
                new ActionItem(R.string.abid_common_move_text_one_line_down, R.drawable.ic_baseline_arrow_downward_24, R.string.move_text_one_line_down),
                new ActionItem(R.string.abid_common_insert_snippet, R.drawable.ic_baseline_file_copy_24, R.string.insert_snippet),

                new ActionItem(R.string.abid_common_web_jump_to_very_top_or_bottom, R.drawable.ic_vertical_align_center_black_24dp, R.string.jump_to_bottom, ActionItem.DisplayMode.VIEW),
                new ActionItem(R.string.abid_common_rotate_screen, R.drawable.ic_rotate_left_black_24dp, R.string.rotate, ActionItem.DisplayMode.ANY),
        };

        return Arrays.asList(TMA_ACTIONS);
    }

    @Override
    @StringRes
    protected int getFormatActionsKey() {
        return R.string.pref_key__todotxt__action_keys;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onActionClick(final @StringRes int action) {
        final List<TodoTxtTask> selTasks = TodoTxtTask.getSelectedTasks(_hlEditor);

        switch (action) {
            case R.string.abid_todotxt_toggle_done: {
                final String doneMark = "x" + (_appSettings.isTodoAddCompletionDateEnabled() ? (" " + TodoTxtTask.getToday()) : "") + " ";
                final String bodyWithPri = "(.*)(\\spri:([A-Z])(?=\\s|$))(.*)"; // +1 = pre, +2 = full tag, +3 = pri, +4 = post
                final String doneWithDate = "^([Xx]\\s(?:" + TodoTxtTask.PT_DATE + "\\s)?)";
                final String startingPriority = "^\\(([A-Z])\\)\\s";
                runRegexReplaceAction(
                        // If task not done and starts with a priority and contains a pri tag
                        new ReplacePattern(startingPriority + bodyWithPri, doneMark + "$2 pri:$1$5"),
                        // else if task not done and starts with a priority and does not contain a pri tag
                        new ReplacePattern(startingPriority + "(.*)(\\s*)", doneMark + "$2 pri:$1"),
                        // else if task is done and contains a pri tag
                        new ReplacePattern(doneWithDate + bodyWithPri, "($4) $2$5"),
                        // else if task is done and does not contain a pri tag
                        new ReplacePattern(doneWithDate, ""),
                        // else replace task start with 'x ...'
                        new ReplacePattern("^", doneMark)
                );
                return true;
            }
            case R.string.abid_todotxt_add_context: {
                final List<String> contexts = new ArrayList<>();
                contexts.addAll(TodoTxtTask.getContexts(TodoTxtTask.getAllTasks(_hlEditor.getText())));
                contexts.addAll(new TodoTxtTask(_appSettings.getTodotxtAdditionalContextsAndProjects()).getContexts());
                MarkorDialogFactory.showInsertItemsDialog(getActivity(), R.string.insert_context, contexts, _hlEditor, context -> insertUniqueItem(context, "@"));
                return true;
            }
            case R.string.abid_todotxt_add_project: {
                final List<String> projects = new ArrayList<>();
                projects.addAll(TodoTxtTask.getProjects(TodoTxtTask.getAllTasks(_hlEditor.getText())));
                projects.addAll(new TodoTxtTask(_appSettings.getTodotxtAdditionalContextsAndProjects()).getProjects());
                MarkorDialogFactory.showInsertItemsDialog(getActivity(), R.string.insert_project, projects, _hlEditor, project -> insertUniqueItem(project, "+"));
                return true;
            }
            case R.string.abid_todotxt_priority: {
                MarkorDialogFactory.showPriorityDialog(getActivity(), selTasks.get(0).getPriority(), (priority) -> {
                    ArrayList<ReplacePattern> patterns = new ArrayList<>();
                    if (priority.length() > 1) {
                        patterns.add(new ReplacePattern(TodoTxtTask.PATTERN_PRIORITY_ANY, ""));
                    } else if (priority.length() == 1) {
                        final String _priority = String.format("(%c) ", priority.charAt(0));
                        patterns.add(new ReplacePattern(TodoTxtTask.PATTERN_PRIORITY_ANY, _priority));
                        patterns.add(new ReplacePattern("^\\s*", _priority));
                    }
                    runRegexReplaceAction(patterns);
                    trimLeadingWhiteSpace();
                });
                return true;
            }
            case R.string.abid_todotxt_current_date: {
                setDueDate(_appSettings.getDueDateOffset());
                return true;
            }
            case R.string.abid_todotxt_archive_done_tasks: {
                final String last = _appSettings.getLastTodoDoneName(_document.getPath());
                MarkorDialogFactory.showSttArchiveDialog(getActivity(), last, (callbackPayload) -> {
                    callbackPayload = Document.normalizeFilename(callbackPayload);

                    final ArrayList<TodoTxtTask> keep = new ArrayList<>();
                    final ArrayList<TodoTxtTask> move = new ArrayList<>();
                    final List<TodoTxtTask> allTasks = TodoTxtTask.getAllTasks(_hlEditor.getText());

                    final int[] sel = TextViewUtils.getSelection(_hlEditor);
                    final CharSequence text = _hlEditor.getText();
                    final int[] selStart = TextViewUtils.getLineOffsetFromIndex(text, sel[0]);
                    final int[] selEnd = TextViewUtils.getLineOffsetFromIndex(text, sel[1]);

                    for (int i = 0; i < allTasks.size(); i++) {
                        final TodoTxtTask task = allTasks.get(i);
                        if (task.isDone()) {
                            move.add(task);
                            if (i <= selStart[0]) selStart[0]--;
                            if (i <= selEnd[0]) selEnd[0]--;
                        } else {
                            keep.add(task);
                        }
                    }
                    if (!move.isEmpty() && _document.testCreateParent()) {
                        File doneFile = new File(_document.getFile().getParentFile(), callbackPayload);
                        String doneFileContents = "";
                        if (doneFile.exists() && doneFile.canRead()) {
                            doneFileContents = GsFileUtils.readTextFileFast(doneFile).first.trim() + "\n";
                        }
                        doneFileContents += TodoTxtTask.tasksToString(move) + "\n";

                        // Write to done file
                        if (new Document(doneFile).saveContent(getActivity(), doneFileContents)) {
                            final String tasksString = TodoTxtTask.tasksToString(keep);
                            _hlEditor.setText(tasksString);
                            _hlEditor.setSelection(
                                    TextViewUtils.getIndexFromLineOffset(tasksString, selStart),
                                    TextViewUtils.getIndexFromLineOffset(tasksString, selEnd)
                            );
                        }
                    }
                    _appSettings.setLastTodoDoneName(_document.getPath(), callbackPayload);
                });
                return true;
            }
            case R.string.abid_todotxt_sort_todo: {
                MarkorDialogFactory.showSttSortDialogue(getActivity(), (orderBy, descending) -> new Thread() {
                    @Override
                    public void run() {
                        final List<TodoTxtTask> tasks = TodoTxtTask.getAllTasks(_hlEditor.getText());
                        TodoTxtTask.sortTasks(tasks, orderBy, descending);
                        setEditorTextAsync(TodoTxtTask.tasksToString(tasks));
                        _appSettings.setStringList(LAST_SORT_ORDER_KEY, Arrays.asList(orderBy, Boolean.toString(descending)));
                    }
                }.start());
                return true;
            }
            default: {
                return runCommonAction(action);
            }
        }
    }

    @Override
    public boolean onActionLongClick(final @StringRes int action) {

        switch (action) {
            case R.string.abid_todotxt_add_context: {
                MarkorDialogFactory.showSttKeySearchDialog(getActivity(), _hlEditor, R.string.browse_by_context, true, true, TextViewUtils.isImeOpen(_hlEditor), TodoTxtFilter.TYPE.CONTEXT);
                return true;
            }
            case R.string.abid_todotxt_add_project: {
                MarkorDialogFactory.showSttKeySearchDialog(getActivity(), _hlEditor, R.string.browse_by_project, true, true, TextViewUtils.isImeOpen(_hlEditor), TodoTxtFilter.TYPE.PROJECT);
                return true;
            }
            case R.string.abid_todotxt_sort_todo: {
                final List<String> last = _appSettings.getStringList(LAST_SORT_ORDER_KEY);
                if (last != null && last.size() == 2) {
                    final List<TodoTxtTask> tasks = TodoTxtTask.getAllTasks(_hlEditor.getText());
                    TodoTxtTask.sortTasks(tasks, last.get(0), Boolean.parseBoolean(last.get(1)));
                    setEditorTextAsync(TodoTxtTask.tasksToString(tasks));
                }
                return true;
            }
            case R.string.abid_todotxt_current_date: {
                setDate();
                return true;
            }
            default: {
                return runCommonLongPressAction(action);
            }
        }
    }

    @Override
    public boolean runTitleClick() {
        MarkorDialogFactory.showSttFilteringDialog(getActivity(), _hlEditor);
        return true;
    }

    @Override
    public boolean onSearch() {
        MarkorDialogFactory.showSttSearchDialog(getActivity(), _hlEditor);
        return true;
    }

    private void insertUniqueItem(String item, final String prefix) {
        // Prepare item
        if (prefix != null) {
            item = item.startsWith(prefix) ? item : prefix + item;
        }
        item = item.trim().replace(" ", "_");

        // Pattern to match <space><literal string><space OR end of line>
        // i.e. to check if a word is present in the line
        final Pattern pattern = Pattern.compile(String.format("\\s\\Q%s\\E(:?\\s|$)", item));
        final String lines = TextViewUtils.getSelectedLines(_hlEditor);
        // Multiline or setting
        if (lines.contains("\n") || _appSettings.isTodoAppendProConOnEndEnabled()) {
            runRegexReplaceAction(
                    // Replace existing item with itself. i.e. do nothing
                    new ReplacePattern(pattern, "$0"),
                    // Append to end
                    new ReplacePattern("\\s*$", " " + item)
            );
        } else if (!pattern.matcher(lines).find()) {
            insertInline(item);
        }
    }

    private void trimLeadingWhiteSpace() {
        runRegexReplaceAction("^\\s*", "");
    }

    private void insertInline(String thing) {
        final int[] sel = TextViewUtils.getSelection(_hlEditor);
        final CharSequence text = _hlEditor.getText();
        if (sel[0] > 0) {
            final char before = text.charAt(sel[0] - 1);
            if (before != ' ' && before != '\n') {
                thing = " " + thing;
            }
        }
        if (sel[1] < text.length()) {
            final char after = text.charAt(sel[1]);
            if (after != ' ' && after != '\n') {
                thing = thing + " ";
            }
        }
        _hlEditor.insertOrReplaceTextOnCursor(thing);
    }

    private static Calendar parseDateString(final String dateString, final Calendar fallback) {
        if (dateString == null || dateString.length() != TodoTxtTask.DATEF_YYYY_MM_DD_LEN) {
            return fallback;
        }

        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(TodoTxtTask.DATEF_YYYY_MM_DD.parse(dateString));
            return calendar;
        } catch (ParseException e) {
            return fallback;
        }
    }

    private void setDate() {
        final int[] sel = TextViewUtils.getSelection(_hlEditor);
        final Editable text = _hlEditor.getText();
        final String selStr = text.subSequence(sel[0], sel[1]).toString();
        Calendar initDate = parseDateString(selStr, Calendar.getInstance());

        DatePickerDialog.OnDateSetListener listener = (_view, year, month, day) -> {
            Calendar fmtCal = Calendar.getInstance();
            fmtCal.set(year, month, day);
            final String newDate = TodoTxtTask.DATEF_YYYY_MM_DD.format(fmtCal.getTime());
            text.replace(sel[0], sel[1], newDate);
        };

        new DateFragment()
                .setActivity(getActivity())
                .setListener(listener)
                .setCalendar(initDate)
                .show(((FragmentActivity) getActivity()).getSupportFragmentManager(), "date");
    }


    private void setDueDate(final int offset) {
        final String dueString = TodoTxtTask.getSelectedTasks(_hlEditor).get(0).getDueDate();
        Calendar initDate = parseDateString(dueString, Calendar.getInstance());
        initDate.add(Calendar.DAY_OF_MONTH, (dueString == null || dueString.isEmpty()) ? offset : 0);

        final DatePickerDialog.OnDateSetListener listener = (_view, year, month, day) -> {
            Calendar fmtCal = Calendar.getInstance();
            fmtCal.set(year, month, day);
            final String newDue = "due:" + TodoTxtTask.DATEF_YYYY_MM_DD.format(fmtCal.getTime());
            runRegexReplaceAction(
                    // Replace due date
                    new ReplacePattern(TodoTxtTask.PATTERN_DUE_DATE, "$1" + newDue + "$4"),
                    // Add due date to end if none already exists. Will correctly handle trailing whitespace.
                    new ReplacePattern("\\s*$", " " + newDue)
            );
        };

        final DatePickerDialog.OnClickListener clear = (dialog, which) -> {
            runRegexReplaceAction(new ReplacePattern(TodoTxtTask.PATTERN_DUE_DATE, "$4"));
        };

        new DateFragment()
                .setActivity(getActivity())
                .setListener(listener)
                .setCalendar(initDate)
                .setMessage(getContext().getString(R.string.due_date))
                .setExtraLabel(getContext().getString(R.string.clear))
                .setExtraListener(clear)
                .show(((FragmentActivity) getActivity()).getSupportFragmentManager(), "date");
    }

    /**
     * A DialogFragment to manage showing a DatePicker
     * Must be public and have default constructor.
     */
    public static class DateFragment extends DialogFragment {

        private DatePickerDialog.OnDateSetListener _listener;
        private DatePickerDialog.OnClickListener _extraListener;
        private String _extraLabel;

        private Activity _activity;
        private int _year;
        private int _month;
        private int _day;
        private String _message;

        public DateFragment() {
            super();
            setCalendar(Calendar.getInstance());
        }

        public DateFragment setListener(DatePickerDialog.OnDateSetListener listener) {
            _listener = listener;
            return this;
        }

        public DateFragment setExtraListener(DatePickerDialog.OnClickListener listener) {
            _extraListener = listener;
            return this;
        }

        public DateFragment setExtraLabel(String label) {
            _extraLabel = label;
            return this;
        }

        public DateFragment setActivity(Activity activity) {
            _activity = activity;
            return this;
        }

        public DateFragment setYear(int year) {
            _year = year;
            return this;
        }

        public DateFragment setMonth(int month) {
            _month = month;
            return this;
        }

        public DateFragment setDay(int day) {
            _day = day;
            return this;
        }

        public DateFragment setMessage(String message) {
            _message = message;
            return this;
        }

        public DateFragment setCalendar(Calendar calendar) {
            setYear(calendar.get(Calendar.YEAR));
            setMonth(calendar.get(Calendar.MONTH));
            setDay(calendar.get(Calendar.DAY_OF_MONTH));
            return this;
        }

        @NonNull
        @Override
        public DatePickerDialog onCreateDialog(Bundle savedInstanceState) {
            super.onCreateDialog(savedInstanceState);

            DatePickerDialog dialog = new DatePickerDialog(_activity, _listener, _year, _month, _day);

            if (_message != null && !_message.isEmpty()) {
                dialog.setMessage(_message);
            }

            if (_extraListener != null && _extraLabel != null && !_extraLabel.isEmpty()) {
                dialog.setButton(DialogInterface.BUTTON_NEUTRAL, _extraLabel, _extraListener);
            }

            return dialog;
        }
    }
}