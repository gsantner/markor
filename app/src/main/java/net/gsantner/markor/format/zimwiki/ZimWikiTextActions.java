/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.zimwiki;

import android.app.Activity;
import android.support.annotation.StringRes;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.Toast;

import net.gsantner.markor.R;
import net.gsantner.markor.format.general.CommonTextActions;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.AttachImageOrLinkDialog;
import net.gsantner.markor.ui.SearchOrCustomTextDialogCreator;
import net.gsantner.opoc.util.ContextUtils;
import net.gsantner.opoc.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class ZimWikiTextActions extends net.gsantner.markor.ui.hleditor.TextActions {

    // TODO: adapt these to zim wiki
    public static final Pattern PREFIX_ORDERED_LIST = Pattern.compile("^(\\s*)((\\d+)([.)])(\\s+))");
    public static final Pattern PREFIX_CHECKED_LIST = Pattern.compile("^(\\s*)(([-*+])\\s\\[([xX])]\\s)");
    public static final Pattern PREFIX_UNCHECKED_LIST = Pattern.compile("^(\\s*)(([-*+])\\s\\[\\s]\\s)");
    public static final Pattern PREFIX_UNORDERED_LIST = Pattern.compile("^(\\s*)(([-*+])\\s)");
    public static final Pattern PREFIX_LEADING_SPACE = Pattern.compile("^(\\s*)");

    private static final Pattern[] PREFIX_PATTERNS = {
            PREFIX_ORDERED_LIST,
            PREFIX_CHECKED_LIST,
            PREFIX_UNCHECKED_LIST,
            // Unordered has to be after checked list. Otherwise checklist will match as an unordered list.
            PREFIX_UNORDERED_LIST,
            PREFIX_LEADING_SPACE,
    };

    private final ZimWikiReplacePatternGenerator replacePatternGenerator;

    public ZimWikiTextActions(Activity activity, Document document) {
        super(activity, document);
        replacePatternGenerator = new ZimWikiReplacePatternGenerator();
    }

    @Override
    public boolean runAction(String action, boolean modLongClick, String anotherArg) {
        int res = new ContextUtils(_context).getResId(ContextUtils.ResType.STRING, action);
        return new ZimWikiTextActionsImpl(res).onClickImpl(null);
    }

    @Override
    protected ActionCallback getActionCallback(@StringRes int keyId) {
        return new ZimWikiTextActionsImpl(keyId);
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
                new ActionItem(R.string.tmaid_zimwiki_bold, R.drawable.ic_format_bold_black_24dp, R.string.bold),
                new ActionItem(R.string.tmaid_zimwiki_strikeout, R.drawable.ic_format_strikethrough_black_24dp, R.string.strikeout),
                new ActionItem(R.string.tmaid_zimwiki_italic, R.drawable.ic_format_italic_black_24dp, R.string.italic),
                new ActionItem(R.string.tmaid_zimwiki_mark, R.drawable.ic_format_underlined_black_24dp, R.string.marked),
                new ActionItem(R.string.tmaid_zimwiki_code_inline, R.drawable.ic_code_black_24dp, R.string.inline_code),
                new ActionItem(R.string.tmaid_zimwiki_h1, R.drawable.format_header_1, R.string.heading_1),
                new ActionItem(R.string.tmaid_zimwiki_h2, R.drawable.format_header_2, R.string.heading_2),
                new ActionItem(R.string.tmaid_zimwiki_h3, R.drawable.format_header_3, R.string.heading_3),
                new ActionItem(R.string.tmaid_zimwiki_h4, R.drawable.format_header_4, R.string.heading_4),
                new ActionItem(R.string.tmaid_zimwiki_h5, R.drawable.format_header_5, R.string.heading_5),
                new ActionItem(R.string.tmaid_common_delete_lines, CommonTextActions.ACTION_DELETE_LINES_ICON, R.string.delete_lines),
                new ActionItem(R.string.tmaid_common_open_link_browser, CommonTextActions.ACTION_OPEN_LINK_BROWSER__ICON, R.string.open_link),
                new ActionItem(R.string.tmaid_common_attach_something, R.drawable.ic_attach_file_black_24dp, R.string.attach),
                new ActionItem(R.string.tmaid_common_special_key, CommonTextActions.ACTION_SPECIAL_KEY__ICON, R.string.special_key),
                new ActionItem(R.string.tmaid_common_time, R.drawable.ic_access_time_black_24dp, R.string.date_and_time),
                new ActionItem(R.string.tmaid_common_ordered_list_number, R.drawable.ic_format_list_numbered_black_24dp, R.string.ordered_list),
                new ActionItem(R.string.tmaid_common_accordion, R.drawable.ic_arrow_drop_down_black_24dp, R.string.accordion),
                new ActionItem(R.string.tmaid_common_indent, R.drawable.ic_format_indent_increase_black_24dp, R.string.indent),
                new ActionItem(R.string.tmaid_common_deindent, R.drawable.ic_format_indent_decrease_black_24dp, R.string.deindent),
                // TODO: insert link, insert image
        };

        return Arrays.asList(TMA_ACTIONS);
    }

    private class ZimWikiTextActionsImpl extends ActionCallback {
        private int _action;

        ZimWikiTextActionsImpl(int action) {
            _action = action;
        }

        @Override
        public void onClick(final View view) {
            onClickImpl(view);
        }

        private boolean onClickImpl(final View view) {
            if (view != null) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            }
            switch (_action) {
                case R.string.tmaid_zimwiki_h1: {
                    setHeadingAction(1);
                    return true;
                }
                case R.string.tmaid_zimwiki_h2: {
                    setHeadingAction(2);
                    return true;
                }
                case R.string.tmaid_zimwiki_h3: {
                    setHeadingAction(3);
                    return true;
                }
                case R.string.tmaid_zimwiki_h4: {
                    setHeadingAction(4);
                    return true;
                }
                case R.string.tmaid_zimwiki_h5: {
                    setHeadingAction(5);
                    return true;
                }
                case R.string.tmaid_common_unordered_list_char: {
                    // TODO: adapt to zim wiki
                    final String listChar = _appSettings.getUnorderedListCharacter();
                    final String listPrefix = "$1" + listChar + " ";
                    runPrefixReplaceAction(PREFIX_UNORDERED_LIST, listPrefix, "$1");
                    return true;
                }
                case R.string.tmaid_common_checkbox_list: {
                    // TODO: adapt to zim wiki
                    final String listChar = _appSettings.getUnorderedListCharacter();
                    final String uncheck = "$1" + listChar + " [ ] ";
                    final String check = "$1" + listChar + " [x] ";
                    runPrefixReplaceAction(PREFIX_UNCHECKED_LIST, uncheck, check);
                    return true;
                }
                case R.string.tmaid_common_ordered_list_number: {
                    // TODO: adapt to zim wiki
                    runPrefixReplaceAction(PREFIX_ORDERED_LIST, "$11. ", "$1");
                    runRenumberOrderedListIfRequired();
                    return true;
                }
                case R.string.tmaid_zimwiki_bold: {
                    runInlineAction("**");
                    return true;
                }
                case R.string.tmaid_zimwiki_italic: {
                    runInlineAction("//");
                    return true;
                }
                case R.string.tmaid_zimwiki_mark: {
                    runInlineAction("__");
                    return true;
                }
                case R.string.tmaid_zimwiki_strikeout: {
                    runInlineAction("~~");
                    return true;
                }
                case R.string.tmaid_zimwiki_code_inline: {
                    runInlineAction("''");
                    return true;
                }
//                case R.string.tmaid_zimwiki_horizontal_line: {
//                    runInlineAction("----\n");
//                    return true;
//                }
                case R.string.tmaid_zimwiki_insert_link:
                case R.string.tmaid_zimwiki_insert_image: {
                    // TODO: adapt to zim wiki
                    AttachImageOrLinkDialog.showInsertImageOrLinkDialog(_action == R.string.tmaid_zimwiki_insert_image ? 2 : 3, _document.getFormat(), _activity, _hlEditor, _document.getFile());
                    return true;
                }
                case R.string.tmaid_common_toolbar_title_clicked_edit_action: {
                    // TODO: adapt to zim wiki
                    final String origText = _hlEditor.getText().toString();
                    SearchOrCustomTextDialogCreator.showMarkdownHeadlineDialog(_activity, origText, callbackPayload -> {
                        int cursor = origText.indexOf(callbackPayload);
                        _hlEditor.setSelection(Math.min(_hlEditor.length(), Math.max(0, cursor)));
                    });
                    return true;
                }
                case R.string.tmaid_common_indent:
                case R.string.tmaid_common_deindent: {
                    runCommonTextAction(_context.getString(_action));
                    runRenumberOrderedListIfRequired();
                    return true;
                }
                default: {
                    return runCommonTextAction(_context.getString(_action));
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
                case R.string.tmaid_zimwiki_insert_image: {
                    // TODO: adapt to zim wiki
                    int pos = _hlEditor.getSelectionStart();
                    _hlEditor.getText().insert(pos, "<img style=\"width:auto;max-height: 256px;\" src=\"\" />");
                    _hlEditor.setSelection(pos + 48);
                    return true;
                }
                case R.string.tmaid_common_time: {
                    runCommonTextAction("tmaid_common_time_insert_timestamp");
                    return true;
                }
                case R.string.tmaid_zimwiki_code_inline: {
                    // TODO: adapt to zim wiki
                    _hlEditor.disableHighlighterAutoFormat();
                    final int c = _hlEditor.setSelectionExpandWholeLines();
                    _hlEditor.getText().insert(_hlEditor.getSelectionStart(), "\n```\n");
                    _hlEditor.getText().insert(_hlEditor.getSelectionEnd(), "\n```\n");
                    _hlEditor.setSelection(c + "\n```\n".length());
                    _hlEditor.enableHighlighterAutoFormat();
                    Toast.makeText(_activity, R.string.code_block, Toast.LENGTH_SHORT).show();
                    return true;
                }
                case R.string.tmaid_common_ordered_list_number: {
                    // TODO: adapt to zim wiki
                    ZimWikiAutoFormat.renumberOrderedList(_hlEditor.getText(), StringUtils.getSelection(_hlEditor)[0]);
                }
            }
            return false;
        }
    }


    /**
     * Set/unset heading level for the line (the lower the level, the higher the count of equal signs)
     * <p>
     * This routine will make the following conditional changes
     * <p>
     * Line is heading of same level as requested -> remove heading
     * Line is heading of different level that that requested -> replace with requested heading
     * Line is not heading -> add heading of specified level
     *
     * @param level heading level
     */
    private void setHeadingAction(int level) {

        List<ReplacePattern> patterns = new ArrayList<>();

        final int numberOfEqualSigns = 7-level;
        String headingChars = StringUtils.repeatChars('=', numberOfEqualSigns);

        patterns.add(replacePatternGenerator.removeHeadingCharsForExactHeadingLevel(headingChars));
        patterns.add(replacePatternGenerator.replaceDifferentHeadingLevelWithThisLevel(headingChars));
        patterns.add(replacePatternGenerator.createHeadingIfNoneThere(headingChars));

        runRegexReplaceAction(patterns);
    }

    private void runPrefixReplaceAction(final Pattern actionPattern, final String action, final String alt) {

        List<ReplacePattern> patterns = new ArrayList<>();

        // Replace prefixes with action (or alt if prefix is specified action)
        for (final Pattern pp : PREFIX_PATTERNS) {
            patterns.add(new ReplacePattern(pp, pp == actionPattern ? alt : action));
        }

        runRegexReplaceAction(patterns);
    }

    private void runRenumberOrderedListIfRequired() {
        if (_appSettings.isMarkdownAutoUpdateList()) {
            ZimWikiAutoFormat.renumberOrderedList(_hlEditor.getText(), StringUtils.getSelection(_hlEditor)[0]);
        }
    }
}
