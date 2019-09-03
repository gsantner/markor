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
import android.text.Editable;
import android.text.InputFilter;

import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.hleditor.Highlighter;
import net.gsantner.markor.ui.hleditor.HighlightingEditor;
import net.gsantner.markor.util.AppSettings;

import other.writeily.format.markdown.WrMarkdownHeaderSpanCreator;

public class MarkdownHighlighter extends Highlighter {
    public final String _fontType;
    public final Integer _fontSize;
    private final boolean _highlightLineEnding;
    private final boolean _highlightCodeChangeFont;
    private final boolean _highlightBiggerHeadings;

    private static final int MD_COLOR_HEADING = 0xffef6D00;
    private static final int MD_COLOR_LINK = 0xff1ea3fe;
    private static final int MD_COLOR_LIST = 0xffdaa521;
    private static final int MD_COLOR_QUOTE = 0xff88b04c;
    private static final int MD_COLOR_CODEBLOCK = 0xff8c8c8c;

    public MarkdownHighlighter(HighlightingEditor hlEditor, Document document) {
        super(hlEditor, document);
        _highlightLinks = false;
        _fontType = _appSettings.getFontFamily();
        _fontSize = _appSettings.getFontSize();
        _highlightLineEnding = _appSettings.isMarkdownHighlightLineEnding();
        _highlightCodeChangeFont = _appSettings.isMarkdownHighlightCodeFontMonospaceAllowed();
        _highlightBiggerHeadings = _appSettings.isMarkdownBiggerHeadings();
    }

    @Override
    protected Editable run(final Editable editable) {
        try {
            clearSpans(editable);

            if (editable.length() == 0) {
                return editable;
            }

            _profiler.start(true, "Markdown Highlighting");
            generalHighlightRun(editable);

            _profiler.restart("Heading");
            if (_highlightBiggerHeadings) {
                createHeaderSpanForMatches(editable, MarkdownHighlighterPattern.HEADING, MD_COLOR_HEADING);
            } else {
                createColorSpanForMatches(editable, MarkdownHighlighterPattern.HEADING_SIMPLE.pattern, MD_COLOR_HEADING);
            }
            _profiler.restart("Link");
            createColorSpanForMatches(editable, MarkdownHighlighterPattern.LINK.pattern, MD_COLOR_LINK);
            _profiler.restart("List");
            createColorSpanForMatches(editable, MarkdownHighlighterPattern.LIST_UNORDERED.pattern, MD_COLOR_LIST);
            _profiler.restart("OrderedList");
            createColorSpanForMatches(editable, MarkdownHighlighterPattern.LIST_ORDERED.pattern, MD_COLOR_LIST);
            if (_highlightLineEnding) {
                _profiler.restart("Double space ending - bgcolor");
                createColorBackgroundSpan(editable, MarkdownHighlighterPattern.DOUBLESPACE_LINE_ENDING.pattern, MD_COLOR_CODEBLOCK);
            }
            _profiler.restart("Bold");
            createStyleSpanForMatches(editable, MarkdownHighlighterPattern.BOLD.pattern, Typeface.BOLD);
            _profiler.restart("Italics");
            createStyleSpanForMatches(editable, MarkdownHighlighterPattern.ITALICS.pattern, Typeface.ITALIC);
            _profiler.restart("Quotation");
            createColorSpanForMatches(editable, MarkdownHighlighterPattern.QUOTATION.pattern, MD_COLOR_QUOTE);
            _profiler.restart("Strikethrough");
            createSpanWithStrikeThroughForMatches(editable, MarkdownHighlighterPattern.STRIKETHROUGH.pattern);
            if (_highlightCodeChangeFont) {
                _profiler.restart("Code - Font [MonoSpace]");
                createMonospaceSpanForMatches(editable, MarkdownHighlighterPattern.CODE.pattern);
            }
            _profiler.restart("Code - bgcolor");
            createColorBackgroundSpan(editable, MarkdownHighlighterPattern.CODE.pattern, MD_COLOR_CODEBLOCK);

            _profiler.end();
            _profiler.printProfilingGroup();
        } catch (Exception ex) {
            // Ignoring errors
        }

        return editable;
    }

    private void createHeaderSpanForMatches(Editable editable, MarkdownHighlighterPattern pattern, int headerColor) {
        createSpanForMatches(editable, pattern.pattern, new WrMarkdownHeaderSpanCreator(this, editable, headerColor, _highlightBiggerHeadings));
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

