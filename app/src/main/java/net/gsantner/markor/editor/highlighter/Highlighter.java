/*
 * Copyright (c) 2014 Jeff Martin
 * Copyright (c) 2015 Pedro Lafuente
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.editor.highlighter;

import android.graphics.Typeface;
import android.text.Editable;
import android.text.ParcelableSpan;
import android.text.Spannable;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;
import android.text.style.TypefaceSpan;

import net.gsantner.markor.editor.highlighter.markdown.HighlighterColors;
import net.gsantner.markor.editor.highlighter.markdown.HighlighterPattern;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO: Seperate markdown from this out, make use of derivation/abstract
public class Highlighter {
    private final HighlighterColors colors;
    final String fontType;
    final Integer fontSize;

    Highlighter(final HighlighterColors colors, String fontType, int fontSize) {
        this.colors = colors;
        this.fontType = fontType;
        this.fontSize = fontSize;
    }

    Editable run(Editable e) {
        try {
            clearSpans(e);

            if (e.length() == 0) {
                return e;
            }

            createHeaderSpanForMatches(e, HighlighterPattern.HEADER, colors.getHeaderColor());
            createColorSpanForMatches(e, HighlighterPattern.LINK, colors.getLinkColor());
            createColorSpanForMatches(e, HighlighterPattern.LIST, colors.getListColor());
            createStyleSpanForMatches(e, HighlighterPattern.BOLD, Typeface.BOLD);
            createStyleSpanForMatches(e, HighlighterPattern.ITALICS, Typeface.ITALIC);
            createColorSpanForMatches(e, HighlighterPattern.QUOTATION, colors.getQuotationColor());
            createSpanWithStrikeThroughForMatches(e, HighlighterPattern.STRIKETHROUGH);
            createMonospaceSpanForMatches(e, HighlighterPattern.MONOSPACED);

        } catch (Exception ex) {
            // Ignoring errors
        }

        return e;
    }

    private void createHeaderSpanForMatches(Editable e, HighlighterPattern pattern, int headerColor) {

        createSpanForMatches(e, pattern, new HeaderSpanCreator(this, e, headerColor));
    }

    private void createMonospaceSpanForMatches(Editable e, HighlighterPattern pattern) {
        createSpanForMatches(e, pattern, new SpanCreator() {
            @Override
            public ParcelableSpan create(Matcher matcher) {
                return new TypefaceSpan("monospace");
            }
        });
    }

    private void createSpanWithStrikeThroughForMatches(Editable e, HighlighterPattern pattern) {
        createSpanForMatches(e, pattern, new SpanCreator() {
            @Override
            public ParcelableSpan create(Matcher matcher) {
                return new StrikethroughSpan();
            }
        });
    }

    private void createStyleSpanForMatches(final Editable e, final HighlighterPattern pattern, final int style) {
        createSpanForMatches(e, pattern, new SpanCreator() {
            @Override
            public ParcelableSpan create(Matcher matcher) {
                return new StyleSpan(style);
            }
        });
    }

    private void createColorSpanForMatches(final Editable e, final HighlighterPattern pattern, final int color) {
        createSpanForMatches(e, pattern, new SpanCreator() {
            @Override
            public ParcelableSpan create(Matcher matcher) {
                return new ForegroundColorSpan(color);
            }
        });
    }

    private void createSpanForMatches(final Editable e, final HighlighterPattern pattern, final SpanCreator creator) {
        createSpanForMatches(e, pattern.getPattern(), creator);
    }

    private void createSpanForMatches(final Editable e, final Pattern pattern, final SpanCreator creator) {
        for (Matcher m = pattern.matcher(e); m.find(); ) {
            e.setSpan(creator.create(m), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void clearSpans(Editable e) {
        clearSpanType(e, TextAppearanceSpan.class);
        clearSpanType(e, ForegroundColorSpan.class);
        clearSpanType(e, BackgroundColorSpan.class);
        clearSpanType(e, StrikethroughSpan.class);
        clearSpanType(e, StyleSpan.class);
    }

    private <T extends CharacterStyle> void clearSpanType(Editable e, Class<T> spanType) {
        CharacterStyle[] spans = e.getSpans(0, e.length(), spanType);

        for (int n = spans.length; n-- > 0; ) {
            e.removeSpan(spans[n]);
        }
    }

}
