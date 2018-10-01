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
import android.text.style.ParagraphStyle;

import net.gsantner.markor.ui.hleditor.SpanCreator;

import java.util.regex.Matcher;

public class FilledBackgroundParagraphSpan extends BackgroundParagraphSpan {
    private final int _color;

    public FilledBackgroundParagraphSpan(int color) {
        _color = color;
    }


    @Override
    public void drawBackground(Canvas c, Paint p, int left, int right, int top, int baseline, int bottom, CharSequence text, int start, int end, int lnum, boolean isFirstLineOfParagraph) {

        p.setColor(_color);
        c.drawRect(left, top, right, bottom, p);
    }

    //
    //
    //

    public static class EverySecondLineSpanCreatorP implements SpanCreator.ParagraphStyleCreator {
        private int _color;

        public EverySecondLineSpanCreatorP(int color) {
            _color = color;
        }

        @Override
        public ParagraphStyle create(Matcher matcher, int iM) {
            return (iM == 0 || iM % 2 == 1)
                    ? null : new FilledBackgroundParagraphSpan(_color
            );
        }
    }
}
