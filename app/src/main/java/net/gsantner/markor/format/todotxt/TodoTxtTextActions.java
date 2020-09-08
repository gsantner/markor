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

import android.app.Activity;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.view.HapticFeedbackConstants;
import android.view.View;

import net.gsantner.markor.R;
import net.gsantner.markor.format.general.CommonTextActions;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.SearchOrCustomTextDialogCreator;
import net.gsantner.markor.ui.hleditor.TextActions;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.DocumentIO;
import net.gsantner.markor.util.ShareUtil;
import net.gsantner.opoc.util.FileUtils;
import net.gsantner.opoc.util.StringUtils;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

//TODO
public class TodoTxtTextActions extends TextActions {

    public TodoTxtTextActions(Activity activity, Document document) {
        super(activity, document);
    }

    @Override
    public boolean runAction(String action, boolean modLongClick, String anotherArg) {
        return runCommonTextAction(action);
    }

    @Override
    protected ActionCallback getActionCallback(@StringRes int keyId) {
        return new TodoTxtTextActionsImpl(keyId);
    }

    @Override
    public List<ActionItem> getActiveActionList() {


        final int projectIcon = _appSettings.isTodoTxtAlternativeNaming() ? R.drawable.ic_local_offer_black_24dp : R.drawable.ic_baseline_add_24;

        final ActionItem[] TMA_ACTIONS = {
                new ActionItem(R.string.tmaid_todotxt_toggle_done, R.drawable.ic_check_box_black_24dp, R.string.toggle_done),
                new ActionItem(R.string.tmaid_todotxt_add_context, R.drawable.gs_email_sign_black_24dp, R.string.add_context),
                new ActionItem(R.string.tmaid_todotxt_add_project, projectIcon, R.string.add_project),
                new ActionItem(R.string.tmaid_todotxt_priority, R.drawable.ic_star_border_black_24dp, R.string.priority),
                new ActionItem(R.string.tmaid_common_delete_lines, CommonTextActions.ACTION_DELETE_LINES_ICON, R.string.delete_lines),
                new ActionItem(R.string.tmaid_common_open_link_browser, CommonTextActions.ACTION_OPEN_LINK_BROWSER__ICON, R.string.open_link),
                new ActionItem(R.string.tmaid_common_attach_something, R.drawable.ic_attach_file_black_24dp, R.string.attach),
                new ActionItem(R.string.tmaid_common_special_key, CommonTextActions.ACTION_SPECIAL_KEY__ICON, R.string.special_key),
                new ActionItem(R.string.tmaid_todotxt_archive_done_tasks, R.drawable.ic_archive_black_24dp, R.string.archive_completed_tasks),
                new ActionItem(R.string.tmaid_todotxt_sort_todo, R.drawable.ic_sort_by_alpha_black_24dp, R.string.sort_alphabetically),
                new ActionItem(R.string.tmaid_todotxt_current_date, R.drawable.ic_date_range_black_24dp, R.string.current_date),
        };

        return Arrays.asList(TMA_ACTIONS);
    }

    @Override
    protected @StringRes
    int getFormatActionsKey() {
        return R.string.pref_key__todotxt__action_keys;
    }

    private class TodoTxtTextActionsImpl extends ActionCallback {
        private int _action;

        TodoTxtTextActionsImpl(int action) {
            _action = action;
        }

        @SuppressWarnings("StatementWithEmptyBody")
        @Override
        public void onClick(View view) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            final String origText = _hlEditor.getText().toString();
            final CommonTextActions commonTextActions = new CommonTextActions(_activity, _hlEditor);
            final TodoTxtTask[] selTasks = TodoTxtTask.getSelectedTasks(_hlEditor);

            switch (_action) {
                case R.string.tmaid_todotxt_toggle_done: {
                    String replaceDone = "x ";
                    if (AppSettings.get().isTodoAddCompletionDateEnabled()) {
                        replaceDone += TodoTxtTask.getToday() + " ";
                    }
                    runRegexReplaceAction(
                            // If task starts with a priority, replace priority with 'x ...'
                            new ReplacePattern(TodoTxtTask.PATTERN_PRIORITY_ANY, replaceDone),
                            // else if task stars with 'x + (completion date)?' replace with "" (i.e. remove done)
                            new ReplacePattern(TodoTxtTask.PATTERN_COMPLETION_DATE, ""),
                            // else replace task start with 'x ...'
                            new ReplacePattern("^", replaceDone)
                    );
                    trimLeadingWhiteSpace();
                    return;
                }
                case R.string.tmaid_todotxt_add_context: {
                    final List<String> allContexts = StringUtils.toArrayList(TodoTxtTask.getContexts(TodoTxtTask.getAllTasks(_hlEditor)));
                    SearchOrCustomTextDialogCreator.showSttContextDialog(_activity, allContexts, (context) -> {
                        insertUniqueItem((context.charAt(0) == '@') ? context : "@" + context);
                    });
                    return;
                }
                case R.string.tmaid_todotxt_add_project: {
                    final List<String> allProjects = StringUtils.toArrayList(TodoTxtTask.getProjects(TodoTxtTask.getAllTasks(_hlEditor)));
                    SearchOrCustomTextDialogCreator.showSttProjectDialog(_activity, allProjects, (project) -> {
                        insertUniqueItem((project.charAt(0) == '+') ? project : "+" + project);
                    });
                    return;
                }
                case R.string.tmaid_todotxt_priority: {
                    SearchOrCustomTextDialogCreator.showPriorityDialog(_activity, selTasks[0].getPriority(), (priority) -> {
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
                    return;
                }
                case R.string.tmaid_todotxt_current_date: {
                    setDate();
                    return;
                }
                case R.string.tmaid_todotxt_archive_done_tasks: {
                    SearchOrCustomTextDialogCreator.showSttArchiveDialog(_activity, (callbackPayload) -> {
                        // Don't do parse tasks in this case, performance wise
                        final ArrayList<TodoTxtTask> keep = new ArrayList<>();
                        final ArrayList<TodoTxtTask> move = new ArrayList<>();
                        final TodoTxtTask[] allTasks = TodoTxtTask.getAllTasks(_hlEditor);

                        final int[] sel = StringUtils.getSelection(_hlEditor);
                        final CharSequence text = _hlEditor.getText();
                        final int[] selStart = StringUtils.getLineOffsetFromIndex(text, sel[0]);
                        final int[] selEnd = StringUtils.getLineOffsetFromIndex(text, sel[1]);

                        for (int i = 0; i < allTasks.length; i++) {
                            final TodoTxtTask task = allTasks[i];
                            if (task.isDone()) {
                                move.add(task);
                                if (i <= selStart[0]) selStart[0]--;
                                if (i <= selEnd[0]) selEnd[0]--;
                            } else {
                                keep.add(task);
                            }
                        }
                        if (!move.isEmpty()) {
                            File todoFile = _document.getFile();
                            if (todoFile != null && (todoFile.getParentFile().exists() || todoFile.getParentFile().mkdirs())) {
                                File doneFile = new File(todoFile.getParentFile(), callbackPayload);
                                String doneFileContents = "";

                                if (doneFile.exists() && doneFile.canRead()) {
                                    doneFileContents = FileUtils.readTextFileFast(doneFile).trim() + "\n";
                                    doneFileContents += TodoTxtTask.tasksToString(move) + "\n";
                                }

                                // Write to do done file
                                if (DocumentIO.saveDocument(new Document(doneFile), doneFileContents, new ShareUtil(_activity), getContext())) {
                                    final String tasksString = TodoTxtTask.tasksToString(keep);
                                    _hlEditor.setText(tasksString);
                                    _hlEditor.setSelection(
                                            StringUtils.getIndexFromLineOffset(tasksString, selStart),
                                            StringUtils.getIndexFromLineOffset(tasksString, selEnd)
                                    );
                                }
                            }
                        }
                        new AppSettings(_activity).setLastTodoUsedArchiveFilename(callbackPayload);
                    });
                    return;
                }
                case R.string.tmaid_todotxt_sort_todo: {
                    SearchOrCustomTextDialogCreator.showSttSortDialogue(_activity, (orderBy, descending) -> new Thread() {
                        @Override
                        public void run() {
                            List<TodoTxtTask> tasks = Arrays.asList(TodoTxtTask.getAllTasks(_hlEditor));
                            TodoTxtTask.sortTasks(tasks, orderBy, descending);
                            setEditorTextAsync(TodoTxtTask.tasksToString(tasks));
                        }
                    }.start());
                    break;
                }
                case R.string.tmaid_common_open_link_browser: {
                    commonTextActions.runAction(CommonTextActions.ACTION_OPEN_LINK_BROWSER);
                    break;
                }
                case R.string.tmaid_common_special_key: {
                    commonTextActions.runAction(CommonTextActions.ACTION_SPECIAL_KEY);
                    break;
                }
                default:
                    runAction(_context.getString(_action));
            }
        }

        @Override
        public boolean onLongClick(View v) {
            String origText = _hlEditor.getText().toString();
            final CommonTextActions commonTextActions = new CommonTextActions(_activity, _hlEditor);

            switch (_action) {
                case R.string.tmaid_todotxt_add_context: {
                    final List<String> allContexts = StringUtils.toArrayList(TodoTxtTask.getContexts(TodoTxtTask.getAllTasks(_hlEditor)));
                    SearchOrCustomTextDialogCreator.showSttContextListDialog(_activity, allContexts, origText, (callbackPayload) -> {
                        int cursor = origText.indexOf(callbackPayload);
                        _hlEditor.setSelection(Math.min(_hlEditor.length(), Math.max(0, cursor)));
                    });
                    return true;
                }
                case R.string.tmaid_todotxt_add_project: {
                    final List<String> allProjects = Arrays.asList(TodoTxtTask.getProjects(TodoTxtTask.getAllTasks(_hlEditor)));
                    SearchOrCustomTextDialogCreator.showSttProjectListDialog(_activity, allProjects, origText, (callbackPayload) -> {
                        int cursor = origText.indexOf(callbackPayload);
                        _hlEditor.setSelection(Math.min(_hlEditor.length(), Math.max(0, cursor)));
                    });
                    return true;
                }
                case R.string.tmaid_common_special_key: {
                    commonTextActions.runAction(CommonTextActions.ACTION_JUMP_BOTTOM_TOP);
                    return true;
                }

                case R.string.tmaid_common_open_link_browser: {
                    commonTextActions.runAction(CommonTextActions.ACTION_SEARCH);
                    return true;
                }
                case R.string.tmaid_todotxt_current_date: {
                    setDueDate(3);
                    return true;
                }
            }
            return false;
        }
    }

    private void insertUniqueItem(String item) {
        item = item.trim().replace(" ", "_");
        if (!selectionIsSingleTask() || _appSettings.isTodoAppendProConOnEndEnabled()) {
            runRegexReplaceAction(
                    // Replace existing item with itself. i.e. do nothing
                    new ReplacePattern(String.format("\\s\\Q%s\\E(:?\\s|$)", item), "$0"),
                    // Append to end
                    new ReplacePattern("\\s*$", " " + item)
            );
        } else {
            insertInline(item);
        }
    }

    private boolean selectionIsSingleTask() {
        final int[] sel = StringUtils.getSelection(_hlEditor);
        if (sel[0] != sel[1]) {
            final CharSequence text = _hlEditor.getText();
            return StringUtils.getLineStart(text, sel[0]) == StringUtils.getLineStart(text, sel[1]);
        }
        return true;
    }

    private void trimLeadingWhiteSpace() {
        runRegexReplaceAction("^\\s*", "");
    }

    private void insertInline(String thing) {
        final int[] sel = StringUtils.getSelection(_hlEditor);
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

    private static Calendar parseDateString(String dateString, Calendar fallback) {
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
        final int[] sel = StringUtils.getSelection(_hlEditor);
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
                .setActivity(_activity)
                .setListener(listener)
                .setCalendar(initDate)
                .show(((FragmentActivity) _activity).getSupportFragmentManager(), "date");
    }


    private void setDueDate(int offset) {
        final String dueString = TodoTxtTask.getSelectedTasks(_hlEditor)[0].getDueDate(TodoTxtTask.getToday());
        Calendar initDate = parseDateString(dueString, Calendar.getInstance());
        initDate.add(Calendar.DAY_OF_MONTH, dueString == null ? offset : 0);

        DatePickerDialog.OnDateSetListener listener = (_view, year, month, day) -> {
            Calendar fmtCal = Calendar.getInstance();
            fmtCal.set(year, month, day);
            final String newDue = "due:" + TodoTxtTask.DATEF_YYYY_MM_DD.format(fmtCal.getTime());
            runRegexReplaceAction(
                    // Replace due date
                    new ReplacePattern(TodoTxtTask.PATTERN_DUE_DATE, newDue),
                    // Add due date to end if none already exists. Will correctly handle trailing whitespace.
                    new ReplacePattern("(\\s)*$", " " + newDue)
            );
        };

        new DateFragment()
                .setActivity(_activity)
                .setListener(listener)
                .setCalendar(initDate)
                .setMessage(getContext().getString(R.string.due_date))
                .show(((FragmentActivity) _activity).getSupportFragmentManager(), "date");
    }

    /**
     * A DialogFragment to manage showing a DatePicker
     * Must be public and have default constructor.
     */
    public static class DateFragment extends DialogFragment {

        private DatePickerDialog.OnDateSetListener _listener;
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

        @Override
        public DatePickerDialog onCreateDialog(Bundle savedInstanceState) {
            super.onCreateDialog(savedInstanceState);

            DatePickerDialog dialog = new DatePickerDialog(_activity, _listener, _year, _month, _day);
            if (_message != null) {
                dialog.setMessage(_message);
            }
            return dialog;
        }
    }
}
