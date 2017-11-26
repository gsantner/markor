/*
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.format.highlighter.plain;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;

import net.gsantner.markor.format.highlighter.Highlighter;
import net.gsantner.markor.format.highlighter.HighlightingEditor;

public class PlainHighlighter extends Highlighter {

    public PlainHighlighter() {
    }

    @Override
    protected Editable run(final HighlightingEditor editor, final Editable editable) {
        try {
            clearSpans(editable);
        } catch (Exception ex) {
            // Ignoring errors
        }
        return editable;
    }

    @Override
    public InputFilter getAutoFormatter() {
        return AUTOFORMATTER_NONE;
    }

    @Override
    public int getHighlightingDelay(Context context) {
        return 99999;
    }
}

