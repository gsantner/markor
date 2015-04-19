package me.writeily.pro.editor;

import android.graphics.Typeface;
import android.text.Editable;
import android.text.ParcelableSpan;
import android.text.Spannable;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Highlighter {

    final HighlighterColors colors;

    public Highlighter(final HighlighterColors colors) {
        this.colors = colors;
    }

    enum HighlighterPattern {
        LIST(Pattern.compile("\\n\\s+(\\*|\\d*\\.)\\s")),
        HEADER(Pattern.compile("(((\\n|^)#+.*?\\n)|((\\n|^).*?\\n(-|=)+))")),
        LINK(Pattern.compile("\\[[^\\]]*\\]\\([^\\)]*\\)")),
        STRIKETHROUGH(Pattern.compile("~~.+~~")),
        MONOSPACED(Pattern.compile("`.+`")),
        BOLD(Pattern.compile("\\*{2}.+?\\*{2}")),
        ITALICS(Pattern.compile("[^\\*]\\*[^\\*\\n]+\\*[^\\*]"));

        private Pattern pattern;

        HighlighterPattern(Pattern pattern) {
            this.pattern = pattern;
        }
    }

    public Editable run(Editable e) {
        try {
            clearSpans(e);

            if (e.length() == 0)
                return e;

            createColorSpanForMatches(e, HighlighterPattern.HEADER.pattern, colors.getHeaderColor());
            createColorSpanForMatches(e, HighlighterPattern.LINK.pattern, colors.getLinkColor());
            createColorSpanForMatches(e, HighlighterPattern.LIST.pattern, colors.getListColor());
            createStyleSpanForMatches(e, HighlighterPattern.BOLD.pattern, Typeface.BOLD);
            createStyleSpanForMatches(e, HighlighterPattern.ITALICS.pattern, Typeface.ITALIC);
            createSpanWithStrikeThroughForMatches(e, HighlighterPattern.STRIKETHROUGH.pattern);
            createMonospaceSpanForMatches(e, HighlighterPattern.MONOSPACED.pattern);

        } catch (Exception ex) {
            // Ignoring errors
        }

        return e;
    }

    private void createMonospaceSpanForMatches(Editable e, Pattern pattern) {
        createSpanForMatches(e, pattern, new SpanCreator() {
            @Override
            public ParcelableSpan create() {
                return new TypefaceSpan("monospace");
            }
        });

    }

    private void createSpanWithStrikeThroughForMatches(Editable e, Pattern pattern) {

        createSpanForMatches(e, pattern, new SpanCreator() {
            @Override
            public ParcelableSpan create() {
                return new StrikethroughSpan();
            }
        });
    }

    private void createStyleSpanForMatches(final Editable e, final Pattern pattern,
                                           final int style) {
        createSpanForMatches(e, pattern, new SpanCreator() {
            @Override
            public ParcelableSpan create() {
                return new StyleSpan(style);
            }
        });
    }

    private void createColorSpanForMatches(final Editable e, final Pattern pattern,
                                           final int color) {
        createSpanForMatches(e, pattern, new SpanCreator() {
            @Override
            public ParcelableSpan create() {
                return new ForegroundColorSpan(color);
            }
        });
    }


    private void createSpanForMatches(final Editable e, final Pattern pattern,
                                      final SpanCreator spanCreator) {
        for (Matcher m = pattern.matcher(e);
             m.find(); ) {
            e.setSpan(
                    spanCreator.create(),
                    m.start(),
                    m.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

    }

    private void clearSpans(Editable e) {

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
