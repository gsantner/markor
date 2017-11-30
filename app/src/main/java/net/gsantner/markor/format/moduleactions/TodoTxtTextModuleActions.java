/*
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.format.moduleactions;

import android.app.Activity;
import android.text.Editable;
import android.view.View;
import android.view.ViewGroup;

import net.gsantner.markor.R;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.TmpDialog;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.opoc.format.todotxt.SttCommander;
import net.gsantner.opoc.format.todotxt.extension.SttTaskWithParserInfo;

import java.util.ArrayList;
import java.util.Date;
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
            for (int[] actions : KEYBOARD_REGULAR_ACTIONS_ICONS) {
                appendTextModuleActionToBar(barLayout, actions[0], new KeyboardRegularActionListener(KEYBOARD_REGULAR_ACTIONS[actions[1]]));
            }
        } else if (!AppSettings.get().isEditor_ShowTextmoduleBar()) {
            setBarVisible(barLayout, false);
        }
    }

    //
    //
    //

    private static final int[][] KEYBOARD_REGULAR_ACTIONS_ICONS = {
            {R.drawable.ic_date_range_black_24dp, 0},
            {R.drawable.ic_email_at_sign_24dp, 1},
            {R.drawable.ic_star_border_black_24dp, 2}
    };
    private static final String[] KEYBOARD_REGULAR_ACTIONS = {" " + SttCommander.DATEF_YYYY_MM_DD.format(new Date()) + " "
            , "context", "priority"};

    private class KeyboardRegularActionListener implements View.OnClickListener {
        String _action;

        KeyboardRegularActionListener(String action) {
            _action = action;
        }

        @Override
        public void onClick(View v) {
            if (_action.equals("context")) {
                List<String> exampleData = SttCommander.get().parseProjects(_document.getContent());
                List<String> selectedData = new ArrayList<>();
                selectedData.add("foss");
                TmpDialog.showTodoTxtContextDialog(_activity, exampleData, selectedData, (callbackPayload) -> {
                    String text = _hlEditor.getText().toString();

                    SttCommander sttCommander = SttCommander.get();
                    SttTaskWithParserInfo task = sttCommander.parseTask(_hlEditor.getText().toString(), _hlEditor.getSelectionStart());
                    sttCommander.insertContext(task, callbackPayload, SttCommander.get().lastParseTextStartOffset);
                    //_hlEditor.setText();
                });
                return;
            }

            if (_action.equals("priority")) {
                SttCommander.get().parseTask(_document.getContent(), _hlEditor.getSelectionEnd());

                return;
            }

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
            }
        }
    }
}
