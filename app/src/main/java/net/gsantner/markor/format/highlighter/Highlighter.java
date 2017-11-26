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
import android.text.ParcelableSpan;
import android.text.Spannable;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.LineBackgroundSpan;
import android.text.style.LineHeightSpan;
import android.text.style.ParagraphStyle;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;
import android.text.style.TypefaceSpan;

import net.gsantner.markor.format.highlighter.markdown.MarkdownHighlighter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("UnusedReturnValue")
public abstract class Highlighter {
    protected final static InputFilter AUTOFORMATTER_NONE = (charSequence, i, i1, spanned, i2, i3) -> null;

    protected abstract Editable run(final HighlightingEditor editor, final Editable editable);

    public abstract InputFilter getAutoFormatter();

    protected static Highlighter getDefaultHighlighter() {
        return new MarkdownHighlighter();
    }

    //
    // Clear spans
    //

    protected void clearSpans(Editable editable) {
        clearCharacterSpanType(editable, TextAppearanceSpan.class);
        clearCharacterSpanType(editable, ForegroundColorSpan.class);
        clearCharacterSpanType(editable, BackgroundColorSpan.class);
        clearCharacterSpanType(editable, StrikethroughSpan.class);
        clearCharacterSpanType(editable, RelativeSizeSpan.class);
        clearCharacterSpanType(editable, StyleSpan.class);
        clearParagraphSpanType(editable, LineBackgroundSpan.class);
        clearParagraphSpanType(editable, LineHeightSpan.class);
    }

    private <T extends CharacterStyle> void clearCharacterSpanType(Editable editable, Class<T> spanType) {
        CharacterStyle[] spans = editable.getSpans(0, editable.length(), spanType);

        for (int n = spans.length; n-- > 0; ) {
            editable.removeSpan(spans[n]);
        }
    }

    private <T extends ParagraphStyle> void clearParagraphSpanType(Editable editable, Class<T> spanType) {
        ParagraphStyle[] spans = editable.getSpans(0, editable.length(), spanType);

        for (int n = spans.length; n-- > 0; ) {
            editable.removeSpan(spans[n]);
        }
    }

    //
    // Create spans
    //

    protected void createSpanForMatches(final Editable editable, final Pattern pattern, final SpanCreator creator) {
        int i = 0;
        for (Matcher m = pattern.matcher(editable); m.find(); i++) {
            ParcelableSpan span = creator.create(m, i);
            if (span != null) {
                editable.setSpan(span, m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    // Create spans for paragraphs, these have no common way of derivation with ParcelableSpan
    protected void createSpanForMatchesP(final Editable editable, final Pattern pattern, final SpanCreatorP creator) {
        int i = 0;
        for (Matcher m = pattern.matcher(editable); m.find(); i++) {
            ParagraphStyle span = creator.create(m, i);
            if (span != null) {
                editable.setSpan(span, m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    protected void createStyleSpanForMatches(final Editable editable, final Pattern pattern, final int style) {
        createSpanForMatches(editable, pattern, (matcher, iM) -> new StyleSpan(style));
    }

    protected void createColorSpanForMatches(final Editable editable, final Pattern pattern, final int color) {
        createSpanForMatches(editable, pattern, (matcher, iM) -> new ForegroundColorSpan(color));
    }

    private void createColorBackgroundSpan(Editable editable, final Pattern pattern, final int color) {
        createSpanForMatches(editable, pattern, (matcher, iM) -> new BackgroundColorSpan(color));
    }

    protected void createSpanWithStrikeThroughForMatches(Editable editable, final Pattern pattern) {
        createSpanForMatches(editable, pattern, (matcher, iM) -> new StrikethroughSpan());
    }

    protected void createTypefaceSpanForMatches(Editable editable, Pattern pattern, final String typeface) {
        createSpanForMatches(editable, pattern, (matcher, iM) -> new TypefaceSpan(typeface));
    }

    protected void createRelativeSizeSpanForMatches(Editable editable, final Pattern pattern, float relativeSize) {
        createSpanForMatches(editable, pattern, (matcher, iM) -> new RelativeSizeSpan(relativeSize));
    }

    protected void createMonospaceSpanForMatches(Editable editable, final Pattern pattern) {
        createTypefaceSpanForMatches(editable, pattern, "monospace");
    }

    protected void createParagraphStyleSpanForMatches(Editable editable, final Pattern pattern, final SpanCreatorP creator) {
        createSpanForMatchesP(editable, pattern, creator);
    }


}
