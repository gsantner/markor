/*#######################################################
 *
 *   Maintained 2018-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.todotxt;

import android.annotation.SuppressLint;
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
import net.gsantner.opoc.util.GsCollectionUtils;
import net.gsantner.opoc.util.GsFileUtils;
import net.gsantner.opoc.wrapper.GsCallback;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

public class TodoTxtActionButtons extends ActionButtonBase {

    private static final String LAST_SORT_ORDER_KEY = TodoTxtActionButtons.class.getCanonicalName() + "_last_sort_order_key";

    public TodoTxtActionButtons(@NonNull Context context, Document document) {
        super(context, document);
    }

    @Override
    public List<ActionItem> getFormatActionList() {
        return Arrays.asList(
                new ActionItem(R.string.abid_todotxt_toggle_done, R.drawable.ic_check_box_black_24dp, R.string.toggle_done),
                new ActionItem(R.string.abid_todotxt_add_context, R.drawable.gs_email_sign_black_24dp, R.string.add_context),
                new ActionItem(R.string.abid_todotxt_add_project, R.drawable.ic_new_label_black_24dp, R.string.add_project),
                new ActionItem(R.string.abid_todotxt_priority, R.drawable.ic_star_border_black_24dp, R.string.priority),
                new ActionItem(R.string.abid_todotxt_archive_done_tasks, R.drawable.ic_archive_black_24dp, R.string.archive_completed_tasks),
                new ActionItem(R.string.abid_todotxt_due_date, R.drawable.ic_date_range_black_24dp, R.string.due_date),
                new ActionItem(R.string.abid_todotxt_sort_todo, R.drawable.ic_sort_by_alpha_black_24dp, R.string.sort_by),
                new ActionItem(R.string.abid_common_insert_link, R.drawable.ic_link_black_24dp, R.string.insert_link),
                new ActionItem(R.string.abid_common_insert_image, R.drawable.ic_image_black_24dp, R.string.insert_image),
                new ActionItem(R.string.abid_common_insert_audio, R.drawable.ic_keyboard_voice_black_24dp, R.string.audio)
        );
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
                addRemoveItems("@", TodoTxtTask::getContexts);
                return true;
            }
            case R.string.abid_todotxt_add_project: {
                addRemoveItems("+", TodoTxtTask::getProjects);
                return true;
            }
            case R.string.abid_todotxt_priority: {
                MarkorDialogFactory.showPriorityDialog(getActivity(), selTasks.get(0).getPriority(), (priority) -> {
                    setPriority(priority.length() == 1 ? priority.charAt(0) : TodoTxtTask.PRIORITY_NONE);
                });
                return true;
            }
            case R.string.abid_todotxt_due_date: {
                setDueDate(_appSettings.getDueDateOffset());
                return true;
            }
            case R.string.abid_todotxt_archive_done_tasks: {
                archiveDoneTasks();
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
                MarkorDialogFactory.showSttKeySearchDialog(getActivity(), _hlEditor, R.string.browse_by_context, true, true, TodoTxtFilter.TYPE.CONTEXT);
                return true;
            }
            case R.string.abid_todotxt_add_project: {
                MarkorDialogFactory.showSttKeySearchDialog(getActivity(), _hlEditor, R.string.browse_by_project, true, true, TodoTxtFilter.TYPE.PROJECT);
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
            case R.string.abid_todotxt_priority: {
                final Editable text = _hlEditor.getText();
                final int[] sel = TextViewUtils.getSelection(_hlEditor);
                final int lineStart = TextViewUtils.getLineStart(text, sel[0]);
                final int lineEnd = TextViewUtils.getLineEnd(text, sel[1]);
                final List<TodoTxtTask> tasks = TodoTxtTask.getTasks(text, new int[]{sel[0], sel[1]});
                char prevPriority = '\0', nextPriority = '\0';
                boolean areAllSamePriority = true;
                if (lineStart != 0) {
                    final int prevLineStart = TextViewUtils.getLineStart(text, lineStart - 1);
                    final int prevLineEnd = TextViewUtils.getLineEnd(text, prevLineStart);
                    final String prevLine = text.subSequence(prevLineStart, prevLineEnd).toString();
                    prevPriority = new TodoTxtTask(prevLine).getPriority();
                }
                if (lineEnd != text.length()) {
                    final int nextLineStart = TextViewUtils.getLineStart(text, lineEnd + 1);
                    final int nextLineEnd = TextViewUtils.getLineEnd(text, nextLineStart);
                    final String nextLine = text.subSequence(nextLineStart, nextLineEnd).toString();
                    nextPriority = new TodoTxtTask(nextLine).getPriority();
                }
                for (TodoTxtTask task : tasks) {
                    if (task.getPriority() != tasks.get(0).getPriority()) {
                        areAllSamePriority = false;
                        break;
                    }
                }
                if (areAllSamePriority) {
                    if (prevPriority != tasks.get(0).getPriority() && prevPriority != '\0') {
                        setPriority(prevPriority);
                    } else if (nextPriority != tasks.get(tasks.size() - 1).getPriority() && nextPriority != '\0') {
                        setPriority(nextPriority);
                    } else {
                        setPriority(TodoTxtTask.PRIORITY_NONE);
                    }
                } else {
                    if (prevPriority != '\0') {
                        setPriority(prevPriority);
                    } else {
                        setPriority(tasks.get(0).getPriority());
                    }
                }
                return true;
            }
            case R.string.abid_todotxt_due_date: {
                setDate();
                return true;
            }
            default: {
                return runCommonLongPressAction(action);
            }
        }
    }

    public void archiveDoneTasks() {
        final String lastDoneName = _appSettings.getLastTodoDoneName(_document.path);
        MarkorDialogFactory.showSttArchiveDialog(getActivity(), lastDoneName, callbackPayload -> {
            final String doneName = Document.normalizeFilename(callbackPayload);
            final CharSequence text = _hlEditor.getText();
            final int[] sel = TextViewUtils.getSelection(text);
            final int[][] offsets = TextViewUtils.getLineOffsetFromIndex(text, sel);

            final ArrayList<TodoTxtTask> keep = new ArrayList<>();
            final ArrayList<TodoTxtTask> move = new ArrayList<>();
            final List<TodoTxtTask> allTasks = TodoTxtTask.getAllTasks(text);

            for (int i = 0; i < allTasks.size(); i++) {
                final TodoTxtTask task = allTasks.get(i);
                if (task.isDone()) {
                    move.add(task);
                    if (i <= offsets[0][0]) offsets[0][0]--;
                    if (i <= offsets[1][0]) offsets[1][0]--;
                } else {
                    keep.add(task);
                }
            }

            if (!move.isEmpty() && _document.testCreateParent()) {
                final File doneFile = new File(_document.file.getParentFile(), doneName);
                final StringBuilder doneContents = new StringBuilder();
                if (doneFile.exists() && doneFile.canRead()) {
                    doneContents.append(GsFileUtils.readTextFileFast(doneFile).first.trim()).append("\n");
                }
                doneContents.append(TodoTxtTask.tasksToString(move)).append("\n");

                // Write to done file
                if (new Document(doneFile).saveContent(getActivity(), doneContents.toString())) {
                    final String tasksString = TodoTxtTask.tasksToString(keep);
                    _hlEditor.setText(tasksString);
                    TextViewUtils.setSelectionFromOffsets(_hlEditor, offsets);
                }
            }
            _appSettings.setLastTodoDoneName(_document.path, doneName);
        });
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

    private void addRemoveItems(final String prefix, final GsCallback.r1<Collection<String>, List<TodoTxtTask>> keyGetter) {
        final Set<String> all = new TreeSet<>(keyGetter.callback(TodoTxtTask.getAllTasks(_hlEditor.getText())));
        final TodoTxtTask additional = new TodoTxtTask(_appSettings.getTodotxtAdditionalContextsAndProjects());
        all.addAll(keyGetter.callback(Collections.singletonList(additional)));

        final Set<String> current = new HashSet<>(keyGetter.callback(TodoTxtTask.getSelectedTasks(_hlEditor)));

        final boolean append = _appSettings.isTodoAppendProConOnEndEnabled();

        MarkorDialogFactory.showUpdateItemsDialog(getActivity(), R.string.insert_context, all, current,
                updated -> {
                    final TextViewUtils.ChunkedEditable chunk = TextViewUtils.ChunkedEditable.wrap(_hlEditor.getText());
                    for (final String item : GsCollectionUtils.setDiff(current, updated)) {
                        removeItem(chunk, prefix + item);
                    }
                    for (final String item : GsCollectionUtils.setDiff(updated, current)) {
                        insertUniqueItem(chunk, prefix + item, append);
                    }
                    chunk.applyChanges();
                });
    }

    private static void removeItem(final Editable editable, final String item) {
        runRegexReplaceAction(
                editable,
                // In the middle - replace with space
                new ReplacePattern(String.format("\\s\\Q%s\\E\\s", item), " "),
                // In the end - remove
                new ReplacePattern(String.format("\\s\\Q%s\\E$", item), "")
        );
    }

    private static void insertUniqueItem(final Editable editable, final String item, final boolean append) {

        // Pattern to match <space><literal string><space OR end of line>
        // i.e. to check if a word is present in the line
        final Pattern pattern = Pattern.compile(String.format("\\s\\Q%s\\E(:?\\s|$)", item));
        final String lines = TextViewUtils.getSelectedLines(editable);
        // Multiline or setting
        if (append || lines.contains("\n")) {
            runRegexReplaceAction(
                    editable,
                    // Replace existing item with itself. i.e. do nothing
                    new ReplacePattern(pattern, "$0"),
                    // Append to end
                    new ReplacePattern("\\s*$", " " + item)
            );
        } else if (!pattern.matcher(lines).find()) {
            insertInline(editable, item);
        }
    }

    private void trimLeadingWhiteSpace() {
        runRegexReplaceAction("^\\s*", "");
    }

    private static void insertInline(final Editable editable, String thing) {
        final int[] sel = TextViewUtils.getSelection(editable);
        if (sel[0] < 0) {
            return;
        }

        if (sel[0] > 0) {
            final char before = editable.charAt(sel[0] - 1);
            if (before != ' ' && before != '\n') {
                thing = " " + thing;
            }
        }
        if (sel[1] < editable.length()) {
            final char after = editable.charAt(sel[1]);
            if (after != ' ' && after != '\n') {
                thing = thing + " ";
            }
        }
        editable.replace(sel[0], sel[1], thing);
    }

    private void setPriority(char priority) {
        ArrayList<ReplacePattern> patterns = new ArrayList<>();
        if (priority == TodoTxtTask.PRIORITY_NONE) {
            patterns.add(new ReplacePattern(TodoTxtTask.PATTERN_PRIORITY_ANY, ""));
        } else {
            final String _priority = String.format("(%c) ", priority);
            patterns.add(new ReplacePattern(TodoTxtTask.PATTERN_PRIORITY_ANY, _priority));
            patterns.add(new ReplacePattern("^\\s*", _priority));
        }
        runRegexReplaceAction(patterns);
        trimLeadingWhiteSpace();
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
        final Editable text = _hlEditor.getText();
        final int[] sel = TextViewUtils.getSelection(text);
        if (text == null || sel[0] < 0) {
            return;
        }
        final String selStr = text.subSequence(sel[0], sel[1]).toString();
        final Calendar initDate = parseDateString(selStr, Calendar.getInstance());

        final DatePickerDialog.OnDateSetListener listener = (_view, year, month, day) -> {
            Calendar fmtCal = Calendar.getInstance();
            fmtCal.set(year, month, day);
            final String newDate = TodoTxtTask.DATEF_YYYY_MM_DD.format(fmtCal.getTime());
            text.replace(sel[0], sel[1], newDate);
        };

        new DateFragment()
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
        public DatePickerDialog onCreateDialog(final Bundle savedInstanceState) {
            super.onCreateDialog(savedInstanceState);

            final DatePickerDialog dialog = new DatePickerDialog(getContext(), _listener, _year, _month, _day);

            if (_message != null && !_message.isEmpty()) {
                dialog.setMessage(_message);
            }

            if (_extraListener != null && _extraLabel != null && !_extraLabel.isEmpty()) {
                dialog.setButton(DialogInterface.BUTTON_NEUTRAL, _extraLabel, _extraListener);
            }

            return dialog;
        }

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // Do not auto-recreate
            if (savedInstanceState != null) {
                dismiss();
            }
        }
    }
}