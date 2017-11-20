/*
 * Copyright (c) 2014 Jeff Martin
 * Copyright (c) 2015 Pedro Lafuente
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.format.highlighter;

import android.text.Editable;
import android.text.InputFilter;

import net.gsantner.markor.format.highlighter.markdown.MarkdownHighlighter;

public abstract class Highlighter {

    protected abstract Editable run(Editable e);

    public abstract InputFilter getAutoFormatter();

    protected static Highlighter getDefaultHighlighter(){
        return new MarkdownHighlighter();
    }

}
