/*#######################################################
 *
 *   Maintained 2018-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.text.format.DateFormat;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import net.gsantner.markor.ApplicationObject;
import net.gsantner.markor.R;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.markor.model.Document;
import net.gsantner.opoc.format.GsTextUtils;
import net.gsantner.opoc.util.GsContextUtils;
import net.gsantner.opoc.util.GsFileUtils;

import java.io.File;
import java.util.Date;
import java.util.Locale;

import other.de.stanetz.jpencconverter.JavaPasswordbasedCryption;

@SuppressWarnings("WeakerAccess")
public abstract class TextConverterBase {
    //########################
    //## HTML
    //########################
    protected static final String UTF_CHARSET = "utf-8";
    protected static final String CONTENT_TYPE_HTML = "text/html";
    protected static final String CONTENT_TYPE_PLAIN = "text/plain";

    protected static final String CSS_S = "<style type='text/css'>";
    protected static final String CSS_E = "</style>";
    protected static final String JS_S = "<script>";
    protected static final String JS_E = "</script>";

    protected static final String TOKEN_TEXT_DIRECTION = "{{ app.text_direction }}"; // this is either 'right' or 'left'
    protected static final String TOKEN_FONT = "{{ app.text_font }}";
    protected static final String TOKEN_BW_INVERSE_OF_THEME = "{{ app.token_bw_inverse_of_theme }}";
    protected static final String TOKEN_BW_INVERSE_OF_THEME_HEADER_UNDERLINE = "{{ app.token_headline_underline_inverse_of_theme }}";
    protected static final String TOKEN_COLOR_GREY_OF_THEME = "{{ app.token_color_grey_inverse_of_theme }}";
    protected static final String TOKEN_LINK_COLOR = "{{ app.token_link_color }}";
    protected static final String TOKEN_ACCENT_COLOR = "{{ app.token_accent_color }}";
    protected static final String TOKEN_TEXT_CONVERTER_CSS_CLASS = "{{ post.text_converter_name }}";
    protected static final String TOKEN_TEXT_CONVERTER_MAX_ZOOM_OUT_BY_DEFAULT = "{{ app.webview_max_zoom_out_by_default }}";
    protected static final String TOKEN_POST_TODAY_DATE = "{{ post.date_today }}";
    protected static final String TOKEN_POST_LANG = "{{ post.lang }}";
    protected static final String TOKEN_FILEURI_VIEWED_FILE = "{{ app.fileuri_viewed_file }}";

    protected static final String HTML_DOCTYPE = "<!DOCTYPE html>";
    protected static final String HTML001_HEAD_WITH_BASESTYLE = "<html lang='" + TOKEN_POST_LANG + "'><head>" + CSS_S + "html,body{padding:4px 8px 4px 8px;font-family:'" + TOKEN_FONT + "';}h1,h2,h3,h4,h5,h6{font-family:'sans-serif-condensed';}a{color: " + TOKEN_LINK_COLOR + ";text-decoration:underline;}img{height:auto;max-width:100%;max-height: 90vh;margin:auto;}" + CSS_E;
    protected static final String HTML002_HEAD_WITH_STYLE_LIGHT = CSS_S + "html,body{color:#303030;}blockquote{color:#73747d;}" + CSS_E;
    protected static final String HTML002_HEAD_WITH_STYLE_DARK = CSS_S + "html,body{color:#ffffff;background-color:#303030;}a:visited{color:#dddddd;}blockquote{color:#cccccc;}" + CSS_E;
    protected static final String HTML003_RIGHT_TO_LEFT = CSS_S + "body{text-align:" + TOKEN_TEXT_DIRECTION + ";direction:rtl;}" + CSS_E;
    protected static final String HTML004_HEAD_META_VIEWPORT_MOBILE = "<style>video, img { max-width: 100%; } pre { max-width: 100%; overflow: auto; } </style>";//"<meta name='viewport' content='width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no'>";
    protected static final String HTML100_PERCENT_IN_FILEPATH = "<base>" + JS_S + "var newbase = document.baseURI.split('%').join('%25'); document.querySelector('base').setAttribute('href', newbase);" + JS_E;
    protected static final String CSS_TABLE_STYLE = CSS_S + "table, th, td {  border: 1px solid " + TOKEN_BW_INVERSE_OF_THEME + "; border-collapse: collapse; border-spacing: 0; padding: 6px; }" + CSS_E;
    protected static final String CSS_BUTTON_STYLE_MATERIAL = CSS_S + "button:hover,button:active {filter: invert(1);} button { display: inline-block; box-sizing: border-box; border: none; border-radius: 4px; padding: 0 16px; min-width: 64px; height: 36px; font-family: 'Roboto'; font-size: 14px; font-weight: 500;  line-height: 36px; overflow: hidden; outline: none; vertical-align: middle; text-align: center; text-overflow: ellipsis; text-transform: uppercase; box-shadow: 0 3px 1px -2px rgba(0, 0, 0, 0.2), 0 2px 2px 0 rgba(0, 0, 0, 0.14), 0 1px 5px 0 rgba(0, 0, 0, 0.12); margin: 4px 4px 8px 0px;}   " + CSS_E;
    protected static final String CSS_BUTTON_STYLE_EMOJIBTN = CSS_S + " .emojibtn,.fa {font-size:250%; background: transparent; padding: 0px; min-width:0px;}   " + CSS_E;
    protected static final String CSS_CLASS_STICKY = CSS_S + " .sticky {position: sticky; display: inline-block; border: 0px solid " + TOKEN_BW_INVERSE_OF_THEME + ";} " + CSS_E;
    protected static final String CSS_CLASS_FLOAT = CSS_S + " .floatl {float: left;} .clear {clear:both;} " + CSS_E;

    // onPageLoaded_markor_private() invokes the user injected function onPageLoaded()
    protected static final String HTML500_BODY = "</head>\n<body class='" + TOKEN_TEXT_CONVERTER_CSS_CLASS + "' onload='onPageLoaded_markor_private();'>\n\n<!-- USER DOCUMENT CONTENT -->\n\n\n";
    //protected static final String HTML900_TO_TOP = "<a class='back_to_top'>&uarr;</a>"
    //        + CSS_S + ".back_to_top { position: fixed; bottom: 80px; right: 40px; z-index: 9999; width: 30px; height: 30px; text-align: center; line-height: 30px; background: #f5f5f5; color: #444; cursor: pointer; border-radius: 2px; display: none; } .back_to_top:hover { background: #e9ebec; } .back_to_top-show { display: block; }" +CSS_E
    //        + "<script>" + "(function() { 'use strict'; function trackScroll() { var scrolled = window.pageYOffset; var coords = document.documentElement.clientHeight; if (scrolled > coords) { goTopBtn.classList.add('back_to_top-show'); } if (scrolled < coords) { goTopBtn.classList.remove('back_to_top-show'); } } function backToTop() { if (window.pageYOffset > 0) { window.scrollBy(0, -80); setTimeout(backToTop, 0); } } var goTopBtn = document.querySelector('.back_to_top'); window.addEventListener('scroll', trackScroll); goTopBtn.addEventListener('click', backToTop); })();" + "</script>";
    protected static final String HTML990_BODY_END = "\n\n<!-- USER DOCUMENT CONTENT END -->\n\n</body></html>";

    protected static final String HTML_ON_PAGE_LOAD_S = "<script> function onPageLoaded_markor_private() {\n";
    protected static final String HTML_ON_PAGE_LOAD_E = "\nonPageLoaded(); }\n</script>";

    // protected static final String HTML_JQUERY_INCLUDE = "<script src='file:///android_asset/jquery/jquery-3.3.1.min.js'></script>"; // currently not bundled

    //########################
    //## Methods
    //########################
    protected final AppSettings _appSettings;

    public TextConverterBase() {
        _appSettings = ApplicationObject.settings();
    }

    /**
     * Convert markup to target format and show the result in a WebView
     *
     * @param document The document containing the contents
     * @param webView  The WebView content to be shown in
     * @return Copy of converted html
     */
    public String convertMarkupShowInWebView(
            final Document document,
            final String content,
            final Activity context,
            final WebView webView,
            final boolean lightMode,
            final boolean lineNum) {
        String html;
        try {
            html = convertMarkup(content, context, lightMode, lineNum, document.getFile());
        } catch (Exception e) {
            html = "Please report at project issue tracker: " + e;
        }

        String parent = document.getFile().getParent();
        if (parent == null) {
            parent = _appSettings.getNotebookDirectory().getAbsolutePath();
        }
        final String baseFolder = "file://" + parent + "/";

        webView.loadDataWithBaseURL(baseFolder, html, getContentType(), UTF_CHARSET, null);

        // When TOKEN_TEXT_CONVERTER_MAX_ZOOM_OUT_BY_DEFAULT is contained in text zoom out as far possible
        // Notice: overViewMode / useWideViewPort work differently
        for (int i = (html.contains(TOKEN_TEXT_CONVERTER_MAX_ZOOM_OUT_BY_DEFAULT) ? 0 : 99); i < 30; i++) {
            webView.postDelayed(webView::zoomOut, 210 * (i < 5 ? 1 : (i < 10 ? 2 : (i < 15 ? 3 : (i < 20 ? 5 : 9)))));
        }

        return html;
    }

    /**
     * Convert markup text to target format
     *
     * @param markup    Markup text
     * @param context   Android Context
     * @param lightMode
     * @param lineNum
     * @return html as String
     */
    public abstract String convertMarkup(String markup, Context context, boolean lightMode, boolean lineNum, File file);

    protected String putContentIntoTemplate(Context context, String content, boolean isExportInLightMode, File file, String onLoadJs, String head) {
        final String contentLower = content.toLowerCase();
        boolean darkTheme = GsContextUtils.instance.isDarkModeEnabled(context) && !isExportInLightMode;
        String html = HTML_DOCTYPE + HTML001_HEAD_WITH_BASESTYLE.replace(TOKEN_POST_LANG, Locale.getDefault().getLanguage()) + (darkTheme ? HTML002_HEAD_WITH_STYLE_DARK : HTML002_HEAD_WITH_STYLE_LIGHT);
        if (isExportInLightMode) {
            html = html.replace("html,body{color:#303030;}", "html,body{color: black !important; background-color: white !important;}");
        }
        html += HTML004_HEAD_META_VIEWPORT_MOBILE + CSS_TABLE_STYLE + CSS_CLASS_FLOAT + CSS_BUTTON_STYLE_MATERIAL + CSS_BUTTON_STYLE_EMOJIBTN + CSS_CLASS_STICKY;
        if (_appSettings.isRenderRtl()) {
            html += HTML003_RIGHT_TO_LEFT;
        }

        html += head + _appSettings.getInjectedHeader();

        html += HTML_ON_PAGE_LOAD_S + onLoadJs + HTML_ON_PAGE_LOAD_E;

        // Add custom font css if font is a filepath, swap path with new font-family
        String font = _appSettings.getFontFamily();
        if (font.startsWith("/")) {
            html += CSS_S + "@font-face { font-family: customfont; src: url('file://" + font + "'); }" + CSS_E;
            font = "customfont";
        }

        // Remove duplicate style blocks
        html = html.replace(CSS_E + CSS_S, "").replace(CSS_E + "\n" + CSS_S, "");

        // Options based on filepath
        if (file != null) {
            if (file.getAbsolutePath().contains("%") || ((contentLower.contains(".nextcloud") || contentLower.contains(".owncloud")) && (contentLower.contains("%2") || contentLower.contains("%4")))) {
                html += HTML100_PERCENT_IN_FILEPATH;
            }
        }

        // Load content
        html += HTML500_BODY;
        html += _appSettings.getInjectedBody();
        html += content;
        html += HTML990_BODY_END;

        // Replace tokens
        html = html
                .replace(TOKEN_BW_INVERSE_OF_THEME, darkTheme ? "white" : "black")
                .replace(TOKEN_BW_INVERSE_OF_THEME_HEADER_UNDERLINE, darkTheme ? "#eaecef" : "#696969")
                .replace(TOKEN_COLOR_GREY_OF_THEME, darkTheme ? "#393939" : GsTextUtils.colorToHexString(ContextCompat.getColor(context, R.color.lighter_grey)))
                .replace(TOKEN_LINK_COLOR, _appSettings.getViewModeLinkColor())
                .replace(TOKEN_ACCENT_COLOR, GsTextUtils.colorToHexString(ContextCompat.getColor(context, R.color.accent)))
                .replace(TOKEN_TEXT_DIRECTION, _appSettings.isRenderRtl() ? "right" : "left")
                .replace(TOKEN_FONT, font)
                .replace(TOKEN_TEXT_CONVERTER_CSS_CLASS, "format-" + getClass().getSimpleName().toLowerCase().replace("textconverter", "").replace("converter", "") + " fileext-" + GsFileUtils.getFilenameExtension(file).replace(".", ""))
                .replace(TOKEN_POST_TODAY_DATE, DateFormat.getDateFormat(context).format(new Date()))
                .replace(TOKEN_FILEURI_VIEWED_FILE, (file != null ? Uri.fromFile(file.getAbsoluteFile()).toString() : "file:///dummy").replace("'", "\\'").replace("\"", "\\\""));

        return html;
    }

    protected String getContentType() {
        return CONTENT_TYPE_HTML;
    }

    public boolean isFileOutOfThisFormat(final @NonNull File file) {
        final String name = file.getName().toLowerCase().replace(JavaPasswordbasedCryption.DEFAULT_ENCRYPTION_EXTENSION, "");
        final String ext = name.replaceAll(".*\\.", ".");
        return isFileOutOfThisFormat(file, name, ext);
    }

    protected abstract boolean isFileOutOfThisFormat(final File file, final String name, final String ext);
}
