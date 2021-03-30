/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.txt2tags;

import android.app.Activity;
import android.support.annotation.StringRes;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.Toast;

import net.gsantner.markor.R;
import net.gsantner.markor.format.AutoFormatter;
import net.gsantner.markor.format.general.CommonTextActions;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.AttachImageOrLinkDialog;
import net.gsantner.markor.ui.SearchOrCustomTextDialogCreator;
import net.gsantner.opoc.util.ContextUtils;
import net.gsantner.opoc.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

public class Txt2tagsTextActions extends net.gsantner.markor.ui.hleditor.TextActions {

    public Txt2tagsTextActions(Activity activity, Document document) {
        super(activity, document);
    }

    @Override
    public boolean runAction(String action, boolean modLongClick, String anotherArg) {
        int res = new ContextUtils(_context).getResId(ContextUtils.ResType.STRING, action);
        return new Txt2tagsTextActionsImpl(res).onClickImpl(null);
    }

    @Override
    protected ActionCallback getActionCallback(@StringRes int keyId) {
        return new Txt2tagsTextActionsImpl(keyId);
    }

    @Override
    protected @StringRes
    int getFormatActionsKey() {
        return R.string.pref_key__txt2tags__action_keys;
    }

    @Override
    public List<ActionItem> getActiveActionList() {

        final ActionItem[] Txt2tags_ACTIONS = {
                new ActionItem(R.string.tmaid_common_checkbox_list, R.drawable.ic_check_box_black_24dp, R.string.check_list),
                new ActionItem(R.string.tmaid_common_unordered_list_char, R.drawable.ic_list_black_24dp, R.string.unordered_list),
                new ActionItem(R.string.tmaid_txt2tags_bold, R.drawable.ic_format_bold_black_24dp, R.string.bold),
                new ActionItem(R.string.tmaid_txt2tags_strikeout, R.drawable.ic_format_strikethrough_black_24dp, R.string.strikeout),
                new ActionItem(R.string.tmaid_txt2tags_italic, R.drawable.ic_format_italic_black_24dp, R.string.italic),
                new ActionItem(R.string.tmaid_txt2tags_highlight, R.drawable.ic_format_underlined_black_24dp, R.string.highlighted),
                new ActionItem(R.string.tmaid_txt2tags_h2, R.drawable.format_header_2, R.string.heading_2),
                new ActionItem(R.string.tmaid_common_time, R.drawable.ic_access_time_black_24dp, R.string.date_and_time),
                new ActionItem(R.string.tmaid_txt2tags_h3, R.drawable.format_header_3, R.string.heading_3),
                new ActionItem(R.string.tmaid_txt2tags_h1, R.drawable.format_header_1, R.string.heading_1),
                new ActionItem(R.string.tmaid_txt2tags_code_inline, R.drawable.ic_code_black_24dp, R.string.inline_code),
                new ActionItem(R.string.tmaid_common_delete_lines, CommonTextActions.ACTION_DELETE_LINES_ICON, R.string.delete_lines),
                new ActionItem(R.string.tmaid_common_open_link_browser, CommonTextActions.ACTION_OPEN_LINK_BROWSER__ICON, R.string.open_link),
                new ActionItem(R.string.tmaid_common_special_key, CommonTextActions.ACTION_SPECIAL_KEY__ICON, R.string.special_key),
                new ActionItem(R.string.tmaid_common_ordered_list_number, R.drawable.ic_format_list_numbered_black_24dp, R.string.ordered_list),
                new ActionItem(R.string.tmaid_common_accordion, R.drawable.ic_arrow_drop_down_black_24dp, R.string.accordion),
                new ActionItem(R.string.tmaid_common_indent, R.drawable.ic_format_indent_increase_black_24dp, R.string.indent),
                new ActionItem(R.string.tmaid_common_deindent, R.drawable.ic_format_indent_decrease_black_24dp, R.string.deindent),
                new ActionItem(R.string.tmaid_txt2tags_h4, R.drawable.format_header_4, R.string.heading_4),
                new ActionItem(R.string.tmaid_txt2tags_h5, R.drawable.format_header_5, R.string.heading_5),
                new ActionItem(R.string.tmaid_txt2tags_insert_image, R.drawable.ic_image_black_24dp, R.string.insert_image),
                new ActionItem(R.string.tmaid_txt2tags_insert_link, R.drawable.ic_link_black_24dp, R.string.insert_link),
                new ActionItem(R.string.tmaid_common_new_line_below, R.drawable.ic_baseline_keyboard_return_24, R.string.start_new_line_below),
                new ActionItem(R.string.tmaid_common_move_text_one_line_up, R.drawable.ic_baseline_arrow_upward_24, R.string.move_text_one_line_up),
                new ActionItem(R.string.tmaid_common_move_text_one_line_down, R.drawable.ic_baseline_arrow_downward_24, R.string.move_text_one_line_down),
        };

        return Arrays.asList(Txt2tags_ACTIONS);
    }

    private class Txt2tagsTextActionsImpl extends ActionCallback {
        private int _action;

        Txt2tagsTextActionsImpl(int action) {
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
                case R.string.tmaid_txt2tags_h1: {
                    toggleHeading(1);
                    return true;
                }
                case R.string.tmaid_txt2tags_h2: {
                    toggleHeading(2);
                    return true;
                }
                case R.string.tmaid_txt2tags_h3: {
                    toggleHeading(3);
                    return true;
                }
                case R.string.tmaid_txt2tags_h4: {
                    toggleHeading(4);
                    return true;
                }
                case R.string.tmaid_txt2tags_h5: {
                    toggleHeading(5);
                    return true;
                }
                case R.string.tmaid_common_unordered_list_char: {
                //    runRegexReplaceAction(Txt2tagsReplacePatternGenerator.replaceWithUnorderedListPrefixOrRemovePrefix());
                runInlineAction("- ");  // todo: add extra -

                    return true;
                }
                case R.string.tmaid_common_checkbox_list: {
                    runRegexReplaceAction(Txt2tagsReplacePatternGenerator.replaceWithNextStateCheckbox());
                    return true;
                }
                case R.string.tmaid_common_ordered_list_number: {
                    //runRegexReplaceAction(Txt2tagsReplacePatternGenerator.replaceWithOrderedListPrefixOrRemovePrefix());
                    // TODO: adapt to zim wiki
                    runInlineAction("+ ");
                    //runRenumberOrderedListIfRequired();
                    return true;
                }
                case R.string.tmaid_txt2tags_bold: {
                    runInlineAction("**");
                    return true;
                }
                case R.string.tmaid_txt2tags_italic: {
                    runInlineAction("//");
                    return true;
                }
                case R.string.tmaid_txt2tags_highlight: {
                    runInlineAction("__");
                    return true;
                }
                case R.string.tmaid_txt2tags_strikeout: {
                    runInlineAction("--");
                    return true;
                }
                case R.string.tmaid_txt2tags_code_inline: {
                    runInlineAction("``");
                    return true;
                }
//                case R.string.tmaid_txt2tags_horizontal_line: {
//                    runInlineAction("------------------\n");
//                    return true;
//                }
                case R.string.tmaid_txt2tags_insert_link:
                    AttachImageOrLinkDialog.showInsertImageOrLinkDialog(AttachImageOrLinkDialog.FILE_OR_LINK_ACTION, _document.getFormat(), _activity, _hlEditor, _document.getFile());
                    return true;
                case R.string.tmaid_txt2tags_insert_image: {
                    AttachImageOrLinkDialog.showInsertImageOrLinkDialog(AttachImageOrLinkDialog.IMAGE_ACTION, _document.getFormat(), _activity, _hlEditor, _document.getFile());
                    return true;
                }
                case R.string.tmaid_common_toolbar_title_clicked_edit_action: {
                    final String origText = _hlEditor.getText().toString();
                    SearchOrCustomTextDialogCreator.showTxt2tagsHeadlineDialog(_activity, origText, (text, lineNr) -> {
                        _hlEditor.setSelection(StringUtils.getIndexFromLineOffset(origText, lineNr, 0));
                    });
                    return true;
                }
                case R.string.tmaid_common_indent:
                    runRegexReplaceAction(Txt2tagsReplacePatternGenerator.indentOneTab());
                    return true;
                case R.string.tmaid_common_deindent: {
                    runRegexReplaceAction(Txt2tagsReplacePatternGenerator.deindentOneTab());
                    //runRenumberOrderedListIfRequired();
                    return true;
                }
                default: {
                    return runCommonTextAction(_context.getString(_action));
                }
            }
        }

        private void toggleHeading(int headingLevel) {
            final CharSequence text = _hlEditor.getText();
            runRegexReplaceAction(Txt2tagsReplacePatternGenerator.setOrUnsetHeadingWithLevel(headingLevel));

            final int[] lineSelection = StringUtils.getLineSelection(_hlEditor);
            Matcher m = Txt2tagsHighlighter.Patterns.HEADING.pattern.matcher(text.subSequence(lineSelection[0], lineSelection[1]));
            if (m.find()) {
                final int afterHeadingTextOffset = m.end(3);
                final int lineStart = StringUtils.getLineStart(text, StringUtils.getSelection(_hlEditor)[0]);
                _hlEditor.setSelection(lineStart + afterHeadingTextOffset);
            }
        }

        @Override
        public boolean onLongClick(View view) {
            switch (_action) {
                case R.string.tmaid_common_checkbox_list: {
                    runRegexReplaceAction(Txt2tagsReplacePatternGenerator.removeCheckbox());
                    return true;
                }
                case R.string.tmaid_common_open_link_browser: {
                    new CommonTextActions(_activity, _hlEditor).runAction(CommonTextActions.ACTION_SEARCH);
                    return true;
                }
                case R.string.tmaid_common_special_key: {
                    new CommonTextActions(_activity, _hlEditor).runAction(CommonTextActions.ACTION_JUMP_BOTTOM_TOP);
                    return true;
                }
                case R.string.tmaid_txt2tags_insert_link: {
                    int pos = _hlEditor.getSelectionStart();
                    _hlEditor.getText().insert(pos, "[[]]");
                    _hlEditor.setSelection(pos + 2);
                    return true;
                }
                case R.string.tmaid_txt2tags_insert_image: {
                    int pos = _hlEditor.getSelectionStart();
                    _hlEditor.getText().insert(pos, "{{}}");
                    _hlEditor.setSelection(pos + 2);
                    return true;
                }
                case R.string.tmaid_common_time: {
                    runCommonTextAction("tmaid_common_time_insert_timestamp");
                    return true;
                }
                case R.string.tmaid_txt2tags_code_inline: {
                    _hlEditor.disableHighlighterAutoFormat();
                    final int c = _hlEditor.setSelectionExpandWholeLines();
                    _hlEditor.getText().insert(_hlEditor.getSelectionStart(), "\n'''\n");
                    _hlEditor.getText().insert(_hlEditor.getSelectionEnd(), "\n'''\n");
                    _hlEditor.setSelection(c + "\n'''\n".length());
                    _hlEditor.enableHighlighterAutoFormat();
                    Toast.makeText(_activity, R.string.code_block, Toast.LENGTH_SHORT).show();
                    return true;
                }
                //case R.string.tmaid_common_ordered_list_number: {
                    // TODO: adapt to zim wiki
                    //AutoFormatter.renumberOrderedList(_hlEditor.getText(), StringUtils.getSelection(_hlEditor)[0], Txt2tagsAutoFormat.getPrefixPatterns());
                 //   return true;
                //}
            }
            return false;
        }
    }

    private void runRenumberOrderedListIfRequired() {
        if (_appSettings.isMarkdownAutoUpdateList()) {
           // AutoFormatter.renumberOrderedList(_hlEditor, StringUtils.getSelection(_hlEditor)[0], Txt2tagsAutoFormat.getPrefixPatterns());
        }
    }

    public static String createTxt2tagsHeaderAndTitleContents(String fileNameWithoutExtension, Date creationDate, String creationDateLinePrefix) {
        String creationDateFormatted;
        try {
            creationDateFormatted = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ROOT).format(creationDate);
        } catch (Exception e) {
            creationDateFormatted = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZ", Locale.ROOT).format(creationDate);
            creationDateFormatted = (!creationDateFormatted.contains("+")) ? creationDateFormatted : (creationDateFormatted.substring(0, 22) + ":" + creationDateFormatted.substring(22));
        }

        String headerContentTypeLine = "Content-Type: text/txt2tags";
        String headerWikiFormatLine = " ";
        String headerCreationDateLine = "Creation-Date: " + creationDateFormatted;
        String title = fileNameWithoutExtension.trim().replaceAll("_", " ");
        String titleLine = "== " + title + " ==";
        SimpleDateFormat creationDateLineFormat = new SimpleDateFormat("'" + creationDateLinePrefix + "'" + " EEEE dd MMMM yyyy", Locale.getDefault());
        String creationDateLine = creationDateLineFormat.format(creationDate);

        String contents = headerContentTypeLine + "\n"
                + headerWikiFormatLine + "\n"
                + headerCreationDateLine + "\n\n"
                + titleLine + "\n"
                + creationDateLine + "\n";
        return contents;
    }
}
