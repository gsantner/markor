/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.asciidoc;

import android.content.Context;

// used in markdown
// TODO: check, if it can be removed finaly
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.gsantner.markor.R;
import net.gsantner.markor.format.ActionButtonBase;
import net.gsantner.markor.format.asciidoc.AsciidocReplacePatternGenerator;
import net.gsantner.markor.frontend.textview.AutoTextFormatter;
import net.gsantner.markor.model.Document;

// used in markdown
// TODO: check, if it can be removed finaly
import net.gsantner.markor.frontend.AttachLinkOrFileDialog;
import net.gsantner.markor.frontend.MarkorDialogFactory;
import net.gsantner.markor.frontend.textview.TextViewUtils;


import java.util.Arrays;
import java.util.List;

public class AsciidocActionButtons extends ActionButtonBase {

    public AsciidocActionButtons(@NonNull Context context, Document document) {
        super(context, document);
    }

    @Override
    public List<ActionItem> getActiveActionList() {
        final ActionItem[] TMA_ACTIONS = {
                new ActionItem(R.string.abid_asciidoc_checkbox_list, R.drawable.ic_check_box_black_24dp, R.string.check_list),
                new ActionItem(R.string.abid_asciidoc_unordered_list_char, R.drawable.ic_list_black_24dp, R.string.unordered_list),
                new ActionItem(R.string.abid_asciidoc_ordered_list_char, R.drawable.ic_format_list_numbered_black_24dp, R.string.ordered_list),

                new ActionItem(R.string.abid_common_special_key, R.drawable.ic_keyboard_black_24dp, R.string.special_key),
                // similar to abid_common_special_key, but separate menu
                new ActionItem(R.string.abid_asciidoc_special_key, R.drawable.asciidoc_icon_black_24dp, R.string.asciidoc_special_key),

                new ActionItem(R.string.abid_asciidoc_h1, R.drawable.format_header_1, R.string.heading_1),
                new ActionItem(R.string.abid_asciidoc_h2, R.drawable.format_header_2, R.string.heading_2),
                new ActionItem(R.string.abid_asciidoc_h3, R.drawable.format_header_3, R.string.heading_3),
                new ActionItem(R.string.abid_asciidoc_h4, R.drawable.format_header_4, R.string.heading_4),
                new ActionItem(R.string.abid_asciidoc_h5, R.drawable.format_header_5, R.string.heading_5),

                new ActionItem(R.string.abid_asciidoc_bold, R.drawable.ic_format_bold_black_24dp, R.string.bold),
                new ActionItem(R.string.abid_asciidoc_italic, R.drawable.ic_format_italic_black_24dp, R.string.italic),
                new ActionItem(R.string.abid_asciidoc_monospace, R.drawable.ic_code_black_24dp, R.string.inline_code),

                new ActionItem(R.string.abid_common_delete_lines, R.drawable.ic_delete_black_24dp, R.string.delete_lines),

                // TODO: these are plaintext action buttons, adapt for asciidoc
                new ActionItem(R.string.abid_common_open_link_browser, R.drawable.ic_open_in_browser_black_24dp, R.string.open_link),
                new ActionItem(R.string.abid_common_attach_something, R.drawable.ic_attach_file_black_24dp, R.string.attach),

                new ActionItem(R.string.abid_common_time, R.drawable.ic_access_time_black_24dp, R.string.date_and_time),
                // TODO: indent and deindent for lists are different in asciidoc
                new ActionItem(R.string.abid_asciidoc_indent, R.drawable.ic_format_indent_increase_black_24dp, R.string.indent),
                new ActionItem(R.string.abid_asciidoc_deindent, R.drawable.ic_format_indent_decrease_black_24dp, R.string.deindent),
                // TODO: adapt to AsciiDoc

                // block actions via abid_asciidoc_special_key:
                // new ActionItem(R.string.abid_asciidoc_block_quote, R.drawable.ic_format_quote_black_24dp, R.string.quote),
                // new ActionItem(R.string.abid_asciidoc_table_insert_columns, R.drawable.ic_view_module_black_24dp, R.string.table),

                new ActionItem(R.string.abid_asciidoc_break_thematic, R.drawable.ic_more_horiz_black_24dp, R.string.horizontal_line),
                new ActionItem(R.string.abid_asciidoc_linethrough, R.drawable.ic_format_strikethrough_black_24dp, R.string.strikeout),
                new ActionItem(R.string.abid_common_accordion, R.drawable.ic_arrow_drop_down_black_24dp, R.string.accordion),
                new ActionItem(R.string.abid_common_web_jump_to_table_of_contents, R.drawable.ic_list_black_24dp, R.string.table_of_contents, ActionItem.DisplayMode.VIEW),

                new ActionItem(R.string.abid_common_new_line_below, R.drawable.ic_baseline_keyboard_return_24, R.string.start_new_line_below),
                new ActionItem(R.string.abid_common_move_text_one_line_up, R.drawable.ic_baseline_arrow_upward_24, R.string.move_text_one_line_up),
                new ActionItem(R.string.abid_common_move_text_one_line_down, R.drawable.ic_baseline_arrow_downward_24, R.string.move_text_one_line_down),
                new ActionItem(R.string.abid_common_insert_snippet, R.drawable.ic_baseline_file_copy_24, R.string.insert_snippet),

                // new ActionItem(R.string.abid_asciidoc_insert_snippet, R.drawable.asciidoc_icon_black_24dp, R.string.insert_asciidoc_snippet),

                new ActionItem(R.string.abid_common_web_jump_to_very_top_or_bottom, R.drawable.ic_vertical_align_center_black_24dp, R.string.jump_to_bottom, ActionItem.DisplayMode.VIEW),
                new ActionItem(R.string.abid_common_view_file_in_other_app, R.drawable.ic_open_in_browser_black_24dp, R.string.open_with, ActionItem.DisplayMode.ANY),
                new ActionItem(R.string.abid_common_rotate_screen, R.drawable.ic_rotate_left_black_24dp, R.string.rotate, ActionItem.DisplayMode.ANY),

        };

        return Arrays.asList(TMA_ACTIONS);
    }

    @Override
    public boolean onActionClick(final @StringRes int action) {
        switch (action) {
            // TODO: still issues
            case R.string.abid_asciidoc_h1: {
                runRegexReplaceAction(AsciidocReplacePatternGenerator.setOrUnsetHeadingWithLevel(1));
                return true;
            }
            // TODO: still issues
            case R.string.abid_asciidoc_h2: {
                runRegexReplaceAction(AsciidocReplacePatternGenerator.setOrUnsetHeadingWithLevel(2));
                return true;
            }
            // TODO: still issues
            case R.string.abid_asciidoc_h3: {
                runRegexReplaceAction(AsciidocReplacePatternGenerator.setOrUnsetHeadingWithLevel(3));
                return true;
            }
            // TODO: still issues
            case R.string.abid_asciidoc_h4: {
                runRegexReplaceAction(AsciidocReplacePatternGenerator.setOrUnsetHeadingWithLevel(4));
                return true;
            }
            // TODO: still issues
            case R.string.abid_asciidoc_h5: {
                runRegexReplaceAction(AsciidocReplacePatternGenerator.setOrUnsetHeadingWithLevel(5));
                return true;
            }
            // TODO: not tested
            // https://docs.asciidoctor.org/asciidoc/latest/text/bold/
            case R.string.abid_asciidoc_bold: {
                runInlineAction("**");
                return true;
            }
            // TODO: not tested
            // __ vs _
            // https://docs.asciidoctor.org/asciidoc/latest/text/italic/
            case R.string.abid_asciidoc_italic: {
                runInlineAction("__");
                return true;
            }
            // TODO: not tested
            // https://docs.asciidoctor.org/asciidoc/latest/text/monospace/
            case R.string.abid_asciidoc_monospace: {
                runInlineAction("``");
                return true;
            }
            // TODO: not tested
            case R.string.abid_asciidoc_linethrough: {
                runInlineAction("~~");
                return true;
            }
            // // TODO: not tested
            // case R.string.abid_asciidoc_block_quote: {
            //     runInlineAction("\n____\n");
            //     return true;
            // }
            // TODO: not tested
            case R.string.abid_asciidoc_break_thematic: {
                runInlineAction("'''\n");
                return true;
            }
            // TODO: not tested
            case R.string.abid_asciidoc_break_page: {
                runInlineAction("<<<\n");
                return true;
            }
            
            // TODO: still issues
            case R.string.abid_asciidoc_checkbox_list: {
                final String listChar = "*";
                runRegexReplaceAction(AsciidocReplacePatternGenerator.toggleToCheckedOrUncheckedListPrefix(listChar));
                return true;
            }
            // TODO: still issues
            case R.string.abid_asciidoc_unordered_list_char: {
                final String listChar = "*";
                runRegexReplaceAction(AsciidocReplacePatternGenerator.replaceWithUnorderedListPrefixOrRemovePrefix(listChar));
                return true;
            }
            // TODO: still issues
            case R.string.abid_asciidoc_ordered_list_char: {
                final String listChar = ".";
                runRegexReplaceAction(AsciidocReplacePatternGenerator.replaceWithOrderedListPrefixOrRemovePrefix(listChar));
//                runRenumberOrderedListIfRequired();
                return true;
            }
            case R.string.abid_asciidoc_table_insert_columns: {
                MarkorDialogFactory.showInsertTableRowDialog(getActivity(), false, this::insertTableRow);
                return true;
            }
            // TODO: AsciiDoc has a different syntax
            case R.string.abid_asciidoc_insert_link:
            case R.string.abid_asciidoc_insert_image: {
                AttachLinkOrFileDialog.showInsertImageOrLinkDialog(action == R.string.abid_asciidoc_insert_image ? 2 : 3, _document.getFormat(), getActivity(), _hlEditor, _document.getFile());
                return true;
            }
            case R.string.abid_asciidoc_special_key: {
                runAsciidocSpecialKeyAction();
                return true;
            }
            default: {
                return runCommonAction(action);
            }
        }
    }

    private String rstr(@StringRes int resKey) {
        return getContext().getString(resKey);
    }
    // idea based on runSpecialKeyAction()
    private void runAsciidocSpecialKeyAction() {

        // Needed to prevent selection from being overwritten on refocus
        final int[] sel = TextViewUtils.getSelection(_hlEditor);
        _hlEditor.clearFocus();
        _hlEditor.requestFocus();
        _hlEditor.setSelection(sel[0], sel[1]);
        // showAsciidocSpecialKeyDialog is used instead of showSpecialKeyDialog
        MarkorDialogFactory.showAsciidocSpecialKeyDialog(getActivity(), (callbackPayload) -> {
            if (!_hlEditor.hasSelection() && _hlEditor.length() > 0) {
                _hlEditor.requestFocus();
            }
            // https://docs.asciidoctor.org/asciidoc/latest/blocks/delimited/
            // AsciiDoc actions
            if (callbackPayload.equals(rstr(R.string.asciidoc_block_quote))) {
            runInlineAction("\n____\n");
            } else if (callbackPayload.equals(rstr(R.string.asciidoc_block_comment))) {
            runInlineAction("\n////\n");
            } else if (callbackPayload.equals(rstr(R.string.asciidoc_block_example))) {
            runInlineAction("\n====\n");
            } else if (callbackPayload.equals(rstr(R.string.asciidoc_block_listing))) {
            runInlineAction("\n----\n");
            } else if (callbackPayload.equals(rstr(R.string.asciidoc_block_literal))) {
            runInlineAction("\n....\n");
            } else if (callbackPayload.equals(rstr(R.string.asciidoc_block_open))) {
            runInlineAction("\n--\n");
            } else if (callbackPayload.equals(rstr(R.string.asciidoc_block_sidebar))) {
            runInlineAction("\n****\n");
            } else if (callbackPayload.equals(rstr(R.string.asciidoc_block_table))) {
            runInlineAction("\n|===\n");
            } else if (callbackPayload.equals(rstr(R.string.asciidoc_block_pass))) {
            runInlineAction("\n++++\n");
            // // TODO: some inline roles
            // } else if (callbackPayload.equals(rstr(R.string.asciidoc_block_pass))) {
            // runInlineAction("\n++++\n");
            // } else if (callbackPayload.equals(rstr(R.string.asciidoc_block_pass))) {
            // runInlineAction("\n++++\n");
            // } else if (callbackPayload.equals(rstr(R.string.asciidoc_block_pass))) {
            // runInlineAction("\n++++\n");
            // } else if (callbackPayload.equals(rstr(R.string.asciidoc_block_pass))) {
            // runInlineAction("\n++++\n");
            // // TODO: break
            // } else if (callbackPayload.equals(rstr(R.string.asciidoc_break_thematic))) {
            // runInlineAction("\n++++\n");
            // } else if (callbackPayload.equals(rstr(R.string.asciidoc_break_page))) {
            // runInlineAction("\n++++\n");
            }
        });
    }
  
    // aus markdown übernommen, weil es weiter oben verwendet wird
    // TODO: muss geändert werden oder raus
    private void insertTableRow(int cols, boolean isHeaderEnabled) {
        StringBuilder sb = new StringBuilder();
        _hlEditor.requestFocus();

        // Append if current line empty
        final int[] sel = TextViewUtils.getLineSelection(_hlEditor);
        if (sel[0] != -1 && sel[0] == sel[1]) {
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
    protected @StringRes
    int getFormatActionsKey() {
        return R.string.pref_key__asciidoc__action_keys;
    }

    // @Override
    // protected void renumberOrderedList() {
    //     AutoTextFormatter.renumberOrderedList(_hlEditor.getText(), AsciidocReplacePatternGenerator.formatPatterns);
    // }
}
