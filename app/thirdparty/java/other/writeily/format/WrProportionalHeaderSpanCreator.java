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

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.text.ParcelableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.TextAppearanceSpan;

public class WrProportionalHeaderSpanCreator {

    private final String _fontFamily;
    private final float _textSize;
    private final int _color;

    public WrProportionalHeaderSpanCreator(final String fontFamily, float textSize, int color) {
        _fontFamily = fontFamily;
        _textSize = textSize;
        _color = color;
    }

    public ParcelableSpan createHeaderSpan(float proportion) {
        final int size = (int) (_textSize * proportion);
        return new TextAppearanceSpan(_fontFamily, Typeface.BOLD, size, ColorStateList.valueOf(_color), null);
    }
}
