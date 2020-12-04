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

import android.text.ParcelableSpan;
import android.text.Spannable;

import net.gsantner.markor.ui.hleditor.Highlighter;
import net.gsantner.markor.ui.hleditor.SpanCreator;

import java.util.regex.Matcher;

import other.writeily.format.WrProportionalHeaderSpanCreator;

public class WrMarkdownHeaderSpanCreator implements SpanCreator.ParcelableSpanCreator {
    private static final Character POUND_SIGN = '#';
    private static final float STANDARD_PROPORTION_MAX = 1.80f;
    private static final float SIZE_STEP = 0.20f;

    protected Highlighter _highlighter;
    private final Spannable _spannable;
    private final WrProportionalHeaderSpanCreator _spanCreator;

    public WrMarkdownHeaderSpanCreator(Highlighter highlighter, Spannable spannable, int color, boolean dynamicTextSize) {
        _highlighter = highlighter;
        _spannable = spannable;
        _spanCreator = new WrProportionalHeaderSpanCreator(highlighter.getAppSettings().getFontFamily(), highlighter.getAppSettings().getFontSize(), color, dynamicTextSize);
    }

    public ParcelableSpan create(Matcher m, int iM) {
        final char[] charSequence = extractMatchingRange(m);
        float proportion = calculateProportionBasedOnHeaderType(charSequence);
        return _spanCreator.createHeaderSpan(proportion);
    }

    private char[] extractMatchingRange(Matcher m) {
        return _spannable.subSequence(m.start(), m.end()).toString().trim().toCharArray();
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
