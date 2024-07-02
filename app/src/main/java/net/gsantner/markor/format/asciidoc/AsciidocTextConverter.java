/*#######################################################
 *
 *   Maintained 2018-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.asciidoc;

import android.content.Context;

import net.gsantner.markor.format.TextConverterBase;
import net.gsantner.opoc.format.GsTextUtils;
import net.gsantner.opoc.util.GsContextUtils;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("WeakerAccess")
public class AsciidocTextConverter extends TextConverterBase {
    //########################
    //## Extensions
    //########################
    private static final Set<String> EXT = new HashSet<>(Arrays.asList(".adoc", ".asciidoc", ".asc"));
    public static final String HTML_ASCIIDOCJS_JS_INCLUDE = "<script src='file:///android_asset/asciidoc/asciidoctor.min.js'></script>";
    public static final String HTML_ASCIIDOCJS_DEFAULT_CSS_INCLUDE = "file:///android_asset/asciidoc/asciidoctor.css";
    /**
     * that file was loaded from <a href="https://github.com/darshandsoni/asciidoctor-skins/blob/gh-pages/css/dark.css">dark.css</a>
     * "import" block was changed to load local css
     * "literalblock" block was changes to support new rules
     */
    public static final String HTML_ASCIIDOCJS_DARK_CSS_INCLUDE = "file:///android_asset/asciidoc/dark.css";

    @Override
    public String convertMarkup(String markup, Context context, boolean lightMode, boolean lineNum, File file) {
        String converted = "<div id=\"asciidoc_content\"></div>\n";
        String onLoadJs = "var textBase64 = `" +
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
                "var asciidoctor = Asciidoctor();\n" +
                //standalone : true - to generate header 1 (= title) in the page. if don't do that - title will be absent.
                //nofooter: true - to don't generate footer (Last updated ...). if don't do that and use standalone : true - the page will have that footer.
                "var html = asciidoctor.convert(utf8PlainText, {standalone : true, attributes : {nofooter: true, stylesheet: \"" +
                (!lightMode && GsContextUtils.instance.isDarkModeEnabled(context) ? HTML_ASCIIDOCJS_DARK_CSS_INCLUDE : HTML_ASCIIDOCJS_DEFAULT_CSS_INCLUDE)
                + "\"}});\n" +
                "document.getElementById(\"asciidoc_content\").innerHTML = html;";
        return putContentIntoTemplate(context, converted, lightMode, file, onLoadJs, HTML_ASCIIDOCJS_JS_INCLUDE);
    }

    @Override
    protected boolean isFileOutOfThisFormat(final File file, final String name, final String ext) {
        return EXT.contains(ext);
    }
}
