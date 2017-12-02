/*
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.format.highlighter.general;

import android.graphics.Color;
import android.text.ParcelableSpan;

import net.gsantner.markor.format.highlighter.SpanCreator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HexColorCodeUnderlineSpan implements SpanCreator {
    public static final Pattern PATTERN = Pattern.compile("(?:\\s|^)(#[A-Fa-f0-9]{6,8})+(?:\\s|$)");

    public ParcelableSpan create(Matcher matcher, int iM) {
        return new ColorUnderlineSpan(Color.parseColor(matcher.group(1)), null);

    }
}
