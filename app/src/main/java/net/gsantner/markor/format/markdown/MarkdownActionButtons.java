/*#######################################################
 *
 *   Maintained 2017-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.markdown;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Editable;
import android.view.KeyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.gsantner.markor.R;
import net.gsantner.markor.activity.DocumentActivity;
import net.gsantner.markor.format.ActionButtonBase;
import net.gsantner.markor.frontend.MarkorDialogFactory;
import net.gsantner.markor.frontend.textview.AutoTextFormatter;
import net.gsantner.markor.frontend.textview.TextViewUtils;
import net.gsantner.markor.model.Document;
import net.gsantner.opoc.util.GsContextUtils;
import net.gsantner.opoc.util.GsFileUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownActionButtons extends ActionButtonBase {

    private static final Pattern WEB_URL = Pattern.compile("https?://[^\\s/$.?#].[^\\s]*");

    private final MarkorDialogFactory.HeadlineDialogState _headlineDialogState = new MarkorDialogFactory.HeadlineDialogState();

    public static final String LINE_PREFIX = "^(>\\s|#{1,6}\\s|\\s*[-*+](?:\\s\\[[ xX]\\])?\\s|\\s*\\d+[.)]\\s)?";

    // Patterns used for surrounding entire lines
    // ----------------------------------------------------------------------------
    // TODO - make these more intelligent. Should work with combined delimiters.
    // Goup 1: Prefix, Group 2: Pre-space, Group 3: Open delim, Group 4: Text, Group 5: Close delim, Group 6: Post-space
    public static final Pattern LINE_BOLD = Pattern.compile(LINE_PREFIX + "(\\s*)(\\*\\*)(\\S.*\\S)(\\3)(\\s*)$");
    public static final Pattern LINE_ITALIC = Pattern.compile(LINE_PREFIX + "(\\s*)(_)(\\S.*\\S)(\\3)(\\s*)$");
    public static final Pattern LINE_STRIKEOUT = Pattern.compile(LINE_PREFIX + "(\\s*)(~~)(\\S.*\\S)(\\3)(\\s*)$");
    // Group 1: Prefix, Group 2: Pre-space, Group 3: Text, Group 4: Post-space
    public static final Pattern LINE_NONE = Pattern.compile(LINE_PREFIX + "(\\s*)(.*?)(\\s*)$");
    // ----------------------------------------------------------------------------

    public static final Pattern CHECKED_LIST_LINE = Pattern.compile("^(\\s*)(([-*+])\\s\\[([xX ])\\]\\s)");

    public MarkdownActionButtons(@NonNull Context context, Document document) {
        super(context, document);
    }

    @Override
    protected @StringRes
    int getFormatActionsKey() {
        return R.string.pref_key__markdown__action_keys;
    }

    @Override
    public List<ActionItem> getFormatActionList() {
        return Arrays.asList(
                new ActionItem(R.string.abid_common_checkbox_list, R.drawable.ic_check_box_black_24dp, R.string.check_list),
                new ActionItem(R.string.abid_common_unordered_list_char, R.drawable.ic_list_black_24dp, R.string.unordered_list),
                new ActionItem(R.string.abid_common_ordered_list_number, R.drawable.ic_format_list_numbered_black_24dp, R.string.ordered_list),
                new ActionItem(R.string.abid_markdown_bold, R.drawable.ic_format_bold_black_24dp, R.string.bold),
                new ActionItem(R.string.abid_markdown_italic, R.drawable.ic_format_italic_black_24dp, R.string.italic),
                new ActionItem(R.string.abid_markdown_strikeout, R.drawable.ic_format_strikethrough_black_24dp, R.string.strikeout),
                new ActionItem(R.string.abid_common_insert_link, R.drawable.ic_link_black_24dp, R.string.insert_link),
                new ActionItem(R.string.abid_common_insert_image, R.drawable.ic_image_black_24dp, R.string.insert_image),
                new ActionItem(R.string.abid_common_insert_audio, R.drawable.ic_keyboard_voice_black_24dp, R.string.audio),
                new ActionItem(R.string.abid_markdown_code_inline, R.drawable.ic_code_black_24dp, R.string.inline_code),
                new ActionItem(R.string.abid_markdown_table_insert_columns, R.drawable.ic_view_module_black_24dp, R.string.table),
                new ActionItem(R.string.abid_markdown_quote, R.drawable.ic_format_quote_black_24dp, R.string.quote),
                new ActionItem(R.string.abid_markdown_h1, R.drawable.format_header_1, R.string.heading_1),
                new ActionItem(R.string.abid_markdown_h2, R.drawable.format_header_2, R.string.heading_2),
                new ActionItem(R.string.abid_markdown_h3, R.drawable.format_header_3, R.string.heading_3),
                new ActionItem(R.string.abid_markdown_horizontal_line, R.drawable.ic_more_horiz_black_24dp, R.string.horizontal_line),
                new ActionItem(R.string.abid_common_indent, R.drawable.ic_format_indent_increase_black_24dp, R.string.indent),
                new ActionItem(R.string.abid_common_deindent, R.drawable.ic_format_indent_decrease_black_24dp, R.string.deindent),
                new ActionItem(R.string.abid_common_accordion, R.drawable.ic_arrow_drop_down_black_24dp, R.string.accordion)
        );
    }

    @Override
    public boolean onActionClick(final @StringRes int action) {
        switch (action) {
            case R.string.abid_markdown_quote: {
                runRegexReplaceAction(MarkdownReplacePatternGenerator.toggleQuote());
                return true;
            }
            case R.string.abid_markdown_h1: {
                runRegexReplaceAction(MarkdownReplacePatternGenerator.setOrUnsetHeadingWithLevel(1));
                return true;
            }
            case R.string.abid_markdown_h2: {
                runRegexReplaceAction(MarkdownReplacePatternGenerator.setOrUnsetHeadingWithLevel(2));
                return true;
            }
            case R.string.abid_markdown_h3: {
                runRegexReplaceAction(MarkdownReplacePatternGenerator.setOrUnsetHeadingWithLevel(3));
                return true;
            }
            case R.string.abid_common_unordered_list_char: {
                final String listChar = _appSettings.getUnorderedListCharacter();
                runRegexReplaceAction(MarkdownReplacePatternGenerator.replaceWithUnorderedListPrefixOrRemovePrefix(listChar));
                return true;
            }
            case R.string.abid_common_checkbox_list: {
                final String listChar = _appSettings.getUnorderedListCharacter();
                runRegexReplaceAction(MarkdownReplacePatternGenerator.toggleToCheckedOrUncheckedListPrefix(listChar));
                return true;
            }
            case R.string.abid_common_ordered_list_number: {
                runRegexReplaceAction(MarkdownReplacePatternGenerator.replaceWithOrderedListPrefixOrRemovePrefix());
                runRenumberOrderedListIfRequired();
                return true;
            }
            case R.string.abid_markdown_bold: {
                runSurroundAction("**");
                return true;
            }
            case R.string.abid_markdown_italic: {
                runSurroundAction("_");
                return true;
            }
            case R.string.abid_markdown_strikeout: {
                runSurroundAction("~~");
                return true;
            }
            case R.string.abid_markdown_code_inline: {
                runSurroundAction("`");
                return true;
            }
            case R.string.abid_markdown_horizontal_line: {
                _hlEditor.insertOrReplaceTextOnCursor("----\n");
                return true;
            }
            case R.string.abid_markdown_table_insert_columns: {
                MarkorDialogFactory.showInsertTableRowDialog(getActivity(), false, this::insertTableRow);
                return true;
            }
            case R.string.abid_common_open_link_browser: {
                if (followLinkUnderCursor()) {
                    return true;
                }
            }
            default: {
                return runCommonAction(action);
            }
        }
    }

    /**
     * Used to surround selected text with a given delimiter (and remove it if present)
     * <p>
     * Not super intelligent about how patterns can be combined.
     * Current regexes just look for the litera delimiters.
     *
     * @param pattern - Pattern to match if delimiter is present
     * @param delim   - Delimiter to surround text with
     */
    private void runLineSurroundAction(final Pattern pattern, final String delim) {
        final int[] sel = TextViewUtils.getSelection(_hlEditor);
        final String lineBefore = sel[0] == sel[1] ? TextViewUtils.getSelectedLines(_hlEditor, sel[0]) : null;

        runRegexReplaceAction(
                new ReplacePattern(pattern, "$1$2$4$6"),
                new ReplacePattern(LINE_NONE, "$1$2" + delim + "$3" + delim + "$4")
        );

        // This logic sets the cursor to the inside of the delimiters if the delimiters were empty
        if (lineBefore != null) {
            final String lineAfter = TextViewUtils.getSelectedLines(_hlEditor, sel[0]);
            final String pair = delim + delim;
            if (lineAfter.length() - lineBefore.length() == pair.length() && lineAfter.trim().endsWith(pair)) {
                final Editable text = _hlEditor.getText();
                final int end = TextViewUtils.getLineEnd(text, sel[0]);
                final int ns = TextViewUtils.getLastNonWhitespace(text, end) - delim.length();
                _hlEditor.setSelection(ns);
            }
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onActionLongClick(final @StringRes int action) {
        switch (action) {
            case R.string.abid_markdown_table_insert_columns: {
                MarkorDialogFactory.showInsertTableRowDialog(getActivity(), true, this::insertTableRow);
                return true;
            }
            case R.string.abid_markdown_code_inline: {
                _hlEditor.withAutoFormatDisabled(() -> {
                    final int c = _hlEditor.setSelectionExpandWholeLines();
                    _hlEditor.getText().insert(_hlEditor.getSelectionStart(), "\n```\n");
                    _hlEditor.getText().insert(_hlEditor.getSelectionEnd(), "\n```\n");
                    _hlEditor.setSelection(c + "\n```\n".length());
                });
                return true;
            }
            case R.string.abid_markdown_bold: {
                runLineSurroundAction(LINE_BOLD, "**");
                return true;
            }
            case R.string.abid_markdown_italic: {
                runLineSurroundAction(LINE_ITALIC, "_");
                return true;
            }
            case R.string.abid_markdown_strikeout: {
                runLineSurroundAction(LINE_STRIKEOUT, "~~");
                return true;
            }
            case R.string.abid_common_checkbox_list: {
                MarkorDialogFactory.showDocumentChecklistDialog(
                        getActivity(), _hlEditor.getText(), CHECKED_LIST_LINE, 4, "xX", " ",
                        pos -> TextViewUtils.setSelectionAndShow(_hlEditor, pos));
                return true;
            }
            default: {
                return runCommonLongPressAction(action);
            }
        }
    }

    public static class Link {
        public final String title, link;
        public final boolean isImage;
        public final int start, end;

        private Link(String title, String link, boolean isImage, int start, int end) {
            this.title = title;
            this.link = link;
            this.isImage = isImage;
            this.start = start;
            this.end = end;
        }

        public boolean isValid() {
            return !link.isEmpty() && start >= 0 && end >= 0;
        }

        public static Link extract(final CharSequence text, final int pos) {
            final int[] sel = TextViewUtils.getLineSelection(text, pos);
            if (sel != null && sel[0] != -1 && sel[1] != -1) {
                final String line = text.subSequence(sel[0], sel[1]).toString();
                final Matcher m = MarkdownSyntaxHighlighter.LINK.matcher(line);
                final int po = pos - sel[0];

                while (m.find()) {
                    final int start = m.start(), end = m.end();
                    if (start <= po && end >= po) {
                        final boolean isImage = m.group(1) != null;
                        return new Link(m.group(2), m.group(3), isImage, start, end);
                    }
                }
            }

            return new Link("", "", false, -1, -1);
        }
    }

    private boolean followLinkUnderCursor() {
        final int sel = TextViewUtils.getSelection(_hlEditor)[0];
        if (sel < 0) {
            return false;
        }

        final Link link = Link.extract(_hlEditor.getText(), sel);
        if (link.isValid()) {
            if (WEB_URL.matcher(link.link).matches()) {
                GsContextUtils.instance.openWebpageInExternalBrowser(getActivity(), link.link);
                return true;
            } else {
                final File f = GsFileUtils.makeAbsolute(link.link, _document.getFile().getParentFile());
                if (GsFileUtils.canCreate(f)) {
                    DocumentActivity.launch(getActivity(), f, null, null);
                    return true;
                }
            }
        }

        return false;
    }

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
    public boolean runTitleClick() {
        final Matcher m = MarkdownReplacePatternGenerator.PREFIX_ATX_HEADING.matcher("");
        MarkorDialogFactory.showHeadlineDialog(getActivity(), _hlEditor, _webView, _headlineDialogState, (text, start, end) -> {
            if (m.reset(text.subSequence(start, end)).find()) {
                return m.end(2) - m.start(2) - 1;
            }
            return -1;
        });
        return true;
    }

    @Override
    protected void renumberOrderedList() {
        AutoTextFormatter.renumberOrderedList(_hlEditor.getText(), MarkdownReplacePatternGenerator.formatPatterns);
    }
}
