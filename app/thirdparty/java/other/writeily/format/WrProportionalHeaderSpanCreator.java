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
import android.content.res.Resources;
import android.graphics.Typeface;
import android.text.ParcelableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.TextAppearanceSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;

public class WrProportionalHeaderSpanCreator {

    private static final DisplayMetrics DISPLAY_METRICS = Resources.getSystem().getDisplayMetrics();

    private final String _fontType;
    private final int _fontSize;
    private final int _color;
    private final boolean _shouldUseDynamicTextSize;

    public WrProportionalHeaderSpanCreator(String fontType, int fontSize, int color, boolean shouldUseDynamicTextSize) {
        _fontType = fontType;
        _fontSize = fontSize;
        _color = color;
        _shouldUseDynamicTextSize = shouldUseDynamicTextSize;
    }

    public ParcelableSpan createHeaderSpan(float proportion) {
        if (_shouldUseDynamicTextSize) {
            Float size = calculateAdjustedSize(proportion);
            return new TextAppearanceSpan(_fontType, Typeface.BOLD, size.byteValue(),
                    ColorStateList.valueOf(_color), null);
        } else {
            return new ForegroundColorSpan(_color);
        }
    }

    private float calculateAdjustedSize(Float proportion) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                _fontSize * proportion,
                DISPLAY_METRICS);
    }
}
