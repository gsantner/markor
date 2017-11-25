/*
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.format.highlighter.plain;

import android.text.Editable;
import android.text.InputFilter;

import net.gsantner.markor.format.highlighter.Highlighter;

public class PlainHighlighter extends Highlighter {

    public PlainHighlighter() {
    }

    @Override
    protected Editable run(Editable e) {
        try {
            clearSpans(e);
        } catch (Exception ex) {
            // Ignoring errors
        }
        return e;
    }

    @Override
    public InputFilter getAutoFormatter() {
        return AUTOFORMATTER_NONE;
    }
}

