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

import android.graphics.Paint;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.text.style.LineHeightSpan;
import android.text.style.UpdateLayout;

import androidx.annotation.ColorInt;

public class WrProportionalHeaderSpanCreator {

    private final float _textSize;
    private final int _color;

    public WrProportionalHeaderSpanCreator(float textSize, int color) {
        _textSize = textSize;
        _color = color;
    }

    public Object createHeaderSpan(final float proportion) {
        return new LargerHeaderSpan(_color, _textSize, proportion);
    }

    public static class LargerHeaderSpan extends CharacterStyle implements LineHeightSpan, UpdateLayout {

        final @ColorInt
        int _color;
        final float _headerSize;
        final float _offset;

        public LargerHeaderSpan(final @ColorInt int color, final float textSize, final float proportion) {
            _color = color;
            _headerSize = textSize * proportion;
            _offset = _headerSize - textSize;
        }

        @Override
        public void updateDrawState(TextPaint textPaint) {
            textPaint.setFakeBoldText(true);
            textPaint.setColor(_color);
            textPaint.setTextSize(_headerSize);
        }

        @Override
        public void chooseHeight(CharSequence charSequence, int i, int i1, int i2, int i3, Paint.FontMetricsInt fontMetricsInt) {
            fontMetricsInt.ascent -= _offset;
        }
    }
}