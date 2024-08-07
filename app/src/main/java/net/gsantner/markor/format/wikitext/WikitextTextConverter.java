/*#######################################################
 *
 *   Maintained 2018-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.wikitext;

import android.content.Context;

import androidx.arch.core.util.Function;

import net.gsantner.markor.ApplicationObject;
import net.gsantner.markor.format.FormatRegistry;
import net.gsantner.markor.format.TextConverterBase;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.opoc.format.GsTextUtils;

import org.apache.commons.io.FilenameUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Objects;
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
     * @param markup    Markup text
     * @param context   Android Context
     * @param lightMode True if the light theme is to apply.
     * @param lineNum
     * @param file      The file to convert.
     * @return HTML text
     */
    @Override
    public String convertMarkup(String markup, Context context, boolean lightMode, boolean lineNum, File file) {
        String contentWithoutHeader = markup.replaceFirst(WikitextSyntaxHighlighter.ZIMHEADER.toString(), "");
        StringBuilder markdownContent = new StringBuilder();

        for (String line : contentWithoutHeader.split("\\r\\n|\\r|\\n")) {
            String markdownEquivalentLine = getMarkdownEquivalentLine(context, file, line, lightMode);
            markdownContent.append(markdownEquivalentLine);
            markdownContent.append("  "); // line breaks must be made explicit in markdown by two spaces
            markdownContent.append(String.format("%n"));
        }

        return FormatRegistry.CONVERTER_MARKDOWN.convertMarkup(markdownContent.toString(), context, lightMode, lineNum, file);
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
                GsTextUtils.repeatChars('#', markdownLevel),
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

        // Zim may insert in the image link, after a '?' character, the 'id', 'width',
        // 'height', 'type', and 'href' tags, separating them with a '&' character, so
        // you may not want to use '?' and '&' as directory or file name:
        // https://github.com/zim-desktop-wiki/zim-desktop-wiki/blob/c88cf3cb53896bf272e87704826b77e82eddb3ef/zim/formats/__init__.py#L903
        final int pos = markdownPathToImage.indexOf("?");
        if (pos != -1) {
            final String image = markdownPathToImage.substring(0, pos);
            final String[] options = markdownPathToImage.substring(pos + 1).split("&");
            String link = null; // <a href="link"></a> or [![name](image)](link)
            StringBuilder attributes = new StringBuilder(); // <img id="" width="" height="" />
            // The 'type' tag is for backward compatibility of image generators before
            // Zim version 0.70.  Here, it probably may be ignored:
            // https://github.com/zim-desktop-wiki/zim-desktop-wiki/blob/c88cf3cb53896bf272e87704826b77e82eddb3ef/zim/formats/wiki.py#586
            final Pattern tags = Pattern.compile("(id|width|height|href)=(.+)", Pattern.CASE_INSENSITIVE);
            for (String item : options) {
                final Matcher data = tags.matcher(item);
                if (data.matches()) {
                    final String key = Objects.requireNonNull(data.group(1)).toLowerCase();
                    String value = data.group(2);
                    try {
                        value = URLDecoder.decode(value, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    if (key.equals("href")) {
                        link = value;
                    } else {
                        attributes.append(String.format("%s=\"%s\" ", key, value));
                    }
                }
            }
            String html = String.format("<img src=\"%s\" alt=\"%s\" %s/>", image, currentPageFileName, attributes);
            if (link != null) {
                AppSettings settings = ApplicationObject.settings();
                File notebookDir = settings.getNotebookDirectory();
                link = WikitextLinkResolver.resolveAttachmentPath(link, notebookDir, file, settings.isWikitextDynamicNotebookRootEnabled());
                html = String.format("<a href=\"%s\">%s</a>", link, html);
            }
            return html;
        }

        return String.format("![%s](%s)", currentPageFileName, markdownPathToImage);
    }

    @Override
    protected boolean isFileOutOfThisFormat(final File file, final String name, final String ext) {
        if (ext.equals(".txt")) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                return WikitextSyntaxHighlighter.ZIMHEADER_CONTENT_TYPE_ONLY.matcher(reader.readLine()).find();
            } catch (Exception ignored) {
            }
        }
        return Arrays.asList(new String[]{".wikitext"}).contains(ext);
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
