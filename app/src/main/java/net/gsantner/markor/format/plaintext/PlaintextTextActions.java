/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.plaintext;

import android.app.Activity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;

import net.gsantner.markor.R;
import net.gsantner.markor.format.general.CommonTextActions;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.hleditor.TextActions;

public class PlaintextTextActions extends TextActions {

    public PlaintextTextActions(Activity activity, Document document) {
        super(activity, document);
    }


    @Override
    public void appendTextActionsToBar(ViewGroup barLayout) {
        if (barLayout.getChildCount() == 0) {
            setBarVisible(barLayout, true);

            // Regular actions
            for (int[] actions : TMA_ACTIONS) {
                PlaintextTextActionImpl actionCallback = new PlaintextTextActionImpl(actions[0]);
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
    private static final int[][] TMA_ACTIONS = {
            {R.string.tmaid_common_checkbox_list, R.drawable.ic_check_box_black_24dp, R.string.check_list},
            {R.string.tmaid_common_unordered_list_hyphen, R.drawable.ic_list_black_24dp, R.string.unordered_list},
            {R.string.tmaid_common_ordered_list_number, R.drawable.ic_format_list_numbered_black_24dp, R.string.ordered_list},
            {R.string.tmaid_common_jump_to_bottom, CommonTextActions.ACTION_JUMP_BOTTOM_TOP_ICON, R.string.jump_to_bottom},
            {R.string.tmaid_common_delete_lines, CommonTextActions.ACTION_DELETE_LINES_ICON, R.string.delete_lines},
            {R.string.tmaid_common_open_link_browser, CommonTextActions.ACTION_OPEN_LINK_BROWSER__ICON, R.string.open_link},
            {R.string.tmaid_common_attach_something, R.drawable.ic_attach_file_black_24dp, R.string.attach},
            {R.string.tmaid_common_special_key, CommonTextActions.ACTION_SPECIAL_KEY__ICON, R.string.special_key},
            {R.string.tmaid_common_time, R.drawable.ic_access_time_black_24dp, R.string.date_and_time},
    };

    private class PlaintextTextActionImpl implements View.OnClickListener, View.OnLongClickListener {
        private int _action;

        PlaintextTextActionImpl(int action) {
            _action = action;
        }

        @Override
        public void onClick(View view) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            runAction(_context.getString(_action), false, null);
        }

        @Override
        public boolean onLongClick(View v) {
            String action = _context.getString(_action);
            switch (action) {
                case CommonTextActions.ACTION_OPEN_LINK_BROWSER: {
                    action = CommonTextActions.ACTION_SEARCH;
                    break;
                }
                case CommonTextActions.ACTION_SPECIAL_KEY: {
                    action = CommonTextActions.ACTION_JUMP_BOTTOM_TOP;
                    break;
                }
                case "tmaid_common_time": {
                    action = "tmaid_common_time_insert_timestamp";
                    break;
                }
            }
            return runAction(action, true, null);
        }
    }
}
