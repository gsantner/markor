package net.gsantner.markor.format.orgmode;

import android.content.Context;

import net.gsantner.markor.format.TextConverterBase;
import net.gsantner.opoc.format.GsTextUtils;

import java.io.File;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class OrgmodeTextConverter extends TextConverterBase {
    private static final List<String> EXT = Collections.singletonList(".org");

    /**
     * this file is exported by browserify from  <a href="https://github.com/mooz/org-js">org-js</a>
     */
    public static final String HTML_ORG_JS_INCLUDE = "<script src='file:///android_asset/orgmode/org-bundle.js'></script>\n";
    public static final String HTML_ORG_CSS_INCLUDE = "<link href='file:///android_asset/orgmode/org.css' type='text/css' rel='stylesheet'/>\n";

    //########################
    //## Methods
    //########################

    @Override
    public String convertMarkup(String markup, Context context, boolean lightMode, boolean lineNum, File file) {

        String converted = "<div id=\"orgmode_content\"></div>\n";
        String onLoadJs =
                "var textBase64 = `" +
                        //convert a text to base64 to simplify supporting special characters
                        GsTextUtils.toBase64(markup) +
                        "`;\n" +
                        //decode base64 to utf8 string
                        "const asciiPlainText = atob(textBase64);\n" +
                        "const length = asciiPlainText.length;\n" +
                        "const bytes = new Uint8Array(length);\n" +
                        "for (let i = 0; i < length; i++) {\n" +
                        "    bytes[i] = asciiPlainText.charCodeAt(i);\n" +
                        "}\n" +
                        "const decoder = new TextDecoder();\n" +
                        "var utf8PlainText = decoder.decode(bytes);" +
                        "var parser = new org.Parser();\n" +
                        "var orgDocument = parser.parse(utf8PlainText);\n" +
                        "var orgHTMLDocument = orgDocument.convert(org.ConverterHTML, {});" +
                        "document.getElementById(\"orgmode_content\").innerHTML = orgHTMLDocument;" +
                        "";
        return putContentIntoTemplate(context, converted, lightMode, file, onLoadJs, HTML_ORG_JS_INCLUDE + HTML_ORG_CSS_INCLUDE);
    }

    @Override
    protected String getContentType() {
        return CONTENT_TYPE_HTML;
    }


    @Override
    protected boolean isFileOutOfThisFormat(final File file, final String name, final String ext) {
        return EXT.contains(ext);
    }
}
