/*#######################################################
 *
 *   Maintained 2018-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.todotxt;

import android.content.Context;

import androidx.core.text.TextUtilsCompat;

import net.gsantner.markor.format.TextConverterBase;

import java.io.File;
import java.util.regex.Pattern;

@SuppressWarnings("WeakerAccess")
public class TodoTxtTextConverter extends TextConverterBase {

    private static final String HTML100_BODY_PRE_BEGIN = "<pre style='white-space: pre-wrap;font-family: " + TOKEN_FONT + "' >";
    private static final String HTML101_BODY_PRE_END = "</pre>";
    public static final Pattern TODOTXT_FILE_PATTERN = Pattern.compile("(?i)(^todo[-.]?.*)|(.*[-.]todo\\.((txt)|(text))$)");


    //########################
    //## Methods
    //########################

    @Override
    public String convertMarkup(String markup, Context context, boolean lightMode, boolean lineNum, File file) {
        String converted = "", onLoadJs = "", head = "";
        converted = HTML100_BODY_PRE_BEGIN
                + parse(TextUtilsCompat.htmlEncode(markup))
                + HTML101_BODY_PRE_END;
        return putContentIntoTemplate(context, converted, lightMode, file, onLoadJs, head);
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
    protected boolean isFileOutOfThisFormat(final File file, final String name, final String ext) {
        return name.equals("todo.txt") ||
                (TODOTXT_FILE_PATTERN.matcher(name).matches() && (name.endsWith(".txt") || name.endsWith(".text")));
    }
}
