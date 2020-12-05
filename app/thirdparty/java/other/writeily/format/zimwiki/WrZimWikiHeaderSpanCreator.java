/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package other.writeily.format.zimwiki;

import android.text.ParcelableSpan;
import android.text.Spannable;

import net.gsantner.markor.format.zimwiki.ZimWikiHighlighter;
import net.gsantner.markor.ui.hleditor.SpanCreator;

import java.util.regex.Matcher;

import other.writeily.format.WrProportionalHeaderSpanCreator;

public class WrZimWikiHeaderSpanCreator implements SpanCreator.ParcelableSpanCreator {
    private static final Character EQUAL_SIGN = '=';
    private static final float STANDARD_PROPORTION_MAX = 1.60f;
    private static final float STANDARD_PROPORTION_MIN = 1.00f;
    private static final float SIZE_STEP = (STANDARD_PROPORTION_MAX - STANDARD_PROPORTION_MIN) / 5f;

    protected ZimWikiHighlighter _highlighter;
    private final Spannable _spannable;
    private final WrProportionalHeaderSpanCreator _spanCreator;

    public WrZimWikiHeaderSpanCreator(ZimWikiHighlighter highlighter, Spannable spannable, int color, boolean dynamicTextSize, final String fontType, final int fontSize) {
        _highlighter = highlighter;
        _spannable = spannable;
        _spanCreator = new WrProportionalHeaderSpanCreator(fontType, fontSize, color, dynamicTextSize);
    }

    public ParcelableSpan create(Matcher m, int iM) {
        final char[] headingCharacters = extractMatchingRange(m);
        float proportion = calculateProportionBasedOnEqualSignCount(headingCharacters);
        return _spanCreator.createHeaderSpan(proportion);
    }

    private char[] extractMatchingRange(Matcher m) {
        return _spannable.subSequence(m.start(), m.end()).toString().trim().toCharArray();
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
