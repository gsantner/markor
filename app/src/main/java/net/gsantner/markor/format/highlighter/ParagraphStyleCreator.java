/*
 * Copyright (c) 2017-2018 Gregor Santner
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.format.highlighter;

import android.text.style.ParagraphStyle;

import java.util.regex.Matcher;

public interface ParagraphStyleCreator {
    ParagraphStyle create(Matcher matcher, int iM);
}
