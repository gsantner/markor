/*
 * Copyright (c) 2014 Jeff Martin
 * Copyright (c) 2015 Pedro Lafuente
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.format.converter;

import android.content.Context;

@SuppressWarnings("WeakerAccess")
public class PlainTextConverter extends TextConverter {
    //########################
    //## Methods
    //########################

    @Override
    public String markupToHtml(String markup, Context context) {
        return putContentIntoTemplate(context, markup);
    }
}
