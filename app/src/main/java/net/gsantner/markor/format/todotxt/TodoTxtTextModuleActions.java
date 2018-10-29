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
import android.view.View;
import android.view.ViewGroup;

import net.gsantner.markor.R;
import net.gsantner.markor.format.plaintext.CommonTextModuleActions;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.SearchOrCustomTextDialogCreator;
import net.gsantner.markor.ui.hleditor.TextModuleActions;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.opoc.format.todotxt.SttCommander;
import net.gsantner.opoc.format.todotxt.SttTask;
import net.gsantner.opoc.format.todotxt.extension.SttTaskWithParserInfo;
import net.gsantner.opoc.util.Callback;
import net.gsantner.opoc.util.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

//TODO
public class TodoTxtTextModuleActions extends TextModuleActions {

    public TodoTxtTextModuleActions(Activity activity, Document document) {
        super(activity, document);
    }

    @Override
    public void appendTextModuleActionsToBar(ViewGroup barLayout) {
        if (AppSettings.get().isEditor_ShowTextmoduleBar() && barLayout.getChildCount() == 0) {
            setBarVisible(barLayout, true);

            // Regular actions
            for (int[] actions : STT_INSERT_ACTIONS_ICONS) {
                TodoTxtTextActionsImpl callback = new TodoTxtTextActionsImpl(STT_INSERT_ACTIONS[actions[1]]);
                appendTextModuleActionToBar(barLayout, actions[0], callback, callback);
            }
        } else if (!AppSettings.get().isEditor_ShowTextmoduleBar()) {
            setBarVisible(barLayout, false);
        }
    }

    //
    //
    //

    private static final int[][] STT_INSERT_ACTIONS_ICONS = {
            {R.drawable.ic_close_black_24dp, 0},
            {R.drawable.ic_delete_black_24dp, 1},
            {R.drawable.gs_email_sign_black_24dp, 2},
            {R.drawable.ic_local_offer_black_24dp, 3},
            {R.drawable.ic_star_border_black_24dp, 4},
            {CommonTextModuleActions.ACTION_OPEN_LINK_BROWSER__ICON, 5},
            {CommonTextModuleActions.ACTION_SPECIAL_KEY__ICON, 6},
            //{R.drawable.ic_add_white_24dp, 5},
            {R.drawable.ic_archive_black_24dp, 7},
            {R.drawable.ic_date_range_black_24dp, 8},
    };
    private static final String[] STT_INSERT_ACTIONS = {
            "toggle_done",
            "delete_task",
            "add_context",
            "add_project",
            "set_priority",
            CommonTextModuleActions.ACTION_OPEN_LINK_BROWSER,
            CommonTextModuleActions.ACTION_SPECIAL_KEY,
            //"add_task",
            "archive_done_tasks",
            "insert_date",
            CommonTextModuleActions.ACTION_COLOR_PICKER,
    };

    private class TodoTxtTextActionsImpl implements View.OnClickListener, View.OnLongClickListener {
        String _action;

        TodoTxtTextActionsImpl(String action) {
            _action = action;
        }

        @SuppressWarnings("StatementWithEmptyBody")
        @Override
        public void onClick(View view) {
            final SttCommander sttcmd = SttCommander.get();
            final String origText = _hlEditor.getText().toString();
            final int origSelectionStart = _hlEditor.getSelectionStart();
            final SttTaskWithParserInfo origTask = sttcmd.parseTask(origText, origSelectionStart);

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
                case "toggle_done": {
                    origTask.setDone(!origTask.isDone());
                    origTask.setCompletionDate(SttCommander.getToday());
                    cbUpdateOrigTask.callback(origTask);
                    return;
                }
                case "add_context": {
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
                case "add_project": {
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

                case "set_priority": {
                    SearchOrCustomTextDialogCreator.showPriorityDialog(_activity, origTask.getPriority(), (callbackPayload) -> {
                        origTask.setPriority((callbackPayload.length() == 1) ? callbackPayload.charAt(0) : SttTask.PRIORITY_NONE);
                        cbUpdateOrigTask.callback(origTask);
                    });
                    return;
                }
                case "insert_date": {
                    _hlEditor.getText().insert(origSelectionStart, SttCommander.getToday());
                    return;
                }
                case "add_task": {
                    return;
                }
                case "delete_task": {
                    removeTasksBetweenIndexes(_hlEditor.getText(), _hlEditor.getSelectionStart(), _hlEditor.getSelectionEnd());
                    return;
                }
                case "archive_done_tasks": {
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
                                    doneFileContents = FileUtils.readTextFileFast(doneFile).trim();
                                }
                                doneFileContents += TextUtils.join("\n", move);
                                if (FileUtils.writeFile(doneFile, doneFileContents)) {
                                    // All went good
                                    _hlEditor.setText(TextUtils.join("\n", keep));
                                    int newIndex = _hlEditor.getText().toString().indexOf(newCursorTarget);
                                    if (newIndex < 0 || newIndex >= _hlEditor.length()) {
                                        newIndex = _hlEditor.length();
                                    }
                                    _hlEditor.setSelection(newIndex);
                                }
                            }
                        } else {
                            // Maybe show a nice message popup? :)
                        }
                    });
                    return;
                }
            }

            CommonTextModuleActions commonTextModuleActions = new CommonTextModuleActions(_activity, _document, _hlEditor);
            commonTextModuleActions.runAction(_action);

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
            final SttCommander sttcmd = SttCommander.get();
            final String origText = _hlEditor.getText().toString();
            final int origSelectionStart = _hlEditor.getSelectionStart();
            final SttTaskWithParserInfo origTask = sttcmd.parseTask(origText, origSelectionStart);

            switch (_action) {
                case "add_context": {
                    SearchOrCustomTextDialogCreator.showSttContextListDialog(_activity, sttcmd.parseContexts(origText), origTask.getContexts(), origText, (callbackPayload) -> {
                        int cursor = origText.indexOf(callbackPayload);
                        _hlEditor.setSelection(Math.min(_hlEditor.length(), Math.max(0, cursor)));
                    });
                    return true;
                }
                case "add_project": {
                    SearchOrCustomTextDialogCreator.showSttProjectListDialog(_activity, sttcmd.parseProjects(origText), origTask.getContexts(), origText, (callbackPayload) -> {
                        int cursor = origText.indexOf(callbackPayload);
                        _hlEditor.setSelection(Math.min(_hlEditor.length(), Math.max(0, cursor)));
                    });
                    return true;
                }
                case CommonTextModuleActions.ACTION_SPECIAL_KEY: {
                    new CommonTextModuleActions(_activity, _document, _hlEditor).runAction(CommonTextModuleActions.ACTION_JUMP_BOTTOM_TOP);
                    return true;
                }

                case CommonTextModuleActions.ACTION_OPEN_LINK_BROWSER: {
                    new CommonTextModuleActions(_activity, _document, _hlEditor).runAction(CommonTextModuleActions.ACTION_SEARCH);
                    return true;
                }
            }

            CommonTextModuleActions commonTextModuleActions = new CommonTextModuleActions(_activity, _document, _hlEditor);
            commonTextModuleActions.runAction(_action);


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
