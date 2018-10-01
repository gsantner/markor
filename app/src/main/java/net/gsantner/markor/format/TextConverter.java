/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format;

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

    protected static final String CSS_S = "<style type='text/css'>";
    protected static final String CSS_E = "</style>";

    protected static final String TOKEN_TEXT_DIRECTION = "{{ app.text_direction }}"; // this is either 'right' or 'left'
    protected static final String TOKEN_FONT = "{{ app.text_font }}";
    protected static final String TOKEN_BW_INVERSE_OF_THEME = "{{ app.token_bw_inverse_of_theme }}";
    protected static final String TOKEN_TEXT_CONVERTER_NAME = "{{ post.text_converter_name }}";

    protected static final String HTML_DOCTYPE = "<!DOCTYPE html>";
    protected static final String HTML001_HEAD_WITH_BASESTYLE = "<html><head>" + CSS_S + "html,body{padding:4px 8px 4px 8px;font-family:'" + TOKEN_FONT + "';}h1,h2,h3,h4,h5,h6{font-family:'sans-serif-condensed';}a{color:#388E3C;text-decoration:underline;}img{height:auto;width:325px;margin:auto;}" + CSS_E;
    protected static final String HTML002_HEAD_WITH_STYLE_LIGHT = CSS_S + "html,body{color:#303030;}blockquote{color:#73747d;}" + CSS_E;
    protected static final String HTML002_HEAD_WITH_STYLE_DARK = CSS_S + "html,body{color:#ffffff;background-color:#303030;}a:visited{color:#dddddd;}blockquote{color:#cccccc;}" + CSS_E;
    protected static final String HTML003_RIGHT_TO_LEFT = CSS_S + "body{text-align:" + TOKEN_TEXT_DIRECTION + ";direction:rtl;}" + CSS_E;

    // onPageLoaded_markor_private() invokes the user injected function onPageLoaded()
    protected static final String HTML500_BODY = "</head>\n<body onload='onPageLoaded_markor_private();'>\n\n<!-- USER CONTENT -->\n\n\n";
    protected static final String HTML990_BODY_END = "\n\n<!-- USER CONTENT END -->\n</body></html>";

    protected static final String HTML_ON_PAGE_LOAD_S = "<script> function onPageLoaded_markor_private() {\n";
    protected static final String HTML_ON_PAGE_LOAD_E = "\nonPageLoaded(); }\n</script>";

    // protected static final String HTML_JQUERY_INCLUDE = "<script src='file:///android_asset/jquery/jquery-3.3.1.min.js'></script>"; // currently not bundled


    //########################
    //## Methods
    //########################

    /**
     * Convert markup to target format and show the result in a WebView
     *
     * @param document The document containing the contents
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

    protected String putContentIntoTemplate(Context context, String content, String onLoadJs, String head) {
        AppSettings appSettings = new AppSettings(context);
        String html = HTML_DOCTYPE + HTML001_HEAD_WITH_BASESTYLE + (appSettings.isDarkThemeEnabled() ? HTML002_HEAD_WITH_STYLE_DARK : HTML002_HEAD_WITH_STYLE_LIGHT);
        if (appSettings.isRenderRtl()) {
            html += HTML003_RIGHT_TO_LEFT;
        }

        html += head + appSettings.getInjectedHeader();

        html += HTML_ON_PAGE_LOAD_S + onLoadJs + HTML_ON_PAGE_LOAD_E;

        // Remove duplicate style blocks
        html = html.replace(CSS_E + CSS_S, "").replace(CSS_E + "\n" + CSS_S, "");

        // Load content
        html += HTML500_BODY;
        html += appSettings.getInjectedBody();
        html += content;
        html += HTML990_BODY_END;

        // Replace tokens
        html = html
                .replace(TOKEN_BW_INVERSE_OF_THEME, appSettings.isDarkThemeEnabled() ? "white" : "black")
                .replace(TOKEN_TEXT_DIRECTION, appSettings.isRenderRtl() ? "right" : "left")
                .replace(TOKEN_FONT, appSettings.getFontFamily())
                .replace(TOKEN_TEXT_CONVERTER_NAME, getClass().getSimpleName())
        ;

        return html;
    }

    protected String getContentType() {
        return CONTENT_TYPE_HTML;
    }
}
