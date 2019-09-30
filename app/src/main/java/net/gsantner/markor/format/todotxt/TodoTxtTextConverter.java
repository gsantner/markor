/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.todotxt;

import android.content.Context;
import android.support.v4.text.TextUtilsCompat;

import net.gsantner.markor.format.TextConverter;
import net.gsantner.opoc.format.todotxt.SttCommander;

import java.io.File;

@SuppressWarnings("WeakerAccess")
public class TodoTxtTextConverter extends TextConverter {

    private static final String HTML100_BODY_PRE_BEGIN = "<pre style='white-space: pre-wrap;font-family: " + TOKEN_FONT + "' >";
    private static final String HTML101_BODY_PRE_END = "</pre>";


    //########################
    //## Methods
    //########################

    @Override
    public String convertMarkup(String markup, Context context, boolean isExportInLightMode, File file) {
        String converted = "", onLoadJs = "", head = "";
        converted = HTML100_BODY_PRE_BEGIN
                + parse(TextUtilsCompat.htmlEncode(markup))
                + HTML101_BODY_PRE_END;
        return putContentIntoTemplate(context, converted, isExportInLightMode, file, onLoadJs, head);
    }

    private String parse(String str) {
        str = str.replace("\n", "</br><span style='margin-bottom=20px;'/><hr/><span style='margin-bottom=20px;'/>");
        return str;
    }

    @Override
    protected String getContentType() {
        return CONTENT_TYPE_HTML;
    }

    @Override
    public boolean isFileOutOfThisFormat(String filepath) {
        return SttCommander.isTodoFile(filepath);
    }
}
