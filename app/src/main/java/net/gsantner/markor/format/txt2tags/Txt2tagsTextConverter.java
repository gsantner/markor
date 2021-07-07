/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.txt2tags;

import android.arch.core.util.Function;
import android.content.Context;

import net.gsantner.markor.format.TextConverter;
import net.gsantner.markor.format.TextFormat;
import net.gsantner.opoc.util.StringUtils;

import org.apache.commons.io.FilenameUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Make use of MarkdownConverter by converting txt2tags syntax to Markdown.
 * Used for preview.
 */
@SuppressWarnings("WeakerAccess")
public class Txt2tagsTextConverter extends TextConverter {
    //private static final Pattern LIST_ORDERED_LETTERS = Pattern.compile("^\t*([\\d]+\\.|[a-zA-Z]+\\.) ");
    //private static final Pattern LIST_ORDERED_LETTERS = Pattern.compile("^\t*([\\d]+\\.|[a-zA-Z]+\\.) ");

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
        String contentWithoutHeader = markup.replaceFirst(Txt2tagsHighlighter.Patterns.ZIMHEADER.pattern.toString(), "");
        StringBuilder markdownContent = new StringBuilder();

        for (String line : contentWithoutHeader.split("\\r\\n|\\r|\\n")) {
            String markdownEquivalentLine = getMarkdownEquivalentLine(context, file, line, isExportInLightMode);
            markdownContent.append(markdownEquivalentLine);
            markdownContent.append("  "); // line breaks must be made explicit in markdown by two spaces
            markdownContent.append(String.format("%n"));
        }

        return TextFormat.CONVERTER_MARKDOWN.convertMarkup(markdownContent.toString(), context, isExportInLightMode, file);
    }

    private String getMarkdownEquivalentLine(final Context context, final File file, String Txt2tagsLine, final boolean isExportInLightMode) {
        final AtomicReference<String> currentLine = new AtomicReference<>(Txt2tagsLine);

        // Headings
        replaceAllMatchesInLine(currentLine, Txt2tagsHighlighter.Patterns.HEADING.pattern, this::convertHeading);

        // bold syntax is the same as for markdown
        replaceAllMatchesInLinePartially(currentLine, Txt2tagsHighlighter.Patterns.ITALICS.pattern, "^/+|/+$", "*");
        replaceAllMatchesInLine(currentLine, Txt2tagsHighlighter.Patterns.HIGHLIGHTED.pattern, match -> convertHighlighted(match, isExportInLightMode));
        // strikethrough syntax
        replaceAllMatchesInLine(currentLine, Txt2tagsHighlighter.Patterns.STRIKETHROUGH.pattern, match -> convertStrike(match, isExportInLightMode));

        replaceAllMatchesInLine(currentLine, Txt2tagsHighlighter.Patterns.PREFORMATTED_INLINE.pattern, fullMatch -> "`$1`");
        replaceAllMatchesInLine(currentLine, Pattern.compile("^'''$"), fullMatch -> "```");  // preformatted multiline

        // unordered list syntax is compatible with markdown
        //replaceAllMatchesInLinePartially(currentLine, LIST_ORDERED_LETTERS, "[0-9a-zA-Z]+\\.", "1.");    // why does this work?
        //replaceAllMatchesInLinePartially(currentLine, LIST_ORDERED_LETTERS, "[0-9a-zA-Z]+\\.", "+");    // why does this work?
        replaceAllMatchesInLine(currentLine, Txt2tagsHighlighter.Patterns.CHECKLIST.pattern, this::convertChecklist);

        replaceAllMatchesInLine(currentLine, Txt2tagsHighlighter.Patterns.SUPERSCRIPT.pattern, fullMatch -> String.format("<sup>%s</sup>",
                fullMatch.replaceAll("^\\^\\{|\\}$", "")));
        replaceAllMatchesInLine(currentLine, Txt2tagsHighlighter.Patterns.SUBSCRIPT.pattern, fullMatch -> String.format("<sub>%s</sub>",
                fullMatch.replaceAll("^_\\{|\\}$", "")));
        replaceAllMatchesInLine(currentLine, Txt2tagsHighlighter.Patterns.LINK.pattern, fullMatch -> convertLink(fullMatch, context, file));
        replaceAllMatchesInLine(currentLine, Txt2tagsHighlighter.Patterns.IMAGE.pattern, fullMatch -> convertImage(file, fullMatch));

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
        //int markdownLevel = 7 - Math.min(6, equalSignsCount);
        int markdownLevel = equalSignsCount;

        return String.format("%s %s",
                StringUtils.repeatChars('#', markdownLevel),
                group.replaceAll("^=+\\s*|\\s*=+$", ""));
    }

    private String convertHighlighted(String fullMatch, final boolean isExportInLightMode) {
        String content = fullMatch.substring(2, fullMatch.length() - 2);
        //return "<span style=\"background-color: " + (isExportInLightMode ? "#ffff00" : "#FFA062") + "\">" + content + "</span>";
        return "<u>" + content + "</u>";
    }
    
    private String convertStrike(String fullMatch, final boolean isExportInLightMode) {
        String content = fullMatch.substring(2, fullMatch.length() - 2);
        return "<s>" + content + "</s>";
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
//        if (pair[0].charAt(0) == '+') {
////            fullPath.append("file://");
////            fullPath.append(context.getFilesDir().getAbsolutePath());
////            fullPath.append(File.separator);
////            fullPath.append(pair[0].substring(1));
////            fullPath.append(".txt");
//        } else if (pair[0].matches("^[a-z]+://.+$")) {
//         //   fullPath.append(pair[0]);
//        } else {
////            fullPath.append("file://");
////            if (pair[0].charAt(0) == ':')
////                fullPath.append(context.getFilesDir().getAbsolutePath());
////            else
////                fullPath.append(file.getParentFile().getAbsolutePath());
////            for (String token : pair[0].split(":")) {
////                fullPath.append(File.separator);
////                fullPath.append(token);
////            }
////            fullPath.append(".txt");
//        }

        return String.format("[%s]", pair[pair.length - 1], fullPath.toString().replaceAll(" ", "%20"));
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
        if (filepath.matches("(?i)^.+\\.txt$")) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(new File(filepath)));
                return Txt2tagsHighlighter.Patterns.ZIMHEADER_CONTENT_TYPE_ONLY.pattern.matcher(reader.readLine()).find();
            } catch (Exception ignored) {
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return false;
    }

    /*
    public static boolean isTxt2tagsFile(String filename, Document document) {
        return filename.endsWith(".txt") && containsTxt2tagsHeader(document);
    }

    private static boolean containsTxt2tagsHeader(Document document) {
        Pattern headerPattern = Txt2tagsHighlighterPattern.ZIMHEADER.pattern;
        Matcher headerMatcher = headerPattern.matcher(document.getContent());
        return headerMatcher.find();
    }*/
}
