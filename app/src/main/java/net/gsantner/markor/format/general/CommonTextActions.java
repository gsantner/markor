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

    public static final String ACTION_MOVE_UP = "tmaid_common_move_text_one_line_up";
    public static final String ACTION_MOVE_DOWN = "tmaid_common_move_text_one_line_down";
    public static final String ACTION_NEW_LINE_BELOW = "tmaid_common_new_line_below";

    private static final String LINE_SEPARATOR = TextUtils.isEmpty(System.getProperty("line.separator")) ? "\n" : System.getProperty("line.separator");

    private final Activity _activity;
    private final HighlightingEditor _hlEditor;

    public CommonTextActions(final Activity activity, final HighlightingEditor hlEditor) {
        _activity = activity;
        _hlEditor = hlEditor;
    }

    private String rstr(@StringRes int resKey) {
        return _activity.getString(resKey);
    }

    // Returns true when handled
    public boolean runAction(String action) {
        final String origText = _hlEditor.getText().toString();
        switch (action) {
            case ACTION_SPECIAL_KEY: {

                // Needed to prevent selection from being overwritten on refocus
                final int[] sel = StringUtils.getSelection(_hlEditor);
                _hlEditor.clearFocus();
                _hlEditor.requestFocus();
                _hlEditor.setSelection(sel[0], sel[1]);

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
            case ACTION_MOVE_UP: {
                moveLineBy1(true);
                return true;
            }
            case ACTION_MOVE_DOWN: {
                moveLineBy1(false);
                return true;
            }
            case ACTION_NEW_LINE_BELOW: {
                // Go to end of line, works with wrapped lines too
                _hlEditor.setSelection(StringUtils.getLineEnd(_hlEditor.getText(), StringUtils.getSelection(_hlEditor)[1]));
                _hlEditor.simulateKeyPress(KeyEvent.KEYCODE_ENTER);
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
                final int[] sel = StringUtils.getLineSelection(_hlEditor);
                final Editable text = _hlEditor.getText();
                final boolean lastLine = sel[1] == text.length();
                final boolean firstLine = sel[0] == 0;
                text.delete(sel[0] - (lastLine && !firstLine ? 1 : 0), sel[1] + (lastLine ? 0 : 1));
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
                SearchOrCustomTextDialogCreator.showSearchDialog(_activity, _hlEditor.getText(), StringUtils.getSelection(_hlEditor), null, (text, lineNr) -> {
                    if (!_hlEditor.hasFocus()) {
                        _hlEditor.requestFocus();
                    }
                    // Go to beginning of line #line
                    _hlEditor.setSelection(StringUtils.getLineStart(origText, StringUtils.getIndexFromLineOffset(origText, lineNr, 0)));
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
            default:
                break;
        }
        return false;
    }

    public void moveLineBy1(final boolean up) {

        final Editable text = _hlEditor.getText();

        final int[] sel = StringUtils.getSelection(_hlEditor);
        final int linesStart = StringUtils.getLineStart(text, sel[0]);
        final int linesEnd = StringUtils.getLineEnd(text, sel[1]);

        if ((up && linesStart > 0) || (!up && linesEnd < text.length())) {

            final CharSequence lines = text.subSequence(linesStart, linesEnd);

            final int altStart = up ? StringUtils.getLineStart(text, linesStart - 1) : linesEnd + 1;
            final int altEnd = StringUtils.getLineEnd(text, altStart);
            final CharSequence altLine = text.subSequence(altStart, altEnd);

            try {
                // Prevents changes in text from triggering list prefix insert etc
                _hlEditor.disableHighlighterAutoFormat();

                final int[] selStart = StringUtils.getLineOffsetFromIndex(text, sel[0]);
                final int[] selEnd = StringUtils.getLineOffsetFromIndex(text, sel[1]);

                final String newPair = String.format("%s\n%s", up ? lines : altLine, up ? altLine : lines);
                text.replace(Math.min(linesStart, altStart), Math.max(altEnd, linesEnd), newPair);

                selStart[0] += up ? -1 : 1;
                selEnd[0] += up ? -1 : 1;
                _hlEditor.setSelection(
                        StringUtils.getIndexFromLineOffset(text, selStart),
                        StringUtils.getIndexFromLineOffset(text, selEnd)
                );

            } finally {
                _hlEditor.enableHighlighterAutoFormat();
            }
        }
    }
}
