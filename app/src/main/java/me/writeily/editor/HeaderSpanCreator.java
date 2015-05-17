package me.writeily.editor;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.ParcelableSpan;
import android.text.style.TextAppearanceSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import java.util.regex.Matcher;

class HeaderSpanCreator implements SpanCreator {

    public static final Character POUND_SIGN = '#';
    public static final DisplayMetrics DISPLAY_METRICS = Resources.getSystem().getDisplayMetrics();
    public static final float STANDARD_PROPORTION_MAX = 1.80f;
    public static final float SIZE_STEP = 0.20f;

    private Highlighter highlighter;
    private final Editable e;
    private final int color;

    public HeaderSpanCreator(Highlighter highlighter, Editable e, int color) {
        this.highlighter = highlighter;
        this.e = e;
        this.color = color;
    }

    public ParcelableSpan create(Matcher m) {
        final char[] charSequence = extractMatchingRange(m);
        Float proportion = calculateProportionBasedOnHeaderType(charSequence);
        Float size = calculateAdjustedSize(proportion);
        return new TextAppearanceSpan(highlighter.fontType, Typeface.BOLD, (int) size.byteValue(),
                ColorStateList.valueOf(color), null);
    }

    private float calculateAdjustedSize(Float proportion) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                highlighter.fontSize * proportion,
                DISPLAY_METRICS);
    }

    private char[] extractMatchingRange(Matcher m) {
        return e.subSequence(m.start(), m.end()).toString().trim().toCharArray();
    }

    private Float calculateProportionBasedOnHeaderType(final char[] charSequence) {

        Float proportion = calculateProportionForHashesHeader(charSequence);
        if (proportion == STANDARD_PROPORTION_MAX) {
            return calculateProportionForUnderlineHeader(charSequence);
        }
        return proportion;
    }

    private Float calculateProportionForUnderlineHeader(final char[] charSequence) {
        Float proportion = STANDARD_PROPORTION_MAX;
        if (Character.valueOf('=').equals(charSequence[charSequence.length - 1])) {
            proportion -= SIZE_STEP;
        } else if (Character.valueOf('-').equals(charSequence[charSequence.length - 1])) {
            proportion -= (SIZE_STEP * 2);
        }
        return proportion;
    }

    private Float calculateProportionForHashesHeader(final char[] charSequence) {
        float proportion = STANDARD_PROPORTION_MAX;
        int i = 0;
        // Reduce by SIZE_STEP for each #
        while (POUND_SIGN.equals(charSequence[i])) {
            proportion -= SIZE_STEP;
            i++;
        }
        return proportion;
    }
}
