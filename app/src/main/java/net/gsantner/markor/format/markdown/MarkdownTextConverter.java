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
import android.support.annotation.NonNull;

import com.vladsch.flexmark.Extension;
import com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.emoji.EmojiExtension;
import com.vladsch.flexmark.ext.emoji.EmojiImageType;
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.ins.InsExtension;
import com.vladsch.flexmark.ext.jekyll.front.matter.JekyllFrontMatterExtension;
import com.vladsch.flexmark.ext.jekyll.tag.JekyllTagExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.SimTocExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.ext.toc.internal.TocOptions;
import com.vladsch.flexmark.ext.wikilink.WikiLinkExtension;
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.options.MutableDataSet;

import net.gsantner.markor.R;
import net.gsantner.markor.format.TextConverter;
import net.gsantner.markor.format.TextFormat;
import net.gsantner.markor.util.AppSettings;

import java.io.File;
import java.util.Arrays;
import java.util.List;
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

    public static final String MD_EXTENSIONS_PATTERN_LIST = "((md)|(markdown)|(mkd)|(mdown)|(mkdn)|(txt)|(mdwn)|(text)|(rmd))";
    public static final Pattern PATTERN_HAS_FILE_EXTENSION_FOR_THIS_FORMAT = Pattern.compile("((?i).*\\." + MD_EXTENSIONS_PATTERN_LIST + "$)");
    public static final Pattern MD_EXTENSION_PATTERN = Pattern.compile("((?i)\\." + MD_EXTENSIONS_PATTERN_LIST + "$)");
    public static final String[] MD_EXTENSIONS = new String[]{
            EXT_MARKDOWN__MD, EXT_MARKDOWN__MARKDOWN, EXT_MARKDOWN__MKD, EXT_MARKDOWN__MDOWN,
            EXT_MARKDOWN__MKDN, EXT_MARKDOWN__TXT, EXT_MARKDOWN__MDWN, EXT_MARKDOWN__TEXT,
            EXT_MARKDOWN__RMD, EXT_MARKDOWN__MD_TXT
    };

    //########################
    //## Injected CSS / JS / HTML
    //########################
    public static final String CSS_HEADER_UNDERLINE = CSS_S + " .header_no_underline { text-decoration: none; color: " + TOKEN_BW_INVERSE_OF_THEME + "; } h1 < a.header_no_underline { border-bottom: 2px solid #eaecef; } " + CSS_E;
    public static final String CSS_H1_H2_UNDERLINE = CSS_S + " h1,h2 { border-bottom: 2px solid #eaecef; } " + CSS_E;
    public static final String CSS_BLOCKQUOTE_VERTICAL_LINE = CSS_S + "blockquote{padding:0px 14px;border-" + TOKEN_TEXT_DIRECTION + ":3.5px solid #dddddd;margin:4px 0}" + CSS_E;
    public static final String CSS_LIST_TASK_NO_BULLET = CSS_S + ".task-list-item { list-style-type:none; text-indent: -1.4em; }" + CSS_E;

    public static final String HTML_KATEX_INCLUDE = "<link rel='stylesheet'  type='text/css' href='file:///android_asset/katex/katex.min.css'>" +
            "<script src='file:///android_asset/katex/katex.min.js'></script>" +
            "<script src='file:///android_asset/katex/auto-render.min.js'></script>";
    public static final String JS_KATEX = "" +
            "renderMathInElement(document.body, {" +
            "   'delimiters': [ " +
            "       {left: '$$', right: '$$', display: true}, { left: '$', right: '$', display: false }," +
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
            TocExtension.create(),                // https://github.com/vsch/flexmark-java/wiki/Table-of-Contents-Extension
            SimTocExtension.create(),             // https://github.com/vsch/flexmark-java/wiki/Table-of-Contents-Extension
            WikiLinkExtension.create(),           // https://github.com/vsch/flexmark-java/wiki/Extensions#wikilinks
            YamlFrontMatterExtension.create(),
            FootnoteExtension.create()            // https://github.com/vsch/flexmark-java/wiki/Footnotes-Extension#overview
    );
    private static final Parser flexmarkParser = Parser.builder().extensions(flexmarkExtensions).build();
    private static final HtmlRenderer flexmarkRenderer = HtmlRenderer.builder().extensions(flexmarkExtensions).build();

    //########################
    //## Methods
    //########################

    @Override
    public String convertMarkup(String markup, Context context, boolean isExportInLightMode, File file) {
        AppSettings appSettings = new AppSettings(context);
        String converted = "", onLoadJs = "", head = "";

        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, flexmarkExtensions);
        options.set(Parser.SPACE_IN_LINK_URLS, true); // allow links like [this](some filename with spaces.md)
        //options.set(HtmlRenderer.SOFT_BREAK, "<br />\n"); // Add linefeed to html break
        options.set(EmojiExtension.USE_IMAGE_TYPE, EmojiImageType.UNICODE_ONLY); // Use unicode (OS/browser images)

        // gfm table parsing
        options.set(TablesExtension.WITH_CAPTION, false)
                .set(TablesExtension.COLUMN_SPANS, false)
                .set(TablesExtension.MIN_HEADER_ROWS, 1)
                .set(TablesExtension.MAX_HEADER_ROWS, 1)
                .set(TablesExtension.APPEND_MISSING_COLUMNS, true)
                .set(TablesExtension.DISCARD_EXTRA_COLUMNS, true)
                .set(WikiLinkExtension.LINK_ESCAPE_CHARS, "")
                .set(TablesExtension.HEADER_SEPARATOR_COLUMN_MATCH, true);

        // Add id to headers
        options.set(HtmlRenderer.GENERATE_HEADER_ID, true)
                .set(AnchorLinkExtension.ANCHORLINKS_WRAP_TEXT, true)
                .set(AnchorLinkExtension.ANCHORLINKS_ANCHOR_CLASS, "header_no_underline");

        // Prepare head and javascript calls
        head += CSS_HEADER_UNDERLINE + CSS_H1_H2_UNDERLINE + CSS_BLOCKQUOTE_VERTICAL_LINE + CSS_LIST_TASK_NO_BULLET;
        if (appSettings.isMarkdownTableOfContentsEnabled() && (markup.contains("#") || markup.contains("<h"))) {
            markup = "[TOC]: # ''\n  \n" + markup;
            options.set(TocExtension.LEVELS, TocOptions.getLevels(1, 2, 3))
                    .set(TocExtension.TITLE, context.getString(R.string.table_of_contents))
                    .set(TocExtension.BLANK_LINE_SPACER, false);
        }

        // Enable Math / KaTex
        if (appSettings.isMarkdownMathEnabled() && markup.contains("$")) {
            head += HTML_KATEX_INCLUDE;
            onLoadJs += JS_KATEX;
        }

        // Enable View (block) code syntax highlighting
        final String xt = getViewHlPrismIncludes(context, (appSettings.isDarkThemeEnabled() ? "-tomorrow" : ""));
        head += xt;


        // Replace {{ site.baseurl }} with ..--> usually used in Jekyll blog _posts folder which is one folder below repository root, for reference to e.g. pictures in assets folder
        markup = markup.replace("{{ site.baseurl }}", "..");

        ////////////
        // Markup parsing - afterwards = HTML
        converted = flexmarkRenderer.withOptions(options).render(flexmarkParser.parse(markup));

        // After render changes: Fixes for Footnotes (converter creates footnote + <br> + ref#(click) --> remove line break)
        if (converted.contains("footnote-")) {
            converted = converted.replace("</p>\n<a href=\"#fnref-", "<a href=\"#fnref-").replace("class=\"footnote-backref\">&#8617;</a>", "class=\"footnote-backref\"> &#8617;</a></p>");
        }

        // Deliver result
        return putContentIntoTemplate(context, converted, isExportInLightMode, file, onLoadJs, head);
    }

    @SuppressWarnings({"ConstantConditions", "StringConcatenationInsideStringBufferAppend"})
    private String getViewHlPrismIncludes(@NonNull final Context context, final String themeName) {
        final StringBuilder sb = new StringBuilder(1500);
        final String js_prefix = "<script type='text/javascript' src='file:///android_asset/prism/";
        sb.append("\n\n");
        sb.append("<link rel='stylesheet' href='file:///android_asset/prism/prism" + themeName + ".min.css' /> ");
        sb.append(js_prefix + "prism.min.js'></script> ");
        sb.append(js_prefix + "prism-markup-templating.min.js'></script> ");
        try {
            for (String lang : context.getAssets().list("prism")) {
                if (!lang.endsWith(".js") || lang.contains("prism.min.js") || lang.contains("prism-markup-templating.min.js")) {
                    continue;
                }
                sb.append(js_prefix);
                sb.append(lang);
                sb.append("'></script> ");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        sb.append("\n\n");
        return sb.toString();
    }

    @Override
    public boolean isFileOutOfThisFormat(String filepath) {
        return (MarkdownTextConverter.PATTERN_HAS_FILE_EXTENSION_FOR_THIS_FORMAT.matcher(filepath).matches() && !filepath.toLowerCase().endsWith(".txt")) || filepath.toLowerCase().endsWith(".md.txt");
    }

    public static boolean isMarkdownFile(File file) {
        String fnlower = file.getAbsolutePath().toLowerCase();
        if (fnlower.endsWith(".zim")) {
            return false;
        }
        return TextFormat.isTextFile(file) && (!fnlower.endsWith(".txt") || fnlower.endsWith(".md.txt"));
    }

}
