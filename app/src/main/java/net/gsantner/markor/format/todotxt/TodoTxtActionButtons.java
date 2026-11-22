/*#######################################################
 *
 *   Maintained 2018-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
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
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import net.gsantner.markor.R;
import net.gsantner.markor.activity.DocumentEditAndViewFragment;
import net.gsantner.markor.format.ActionButtonBase;
import net.gsantner.markor.format.FormatRegistry;
import net.gsantner.markor.frontend.MarkorDialogFactory;
import net.gsantner.markor.frontend.textview.TextViewUtils;
import net.gsantner.markor.model.Document;
import net.gsantner.opoc.format.GsTextUtils;
import net.gsantner.opoc.util.GsCollectionUtils;
import net.gsantner.opoc.util.GsFileUtils;
import net.gsantner.opoc.wrapper.GsCallback;

import org.json.JSONObject;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TodoTxtActionButtons extends ActionButtonBase {

    private static final String LAST_SORT_ORDER_KEY = TodoTxtActionButtons.class.getCanonicalName() + "_last_sort_order_key";
    private static final Pattern LINE_BREAK = Pattern.compile("\\r?\\n");
    private String _previewRevision;
    private String _previewText;

    public TodoTxtActionButtons(@NonNull Context context, Document document) {
        super(context, document);
    }

    public void showPreview(final String text, final boolean print, final DocumentEditAndViewFragment fragment) {
        final TodoTxtTextConverter converter = FormatRegistry.CONVERTER_TODOTXT;
        if (print) {
            _previewRevision = null;
            _previewText = null;
            converter.convertMarkupShowInWebView(_document, text, getActivity(), _webView, true, false);
        } else if (_previewRevision == null) {
            _previewRevision = UUID.randomUUID().toString();
            _previewText = text;
            converter.showInteractivePreview(_document, getActivity(), _webView, text, _previewRevision);
        } else {
            updatePreviewRows(text, _previewText.equals(text) ? null : TodoTxtTextConverter.renderRows(getActivity(), text), fragment);
        }
    }

    private void updatePreviewRows(final String text, final String rows, final DocumentEditAndViewFragment fragment) {
        final String previous = _previewRevision;
        final String revision = rows == null ? previous : UUID.randomUUID().toString();
        final WebView webView = _webView;
        _previewRevision = revision;
        _previewText = text;
        final String update = "window.markorTodo && window.markorTodo.updateRows("
                + JSONObject.quote(previous) + "," + JSONObject.quote(revision) + "," + (rows == null ? "null" : JSONObject.quote(rows)) + ")";
        webView.evaluateJavascript(update, result -> {
            if (!"true".equals(result) && revision.equals(_previewRevision) && _webView == webView && fragment.isPreviewing(this)) {
                final Activity activity = getActivity();
                if (activity != null && !activity.isDestroyed()) {
                    FormatRegistry.CONVERTER_TODOTXT.showInteractivePreview(_document, activity, webView, _previewText, revision);
                }
            }
        });
    }

    public boolean handlePreviewAction(final String[] args, final DocumentEditAndViewFragment fragment) {
        if (args.length < 2 || !"todotxt-preview".equals(args[0])) {
            return false;
        }
        if (args.length < 3 || !isCurrentPreview(args[1], fragment)) {
            return true;
        }
        if (args.length == 3 && "sort".equals(args[2])) {
            showSortDialog(args[1], fragment);
            return true;
        }
        if (args.length != 4 && args.length != 5) {
            return true;
        }
        final int line = args[3] == null ? -1 : GsTextUtils.tryParseInt(args[3], -1);
        final Editable text = _hlEditor.getText();
        // The utility takes an offset from the end of the line, clamped to its start.
        final int start = TextViewUtils.getIndexFromLineOffset(text, line, Integer.MAX_VALUE);
        if (start < 0 || start >= text.length()) {
            return true;
        }
        final int lineEnd = TextViewUtils.getLineEnd(text, start);
        final int taskEnd = lineEnd > start && text.charAt(lineEnd - 1) == '\r' ? lineEnd - 1 : lineEnd;
        final String task = text.subSequence(start, taskEnd).toString();
        if (task.trim().isEmpty()) {
            return true;
        }
        if (args.length == 4 && "edit".equals(args[2])) {
            fragment.setViewModeVisibility(false);
            _hlEditor.post(() -> TextViewUtils.setSelectionAndShow(_hlEditor, start));
        } else if (args.length == 5 && "done".equals(args[2]) && ("true".equals(args[4]) || "false".equals(args[4]))) {
            final boolean done = Boolean.parseBoolean(args[4]);
            if (new TodoTxtTask(task).isDone() != done) {
                for (final ReplacePattern pattern : toggleDonePatterns(false)) {
                    if (pattern.matcher.reset(task).find()) {
                        final CharSequence replacement = pattern.replace();
                        if (!_hlEditor.replaceIfUnchanged(start, task, replacement)) {
                            break;
                        }
                        _previewText = text.toString();
                        final String row = TodoTxtTextConverter.renderRow(getActivity(), replacement.toString(), line);
                        _webView.evaluateJavascript("window.markorTodo.updateRow(" + JSONObject.quote(_previewRevision)
                                + "," + line + "," + JSONObject.quote(row) + ")", null);
                        break;
                    }
                }
            }
        }
        return true;
    }

    private boolean isCurrentPreview(final String revision, final DocumentEditAndViewFragment fragment) {
        return fragment.isPreviewing(this) && _webView != null && _hlEditor != null &&
               _previewRevision != null && _previewRevision.equals(revision) && _previewText.contentEquals(_hlEditor.getText());
    }

    private ReplacePattern[] toggleDonePatterns(final boolean allowLegacyWhitespace) {
        final String doneMark = "x" + (_appSettings.isTodoAddCompletionDateEnabled() ? (" " + TodoTxtTask.getToday()) : "") + " ";
        final String bodyWithPri = "(.*)(\\spri:([A-Z])(?=\\s|$))(.*)"; // +1 = pre, +2 = full tag, +3 = pri, +4 = post
        final String doneWithDate = "^([Xx]" + (allowLegacyWhitespace ? "\\s" : " ") + "(?:" + TodoTxtTask.PT_DATE + "\\s)?)";
        final String startingPriority = "^\\(([A-Z])\\)\\s";
        return new ReplacePattern[]{
                // Move the leading priority to a pri: tag when completing a task.
                new ReplacePattern(startingPriority + bodyWithPri, doneMark + "$2 pri:$1$5"),
                new ReplacePattern(startingPriority + "(.*)(\\s*)", doneMark + "$2 pri:$1"),
                // Restore the leading priority when reopening a task.
                new ReplacePattern(doneWithDate + bodyWithPri, "($4) $2$5"),
                new ReplacePattern(doneWithDate, ""),
                new ReplacePattern("^", doneMark)
        };
    }

    private void showSortDialog(final String revision, final DocumentEditAndViewFragment fragment) {
        final Activity activity = getActivity();
        MarkorDialogFactory.showSttSortDialogue(activity, (orderBy, descending) -> {
            if (fragment != null && !isCurrentPreview(revision, fragment)) {
                return;
            }
            final String source = _hlEditor.getText().toString();
            final String startingPreviewRevision = _previewRevision;
            new Thread(() -> {
                final String sorted = sortTaskText(source, orderBy, descending);
                final String rows = fragment != null && !source.equals(sorted) ? TodoTxtTextConverter.renderRows(activity, sorted) : "";
                activity.runOnUiThread(() -> {
                    if (activity.isDestroyed() || !Objects.equals(startingPreviewRevision, _previewRevision) ||
                        (fragment != null && (_webView == null || !fragment.isPreviewing(this)))) {
                        return;
                    }
                    final Editable text = _hlEditor.getText();
                    if (!source.contentEquals(text)) {
                        return;
                    }
                    _appSettings.setStringList(LAST_SORT_ORDER_KEY, Arrays.asList(orderBy, Boolean.toString(descending)));
                    if (_hlEditor.replaceIfUnchanged(0, source, sorted) && fragment != null) {
                        updatePreviewRows(text.toString(), rows, fragment);
                    }
                });
            }).start();
        });
    }

    private static String sortTaskText(final String source, final String orderBy, final boolean descending) {
        final List<TodoTxtTask> tasks = new ArrayList<>();
        final Matcher breaks = LINE_BREAK.matcher(source);
        int start = 0;
        while (breaks.find()) {
            tasks.add(new TodoTxtTask(source.substring(start, breaks.start())));
            start = breaks.end();
        }
        tasks.add(new TodoTxtTask(source.substring(start)));

        // Keep trailing blank lines and each original line separator in place.
        int end = tasks.size();
        while (end > 0 && tasks.get(end - 1).getLine().isEmpty()) {
            end--;
        }
        TodoTxtTask.sortTasks(tasks.subList(0, end), orderBy, descending);
        final StringBuilder sorted = new StringBuilder(source.length());
        breaks.reset();
        for (final TodoTxtTask task : tasks) {
            sorted.append(task.getLine());
            if (breaks.find()) {
                sorted.append(breaks.group());
            }
        }
        return sorted.toString();
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
                runRegexReplaceAction(toggleDonePatterns(true));
                return true;
            }
            case R.string.abid_todotxt_add_context: {
                addRemoveItems("@", R.string.insert_context, TodoTxtTask::getContexts);
                return true;
            }
            case R.string.abid_todotxt_add_project: {
                addRemoveItems("+", R.string.insert_project, TodoTxtTask::getProjects);
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
                showSortDialog(null, null);
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
                    setEditorTextAsync(sortTaskText(_hlEditor.getText().toString(), last.get(0), Boolean.parseBoolean(last.get(1))));
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
                if (new Document(doneFile, FormatRegistry.FORMAT_TODOTXT).saveContent(getActivity(), doneContents.toString())) {
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

    private void addRemoveItems(
            final String prefix,
            final int titleResId,
            final GsCallback.r1<Collection<String>, List<TodoTxtTask>> keyGetter
    ) {
        final Set<String> all = new TreeSet<>(keyGetter.callback(TodoTxtTask.getAllTasks(_hlEditor.getText())));
        final TodoTxtTask additional = new TodoTxtTask(_appSettings.getTodotxtAdditionalContextsAndProjects());
        all.addAll(keyGetter.callback(Collections.singletonList(additional)));

        final Set<String> current = new HashSet<>(keyGetter.callback(TodoTxtTask.getSelectedTasks(_hlEditor)));

        final boolean append = _appSettings.isTodoAppendProConOnEndEnabled();

        MarkorDialogFactory.showUpdateItemsDialog(getActivity(), titleResId, all, current,
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

            final Context context = requireContext();
            final int backgroundColor = ContextCompat.getColor(context, R.color.background);
            final int primaryTextColor = ContextCompat.getColor(context, R.color.primary_text);
            final DatePickerDialog dialog = new DatePickerDialog(
                    context,
                    R.style.Theme_AppCompat_DayNight_Dialog_Rounded,
                    _listener,
                    _year,
                    _month,
                    _day
            );
            styleDatePicker(dialog, backgroundColor, primaryTextColor);

            if (_message != null && !_message.isEmpty()) {
                dialog.setTitle(_message);
            }

            if (_extraListener != null && _extraLabel != null && !_extraLabel.isEmpty()) {
                dialog.setButton(DialogInterface.BUTTON_NEUTRAL, _extraLabel, _extraListener);
            }

            dialog.setOnShowListener(dialogInterface -> {
                styleDatePicker(dialog, backgroundColor, primaryTextColor);
                final Window win = dialog.getWindow();
                if (win != null) {
                    win.setBackgroundDrawableResource(R.drawable.rounded_corner_background);
                    win.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
                }
            });

            return dialog;
        }

        private static void styleDatePicker(final DatePickerDialog dialog, final int backgroundColor, final int primaryTextColor) {
            dialog.getDatePicker().setBackgroundColor(backgroundColor);

            setPlatformDatePickerViewColor(dialog, "date_picker_header", backgroundColor, primaryTextColor);
            setPlatformDatePickerViewColor(dialog, "date_picker_header_year", backgroundColor, primaryTextColor);
            setPlatformDatePickerViewColor(dialog, "date_picker_header_date", backgroundColor, primaryTextColor);
            setPlatformDatePickerViewColor(dialog, "date_picker_header_day", backgroundColor, primaryTextColor);
        }

        private static void setPlatformDatePickerViewColor(
                final DatePickerDialog dialog,
                final String name,
                final int backgroundColor,
                final int primaryTextColor
        ) {
            final int id = dialog.getContext().getResources().getIdentifier(name, "id", "android");
            final View view = id != 0 ? dialog.findViewById(id) : null;
            if (view != null) {
                view.setBackgroundColor(backgroundColor);
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(primaryTextColor);
                }
            }
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
