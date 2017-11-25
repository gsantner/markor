/*
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.format.highlighter.todotxt;

import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputFilter;
import android.text.ParcelableSpan;
import android.text.Spannable;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
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

            // Do this at the end
            createColorSpanForMatches(e, TodoTxtHighlighterPattern.DONE, colors.getDoneColor());
            createSpanWithStrikeThroughForMatches(e, TodoTxtHighlighterPattern.DONE);
            ///createColorSpanForMatches(e, TodoTxtHighlighterPattern.LIST, colors.getListColor());
            //createColorSpanForMatches(e, TodoTxtHighlighterPattern.ORDEREDLIST, colors.getListColor());
            //createColorSpanForDoublespace(e, TodoTxtHighlighterPattern.DOUBLESPACE, colors.getPriorityColor());
            //createStyleSpanForMatches(e, TodoTxtHighlighterPattern.BOLD, Typeface.BOLD);
            //createStyleSpanForMatches(e, TodoTxtHighlighterPattern.ITALICS, Typeface.ITALIC);
            //createColorSpanForMatches(e, TodoTxtHighlighterPattern.QUOTATION, colors.getQuotationColor());
            //createSpanWithStrikeThroughForMatches(e, TodoTxtHighlighterPattern.STRIKETHROUGH);
            //createMonospaceSpanForMatches(e, TodoTxtHighlighterPattern.MONOSPACED);
            //createColorSpanForDoublespace(e, TodoTxtHighlighterPattern.MONOSPACED, colors.getPriorityColor());

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
}

