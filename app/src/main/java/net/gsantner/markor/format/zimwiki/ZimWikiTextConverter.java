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
import net.gsantner.opoc.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wrapper class around MarkdownTextConverter
 */
@SuppressWarnings("WeakerAccess")
public class ZimWikiTextConverter extends net.gsantner.markor.format.TextConverter {

    private static MarkdownTextConverter converter;

    private enum ZimWikiPatterns {
        HEADING(Pattern.compile("^(==+\\s+\\S.*?\\s*=*)$")),
        LINK(Pattern.compile("(\\[\\[(?!\\[)(.+?\\]*)\\]\\])")),
        IMAGE(Pattern.compile("(\\{\\{(?!\\{)(.*?)\\}\\})")),
        LIST_CHECK(Pattern.compile("^\t*(\\[[ xX*>]?]|\\([ xX*>]?\\)) ")),
        LIST_ORDERED(Pattern.compile("^\t*([\\d]+\\.|[a-zA-Z]+\\.) ")),
        LIST_UNORDERED(Pattern.compile("^\t*(\\*)(?= )")),
        EMPHASIS(Pattern.compile("(//(?!/)(.*?)(?<!:)//)")),
        STRONG(Pattern.compile("(\\*\\*(?!\\*)(.*?)\\*\\*)")),
        MARK(Pattern.compile("(__(?!_)(.*?)__)")),
        STRIKE(Pattern.compile("(~~(?!~)(.+?)~~)")),
        SUBSCRIPT(Pattern.compile("(_\\{(?!~)(.+?)\\})")),
        SUPERSCRIPT(Pattern.compile("(\\^\\{(?!~)(.+?)\\})")),
        VERBATIM(Pattern.compile("(''(?!').+?'')")),
        VERBATIM_BLOCK(Pattern.compile("(?m)('''(?!''')(.+?)''')"));
        // TODO Table

        private Pattern pattern;

        ZimWikiPatterns(Pattern pattern) {
            this.pattern = pattern;
        }
    }

    public ZimWikiTextConverter(MarkdownTextConverter converter) {
        ZimWikiTextConverter.converter = converter;
    }

    /**
     * First, convert zim-wiki to regular Markor markdown. Then, calls the regular converter.
     *
     * @param markup              Markup text
     * @param context             Android Context
     * @param isExportInLightMode True if the light theme is to apply.
     * @param file                The file to convert.
     * @return HTML text
     */
    @Override
    public String convertMarkup(String markup, Context context, boolean isExportInLightMode, File file) {
        StringBuilder result = new StringBuilder();
        int lineNr = 0;
        Matcher matcher;
        for (String line : markup.split("\\n\\r?")) {
            if (!checkHeader(++lineNr, line))
                continue;

            for (ZimWikiPatterns pattern : ZimWikiPatterns.values()) {
                matcher = pattern.pattern.matcher(line);
                while (matcher.find()) {
                    StringBuffer converted = new StringBuffer();
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
                    matcher.appendTail(converted);
                    line = converted.toString();
                }
            }
            result.append(String.format("%s%n", line));
        }

        return converter.convertMarkup(result.toString(), context, isExportInLightMode, file);
    }

    private String convertHeading(String group) {
        // Header level 1 has 6 equal signs (=)x6; while MD's top level is one hash (#)
        int markdownLevel = 0;
        while (group.charAt(markdownLevel) == '=')
            markdownLevel++;

        // Maximum header level is 5, and has two equal signs
        markdownLevel = 7 - Math.min(6, markdownLevel);

        return String.format("%s %s",
                StringUtils.repeatChars('#', markdownLevel),
                group.replaceAll("^=+\\s*|\\s*=+$", ""));
    }

    private String convertLink(String group, Context context, File file) {
        String[] pair = group //
                .replaceAll("^\\[+", "") //
                .replaceAll("]+$", "") //
                .split("\\|");

        StringBuilder fullPath = new StringBuilder();
        if (pair[0].charAt(0) == '+') {
            fullPath.append("file://");
            fullPath.append(context.getFilesDir().getAbsolutePath());
            fullPath.append(File.separator);
            fullPath.append(pair[0].substring(1));
            fullPath.append(".txt");
        } else if (pair[0].matches("^[a-z]://.+$")) {
            fullPath.append(pair[0]);
        } else {
            fullPath.append("file://");
            if (pair[0].charAt(0) == ':')
                fullPath.append(context.getFilesDir().getAbsolutePath());
            else
                fullPath.append(file.getParentFile().getAbsolutePath());
            for (String token : pair[0].split(":")) {
                fullPath.append(File.separator);
                fullPath.append(token);
            }
            fullPath.append(".txt");
        }
        // TODO proper URL encoding
        return String.format("[%s](%s)", pair[pair.length - 1], fullPath.toString().replaceAll(" ", "%20"));
    }

    /**
     * @param lineNr The line number. First line has number 0.
     * @param line   A line.
     * @return True iff given line number and line of Zim-Wiki header is valid.
     */
    private boolean checkHeader(int lineNr, String line) {
        switch (++lineNr) {
            case 1:
                if (!line.matches("^Content-Type: text/x-zim-wiki$"))
                    return false;
                break;
            case 2:
                if (!line.matches("^Wiki-Format: zim \\d+\\.\\d+$"))
                    return false;
                break;
            case 3:
                if (!line.matches("^Creation-Date: \\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[.+:\\d]+$"))
                    return false;
                break;
            case 4:
                if (!line.isEmpty())
                    return false;
                break;
        }
        return true;
    }

    /**
     * NOTE: This method only works if the full file path is specified.
     * @param filepath of a file
     * @return true if the file extension is .txt; false otherwise
     */
    @Override
    public boolean isFileOutOfThisFormat(String filepath) {
        if (!filepath.matches("(?i)^.+\\.txt$"))
            return false;

        boolean result = true;
        File file = new File(filepath);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            int lineno = 0;
            while ((line = reader.readLine()) != null && ++lineno < 4) {
                result &= checkHeader(lineno, line);
            }
        } catch (IOException e) {
            // dunno
        }
        try {
            if (reader != null)
                reader.close();
        } catch (IOException e) {
            return false;
        }
        return result;
    }
}
