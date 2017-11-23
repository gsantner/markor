package net.gsantner.markor.format.highlighter.markdown;

import net.gsantner.markor.format.highlighter.Highlighter;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputFilter;
import android.text.ParcelableSpan;
import android.text.Spannable;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;
import android.text.style.TypefaceSpan;

import net.gsantner.markor.format.highlighter.SpanCreator;
import net.gsantner.markor.util.AppSettings;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownHighlighter extends Highlighter{
    private final MarkdownHighlighterColors colors;
    public final String fontType;
    public final Integer fontSize;

    public MarkdownHighlighter()
    {
        colors = new MarkdownHighlighterColorsNeutral();
        fontType = AppSettings.get().getFontFamily();
        fontSize = AppSettings.get().getFontSize();
    }

    @Override
    protected Editable run(Editable e) {
        try {
            clearSpans(e);

            if (e.length() == 0) {
                return e;
            }

            createHeaderSpanForMatches(e, MarkdownHighlighterPattern.HEADER, colors.getHeaderColor());
            createColorSpanForMatches(e, MarkdownHighlighterPattern.LINK, colors.getLinkColor());
            createColorSpanForMatches(e, MarkdownHighlighterPattern.LIST, colors.getListColor());
            createColorSpanForMatches(e, MarkdownHighlighterPattern.ORDEREDLIST, colors.getListColor());
            createColorSpanForDoublespace(e, MarkdownHighlighterPattern.DOUBLESPACE, colors.getDoublespaceColor());
            createStyleSpanForMatches(e, MarkdownHighlighterPattern.BOLD, Typeface.BOLD);
            createStyleSpanForMatches(e, MarkdownHighlighterPattern.ITALICS, Typeface.ITALIC);
            createColorSpanForMatches(e, MarkdownHighlighterPattern.QUOTATION, colors.getQuotationColor());
            createSpanWithStrikeThroughForMatches(e, MarkdownHighlighterPattern.STRIKETHROUGH);
            createMonospaceSpanForMatches(e, MarkdownHighlighterPattern.MONOSPACED);
            createColorSpanForDoublespace(e, MarkdownHighlighterPattern.MONOSPACED,colors.getDoublespaceColor());

        } catch (Exception ex) {
            // Ignoring errors
        }

        return e;
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
            public ParcelableSpan create(Matcher matcher) {
                return new BackgroundColorSpan(color);
            }
        });
    }

    private void createMonospaceSpanForMatches(Editable e, MarkdownHighlighterPattern pattern) {
        createSpanForMatches(e, pattern, new SpanCreator() {
            @Override
            public ParcelableSpan create(Matcher matcher) {
                return new TypefaceSpan("monospace");
            }
        });
    }

    private void createSpanWithStrikeThroughForMatches(Editable e, MarkdownHighlighterPattern pattern) {
        createSpanForMatches(e, pattern, new SpanCreator() {
            @Override
            public ParcelableSpan create(Matcher matcher) {
                return new StrikethroughSpan();
            }
        });
    }

    private void createStyleSpanForMatches(final Editable e, final MarkdownHighlighterPattern pattern, final int style) {
        createSpanForMatches(e, pattern, new SpanCreator() {
            @Override
            public ParcelableSpan create(Matcher matcher) {
                return new StyleSpan(style);
            }
        });
    }

    private void createColorSpanForMatches(final Editable e, final MarkdownHighlighterPattern pattern, final int color) {
        createSpanForMatches(e, pattern, new SpanCreator() {
            @Override
            public ParcelableSpan create(Matcher matcher) {

                return new ForegroundColorSpan(color);
            }
        });
    }

    private void createSpanForMatches(final Editable e, final MarkdownHighlighterPattern pattern, final SpanCreator creator) {
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

