/*#######################################################
 * Copyright (c) 2014 Jeff Martin
 * Copyright (c) 2015 Pedro Lafuente
 * Copyright (c) 2017-2024 Gregor Santner
 *
 * Licensed under the MIT license.
 * You can get a copy of the license text here:
 *   https://opensource.org/licenses/MIT
###########################################################*/
package other.writeily.format.asciidoc;

import net.gsantner.opoc.wrapper.GsCallback;

import java.util.regex.Matcher;

import other.writeily.format.WrProportionalHeaderSpanCreator;

public class WrAsciidocHeaderSpanCreator implements GsCallback.r1<Object, Matcher> {
    private static final Character POUND_SIGN = '=';
    private static final float STANDARD_PROPORTION_MAX = 1.80f;
    private static final float SIZE_STEP = 0.20f;

    private final CharSequence _text;
    private final WrProportionalHeaderSpanCreator _spanCreator;

    public WrAsciidocHeaderSpanCreator(final CharSequence text, int color) {
        _text = text;
        _spanCreator = new WrProportionalHeaderSpanCreator(color);
    }

    public Object callback(Matcher m) {
        final char[] charSequence = extractMatchingRange(m);
        float proportion = calculateProportionBasedOnHeaderType(charSequence);
        return _spanCreator.createHeaderSpan(proportion);
    }

    private char[] extractMatchingRange(Matcher m) {
        return _text.subSequence(m.start(), m.end()).toString().trim().toCharArray();
    }

    private Float calculateProportionBasedOnHeaderType(final char[] charSequence) {

        Float proportion = calculateProportionForHashesHeader(charSequence);
        if (proportion == STANDARD_PROPORTION_MAX) {
            return calculateProportionForUnderlineHeader(charSequence);
        }
        return proportion;
    }

    private Float calculateProportionForUnderlineHeader(final char[] charSequence) {
        return Character.valueOf('=').equals(charSequence[charSequence.length - 1]) ? 1.6f : 1.0f;
    }

    private Float calculateProportionForHashesHeader(final char[] charSequence) {
        float proportion = STANDARD_PROPORTION_MAX;
        int i = 0;
        // Reduce by SIZE_STEP for each POUND_SIGN
        while (POUND_SIGN.equals(charSequence[i]) && proportion >= 1.0) {
            proportion -= SIZE_STEP;
            i++;
        }
        return proportion;
    }
}
