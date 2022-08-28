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

import androidx.core.text.TextUtilsCompat;

import net.gsantner.markor.format.FormatRegistry;
import net.gsantner.markor.format.TextConverterBase;
import net.gsantner.markor.format.binary.EmbedBinaryTextConverter;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.opoc.util.GsFileUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class PlaintextTextConverter extends TextConverterBase {
    private static final String HTML100_BODY_PRE_BEGIN = "<pre style='white-space: pre-wrap;font-family: " + TOKEN_FONT + "' >";
    private static final String HTML101_BODY_PRE_END = "</pre>";
    private static final List<String> EXT = Arrays.asList(".txt", ".taskpaper", ".html", ".htm", ".adoc", ".org", ".ldg", ".ledger", ".diff", ".patch", ".m3u", ".m3u8");

    //########################
    //## Methods
    //########################

    @Override
    public String convertMarkup(String markup, Context context, boolean isExportInLightMode, File file) {
        String converted = "", onLoadJs = "", head = "";
        final String extWithDot = GsFileUtils.getFilenameExtension(file);

        if (extWithDot.startsWith(".htm")) {
            converted += markup;
        } else if (extWithDot.matches(EmbedBinaryTextConverter.EXT_MATCHES_M3U_PLAYLIST)) {
            return FormatRegistry.CONVERTER_EMBEDBINARY.convertMarkup(markup, context, isExportInLightMode, file);
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
    protected boolean isFileOutOfThisFormat(String filepath, String extWithDot) {
        AppSettings appSettings = AppSettings.get();
        return EXT.contains(extWithDot) || appSettings.isExtOpenWithThisApp(extWithDot);
    }
}
