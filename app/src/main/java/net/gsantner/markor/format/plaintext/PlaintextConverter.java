/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.plaintext;

import android.content.Context;
import android.support.v4.text.TextUtilsCompat;

import net.gsantner.markor.format.TextConverter;
import net.gsantner.markor.util.AppSettings;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class PlaintextConverter extends TextConverter {
    private static final String HTML100_BODY_PRE_BEGIN = "<pre style='white-space: pre-wrap;font-family: " + TOKEN_FONT + "' >";
    private static final String HTML101_BODY_PRE_END = "</pre>";
    private static final List<String> EXT = Arrays.asList(".txt", ".taskpaper", ".html");


    //########################
    //## Methods
    //########################

    @Override
    public String convertMarkup(String markup, Context context, boolean isExportInLightMode, File file) {
        String converted = "", onLoadJs = "", head = "";
        if (file != null && file.getName().toLowerCase().endsWith(".html")) {
            converted += markup;
        } else {
            converted = HTML100_BODY_PRE_BEGIN
                    + TextUtilsCompat.htmlEncode(markup)
                    + HTML101_BODY_PRE_END;
        }
        return putContentIntoTemplate(context, converted, isExportInLightMode, file, onLoadJs, head);
    }

    @Override
    protected String getContentType() {
        return CONTENT_TYPE_HTML;
    }

    @Override
    public boolean isFileOutOfThisFormat(String filepath) {
        AppSettings appSettings = AppSettings.get();
        if (!filepath.contains(".")) {
            return appSettings.isExtOpenWithThisApp("");
        }
        String ext = filepath.substring(filepath.lastIndexOf("."));
        if (appSettings.isExtOpenWithThisApp(ext)) {
            return true;
        }
        return EXT.contains(ext);
    }
}
