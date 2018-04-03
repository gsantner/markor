/*
 * Copyright (c) 2017-2018 Gregor Santner
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.format.highlighter.markdown;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputFilter;

import net.gsantner.markor.format.highlighter.Highlighter;
import net.gsantner.markor.format.highlighter.HighlightingEditor;
import net.gsantner.markor.format.highlighter.general.HexColorCodeUnderlineSpan;
import net.gsantner.markor.util.AppSettings;

public class MarkdownHighlighter extends Highlighter {
    private final MarkdownHighlighterColors _colors;
    final String _fontType;
    final Integer _fontSize;
    private final boolean _highlightHexcolorEnabled;
    private final boolean _highlightLineEnding;
    private final boolean _highlightCodeChangeFont;

    public MarkdownHighlighter() {
        AppSettings as = AppSettings.get();
        _colors = new MarkdownHighlighterColors();
        _fontType = as.getFontFamily();
        _fontSize = as.getFontSize();
        _highlightHexcolorEnabled = as.isHighlightingHexColorEnabled();
        _highlightLineEnding = as.isMarkdownHighlightLineEnding();
        _highlightCodeChangeFont = as.isMarkdownHighlightCodeFontMonospaceAllowed();
    }

    @Override
    protected Editable run(final HighlightingEditor editor, final Editable editable) {
        try {
            clearSpans(editable);

            if (editable.length() == 0) {
                return editable;
            }

            _profiler.start(true, "Markdown Highlighting");

            _profiler.restart("Header");
            createHeaderSpanForMatches(editable, MarkdownHighlighterPattern.HEADER, _colors.getHeaderColor());
            _profiler.restart("Link");
            createColorSpanForMatches(editable, MarkdownHighlighterPattern.LINK.pattern, _colors.getLinkColor());
            _profiler.restart("List");
            createColorSpanForMatches(editable, MarkdownHighlighterPattern.LIST_UNORDERED.pattern, _colors.getListColor());
            _profiler.restart("OrderedList");
            createColorSpanForMatches(editable, MarkdownHighlighterPattern.LIST_ORDERED.pattern, _colors.getListColor());
            if (_highlightLineEnding) {
                _profiler.restart("Double space ending - bgcolor");
                createColorBackgroundSpan(editable, MarkdownHighlighterPattern.DOUBLESPACE_LINE_ENDING.pattern, _colors.getDoublespaceColor());
            }
            _profiler.restart("Bold");
            createStyleSpanForMatches(editable, MarkdownHighlighterPattern.BOLD.pattern, Typeface.BOLD);
            _profiler.restart("Italics");
            createStyleSpanForMatches(editable, MarkdownHighlighterPattern.ITALICS.pattern, Typeface.ITALIC);
            _profiler.restart("Quotation");
            createColorSpanForMatches(editable, MarkdownHighlighterPattern.QUOTATION.pattern, _colors.getQuotationColor());
            _profiler.restart("Strikethrough");
            createSpanWithStrikeThroughForMatches(editable, MarkdownHighlighterPattern.STRIKETHROUGH.pattern);
            if (_highlightCodeChangeFont) {
                _profiler.restart("Code - Font [MonoSpace]");
                createMonospaceSpanForMatches(editable, MarkdownHighlighterPattern.CODE.pattern);
            }
            _profiler.restart("Code - bgolor");
            createColorBackgroundSpan(editable, MarkdownHighlighterPattern.CODE.pattern, _colors.getDoublespaceColor());
            if (_highlightHexcolorEnabled) {
                _profiler.restart("RGB Color underline");
                createColoredUnderlineSpanForMatches(editable, HexColorCodeUnderlineSpan.PATTERN, new HexColorCodeUnderlineSpan(), 1);
            }

            _profiler.end();
            _profiler.printProfilingGroup();
        } catch (Exception ex) {
            // Ignoring errors
        }

        return editable;
    }

    private void createHeaderSpanForMatches(Editable editable, MarkdownHighlighterPattern pattern, int headerColor) {
        createSpanForMatches(editable, pattern.pattern, new MarkdownHeaderSpanCreator(this, editable, headerColor));
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

