/*#######################################################
 *
 *   Maintained 2018-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package other.writeily.format;

import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

public class WrProportionalHeaderSpanCreator {

    private final int _color;

    public WrProportionalHeaderSpanCreator(final int color) {
        _color = color;
    }

    public Object createHeaderSpan(final float proportion) {
        return new LargerHeaderSpan(_color, proportion);
    }

    public static class LargerHeaderSpan extends MetricAffectingSpan {

        final @ColorInt
        int _color;
        final float _proportion;

        public LargerHeaderSpan(final @ColorInt int color, final float proportion) {
            _color = color;
            _proportion = proportion;
        }

        @Override
        public void updateMeasureState(@NonNull TextPaint textPaint) {
            textPaint.setFakeBoldText(true);
            textPaint.setColor(_color);
            textPaint.setTextSize(textPaint.getTextSize() * _proportion);
        }

        @Override
        public void updateDrawState(@NonNull TextPaint textPaint) {
            textPaint.setFakeBoldText(true);
            textPaint.setColor(_color);
            textPaint.setTextSize(textPaint.getTextSize() * _proportion);
        }
    }
}