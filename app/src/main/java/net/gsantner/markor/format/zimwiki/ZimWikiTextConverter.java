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
import net.gsantner.markor.format.TextFormat;
import net.gsantner.opoc.util.StringUtils;

import org.apache.commons.io.FilenameUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Make use of MarkdownConverter by converting Zim syntax to Markdown
 */
@SuppressWarnings("WeakerAccess")
public class ZimWikiTextConverter extends TextConverter {
    private static final Pattern LIST_ORDERED_LETTERS = Pattern.compile("^\t*([\\d]+\\.|[a-zA-Z]+\\.) ");

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
        String contentWithoutHeader = markup.replaceFirst(ZimWikiHighlighter.Patterns.ZIMHEADER.pattern.toString(), "");
        StringBuilder markdownContent = new StringBuilder();

        for (String line : contentWithoutHeader.split("\\r\\n|\\r|\\n")) {
            String markdownEquivalentLine = getMarkdownEquivalentLine(context, file, line, isExportInLightMode);
            markdownContent.append(markdownEquivalentLine);
            markdownContent.append("  "); // line breaks must be made explicit in markdown by two spaces
            markdownContent.append(String.format("%n"));
        }

        return TextFormat.CONVERTER_MARKDOWN.convertMarkup(markdownContent.toString(), context, isExportInLightMode, file);
    }

    private String getMarkdownEquivalentLine(final Context context, final File file, String zimWikiLine, final boolean isExportInLightMode) {
        final AtomicReference<String> currentLine = new AtomicReference<>(zimWikiLine);

        // Headings
        replaceAllMatchesInLine(currentLine, ZimWikiHighlighter.Patterns.HEADING.pattern, this::convertHeading);

        // bold syntax is the same as for markdown
        replaceAllMatchesInLinePartially(currentLine, ZimWikiHighlighter.Patterns.ITALICS.pattern, "^/+|/+$", "*");
        replaceAllMatchesInLine(currentLine, ZimWikiHighlighter.Patterns.HIGHLIGHTED.pattern, match -> convertHighlighted(match, isExportInLightMode));
        // strikethrough syntax is the same as for markdown

        replaceAllMatchesInLine(currentLine, ZimWikiHighlighter.Patterns.PREFORMATTED_INLINE.pattern, fullMatch -> "`$1`");
        replaceAllMatchesInLine(currentLine, Pattern.compile("^'''$"), fullMatch -> "```");  // preformatted multiline

        // unordered list syntax is compatible with markdown
        replaceAllMatchesInLinePartially(currentLine, LIST_ORDERED_LETTERS, "[0-9a-zA-Z]+\\.", "1.");    // why does this work?
        replaceAllMatchesInLine(currentLine, ZimWikiHighlighter.Patterns.CHECKLIST.pattern, this::convertChecklist);

        replaceAllMatchesInLine(currentLine, ZimWikiHighlighter.Patterns.SUPERSCRIPT.pattern, fullMatch -> String.format("<sup>%s</sup>",
                fullMatch.replaceAll("^\\^\\{|\\}$", "")));
        replaceAllMatchesInLine(currentLine, ZimWikiHighlighter.Patterns.SUBSCRIPT.pattern, fullMatch -> String.format("<sub>%s</sub>",
                fullMatch.replaceAll("^_\\{|\\}$", "")));
        replaceAllMatchesInLine(currentLine, ZimWikiHighlighter.Patterns.LINK.pattern, fullMatch -> convertLink(fullMatch, context, file));
        replaceAllMatchesInLine(currentLine, ZimWikiHighlighter.Patterns.IMAGE.pattern, fullMatch -> convertImage(file, fullMatch));

        return currentLine.getAndSet("");
    }

    private void replaceAllMatchesInLinePartially(final AtomicReference<String> currentLine, Pattern zimPattern, String matchPartToBeReplaced, String replacementForMatchPart) {
        replaceAllMatchesInLine(currentLine, zimPattern, fullMatch -> fullMatch.replaceAll(matchPartToBeReplaced, replacementForMatchPart));
    }

    private void replaceAllMatchesInLine(final AtomicReference<String> currentLine, Pattern zimPattern, Function<String, String> replaceMatchWithMarkdown) {
        Matcher matcher = zimPattern.matcher(currentLine.get());
        StringBuffer replacedLine = new StringBuffer();
        while (matcher.find()) {
            String fullMatch = matcher.group();
            String replacementForMatch = replaceMatchWithMarkdown.apply(fullMatch);
            matcher.appendReplacement(replacedLine, replacementForMatch);
        }
        matcher.appendTail(replacedLine);
        currentLine.set(replacedLine.toString());
    }

    private String convertHeading(String group) {
        // Header level 1 has 6 equal signs (=)x6; while MD's top level is one hash (#)
        int equalSignsCount = 0;
        while (group.charAt(equalSignsCount) == '=')
            equalSignsCount++;

        // Maximum header level is 5, and has two equal signs
        int markdownLevel = 7 - Math.min(6, equalSignsCount);

        return String.format("%s %s",
                StringUtils.repeatChars('#', markdownLevel),
                group.replaceAll("^=+\\s*|\\s*=+$", ""));
    }

    private String convertHighlighted(String fullMatch, final boolean isExportInLightMode) {
        String content = fullMatch.substring(2, fullMatch.length() - 2);
        return "<span style=\"background-color: " + (isExportInLightMode ? "#ffff00" : "#FFA062") + "\">" + content + "</span>";
    }

    private String convertChecklist(String fullMatch) {
        // TODO: convert to more than two checkstates
        Matcher matcher = Pattern.compile("\\[([ *x>])]").matcher(fullMatch);
        matcher.find();
        String checkboxContent = matcher.group(1);
        if ("*".equals(checkboxContent)) {
            return matcher.replaceFirst("- [x]");
        }
        return matcher.replaceFirst("- [ ]");
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
        } else if (pair[0].matches("^[a-z]+://.+$")) {
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

    private String convertImage(File file, String fullMatch) {
        String imagePathFromPageFolder = fullMatch.substring(2, fullMatch.length() - 2);
        String currentPageFileName = file.getName();
        String currentPageFolderName = currentPageFileName.replaceFirst(".txt$", "");
        String markdownPathToImage = FilenameUtils.concat(currentPageFolderName, imagePathFromPageFolder);
        return "![" + file.getName() + "](" + markdownPathToImage + ")";
    }

    /**
     * NOTE: This method only works if the full file path is specified.
     *
     * @param filepath of a file
     * @return true if the file extension is .txt and the file contains a zim header; false otherwise
     */
    @Override
    public boolean isFileOutOfThisFormat(String filepath) {
        if (!filepath.matches("(?i)^.+\\.txt$")) {
            return false;
        }

        File file = new File(filepath);
        boolean hasZimHeader = false;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder firstLinesOfFile = new StringBuilder();
            for (int lineNumber = 0; lineNumber < 4; lineNumber++) {
                String line = reader.readLine();
                if (line != null) {
                    firstLinesOfFile.append(line + String.format("%n"));
                }
            }
            hasZimHeader = ZimWikiHighlighter.Patterns.ZIMHEADER.pattern.matcher(firstLinesOfFile).find();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return hasZimHeader;
    }

    /*
    public static boolean isZimWikiFile(String filename, Document document) {
        return filename.endsWith(".txt") && containsZimWikiHeader(document);
    }

    private static boolean containsZimWikiHeader(Document document) {
        Pattern headerPattern = ZimWikiHighlighterPattern.ZIMHEADER.pattern;
        Matcher headerMatcher = headerPattern.matcher(document.getContent());
        return headerMatcher.find();
    }*/
}
