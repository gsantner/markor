/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.general;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.StringRes;
import android.text.Editable;
import android.text.TextUtils;
import android.view.KeyEvent;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.Utils;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;

import net.gsantner.markor.R;
import net.gsantner.markor.ui.SearchOrCustomTextDialogCreator;
import net.gsantner.markor.ui.hleditor.HighlightingEditor;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.opoc.format.plaintext.PlainTextStuff;
import net.gsantner.opoc.util.Callback;
import net.gsantner.opoc.util.ContextUtils;
import net.gsantner.opoc.util.StringUtils;

@SuppressWarnings("WeakerAccess")
public class CommonTextActions {
    public static final int ACTION_SPECIAL_KEY__ICON = R.drawable.ic_keyboard_black_24dp;
    public static final String ACTION_SPECIAL_KEY = "tmaid_common_special_key";

    public static final int ACTION_OPEN_LINK_BROWSER__ICON = R.drawable.ic_open_in_browser_black_24dp;
    public static final String ACTION_OPEN_LINK_BROWSER = "tmaid_common_open_link_browser";

    public static final int ACTION_COLOR_PICKER_ICON = R.drawable.ic_format_color_fill_black_24dp;
    public static final String ACTION_COLOR_PICKER = "tmaid_common_color_picker";

    public static final int ACTION_DELETE_LINES_ICON = R.drawable.ic_delete_black_24dp;
    public static final String ACTION_DELETE_LINES = "tmaid_common_delete_lines";

    public static final int ACTION_END_LINE_WITH_TWO_SPACES_ICON = R.drawable.ic_keyboard_return_black_24dp;
    public static final String ACTION_END_LINE_WITH_TWO_SPACES = "tmaid_markdown_end_line_with_two_spaces";

    public static final int ACTION_SEARCH_ICON = R.drawable.ic_search_black_24dp;
    public static final String ACTION_SEARCH = "tmaid_common_search_in_content_of_current_file";

    public static final int ACTION_JUMP_BOTTOM_TOP_ICON = R.drawable.ic_vertical_align_center_black_24dp;
    public static final String ACTION_JUMP_BOTTOM_TOP = "tmaid_common_jump_to_bottom";

    public static final String ACTION_INDENT = "tmaid_common_indent";

    public static final String ACTION_DEINDENT = "tmaid_common_deindent";

    private static final String LINE_SEPARATOR = TextUtils.isEmpty(System.getProperty("line.separator")) ? "\n" : System.getProperty("line.separator");

    private final Activity _activity;
    private final HighlightingEditor _hlEditor;

    private int _tabWidth;

    public CommonTextActions(Activity activity, HighlightingEditor hlEditor) {
        _activity = activity;
        _hlEditor = hlEditor;

        Context context = activity != null ? activity : _hlEditor.getContext();
        AppSettings settings = new AppSettings(context);
        _tabWidth = settings.getTabWidth();
    }

    private String rstr(@StringRes int resKey) {
        return _activity.getString(resKey);
    }

    // Returns true when handled
    public boolean runAction(String action) {
        final String origText = _hlEditor.getText().toString();
        switch (action) {
            case ACTION_SPECIAL_KEY: {
                SearchOrCustomTextDialogCreator.showSpecialKeyDialog(_activity, (callbackPayload) -> {
                    if (!_hlEditor.hasSelection() && _hlEditor.length() > 0) {
                        _hlEditor.requestFocus();
                    }
                    if (callbackPayload.equals(rstr(R.string.key_page_down))) {
                        _hlEditor.simulateKeyPress(KeyEvent.KEYCODE_PAGE_DOWN);
                    } else if (callbackPayload.equals(rstr(R.string.key_page_up))) {
                        _hlEditor.simulateKeyPress(KeyEvent.KEYCODE_PAGE_UP);
                    } else if (callbackPayload.equals(rstr(R.string.key_pos_1))) {
                        _hlEditor.simulateKeyPress(KeyEvent.KEYCODE_MOVE_HOME);
                    } else if (callbackPayload.equals(rstr(R.string.key_pos_end))) {
                        _hlEditor.simulateKeyPress(KeyEvent.KEYCODE_MOVE_END);
                    } else if (callbackPayload.equals(rstr(R.string.key_pos_1_document))) {
                        _hlEditor.setSelection(0);
                    } else if (callbackPayload.equals(rstr(R.string.move_text_one_line_up))) {
                        moveLineBy1(true);
                    } else if (callbackPayload.equals(rstr(R.string.move_text_one_line_down))) {
                        moveLineBy1(false);
                    } else if (callbackPayload.equals(rstr(R.string.key_pos_end_document))) {
                        _hlEditor.setSelection(_hlEditor.length());
                    } else if (callbackPayload.equals(rstr(R.string.key_ctrl_a))) {
                        _hlEditor.setSelection(0, _hlEditor.length());
                    } else if (callbackPayload.equals(rstr(R.string.key_tab))) {
                        _hlEditor.insertOrReplaceTextOnCursor("\u0009");
                    } else if (callbackPayload.equals(rstr(R.string.zero_width_space))) {
                        _hlEditor.insertOrReplaceTextOnCursor("\u200B");
                    } else if (callbackPayload.equals(rstr(R.string.search))) {
                        runAction(ACTION_SEARCH);
                    } else if (callbackPayload.equals(rstr(R.string.break_page_pdf_print))) {
                        _hlEditor.insertOrReplaceTextOnCursor("<div style='page-break-after:always;'></div>");
                    } else if (callbackPayload.equals(rstr(R.string.ohm))) {
                        _hlEditor.insertOrReplaceTextOnCursor("Ω");
                    } else if (callbackPayload.equals(rstr(R.string.continued_overline))) {
                        _hlEditor.insertOrReplaceTextOnCursor("‾‾‾‾‾");
                    } else if (callbackPayload.equals(rstr(R.string.shrug))) {
                        _hlEditor.insertOrReplaceTextOnCursor("¯\\_(ツ)_/¯");
                    } else if (callbackPayload.equals(rstr(R.string.char_punctation_mark_arrows))) {
                        _hlEditor.insertOrReplaceTextOnCursor("»«");
                    } else if (callbackPayload.equals(rstr(R.string.select_current_line))) {
                        _hlEditor.setSelectionExpandWholeLines();
                    }
                });
                return true;
            }
            case ACTION_OPEN_LINK_BROWSER: {
                String url;
                if ((url = PlainTextStuff.tryExtractUrlAroundPos(_hlEditor.getText().toString(), _hlEditor.getSelectionStart())) != null) {
                    if (url.endsWith(")")) {
                        url = url.substring(0, url.length() - 1);
                    }
                    new ContextUtils(_activity).openWebpageInExternalBrowser(url);
                }
                return true;
            }
            case ACTION_DELETE_LINES: {
                int[] indexes = PlainTextStuff.getNeighbourLineEndings(_hlEditor.getText().toString(), _hlEditor.getSelectionStart(), _hlEditor.getSelectionEnd());
                if (indexes != null) {
                    _hlEditor.getText().delete(indexes[0], indexes[1]);
                }
                return true;
            }
            case ACTION_END_LINE_WITH_TWO_SPACES: {
                if (_hlEditor.length() > 1) {
                    int start = _hlEditor.getSelectionStart();
                    String text = _hlEditor.getText().toString();
                    int insertPos = text.indexOf('\n', start);
                    insertPos = insertPos < 1 ? text.length() : insertPos;
                    _hlEditor.getText().insert(insertPos, "  " + (text.endsWith("\n") ? "" : "\n"));
                    _hlEditor.setSelection(((insertPos + 3) > _hlEditor.length() ? _hlEditor.length() : (insertPos + 3)));
                }
                return true;
            }
            case ACTION_SEARCH: {
                SearchOrCustomTextDialogCreator.showSearchDialog(_activity, origText, callbackPayload -> {
                    int cursor = origText.indexOf(callbackPayload);
                    _hlEditor.setSelection(Math.min(_hlEditor.length(), Math.max(0, cursor)));
                });
                return true;
            }

            case ACTION_JUMP_BOTTOM_TOP: {
                int pos = _hlEditor.getSelectionStart();
                _hlEditor.setSelection(pos == 0 ? _hlEditor.getText().length() : 0);
                return true;
            }
            case ACTION_COLOR_PICKER: {
                SearchOrCustomTextDialogCreator.showColorSelectionModeDialog(_activity, new Callback.a1<Integer>() {
                    @Override
                    public void callback(Integer colorInsertType) {
                        ColorPickerDialogBuilder
                                .with(_hlEditor.getContext())
                                .setTitle(R.string.color)
                                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                                .density(12)
                                .setPositiveButton(android.R.string.ok, new ColorPickerClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
                                        String hex = Utils.getHexString(selectedColor, false).toLowerCase();
                                        int pos = _hlEditor.getSelectionStart();
                                        switch (colorInsertType) {
                                            case R.string.hexcode: {
                                                _hlEditor.getText().insert(pos, hex);
                                                break;
                                            }
                                            case R.string.foreground: {
                                                _hlEditor.getText().insert(pos, "<span style='color:" + hex + ";'></span>");
                                                _hlEditor.setSelection(_hlEditor.getSelectionStart() - 7);
                                                break;
                                            }
                                            case R.string.background: {
                                                _hlEditor.getText().insert(pos, "<span style='background-color:" + hex + ";'></span>");
                                                _hlEditor.setSelection(_hlEditor.getSelectionStart() - 7);
                                                break;
                                            }
                                        }

                                    }
                                })
                                .setNegativeButton(R.string.cancel, null)
                                .build()
                                .show();
                    }
                });
                return true;
            }
            case ACTION_INDENT: {
                runIndentLines(false);
                return true;
            }
            case ACTION_DEINDENT: {
                runIndentLines(true);
                return true;
            }
            default:
                break;
        }
        return false;
    }

    protected void runIndentLines(Boolean deIndent) {

        Editable text = _hlEditor.getText();

        int[] selection = StringUtils.getSelection(_hlEditor);
        final int[] lStart = StringUtils.getLineOffsetFromIndex(text, selection[0]);
        final int[] lEnd = StringUtils.getLineOffsetFromIndex(text, selection[1]);

        int selectionStart = selection[0];
        int selectionEnd = selection[1];

        int lineStart = StringUtils.getLineStart(text, selectionStart);

        String tabString = StringUtils.repeatChars(' ', _tabWidth);

        while (lineStart <= selectionEnd) {

            if (deIndent) {
                int textStart = StringUtils.getNextNonWhitespace(text, lineStart, selectionEnd);
                int spaceCount = textStart - lineStart;
                int delCount = Math.min(_tabWidth, spaceCount);
                int delEnd = lineStart + delCount;
                if (delCount > 0 && delEnd <= text.length()) {
                    text.delete(lineStart, delEnd);
                    selectionEnd -= delCount;
                }
            } else {
                text.insert(lineStart, tabString);
                selectionEnd += _tabWidth;
            }

            text = _hlEditor.getText();
            // Get next line
            lineStart = StringUtils.getLineEnd(text, lineStart, selectionEnd) + 1;
        }

        _hlEditor.setSelection(
                StringUtils.getIndexFromLineOffset(text, lStart),
                StringUtils.getIndexFromLineOffset(text, lEnd));
    }

    public void moveLineBy1(boolean up) {
        selectWholeLine(true);
        selectWholeLine(false);
        String lineToMove = _hlEditor.getText().toString().substring(_hlEditor.getSelectionStart(), _hlEditor.getSelectionEnd());
        if (!lineToMove.endsWith(LINE_SEPARATOR)) {
            lineToMove += LINE_SEPARATOR;
        }
        _hlEditor.simulateKeyPress(KeyEvent.KEYCODE_DEL);
        _hlEditor.simulateKeyPress(up ? KeyEvent.KEYCODE_DPAD_UP : KeyEvent.KEYCODE_DPAD_DOWN);
        _hlEditor.simulateKeyPress(KeyEvent.KEYCODE_MOVE_HOME);
        _hlEditor.getText().insert(_hlEditor.getSelectionStart(), lineToMove);
    }

    public void selectWholeLine(boolean toStart) {
        final String content = _hlEditor.getText().toString();
        if (_hlEditor.getSelectionStart() == content.length()) {
            _hlEditor.setSelection(_hlEditor.getSelectionStart() - 1, _hlEditor.getSelectionEnd());
        }
        if (_hlEditor.getSelectionEnd() == content.length()) {
            _hlEditor.setSelection(_hlEditor.getSelectionStart(), _hlEditor.getSelectionEnd() - 1);
        }

        int i = (toStart ? _hlEditor.getSelectionStart() : _hlEditor.getSelectionEnd());
        for (; i > 0 && i < content.length(); i = toStart ? (i - 1) : (i + 1)) {
            if (content.charAt(i) == '\n' || content.charAt(i) == '\r') {
                i = toStart ? i + 1 : i + 1;
                break;
            }
        }
        _hlEditor.setSelection(toStart ? i : _hlEditor.getSelectionStart(), toStart ? _hlEditor.getSelectionEnd() : i);
    }

}
