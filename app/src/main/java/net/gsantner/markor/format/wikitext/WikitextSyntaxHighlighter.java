/*#######################################################
 *
 *   Maintained 2018-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.wikitext;

import android.graphics.Paint;
import android.graphics.Typeface;

import net.gsantner.markor.frontend.textview.SyntaxHighlighterBase;
import net.gsantner.markor.model.AppSettings;

import java.util.regex.Pattern;

import other.writeily.format.wikitext.WrWikitextHeaderSpanCreator;

public class WikitextSyntaxHighlighter extends SyntaxHighlighterBase {

    //
    // Statics
    //
    public final static Pattern BOLD = Pattern.compile("(?<=(\\n|^|\\s|\\*))(\\*{2})[^*\\s](?=\\S)(.*?)[^*\\s]?\\2(?=(\\n|$|\\s|\\*))");
    public final static Pattern ITALICS = Pattern.compile("(?<=(\\n|^|\\s|/))(/{2})[^/\\s](.*?)[^/\\s]?\\2(?=(\\n|$|\\s|/))");
    public final static Pattern HIGHLIGHTED = Pattern.compile("(?<=(\\n|^|\\s|_))(_{2})[^_\\s](.*?)[^_\\s]?\\2(?=(\\n|$|\\s|_))");
    public final static Pattern STRIKETHROUGH = Pattern.compile("(?<=(\\n|^|\\s|~))(~{2})[^~\\s](.*?)[^~\\s]?\\2(?=(\\n|$|\\s|~))");
    public final static Pattern HEADING = Pattern.compile("(?<=(\\n|^|\\s))(==+)[ \\t]+(.*?)[ \\t]\\2(?=(\\n|$|\\s))");
    public final static Pattern PREFORMATTED_INLINE = Pattern.compile("''(?!')(.+?)''");
    public final static Pattern PREFORMATTED_MULTILINE = Pattern.compile("(?s)(?<=[\\n^])'''[\\n$](.*?)[\\n^]'''(?=[\\n$])");
    public final static Pattern LIST_UNORDERED = Pattern.compile("(?<=((\\n|^)\\s{0,10}))\\*(?= )");
    public final static Pattern LIST_ORDERED = Pattern.compile("(?<=((\\n|^)(\\s{0,10})))(\\d+|[a-zA-Z])(\\.)(?= )");
    public final static Pattern LINK = WikitextLinkResolver.Patterns.LINK.pattern;
    public final static Pattern IMAGE = Pattern.compile("(\\{\\{(?!\\{)(.*?)\\}\\})");
    public final static Pattern CHECKLIST = Pattern.compile("(?<=(\\n|^))\t*(\\[)([ x*><])(])(?= )");
    public final static Pattern CHECKLIST_UNCHECKED = Pattern.compile("(?<=(\\n|^))\t*(\\[)( )(])(?= )");
    public final static Pattern CHECKLIST_CHECKED = Pattern.compile("(?<=(\\n|^))\t*(\\[)(\\*)(])(?= )");
    public final static Pattern CHECKLIST_CROSSED = Pattern.compile("(?<=(\\n|^))\t*(\\[)(x)(])(?= )");
    public final static Pattern CHECKLIST_RIGHT_ARROW = Pattern.compile("(?<=(\\n|^))\t*(\\[)(>)(])(?= )");
    public final static Pattern CHECKLIST_LEFT_ARROW = Pattern.compile("(?<=(\\n|^))\t*(\\[)(<)(])(?= )");
    public final static Pattern SUBSCRIPT = Pattern.compile("(_\\{(?!~)(.+?)\\})");
    public final static Pattern SUPERSCRIPT = Pattern.compile("(\\^\\{(?!~)(.+?)\\})");
    public final static Pattern ZIMHEADER_CONTENT_TYPE_ONLY = Pattern.compile("^\\s*Content-Type:\\s*text/x-zim-wiki");
    public final static Pattern ZIMHEADER = Pattern.compile(
            "^Content-Type: text/x-zim-wiki(\r\n|\r|\n)" +
                    "Wiki-Format: zim \\d+\\.\\d+(\r\n|\r|\n)" +
                    "Creation-Date: \\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[.+:\\d]+");

    // groups for matching individual parts of the checklist regex
    public static final int CHECKBOX_LEFT_BRACKET_GROUP = 2;
    public static final int CHECKBOX_SYMBOL_GROUP = 3;
    public static final int CHECKBOX_RIGHT_BRACKET_GROUP = 4;

    public static class Colors {
        private static final int COLOR_HEADING = 0xff4e9a06;
        private static final int HIGHLIGHT_BACKGROUND_COLOR = 0xffFFA062;   // zim original color: 0xffffff00
        private static final int UNORDERED_LIST_BULLET_COLOR = 0xffdaa521;
        private static final int CHECKLIST_BASE_COLOR = UNORDERED_LIST_BULLET_COLOR;
        private static final int CHECKLIST_ARROW_COLOR = CHECKLIST_BASE_COLOR;
        private static final int ORDERED_LIST_NUMBER_COLOR = 0xffdaa521;
        private static final int LINK_COLOR = 0xff1ea3fd; // zim original color: 0xff0000ff
        private static final int CHECKLIST_CHECKED_COLOR = 0xff54a309;
        private static final int CHECKLIST_CROSSED_COLOR = 0xffa90000;
        private static final int ZIMHEADER_COLOR = 0xff808080;
        private static final int CODEBLOCK_COLOR = 0xff8c8c8c;
    }

    //
    // Members
    //

    public WikitextSyntaxHighlighter(AppSettings as) {
        super(as);
    }

    private boolean _isWikitextBiggerHeadings = false;
    private String _fontFamily = "";
    private boolean _isHighlightCodeBlock = false;
    private boolean _isHighlightCodeMonospace = false;

    @Override
    public SyntaxHighlighterBase configure(final Paint paint) {
        _delay = _appSettings.getMarkdownHighlightingDelay();
        _isWikitextBiggerHeadings = _appSettings.isHighlightBiggerHeadings();
        _fontFamily = _appSettings.getFontFamily();
        _isHighlightCodeMonospace = _appSettings.isHighlightCodeMonospaceFont();
        _isHighlightCodeBlock = _appSettings.isHighlightCodeBlock();
        return super.configure(paint);
    }

    @Override
    protected void generateSpans() {

        createTabSpans(_tabSize);
        createUnderlineHexColorsSpans();

        if (_isWikitextBiggerHeadings) {
            createSpanForMatches(HEADING, new WrWikitextHeaderSpanCreator(_spannable, Colors.COLOR_HEADING));
        } else {
            createColorSpanForMatches(HEADING, Colors.COLOR_HEADING);
        }

        createStyleSpanForMatches(BOLD, Typeface.BOLD);

        createStyleSpanForMatches(ITALICS, Typeface.ITALIC);

        createColorBackgroundSpan(HIGHLIGHTED, Colors.HIGHLIGHT_BACKGROUND_COLOR);

        createStrikeThroughSpanForMatches(STRIKETHROUGH);

        if (_isHighlightCodeMonospace) {
            createMonospaceSpanForMatches(PREFORMATTED_INLINE);
            createMonospaceSpanForMatches(PREFORMATTED_MULTILINE);
        }

        if (_isHighlightCodeBlock) {
            createColorBackgroundSpan(PREFORMATTED_INLINE, Colors.CODEBLOCK_COLOR);
            createColorBackgroundSpan(PREFORMATTED_MULTILINE, Colors.CODEBLOCK_COLOR);
        }

        createColorSpanForMatches(LIST_UNORDERED, Colors.UNORDERED_LIST_BULLET_COLOR);

        createColorSpanForMatches(LIST_ORDERED, Colors.ORDERED_LIST_NUMBER_COLOR);

        createSmallBlueLinkSpans();
        createColorSpanForMatches(LINK, Colors.LINK_COLOR);

        createSuperscriptStyleSpanForMatches(SUPERSCRIPT);

        createSubscriptStyleSpanForMatches(SUBSCRIPT);

        createCheckboxSpansForAllCheckStates();

        createColorSpanForMatches(ZIMHEADER, Colors.ZIMHEADER_COLOR);

    }

    private void createCheckboxSpansForAllCheckStates() {
        createCheckboxSpanWithDifferentColors(CHECKLIST_UNCHECKED, 0xffffffff);
        createCheckboxSpanWithDifferentColors(CHECKLIST_CHECKED, Colors.CHECKLIST_CHECKED_COLOR);
        createCheckboxSpanWithDifferentColors(CHECKLIST_CROSSED, Colors.CHECKLIST_CROSSED_COLOR);
        createCheckboxSpanWithDifferentColors(CHECKLIST_RIGHT_ARROW, Colors.CHECKLIST_ARROW_COLOR);
        createCheckboxSpanWithDifferentColors(CHECKLIST_LEFT_ARROW, Colors.CHECKLIST_ARROW_COLOR);
    }

    private void createCheckboxSpanWithDifferentColors(final Pattern checkboxPattern, final int symbolColor) {
        createColorSpanForMatches(checkboxPattern, Colors.CHECKLIST_BASE_COLOR, CHECKBOX_LEFT_BRACKET_GROUP);
        createColorSpanForMatches(checkboxPattern, symbolColor, CHECKBOX_SYMBOL_GROUP);
        createColorSpanForMatches(checkboxPattern, Colors.CHECKLIST_BASE_COLOR, CHECKBOX_RIGHT_BRACKET_GROUP);
    }
}