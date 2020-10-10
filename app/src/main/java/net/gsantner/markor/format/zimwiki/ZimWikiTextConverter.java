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
        StringBuffer result = new StringBuffer();
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
                    StringBuffer converted = new StringBuffer();
                    found = true;
                    switch (pattern) {
                        case EMPHASIS:
                            matcher.appendReplacement(converted,
                                    matcher.group().replaceAll("^/+|/+$", "*"));
                            break;
                        case LINK:
                            matcher.appendReplacement(converted,
                                    convertLink(matcher.group(), context, file));
                            break;
                        case LIST_ORDERED:
                            matcher.appendReplacement(converted,
                                    matcher.group().replaceAll("[0-9a-zA-Z]+\\.", "1."));
                            break;
                        case HEADING:
                            matcher.appendReplacement(converted, convertHeading(matcher.group()));
                            break;
                        case LIST_CHECK:
                            matcher.appendReplacement(converted, "- " + matcher.group());
                            break;
                        case VERBATIM:
                            matcher.appendReplacement(converted, "`" + matcher.group() + "`");
                            break;
                        case SUBSCRIPT:
                            matcher.appendReplacement(converted,
                                    String.format("<sub>%s</sub>",
                                            matcher.group().replaceAll("^_\\{|\\}$", "")));
                            break;
                        case SUPERSCRIPT:
                            matcher.appendReplacement(converted,
                                    String.format("<sup>%s</sup>",
                                            matcher.group().replaceAll("^\\^\\{|\\}$", "")));
                            break;
                        case LIST_UNORDERED:
                        case STRIKE:
                        case STRONG:
                        default:
                            break;
                    }
                    if (found) {
                        matcher.appendTail(converted);
                        line = converted.toString();
                    }
                }
            }
            result.append(String.format("%s%n", line));
        }

        return converter.convertMarkup(result.toString(), context, isExportInLightMode, file);
    }

    /**
     *
     */
    private String convertHeading(String group) {
        // Header level 1 has 6 equal signs (=)x6; while MD's top level is one hash (#)
        int markdownLevel = 0;
        while (group.charAt(markdownLevel) == '=')
            markdownLevel++;

        // Maximum header level is 5, and has two equal signs
        markdownLevel = 7 - (markdownLevel >= 6 ? 6 : markdownLevel);

        String hashes = " ";
        for (int iHash = 0; iHash < markdownLevel; iHash++)
            hashes = "#" + hashes;

        return hashes + group.replaceAll("^=+\\s*|\\s*=+$", "");
    }

    /**
     *
     */
    private String convertLink(String group, Context context, File file) {
        String[] pair = group //
                .replaceAll("^\\[+", "") //
                .replaceAll("\\]+$", "") //
                .split("\\|");

        String fullPath = "";
        if (pair[0].charAt(0) == '+') {
            fullPath = "file://" //
                    + context.getFilesDir().getAbsolutePath() //
                    + File.separator //
                    + pair[0].substring(1) //
                    + ".txt";
        } else if (pair[0].matches("^[a-z]://.+$")) {
            fullPath = pair[0];
        } else {
            fullPath = "file://";
            if (pair[0].charAt(0) == ':')
                fullPath += context.getFilesDir().getAbsolutePath();
            else
                fullPath += file.getParentFile().getAbsolutePath();
            for (String token : pair[0].split(":")) {
                fullPath += File.separator + token;
            }
            fullPath += ".txt";
            fullPath = fullPath.replaceAll(" ", "%20"); // TODO proper URL encoding
        }
        return String.format("[%s](%s)", pair[pair.length - 1], fullPath);
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
