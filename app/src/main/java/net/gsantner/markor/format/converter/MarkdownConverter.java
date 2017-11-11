/*
 * Copyright (c) 2014 Jeff Martin
 * Copyright (c) 2015 Pedro Lafuente
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.format.converter;

import android.content.Context;
import android.webkit.WebView;

import net.gsantner.markor.model.Document;
import net.gsantner.markor.util.AppSettings;

import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.front.matter.YamlFrontMatterExtension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.heading.anchor.HeadingAnchorExtension;
import org.commonmark.ext.ins.InsExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@SuppressWarnings("WeakerAccess")
public class MarkdownConverter {
    //########################
    //## HTML
    //########################
    public static final String UTF_CHARSET = "utf-8";
    public static final String UNSTYLED_HTML_PREFIX = "<html><body>";
    public static final String MD_HTML_PREFIX = "<html><head><style type=\"text/css\">html,body{padding:4px 8px 4px 8px;font-family:'sans-serif-light';color:#303030;}h1,h2,h3,h4,h5,h6{font-family:'sans-serif-condensed';}a{color:#388E3C;text-decoration:underline;}img{height:auto;width:325px;margin:auto;}</style>";
    public static final String DARK_MD_HTML_PREFIX = "<html><head><style type=\"text/css\">html,body{padding:4px 8px 4px 8px;font-family:'sans-serif-light';color:#ffffff;background-color:#303030;}h1,h2,h3,h4,h5,h6{font-family:'sans-serif-condensed';}a{color:#388E3C;text-decoration:underline;}a:visited{color:#dddddd;}img{height:auto;width:325px;margin:auto;}</style>";
    public static final String MD_HTML_PREFIX_END = "</head><body>";
    public static final String MD_HTML_RTL_CSS = "<style>body{text-align:right; direction:rtl;}</style>";
    public static final String MD_HTML_SUFFIX = "</body></html>";

    //########################
    //## Extensions
    //########################
    public static final String EXT_MARKDOWN__TXT = ".txt";
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
            EXT_MARKDOWN__MKDN, EXT_MARKDOWN__TXT, EXT_MARKDOWN__MDWN, EXT_MARKDOWN__TEXT, EXT_MARKDOWN__RMD
    };

    //########################
    //## Converter library
    //########################
    // See https://github.com/atlassian/commonmark-java/blob/master/README.md#extensions
    private static final List<Extension> COMMONMARK_JAVA_EXTENSIONS = Arrays.asList(
            StrikethroughExtension.create(),
            TablesExtension.create(),
            AutolinkExtension.create(),
            InsExtension.create(),
            HeadingAnchorExtension.create(),
            YamlFrontMatterExtension.create());
    private static final Parser parser = Parser.builder().extensions(COMMONMARK_JAVA_EXTENSIONS).build();
    private static final HtmlRenderer renderer = HtmlRenderer.builder().extensions(COMMONMARK_JAVA_EXTENSIONS).build();


    //########################
    //## Methods
    //########################
    public static String convertToHtmlRenderIntoWebview(Document document, WebView webView) {
        String html = new MarkdownConverter().convertMarkdownToHtml(document.getContent(), webView.getContext());

        // Default font is set by css in line 1 of generated html
        html = html.replaceFirst("sans-serif-light", AppSettings.get().getFontFamily());
        if (document.getFile() != null && document.getFile().getParentFile() != null) {
            webView.loadDataWithBaseURL(document.getFile().getParent(), html, "text/html", UTF_CHARSET, null);
        } else {
            webView.loadData(html, "text/html", UTF_CHARSET);
        }

        return html;
    }


    public String convertMarkdownToHtml(String markdownText, Context context) {
        return themeStringFromContext(context) +
                renderer.render(parser.parse(markdownText)) +
                MD_HTML_SUFFIX;
    }

    private String themeStringFromContext(Context context) {
        String s = "";
        if (AppSettings.get().isDarkThemeEnabled()) {
            s += DARK_MD_HTML_PREFIX;
        } else {
            s += MD_HTML_PREFIX;
        }
        if (AppSettings.get().isRenderRtl())
            s += MD_HTML_RTL_CSS;
        s += MD_HTML_PREFIX_END;
        return s;
    }
}
