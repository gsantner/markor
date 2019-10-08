/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.markdown;

import android.app.Activity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;

import net.gsantner.markor.R;
import net.gsantner.markor.format.general.CommonTextActions;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.AttachImageOrLinkDialog;
import net.gsantner.markor.ui.hleditor.TextActions;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ContextUtils;

public class MarkdownTextActions extends TextActions {

    public MarkdownTextActions(Activity activity, Document document) {
        super(activity, document);
    }

    @Override
    public void appendTextActionsToBar(ViewGroup barLayout) {
        if (AppSettings.get().isEditor_ShowTextActionsBar() && barLayout.getChildCount() == 0) {
            setBarVisible(barLayout, true);
            for (int[] actions : TMA_ACTIONS) {
                MarkdownTextActionsImpl actionCallback = new MarkdownTextActionsImpl(actions[0]);
                appendTextActionToBar(barLayout, actions[1], actionCallback, actionCallback);
            }
        } else if (!AppSettings.get().isEditor_ShowTextActionsBar()) {
            setBarVisible(barLayout, false);
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
            {R.string.tmaid_common_checkbox_list, R.drawable.ic_check_box_black_24dp},
            {R.string.tmaid_common_unordered_list_hyphen, R.drawable.ic_list_black_24dp},
            {R.string.tmaid_markdown_bold, R.drawable.ic_format_bold_black_24dp},
            {R.string.tmaid_markdown_italic, R.drawable.ic_format_italic_black_24dp},
            {R.string.tmaid_common_delete_lines, CommonTextActions.ACTION_DELETE_LINES_ICON},
            {R.string.tmaid_common_open_link_browser, CommonTextActions.ACTION_OPEN_LINK_BROWSER__ICON},
            {R.string.tmaid_common_attach_something, R.drawable.ic_attach_file_black_24dp},
            {R.string.tmaid_common_special_key, CommonTextActions.ACTION_SPECIAL_KEY__ICON},
            {R.string.tmaid_common_time, R.drawable.ic_access_time_black_24dp},
            {R.string.tmaid_markdown_code_inline, R.drawable.ic_code_black_24dp},
            {R.string.tmaid_common_ordered_list_number, R.drawable.ic_format_list_numbered_black_24dp},
            {R.string.tmaid_markdown_quote, R.drawable.ic_format_quote_black_24dp},
            {R.string.tmaid_markdown_h1, R.drawable.format_header_1},
            {R.string.tmaid_markdown_h2, R.drawable.format_header_2},
            {R.string.tmaid_markdown_h3, R.drawable.format_header_3},
            {R.string.tmaid_markdown_horizontal_line, R.drawable.ic_more_horiz_black_24dp},
            {R.string.tmaid_markdown_strikeout, R.drawable.ic_format_strikethrough_black_24dp},
    };

    private class MarkdownTextActionsImpl implements View.OnClickListener, View.OnLongClickListener {
        private int _action;

        MarkdownTextActionsImpl(int action) {
            _action = action;
        }

        @Override
        public void onClick(View view) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            switch (_action) {
                case R.string.tmaid_markdown_quote: {
                    runMarkdownRegularPrefixAction("> ");
                    break;
                }
                case R.string.tmaid_markdown_h1: {
                    runMarkdownRegularPrefixAction("# ");
                    break;
                }
                case R.string.tmaid_markdown_h2: {
                    runMarkdownRegularPrefixAction("## ");
                    break;
                }
                case R.string.tmaid_markdown_h3: {
                    runMarkdownRegularPrefixAction("### ");
                    break;
                }
                /*case R.string.tmaid_common_unordered_list_hyphen: {
                    runMarkdownRegularPrefixAction("- ");
                    break;
                }*/
                case R.string.tmaid_markdown_bold: {
                    runMarkdownInlineAction("**");
                    break;
                }
                case R.string.tmaid_markdown_italic: {
                    runMarkdownInlineAction("_");
                    break;
                }
                case R.string.tmaid_markdown_strikeout: {
                    runMarkdownInlineAction("~~");
                    break;
                }
                case R.string.tmaid_markdown_code_inline: {
                    runMarkdownInlineAction("`");
                    break;
                }
                case R.string.tmaid_markdown_horizontal_line: {
                    runMarkdownInlineAction("----\n");
                    break;
                }
                case R.string.tmaid_markdown_insert_link:
                case R.string.tmaid_markdown_insert_image: {
                    AttachImageOrLinkDialog.showInsertImageOrLinkDialog(_action == R.string.tmaid_markdown_insert_image ? 2 : 3, _document.getFormat(), _activity, _hlEditor, _document.getFile());
                    break;
                }
                default: {
                    runCommonTextAction(_context.getString(_action));
                    break;
                }
            }
        }

        @Override
        public boolean onLongClick(View view) {
            switch (_action) {
                case R.string.tmaid_common_open_link_browser: {
                    new CommonTextActions(_activity, _hlEditor).runAction(CommonTextActions.ACTION_SEARCH);
                    return true;
                }
                case R.string.tmaid_common_special_key: {
                    new CommonTextActions(_activity, _hlEditor).runAction(CommonTextActions.ACTION_JUMP_BOTTOM_TOP);
                    return true;
                }
                case R.string.tmaid_markdown_insert_image: {
                    int pos = _hlEditor.getSelectionStart();
                    _hlEditor.getText().insert(pos, "<img style=\"width:auto;max-height: 256px;\" src=\"\" />");
                    _hlEditor.setSelection(pos + 48);
                    return true;
                }
            }
            return false;
        }
    }
}
