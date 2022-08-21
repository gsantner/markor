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
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.StringRes;

import net.gsantner.markor.R;
import net.gsantner.markor.activity.DocumentActivity;
import net.gsantner.markor.format.AutoFormatter;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.AttachImageOrLinkDialog;
import net.gsantner.markor.ui.SearchOrCustomTextDialogCreator;
import net.gsantner.opoc.util.ContextUtils;
import net.gsantner.opoc.util.StringUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

public class ZimWikiTextActions extends net.gsantner.markor.ui.hleditor.TextActions {

    public ZimWikiTextActions(Activity activity, Document document) {
        super(activity, document);
    }

    @Override
    protected @StringRes
    int getFormatActionsKey() {
        return R.string.pref_key__zimwiki__action_keys;
    }

    @Override
    public List<ActionItem> getActiveActionList() {

        final ActionItem[] ZIMWIKI_ACTIONS = {
                new ActionItem(R.string.tmaid_common_checkbox_list, R.drawable.ic_check_box_black_24dp, R.string.check_list),
                new ActionItem(R.string.tmaid_common_unordered_list_char, R.drawable.ic_list_black_24dp, R.string.unordered_list),
                new ActionItem(R.string.tmaid_zimwiki_bold, R.drawable.ic_format_bold_black_24dp, R.string.bold),
                new ActionItem(R.string.tmaid_zimwiki_strikeout, R.drawable.ic_format_strikethrough_black_24dp, R.string.strikeout),
                new ActionItem(R.string.tmaid_zimwiki_italic, R.drawable.ic_format_italic_black_24dp, R.string.italic),
                new ActionItem(R.string.tmaid_zimwiki_highlight, R.drawable.ic_format_underlined_black_24dp, R.string.highlighted),
                new ActionItem(R.string.tmaid_zimwiki_code_inline, R.drawable.ic_code_black_24dp, R.string.inline_code),
                new ActionItem(R.string.tmaid_zimwiki_h1, R.drawable.format_header_1, R.string.heading_1),
                new ActionItem(R.string.tmaid_zimwiki_h2, R.drawable.format_header_2, R.string.heading_2),
                new ActionItem(R.string.tmaid_zimwiki_h3, R.drawable.format_header_3, R.string.heading_3),
                new ActionItem(R.string.tmaid_common_delete_lines, R.drawable.ic_delete_black_24dp, R.string.delete_lines),
                new ActionItem(R.string.tmaid_common_open_link_browser, R.drawable.ic_open_in_browser_black_24dp, R.string.open_link),
                new ActionItem(R.string.tmaid_common_special_key, R.drawable.ic_keyboard_black_24dp, R.string.special_key),
                new ActionItem(R.string.tmaid_common_time, R.drawable.ic_access_time_black_24dp, R.string.date_and_time),
                new ActionItem(R.string.tmaid_common_ordered_list_number, R.drawable.ic_format_list_numbered_black_24dp, R.string.ordered_list),
                new ActionItem(R.string.tmaid_common_accordion, R.drawable.ic_arrow_drop_down_black_24dp, R.string.accordion),
                new ActionItem(R.string.tmaid_common_indent, R.drawable.ic_format_indent_increase_black_24dp, R.string.indent),
                new ActionItem(R.string.tmaid_common_deindent, R.drawable.ic_format_indent_decrease_black_24dp, R.string.deindent),
                new ActionItem(R.string.tmaid_zimwiki_h4, R.drawable.format_header_4, R.string.heading_4),
                new ActionItem(R.string.tmaid_zimwiki_h5, R.drawable.format_header_5, R.string.heading_5),
                new ActionItem(R.string.tmaid_zimwiki_insert_image, R.drawable.ic_image_black_24dp, R.string.insert_image),
                new ActionItem(R.string.tmaid_zimwiki_insert_link, R.drawable.ic_link_black_24dp, R.string.insert_link),
                new ActionItem(R.string.tmaid_common_new_line_below, R.drawable.ic_baseline_keyboard_return_24, R.string.start_new_line_below),
                new ActionItem(R.string.tmaid_common_move_text_one_line_up, R.drawable.ic_baseline_arrow_upward_24, R.string.move_text_one_line_up),
                new ActionItem(R.string.tmaid_common_move_text_one_line_down, R.drawable.ic_baseline_arrow_downward_24, R.string.move_text_one_line_down),
                new ActionItem(R.string.tmaid_common_insert_snippet, R.drawable.ic_baseline_file_copy_24, R.string.insert_snippet),

                new ActionItem(R.string.tmaid_common_web_jump_to_very_top_or_bottom, R.drawable.ic_vertical_align_center_black_24dp, R.string.jump_to_bottom, ActionItem.DisplayMode.VIEW),
                new ActionItem(R.string.tmaid_common_web_jump_to_table_of_contents, R.drawable.ic_list_black_24dp, R.string.table_of_contents, ActionItem.DisplayMode.VIEW),
        };

        return Arrays.asList(ZIMWIKI_ACTIONS);
    }

    @Override
    public boolean onActionClick(final @StringRes int action) {
        switch (action) {
            case R.string.tmaid_zimwiki_h1: {
                toggleHeading(1);
                return true;
            }
            case R.string.tmaid_zimwiki_h2: {
                toggleHeading(2);
                return true;
            }
            case R.string.tmaid_zimwiki_h3: {
                toggleHeading(3);
                return true;
            }
            case R.string.tmaid_zimwiki_h4: {
                toggleHeading(4);
                return true;
            }
            case R.string.tmaid_zimwiki_h5: {
                toggleHeading(5);
                return true;
            }
            case R.string.tmaid_common_unordered_list_char: {
                runRegexReplaceAction(ZimWikiReplacePatternGenerator.replaceWithUnorderedListPrefixOrRemovePrefix());
                return true;
            }
            case R.string.tmaid_common_checkbox_list: {
                runRegexReplaceAction(ZimWikiReplacePatternGenerator.replaceWithNextStateCheckbox());
                return true;
            }
            case R.string.tmaid_common_ordered_list_number: {
                runRegexReplaceAction(ZimWikiReplacePatternGenerator.replaceWithOrderedListPrefixOrRemovePrefix());
                // TODO: adapt to zim wiki
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
            case R.string.tmaid_zimwiki_highlight: {
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
            // case R.string.tmaid_zimwiki_horizontal_line: {
            //       runInlineAction("----\n");
            //       return true;
            // }
            case R.string.tmaid_zimwiki_insert_link:
                AttachImageOrLinkDialog.showInsertImageOrLinkDialog(AttachImageOrLinkDialog.FILE_OR_LINK_ACTION, _document.getFormat(), _activity, _hlEditor, _document.getFile());
                return true;
            case R.string.tmaid_zimwiki_insert_image: {
                AttachImageOrLinkDialog.showInsertImageOrLinkDialog(AttachImageOrLinkDialog.IMAGE_ACTION, _document.getFormat(), _activity, _hlEditor, _document.getFile());
                return true;
            }
            case R.string.tmaid_common_indent:
                runRegexReplaceAction(ZimWikiReplacePatternGenerator.indentOneTab());
                runRenumberOrderedListIfRequired();
                return true;
            case R.string.tmaid_common_deindent: {
                runRegexReplaceAction(ZimWikiReplacePatternGenerator.deindentOneTab());
                runRenumberOrderedListIfRequired();
                return true;
            }
            case R.string.tmaid_common_open_link_browser: {
                openLink();
                return true;
            }
            default: {
                return runCommonTextAction(action);
            }
        }
    }

    @Override
    public boolean onActionLongClick(final @StringRes int action) {
        switch (action) {
            case R.string.tmaid_common_checkbox_list: {
                runRegexReplaceAction(ZimWikiReplacePatternGenerator.removeCheckbox());
                return true;
            }
            case R.string.tmaid_zimwiki_insert_link: {
                int pos = _hlEditor.getSelectionStart();
                _hlEditor.getText().insert(pos, "[[]]");
                _hlEditor.setSelection(pos + 2);
                return true;
            }
            case R.string.tmaid_zimwiki_insert_image: {
                int pos = _hlEditor.getSelectionStart();
                _hlEditor.getText().insert(pos, "{{}}");
                _hlEditor.setSelection(pos + 2);
                return true;
            }
            case R.string.tmaid_zimwiki_code_inline: {
                _hlEditor.withAutoFormatDisabled(() -> {
                    final int c = _hlEditor.setSelectionExpandWholeLines();
                    _hlEditor.getText().insert(_hlEditor.getSelectionStart(), "\n'''\n");
                    _hlEditor.getText().insert(_hlEditor.getSelectionEnd(), "\n'''\n");
                    _hlEditor.setSelection(c + "\n'''\n".length());
                });
                Toast.makeText(_activity, R.string.code_block, Toast.LENGTH_SHORT).show();
                return true;
            }
            default: {
                return runCommonLongPressTextActions(action);
            }
        }
    }


    private void openLink() {
        String fullZimLink = tryExtractZimLink();

        if (fullZimLink == null) {
            // the link under the cursor is not a zim link, probably just a plain url
            runCommonTextAction(R.string.tmaid_common_open_link_browser);
            return;
        }

        ZimWikiLinkResolver resolver = ZimWikiLinkResolver.resolve(fullZimLink, _appSettings.getNotebookDirectory(), _document.getFile(), _appSettings.isZimWikiDynamicNotebookRootEnabled());
        String resolvedLink = resolver.getResolvedLink();
        if (resolvedLink == null) {
            return;
        }

        if (resolver.isWebLink()) {
            new ContextUtils(_activity).openWebpageInExternalBrowser(resolvedLink);
        } else {
            DocumentActivity.launch(_activity, new File(resolvedLink), false, null, null);
        }
    }

    private String tryExtractZimLink() {
        int cursorPos = StringUtils.getSelection(_hlEditor)[0];
        CharSequence text = _hlEditor.getText();
        int lineStart = StringUtils.getLineStart(text, cursorPos);
        int lineEnd = StringUtils.getLineEnd(text, cursorPos);
        CharSequence line = text.subSequence(lineStart, lineEnd);
        int cursorPosInLine = cursorPos - lineStart;

        Matcher m = ZimWikiHighlighter.Patterns.LINK.pattern.matcher(line);
        while (m.find()) {
            if (m.start() < cursorPosInLine && m.end() > cursorPosInLine) {
                return m.group();
            }
        }
        return null;
    }

    private void toggleHeading(int headingLevel) {
        final CharSequence text = _hlEditor.getText();
        runRegexReplaceAction(ZimWikiReplacePatternGenerator.setOrUnsetHeadingWithLevel(headingLevel));

        final int[] lineSelection = StringUtils.getLineSelection(_hlEditor);
        Matcher m = ZimWikiHighlighter.Patterns.HEADING.pattern.matcher(text.subSequence(lineSelection[0], lineSelection[1]));
        if (m.find()) {
            final int afterHeadingTextOffset = m.end(3);
            final int lineStart = StringUtils.getLineStart(text, StringUtils.getSelection(_hlEditor)[0]);
            _hlEditor.setSelection(lineStart + afterHeadingTextOffset);
        }
    }

    public static String createZimWikiHeaderAndTitleContents(String fileNameWithoutExtension, Date creationDate, String creationDateLinePrefix) {
        String creationDateFormatted;
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                throw new Exception();
            }
            creationDateFormatted = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ROOT).format(creationDate);
        } catch (Exception e) {
            creationDateFormatted = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZ", Locale.ROOT).format(creationDate);
            creationDateFormatted = (!creationDateFormatted.contains("+")) ? creationDateFormatted : (creationDateFormatted.substring(0, 22) + ":" + creationDateFormatted.substring(22));
        }

        String headerContentTypeLine = "Content-Type: text/x-zim-wiki";
        String headerWikiFormatLine = "Wiki-Format: zim 0.6";
        String headerCreationDateLine = "Creation-Date: " + creationDateFormatted;
        String title = fileNameWithoutExtension.trim().replaceAll("_", " ");
        String titleLine = "====== " + title + " ======";
        SimpleDateFormat creationDateLineFormat = new SimpleDateFormat("'" + creationDateLinePrefix + "'" + " EEEE dd MMMM yyyy", Locale.getDefault());
        String creationDateLine = creationDateLineFormat.format(creationDate);

        String contents = headerContentTypeLine + "\n"
                + headerWikiFormatLine + "\n"
                + headerCreationDateLine + "\n\n"
                + titleLine + "\n"
                + creationDateLine + "\n";
        return contents;
    }

    @Override
    public boolean runTitleClick() {
        SearchOrCustomTextDialogCreator.showHeadlineDialog(ZimWikiHighlighter.Patterns.HEADING.pattern.toString(), _activity, _hlEditor);
        return true;
    }

    @Override
    protected void renumberOrderedList(final int position) {
        AutoFormatter.renumberOrderedList(_hlEditor.getText(), position, ZimWikiAutoFormat.getPrefixPatterns());
    }
}