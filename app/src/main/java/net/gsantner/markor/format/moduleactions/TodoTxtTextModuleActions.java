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
import android.widget.Toast;

import net.gsantner.markor.R;
import net.gsantner.markor.format.highlighter.todotxt.TodoTxtAutoFormat;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.TmpDialog;
import net.gsantner.markor.util.AppSettings;

import java.util.Date;

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
            {R.drawable.ic_email_at_sign_24dp, 1}
    };
    private static final String[] KEYBOARD_REGULAR_ACTIONS = {" " + TodoTxtAutoFormat.SDF_YYYY_MM_DD.format(new Date()) + " "
            , "context"};

    private class KeyboardRegularActionListener implements View.OnClickListener {
        String _action;

        KeyboardRegularActionListener(String action) {
            _action = action;
        }

        @Override
        public void onClick(View v) {
            if (_action.equals("context")) {
                String[] exampleData = new String[]{"home", "electronics", "fun", "foss"};
                String[] selectedData = new String[]{"foss"};
                TmpDialog.showTodoTxtContextDialog(_activity, exampleData, selectedData,
                        (callbackPayload) -> Toast.makeText(_activity, callbackPayload + " selected. work in progress.", Toast.LENGTH_SHORT).show());
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
