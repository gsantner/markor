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

import java.io.File;
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
        StringBuffer converted = new StringBuffer();
        int lineno = 0;
        Matcher matcher;
        for (String line : markup.split("\\n\\r?")) {
            boolean skipLine = false;
            switch (++lineno) {
                case 1:
                    skipLine = line.matches("^Content-Type: text/x-zim-wiki$");
                    break;
                case 2:
                    skipLine = line.matches("^Wiki-Format: zim \\d+\\.\\d+$");
                    break;
                case 3:
                    skipLine = line.matches("^Creation-Date: \\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+.:\\d+]*");
                    break;
                case 4:
                    skipLine = line.isEmpty();
                    break;
            }
            if (skipLine)
                continue;

            for (ZimWikiHighlighterPattern pattern : ZimWikiHighlighterPattern.values()) {
                String target;
                matcher = pattern.pattern.matcher(line);
                boolean found = false;
                while (matcher.find()) {
                    found = true;
                    switch (pattern) {
                        case EMPHASIS:
                            matcher.appendReplacement(converted, //
                                    matcher.group().replaceAll("^/+|/+$", "*"));
                            break;
                        case LINK:
                            String[] sides = matcher.group() //
                                    .replaceAll("^\\[+", "") //
                                    .replaceAll("\\]+$", "") //
                                    .split("\\|");
                            matcher.appendReplacement(converted, //
                                    String.format("[%s](%s)", sides[1], sides[0]));
                            break;
                        case LINKTOP:
                            target = matcher.group() //
                                    .replaceAll("^\\[+", "") //
                                    .replaceAll("\\]+$", "");
                            matcher.appendReplacement(converted, //
                                    String.format("[%s](file://%s/%s)", //
                                            target, //
                                            file.getParentFile().getAbsoluteFile(), //
                                            target));
                            break;
                        case LINKSUB:
                            target = matcher.group() //
                                    .replaceAll("^\\[+", "") //
                                    .replaceAll("\\]+$", "");
                            matcher.appendReplacement(converted, //
                                    String.format("[%s](file://%s)", target, target));
                        case LIST_ORDERED:
                            matcher.appendReplacement(converted, //
                                    matcher.group().replaceAll("[0-9a-zA-Z]+\\.", "1."));
                            break;
                        case HEADING:
                            // Header level 1 has 6 equal signs (=)x6; while MD's top level is one hash (#)
                            int markdownLevel = 0;
                            while (matcher.group().charAt(markdownLevel) == '=')
                                markdownLevel++;

                            // Maximum header level is 5, and has two equal signs
                            markdownLevel = 7 - (markdownLevel >= 6 ? 6 : markdownLevel);

                            String hashes = " ";
                            for (int iHash = 0; iHash < markdownLevel; iHash++)
                                hashes = "#" + hashes;

                            matcher.appendReplacement(converted, //
                                    hashes + matcher.group().replaceAll("^=+\\s*|\\s*=+$", ""));
                            break;
                        case LIST_CHECK:
                            matcher.appendReplacement(converted, "- " + matcher.group());
                            break;
                        case VERBATIM:
                            matcher.appendReplacement(converted, "`" + matcher.group() + "`");
                            break;
                        case LIST_UNORDERED:
                        case STRIKE:
                        case STRONG:
                        default:
                            line = line;
                            break;
                    }
                    if (found)
                        matcher.appendTail(converted);
                }
                converted.append('\n');
            }
        }

        return converter.convertMarkup(converted.toString(), context, isExportInLightMode, file);
    }

    /**
     * @param filepath of a file
     * @return true if the file extension is .txt; false otherwise
     */
    @Override
    public boolean isFileOutOfThisFormat(String filepath) {
        return filepath.matches("(?i)^.+\\.txt$");
    }
}
