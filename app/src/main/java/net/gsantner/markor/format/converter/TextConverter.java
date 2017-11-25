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

@SuppressWarnings("WeakerAccess")
public abstract class TextConverter {
    //########################
    //## HTML
    //########################
    protected static final String UTF_CHARSET = "utf-8";
    protected static final String HTML001_HEAD_WITH_STYLE_LIGHT = "<html><head><style type=\"text/css\">html,body{padding:4px 8px 4px 8px;font-family:'sans-serif-light';color:#303030;}h1,h2,h3,h4,h5,h6{font-family:'sans-serif-condensed';}a{color:#388E3C;text-decoration:underline;}img{height:auto;width:325px;margin:auto;}</style>";
    protected static final String HTML001_HEAD_WITH_STYLE_DARK = "<html><head><style type=\"text/css\">html,body{padding:4px 8px 4px 8px;font-family:'sans-serif-light';color:#ffffff;background-color:#303030;}h1,h2,h3,h4,h5,h6{font-family:'sans-serif-condensed';}a{color:#388E3C;text-decoration:underline;}a:visited{color:#dddddd;}img{height:auto;width:325px;margin:auto;}</style>";
    protected static final String HTML002_RIGHT_TO_LEFT = "<style>body{text-align:right; direction:rtl;}</style>";
    protected static final String HTML010_BODY = "</head><body>";
    protected static final String HTML990_BODY_END = "</body></html>";
    protected static final String CONTENT_TYPE_HTML = "text/html";
    protected static final String CONTENT_TYPE_PLAIN = "text/plain";

    //########################
    //## Methods
    //########################

    /**
     * Convert markup to target format and show the result in a WebView
     *
     * @param document The document containting the contents
     * @param webView  The WebView content to be shown in
     * @return Copy of converted html
     */
    public String convertMarkupShowInWebView(Document document, WebView webView) {
        Context context = webView.getContext();
        String html = convertMarkup(document.getContent(), context);


        String baseFolder = new AppSettings(context).getNotebookDirectoryAsStr();
        if (document.getFile() != null && document.getFile().getParentFile() != null) {
            baseFolder = document.getFile().getParent();
        }
        baseFolder = "file://" + baseFolder + "/";
        webView.loadDataWithBaseURL(baseFolder, html, getContentType(), UTF_CHARSET, null);

        return html;
    }

    /**
     * Convert markup text to target format
     *
     * @param markup  Markup text
     * @param context Android Context
     * @return html as String
     */
    public abstract String convertMarkup(String markup, Context context);

    protected String putContentIntoTemplate(Context context, String content) {
        AppSettings as = new AppSettings(context);
        String html = as.isDarkThemeEnabled() ? HTML001_HEAD_WITH_STYLE_DARK : HTML001_HEAD_WITH_STYLE_LIGHT;
        if (as.isRenderRtl()) {
            html += HTML002_RIGHT_TO_LEFT;
        }
        html += HTML010_BODY;

        // Default font is set by css in line 1 of generated html
        html = html.replaceFirst("sans-serif-light", as.getFontFamily());

        html += content;
        html += HTML990_BODY_END;

        return html;
    }

    protected String getContentType() {
        return CONTENT_TYPE_HTML;
    }
}
