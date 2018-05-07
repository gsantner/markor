/*
 * Copyright (c) 2017-2018 Gregor Santner
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
    protected static final String CONTENT_TYPE_HTML = "text/html";
    protected static final String CONTENT_TYPE_PLAIN = "text/plain";

    protected static final String CSS_S = "<style type=\"text/css\">";
    protected static final String CSS_E = "</style>";

    protected static final String TOKEN_TEXT_DIRECTION = "{% app.TEXT_DIRECTION %}";
    protected static final String TOKEN_FONT = "{% app.TEXT_FONT %}";
    protected static final String TOKEN_BW_INVERSE_OF_THEME = "{% app.TOKEN_BW_INVERSE_OF_THEME %}";

    protected static final String HTML001_HEAD_WITH_BASESTYLE = "<html><head>" + CSS_S + "html,body{padding:4px 8px 4px 8px;font-family:'" + TOKEN_FONT + "';}h1,h2,h3,h4,h5,h6{font-family:'sans-serif-condensed';}a{color:#388E3C;text-decoration:underline;}img{height:auto;width:325px;margin:auto;}" + CSS_E;
    protected static final String HTML002_HEAD_WITH_STYLE_LIGHT = CSS_S + "html,body{color:#303030;}blockquote{color:#73747d;}" + CSS_E;
    protected static final String HTML002_HEAD_WITH_STYLE_DARK = CSS_S + "html,body{color:#ffffff;background-color:#303030;}a:visited{color:#dddddd;}blockquote{color:#cccccc;}" + CSS_E;
    protected static final String HTML003_RIGHT_TO_LEFT = CSS_S + "body{text-align:" + TOKEN_TEXT_DIRECTION + ";direction:rtl;}" + CSS_E;
    protected static final String HTML100_HEADER_WITHOUT_UNDERLINE = CSS_S + ".header_no_underline { text-decoration: none; color: " + TOKEN_BW_INVERSE_OF_THEME + "; }" + CSS_E;
    protected static final String HTML101_BLOCKQUOTE_VERTICAL_LINE = CSS_S + "blockquote{padding:0px 14px;border-" + TOKEN_TEXT_DIRECTION + ":3.5px solid #dddddd;margin:4px 0}" + CSS_E;
    protected static final String HTML500_BODY = "</head><body>\n\n\n";
    protected static final String HTML990_BODY_END = "</body></html>";

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
        String html = HTML001_HEAD_WITH_BASESTYLE + (as.isDarkThemeEnabled() ? HTML002_HEAD_WITH_STYLE_DARK : HTML002_HEAD_WITH_STYLE_LIGHT);
        if (as.isRenderRtl()) {
            html += HTML003_RIGHT_TO_LEFT;
        }
        html += HTML100_HEADER_WITHOUT_UNDERLINE + HTML101_BLOCKQUOTE_VERTICAL_LINE;

        // Remove duplicate style blocks
        html = html.replace(CSS_E + CSS_S, "");

        // Load content
        html += HTML500_BODY;
        html += content;
        html += HTML990_BODY_END;

        // Replace tokens
        html = html
                .replace(TOKEN_BW_INVERSE_OF_THEME, as.isDarkThemeEnabled() ? "white" : "black")
                .replace(TOKEN_TEXT_DIRECTION, as.isRenderRtl() ? "right" : "left")
                .replace(TOKEN_FONT, as.getFontFamily())
        ;

        return html;
    }

    protected String getContentType() {
        return CONTENT_TYPE_HTML;
    }
}
