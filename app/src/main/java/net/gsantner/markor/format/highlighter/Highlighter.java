/*
 * Copyright (c) 2014 Jeff Martin
 * Copyright (c) 2015 Pedro Lafuente
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.format.highlighter;

import android.text.Editable;
import android.text.InputFilter;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;

import net.gsantner.markor.format.highlighter.markdown.MarkdownHighlighter;

public abstract class Highlighter {

    protected abstract Editable run(Editable e);

    public abstract InputFilter getAutoFormatter();

    protected static Highlighter getDefaultHighlighter() {
        return new MarkdownHighlighter();
    }


    protected void clearSpans(Editable e) {
        clearSpanType(e, TextAppearanceSpan.class);
        clearSpanType(e, ForegroundColorSpan.class);
        clearSpanType(e, BackgroundColorSpan.class);
        clearSpanType(e, StrikethroughSpan.class);
        clearSpanType(e, StyleSpan.class);
    }

    protected <T extends CharacterStyle> void clearSpanType(Editable e, Class<T> spanType) {
        CharacterStyle[] spans = e.getSpans(0, e.length(), spanType);

        for (int n = spans.length; n-- > 0; ) {
            e.removeSpan(spans[n]);
        }
    }
}
