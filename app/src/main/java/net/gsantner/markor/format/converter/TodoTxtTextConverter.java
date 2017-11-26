/*
 * Copyright (c) 2014 Jeff Martin
 * Copyright (c) 2015 Pedro Lafuente
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.format.converter;

import android.content.Context;
import android.support.v4.text.TextUtilsCompat;

import net.gsantner.markor.util.AppSettings;

import java.util.regex.Pattern;

@SuppressWarnings("WeakerAccess")
public class TodoTxtTextConverter extends TextConverter {
    public static final Pattern TODOTXT_FILE_PATTERN = Pattern.compile("(?i)(^todo[-.]?.*)|(.*[-.]todo\\.((txt)|(md))$)");

    private static final String HTML100_BODY_PRE_BEGIN = "<pre style='white-space: pre-wrap;font-family: %FONT%' >";
    private static final String HTML101_BODY_PRE_END = "</pre>";


    //########################
    //## Methods
    //########################

    @Override
    public String convertMarkup(String markup, Context context) {
        AppSettings as = new AppSettings(context);
        String html = HTML100_BODY_PRE_BEGIN.replace("%FONT%", as.getFontFamily())
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
