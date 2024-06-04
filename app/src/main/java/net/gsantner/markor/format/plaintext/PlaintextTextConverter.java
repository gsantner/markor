/*#######################################################
 *
 *   Maintained 2018-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.plaintext;

import android.content.Context;

import androidx.core.text.TextUtilsCompat;

import net.gsantner.markor.format.FormatRegistry;
import net.gsantner.markor.format.TextConverterBase;
import net.gsantner.markor.format.binary.EmbedBinaryTextConverter;
import net.gsantner.markor.format.keyvalue.KeyValueTextConverter;
import net.gsantner.opoc.format.GsTextUtils;
import net.gsantner.opoc.util.GsFileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("WeakerAccess")
public class PlaintextTextConverter extends TextConverterBase {
    private static final String HTML100_BODY_PRE_BEGIN = "<pre style='white-space: pre-wrap;font-family: " + TOKEN_FONT + "' >";
    private static final String HTML101_BODY_PRE_END = "</pre>";
    private static final List<String> EXT_TEXT = Arrays.asList(".txt", ".taskpaper", ".org", ".ldg", ".ledger", ".m3u", ".m3u8");
    private static final List<String> EXT_HTML = Arrays.asList(".html", ".htm");
    private static final List<String> EXT_CODE_HL = Arrays.asList(".py", ".cpp", ".h", ".c", ".js", ".mjs", ".css", ".cs", ".kt", ".lua", ".perl", ".java", ".qml", ".diff", ".php", ".r", ".patch", ".rs", ".swift", ".ts", ".mm", ".go", ".sh", ".rb", ".tex", ".xml", ".xlf");
    private static final List<String> EXT = new ArrayList<>();

    static {
        EXT.addAll(EXT_TEXT);
        EXT.addAll(EXT_HTML);
        EXT.addAll(EXT_CODE_HL);
    }

    //########################
    //## Methods
    //########################

    @Override
    public String convertMarkup(String markup, Context context, boolean lightMode, boolean lineNum, File file) {
        String converted = "", onLoadJs = "", head = "";
        final String extWithDot = GsFileUtils.getFilenameExtension(file);
        String tmp;

        ///////////////////////////////////////////
        // Refactor input
        ///////////////////////////////////////////
        // JSON: try to pretty-print
        if (extWithDot.equals(".json") && (tmp = GsTextUtils.jsonPrettyPrint(markup)) != null) {
            markup = tmp;
        }

        ///////////////////////////////////////////
        // Convert
        ///////////////////////////////////////////
        if (EXT_HTML.contains(extWithDot)) {
            // HTML: Display it
            converted += markup;
        } else if (extWithDot.matches(EmbedBinaryTextConverter.EXT_MATCHES_M3U_PLAYLIST)) {
            // Playlist: Load in Embed-Binary view-mode
            return FormatRegistry.CONVERTER_EMBEDBINARY.convertMarkup(markup, context, lightMode, lineNum, file);
        } else if (EXT_CODE_HL.contains(extWithDot) || (this instanceof KeyValueTextConverter)) {
            // Source code: Load in Markdown view-mode & utilize code block highlighting
            final String hlLang = extWithDot.replace(".sh", ".bash").replace(".", "");
            markup = String.format(Locale.ROOT, "```%s\n%s\n```", hlLang, markup);
            return FormatRegistry.CONVERTER_MARKDOWN.convertMarkup(markup, context, lightMode, lineNum, file);
        } else {
            ///////////////////////////////////////////
            // Whatever else show in plaintext <pre> block
            converted = HTML100_BODY_PRE_BEGIN
                    + TextUtilsCompat.htmlEncode(markup)
                    + HTML101_BODY_PRE_END;
        }
        return putContentIntoTemplate(context, converted, lightMode, file, onLoadJs, head);
    }

    @Override
    protected String getContentType() {
        return CONTENT_TYPE_HTML;
    }

    @Override
    protected boolean isFileOutOfThisFormat(final File file, final String name, final String ext) {
        return EXT.contains(ext) || _appSettings.isExtOpenWithThisApp(ext) || GsFileUtils.isTextFile(file);
    }
}
