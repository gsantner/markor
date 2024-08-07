/*#######################################################
 *
 *   Maintained 2017-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.wikitext;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.gsantner.markor.R;
import net.gsantner.markor.activity.DocumentActivity;
import net.gsantner.markor.format.ActionButtonBase;
import net.gsantner.markor.frontend.MarkorDialogFactory;
import net.gsantner.markor.frontend.textview.AutoTextFormatter;
import net.gsantner.markor.frontend.textview.TextViewUtils;
import net.gsantner.markor.model.Document;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

public class WikitextActionButtons extends ActionButtonBase {

    private MarkorDialogFactory.HeadlineDialogState _headlineDialogState = new MarkorDialogFactory.HeadlineDialogState();

    public WikitextActionButtons(@NonNull Context context, Document document) {
        super(context, document);
    }

    @Override
    protected @StringRes
    int getFormatActionsKey() {
        return R.string.pref_key__wikitext_action_keys;
    }

    @Override
    public List<ActionItem> getFormatActionList() {
        return Arrays.asList(
                new ActionItem(R.string.abid_common_checkbox_list, R.drawable.ic_check_box_black_24dp, R.string.check_list),
                new ActionItem(R.string.abid_common_unordered_list_char, R.drawable.ic_list_black_24dp, R.string.unordered_list),
                new ActionItem(R.string.abid_wikitext_bold, R.drawable.ic_format_bold_black_24dp, R.string.bold),
                new ActionItem(R.string.abid_wikitext_strikeout, R.drawable.ic_format_strikethrough_black_24dp, R.string.strikeout),
                new ActionItem(R.string.abid_wikitext_italic, R.drawable.ic_format_italic_black_24dp, R.string.italic),
                new ActionItem(R.string.abid_wikitext_highlight, R.drawable.ic_format_underlined_black_24dp, R.string.highlighted),
                new ActionItem(R.string.abid_wikitext_code_inline, R.drawable.ic_code_black_24dp, R.string.inline_code),
                new ActionItem(R.string.abid_wikitext_h1, R.drawable.format_header_1, R.string.heading_1),
                new ActionItem(R.string.abid_wikitext_h2, R.drawable.format_header_2, R.string.heading_2),
                new ActionItem(R.string.abid_wikitext_h3, R.drawable.format_header_3, R.string.heading_3),
                new ActionItem(R.string.abid_common_ordered_list_number, R.drawable.ic_format_list_numbered_black_24dp, R.string.ordered_list),
                new ActionItem(R.string.abid_common_accordion, R.drawable.ic_arrow_drop_down_black_24dp, R.string.accordion),
                new ActionItem(R.string.abid_common_indent, R.drawable.ic_format_indent_increase_black_24dp, R.string.indent),
                new ActionItem(R.string.abid_common_deindent, R.drawable.ic_format_indent_decrease_black_24dp, R.string.deindent),
                new ActionItem(R.string.abid_wikitext_h4, R.drawable.format_header_4, R.string.heading_4),
                new ActionItem(R.string.abid_wikitext_h5, R.drawable.format_header_5, R.string.heading_5),
                new ActionItem(R.string.abid_common_insert_audio, R.drawable.ic_keyboard_voice_black_24dp, R.string.audio),
                new ActionItem(R.string.abid_common_insert_image, R.drawable.ic_image_black_24dp, R.string.insert_image),
                new ActionItem(R.string.abid_common_insert_link, R.drawable.ic_link_black_24dp, R.string.insert_link)
        );
    }

    @Override
    public boolean onActionClick(final @StringRes int action) {
        switch (action) {
            case R.string.abid_wikitext_h1: {
                toggleHeading(1);
                return true;
            }
            case R.string.abid_wikitext_h2: {
                toggleHeading(2);
                return true;
            }
            case R.string.abid_wikitext_h3: {
                toggleHeading(3);
                return true;
            }
            case R.string.abid_wikitext_h4: {
                toggleHeading(4);
                return true;
            }
            case R.string.abid_wikitext_h5: {
                toggleHeading(5);
                return true;
            }
            case R.string.abid_common_unordered_list_char: {
                runRegexReplaceAction(WikitextReplacePatternGenerator.replaceWithUnorderedListPrefixOrRemovePrefix());
                return true;
            }
            case R.string.abid_common_checkbox_list: {
                runRegexReplaceAction(WikitextReplacePatternGenerator.replaceWithNextStateCheckbox());
                return true;
            }
            case R.string.abid_common_ordered_list_number: {
                runRegexReplaceAction(WikitextReplacePatternGenerator.replaceWithOrderedListPrefixOrRemovePrefix());
                // TODO: adapt to zim wiki
                runRenumberOrderedListIfRequired();
                return true;
            }
            case R.string.abid_wikitext_bold: {
                runSurroundAction("**");
                return true;
            }
            case R.string.abid_wikitext_italic: {
                runSurroundAction("//");
                return true;
            }
            case R.string.abid_wikitext_highlight: {
                runSurroundAction("__");
                return true;
            }
            case R.string.abid_wikitext_strikeout: {
                runSurroundAction("~~");
                return true;
            }
            case R.string.abid_wikitext_code_inline: {
                runSurroundAction("''");
                return true;
            }
            case R.string.abid_common_indent:
                runRegexReplaceAction(WikitextReplacePatternGenerator.indentOneTab());
                runRenumberOrderedListIfRequired();
                return true;
            case R.string.abid_common_deindent: {
                runRegexReplaceAction(WikitextReplacePatternGenerator.deindentOneTab());
                runRenumberOrderedListIfRequired();
                return true;
            }
            case R.string.abid_common_open_link_browser: {
                openLink();
                return true;
            }
            default: {
                return runCommonAction(action);
            }
        }
    }

    @Override
    public boolean onActionLongClick(final @StringRes int action) {
        switch (action) {
            case R.string.abid_common_checkbox_list: {
                runRegexReplaceAction(WikitextReplacePatternGenerator.removeCheckbox());
                return true;
            }
            case R.string.abid_common_insert_link: {
                int pos = _hlEditor.getSelectionStart();
                _hlEditor.getText().insert(pos, "[[]]");
                _hlEditor.setSelection(pos + 2);
                return true;
            }
            case R.string.abid_common_insert_image: {
                int pos = _hlEditor.getSelectionStart();
                _hlEditor.getText().insert(pos, "{{}}");
                _hlEditor.setSelection(pos + 2);
                return true;
            }
            case R.string.abid_wikitext_code_inline: {
                _hlEditor.withAutoFormatDisabled(() -> {
                    final int c = _hlEditor.setSelectionExpandWholeLines();
                    _hlEditor.getText().insert(_hlEditor.getSelectionStart(), "\n'''\n");
                    _hlEditor.getText().insert(_hlEditor.getSelectionEnd(), "\n'''\n");
                    _hlEditor.setSelection(c + "\n'''\n".length());
                });
                return true;
            }
            default: {
                return runCommonLongPressAction(action);
            }
        }
    }


    private void openLink() {
        String fullWikitextLink = tryExtractWikitextLink();

        if (fullWikitextLink == null) {
            // the link under the cursor is not a wikitext link, probably just a plain url
            runCommonAction(R.string.abid_common_open_link_browser);
            return;
        }

        WikitextLinkResolver resolver = WikitextLinkResolver.resolve(fullWikitextLink, _appSettings.getNotebookDirectory(), _document.getFile(), _appSettings.isWikitextDynamicNotebookRootEnabled());
        String resolvedLink = resolver.getResolvedLink();
        if (resolvedLink == null) {
            return;
        }

        if (resolver.isWebLink()) {
            getCu().openWebpageInExternalBrowser(getContext(), resolvedLink);
        } else {
            DocumentActivity.launch(getActivity(), new File(resolvedLink), false, null);
        }
    }

    private String tryExtractWikitextLink() {
        int cursorPos = TextViewUtils.getSelection(_hlEditor)[0];
        CharSequence text = _hlEditor.getText();
        int lineStart = TextViewUtils.getLineStart(text, cursorPos);
        int lineEnd = TextViewUtils.getLineEnd(text, cursorPos);
        CharSequence line = text.subSequence(lineStart, lineEnd);
        int cursorPosInLine = cursorPos - lineStart;

        Matcher m = WikitextSyntaxHighlighter.LINK.matcher(line);
        while (m.find()) {
            if (m.start() < cursorPosInLine && m.end() > cursorPosInLine) {
                return m.group();
            }
        }
        return null;
    }

    private void toggleHeading(int headingLevel) {
        final CharSequence text = _hlEditor.getText();
        runRegexReplaceAction(WikitextReplacePatternGenerator.setOrUnsetHeadingWithLevel(headingLevel));

        final int[] lineSelection = TextViewUtils.getLineSelection(_hlEditor);
        Matcher m = WikitextSyntaxHighlighter.HEADING.matcher(text.subSequence(lineSelection[0], lineSelection[1]));
        if (m.find()) {
            final int afterHeadingTextOffset = m.end(3);
            final int lineStart = TextViewUtils.getLineStart(text, TextViewUtils.getSelection(_hlEditor)[0]);
            _hlEditor.setSelection(lineStart + afterHeadingTextOffset);
        }
    }

    public static String createWikitextHeaderAndTitleContents(String fileNameWithoutExtension, Date creationDate, String creationDateLinePrefix) {
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
        final Matcher m = WikitextSyntaxHighlighter.HEADING.matcher("");
        MarkorDialogFactory.showHeadlineDialog(getActivity(), _hlEditor, _webView, _headlineDialogState, (text, start, end) -> {
            if (m.reset(text.subSequence(start, end)).find()) {
                return 7 - (m.end(2) - m.start(2));
            }
            return -1;
        });
        return true;
    }

    @Override
    protected void renumberOrderedList() {
        AutoTextFormatter.renumberOrderedList(_hlEditor.getText(), WikitextReplacePatternGenerator.formatPatterns);
    }
}