/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.zimwiki;

import android.content.Context;

import net.gsantner.markor.format.markdown.MarkdownTextConverter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;

/**
 * Wrapper class around MarkdownTextConverter
 */
@SuppressWarnings("WeakerAccess")
public class ZimWikiTextConverter extends net.gsantner.markor.format.TextConverter {

    private static MarkdownTextConverter converter;

    public ZimWikiTextConverter(MarkdownTextConverter converter) {
        ZimWikiTextConverter.converter = converter;
    }

    /**
     * First, convert zim-wiki to regular Markor markdown. Then, calls the regular converter.
     *
     * @param markup              Markup text
     * @param context             Android Context
     * @param isExportInLightMode
     * @param file
     * @return HTML text
     */
    @Override
    public String convertMarkup(String markup, Context context, boolean isExportInLightMode, File file) {
        // TODO Why is this never called yet?
        StringBuffer converted = new StringBuffer();
        Matcher matcher;
        for (String line : markup.split("\\n\\r?")) {
            for (ZimWikiHighlighterPattern pattern : ZimWikiHighlighterPattern.values()) {
                matcher = pattern.pattern.matcher(line);
                while (matcher.find()) {
                    switch (pattern) {
                        case ITALICS:
                            matcher.appendReplacement(converted, //
                                    matcher.group().replaceAll("^//|//$", "*"));
                            break;
                        case LINK:
                            matcher.appendReplacement(converted, //
                                    matcher.group() //
                                            .replaceAll("^\\[\\[", "[") //
                                            .replaceAll("|", "](") //
                                            .replaceAll("\\]\\]$", ")"));
                            break;
                        case LINKTOP:
                            matcher.appendReplacement(converted,
                                    matcher.group() //
                                            .replaceAll("^\\[\\[", "[") //
                                            .replaceAll("\\]\\]$", "](") //
                                            + "file://" // TODO
                                            + ")");
                            break;
                        case LINKSUB:
                            matcher.appendReplacement(converted,
                                    matcher.group() //
                                            .replaceAll("^\\[\\[", "[") //
                                            .replaceAll("\\]\\]$", "](") //
                                            + "file://" // TODO
                                            + ")");
                        case LIST_ORDERED:
                            matcher.appendReplacement(converted, //
                                    matcher.group().replaceAll("[0-9a-zA-Z]+\\.", "1."));
                            break;
                        case HEADING:
                            // Header level 1 has 6 equal signs (=)x6; while MD's top level is one hash (#)
                            int markdownLevel = 5 - matcher.group().replaceAll("[^=]", "").length() / 2;

                            // Maximum header level is 5, and has two equal signs
                            markdownLevel = markdownLevel > 5 ? 5 : markdownLevel;

                            String hashes = " ";
                            for (int iHash = 0; iHash == markdownLevel; iHash++)
                                hashes = "#" + hashes;

                            matcher.appendReplacement(converted, //
                                    hashes + matcher.group().replaceAll("^=+\\s*|\\s*=+$", ""));
                            break;
                        case LIST_CHECK:
                            matcher.appendReplacement(converted, "- " + matcher.group());
                            break;
                        case QUOTATION: // TODO
                        case LIST_UNORDERED:
                        case STRIKETHROUGH:
                        case BOLD:
                        default:
                            line = line;
                            break;
                    }
                }
                matcher.appendTail(converted);
            }
            converted.append('\n');
        }

        return converter.convertMarkup(converted.toString(), context, isExportInLightMode, file);
    }

    /**
     * Checks the first three lines of a legit markdown file to contain typical Zim-Wiki header lines.
     *
     * @param filepath of a file
     * @return true if the file is a markdown file and the zim-wiki header is present, otherwise false.
     */
    @Override
    public boolean isFileOutOfThisFormat(String filepath) {
        if (!converter.isFileOutOfThisFormat(filepath))
            return false;

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(filepath));
            String line;
            int lineno = 0;
            while ((line = reader.readLine()) != null) {
                switch (++lineno) {
                    case 1:
                        if (!line.matches("^Content-Type: text/x-zim-wiki\\n\\r?$"))
                            return false;
                        break;
                    case 2:
                        if (!line.matches("^Wiki-Format: zim \\d+\\.\\d+\\n\\r?$"))
                            return false;
                        break;
                    case 3:
                        if (!line.matches("^Creation-Date: \\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d+\\n\\r?$]"))
                            return false;
                        break;
                    case 4:
                        if (!line.isEmpty())
                            return true;
                        break;
                }
            }
        } catch (IOException e) {
            // TODO no file
            return false;
        }
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                // TODO fails closing
            }
        }

        return true;
    }
}
