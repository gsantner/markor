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

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.ColorInt;

// Creates a colored line at the very top of a paragraph
public class HorizontalLineBackgroundParagraphSpan extends BackgroundParagraphSpan {
    private final int _color;
    private final int _hrHeightPx;
    private final int _topOffsetPx;

    public HorizontalLineBackgroundParagraphSpan(@ColorInt int color) {
        this(color, -1, -1);
    }

    public HorizontalLineBackgroundParagraphSpan(@ColorInt int color, float hrHeightDp, float topOffsetDp) {
        hrHeightDp = hrHeightDp > 0 ? hrHeightDp : 2f;
        topOffsetDp = topOffsetDp > 0 ? topOffsetDp : 9f;
        _color = color;
        _hrHeightPx = Math.round(hrHeightDp * Resources.getSystem().getDisplayMetrics().density);
        _topOffsetPx = (int) topOffsetDp;//Math.round(topOffsetDp * Resources.getSystem().getDisplayMetrics().density);
    }

    @Override
    public void drawBackground(Canvas c, Paint p, int left, int right, int top, int baseline, int bottom, CharSequence text, int start, int end, int lnum, boolean isFirstLineOfParagraph) {
        if (start > 0 && isFirstLineOfParagraph) {
            p.setColor(_color);
            c.drawRect(new Rect(left, top + _topOffsetPx, right, top + _topOffsetPx + _hrHeightPx), p);
        }
    }
}
