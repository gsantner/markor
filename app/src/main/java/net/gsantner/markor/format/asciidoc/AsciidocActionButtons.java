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

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.gsantner.markor.R;
import net.gsantner.markor.format.ActionButtonBase;
import net.gsantner.markor.frontend.AttachLinkOrFileDialog;
import net.gsantner.markor.frontend.MarkorDialogFactory;
import net.gsantner.markor.frontend.textview.TextViewUtils;
import net.gsantner.markor.model.Document;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class AsciidocActionButtons extends ActionButtonBase {

    public AsciidocActionButtons(@NonNull Context context, Document document) {
        super(context, document);
    }

    @Override
    public List<ActionItem> getActiveActionList() {
        final ActionItem[] TMA_ACTIONS = {
                new ActionItem(R.string.abid_asciidoc_checkbox_list,
                        R.drawable.ic_check_box_black_24dp, R.string.check_list),
                new ActionItem(
                        R.string.abid_asciidoc_unordered_list_char, R.drawable.ic_list_black_24dp,
                        R.string.unordered_list),
                new ActionItem(R.string.abid_asciidoc_ordered_list_char,
                        R.drawable.ic_format_list_numbered_black_24dp, R.string.ordered_list),

                new ActionItem(R.string.abid_asciidoc_indent_level,
                        R.drawable.ic_baseline_keyboard_double_arrow_right_24,
                        R.string.indent_level),
                new ActionItem(R.string.abid_asciidoc_deindent_level,
                        R.drawable.ic_baseline_keyboard_double_arrow_left_24,
                        R.string.deindent_level),

                new ActionItem(R.string.abid_common_indent,
                        R.drawable.ic_format_indent_increase_black_24dp, R.string.indent),
                new ActionItem(R.string.abid_common_deindent,
                        R.drawable.ic_format_indent_decrease_black_24dp, R.string.deindent),
                new ActionItem(R.string.abid_asciidoc_squarebrackets,
                        R.drawable.ic_baseline_data_array_24, R.string.squarebrackets),
                new ActionItem(R.string.abid_common_special_key, R.drawable.ic_keyboard_black_24dp,
                        R.string.special_key),
                // similar to abid_common_special_key, but separate menu
                new ActionItem(R.string.abid_asciidoc_special_key,
                        R.drawable.asciidoc_icon_black_24dp, R.string.asciidoc_special_key),

                new ActionItem(R.string.abid_common_insert_snippet,
                        R.drawable.ic_baseline_file_copy_24, R.string.insert_snippet),
                new ActionItem(R.string.abid_asciidoc_h1, R.drawable.format_header_1,
                        R.string.heading_1),
                new ActionItem(R.string.abid_asciidoc_h2, R.drawable.format_header_2,
                        R.string.heading_2),
                new ActionItem(R.string.abid_asciidoc_h3, R.drawable.format_header_3,
                        R.string.heading_3),
//                new ActionItem(R.string.abid_asciidoc_h4, R.drawable.format_header_4,
//                        R.string.heading_4),
//                new ActionItem(R.string.abid_asciidoc_h5, R.drawable.format_header_5,
//                        R.string.heading_5),

                new ActionItem(R.string.abid_asciidoc_bold, R.drawable.ic_format_bold_black_24dp,
                        R.string.bold),
                new ActionItem(R.string.abid_asciidoc_italic,
                        R.drawable.ic_format_italic_black_24dp, R.string.italic),
                new ActionItem(
                        R.string.abid_asciidoc_monospace, R.drawable.ic_code_black_24dp,
                        R.string.inline_code),
                new ActionItem(R.string.abid_asciidoc_underline,
                        R.drawable.ic_format_underlined_black_24dp, R.string.underline),
                new ActionItem(
                        R.string.abid_asciidoc_highlight, R.drawable.ic_highlight_black_24dp,
                        R.string.highlighted),
                new ActionItem(R.string.abid_asciidoc_linethrough,
                        R.drawable.ic_format_strikethrough_black_24dp, R.string.strikeout),
                new ActionItem(
                        R.string.abid_asciidoc_overline, R.drawable.ic_baseline_format_overline_24,
                        R.string.inline_code),
                new ActionItem(R.string.abid_asciidoc_superscript, R.drawable.ic_baseline_superscript_24,
                        R.string.inline_code),
                new ActionItem(R.string.abid_asciidoc_subscript,
                        R.drawable.ic_baseline_subscript_24
                        , R.string.inline_code),
                new ActionItem(
                        R.string.abid_asciidoc_break_thematic, R.drawable.ic_more_horiz_black_24dp,
                        R.string.horizontal_line),
                new ActionItem(R.string.abid_asciidoc_block_quote,
                        R.drawable.ic_format_quote_black_24dp, R.string.quote),
                // TODO: Implement later
//                new ActionItem(R.string.abid_common_web_jump_to_table_of_contents,
//                        R.drawable.ic_list_black_24dp, R.string.table_of_contents,
//                        ActionItem.DisplayMode.VIEW),
                // TODO: Implement Table Generator later
                // new ActionItem(R.string.abid_asciidoc_table_insert_columns, R.drawable
                // .ic_view_module_black_24dp, R.string.table),

                //similar to other formats:
                new ActionItem(R.string.abid_common_delete_lines, R.drawable.ic_delete_black_24dp,
                        R.string.delete_lines),
                new ActionItem(
                        R.string.abid_common_open_link_browser,
                        R.drawable.ic_open_in_browser_black_24dp,
                        R.string.open_link),
                // a different icon was used than in other formats, so that you can distinguish
                // it from opening in the browser
                new ActionItem(R.string.abid_common_view_file_in_other_app,
                        R.drawable.ic_baseline_open_in_new_24, R.string.open_with,
                        ActionItem.DisplayMode.ANY),
                new ActionItem(
                        R.string.abid_common_attach_something, R.drawable.ic_attach_file_black_24dp,
                        R.string.attach),
                new ActionItem(R.string.abid_common_time,
                        R.drawable.ic_access_time_black_24dp, R.string.date_and_time),
                new ActionItem(
                        R.string.abid_common_new_line_below,
                        R.drawable.ic_baseline_keyboard_return_24,
                        R.string.start_new_line_below),
                new ActionItem(
                        R.string.abid_common_move_text_one_line_up,
                        R.drawable.ic_baseline_arrow_upward_24,
                        R.string.move_text_one_line_up),
                new ActionItem(
                        R.string.abid_common_move_text_one_line_down,
                        R.drawable.ic_baseline_arrow_downward_24, R.string.move_text_one_line_down),
                new ActionItem(R.string.abid_common_web_jump_to_very_top_or_bottom,
                        R.drawable.ic_vertical_align_center_black_24dp, R.string.jump_to_bottom,
                        ActionItem.DisplayMode.VIEW),
                new ActionItem(
                        R.string.abid_common_rotate_screen, R.drawable.ic_rotate_left_black_24dp,
                        R.string.rotate, ActionItem.DisplayMode.ANY),

        };

        return Arrays.asList(TMA_ACTIONS);
    }

// indent deindent:

// implemented two different indent deindent

// a "normal" one for blank characters only
// another one that will change the level of headers and lists
// Pseudologics for the level changer:
// * indent level: the first character is added as a prefix
// * deindent level: the first character is removed, but only if something remains after
// '===' => '== ' => '= ' => '= ' (no further removal)

    @Override
    public boolean onActionClick(final @StringRes int action) {
        switch (action) {
            case R.string.abid_asciidoc_h1: {
                runRegexReplaceAction(
                        AsciidocReplacePatternGenerator.setOrUnsetHeadingWithLevel(1));
                return true;
            }
            case R.string.abid_asciidoc_h2: {
                runRegexReplaceAction(
                        AsciidocReplacePatternGenerator.setOrUnsetHeadingWithLevel(2));
                return true;
            }
            case R.string.abid_asciidoc_h3: {
                runRegexReplaceAction(
                        AsciidocReplacePatternGenerator.setOrUnsetHeadingWithLevel(3));
                return true;
            }
            case R.string.abid_asciidoc_h4: {
                runRegexReplaceAction(
                        AsciidocReplacePatternGenerator.setOrUnsetHeadingWithLevel(4));
                return true;
            }
            case R.string.abid_asciidoc_h5: {
                runRegexReplaceAction(
                        AsciidocReplacePatternGenerator.setOrUnsetHeadingWithLevel(5));
                return true;
            }

            case R.string.abid_asciidoc_indent_level: {
                runRegexReplaceAction(
                        AsciidocReplacePatternGenerator.indentLevel());
                return true;
            }
            case R.string.abid_asciidoc_deindent_level: {
                runRegexReplaceAction(
                        AsciidocReplacePatternGenerator.deindentLevel());
                return true;
            }

            // TODO: could be improved to keep level, will be done only on request to keep it simple for now
            case R.string.abid_asciidoc_checkbox_list: {
                final String listChar = "*";
                runRegexReplaceAction(
                        AsciidocReplacePatternGenerator.toggleToCheckedOrUncheckedListPrefix(
                                listChar));
                return true;
            }
            case R.string.abid_asciidoc_unordered_list_char: {
                final String listChar = "*";
                runRegexReplaceAction(
                        AsciidocReplacePatternGenerator.replaceWithUnorderedListPrefixOrRemovePrefix(
                                listChar));
                return true;
            }
            case R.string.abid_asciidoc_ordered_list_char: {
                final String listChar = ".";
                runRegexReplaceAction(
                        AsciidocReplacePatternGenerator.replaceWithOrderedListPrefixOrRemovePrefix(
                                listChar));
                return true;
            }

            // runAsciidocInlineAction works fine
            case R.string.abid_asciidoc_squarebrackets: {
                runAsciidocInlineAction("", "[", "]");
                return true;
            }
            // https://docs.asciidoctor.org/asciidoc/latest/text/bold/
            case R.string.abid_asciidoc_bold: {
                runAsciidocInlineAction("**", "", "");
                return true;
            }
            // __ vs _
            // https://docs.asciidoctor.org/asciidoc/latest/text/italic/
            // we use single and not double, because Markdown action buton also use single and
            // runInlineAction is adapted for this
            case R.string.abid_asciidoc_italic: {
                runAsciidocInlineAction("_", "", "");
                return true;
            }
            // https://docs.asciidoctor.org/asciidoc/latest/text/monospace/
            // we use single and not double, because Markdown action buton also use single and
            // runInlineAction is adapted for this
            case R.string.abid_asciidoc_monospace: {
                runAsciidocInlineAction("`", "", "");
                return true;
            }
            // https://docs.asciidoctor.org/asciidoc/latest/text/highlight/
            case R.string.abid_asciidoc_highlight: {
                runAsciidocInlineAction("#", "", "");
                return true;
            }

            // https://docs.asciidoctor.org/asciidoc/latest/text/text-span-built-in-roles/
            // roles are like highlight, but with a prefix like: [.underline]#underline me#
            case R.string.abid_asciidoc_underline: {
                runAsciidocInlineAction("#", "[.underline]", "");
                return true;
            }
            case R.string.abid_asciidoc_overline: {
                runAsciidocInlineAction("#", "[.overline]", "");
                return true;
            }
            case R.string.abid_asciidoc_linethrough: {
                runAsciidocInlineAction("#", "[.line-through]", "");
                return true;
            }
            case R.string.abid_asciidoc_nobreak: {
                runAsciidocInlineAction("#", "[.nobreak]", "");
                return true;
            }
            case R.string.abid_asciidoc_nowrap: {
                runAsciidocInlineAction("#", "[.nowrap]", "");
                return true;
            }
            case R.string.abid_asciidoc_prewrap: {
                runAsciidocInlineAction("#", "[.pre-wrap]", "");
                return true;
            }

            // https://docs.asciidoctor.org/asciidoc/latest/text/subscript-and-superscript/
            // ~ ^
            case R.string.abid_asciidoc_subscript: {
                runAsciidocInlineAction("~", "", "");
                return true;
            }
            case R.string.abid_asciidoc_superscript: {
                runAsciidocInlineAction("^", "", "");
                return true;
            }

            // https://docs.asciidoctor.org/asciidoc/latest/blocks/breaks/#page-breaks
            case R.string.abid_asciidoc_break_thematic: {
                runAsciidocInlineAction("'''\n", "", "");
                return true;
            }
            case R.string.abid_asciidoc_break_page: {
                runAsciidocInlineAction("<<<\n", "", "");
                return true;
            }

            case R.string.abid_asciidoc_insert_link:
            case R.string.abid_asciidoc_insert_image: {
                AttachLinkOrFileDialog.showInsertImageOrLinkDialog(
                        action == R.string.abid_asciidoc_insert_image ? 2 : 3,
                        _document.getFormat(), getActivity(), _hlEditor, _document.getFile());
                return true;
            }
            //this is an additional extra menu, analogous to special_key menu
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
            // Hint: the order is defined in the array, not here! search for
            // <string-array name="asciidoc_textactions_press_key__text" translatable="false">
            // TODO: Find out if you could also display icons in this list.
            // https://docs.asciidoctor.org/asciidoc/latest/blocks/delimited/
            // AsciiDoc actions
            if (callbackPayload.equals(rstr(R.string.asciidoc_block_comment))) {
                runAsciidocInlineAction("\n////\n", "", "");
            } else if (callbackPayload.equals(rstr(R.string.asciidoc_block_example))) {
                runAsciidocInlineAction("\n====\n", "", "");
            } else if (callbackPayload.equals(rstr(R.string.asciidoc_block_listing))) {
                runAsciidocInlineAction("\n----\n", "", "");
            } else if (callbackPayload.equals(rstr(R.string.asciidoc_block_literal))) {
                runAsciidocInlineAction("\n....\n", "", "");
            } else if (callbackPayload.equals(rstr(R.string.asciidoc_block_open))) {
                runAsciidocInlineAction("\n--\n", "", "");
            } else if (callbackPayload.equals(rstr(R.string.asciidoc_block_sidebar))) {
                runAsciidocInlineAction("\n****\n", "", "");
            } else if (callbackPayload.equals(rstr(R.string.asciidoc_block_table))) {
                runAsciidocInlineAction("\n|===\n", "", "");
            } else if (callbackPayload.equals(rstr(R.string.asciidoc_block_pass))) {
                runAsciidocInlineAction("\n++++\n", "", "");
            } else if (callbackPayload.equals(rstr(R.string.asciidoc_block_quote))) {
                runAsciidocInlineAction("\n____\n", "", "");
            } else if (callbackPayload.equals(rstr(R.string.asciidoc_block_code))) {
                runAsciidocInlineAction("\n----\n", "[source,sql]", "");
            } else if (callbackPayload.equals(rstr(R.string.asciidoc_block_collapsible))) {
                runAsciidocInlineAction("\n====\n", "[%collapsible]", "");
                // TODO: How to insert visual row delimiter?
                // inline roles
            } else if (callbackPayload.equals(rstr(R.string.asciidoc_highlight))) {
                runAsciidocInlineAction("#", "", "");
            } else if (callbackPayload.equals(rstr(R.string.asciidoc_underline))) {
                runAsciidocInlineAction("#", "[.underline]", "");
            } else if (callbackPayload.equals(rstr(R.string.asciidoc_linethrough))) {
                runAsciidocInlineAction("#", "[.line-through]", "");
            } else if (callbackPayload.equals(rstr(R.string.asciidoc_overline))) {
                runAsciidocInlineAction("#", "[.overline]", "");
            } else if (callbackPayload.equals(rstr(R.string.asciidoc_subscript))) {
                runAsciidocInlineAction("~", "", "");
            } else if (callbackPayload.equals(rstr(R.string.asciidoc_superscript))) {
                runAsciidocInlineAction("^", "", "");

            } else if (callbackPayload.equals(rstr(R.string.asciidoc_nobreak))) {
                runAsciidocInlineAction("#", "[.nobreak]", "");
            } else if (callbackPayload.equals(rstr(R.string.asciidoc_nowrap))) {
                runAsciidocInlineAction("#", "[.nowrap]", "");
            } else if (callbackPayload.equals(rstr(R.string.asciidoc_prewrap))) {
                runAsciidocInlineAction("#", "[.pre-wrap]", "");

                // TODO: How to insert visual row delimiter?
                // breaks
                // https://docs.asciidoctor.org/asciidoc/latest/blocks/breaks/#page-breaks
            } else if (callbackPayload.equals(rstr(R.string.asciidoc_break_thematic))) {
                runAsciidocInlineAction("", "\n---\n", "");
            } else if (callbackPayload.equals(rstr(R.string.asciidoc_break_page))) {
                runAsciidocInlineAction("", "\n<<<\n", "");
            }
        });
    }

    protected void runAsciidocInlineAction(String _action, String _prefix, String _suffix) {
        if (_hlEditor.getText() == null) {
            return;
        }
        if (_hlEditor.hasSelection()) {
            //Condition for non empty Selection
            String text = _hlEditor.getText().toString();
            int selectionStart = _hlEditor.getSelectionStart();
            int selectionEnd = _hlEditor.getSelectionEnd();
            int selectionLength = selectionEnd - selectionStart;
            // // NOTE: this does not seem to exist
            // String selectedText =  _hlEditor.getSelectedText();
            String selectedText = text.substring(selectionStart, selectionEnd);
            String comparingText = _prefix + _action + selectedText + _action + _suffix;
            //possible exception, if we try to assign a value here:
            String selectedTextWithSurrounding = "";
            // String selectedTextWithSurrounding =  text.substring(selectionStart - _prefix
            // .length()- _action.length(), selectionEnd + _action.length());
            //if the selected text is surrounded by _prefix + _action ... _action
            //then remove surrounded content
            if ((selectionStart >= _prefix.length() + _action.length()) &&
                    (selectionEnd <= (_hlEditor.length() - _suffix.length() - _action.length()))) {
                selectedTextWithSurrounding = text.substring(
                        selectionStart - _prefix.length() - _action.length(),
                        selectionEnd + _action.length() + _suffix.length());
            }
            if (Objects.equals(selectedTextWithSurrounding, comparingText)) {
                //remove outer surrounding
                _hlEditor.getText()
                        .replace(selectionStart - _prefix.length() - _action.length(),
                                selectionEnd + _action.length() + _suffix.length(), selectedText);
                _hlEditor.setSelection(selectionStart - _prefix.length() - _action.length(),
                        selectionStart - _prefix.length() - _action.length() + selectionLength);
            } else if (Objects.equals(text.substring(selectionStart,
                            selectionStart + _prefix.length() + _action.length()),
                    _prefix + _action + _suffix)) {
                if ((_prefix.length() + _action.length() + _action.length() + _suffix.length()
                        <= selectionLength)
                        && (Objects.equals(
                        text.substring(selectionEnd - _suffix.length() - _action.length(),
                                selectionEnd), _action))) {
                    //remove inner surrounding
                    //does this happen at the same time? Or do we have to cut off the end first and
                    // then the beginning?
                    _hlEditor.getText().replace(selectionStart,
                                    selectionStart + _prefix.length() + _action.length(), "")
                            .replace(selectionEnd - _suffix.length() - _action.length(),
                                    selectionEnd, "");

                } else {
                    //add surrounding
                    // insert _prefix + _action before and _action + _suffix after selection
                    _hlEditor.getText().insert(selectionStart, _prefix + _action);
                    _hlEditor.getText().insert(_hlEditor.getSelectionEnd(), _action + _suffix);
                    //keep selection, which has been moved now to the right
                    _hlEditor.setSelection(selectionStart + _action.length() + _prefix.length(),
                            selectionEnd + _action.length() + _prefix.length());
                }
            } else {
                //same code as above:
                //add surrounding
                // insert _prefix + _action before and _action after selection
                _hlEditor.getText().insert(selectionStart, _prefix + _action);
                _hlEditor.getText().insert(_hlEditor.getSelectionEnd(), _action + _suffix);
                //keep selection, which has been moved now to the right
                _hlEditor.setSelection(selectionStart + _action.length() + _prefix.length(),
                        selectionEnd + _action.length() + _prefix.length());
            }

        } else {
            //Condition for empty Selection
            // insert _prefix + _action before and _action after current cursor (empty
            // selection)
            _hlEditor.getText().insert(_hlEditor.getSelectionStart(), _prefix + _action)
                    .insert(_hlEditor.getSelectionEnd(), _action + _suffix);
            _hlEditor.setSelection(
                    _hlEditor.getSelectionStart() - _prefix.length() - _action.length(),
                    _hlEditor.getSelectionStart() - _prefix.length() - _action.length());
        }
    }


    @Override
    protected @StringRes
    int getFormatActionsKey() {
        return R.string.pref_key__asciidoc__action_keys;
    }

    // @Override
    // protected void renumberOrderedList() {
    //     AutoTextFormatter.renumberOrderedList(_hlEditor.getText(),
    //     AsciidocReplacePatternGenerator.formatPatterns);
    // }
}
