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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.annotation.StringRes;
import android.view.KeyEvent;
import android.widget.Toast;

import net.gsantner.markor.R;
import net.gsantner.markor.format.AutoFormatter;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.AttachImageOrLinkDialog;
import net.gsantner.markor.ui.SearchOrCustomTextDialogCreator;
import net.gsantner.markor.ui.hleditor.TextActions;

import java.util.Arrays;
import java.util.List;

public class MarkdownTextActions extends TextActions {

    public MarkdownTextActions(Activity activity, Document document) {
        super(activity, document);
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
                new ActionItem(R.string.tmaid_common_delete_lines, R.drawable.ic_delete_black_24dp, R.string.delete_lines),
                new ActionItem(R.string.tmaid_common_open_link_browser, R.drawable.ic_open_in_browser_black_24dp, R.string.open_link),
                new ActionItem(R.string.tmaid_common_attach_something, R.drawable.ic_attach_file_black_24dp, R.string.attach),
                new ActionItem(R.string.tmaid_common_special_key, R.drawable.ic_keyboard_black_24dp, R.string.special_key),
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
                new ActionItem(R.string.tmaid_common_new_line_below, R.drawable.ic_baseline_keyboard_return_24, R.string.start_new_line_below),
                new ActionItem(R.string.tmaid_common_move_text_one_line_up, R.drawable.ic_baseline_arrow_upward_24, R.string.move_text_one_line_up),
                new ActionItem(R.string.tmaid_common_move_text_one_line_down, R.drawable.ic_baseline_arrow_downward_24, R.string.move_text_one_line_down),
                new ActionItem(R.string.tmaid_common_insert_snippet, R.drawable.ic_baseline_file_copy_24, R.string.insert_snippet),
        };

        return Arrays.asList(TMA_ACTIONS);
    }

    @Override
    public boolean onActionClick(final @StringRes int action) {
        switch (action) {
            case R.string.tmaid_markdown_quote: {
                runRegexReplaceAction(MarkdownReplacePatternGenerator.toggleQuote());
                return true;
            }
            case R.string.tmaid_markdown_h1: {
                runRegexReplaceAction(MarkdownReplacePatternGenerator.setOrUnsetHeadingWithLevel(1));
                return true;
            }
            case R.string.tmaid_markdown_h2: {
                runRegexReplaceAction(MarkdownReplacePatternGenerator.setOrUnsetHeadingWithLevel(2));
                return true;
            }
            case R.string.tmaid_markdown_h3: {
                runRegexReplaceAction(MarkdownReplacePatternGenerator.setOrUnsetHeadingWithLevel(3));
                return true;
            }
            case R.string.tmaid_common_unordered_list_char: {
                final String listChar = _appSettings.getUnorderedListCharacter();
                runRegexReplaceAction(MarkdownReplacePatternGenerator.replaceWithUnorderedListPrefixOrRemovePrefix(listChar));
                return true;
            }
            case R.string.tmaid_common_checkbox_list: {
                final String listChar = _appSettings.getUnorderedListCharacter();
                runRegexReplaceAction(MarkdownReplacePatternGenerator.toggleToCheckedOrUncheckedListPrefix(listChar));
                return true;
            }
            case R.string.tmaid_common_ordered_list_number: {
                runRegexReplaceAction(MarkdownReplacePatternGenerator.replaceWithOrderedListPrefixOrRemovePrefix());
                runRenumberOrderedListIfRequired();
                return true;
            }
            case R.string.tmaid_markdown_bold: {
                runInlineAction("**");
                return true;
            }
            case R.string.tmaid_markdown_italic: {
                runInlineAction("_");
                return true;
            }
            case R.string.tmaid_markdown_strikeout: {
                runInlineAction("~~");
                return true;
            }
            case R.string.tmaid_markdown_code_inline: {
                runInlineAction("`");
                return true;
            }
            case R.string.tmaid_markdown_horizontal_line: {
                runInlineAction("----\n");
                return true;
            }
            case R.string.tmaid_markdown_table_insert_columns: {
                SearchOrCustomTextDialogCreator.showInsertTableRowDialog(_activity, false, this::insertTableRow);
                return true;
            }
            case R.string.tmaid_markdown_insert_link:
            case R.string.tmaid_markdown_insert_image: {
                AttachImageOrLinkDialog.showInsertImageOrLinkDialog(action == R.string.tmaid_markdown_insert_image ? 2 : 3, _document.getFormat(), _activity, _hlEditor, _document.getFile());
                return true;
            }
            default: {
                return runCommonTextAction(action);
            }
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onActionLongClick(final @StringRes int action) {
        switch (action) {
            case R.string.tmaid_markdown_insert_image: {
                int pos = _hlEditor.getSelectionStart();
                _hlEditor.getText().insert(pos, "<img style=\"width:auto;max-height: 256px;\" src=\"\" />");
                _hlEditor.setSelection(pos + 48);
                return true;
            }
            case R.string.tmaid_markdown_table_insert_columns: {
                SearchOrCustomTextDialogCreator.showInsertTableRowDialog(_activity, true, this::insertTableRow);
                return true;
            }
            case R.string.tmaid_markdown_code_inline: {
                _hlEditor.withAutoFormatDisabled(() -> {
                    final int c = _hlEditor.setSelectionExpandWholeLines();
                    _hlEditor.getText().insert(_hlEditor.getSelectionStart(), "\n```\n");
                    _hlEditor.getText().insert(_hlEditor.getSelectionEnd(), "\n```\n");
                    _hlEditor.setSelection(c + "\n```\n".length());
                });
                Toast.makeText(_activity, R.string.code_block, Toast.LENGTH_SHORT).show();
                return true;
            }
            default: {
                return runCommonLongPressTextActions(action);
            }
        }
    }

    private void insertTableRow(int cols, boolean isHeaderEnabled) {
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
    }

    @Override
    public boolean runTitleClick() {
        SearchOrCustomTextDialogCreator.showHeadlineDialog(MarkdownReplacePatternGenerator.PREFIX_ATX_HEADING.toString(), _activity, _hlEditor);
        return true;
    }

    @Override
    protected void renumberOrderedList(final int position) {
        AutoFormatter.renumberOrderedList(_hlEditor.getText(), position, MarkdownAutoFormat.getPrefixPatterns());
    }
}
