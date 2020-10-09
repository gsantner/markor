/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.zimwiki;

import android.content.Context;
import android.graphics.Typeface;
import android.text.InputFilter;
import android.text.Spannable;

import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.hleditor.HighlightingEditor;
import net.gsantner.markor.util.AppSettings;

import other.writeily.format.markdown.WrMarkdownHeaderSpanCreator;

public class ZimWikiHighlighter extends net.gsantner.markor.ui.hleditor.Highlighter {
    private final boolean _highlightLineEnding;
    private final boolean _highlightCodeChangeFont;
    private final boolean _highlightBiggerHeadings;
    private final boolean _highlightDisableCodeBlock;

    private static final int MD_COLOR_HEADING = 0xffef6D00;
    private static final int MD_COLOR_LINK = 0xff1ea3fe;
    private static final int MD_COLOR_LIST = 0xffdaa521;
    private static final int MD_COLOR_QUOTE = 0xff88b04c;
    private static final int MD_COLOR_CODEBLOCK = 0xff8c8c8c;

    public ZimWikiHighlighter(HighlightingEditor hlEditor, Document document) {
        super(hlEditor, document);
        _highlightLinks = false;
        _highlightLineEnding = _appSettings.isMarkdownHighlightLineEnding();
        _highlightCodeChangeFont = _appSettings.isMarkdownHighlightCodeFontMonospaceAllowed();
        _highlightBiggerHeadings = _appSettings.isMarkdownBiggerHeadings();
        _highlightDisableCodeBlock = _appSettings.isMarkdownDisableCodeBlockHighlight();
        setTextModifier(new ListHandler(_appSettings.isMarkdownAutoUpdateList()));
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
            createHeaderSpanForMatches(spannable, ZimWikiHighlighterPattern.HEADING, MD_COLOR_HEADING);
            _profiler.restart("Link");
            createColorSpanForMatches(spannable, ZimWikiHighlighterPattern.LINK.pattern, MD_COLOR_LINK);
            _profiler.restart("List");
            createColorSpanForMatches(spannable, ZimWikiHighlighterPattern.LIST_UNORDERED.pattern, MD_COLOR_LIST);
            _profiler.restart("OrderedList");
            createColorSpanForMatches(spannable, ZimWikiHighlighterPattern.LIST_ORDERED.pattern, MD_COLOR_LIST);
            if (_highlightLineEnding) {
                _profiler.restart("Double space ending - bgcolor");
                createColorBackgroundSpan(spannable, ZimWikiHighlighterPattern.DOUBLESPACE_LINE_ENDING.pattern, MD_COLOR_CODEBLOCK);
            }
            _profiler.restart("Bold");
            createStyleSpanForMatches(spannable, ZimWikiHighlighterPattern.BOLD.pattern, Typeface.BOLD);
            _profiler.restart("Italics");
            createStyleSpanForMatches(spannable, ZimWikiHighlighterPattern.ITALICS.pattern, Typeface.ITALIC);
            _profiler.restart("Quotation");
            createColorSpanForMatches(spannable, ZimWikiHighlighterPattern.QUOTATION.pattern, MD_COLOR_QUOTE);
            _profiler.restart("Strikethrough");
            createSpanWithStrikeThroughForMatches(spannable, ZimWikiHighlighterPattern.STRIKETHROUGH.pattern);
            if (_highlightCodeChangeFont) {
                _profiler.restart("Code - Font [MonoSpace]");
                createMonospaceSpanForMatches(spannable, ZimWikiHighlighterPattern.CODE.pattern);
            }
            _profiler.restart("Code - bgcolor");
            if (!_highlightDisableCodeBlock) {
                createColorBackgroundSpan(spannable, ZimWikiHighlighterPattern.CODE.pattern, MD_COLOR_CODEBLOCK);
            }

            _profiler.end();
            _profiler.printProfilingGroup();
        } catch (Exception ex) {
            // Ignoring errors
        }

        return spannable;
    }

    private void createHeaderSpanForMatches(Spannable spannable, ZimWikiHighlighterPattern pattern, int headerColor) {
        createSpanForMatches(spannable, pattern.pattern, new WrMarkdownHeaderSpanCreator(this, spannable, headerColor, _highlightBiggerHeadings));
    }

    @Override
    public InputFilter getAutoFormatter() {
        return new ZimWikiAutoFormat();
    }

    @Override
    public int getHighlightingDelay(Context context) {
        return new AppSettings(context).getMarkdownHighlightingDelay();
    }
}

