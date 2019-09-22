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
import android.text.Editable;
import android.view.View;
import android.view.ViewGroup;

import net.gsantner.markor.R;
import net.gsantner.markor.format.TextFormat;
import net.gsantner.markor.format.general.CommonTextActions;
import net.gsantner.markor.format.general.DatetimeFormatDialog;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.AttachImageOrLinkDialog;
import net.gsantner.markor.ui.hleditor.TextActions;
import net.gsantner.markor.util.AppSettings;

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
            {R.string.tmaid_markdown_bold, R.drawable.ic_format_bold_black_24dp},
            {R.string.tmaid_markdown_italic, R.drawable.ic_format_italic_black_24dp},
            {R.string.tmaid_markdown_code_inline, R.drawable.ic_code_black_24dp},
            {R.string.tmaid_markdown_insert_image, R.drawable.ic_image_black_24dp},
            {R.string.tmaid_general_delete_lines, CommonTextActions.ACTION_DELETE_LINES_ICON},
            {R.string.tmaid_general_open_link_browser, CommonTextActions.ACTION_OPEN_LINK_BROWSER__ICON},
            {R.string.tmaid_general_special_key, CommonTextActions.ACTION_SPECIAL_KEY__ICON},
            {R.string.tmaid_markdown_insert_link, R.drawable.ic_link_black_24dp},

            {R.string.tmaid_markdown_horizontal_line, R.drawable.ic_more_horiz_black_24dp},
            {R.string.tmaid_markdown_strikeout, R.drawable.ic_format_strikethrough_black_24dp},
            {R.string.tmaid_markdown_quote, R.drawable.ic_format_quote_black_24dp},
            {R.string.tmaid_markdown_h1, R.drawable.format_header_1},
            {R.string.tmaid_markdown_h2, R.drawable.format_header_2},
            {R.string.tmaid_markdown_h3, R.drawable.format_header_3},
            {R.string.tmaid_markdown_ul, R.drawable.ic_list_black_24dp},
            {R.string.tmaid_markdown_ol, R.drawable.ic_format_list_numbered_black_24dp},
            {R.string.tmaid_markdown_checkbox, R.drawable.ic_check_box_black_24dp},
            {R.string.tmaid_general_color_picker, CommonTextActions.ACTION_COLOR_PICKER_ICON},
            {R.string.tmaid_general_time, R.drawable.ic_access_time_black_24dp},
    };

    private class MarkdownTextActionsImpl implements View.OnClickListener, View.OnLongClickListener {
        private int _action;

        MarkdownTextActionsImpl(int action) {
            _action = action;
        }

        @Override
        public void onClick(View v) {
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
                case R.string.tmaid_markdown_ul: {
                    runMarkdownRegularPrefixAction("- ");
                    break;
                }
                case R.string.tmaid_markdown_ol: {
                    runMarkdownRegularPrefixAction("1. ");
                    break;
                }
                case R.string.tmaid_markdown_checkbox: {
                    runMarkdownRegularPrefixAction("- [ ] ", "- [x] ");
                    break;
                }
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
                case R.string.tmaid_general_delete_lines: {
                    new CommonTextActions(_activity, _hlEditor).runAction(CommonTextActions.ACTION_DELETE_LINES);
                    break;
                }
                case R.string.tmaid_general_open_link_browser: {
                    new CommonTextActions(_activity, _hlEditor).runAction(CommonTextActions.ACTION_OPEN_LINK_BROWSER);
                    break;
                }
                case R.string.tmaid_general_special_key: {
                    new CommonTextActions(_activity, _hlEditor).runAction(CommonTextActions.ACTION_SPECIAL_KEY);
                    break;
                }
                case R.string.tmaid_general_color_picker: {
                    new CommonTextActions(_activity, _hlEditor).runAction(CommonTextActions.ACTION_COLOR_PICKER);
                    break;
                }
                case R.string.tmaid_markdown_insert_link:
                case R.string.tmaid_markdown_insert_image: {
                    AttachImageOrLinkDialog.showInsertImageOrLinkDialog(_action == R.string.tmaid_markdown_insert_image ? 2 : 3, _document.getFormat(),_activity, _hlEditor, _document.getFile());
                    break;
                }
                case R.string.tmaid_general_time: {
                    DatetimeFormatDialog.showDatetimeFormatDialog(_activity, _hlEditor);
                    break;
                }
            }
        }

        @Override
        public boolean onLongClick(View v) {
            switch (_action) {
                case R.string.tmaid_general_open_link_browser: {
                    new CommonTextActions(_activity, _hlEditor).runAction(CommonTextActions.ACTION_SEARCH);
                    return true;
                }
                case R.string.tmaid_general_special_key: {
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

        class TextSelection {

            private int _selectionStart;
            private int _selectionEnd;
            private Editable _editable;


            TextSelection(int start, int end, Editable editable) {
                _selectionStart = start;
                _selectionEnd = end;
                _editable = editable;
            }

            private void insertText(int location, String text) {
                _editable.insert(location, text);
                _selectionEnd += text.length();
            }

            private void removeText(int location, String text) {
                _editable.delete(location, location + text.length());
                _selectionEnd -= text.length();
            }

            private int getSelectionStart() {
                return _selectionStart;
            }

            private int getSelectionEnd() {
                return _selectionEnd;
            }
        }

        private int findLineStart(int cursor, String text) {
            int i = cursor - 1;
            for (; i >= 0; i--) {
                if (text.charAt(i) == '\n') {
                    break;
                }
            }

            return i + 1;
        }

        private int findNextLine(int startIndex, int endIndex, String text) {
            int index = -1;
            for (int i = startIndex; i < endIndex; i++) {
                if (text.charAt(i) == '\n') {
                    index = i + 1;
                    break;
                }
            }

            return index;
        }

        private void runMarkdownRegularPrefixAction(String action) {
            runMarkdownRegularPrefixAction(action, null);
        }

        private void runMarkdownRegularPrefixAction(String action, String replaceString) {
            String text = _hlEditor.getText().toString();
            TextSelection textSelection = new TextSelection(_hlEditor.getSelectionStart(), _hlEditor.getSelectionEnd(), _hlEditor.getText());

            int lineStart = findLineStart(textSelection.getSelectionStart(), text);

            while (lineStart != -1) {
                if (replaceString == null) {
                    if (text.substring(lineStart, textSelection.getSelectionEnd()).startsWith(action)) {
                        textSelection.removeText(lineStart, action);
                    } else {
                        textSelection.insertText(lineStart, action);
                    }
                } else {
                    if (text.substring(lineStart, textSelection.getSelectionEnd()).startsWith(action)) {
                        textSelection.removeText(lineStart, action);
                        textSelection.insertText(lineStart, replaceString);
                    } else if (text.substring(lineStart, textSelection.getSelectionEnd()).startsWith(replaceString)) {
                        textSelection.removeText(lineStart, replaceString);
                        textSelection.insertText(lineStart, action);
                    } else {
                        textSelection.insertText(lineStart, action);
                    }
                }

                text = _hlEditor.getText().toString();

                lineStart = findNextLine(lineStart, textSelection.getSelectionEnd(), text);
            }
        }

        private void runMarkdownInlineAction(String _action) {
            if (_hlEditor.getText() == null) {
                return;
            }
            if (_hlEditor.hasSelection()) {
                String text = _hlEditor.getText().toString();
                int selectionStart = _hlEditor.getSelectionStart();
                int selectionEnd = _hlEditor.getSelectionEnd();

                //Check if Selection includes the shortcut characters
                if (selectionEnd < text.length() && selectionStart >= 0 && (text.substring(selectionStart, selectionEnd)
                        .matches("(\\*\\*|~~|_|`)[a-zA-Z0-9\\s]*(\\*\\*|~~|_|`)"))) {

                    text = text.substring(selectionStart + _action.length(),
                            selectionEnd - _action.length());
                    _hlEditor.getText()
                            .replace(selectionStart, selectionEnd, text);

                }
                //Check if Selection is Preceded and succeeded by shortcut characters
                else if (((selectionEnd <= (_hlEditor.length() - _action.length())) &&
                        (selectionStart >= _action.length())) &&
                        (text.substring(selectionStart - _action.length(),
                                selectionEnd + _action.length())
                                .matches("(\\*\\*|~~|_|`)[a-zA-Z0-9\\s]*(\\*\\*|~~|_|`)"))) {

                    text = text.substring(selectionStart, selectionEnd);
                    _hlEditor.getText()
                            .replace(selectionStart - _action.length(),
                                    selectionEnd + _action.length(), text);

                }
                //Condition to insert shortcut preceding and succeeding the selection
                else {
                    _hlEditor.getText().insert(selectionStart, _action);
                    _hlEditor.getText().insert(_hlEditor.getSelectionEnd(), _action);
                }
            } else {
                //Condition for Empty Selection
                /*if (false) {
                    // Condition for things that should only be placed at the start of the line even if no text is selected
                } else */
                if ("----\n".equals(_action)) {
                    _hlEditor.getText().insert(_hlEditor.getSelectionStart(), _action);
                } else {
                    // Condition for formatting which is inserted on either side of the cursor
                    _hlEditor.getText().insert(_hlEditor.getSelectionStart(), _action)
                            .insert(_hlEditor.getSelectionEnd(), _action);
                    _hlEditor.setSelection(_hlEditor.getSelectionStart() - _action.length());
                }
            }
        }
    }

}
