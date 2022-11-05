/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.asciidoc;

import android.content.Context;

import net.gsantner.markor.format.TextConverterBase;
import net.gsantner.opoc.util.GsFileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class AsciidocTextConverter extends TextConverterBase {
    //########################
    //## Extensions
    //########################

    private static final List<String> EXT_ASCIIDOC = Arrays.asList(".adoc", ".asciidoc", ".asc");
    private static final List<String> EXT = new ArrayList<>();

    static {
        EXT.addAll(EXT_ASCIIDOC);
    }

    //########################
    //## Methods
    //########################

    @Override
    public String convertMarkup(String markup, Context context, boolean isExportInLightMode, File file) {
        String converted = "", onLoadJs = "", head = "";
        final String extWithDot = GsFileUtils.getFilenameExtension(file);
        String tmp;

        return putContentIntoTemplate(context, converted, isExportInLightMode, file, onLoadJs,
                head);
    }

    @Override
    protected String getContentType() {
        return CONTENT_TYPE_HTML;
    }

    @Override
    protected boolean isFileOutOfThisFormat(String filepath, String extWithDot) {
        return EXT.contains(extWithDot);
    }
}
