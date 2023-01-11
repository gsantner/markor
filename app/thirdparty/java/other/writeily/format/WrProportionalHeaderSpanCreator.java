/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package other.writeily.format;

import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

public class WrProportionalHeaderSpanCreator {

    private final float _textSize;
    private final int _color;

    public WrProportionalHeaderSpanCreator(final float textSize, final int color) {
        _textSize = textSize;
        _color = color;
    }

    public Object createHeaderSpan(final float proportion) {
        return new LargerHeaderSpan(_color, _textSize * proportion);
    }

    public static class LargerHeaderSpan extends MetricAffectingSpan {

        final @ColorInt int _color;
        final float _textSize;

        public LargerHeaderSpan(final @ColorInt int color, final float textSize) {
            _color = color;
            _textSize = textSize;
        }

        @Override
        public void updateMeasureState(@NonNull TextPaint textPaint) {
            textPaint.setFakeBoldText(true);
            textPaint.setColor(_color);
            textPaint.setTextSize(_textSize);
        }

        @Override
        public void updateDrawState(@NonNull TextPaint textPaint) {
            textPaint.setFakeBoldText(true);
            textPaint.setColor(_color);
            textPaint.setTextSize(_textSize);
        }
    }
}