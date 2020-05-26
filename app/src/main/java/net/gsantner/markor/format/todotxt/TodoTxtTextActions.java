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
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextUtils;
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
import net.gsantner.opoc.format.todotxt.SttCommander;
import net.gsantner.opoc.format.todotxt.SttTask;
import net.gsantner.opoc.format.todotxt.extension.SttTaskWithParserInfo;
import net.gsantner.opoc.util.Callback;
import net.gsantner.opoc.util.FileUtils;
import net.gsantner.opoc.util.StringUtils;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.gsantner.opoc.format.todotxt.SttCommander.DATEF_YYYY_MM_DD;

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

        final ActionItem[] TMA_ACTIONS = {
                new ActionItem(R.string.tmaid_todotxt_toggle_done, R.drawable.ic_check_box_black_24dp, R.string.toggle_done),
                new ActionItem(R.string.tmaid_todotxt_add_context, R.drawable.gs_email_sign_black_24dp, R.string.add_context),
                new ActionItem(R.string.tmaid_todotxt_add_project, R.drawable.ic_local_offer_black_24dp, R.string.add_project),
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
            final SttCommander sttcmd = SttCommander.get();
            final String origText = _hlEditor.getText().toString();
            final int origSelectionStart = _hlEditor.getSelectionStart();
            final SttTaskWithParserInfo origTask = sttcmd.parseTask(origText, origSelectionStart);
            final CommonTextActions commonTextActions = new CommonTextActions(_activity, _hlEditor);

            final Callback.a1<SttTaskWithParserInfo> cbUpdateOrigTask = (updatedTask) -> {
                if (updatedTask != null) {
                    SttCommander.SttTasksInTextRange rangeInfo = sttcmd.findTasksBetweenIndex(origText, origTask.getLineOffsetInText(), origTask.getLineOffsetInText());
                    Editable editable = _hlEditor.getText();
                    rangeInfo.startIndex = Math.max(rangeInfo.startIndex, 0);
                    rangeInfo.endIndex = Math.max(rangeInfo.endIndex, 0);
                    try {
                        editable.delete(rangeInfo.startIndex, rangeInfo.endIndex);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    sttcmd.regenerateTaskLine(updatedTask);
                    editable.insert(rangeInfo.startIndex, updatedTask.getTaskLine() + "\n");

                    // Try to figure out new cursor pos
                    int cursor = rangeInfo.startIndex + origTask.getCursorOffsetInLine();
                    if (cursor != 0) {
                        cursor += _hlEditor.getText().length() - origText.length(); // difference
                    }
                    if (cursor == _hlEditor.getText().length()) {
                        cursor--; // Move to last char in text;
                    }
                    if (cursor >= 0 && cursor <= _hlEditor.getText().length()) {
                        _hlEditor.setSelection(cursor);
                    }
                }
            };


            switch (_action) {
                case R.string.tmaid_todotxt_toggle_done: {
                    origTask.setDone(!origTask.isDone());
                    origTask.setCompletionDate(SttCommander.getToday());
                    cbUpdateOrigTask.callback(origTask);
                    return;
                }
                case R.string.tmaid_todotxt_add_context: {
                    SearchOrCustomTextDialogCreator.showSttContextDialog(_activity, sttcmd.parseContexts(origText), origTask.getContexts(), (callbackPayload) -> {
                        int offsetInLine = _appSettings.isTodoAppendProConOnEndEnabled() ? origTask.getTaskLine().length() : origTask.getCursorOffsetInLine();
                        sttcmd.insertContext(origTask, callbackPayload, offsetInLine);
                        cbUpdateOrigTask.callback(origTask);
                        if (_appSettings.isTodoAppendProConOnEndEnabled()) {
                            int cursor = _hlEditor.getSelectionStart() - callbackPayload.length() - 2;
                            _hlEditor.setSelection(Math.min(_hlEditor.length(), Math.max(0, cursor)));
                        }
                    });
                    return;
                }
                case R.string.tmaid_todotxt_add_project: {
                    SearchOrCustomTextDialogCreator.showSttProjectDialog(_activity, sttcmd.parseProjects(origText), origTask.getProjects(), (callbackPayload) -> {
                        int offsetInLine = _appSettings.isTodoAppendProConOnEndEnabled() ? origTask.getTaskLine().length() : origTask.getCursorOffsetInLine();
                        sttcmd.insertProject(origTask, callbackPayload, offsetInLine);
                        cbUpdateOrigTask.callback(origTask);
                        if (_appSettings.isTodoAppendProConOnEndEnabled()) {
                            int cursor = _hlEditor.getSelectionStart() - callbackPayload.length() - 2;
                            _hlEditor.setSelection(Math.min(_hlEditor.length(), Math.max(0, cursor)));
                        }
                    });
                    return;
                }

                case R.string.tmaid_todotxt_priority: {
                    SearchOrCustomTextDialogCreator.showPriorityDialog(_activity, origTask.getPriority(), (callbackPayload) -> {
                        origTask.setPriority((callbackPayload.length() == 1) ? callbackPayload.charAt(0) : SttTask.PRIORITY_NONE);
                        cbUpdateOrigTask.callback(origTask);
                    });
                    return;
                }
                case R.string.tmaid_todotxt_current_date: {
                    updateOrInsertSelectedDate();
                    return;
                }
                case R.string.tmaid_common_delete_lines: {
                    removeTasksBetweenIndexes(_hlEditor.getText(), _hlEditor.getSelectionStart(), _hlEditor.getSelectionEnd());
                    return;
                }
                case R.string.tmaid_todotxt_archive_done_tasks: {
                    SearchOrCustomTextDialogCreator.showSttArchiveDialog(_activity, (callbackPayload) -> {
                        // Don't do parse tasks in this case, performance wise
                        ArrayList<String> keep = new ArrayList<>();
                        ArrayList<String> move = new ArrayList<>();

                        String newCursorTarget = origTask.getTaskLine();
                        if (origTask.isDone()) {
                            int pos = origTask.getLineOffsetInText() + origTask.getTaskLine().length() + 1;
                            while (pos < origText.length()) {
                                SttTaskWithParserInfo task = sttcmd.parseTask(origText, pos);
                                if (!task.isDone()) {
                                    newCursorTarget = task.getTaskLine();
                                    break;
                                }
                                pos += task.getTaskLine().length() + 1;
                            }
                        }

                        for (String task : origText.split("\n")) {
                            if (task.startsWith("x ")) {
                                move.add(task);
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
                                }
                                doneFileContents += TextUtils.join("\n", move).trim() + "\n";

                                // Write to do done file
                                if (DocumentIO.saveDocument(new Document(doneFile), doneFileContents, new ShareUtil(_activity), getContext())) {
                                    // All went good
                                    _hlEditor.setText(TextUtils.join("\n", keep));
                                    int newIndex = _hlEditor.getText().toString().indexOf(newCursorTarget);
                                    if (newIndex < 0 || newIndex >= _hlEditor.length()) {
                                        newIndex = _hlEditor.length();
                                    }
                                    _hlEditor.setSelection(newIndex);
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
                            ArrayList<SttTaskWithParserInfo> tasks = SttCommander.parseTasksFromTextWithParserInfo(origText);
                            SttCommander.sortTasks(tasks, orderBy, descending);
                            setEditorTextAsync(SttCommander.tasksToString(tasks));
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

            /*
            if (_hlEditor.hasSelection()) {
                String text = _hlEditor.getText().toString();
                int selectionStart = _hlEditor.getSelectionStart();
                int selectionEnd = _hlEditor.getSelectionEnd();

                _hlEditor.getText().insert(selectionStart, _action);
            } else {
                //Condition for Empty Selection. Should insert the action at the start of the line
                int cursor = _hlEditor.getSelectionStart();
                int i = cursor - 1;
                Editable s = _hlEditor.getText();
                s.insert(cursor, _action);
            }*/
        }

        @Override
        public boolean onLongClick(View v) {
            if (_hlEditor.getText() == null) {
                return false;
            }
            final SttCommander sttcmd = SttCommander.get();
            final String origText = _hlEditor.getText().toString();
            final int origSelectionStart = _hlEditor.getSelectionStart();
            final SttTaskWithParserInfo origTask = sttcmd.parseTask(origText, origSelectionStart);
            final CommonTextActions commonTextActions = new CommonTextActions(_activity, _hlEditor);

            switch (_action) {
                case R.string.tmaid_todotxt_add_context: {
                    SearchOrCustomTextDialogCreator.showSttContextListDialog(_activity, sttcmd.parseContexts(origText), origTask.getContexts(), origText, (callbackPayload) -> {
                        int cursor = origText.indexOf(callbackPayload);
                        _hlEditor.setSelection(Math.min(_hlEditor.length(), Math.max(0, cursor)));
                    });
                    return true;
                }
                case R.string.tmaid_todotxt_add_project: {
                    SearchOrCustomTextDialogCreator.showSttProjectListDialog(_activity, sttcmd.parseProjects(origText), origTask.getContexts(), origText, (callbackPayload) -> {
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
                    updateOrInsertTagDate("due", 3);
                    return true;
                }
            }
            return false;
        }
    }


    // Removes all lines that are between first and second index param
    // These can be anywhere in a line and will expand to line start and ending
    private static List<SttTaskWithParserInfo> removeTasksBetweenIndexes(Editable editable, int indexSomewhereInLineStart, int indexSomewhereInLineEnd) {
        int len = editable.length();
        final SttCommander.SttTasksInTextRange found = SttCommander.get()
                .findTasksBetweenIndex(editable.toString(), indexSomewhereInLineStart, indexSomewhereInLineEnd);

        // Finally delete
        if (found.startIndex >= 0 && found.startIndex < len && found.endIndex >= 0 && found.endIndex <= len) {
            editable.delete(found.startIndex, found.endIndex);
            return found.tasks;
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * Insert or update a date
     *
     * This routine checks if the highlighted text matches the date format (`YYYY-mm-dd`).
     * If a date is found, it is parsed, and displayed to the user to be updated.
     *
     * If no date is found, the user selected date will be inserted at the cursor.
     */
    protected void updateOrInsertSelectedDate() {

        final int[] selection = StringUtils.getSelection(_hlEditor);
        final Editable text = _hlEditor.getText();
        final CharSequence dateText = text.subSequence(selection[0], selection[1]);

        Calendar calendar = Calendar.getInstance();

        // Parse selection for date
        try {
            final Matcher match = SttCommander.PATTERN_IS_DATE.matcher(dateText);
            if (match.find()) calendar.setTime(DATEF_YYYY_MM_DD.parse(match.group(0)));
        } catch (ParseException e) {
            // Regex failed?; should not be here
            e.printStackTrace();
        }

        DatePickerDialog.OnDateSetListener listener = (view, year, month, day) -> {
            Calendar fmtCal = Calendar.getInstance();
            fmtCal.set(year, month, day);
            String date = DATEF_YYYY_MM_DD.format(fmtCal.getTime());
            text.replace(selection[0], selection[1], date);
        };

        DateFragment dateFragment = new DateFragment()
                .setActivity(_activity)
                .setListener(listener)
                .setCalendar(calendar);

        dateFragment.show(((FragmentActivity) _activity).getSupportFragmentManager(), "dateFragment");
    }

    /**
     * This routine searches the current line for a date of in the format 'key:YYYY-mm-dd'.
     * If found, this date is parsed and presented to the user to be updated.
     *
     * If no such date is found, the user is presented with a dialog to enter a date, and
     * 'key:YYYY-mm-dd' is appended to the line
     *
     * @param key key to search for
     * @param offset if not existing date found, today's date is offset by this amount
     */
    protected void updateOrInsertTagDate(final String key, final int offset) {

        final int[] selection = StringUtils.getSelection(_hlEditor);
        Editable text = _hlEditor.getText();

        final int lineStart = StringUtils.getLineStart(text, selection[0]);
        final int lineEnd = StringUtils.getLineEnd(text, selection[1]);
        String line = text.subSequence(lineStart, lineEnd).toString();

        Pattern pattern = Pattern.compile("(?:" + key + ":)(" + SttCommander.PT_DATE + ")");
        Matcher match = pattern.matcher(line);
        final boolean found = match.find();

        Calendar calendar = Calendar.getInstance();
        // Parse selection for date
        try {
            if (found) calendar.setTime(DATEF_YYYY_MM_DD.parse(match.group(0)));
            else calendar.add(Calendar.DAY_OF_MONTH, offset);
        } catch (ParseException e) {
            // Regex failed?; should not be here
            e.printStackTrace();
        }

        DatePickerDialog.OnDateSetListener listener = (view, year, month, day) -> {
            Calendar fmtCal = Calendar.getInstance();
            fmtCal.set(year, month, day);
            String keyDate = String.format("%s:%s", key, DATEF_YYYY_MM_DD.format(fmtCal.getTime()));
            String newline;
            if (found) {
                newline = match.replaceFirst(keyDate);
            } else {
                //Append with space, if needed
                newline = line + (line.endsWith(" ")? "" : " ") + keyDate;
            }
            text.replace(lineStart, lineEnd, newline);
        };

        DateFragment dateFragment = new DateFragment()
                .setActivity(_activity)
                .setListener(listener)
                .setCalendar(calendar);

        dateFragment.show(((FragmentActivity) _activity).getSupportFragmentManager(), "dateFragment");
    }

    /**
     * A DialogFragment to manage showing a DatePicker
     */
    public static class DateFragment extends DialogFragment {

        private DatePickerDialog.OnDateSetListener listener;
        private Activity activity;
        private int year;
        private int month;
        private int day;

        public DateFragment() {
            super();
            setCalendar(Calendar.getInstance());
        }

        public DateFragment setListener(DatePickerDialog.OnDateSetListener listener) {
            this.listener = listener;
            return this;
        }

        public DateFragment setActivity(Activity activity) {
            this.activity = activity;
            return this;
        }

        public DateFragment setYear(int year) {
            this.year = year;
            return this;
        }

        public DateFragment setMonth(int month) {
            this.month = month;
            return this;
        }

        public DateFragment setDay(int day) {
            this.day = day;
            return this;
        }

        public DateFragment setCalendar(Calendar calendar) {
            setYear(calendar.get(Calendar.YEAR));
            setMonth(calendar.get(Calendar.MONTH));
            setDay(calendar.get(Calendar.DAY_OF_MONTH));
            return this;
        }

        @Override
        public Dialog onCreateDialog (Bundle savedInstanceState){
            return new DatePickerDialog(activity, listener, year, month, day);
        }
    }
}
