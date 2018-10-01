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

import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.hleditor.TextModuleActions;
import net.gsantner.markor.util.AppSettings;

public class PlaintextModuleActions extends TextModuleActions {

    public PlaintextModuleActions(Activity activity, Document document) {
        super(activity, document);
    }


    @Override
    public void appendTextModuleActionsToBar(ViewGroup barLayout) {
        if (AppSettings.get().isEditor_ShowTextmoduleBar() && barLayout.getChildCount() == 0) {
            setBarVisible(barLayout, true);

            // Regular actions
            for (int[] actions : ACTIONS_ICONS) {
                PlaintextTextActionImpl actionCallback = new PlaintextTextActionImpl(ACTIONS[actions[1]]);
                appendTextModuleActionToBar(barLayout, actions[0], actionCallback, actionCallback);
            }
        } else if (!AppSettings.get().isEditor_ShowTextmoduleBar()) {
            setBarVisible(barLayout, false);
        }
    }

    //
    //
    //

    private static final int[][] ACTIONS_ICONS = {
            {CommonTextModuleActions.ACTION_OPEN_LINK_BROWSER__ICON, 0},
            {CommonTextModuleActions.ACTION_DELETE_LINES_ICON, 1},
            {CommonTextModuleActions.ACTION_SPECIAL_KEY__ICON, 2},
    };
    private static final String[] ACTIONS = {
            CommonTextModuleActions.ACTION_OPEN_LINK_BROWSER,
            CommonTextModuleActions.ACTION_DELETE_LINES,
            CommonTextModuleActions.ACTION_SPECIAL_KEY,
    };

    private class PlaintextTextActionImpl implements View.OnClickListener, View.OnLongClickListener {
        String _action;

        PlaintextTextActionImpl(String action) {
            _action = action;
        }

        @SuppressWarnings("StatementWithEmptyBody")
        @Override
        public void onClick(View view) {
            CommonTextModuleActions commonTextModuleActions = new CommonTextModuleActions(_activity, _document, _hlEditor);
            commonTextModuleActions.runAction(_action);
        }

        @Override
        public boolean onLongClick(View v) {
            switch (_action) {
                case CommonTextModuleActions.ACTION_SPECIAL_KEY: {
                    new CommonTextModuleActions(_activity, _document, _hlEditor).runAction(CommonTextModuleActions.ACTION_JUMP_BOTTOM_TOP);
                    return true;
                }

                case CommonTextModuleActions.ACTION_OPEN_LINK_BROWSER: {
                    new CommonTextModuleActions(_activity, _document, _hlEditor).runAction(CommonTextModuleActions.ACTION_SEARCH);
                    return true;
                }
            }
            return false;
        }
    }
}
