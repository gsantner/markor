/*#######################################################
 *
 *   Maintained 2018-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package other.writeily.format.wikitext;

import net.gsantner.opoc.wrapper.GsCallback;

import java.util.regex.Matcher;

import other.writeily.format.WrProportionalHeaderSpanCreator;

public class WrWikitextHeaderSpanCreator implements GsCallback.r1<Object, Matcher> {

    private static final Character EQUAL_SIGN = '=';
    private static final float STANDARD_PROPORTION_MAX = 1.60f;
    private static final float STANDARD_PROPORTION_MIN = 1.00f;
    private static final float SIZE_STEP = (STANDARD_PROPORTION_MAX - STANDARD_PROPORTION_MIN) / 5f;

    private final CharSequence _text;
    private final WrProportionalHeaderSpanCreator _spanCreator;

    public WrWikitextHeaderSpanCreator(final CharSequence text, int color) {
        _text = text;
        _spanCreator = new WrProportionalHeaderSpanCreator(color);
    }

    public Object callback(final Matcher m) {
        final char[] headingCharacters = extractMatchingRange(m);
        float proportion = calculateProportionBasedOnEqualSignCount(headingCharacters);
        return _spanCreator.createHeaderSpan(proportion);
    }

    private char[] extractMatchingRange(Matcher m) {
        return _text.subSequence(m.start(), m.end()).toString().trim().toCharArray();
    }

    private float calculateProportionBasedOnEqualSignCount(final char[] headingSequence) {
        float proportion = STANDARD_PROPORTION_MIN;
        int i = 1;  // start with second char (H5 level)
        // one level bigger for each '='
        while (EQUAL_SIGN.equals(headingSequence[i]) && proportion < STANDARD_PROPORTION_MAX) {
            proportion += SIZE_STEP;
            i++;
        }
        return proportion;
    }
}
