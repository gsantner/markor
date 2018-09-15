/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License: Apache 2.0 / Commercial
 *  https://github.com/gsantner/opoc/#licensing
 *  https://www.apache.org/licenses/LICENSE-2.0
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
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.options.MutableDataSet;

import net.gsantner.markor.format.TextConverter;

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

    public static final Pattern MD_EXTENSION_PATTERN = Pattern.compile("((?i)\\.((md)|(markdown)|(mkd)|(mdown)|(mkdn)|(txt)|(mdwn)|(text)|(rmd))$)");
    public static final String[] MD_EXTENSIONS = new String[]{
            EXT_MARKDOWN__MD, EXT_MARKDOWN__MARKDOWN, EXT_MARKDOWN__MKD, EXT_MARKDOWN__MDOWN,
            EXT_MARKDOWN__MKDN, EXT_MARKDOWN__TXT, EXT_MARKDOWN__MDWN, EXT_MARKDOWN__TEXT,
            EXT_MARKDOWN__RMD, EXT_MARKDOWN__MD_TXT
    };
    public static final String HTML_KATEX_INCLUDE = "<link rel='stylesheet'  type='text/css' href='file:///android_asset/katex/katex.min.css'>" +
            "<script src='file:///android_asset/katex/katex.min.js'></script>" +
            "<script src='file:///android_asset/katex/auto-render.min.js'></script>";
    public static final String HTML_KATEX_JS = "" +
            "renderMathInElement(document.body, {" +
            "   'delimiters': [ " +
            "       { left: '$', right: '$', display: false }," +
            "       { left: '$$', right: '$$', display: true }" +
            "]});\n";

    //########################
    //## Converter library
    //########################
    // See https://github.com/vsch/flexmark-java/wiki/Extensions#tables
    private static final List<Extension> MARKDOWN_ENABLED_EXTENSIONS = Arrays.asList(
            StrikethroughExtension.create(),
            AutolinkExtension.create(),
            InsExtension.create(),
            JekyllTagExtension.create(),
            JekyllFrontMatterExtension.create(),
            TablesExtension.create(),
            TaskListExtension.create(),
            EmojiExtension.create(),
            AnchorLinkExtension.create(),
            YamlFrontMatterExtension.create());
    private static final Parser parser = Parser.builder().extensions(MARKDOWN_ENABLED_EXTENSIONS).build();
    private static final HtmlRenderer renderer = HtmlRenderer.builder().extensions(MARKDOWN_ENABLED_EXTENSIONS).build();

    //########################
    //## Methods
    //########################

    @Override
    public String convertMarkup(String markup, Context context) {
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, MARKDOWN_ENABLED_EXTENSIONS);
        // allow links like [this](some filename with spaces.md)
        options.set(Parser.SPACE_IN_LINK_URLS, true);

        options.set(EmojiExtension.USE_IMAGE_TYPE, EmojiImageType.UNICODE_ONLY);
        options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");
        options.set(AnchorLinkExtension.ANCHORLINKS_WRAP_TEXT, true);
        options.set(HtmlRenderer.GENERATE_HEADER_ID, true);
        options.set(AnchorLinkExtension.ANCHORLINKS_ANCHOR_CLASS, "header_no_underline");


        String markupRendered = renderer.withOptions(options).render(parser.parse(markup));
        return putContentIntoTemplate(context, markupRendered);
    }

    public static boolean isMarkdownFile(File file) {
        String fnlower = file.getAbsolutePath().toLowerCase();
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
