/*#######################################################
 * Copyright (c) 2014 Jeff Martin
 * Copyright (c) 2015 Pedro Lafuente
 * Copyright (c) 2017-2019 Gregor Santner
 *
 * Licensed under the MIT license.
 * You can get a copy of the license text here:
 *   https://opensource.org/licenses/MIT
###########################################################*/
package other.writeily.format.markdown;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.ParcelableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.TextAppearanceSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import net.gsantner.markor.format.markdown.MarkdownHighlighter;
import net.gsantner.markor.ui.hleditor.SpanCreator;

import java.util.regex.Matcher;

public class WrMarkdownHeaderSpanCreator implements SpanCreator.ParcelableSpanCreator {
    private static final Character POUND_SIGN = '#';
    private static final DisplayMetrics DISPLAY_METRICS = Resources.getSystem().getDisplayMetrics();
    private static final float STANDARD_PROPORTION_MAX = 1.80f;
    private static final float SIZE_STEP = 0.20f;

    protected MarkdownHighlighter _highlighter;
    private final Editable _editable;
    private final int _color;
    private final boolean _dynmicTextSize;

    public WrMarkdownHeaderSpanCreator(MarkdownHighlighter highlighter, Editable editable, int color, boolean dynamicTextSize) {
        _highlighter = highlighter;
        _editable = editable;
        _color = color;
        _dynmicTextSize = dynamicTextSize;
    }

    public ParcelableSpan create(Matcher m, int iM) {
        if (_dynmicTextSize) {
            final char[] charSequence = extractMatchingRange(m);
            float proportion = calculateProportionBasedOnHeaderType(charSequence);
            Float size = calculateAdjustedSize(proportion);
            return new TextAppearanceSpan(_highlighter._fontType, Typeface.BOLD, (int) size.byteValue(),
                    ColorStateList.valueOf(_color), null);
        } else {
            return new ForegroundColorSpan(_color);
        }
    }

    private float calculateAdjustedSize(Float proportion) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                _highlighter._fontSize * proportion,
                DISPLAY_METRICS);
    }

    private char[] extractMatchingRange(Matcher m) {
        return _editable.subSequence(m.start(), m.end()).toString().trim().toCharArray();
    }

    private Float calculateProportionBasedOnHeaderType(final char[] charSequence) {

        Float proportion = calculateProportionForHashesHeader(charSequence);
        if (proportion == STANDARD_PROPORTION_MAX) {
            return calculateProportionForUnderlineHeader(charSequence);
        }
        return proportion;
    }

    private Float calculateProportionForUnderlineHeader(final char[] charSequence) {
        return Character.valueOf('=').equals(charSequence[charSequence.length - 1])
                ? 1.6f : 1.0f;
    }

    private Float calculateProportionForHashesHeader(final char[] charSequence) {
        float proportion = STANDARD_PROPORTION_MAX;
        int i = 0;
        // Reduce by SIZE_STEP for each #
        while (POUND_SIGN.equals(charSequence[i]) && proportion >= 1.0) {
            proportion -= SIZE_STEP;
            i++;
        }
        return proportion;
    }
}
