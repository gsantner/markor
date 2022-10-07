package net.gsantner.markor.format.asciidoc;
/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/

import android.graphics.Paint;
import android.graphics.Typeface;

import net.gsantner.markor.frontend.textview.SyntaxHighlighterBase;
import net.gsantner.markor.model.AppSettings;

import java.util.regex.Pattern;

import other.writeily.format.markdown.WrMarkdownHeaderSpanCreator;

public class AsciidocSyntaxHighlighter extends SyntaxHighlighterBase {

    // check on https://regex101.com/
    // the syntax patterns are simplified

    // Monospace syntax (`) must be the outermost formatting pair (i.e., outside the
    // bold formatting pair).
    // Italic syntax (_) is always the innermost formatting pair.

    // simplified, OK for basic examples
    public final static Pattern BOLD = Pattern
            .compile("(?m)(\\*(?!\\*)\\b(.*?)\\b\\*(?!\\*))|(\\*(?!\\*)_(.*?)_\\*(?!\\*))|(\\*\\*(?!\\*)(.*?)\\*\\*)");
    // simplified, OK for basic examples
    public final static Pattern ITALICS = Pattern.compile("(?m)(\\b_(?!_)(.*?)_(?!_)\\b)|(__(?!_)(.*?)__)");
    // simplified, OK for basic examples, contains only inline code
    public final static Pattern CODE = Pattern
            .compile("(?m)(`(?!`)\\b(.*?)`(?!`))|(`(?!`)_(.*?)_`(?!`))|(`(?!`)\\*(.*?)\\*`(?!`))|(``(?!`)(.*?)``)");
    public final static Pattern HEADING_SIMPLE = Pattern.compile("(?m)^(={1,6} {1}\\S.*$)");
    public final static Pattern LIST_UNORDERED = Pattern.compile("(?m)^(\\*{1,6})( {1})\\S.*$");
    public final static Pattern LIST_ORDERED = Pattern.compile("(?m)^(\\.{1,6})( {1})\\S.*$");
    // simplified, OK for basic examples
    public final static Pattern HIGHLIGHT = Pattern.compile("(?m)(#(?!#)(.*?)#(?!#))|(##(?!#)(.*?)##)");
    // simplified, OK for basic examples
    public final static Pattern STRIKETHROUGH = Pattern
            .compile("(?m)\\[\\.line-through\\]((#(?!#)(.*?)#(?!#))|(##(?!#)(.*?)##))");
    public final static Pattern HARD_LINE_BREAK = Pattern.compile("(?m)(?<=\\S)([^\\S\\n]{1})\\+\\n");

    // simplified, OK for basic examples
    public final static Pattern LINK = Pattern.compile("link:\\S*?\\[([^\\[]*)\\]");
    public final static Pattern XREF = Pattern.compile("xref:\\S*?\\[([^\\[]*)\\]");
    public final static Pattern ACTION_IMAGE_PATTERN = Pattern.compile("image:\\S*?\\[([^\\[]*)\\]");
    // TODO: include

    // block syntax
    // simplified, contains only the most common case, like "____", "....", "----", ...
    public final static Pattern BLOCK_DELIMITED_QUOTATION = Pattern.compile("(?m)^\\_{4}[\\r\\n]([\\s\\S]+?(?=^\\_{4}[\\r\\n]))\\_{4}[\\r\\n]");
    public final static Pattern BLOCK_DELIMITED_LISTING = Pattern.compile("(?m)^\\-{4}[\\r\\n]([\\s\\S]+?(?=^\\-{4}[\\r\\n]))\\-{4}[\\r\\n]");
    public final static Pattern BLOCK_DELIMITED_LITERAL = Pattern.compile("(?m)^\\.{4}[\\r\\n]([\\s\\S]+?(?=^\\.{4}[\\r\\n]))\\.{4}[\\r\\n]");
//
//    // unchanged
////    public final static Pattern DOUBLESPACE_LINE_ENDING = Pattern.compile("(?m)(?<=\\S)([^\\S\\n]{2,})\\n");
//
//    // TODO: still markdown, but it is not used
//    public final static Pattern ACTION_LINK_PATTERN = Pattern.compile("(?m)\\[(.*?)\\]\\((.*?)\\)");
//
    // TODO better colors for red green color weakness
    // or use settings and predefined color sets for "normal" users and handicapped
    // users
    // 0xba3925ff;
    // 0x2156a5ff;

    private static final int AD_COLOR_HEADING = 0xffef6D00;
    private static final int AD_COLOR_LINK = 0xff1ea3fe;
    private static final int AD_COLOR_LIST = 0xffdaa521;
    private static final int AD_COLOR_QUOTE = 0xff88b04c;
    private static final int AD_COLOR_CODEBLOCK = 0xff8c8c8c;

    public AsciidocSyntaxHighlighter(AppSettings as) {
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
        _highlightBiggerHeadings = _appSettings.isMarkdownBiggerHeadings();
        _highlightCodeBlock = _appSettings.isHighlightCodeBlock();
        _delay = _appSettings.getMarkdownHighlightingDelay();
        return super.configure(paint);
    }

    @Override
    protected void generateSpans() {
        createTabSpans(_tabSize);
        createUnderlineHexColorsSpans();
        // TODO: font is very small, currently general setting: 85% of common size
        // hard to read on dark thema, but this is a general question for all formats,
        // not AsciiDoc specific
        // also it uses private static String formatLink(String text, String link), which is
        // adapted for Markdown
        // maybe, needs to be adapted for AsciiDoc?
        // but not in the current Pull Request
        createSmallBlueLinkSpans();

        if (_highlightBiggerHeadings) {
            createSpanForMatches(HEADING_SIMPLE,
                    new WrMarkdownHeaderSpanCreator(_spannable, AD_COLOR_HEADING, _textSize));
        } else {
            createColorSpanForMatches(HEADING_SIMPLE, AD_COLOR_HEADING);
        }

        createColorSpanForMatches(LINK, AD_COLOR_LINK);
        createColorSpanForMatches(XREF, AD_COLOR_LINK);
        createColorSpanForMatches(LIST_UNORDERED, AD_COLOR_LIST);
        createColorSpanForMatches(LIST_ORDERED, AD_COLOR_LIST);

        if (_highlightLineEnding) {
            createColorBackgroundSpan(HARD_LINE_BREAK, AD_COLOR_CODEBLOCK);
        }

        createStyleSpanForMatches(BOLD, Typeface.BOLD);
        createStyleSpanForMatches(ITALICS, Typeface.ITALIC);
        createColorSpanForMatches(BLOCK_DELIMITED_QUOTATION, AD_COLOR_QUOTE);
        createStrikeThroughSpanForMatches(STRIKETHROUGH);

        if (_highlightCodeChangeFont) {
            createMonospaceSpanForMatches(CODE);
            createMonospaceSpanForMatches(BLOCK_DELIMITED_LISTING);
            createMonospaceSpanForMatches(BLOCK_DELIMITED_LITERAL);
        }

        if (_highlightCodeBlock) {
            createColorBackgroundSpan(CODE, AD_COLOR_CODEBLOCK);
            createColorBackgroundSpan(BLOCK_DELIMITED_LISTING, AD_COLOR_CODEBLOCK);
            createColorBackgroundSpan(BLOCK_DELIMITED_LITERAL, AD_COLOR_CODEBLOCK);
        }
    }

}

