/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.markdown;

import android.content.Context;

import com.vladsch.flexmark.Extension;
import com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.emoji.EmojiExtension;
import com.vladsch.flexmark.ext.emoji.EmojiImageType;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.ins.InsExtension;
import com.vladsch.flexmark.ext.jekyll.front.matter.JekyllFrontMatterExtension;
import com.vladsch.flexmark.ext.jekyll.tag.JekyllTagExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.SimTocExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.ext.toc.internal.TocOptions;
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.options.MutableDataSet;

import net.gsantner.markor.R;
import net.gsantner.markor.format.TextConverter;
import net.gsantner.markor.util.AppSettings;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@SuppressWarnings("WeakerAccess")
public class MarkdownTextConverter extends TextConverter {
    //########################
    //## Extensions
    //########################
    public static final String EXT_MARKDOWN__TXT = ".txt";
    public static final String EXT_MARKDOWN__MD_TXT = ".md.txt";
    public static final String EXT_MARKDOWN__MD = ".md";
    public static final String EXT_MARKDOWN__MARKDOWN = ".markdown";
    public static final String EXT_MARKDOWN__MKD = ".mkd";
    public static final String EXT_MARKDOWN__MDOWN = ".mdown";
    public static final String EXT_MARKDOWN__MKDN = ".mkdn";
    public static final String EXT_MARKDOWN__MDWN = ".mdwn";
    public static final String EXT_MARKDOWN__TEXT = ".text";
    public static final String EXT_MARKDOWN__RMD = ".rmd";
    public static final String EXT_ZIM = ".zim";

    public static final Pattern MD_EXTENSION_PATTERN = Pattern.compile("((?i)\\.((md)|(markdown)|(mkd)|(mdown)|(mkdn)|(txt)|(mdwn)|(text)|(rmd)|(zim))$)");
    public static final String[] MD_EXTENSIONS = new String[]{
            EXT_MARKDOWN__MD, EXT_MARKDOWN__MARKDOWN, EXT_MARKDOWN__MKD, EXT_MARKDOWN__MDOWN,
            EXT_MARKDOWN__MKDN, EXT_MARKDOWN__TXT, EXT_MARKDOWN__MDWN, EXT_MARKDOWN__TEXT,
            EXT_MARKDOWN__RMD, EXT_MARKDOWN__MD_TXT, EXT_ZIM
    };

    //########################
    //## Injected CSS / JS / HTML
    //########################
    public static final String CSS_HEADER_UNDERLINE = CSS_S + " .header_no_underline { text-decoration: none; color: " + TOKEN_BW_INVERSE_OF_THEME + "; } h1 < a.header_no_underline { border-bottom: 2px solid #eaecef; } " + CSS_E;
    public static final String CSS_H1_H2_UNDERLINE = CSS_S + " h1,h2 { border-bottom: 2px solid #eaecef; } " + CSS_E;
    public static final String CSS_BLOCKQUOTE_VERTICAL_LINE = CSS_S + "blockquote{padding:0px 14px;border-" + TOKEN_TEXT_DIRECTION + ":3.5px solid #dddddd;margin:4px 0}" + CSS_E;

    public static final String HTML_KATEX_INCLUDE = "<link rel='stylesheet'  type='text/css' href='file:///android_asset/katex/katex.min.css'>" +
            "<script src='file:///android_asset/katex/katex.min.js'></script>" +
            "<script src='file:///android_asset/katex/auto-render.min.js'></script>";
    public static final String JS_KATEX = "" +
            "renderMathInElement(document.body, {" +
            "   'delimiters': [ " +
            "       {left: '$$', right: '$$', display: true},   {left: '\\(', right: '\\)', display: false},   {left: '\\[', right: '\\]', display: true}, { left: '$', right: '$', display: true }," +
            "]});\n";

    //########################
    //## Converter library
    //########################
    // See https://github.com/vsch/flexmark-java/wiki/Extensions#tables
    private static final List<Extension> flexmarkExtensions = Arrays.asList(
            StrikethroughExtension.create(),
            AutolinkExtension.create(),
            InsExtension.create(),
            JekyllTagExtension.create(),
            JekyllFrontMatterExtension.create(),
            TablesExtension.create(),
            TaskListExtension.create(),
            EmojiExtension.create(),
            AnchorLinkExtension.create(),
            TocExtension.create(),    // https://github.com/vsch/flexmark-java/wiki/Table-of-Contents-Extension
            SimTocExtension.create(), // https://github.com/vsch/flexmark-java/wiki/Table-of-Contents-Extension
            YamlFrontMatterExtension.create());
    private static final Parser flexmarkParser = Parser.builder().extensions(flexmarkExtensions).build();
    private static final HtmlRenderer flexmarkRenderer = HtmlRenderer.builder().extensions(flexmarkExtensions).build();

    //########################
    //## Methods
    //########################

    @Override
    public String convertMarkup(String markup, Context context) {
        AppSettings appSettings = new AppSettings(context);
        String converted = "", onLoadJs = "", head = "";

        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, flexmarkExtensions);
        options.set(Parser.SPACE_IN_LINK_URLS, true); // allow links like [this](some filename with spaces.md)
        options.set(HtmlRenderer.SOFT_BREAK, "<br />\n"); // Add linefeed to html break
        options.set(EmojiExtension.USE_IMAGE_TYPE, EmojiImageType.UNICODE_ONLY); // Use unicode (OS/browser images)

        // gfm table parsing
        options.set(TablesExtension.WITH_CAPTION, false)
                .set(TablesExtension.COLUMN_SPANS, false)
                .set(TablesExtension.MIN_HEADER_ROWS, 1)
                .set(TablesExtension.MAX_HEADER_ROWS, 1)
                .set(TablesExtension.APPEND_MISSING_COLUMNS, true)
                .set(TablesExtension.DISCARD_EXTRA_COLUMNS, true)
                .set(TablesExtension.HEADER_SEPARATOR_COLUMN_MATCH, true);

        // Add id to headers
        options.set(HtmlRenderer.GENERATE_HEADER_ID, true)
                .set(AnchorLinkExtension.ANCHORLINKS_WRAP_TEXT, true)
                .set(AnchorLinkExtension.ANCHORLINKS_ANCHOR_CLASS, "header_no_underline");

        // Prepare head and javascript calls
        head += CSS_HEADER_UNDERLINE + CSS_H1_H2_UNDERLINE + CSS_BLOCKQUOTE_VERTICAL_LINE;
        if (appSettings.isMarkdownTableOfContentsEnabled() && (markup.contains("#") || markup.contains("<h"))) {
            markup = "[TOC]: # ''\n  \n" + markup;
            options.set(TocExtension.LEVELS, TocOptions.getLevels(1, 2, 3))
                    .set(TocExtension.TITLE, context.getString(R.string.table_of_contents))
                    .set(TocExtension.BLANK_LINE_SPACER, false);
        }

        if (appSettings.isMarkdownMathEnabled() && markup.contains("$")) {
            head += HTML_KATEX_INCLUDE;
            onLoadJs += JS_KATEX;
        }

        converted = flexmarkRenderer.withOptions(options).render(flexmarkParser.parse(markup));
        return putContentIntoTemplate(context, converted, onLoadJs, head);
    }

    public static boolean isMarkdownFile(File file) {
        String fnlower = file.getAbsolutePath().toLowerCase();
        if (fnlower.endsWith(".zim")) {
            return false;
        }
        return MarkdownTextConverter.isTextOrMarkdownFile(file) && (!fnlower.endsWith(".txt") || fnlower.endsWith(".md.txt"));
    }

    // Either pass file or null and absolutePath
    public static boolean isTextOrMarkdownFile(File file, String... absolutePath) {
        String path = (absolutePath != null && absolutePath.length > 0)
                ? absolutePath[0] : file.getAbsolutePath();
        path = path.toLowerCase(Locale.ROOT);
        for (String ext : MD_EXTENSIONS) {
            if (path.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
}
