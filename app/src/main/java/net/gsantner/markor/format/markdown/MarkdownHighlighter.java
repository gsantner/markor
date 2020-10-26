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

import android.content.Context;
import android.graphics.Typeface;
import android.text.InputFilter;
import android.text.Spannable;

import net.gsantner.markor.format.ListHandler;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.hleditor.Highlighter;
import net.gsantner.markor.ui.hleditor.HighlightingEditor;
import net.gsantner.markor.util.AppSettings;

import other.writeily.format.markdown.WrMarkdownHeaderSpanCreator;

public class MarkdownHighlighter extends Highlighter {
    private final boolean _highlightLineEnding;
    private final boolean _highlightCodeChangeFont;
    private final boolean _highlightBiggerHeadings;
    private final boolean _highlightDisableCodeBlock;

    private static final int MD_COLOR_HEADING = 0xffef6D00;
    private static final int MD_COLOR_LINK = 0xff1ea3fe;
    private static final int MD_COLOR_LIST = 0xffdaa521;
    private static final int MD_COLOR_QUOTE = 0xff88b04c;
    private static final int MD_COLOR_CODEBLOCK = 0xff8c8c8c;

    public MarkdownHighlighter(HighlightingEditor hlEditor, Document document) {
        super(hlEditor, document);
        _highlightLinks = false;
        _highlightLineEnding = _appSettings.isMarkdownHighlightLineEnding();
        _highlightCodeChangeFont = _appSettings.isMarkdownHighlightCodeFontMonospaceAllowed();
        _highlightBiggerHeadings = _appSettings.isMarkdownBiggerHeadings();
        _highlightDisableCodeBlock = _appSettings.isMarkdownDisableCodeBlockHighlight();
        setTextModifier(new ListHandler(_appSettings.isMarkdownAutoUpdateList(), MarkdownAutoFormat.getPrefixPatterns()));
    }

    @Override
    protected Spannable run(final Spannable spannable) {
        try {
            clearSpans(spannable);

            if (spannable.length() == 0) {
                return spannable;
            }

            _profiler.start(true, "Markdown Highlighting");
            generalHighlightRun(spannable);

            _profiler.restart("Heading");
            if (_highlightBiggerHeadings) {
                createHeaderSpanForMatches(spannable, MarkdownHighlighterPattern.HEADING, MD_COLOR_HEADING);
            } else {
                createColorSpanForMatches(spannable, MarkdownHighlighterPattern.HEADING_SIMPLE.pattern, MD_COLOR_HEADING);
            }
            _profiler.restart("Link");
            createColorSpanForMatches(spannable, MarkdownHighlighterPattern.LINK.pattern, MD_COLOR_LINK);
            _profiler.restart("List");
            createColorSpanForMatches(spannable, MarkdownHighlighterPattern.LIST_UNORDERED.pattern, MD_COLOR_LIST);
            _profiler.restart("OrderedList");
            createColorSpanForMatches(spannable, MarkdownHighlighterPattern.LIST_ORDERED.pattern, MD_COLOR_LIST);
            if (_highlightLineEnding) {
                _profiler.restart("Double space ending - bgcolor");
                createColorBackgroundSpan(spannable, MarkdownHighlighterPattern.DOUBLESPACE_LINE_ENDING.pattern, MD_COLOR_CODEBLOCK);
            }
            _profiler.restart("Bold");
            createStyleSpanForMatches(spannable, MarkdownHighlighterPattern.BOLD.pattern, Typeface.BOLD);
            _profiler.restart("Italics");
            createStyleSpanForMatches(spannable, MarkdownHighlighterPattern.ITALICS.pattern, Typeface.ITALIC);
            _profiler.restart("Quotation");
            createColorSpanForMatches(spannable, MarkdownHighlighterPattern.QUOTATION.pattern, MD_COLOR_QUOTE);
            _profiler.restart("Strikethrough");
            createSpanWithStrikeThroughForMatches(spannable, MarkdownHighlighterPattern.STRIKETHROUGH.pattern);
            if (_highlightCodeChangeFont) {
                _profiler.restart("Code - Font [MonoSpace]");
                createMonospaceSpanForMatches(spannable, MarkdownHighlighterPattern.CODE.pattern);
            }
            _profiler.restart("Code - bgcolor");
            if (!_highlightDisableCodeBlock) {
                createColorBackgroundSpan(spannable, MarkdownHighlighterPattern.CODE.pattern, MD_COLOR_CODEBLOCK);
            }

            _profiler.end();
            _profiler.printProfilingGroup();
        } catch (Exception ex) {
            // Ignoring errors
        }

        return spannable;
    }

    private void createHeaderSpanForMatches(Spannable spannable, MarkdownHighlighterPattern pattern, int headerColor) {
        createSpanForMatches(spannable, pattern.pattern, new WrMarkdownHeaderSpanCreator(this, spannable, headerColor, _highlightBiggerHeadings));
    }

    @Override
    public InputFilter getAutoFormatter() {
        return new MarkdownAutoFormat();
    }

    @Override
    public int getHighlightingDelay(Context context) {
        return new AppSettings(context).getMarkdownHighlightingDelay();
    }
}

