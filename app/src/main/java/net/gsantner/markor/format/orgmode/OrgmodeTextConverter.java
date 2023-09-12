
package net.gsantner.markor.format.orgmode;

import android.content.Context;

import net.gsantner.markor.format.TextConverterBase;
import net.gsantner.opoc.util.GsFileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.core.text.TextUtilsCompat;

@SuppressWarnings("WeakerAccess")
public class OrgmodeTextConverter extends TextConverterBase {
    private static final String HTML100_BODY_PRE_BEGIN = "<pre style='white-space: pre-wrap;font-family: " + TOKEN_FONT + "' >";
    private static final String HTML101_BODY_PRE_END = "</pre>";
    private static final List<String> EXT_ORG = Arrays.asList(".org");
    private static final List<String> EXT = new ArrayList<>();

    static {
        EXT.addAll(EXT_ORG);
    }

    //########################
    //## Methods
    //########################

    @Override
    public String convertMarkup(String markup, Context context, boolean lightMode, boolean lineNum, File file) {
        String converted = "", onLoadJs = "", head = "";
        final String extWithDot = GsFileUtils.getFilenameExtension(file);

        ///////////////////////////////////////////
        // Convert
        ///////////////////////////////////////////
        converted = HTML100_BODY_PRE_BEGIN
                + TextUtilsCompat.htmlEncode(markup)
                + HTML101_BODY_PRE_END;
        return putContentIntoTemplate(context, converted, lightMode, file, onLoadJs, head);
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
