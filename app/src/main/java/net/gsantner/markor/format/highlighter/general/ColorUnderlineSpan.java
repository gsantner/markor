/*
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.format.highlighter.general;

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