/*
 * Copyright (c) 2017-2018 Gregor Santner
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.format.converter;

import android.content.Context;
import android.support.v4.text.TextUtilsCompat;

import net.gsantner.markor.util.AppSettings;

@SuppressWarnings("WeakerAccess")
public class TodoTxtTextConverter extends TextConverter {

    private static final String HTML100_BODY_PRE_BEGIN = "<pre style='white-space: pre-wrap;font-family: " + TOKEN_FONT + "' >";
    private static final String HTML101_BODY_PRE_END = "</pre>";


    //########################
    //## Methods
    //########################

    @Override
    public String convertMarkup(String markup, Context context) {
        AppSettings as = new AppSettings(context);
        String html = HTML100_BODY_PRE_BEGIN
                + parse(TextUtilsCompat.htmlEncode(markup))
                + HTML101_BODY_PRE_END;
        return putContentIntoTemplate(context, html);
    }

    private String parse(String str) {
        str = str.replace("\n", "</br><span style='margin-bottom=20px;'/><hr/><span style='margin-bottom=20px;'/>");
        return str;
    }

    @Override
    protected String getContentType() {
        return CONTENT_TYPE_HTML;
    }
}
