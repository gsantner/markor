/*
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.format.highlighter.markdown;

import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputFilter;
import android.text.ParcelableSpan;
import android.text.style.BackgroundColorSpan;

import net.gsantner.markor.format.highlighter.Highlighter;
import net.gsantner.markor.format.highlighter.HighlightingEditor;
import net.gsantner.markor.format.highlighter.SpanCreator;
import net.gsantner.markor.util.AppSettings;

import java.util.regex.Matcher;

public class MarkdownHighlighter extends Highlighter {
    private final MarkdownHighlighterColors colors;
    public final String _fontType;
    public final Integer _fontSize;

    public MarkdownHighlighter() {
        colors = new MarkdownHighlighterColorsNeutral();
        _fontType = AppSettings.get().getFontFamily();
        _fontSize = AppSettings.get().getFontSize();
    }

    @Override
    protected Editable run(final HighlightingEditor editor, final Editable editable) {
        try {
            clearSpans(editable);

            if (editable.length() == 0) {
                return editable;
            }

            createHeaderSpanForMatches(editable, MarkdownHighlighterPattern.HEADER, colors.getHeaderColor());
            createColorSpanForMatches(editable, MarkdownHighlighterPattern.LINK.getPattern(), colors.getLinkColor());
            createColorSpanForMatches(editable, MarkdownHighlighterPattern.LIST.getPattern(), colors.getListColor());
            createColorSpanForMatches(editable, MarkdownHighlighterPattern.ORDEREDLIST.getPattern(), colors.getListColor());
            createColorSpanForDoublespace(editable, MarkdownHighlighterPattern.DOUBLESPACE, colors.getDoublespaceColor());
            createStyleSpanForMatches(editable, MarkdownHighlighterPattern.BOLD.getPattern(), Typeface.BOLD);
            createStyleSpanForMatches(editable, MarkdownHighlighterPattern.ITALICS.getPattern(), Typeface.ITALIC);
            createColorSpanForMatches(editable, MarkdownHighlighterPattern.QUOTATION.getPattern(), colors.getQuotationColor());
            createSpanWithStrikeThroughForMatches(editable, MarkdownHighlighterPattern.STRIKETHROUGH.getPattern());
            createMonospaceSpanForMatches(editable, MarkdownHighlighterPattern.MONOSPACED.getPattern());
            createColorSpanForDoublespace(editable, MarkdownHighlighterPattern.MONOSPACED, colors.getDoublespaceColor());

        } catch (Exception ex) {
            // Ignoring errors
        }

        return editable;
    }

    @Override
    public InputFilter getAutoFormatter() {
        return new MarkdownAutoFormat();
    }

    private void createHeaderSpanForMatches(Editable e, MarkdownHighlighterPattern pattern, int headerColor) {

        createSpanForMatches(e, pattern, new MarkdownHeaderSpanCreator(this, e, headerColor));
    }

    private void createColorSpanForDoublespace(Editable e, MarkdownHighlighterPattern pattern, final int color) {
        createSpanForMatches(e, pattern, new SpanCreator() {
            @Override
            public ParcelableSpan create(Matcher matcher, int iM) {
                return new BackgroundColorSpan(color);
            }
        });
    }

    private void createSpanForMatches(final Editable e, final MarkdownHighlighterPattern pattern, final SpanCreator creator) {
        createSpanForMatches(e, pattern.getPattern(), creator);
    }
}

