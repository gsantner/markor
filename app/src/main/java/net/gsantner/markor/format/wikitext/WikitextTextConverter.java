/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.wikitext;

import android.content.Context;

import androidx.arch.core.util.Function;

import net.gsantner.markor.ApplicationObject;
import net.gsantner.markor.format.FormatRegistry;
import net.gsantner.markor.format.TextConverterBase;
import net.gsantner.markor.frontend.textview.TextViewUtils;
import net.gsantner.markor.model.AppSettings;

import org.apache.commons.io.FilenameUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Make use of MarkdownConverter by transpiling Wikitext syntax to Markdown
 */
@SuppressWarnings("WeakerAccess")
public class WikitextTextConverter extends TextConverterBase {
    /**
     * First, convert Wikitext to regular Markor markdown. Then, calls the regular converter.
     *
     * @param markup              Markup text
     * @param context             Android Context
     * @param isExportInLightMode True if the light theme is to apply.
     * @param file                The file to convert.
     * @return HTML text
     */
    @Override
    public String convertMarkup(String markup, Context context, boolean isExportInLightMode, File file) {
        String contentWithoutHeader = markup.replaceFirst(WikitextSyntaxHighlighter.ZIMHEADER.toString(), "");
        StringBuilder markdownContent = new StringBuilder();

        for (String line : contentWithoutHeader.split("\\r\\n|\\r|\\n")) {
            String markdownEquivalentLine = getMarkdownEquivalentLine(context, file, line, isExportInLightMode);
            markdownContent.append(markdownEquivalentLine);
            markdownContent.append("  "); // line breaks must be made explicit in markdown by two spaces
            markdownContent.append(String.format("%n"));
        }

        return FormatRegistry.CONVERTER_MARKDOWN.convertMarkup(markdownContent.toString(), context, isExportInLightMode, file);
    }

    private String getMarkdownEquivalentLine(final Context context, final File file, String wikitextLine, final boolean isExportInLightMode) {
        final AtomicReference<String> currentLine = new AtomicReference<>(wikitextLine);

        // Headings
        replaceAllMatchesInLine(currentLine, WikitextSyntaxHighlighter.HEADING, this::convertHeading);

        // bold syntax is the same as for markdown
        replaceAllMatchesInLinePartially(currentLine, WikitextSyntaxHighlighter.ITALICS, "^/+|/+$", "*");
        replaceAllMatchesInLine(currentLine, WikitextSyntaxHighlighter.HIGHLIGHTED, match -> convertHighlighted(match, isExportInLightMode));
        // strikethrough syntax is the same as for markdown

        replaceAllMatchesInLine(currentLine, WikitextSyntaxHighlighter.PREFORMATTED_INLINE, fullMatch -> "`$1`");
        replaceAllMatchesInLine(currentLine, Pattern.compile("^'''$"), fullMatch -> "```");  // preformatted multiline

        // unordered list syntax is compatible with markdown
        replaceAllMatchesInLinePartially(currentLine, WikitextSyntaxHighlighter.LIST_ORDERED, "[0-9a-zA-Z]+\\.", "1.");    // why does this work?
        replaceAllMatchesInLine(currentLine, WikitextSyntaxHighlighter.CHECKLIST, this::convertChecklist);

        replaceAllMatchesInLine(currentLine, WikitextSyntaxHighlighter.SUPERSCRIPT, fullMatch -> String.format("<sup>%s</sup>",
                fullMatch.replaceAll("^\\^\\{|\\}$", "")));
        replaceAllMatchesInLine(currentLine, WikitextSyntaxHighlighter.SUBSCRIPT, fullMatch -> String.format("<sub>%s</sub>",
                fullMatch.replaceAll("^_\\{|\\}$", "")));
        replaceAllMatchesInLine(currentLine, WikitextSyntaxHighlighter.LINK, fullMatch -> convertLink(fullMatch, context, file));
        replaceAllMatchesInLine(currentLine, WikitextSyntaxHighlighter.IMAGE, fullMatch -> convertImage(file, fullMatch));

        return currentLine.getAndSet("");
    }

    private void replaceAllMatchesInLinePartially(final AtomicReference<String> currentLine, Pattern wikitextPattern, String matchPartToBeReplaced, String replacementForMatchPart) {
        replaceAllMatchesInLine(currentLine, wikitextPattern, fullMatch -> fullMatch.replaceAll(matchPartToBeReplaced, replacementForMatchPart));
    }

    private void replaceAllMatchesInLine(final AtomicReference<String> currentLine, Pattern wikitextPattern, Function<String, String> replaceMatchWithMarkdown) {
        Matcher matcher = wikitextPattern.matcher(currentLine.get());
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
                TextViewUtils.repeatChars('#', markdownLevel),
                group.replaceAll("^=+\\s*|\\s*=+$", ""));
    }

    private String convertHighlighted(String fullMatch, final boolean isExportInLightMode) {
        String content = fullMatch.substring(2, fullMatch.length() - 2);
        return "<span style=\"background-color: " + (isExportInLightMode ? "#ffff00" : "#FFA062") + "\">" + content + "</span>";
    }

    private String convertChecklist(String fullMatch) {
        // TODO: convert to more than two checkstates
        // TODO: use a single global regex for zim checkbox expression
        Matcher matcher = Pattern.compile("\\[([ *x><])]").matcher(fullMatch);
        matcher.find();
        String checkboxContent = matcher.group(1);
        if ("*".equals(checkboxContent)) {
            return matcher.replaceFirst("- [x]");
        }
        return matcher.replaceFirst("- [ ]");
    }

    private String convertLink(String group, Context context, File file) {
        AppSettings settings = ApplicationObject.settings();
        File notebookDir = settings.getNotebookDirectory();
        WikitextLinkResolver resolver = WikitextLinkResolver.resolve(group, notebookDir, file, settings.isWikitextDynamicNotebookRootEnabled());

        String markdownLink;
        if (resolver.isWebLink()) {
            markdownLink = resolver.getResolvedLink().replaceAll(" ", "%20");
        } else {
            markdownLink = "file://" + resolver.getResolvedLink();
        }

        String linkDescription = resolver.getLinkDescription() != null ? resolver.getLinkDescription() : resolver.getWikitextPath();
        linkDescription = linkDescription.replaceAll("\\+", "&#43;");

        return String.format("[%s](%s)", linkDescription, markdownLink);
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
     * @param filepath   of a file
     * @param extWithDot
     * @return true if the file extension is .txt and the file contains a zim header; false otherwise
     */
    @Override
    protected boolean isFileOutOfThisFormat(String filepath, String extWithDot) {
        if (extWithDot.equals(".txt")) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(new File(filepath)));
                return WikitextSyntaxHighlighter.ZIMHEADER_CONTENT_TYPE_ONLY.matcher(reader.readLine()).find();
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
        return Arrays.asList(new String[]{".wikitext"}).contains(extWithDot);
    }

    /*
    public static boolean isWikitextFile(String filename, Document document) {
        return filename.endsWith(".txt") && containsZimWikiHeader(document);
    }

    private static boolean containsZimWikiHeader(Document document) {
        Pattern headerPattern = ZimWikiHighlighterPattern.ZIMHEADER.pattern;
        Matcher headerMatcher = headerPattern.matcher(document.getContent());
        return headerMatcher.find();
    }*/
}
