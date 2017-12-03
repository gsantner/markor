/*
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.format.moduleactions;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import net.gsantner.markor.R;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.SearchOrCustomTextDialogCreator;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.opoc.format.todotxt.SttCommander;
import net.gsantner.opoc.format.todotxt.SttTask;
import net.gsantner.opoc.format.todotxt.extension.SttTaskWithParserInfo;
import net.gsantner.opoc.util.Callback;

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
                appendTextModuleActionToBar(barLayout, actions[0], new KeyboardRegularActionListener(STT_INSERT_ACTIONS[actions[1]]));
            }
        } else if (!AppSettings.get().isEditor_ShowTextmoduleBar()) {
            setBarVisible(barLayout, false);
        }
    }

    //
    //
    //

    private static final int[][] STT_INSERT_ACTIONS_ICONS = {
            {R.drawable.ic_close_white_24dp, 0},
            {R.drawable.ic_email_at_sign_24dp, 1},
            {R.drawable.ic_local_offer_white_24dp, 2},
            {R.drawable.ic_star_border_black_24dp, 3},
            {R.drawable.ic_date_range_white_24dp, 4},
            //{R.drawable.ic_add_white_24dp, 5},
            //{R.drawable.ic_delete_white_24dp, 6},
    };
    private static final String[] STT_INSERT_ACTIONS = {
            "toggle_done",
            "add_context",
            "add_project",
            "set_priority",
            "insert_date",
            //"add_task",
            //"delete_task"
    };

    private class KeyboardRegularActionListener implements View.OnClickListener {
        String _action;

        KeyboardRegularActionListener(String action) {
            _action = action;
        }

        @Override
        public void onClick(View view) {
            final SttCommander sttcmd = SttCommander.get();
            final String origText = _hlEditor.getText().toString();
            final int origSelectionStart = _hlEditor.getSelectionStart();
            final SttTaskWithParserInfo origTask = sttcmd.parseTask(origText, origSelectionStart);

            final Callback.a1<SttTaskWithParserInfo> replaceOrigTaskWithTaskCallback = (newTask) -> {
                if (newTask != null) {
                    String out = sttcmd.regenerateText(origText, newTask);
                    _hlEditor.getText().replace(0, origText.length(), out);
                }
            };


            switch (_action) {
                case "toggle_done": {
                    origTask.setDone(!origTask.isDone());
                    origTask.setCompletionDate(SttCommander.getToday());
                    replaceOrigTaskWithTaskCallback.callback(origTask);
                    return;
                }
                case "add_context": {
                    SearchOrCustomTextDialogCreator.showSttContextDialog(_activity, sttcmd.parseContexts(origText), origTask.getContexts(), (callbackPayload) -> {
                        int offsetInLine = _as.isTodoAppendProConOnEndEnabled() ? origTask.getTaskLine().length() : origTask.getCursorOffsetInLine();
                        sttcmd.insertContext(origTask, callbackPayload, offsetInLine);
                        replaceOrigTaskWithTaskCallback.callback(origTask);
                    });
                    return;
                }
                case "add_project": {
                    SearchOrCustomTextDialogCreator.showSttProjectDialog(_activity, sttcmd.parseProjects(origText), origTask.getProjects(), (callbackPayload) -> {
                        int offsetInLine = _as.isTodoAppendProConOnEndEnabled() ? origTask.getTaskLine().length() : origTask.getCursorOffsetInLine();
                        sttcmd.insertProject(origTask, callbackPayload, offsetInLine);
                        replaceOrigTaskWithTaskCallback.callback(origTask);
                    });
                    return;
                }

                case "set_priority": {
                    SearchOrCustomTextDialogCreator.showPriorityDialog(_activity, origTask.getPriority(), (callbackPayload) -> {
                        origTask.setPriority((callbackPayload.length() == 1) ? callbackPayload.charAt(0) : SttTask.PRIORITY_NONE);
                        replaceOrigTaskWithTaskCallback.callback(origTask);
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
                    return;
                }
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
    }
}
