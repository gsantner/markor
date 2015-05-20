package me.writeily.editor;

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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Highlighter {
    final HighlighterColors colors;
    final String fontType;
    final Integer fontSize;

    public Highlighter(final HighlighterColors colors, String fontType, String fontSize) {
        this.colors = colors;
        this.fontType = fontType;
        this.fontSize = Integer.valueOf(fontSize);
    }

    public Editable run(Editable e) {
        try {
            clearSpans(e);

            if (e.length() == 0) {
                return e;
            }

            createHeaderSpanForMatches(e, HighlighterPattern.HEADER.getPattern(), colors.getHeaderColor());
            createColorSpanForMatches(e,  HighlighterPattern.LINK.getPattern(), colors.getLinkColor());
            createColorSpanForMatches(e,  HighlighterPattern.LIST.getPattern(), colors.getListColor());
            createStyleSpanForMatches(e,  HighlighterPattern.BOLD.getPattern(), Typeface.BOLD);
            createStyleSpanForMatches(e,  HighlighterPattern.ITALICS.getPattern(), Typeface.ITALIC);
            createColorSpanForMatches(e,  HighlighterPattern.QUOTATION.getPattern(), colors.getListColor());
            createSpanWithStrikeThroughForMatches(e, HighlighterPattern.STRIKETHROUGH.getPattern());
            createMonospaceSpanForMatches(e, HighlighterPattern.MONOSPACED.getPattern());

        } catch (Exception ex) {
            // Ignoring errors
        }

        return e;
    }

    private void createHeaderSpanForMatches(Editable e, Pattern pattern, int headerColor) {

        createSpanForMatches(e, pattern, new HeaderSpanCreator(this, e, headerColor));
    }

    private void createMonospaceSpanForMatches(Editable e, Pattern pattern) {

        createSpanForMatches(e, pattern, new SpanCreator() {
            @Override
            public ParcelableSpan create(Matcher m) {
                return new TypefaceSpan("monospace");
            }
        });

    }

    private void createSpanWithStrikeThroughForMatches(Editable e, Pattern pattern) {

        createSpanForMatches(e, pattern, new SpanCreator() {
            @Override
            public ParcelableSpan create(Matcher m) {
                return new StrikethroughSpan();
            }
        });
    }

    private void createStyleSpanForMatches(final Editable e, final Pattern pattern,
                                           final int style) {
        createSpanForMatches(e, pattern, new SpanCreator() {
            @Override
            public ParcelableSpan create(Matcher m) {
                return new StyleSpan(style);
            }
        });
    }

    private void createColorSpanForMatches(final Editable e, final Pattern pattern,
                                           final int color) {
        createSpanForMatches(e, pattern, new SpanCreator() {
            @Override
            public ParcelableSpan create(Matcher m) {
                return new ForegroundColorSpan(color);
            }
        });
    }


    private void createSpanForMatches(final Editable e, final Pattern pattern,
                                      final SpanCreator creator) {
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
