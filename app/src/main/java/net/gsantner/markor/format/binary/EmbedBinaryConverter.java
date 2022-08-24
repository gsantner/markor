/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.binary;

import android.content.Context;
import android.util.Pair;

import net.gsantner.markor.R;
import net.gsantner.markor.format.TextConverter;
import net.gsantner.markor.format.markdown.MarkdownTextConverter;
import net.gsantner.opoc.util.FileUtils;
import net.gsantner.opoc.util.ShareUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("WeakerAccess")
public class EmbedBinaryConverter extends TextConverter {
    private static final List<String> EXT = new ArrayList<>();
    private static final List<String> EXT_AUDIO = Arrays.asList(".mp3", ".ogg", ".flac", ".opus", ".oga", ".wma", ".m4a", ".aac", ".wav", ".amr", ".mid", ".midi", ".pcm");
    private static final List<String> EXT_IMAGE = Arrays.asList(".jpg", ".jpeg", ".png", ".bmp", ".gif", ".webp", ".svg", ".heic", ".heif");
    private static final List<String> EXT_VIDEO = Arrays.asList(".webm", ".mp4", ".mpeg4", ".mpeg", ".mpg", ".mkv", ".3gp", ".ts");

    private static final String HTML100_BODY_BEGIN = "<div>\n  ";
    private static final String HTML101_BODY_END = "\n\n</div>";
    private static final String CSS_EMBED_STYLE = CSS_S + "html,body{padding: 0px; margin:0px;}" + CSS_E;
    private static final String CSS_EMBED_TABLE_LIMITS = CSS_S + "table {word-break: break-word;} thead tr th:first-child, tbody tr td:first-child {word-break:keep-all; min-width: 100px;} thead {display:none;}" + CSS_E;

    public static final String JS_CALLBACK_TYPE_OPEN_CURRENT_FILE_IN_EXTERNAL_APP = "openCurrentFileInExternalApp";

    static {
        EXT.addAll(EXT_IMAGE);
        EXT.addAll(EXT_VIDEO);
        EXT.addAll(EXT_AUDIO);
    }


    //########################
    //## Methods
    //########################

    @Override
    public String convertMarkup(String markup, Context context, boolean isExportInLightMode, File file) {
        String converted = "", onLoadJs = "", head = "";
        if (file == null) {
            return "";
        }
        converted = HTML100_BODY_BEGIN;
        head = CSS_EMBED_STYLE + CSS_EMBED_TABLE_LIMITS;
        final String extWithDot = FileUtils.getFilenameExtension(file);
        final ShareUtil shu = new ShareUtil(context);

        // Usage of binary file depending on file extension / MIME
        if (EXT_IMAGE.contains(extWithDot)) {
            converted += "<img class='rotatable' src='" + TOKEN_FILEURI_VIEWED_FILE + "' alt='Your Android device does not support the file format.'/>";
        } else if (EXT_VIDEO.contains(extWithDot)) {
            converted += "<video class='htmlav' autoplay controls loop style='max-height: 85vh; width: 100%; max-width: 100%;' src='" + TOKEN_FILEURI_VIEWED_FILE + "'/>Your Android device does not support the video tag or the file format.</video>";
        } else if (EXT_AUDIO.contains(extWithDot)) {
            converted += " <audio class='htmlav' title='" + file.getName() + "' autoplay controls loop style='width: 100%;'><source src='" + TOKEN_FILEURI_VIEWED_FILE + "'>Your Android device does not support the audio tag or the file format.</audio>";
        }

        // div width side margins
        converted += "\n\n<br/><div style='margin: 16px; margin-top: 0px;'><br/>\n";

        if (converted.contains("htmlav")) {
            converted += "<button type='button' class='fa' onclick=\"javascript:document.avSeek(-9e9);\"/>⏮️️</button>";
            converted += "<button type='button' class='fa' onclick=\"javascript:document.avSeek(-30);\"/>⏪</button>";
            converted += "<button type='button' class='fa' onclick=\"javascript:document.avPause();\"/>⏯️</button>";
            converted += "<button type='button' class='fa' onclick=\"javascript:document.avSeek(-30);\"/>⏩</button>";
            converted += "<button type='button' class='fa' onclick=\"javascript:document.avSeek(+9e9);\"/>⏭️</button>";
            converted += "<button type='button' class='fa' onclick=\"javascript:document.avLoopToggle();\"/>&#128257;</button>";
            onLoadJs += "document.avSeek       = function(delta) { var o=document.getElementsByClassName('htmlav')[0]; o.currentTime +=delta; o.play(); };";
            onLoadJs += "document.avLoopToggle = function()      { var o=document.getElementsByClassName('htmlav')[0]; o.loop = !o.loop; };";
            onLoadJs += "document.avPause      = function()      { var o=document.getElementsByClassName('htmlav')[0]; if(o.paused){o.play();} else{o.pause();}; };";
        }
        if (converted.contains("rotatable")) {
            converted += "<button type='button' class='fa' onclick=\"javascript:document.rotate();\"/>&#128260️</button>";
            onLoadJs += "document.rotation = 0;";
            onLoadJs += "document.rotate       = function()      { var o=document.getElementsByClassName('rotatable')[0]; document.rotation+=90; o.style.transform = 'rotate(xdeg)'.replace('x', document.rotation); };";
        }

        // Add file info table below content
        StringBuilder table = new StringBuilder("");
        table.append(String.format("%s | %s\n-----|-----\n", context.getString(R.string.type), context.getString(R.string.info)));
        for (Pair<String, String> metaPair : shu.extractFileMetadata(context, file, true)) {
            table.append(String.format("%s | %s\n", metaPair.first.replace("|", "/"), metaPair.second.replace("|", "/")));
        }
        converted += MarkdownTextConverter.flexmarkRenderer.render(MarkdownTextConverter.flexmarkParser.parse(table.toString()));

        // end div with side margins
        converted += "</div>";

        converted += HTML101_BODY_END;
        shu.setContext(null);
        return putContentIntoTemplate(context, converted, isExportInLightMode, file, onLoadJs, head);
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
