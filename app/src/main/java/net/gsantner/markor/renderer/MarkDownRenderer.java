/*
 * Copyright (c) 2014 Jeff Martin
 * Copyright (c) 2015 Pedro Lafuente
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.renderer;

import android.content.Context;
import android.webkit.WebView;

import net.gsantner.markor.model.Constants;
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

public class MarkDownRenderer {
    List<Extension> extensions = Arrays.asList(
            StrikethroughExtension.create(),
            TablesExtension.create(),
            AutolinkExtension.create(),
            InsExtension.create(),
            HeadingAnchorExtension.create(),
            YamlFrontMatterExtension.create());

    Parser parser = Parser.builder().extensions(extensions).build();
    HtmlRenderer renderer = HtmlRenderer.builder().extensions(extensions).build();

    public static String renderMarkdownIntoWebview(Document document, WebView webView) {
        String html = new MarkDownRenderer().renderMarkdown(document.getContent(), webView.getContext());

        // Default font is set by css in line 1 of generated html
        html = html.replaceFirst("sans-serif-light", AppSettings.get().getFontFamily());
        if (document.getFile() != null && document.getFile().getParentFile() != null) {
            webView.loadDataWithBaseURL(document.getFile().getParent(), html, "text/html", Constants.UTF_CHARSET, null);
        } else {
            webView.loadData(html, "text/html", Constants.UTF_CHARSET);
        }

        return html;
    }


    public String renderMarkdown(String markdownRaw, Context context) {

        return themeStringFromContext(context) +
                renderer.render(parser.parse(markdownRaw)) +
                Constants.MD_HTML_SUFFIX;
    }

    private String themeStringFromContext(Context context) {
        String s = "";
        if (AppSettings.get().isDarkThemeEnabled()) {
            s += Constants.DARK_MD_HTML_PREFIX;
        } else {
            s += Constants.MD_HTML_PREFIX;
        }
        if (AppSettings.get().isRenderRtl())
            s += Constants.MD_HTML_RTL_CSS;
        s += Constants.MD_HTML_PREFIX_END;
        return s;
    }
}
