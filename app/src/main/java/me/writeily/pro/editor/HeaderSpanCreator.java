package me.writeily.pro.editor;

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
        final char[] charSequence = e.subSequence(m.start(), m.end()).toString().trim().toCharArray();
        Float proportion = calculateProportion(charSequence);
        Float size = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                highlighter.fontSize * proportion,
                DISPLAY_METRICS);
        return new TextAppearanceSpan(highlighter.fontType, Typeface.BOLD, (int) size.byteValue(),
                ColorStateList.valueOf(color), null);
    }

    private Float calculateProportion(final char[] charSequence) {

        Float proportion = calculateProportionForHashesHeader(charSequence);
        if (proportion == STANDARD_PROPORTION_MAX) {
            return calculateProportionForUnderlineHeader(charSequence);
        }
        return proportion;
    }

    private Float calculateProportionForUnderlineHeader(final char[] charSequence) {
        Float proportion = STANDARD_PROPORTION_MAX;
        if (Character.valueOf('=').compareTo(charSequence[charSequence.length - 1]) == 0) {
            proportion -= SIZE_STEP;
        } else if (Character.valueOf('-').compareTo(charSequence[charSequence.length - 1]) == 0) {
            proportion -= (SIZE_STEP * 2);
        }
        return proportion;
    }

    private Float calculateProportionForHashesHeader(final char[] charSequence) {
        float proportion = STANDARD_PROPORTION_MAX;
        int i = 0;
        while (POUND_SIGN.compareTo(charSequence[i]) == 0) {
            proportion -= SIZE_STEP;
            i++;
        }
        return proportion;
    }
}
