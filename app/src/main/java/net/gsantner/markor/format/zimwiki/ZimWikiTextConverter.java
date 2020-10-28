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

import android.arch.core.util.Function;
import android.content.Context;

import net.gsantner.markor.format.TextConverter;
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
public class ZimWikiTextConverter extends TextConverter {

    private Context _context;
    private File _file;
    private String _currentLine;

    private static MarkdownTextConverter converter;
    private StringBuffer _convertedLine;

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

        private Pattern _pattern;

        ZimWikiPatterns(Pattern pattern) {
            _pattern = pattern;
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
        _context = context;
        _file = file;
        StringBuilder result = new StringBuilder();
        int lineNr = 0;
        for (String line : markup.split("\\n\\r?")) {
            if (!checkHeader(++lineNr, line))
                continue;
            String markdownEquivalentLine = getMarkdownEquivalentLine(line);
            result.append(markdownEquivalentLine);
            result.append(String.format("%n"));
        }

        return converter.convertMarkup(result.toString(), context, isExportInLightMode, file);
    }

    private String getMarkdownEquivalentLine(String zimWikiLine) {
        _currentLine = zimWikiLine;
        replaceAllMatchesInLinePartially(ZimWikiPatterns.EMPHASIS._pattern, "^/+|/+$", "*");
        replaceAllMatchesInLinePartially(ZimWikiPatterns.LIST_ORDERED._pattern, "[0-9a-zA-Z]+\\.", "1.");
        replaceAllMatchesInLine(ZimWikiPatterns.HEADING._pattern, this::convertHeading);
        replaceAllMatchesInLine(ZimWikiPatterns.LINK._pattern, fullMatch -> convertLink(fullMatch, _context, _file));
        replaceAllMatchesInLine(ZimWikiPatterns.LIST_CHECK._pattern, fullMatch -> "- "+fullMatch);
        replaceAllMatchesInLine(ZimWikiPatterns.VERBATIM._pattern, fullMatch -> "`" + fullMatch + "`");
        replaceAllMatchesInLine(ZimWikiPatterns.SUPERSCRIPT._pattern, fullMatch -> String.format("<sup>%s</sup>",
                fullMatch.replaceAll("^\\^\\{|\\}$", "")));
        replaceAllMatchesInLine(ZimWikiPatterns.SUBSCRIPT._pattern, fullMatch -> String.format("<sub>%s</sub>",
                fullMatch.replaceAll("^_\\{|\\}$", "")));
        return _currentLine;
    }

    private void replaceAllMatchesInLinePartially(Pattern zimPattern, String matchPartToBeReplaced, String replacementForMatchPart) {
        replaceAllMatchesInLine(zimPattern, fullMatch -> fullMatch.replaceAll(matchPartToBeReplaced, replacementForMatchPart));
    }

    private void replaceAllMatchesInLine(Pattern zimPattern, Function<String, String> replaceMatchWithMarkdown) {
        Matcher matcher = zimPattern.matcher(_currentLine);
        StringBuffer replacedLine = new StringBuffer();
        while (matcher.find()) {
            String fullMatch = matcher.group();
            matcher.appendReplacement(replacedLine, replaceMatchWithMarkdown.apply(fullMatch));
        }
        matcher.appendTail(replacedLine);
        _currentLine = replacedLine.toString();
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
            case 1: {
                if (!line.matches("^Content-Type: text/x-zim-wiki$"))
                    return false;
                break;
            }
            case 2: {
                if (!line.matches("^Wiki-Format: zim \\d+\\.\\d+$"))
                    return false;
                break;
            }
            case 3: {
                if (!line.matches("^Creation-Date: \\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[.+:\\d]+$"))
                    return false;
                break;
            }
            case 4: {
                if (!line.isEmpty())
                    return false;
                break;
            }
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
        if (!filepath.matches("(?i)^.+\\.txt$")) {
            return false;
        }

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
            if (reader != null) {
                reader.close();
            }
        } catch (IOException e) {
            return false;
        }
        return result;
    }
}
