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
import android.text.Editable;
import android.text.TextUtils;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;

import net.gsantner.markor.R;
import net.gsantner.markor.format.general.CommonTextActions;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.SearchOrCustomTextDialogCreator;
import net.gsantner.markor.ui.hleditor.TextActions;
import net.gsantner.markor.util.DocumentIO;
import net.gsantner.markor.util.ShareUtil;
import net.gsantner.opoc.format.todotxt.SttCommander;
import net.gsantner.opoc.format.todotxt.SttTask;
import net.gsantner.opoc.format.todotxt.extension.SttTaskWithParserInfo;
import net.gsantner.opoc.util.Callback;
import net.gsantner.opoc.util.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//TODO
public class TodoTxtTextActions extends TextActions {

    public TodoTxtTextActions(Activity activity, Document document) {
        super(activity, document);
    }

    @Override
    public void appendTextActionsToBar(ViewGroup barLayout) {
        if (barLayout.getChildCount() == 0) {
            setBarVisible(barLayout, true);

            // Regular actions
            for (int[] actions : TMA_ACTIONS) {
                TodoTxtTextActionsImpl actionCallback = new TodoTxtTextActionsImpl(actions[0]);
                appendTextActionToBar(barLayout, actions[1], actions[2], actionCallback, actionCallback);
            }
        }
    }

    @Override
    public boolean runAction(String action, boolean modLongClick, String anotherArg) {
        return runCommonTextAction(action);
    }

    //
    //
    //


    // Mapping from action (string res) to icon (drawable res)
    private static final int[][] TMA_ACTIONS = {
            {R.string.tmaid_todotxt_toggle_done, R.drawable.ic_check_box_black_24dp, R.string.toggle_done},
            {R.string.tmaid_todotxt_add_context, R.drawable.gs_email_sign_black_24dp, R.string.add_context},
            {R.string.tmaid_todotxt_add_project, R.drawable.ic_local_offer_black_24dp, R.string.add_project},
            {R.string.tmaid_todotxt_priority, R.drawable.ic_star_border_black_24dp, R.string.priority},
            {R.string.tmaid_common_delete_lines, CommonTextActions.ACTION_DELETE_LINES_ICON, R.string.delete_lines},
            {R.string.tmaid_common_open_link_browser, CommonTextActions.ACTION_OPEN_LINK_BROWSER__ICON, R.string.open_link},
            {R.string.tmaid_common_attach_something, R.drawable.ic_attach_file_black_24dp, R.string.attach},
            {R.string.tmaid_common_special_key, CommonTextActions.ACTION_SPECIAL_KEY__ICON, R.string.special_key},
            {R.string.tmaid_todotxt_archive_done_tasks, R.drawable.ic_archive_black_24dp, R.string.archive_completed_tasks},
            {R.string.tmaid_todotxt_sort_todo, R.drawable.ic_sort_by_alpha_black_24dp, R.string.sort_alphabetically},
            {R.string.tmaid_todotxt_current_date, R.drawable.ic_date_range_black_24dp, R.string.current_date},
    };

    private class TodoTxtTextActionsImpl implements View.OnClickListener, View.OnLongClickListener {
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
                    _hlEditor.getText().insert(origSelectionStart, SttCommander.getToday());
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
                                if (DocumentIO.saveDocument(new Document(doneFile), doneFileContents, new ShareUtil(_activity))) {
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
                    });
                    return;
                }
                case R.string.tmaid_todotxt_sort_todo: {
                    SearchOrCustomTextDialogCreator.showSttSortDialogue(_activity, (orderBy, descending) -> new Thread() {
                        @Override
                        public void run() {
                            super.run();
                            ArrayList<SttTaskWithParserInfo> tasks = new ArrayList<>();
                            for (String task : origText.split("\n")) {
                                tasks.add(sttcmd.parseTask(task));
                            }
                            Collections.sort(tasks, new SttCommander.SttTaskSimpleComparator(orderBy, descending));
                            ArrayList<String> tasksStrings = new ArrayList<>();
                            for (SttTaskWithParserInfo task : tasks) {
                                tasksStrings.add(task.getTaskLine());
                            }
                            setEditorTextAsync(TextUtils.join("\n", tasksStrings));
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
                    _hlEditor.getText().insert(origSelectionStart, " due:" + SttCommander.getDaysFromToday(3));
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
}
