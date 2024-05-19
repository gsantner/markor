/*#######################################################
 *
 *   Maintained 2018-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.general;

import android.text.TextPaint;
import android.text.style.UnderlineSpan;
import android.text.style.UpdateAppearance;

import net.gsantner.opoc.util.GsContextUtils;

import java.lang.reflect.Method;
import java.util.regex.Pattern;

public class ColorUnderlineSpan extends UnderlineSpan implements UpdateAppearance {
    public static final Pattern HEX_CODE_UNDERLINE_PATTERN = Pattern.compile("(?:\\s|[\";,:'*]|^)(#[A-Fa-f0-9]{6,8})+(?:\\s|[\";,:'*]|$)");

    private final int _color;
    private final float _thickness;

    public ColorUnderlineSpan(String colorHexString, Float thickness) {
        this(GsContextUtils.instance.parseHexColorString(colorHexString), thickness);
    }

    public ColorUnderlineSpan(int color, Float thickness) {
        _color = color;
        _thickness = thickness == null ? 1.0f : thickness;
    }

    @Override
    @SuppressWarnings("JavaReflectionMemberAccess")
    public void updateDrawState(final TextPaint tp) {
        try {
            Method method = TextPaint.class.getMethod("setUnderlineText", Integer.TYPE, Float.TYPE);
            method.invoke(tp, _color, _thickness);
        } catch (Exception e) {
            tp.setUnderlineText(true);
        }
    }
}