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

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.style.LineBackgroundSpan;

// New paragraph can only be determined at the start. End cannot be detected ahead using this method.
public abstract class BackgroundParagraphSpan implements LineBackgroundSpan {
    private int _previousParagraphBeginIndex = -1;

    @Override
    public void drawBackground(Canvas c, Paint p, int left, int right, int top, int baseline, int bottom, CharSequence text, int start, int end, int lnum) {
        final int paintColorBak = p.getColor();
        boolean isFirstLineOfParagraph = _previousParagraphBeginIndex != start;
        drawBackground(c, p, left, right, top, baseline, bottom, text, start, end, lnum, isFirstLineOfParagraph);
        _previousParagraphBeginIndex = end;
        p.setColor(paintColorBak);
    }

    @SuppressWarnings("WeakerAccess")
    public abstract void drawBackground(Canvas c, Paint p, int left, int right, int top, int baseline, int bottom, CharSequence text, int start, int end, int lnum, boolean isFirstLineOfParagraph);

    /*
    // Does not work. Should be respected at Span creation time anyway, not here.
    protected int calculateParagraphNumber(CharSequence text, int start, int end) {
        String s = text.toString();
        int number = -1;
        for (int i = -2; i != -1 && i < end+1 && i < s.length(); ) {
            number++;
            i = s.indexOf("\n", i + 1);
        }
        return number;
    }
    */
}
