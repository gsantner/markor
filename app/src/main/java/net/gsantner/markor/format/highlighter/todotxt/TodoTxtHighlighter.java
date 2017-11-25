/*
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.format.highlighter.todotxt;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputFilter;
import android.text.ParcelableSpan;
import android.text.Spannable;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.LineBackgroundSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;

import net.gsantner.markor.format.highlighter.Highlighter;
import net.gsantner.markor.format.highlighter.SpanCreator;
import net.gsantner.markor.util.AppSettings;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TodoTxtHighlighter extends Highlighter {
    private final TodoTxtHighlighterColors colors;
    public final String fontType;
    public final Integer fontSize;

    public TodoTxtHighlighter() {
        colors = new TodoTxtHighlighterColorsNeutral();
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

            createColorSpanForMatches(e, TodoTxtHighlighterPattern.CONTEXT, colors.getContextColor());
            createColorSpanForMatches(e, TodoTxtHighlighterPattern.LINK, colors.getLinkColor());
            createColorSpanForMatches(e, TodoTxtHighlighterPattern.CATEGORY, colors.getCategoryColor());

            createStyleSpanForMatches(e, TodoTxtHighlighterPattern.KEYVALUE, Typeface.ITALIC);

            // Priorities
            createColorSpanForMatches(e, TodoTxtHighlighterPattern.PRIORITY_A, colors.getPriorityColor(1));
            createColorSpanForMatches(e, TodoTxtHighlighterPattern.PRIORITY_B, colors.getPriorityColor(2));
            createColorSpanForMatches(e, TodoTxtHighlighterPattern.PRIORITY_C, colors.getPriorityColor(3));
            createColorSpanForMatches(e, TodoTxtHighlighterPattern.PRIORITY_D, colors.getPriorityColor(4));
            createColorSpanForMatches(e, TodoTxtHighlighterPattern.PRIORITY_E, colors.getPriorityColor(5));
            createColorSpanForMatches(e, TodoTxtHighlighterPattern.PRIORITY_F, colors.getPriorityColor(6));
            createStyleSpanForMatches(e, TodoTxtHighlighterPattern.PRIORITY_ANY, Typeface.BOLD);

            // Date
            createColorSpanForMatches(e, TodoTxtHighlighterPattern.DATE, colors.getDateColor());
            createStyleSpanForMatches(e, TodoTxtHighlighterPattern.DATE, Typeface.ITALIC);

            // Line highlighting

            // Do this at the end
            createColorSpanForMatches(e, TodoTxtHighlighterPattern.DONE, colors.getDoneColor());
            createSpanWithStrikeThroughForMatches(e, TodoTxtHighlighterPattern.DONE);

            Pattern pattern = TodoTxtHighlighterPattern.LINE_OF_TEXT.getPattern();
            boolean ok = false;
            for (Matcher m = pattern.matcher(e); m.find(); ) {
                if (ok || true) {
                    e.setSpan(new ParagraphLineBackgroundSpan(AppSettings.get().getBackgroundColor()), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                ok = !ok;
            }

        } catch (Exception ex) {
            // Ignoring errors
        }

        return e;
    }

    @Override
    public InputFilter getAutoFormatter() {
        return new TodoTxtAutoFormat();
    }

    private void createHeaderSpanForMatches(Editable e, TodoTxtHighlighterPattern pattern, int headerColor) {

        createSpanForMatches(e, pattern, new TodoTxtHeaderSpanCreator(this, e, headerColor));
    }

    private void createColorSpanForDoublespace(Editable e, TodoTxtHighlighterPattern pattern, final int color) {
        createSpanForMatches(e, pattern, new SpanCreator() {
            @Override
            public ParcelableSpan create(Matcher matcher) {
                return new BackgroundColorSpan(color);
            }
        });
    }

    private void createMonospaceSpanForMatches(Editable e, TodoTxtHighlighterPattern pattern) {
        createSpanForMatches(e, pattern, new SpanCreator() {
            @Override
            public ParcelableSpan create(Matcher matcher) {
                return new TypefaceSpan("monospace");
            }
        });
    }

    private void createSpanWithStrikeThroughForMatches(Editable e, TodoTxtHighlighterPattern pattern) {
        createSpanForMatches(e, pattern, new SpanCreator() {
            @Override
            public ParcelableSpan create(Matcher matcher) {
                return new StrikethroughSpan();
            }
        });
    }

    private void createStyleSpanForMatches(final Editable e, final TodoTxtHighlighterPattern pattern, final int style) {
        createSpanForMatches(e, pattern, new SpanCreator() {
            @Override
            public ParcelableSpan create(Matcher matcher) {
                return new StyleSpan(style);
            }
        });
    }

    private void createColorSpanForMatches(final Editable e, final TodoTxtHighlighterPattern pattern, final int color) {
        createSpanForMatches(e, pattern, new SpanCreator() {
            @Override
            public ParcelableSpan create(Matcher matcher) {

                return new ForegroundColorSpan(color);
            }
        });
    }

    private void createSpanForMatches(final Editable e, final TodoTxtHighlighterPattern pattern, final SpanCreator creator) {
        createSpanForMatches(e, pattern.getPattern(), creator);
    }

    private void createSpanForMatches(final Editable e, final Pattern pattern, final SpanCreator creator) {
        for (Matcher m = pattern.matcher(e); m.find(); ) {
            e.setSpan(creator.create(m), m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    // New paragraph can only be determined at the beginning using this method. End not possible.
    private static abstract class ParagraphBackgroundSpan implements LineBackgroundSpan {
        private int _previousParagraphBeginIndex = -1;
        private boolean _isFirstParagraph = true;

        @Override
        public void drawBackground(Canvas c, Paint p, int left, int right, int top, int baseline, int bottom, CharSequence text, int start, int end, int lnum) {
            final int paintColorBak = p.getColor();
            boolean isFirstLineOfParagraph = _previousParagraphBeginIndex != start;
            drawBackground(c, p, left, right, top, baseline, bottom, text, start, end, lnum, isFirstLineOfParagraph, _isFirstParagraph);
            _previousParagraphBeginIndex = end;
            _isFirstParagraph = false;
            p.setColor(paintColorBak);
        }

        @SuppressWarnings("WeakerAccess")
        public abstract void drawBackground(Canvas c, Paint p, int left, int right, int top, int baseline, int bottom, CharSequence text, int start, int end, int lnum, boolean isFirstLineOfParagraph, boolean isFirstParagraph);
    }

    private static class FilledLineBackgroundSpan implements LineBackgroundSpan {
        private final int _color;
        private int _previousIndex = -1;

        public FilledLineBackgroundSpan(int color) {
            _color = color;
        }

        @Override
        public void drawBackground(Canvas c, Paint p, int left, int right, int top, int baseline, int bottom, CharSequence text, int start, int end, int lnum) {
            final int previousColorBak = p.getColor();
            float onePercent = 0.01f * (bottom - top);
            p.setColor(_color);
            c.drawRect(new Rect(left, top, right, bottom), p);
            if (_previousIndex != start) {
                p.setColor(0xff00ee00);
                c.drawRect(new Rect(left, (int) (top + 2 * onePercent), right, (int) (top + 5 * onePercent)), p);
            }
            _previousIndex = end;
            p.setColor(previousColorBak);
        }
    }

    private static class ParagraphLineBackgroundSpan extends ParagraphBackgroundSpan {
        private final int _color;

        public ParagraphLineBackgroundSpan(int color) {
            _color = color;
        }

        @Override
        public void drawBackground(Canvas c, Paint p, int left, int right, int top, int baseline, int bottom, CharSequence text, int start, int end, int lnum, boolean isFirstLineOfParagraph, boolean isFirstParagraph) {
            if (!isFirstParagraph && isFirstLineOfParagraph) {
                float onePercent = 0.01f * (bottom - top);
                top = Math.round(top - onePercent / 2); // increase top space a little
                top = top < 0 ? 0 : top;
                p.setColor(_color);
                c.drawRect(new Rect(left, top, right, (int) (top + 3 * onePercent)), p);
            }
        }
    }
}

