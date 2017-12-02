/*
 * Copyright (c) 2017 Gregor Santner and Markor contributors
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

    public MarkdownHighlighter() {
        AppSettings as = AppSettings.get();
        _colors = new MarkdownHighlighterColorsNeutral();
        _fontType = as.getFontFamily();
        _fontSize = as.getFontSize();
        _highlightHexcolorEnabled = as.isHighlightingHexColorEnabled();
    }

    @Override
    protected Editable run(final HighlightingEditor editor, final Editable editable) {
        try {
            clearSpans(editable);

            if (editable.length() == 0) {
                return editable;
            }

            createHeaderSpanForMatches(editable, MarkdownHighlighterPattern.HEADER, _colors.getHeaderColor());
            createColorSpanForMatches(editable, MarkdownHighlighterPattern.LINK.getPattern(), _colors.getLinkColor());
            createColorSpanForMatches(editable, MarkdownHighlighterPattern.LIST.getPattern(), _colors.getListColor());
            createColorSpanForMatches(editable, MarkdownHighlighterPattern.ORDEREDLIST.getPattern(), _colors.getListColor());
            createColorBackgroundSpan(editable, MarkdownHighlighterPattern.DOUBLESPACE_ENDING.getPattern(), _colors.getDoublespaceColor());
            createStyleSpanForMatches(editable, MarkdownHighlighterPattern.BOLD.getPattern(), Typeface.BOLD);
            createStyleSpanForMatches(editable, MarkdownHighlighterPattern.ITALICS.getPattern(), Typeface.ITALIC);
            createColorSpanForMatches(editable, MarkdownHighlighterPattern.QUOTATION.getPattern(), _colors.getQuotationColor());
            createSpanWithStrikeThroughForMatches(editable, MarkdownHighlighterPattern.STRIKETHROUGH.getPattern());
            createMonospaceSpanForMatches(editable, MarkdownHighlighterPattern.MONOSPACED.getPattern());
            createColorBackgroundSpan(editable, MarkdownHighlighterPattern.MONOSPACED.getPattern(), _colors.getDoublespaceColor());

            if (_highlightHexcolorEnabled) {
                createColoredUnderlineSpanForMatches(editable, HexColorCodeUnderlineSpan.PATTERN, new HexColorCodeUnderlineSpan(), 1);
            }

        } catch (Exception ex) {
            // Ignoring errors
        }

        return editable;
    }

    private void createHeaderSpanForMatches(Editable editable, MarkdownHighlighterPattern pattern, int headerColor) {
        createSpanForMatches(editable, pattern.getPattern(), new MarkdownHeaderSpanCreator(this, editable, headerColor));
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

