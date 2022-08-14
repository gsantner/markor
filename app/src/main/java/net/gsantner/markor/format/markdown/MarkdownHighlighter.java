/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.markdown;

import android.graphics.Paint;
import android.graphics.Typeface;

import net.gsantner.markor.ui.hleditor.Highlighter;
import net.gsantner.markor.util.AppSettings;

import other.writeily.format.markdown.WrMarkdownHeaderSpanCreator;

public class MarkdownHighlighter extends Highlighter {

    private static final int MD_COLOR_HEADING = 0xffef6D00;
    private static final int MD_COLOR_LINK = 0xff1ea3fe;
    private static final int MD_COLOR_LIST = 0xffdaa521;
    private static final int MD_COLOR_QUOTE = 0xff88b04c;
    private static final int MD_COLOR_CODEBLOCK = 0xff8c8c8c;

    public MarkdownHighlighter(AppSettings as) {
        super(as);
    }

    private boolean _highlightLineEnding;
    private boolean _highlightCodeChangeFont;
    private boolean _highlightBiggerHeadings;
    private boolean _highlightCodeBlock;

    @Override
    public Highlighter configure(Paint paint) {
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

        if (_highlightBiggerHeadings) {
            createSpanForMatches(MarkdownHighlighterPattern.HEADING.pattern,
                    new WrMarkdownHeaderSpanCreator(_spannable, MD_COLOR_HEADING, _textSize));
        } else {
            createColorSpanForMatches(MarkdownHighlighterPattern.HEADING_SIMPLE.pattern, MD_COLOR_HEADING);
        }

        createColorSpanForMatches(MarkdownHighlighterPattern.LINK.pattern, MD_COLOR_LINK);
        createColorSpanForMatches(MarkdownHighlighterPattern.LIST_UNORDERED.pattern, MD_COLOR_LIST);
        createColorSpanForMatches(MarkdownHighlighterPattern.LIST_ORDERED.pattern, MD_COLOR_LIST);

        if (_highlightLineEnding) {
            createColorBackgroundSpan(MarkdownHighlighterPattern.DOUBLESPACE_LINE_ENDING.pattern, MD_COLOR_CODEBLOCK);
        }

        createStyleSpanForMatches(MarkdownHighlighterPattern.BOLD.pattern, Typeface.BOLD);
        createStyleSpanForMatches(MarkdownHighlighterPattern.ITALICS.pattern, Typeface.ITALIC);
        createColorSpanForMatches(MarkdownHighlighterPattern.QUOTATION.pattern, MD_COLOR_QUOTE);
        createStrikeThroughSpanForMatches(MarkdownHighlighterPattern.STRIKETHROUGH.pattern);

        if (_highlightCodeChangeFont) {
            createMonospaceSpanForMatches(MarkdownHighlighterPattern.CODE.pattern);
        }

        if (_highlightCodeBlock) {
            createColorBackgroundSpan(MarkdownHighlighterPattern.CODE.pattern, MD_COLOR_CODEBLOCK);
        }
    }
}