/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.general;

import android.graphics.Paint;
import android.text.style.LineHeightSpan;

public class FirstLineTopPaddedParagraphSpan implements LineHeightSpan {
    private float _paragraphFirstlineSpacing;
    private Integer _origAscent;

    public FirstLineTopPaddedParagraphSpan(float paragraphFirstlineSpacing) {
        _paragraphFirstlineSpacing = paragraphFirstlineSpacing;
    }

    @Override
    public void chooseHeight(CharSequence text, int start, int end, int spanstartv, int v, Paint.FontMetricsInt fm) {
        if (_origAscent == null) {
            _origAscent = fm.ascent;
        }
        boolean isFirstLineInParagraph = spanstartv == v;
        fm.ascent = (isFirstLineInParagraph) ? Math.round(fm.ascent * _paragraphFirstlineSpacing) : _origAscent;
    }
}
