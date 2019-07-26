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
import android.view.View;
import android.view.ViewGroup;

import net.gsantner.markor.R;
import net.gsantner.markor.format.general.CommonTextActions;
import net.gsantner.markor.format.general.DatetimeFormatDialog;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.hleditor.TextActions;
import net.gsantner.markor.util.AppSettings;

public class PlaintextTextActions extends TextActions {

    public PlaintextTextActions(Activity activity, Document document) {
        super(activity, document);
    }


    @Override
    public void appendTextActionsToBar(ViewGroup barLayout) {
        if (AppSettings.get().isEditor_ShowTextActionsBar() && barLayout.getChildCount() == 0) {
            setBarVisible(barLayout, true);

            // Regular actions
            for (int[] actions : ACTIONS_ICONS) {
                PlaintextTextActionImpl actionCallback = new PlaintextTextActionImpl(ACTIONS[actions[1]]);
                appendTextActionToBar(barLayout, actions[0], actionCallback, actionCallback);
            }
        } else if (!AppSettings.get().isEditor_ShowTextActionsBar()) {
            setBarVisible(barLayout, false);
        }
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    @Override
    public boolean runAction(String action, boolean modLongClick, String anotherArg) {
        if (modLongClick) {
            switch (action) {
                case CommonTextActions.ACTION_SPECIAL_KEY: {
                    new CommonTextActions(_activity, _hlEditor).runAction(CommonTextActions.ACTION_JUMP_BOTTOM_TOP);
                    return true;
                }
                case CommonTextActions.ACTION_OPEN_LINK_BROWSER: {
                    new CommonTextActions(_activity, _hlEditor).runAction(CommonTextActions.ACTION_SEARCH);
                    return true;
                }
            }
        }

        switch (action) {
            case "pick_datetime": {
                DatetimeFormatDialog.showDatetimeFormatDialog(_activity, _hlEditor);
                return true;
            }
            default: {
                if (runCommonTextAction(action)) {
                    return true;
                }
                break;
            }
        }
        return false;
    }

    //
    //
    //

    private static final int[][] ACTIONS_ICONS = {
            {CommonTextActions.ACTION_OPEN_LINK_BROWSER__ICON, 0},
            {CommonTextActions.ACTION_DELETE_LINES_ICON, 1},
            {CommonTextActions.ACTION_SPECIAL_KEY__ICON, 2},
            {CommonTextActions.ACTION_COLOR_PICKER_ICON, 3},
            {R.drawable.ic_access_time_black_24dp, 4},
    };
    private static final String[] ACTIONS = {
            CommonTextActions.ACTION_OPEN_LINK_BROWSER,
            CommonTextActions.ACTION_DELETE_LINES,
            CommonTextActions.ACTION_SPECIAL_KEY,
            CommonTextActions.ACTION_COLOR_PICKER,
            "pick_datetime",
    };

    private class PlaintextTextActionImpl implements View.OnClickListener, View.OnLongClickListener {
        private String _action;

        PlaintextTextActionImpl(String action) {
            _action = action;
        }

        @Override
        public void onClick(View view) {
            runAction(_action, false, null);
        }

        @Override
        public boolean onLongClick(View v) {
            return runAction(_action, true, null);
        }
    }
}
