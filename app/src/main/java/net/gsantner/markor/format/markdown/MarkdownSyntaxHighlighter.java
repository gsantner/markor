/*#######################################################
 *
 *   Maintained 2018-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.markdown;

import android.graphics.Paint;
import android.graphics.Typeface;

import net.gsantner.markor.frontend.textview.SyntaxHighlighterBase;
import net.gsantner.markor.model.AppSettings;

import java.util.regex.Pattern;

import other.writeily.format.markdown.WrMarkdownHeaderSpanCreator;

public class MarkdownSyntaxHighlighter extends SyntaxHighlighterBase {

    public final static Pattern BOLD = Pattern.compile("(?<=(\\n|^|\\s|\\[|\\{|\\())(([*_]){2,3})(?=\\S)(.*?)\\S\\2(?=(\\n|$|\\s|\\.|,|:|;|-|\\]|\\}|\\)))");
    public final static Pattern ITALICS = Pattern.compile("(?<=(\\n|^|\\s|\\[|\\{|\\())([*_])(?=((?!\\2)|\\2{2,}))(?=\\S)(.*?)\\S\\2(?=(\\n|$|\\s|\\.|,|:|;|-|\\]|\\}|\\)))");
    public final static Pattern HEADING = Pattern.compile("(?m)((^#{1,6}[^\\S\\n][^\\n]+)|((\\n|^)[^\\s]+.*?\\n(-{2,}|={2,})[^\\S\\n]*$))");
    public final static Pattern HEADING_SIMPLE = Pattern.compile("(?m)^(#{1,6}\\s.*$)");
    // Group 1 matches image, Group 2 matches text, group 3 matches path
    public static final Pattern LINK = Pattern.compile("(?m)(!)?\\[([^]]*)]\\(([^()]*(?:\\([^()]*\\)[^()]*)*)\\)");
    public final static Pattern LIST_UNORDERED = Pattern.compile("(\\n|^)\\s{0,16}([*+-])( \\[[ xX]\\])?(?= )");
    public final static Pattern LIST_ORDERED = Pattern.compile("(?m)^\\s{0,16}(\\d+)(:?\\.|\\))\\s");
    public final static Pattern QUOTATION = Pattern.compile("(\\n|^)>");
    public final static Pattern STRIKETHROUGH = Pattern.compile("~{2}(.*?)\\S~{2}");
    public final static Pattern CODE = Pattern.compile("(?m)(`(?!`)(.*?)`)|(^[^\\S\\n]{4}(?![0-9\\-*+]).*$)");
    public final static Pattern DOUBLESPACE_LINE_ENDING = Pattern.compile("(?m)(?<=\\S)([^\\S\\n]{2,})\\n");

    private static final int MD_COLOR_HEADING = 0xffef6D00;
    private static final int MD_COLOR_LINK = 0xff1ea3fe;
    private static final int MD_COLOR_LIST = 0xffdaa521;
    private static final int MD_COLOR_QUOTE = 0xff88b04c;
    private static final int MD_COLOR_CODEBLOCK = 0x448c8c8c;

    public MarkdownSyntaxHighlighter(AppSettings as) {
        super(as);
    }

    private boolean _highlightLineEnding;
    private boolean _highlightCodeChangeFont;
    private boolean _highlightBiggerHeadings;
    private boolean _highlightCodeBlock;

    @Override
    public SyntaxHighlighterBase configure(Paint paint) {
        _highlightLineEnding = _appSettings.isMarkdownHighlightLineEnding();
        _highlightCodeChangeFont = _appSettings.isHighlightCodeMonospaceFont();
        _highlightBiggerHeadings = _appSettings.isHighlightBiggerHeadings();
        _highlightCodeBlock = _appSettings.isHighlightCodeBlock();
        _delay = _appSettings.getMarkdownHighlightingDelay();
        return super.configure(paint);
    }

    @Override
    protected void generateSpans() {

        createTabSpans(_tabSize);
        createUnderlineHexColorsSpans();
        createSmallBlueLinkSpans();

        if (_highlightBiggerHeadings) {
            createSpanForMatches(HEADING, new WrMarkdownHeaderSpanCreator(_spannable, MD_COLOR_HEADING));
        } else {
            createColorSpanForMatches(HEADING, MD_COLOR_HEADING);
        }

        createColorSpanForMatches(LINK, MD_COLOR_LINK);
        createColorSpanForMatches(LIST_UNORDERED, MD_COLOR_LIST);
        createColorSpanForMatches(LIST_ORDERED, MD_COLOR_LIST);

        if (_highlightLineEnding) {
            createColorBackgroundSpan(DOUBLESPACE_LINE_ENDING, MD_COLOR_CODEBLOCK);
        }

        createStyleSpanForMatches(BOLD, Typeface.BOLD);
        createStyleSpanForMatches(ITALICS, Typeface.ITALIC);
        createColorSpanForMatches(QUOTATION, MD_COLOR_QUOTE);
        createStrikeThroughSpanForMatches(STRIKETHROUGH);

        if (_highlightCodeChangeFont) {
            createMonospaceSpanForMatches(CODE);
        }

        if (_highlightCodeBlock) {
            createColorBackgroundSpan(CODE, MD_COLOR_CODEBLOCK);
        }
    }
}