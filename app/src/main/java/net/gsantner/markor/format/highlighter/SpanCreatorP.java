/*
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.format.highlighter;

import android.text.style.ParagraphStyle;

import java.util.regex.Matcher;

public interface SpanCreatorP {
    ParagraphStyle create(Matcher matcher, int iM);
}
