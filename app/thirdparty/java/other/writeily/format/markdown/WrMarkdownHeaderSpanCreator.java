/*#######################################################
 * Copyright (c) 2014 Jeff Martin
 * Copyright (c) 2015 Pedro Lafuente
 * Copyright (c) 2017-2024 Gregor Santner
 *
 * Licensed under the MIT license.
 * You can get a copy of the license text here:
 *   https://opensource.org/licenses/MIT
###########################################################*/
package other.writeily.format.markdown;

import net.gsantner.opoc.wrapper.GsCallback;

import java.util.regex.Matcher;

import other.writeily.format.WrProportionalHeaderSpanCreator;

public class WrMarkdownHeaderSpanCreator implements GsCallback.r1<Object, Matcher> {
    private static final float STANDARD_PROPORTION_MAX = 1.80f;
    private static final float SIZE_STEP = 0.20f;

    private final CharSequence _text;
    private final WrProportionalHeaderSpanCreator _spanCreator;

    public WrMarkdownHeaderSpanCreator(final CharSequence text, int color) {
        _text = text;
        _spanCreator = new WrProportionalHeaderSpanCreator(color);
    }

    public Object callback(final Matcher m) {
        final float proportion = calculateProportionBasedOnHeaderType(m.start(), m.end());
        return _spanCreator.createHeaderSpan(proportion);
    }

    private float calculateProportionBasedOnHeaderType(final int start, final int end) {
        final float proportion = calculateProportionForHashesHeader(start);
        if (proportion == STANDARD_PROPORTION_MAX) {
            return calculateProportionForUnderlineHeader(end);
        }
        return proportion;
    }

    private float calculateProportionForUnderlineHeader(final int end) {
        return _text.charAt(end - 1) == '=' ? (STANDARD_PROPORTION_MAX - SIZE_STEP) : 1.0f;
    }

    private Float calculateProportionForHashesHeader(int start) {
        float proportion = STANDARD_PROPORTION_MAX;
        // Reduce by SIZE_STEP for each #
        while (_text.charAt(start) == '#' && proportion >= 1.0) {
            proportion -= SIZE_STEP;
            start++;
        }
        return proportion;
    }
}
