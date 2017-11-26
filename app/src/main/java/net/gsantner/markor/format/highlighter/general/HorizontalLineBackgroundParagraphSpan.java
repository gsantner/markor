package net.gsantner.markor.format.highlighter.general;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.ColorInt;

// Creates a colored line at the very top of a paragraph
public class HorizontalLineBackgroundParagraphSpan extends BackgroundParagraphSpan {
    private final int _color;
    private final int _sizePercent;

    public HorizontalLineBackgroundParagraphSpan(@ColorInt int color) {
        this(color, -1);
    }

    public HorizontalLineBackgroundParagraphSpan(@ColorInt int color, int sizePercent) {
        _color = color;
        _sizePercent = (sizePercent > 0 && sizePercent <= 100) ? sizePercent : 3;
    }

    @Override
    public void drawBackground(Canvas c, Paint p, int left, int right, int top, int baseline, int bottom, CharSequence text, int start, int end, int lnum, boolean isFirstLineOfParagraph) {
        if (start > 0 && isFirstLineOfParagraph) {
            float onePercent = 0.01f * (bottom - top);
            top = Math.round(top - onePercent / 2); // increase top space a little
            top = top < 0 ? 0 : top;
            p.setColor(_color);
            int w = c.getWidth();
            int w2 = right-left;
            c.drawRect(new Rect(left, top, right, (int) (top + _sizePercent * onePercent)), p);
        }
    }
}
