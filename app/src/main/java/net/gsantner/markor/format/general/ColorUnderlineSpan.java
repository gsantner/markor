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

import android.text.TextPaint;
import android.text.style.UnderlineSpan;
import android.text.style.UpdateAppearance;

import java.lang.reflect.Method;

public class ColorUnderlineSpan extends UnderlineSpan implements UpdateAppearance {
    private final int _color;
    private final float _thickness;

    public ColorUnderlineSpan(int color, Float thickness) {
        _color = color;
        _thickness = thickness == null ? 1.0f : thickness;
    }

    @Override
    public void updateDrawState(final TextPaint tp) {
        try {
            Method method = TextPaint.class.getMethod("setUnderlineText", Integer.TYPE, Float.TYPE);
            method.invoke(tp, _color, _thickness);
        } catch (Exception e) {
            tp.setUnderlineText(true);
        }
    }
}