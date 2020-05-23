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
import android.support.annotation.StringRes;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.View;

import net.gsantner.markor.R;
import net.gsantner.markor.format.general.CommonTextActions;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.AttachImageOrLinkDialog;
import net.gsantner.markor.ui.SearchOrCustomTextDialogCreator;
import net.gsantner.markor.ui.hleditor.TextActions;
import net.gsantner.opoc.util.Callback;

import java.util.Arrays;
import java.util.List;

public class MarkdownTextActions extends TextActions {

    public MarkdownTextActions(Activity activity, Document document) {
        super(activity, document);
    }

    @Override
    public boolean runAction(String action, boolean modLongClick, String anotherArg) {
        return runCommonTextAction(action);
    }

    @Override
    protected ActionCallback getActionCallback(@StringRes int keyId) {
        return new MarkdownTextActionsImpl(keyId);
    }

    @Override
    protected @StringRes
    int getFormatActionsKey() {
        return R.string.pref_key__markdown__action_keys;
    }

    @Override
    public List<ActionItem> getActiveActionList() {

        final ActionItem[] TMA_ACTIONS = {
                new ActionItem(R.string.tmaid_common_checkbox_list, R.drawable.ic_check_box_black_24dp, R.string.check_list),
                new ActionItem(R.string.tmaid_common_unordered_list_char, R.drawable.ic_list_black_24dp, R.string.unordered_list),
                new ActionItem(R.string.tmaid_markdown_bold, R.drawable.ic_format_bold_black_24dp, R.string.bold),
                new ActionItem(R.string.tmaid_markdown_italic, R.drawable.ic_format_italic_black_24dp, R.string.italic),
                new ActionItem(R.string.tmaid_common_delete_lines, CommonTextActions.ACTION_DELETE_LINES_ICON, R.string.delete_lines),
                new ActionItem(R.string.tmaid_common_open_link_browser, CommonTextActions.ACTION_OPEN_LINK_BROWSER__ICON, R.string.open_link),
                new ActionItem(R.string.tmaid_common_attach_something, R.drawable.ic_attach_file_black_24dp, R.string.attach),
                new ActionItem(R.string.tmaid_common_special_key, CommonTextActions.ACTION_SPECIAL_KEY__ICON, R.string.special_key),
                new ActionItem(R.string.tmaid_common_time, R.drawable.ic_access_time_black_24dp, R.string.date_and_time),
                new ActionItem(R.string.tmaid_markdown_code_inline, R.drawable.ic_code_black_24dp, R.string.inline_code),
                new ActionItem(R.string.tmaid_common_ordered_list_number, R.drawable.ic_format_list_numbered_black_24dp, R.string.ordered_list),
                new ActionItem(R.string.tmaid_markdown_table_insert_columns, R.drawable.ic_view_module_black_24dp, R.string.table),
                new ActionItem(R.string.tmaid_markdown_quote, R.drawable.ic_format_quote_black_24dp, R.string.quote),
                new ActionItem(R.string.tmaid_markdown_h1, R.drawable.format_header_1, R.string.heading_1),
                new ActionItem(R.string.tmaid_markdown_h2, R.drawable.format_header_2, R.string.heading_2),
                new ActionItem(R.string.tmaid_markdown_h3, R.drawable.format_header_3, R.string.heading_3),
                new ActionItem(R.string.tmaid_markdown_horizontal_line, R.drawable.ic_more_horiz_black_24dp, R.string.horizontal_line),
                new ActionItem(R.string.tmaid_markdown_strikeout, R.drawable.ic_format_strikethrough_black_24dp, R.string.strikeout),
                new ActionItem(R.string.tmaid_common_accordion, R.drawable.ic_arrow_drop_down_black_24dp, R.string.accordion),
                new ActionItem(R.string.tmaid_common_indent, R.drawable.ic_format_indent_increase_black_24dp, R.string.indent),
                new ActionItem(R.string.tmaid_common_deindent, R.drawable.ic_format_indent_decrease_black_24dp, R.string.deindent),
        };

        return Arrays.asList(TMA_ACTIONS);
    }

    private class MarkdownTextActionsImpl extends ActionCallback {
        private int _action;

        MarkdownTextActionsImpl(int action) {
            _action = action;
        }

        @Override
        public void onClick(View view) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            switch (_action) {
                case R.string.tmaid_markdown_quote: {
                    runRegularPrefixAction("> ");
                    break;
                }
                case R.string.tmaid_markdown_h1: {
                    setHeadingAction(1);
                    break;
                }
                case R.string.tmaid_markdown_h2: {
                    setHeadingAction(2);
                    break;
                }
                case R.string.tmaid_markdown_h3: {
                    setHeadingAction(3);
                    break;
                }
                /*case R.string.tmaid_common_unordered_list_char: {
                    runRegularPrefixAction(_appSettings.getUnorderedListCharacter() + " ");
                    break;
                }*/
                case R.string.tmaid_markdown_bold: {
                    runInlineAction("**");
                    break;
                }
                case R.string.tmaid_markdown_italic: {
                    runInlineAction("_");
                    break;
                }
                case R.string.tmaid_markdown_strikeout: {
                    runInlineAction("~~");
                    break;
                }
                case R.string.tmaid_markdown_code_inline: {
                    runInlineAction("`");
                    break;
                }
                case R.string.tmaid_markdown_horizontal_line: {
                    runInlineAction("----\n");
                    break;
                }
                case R.string.tmaid_markdown_table_insert_columns: {
                    SearchOrCustomTextDialogCreator.showInsertTableRowDialog(_activity, false, callbackInsertTableRow);
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
                case R.string.tmaid_common_time: {
                    runCommonTextAction("tmaid_common_time_insert_timestamp");
                    return true;
                }
                case R.string.tmaid_markdown_table_insert_columns: {
                    SearchOrCustomTextDialogCreator.showInsertTableRowDialog(_activity, true, callbackInsertTableRow);
                    break;
                }
            }
            return false;
        }

        private final Callback.a2<Integer, Boolean> callbackInsertTableRow = (cols, isHeaderEnabled) -> {
            StringBuilder sb = new StringBuilder();
            _hlEditor.requestFocus();
            if (!_hlEditor.isCurrentLineEmpty()) {
                sb.append("\n");
            }
            for (int i = 0; i < cols - 1; i++) {
                sb.append("  | ");
            }
            if (isHeaderEnabled) {
                sb.append("\n");
                for (int i = 0; i < cols; i++) {
                    sb.append("---");
                    if (i < cols - 1) {
                        sb.append("|");
                    }
                }
            }
            _hlEditor.moveCursorToEndOfLine(0);
            _hlEditor.insertOrReplaceTextOnCursor(sb.toString());
            _hlEditor.moveCursorToBeginOfLine(0);
            if (isHeaderEnabled) {
                _hlEditor.simulateKeyPress(KeyEvent.KEYCODE_DPAD_UP);
            }
        };
    }


    /**
     * Set/unset ATX heading level on each selected line
     *
     * This routine will make the following conditional changes
     *
     * Line is heading of same level as requested -> remove heading
     * Line is heading of different level that that requested -> add heading of specified level
     * Line is not heading -> add heading of specified level
     *
     * @param level ATX heading level
     */
    protected void setHeadingAction(int level) {
        // Commonmark allows headings of level 1 - 6
        assert(level >= 1);
        assert(level <= 6);

        char[] headerChars = new char[level];
        Arrays.fill(headerChars, '#');
        String header = new String(headerChars) + ' ';

        ReplacePattern[] patterns = {
                // Remove extant heading of matching level (preserves leading space)
                // Commonmark allows up to 3 leading spaces
                new ReplacePattern(String.format("^(\\s{0,3})#{%d}\\s", level), "$1"),
                // Convert extant heading to requested level
                new ReplacePattern("^(\\s{0,3})(#{1,6})\\s", "$1" + header),
                // Add heading if not a heading
                new ReplacePattern("^", header),
        };

        runRegexReplaceAction(Arrays.asList(patterns));
    }
}
