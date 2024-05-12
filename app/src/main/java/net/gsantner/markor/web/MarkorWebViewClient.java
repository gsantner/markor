/*#######################################################
 *
 *   Maintained 2017-2023 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.web;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.webkit.WebView;

import net.gsantner.markor.ApplicationObject;
import net.gsantner.markor.activity.DocumentActivity;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.markor.util.JSFileInterface;
import net.gsantner.markor.util.MarkorContextUtils;
import net.gsantner.opoc.web.GsWebViewClient;

import java.io.File;
import java.net.URLDecoder;

public class MarkorWebViewClient extends GsWebViewClient {
    protected final Activity _activity;
    private JSFileInterface _jsFileInterface = null;

    private final WebView _webView;

    public MarkorWebViewClient(final WebView webView, final Activity activity) {
        super(webView);
        _activity = activity;
        _webView = webView;
    }

    public MarkorWebViewClient setJsFileInterface(final JSFileInterface jsFileInterface) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            _jsFileInterface = jsFileInterface;
            final String name = _jsFileInterface.getClass().getSimpleName();
            _webView.removeJavascriptInterface(name);
            _webView.addJavascriptInterface(_jsFileInterface, name);
        }
        return this;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        try {
            Context context = view.getContext();

            if (url.equals("about:blank")) {
                view.reload();
                return true;
            }
            if (url.startsWith("file:///android_asset/")) {
                return false;
            } else if (url.startsWith("file://")) {
                MarkorContextUtils su = new MarkorContextUtils(view.getContext());
                File file = new File(URLDecoder.decode(url.replace("file://", "").replace("+", "%2B")));
                for (String str : new String[]{file.getAbsolutePath(), file.getAbsolutePath().replaceFirst("[#].*$", ""), file.getAbsolutePath() + ".md", file.getAbsolutePath() + ".txt"}) {
                    File f = new File(str);
                    if (f.exists()) {
                        file = f;
                        break;
                    }
                }
                DocumentActivity.handleFileClick(_activity, file, null);
            } else {
                MarkorContextUtils su = new MarkorContextUtils(_activity);
                AppSettings settings = ApplicationObject.settings();
                if (!settings.isOpenLinksWithChromeCustomTabs() || (settings.isOpenLinksWithChromeCustomTabs() && !su.openWebpageInChromeCustomTab(context, url))) {
                    su.openWebpageInExternalBrowser(context, url);
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return true;
    }

    @Override
    public void onPageStarted(final WebView view, final String url, final Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        if (_jsFileInterface != null) {
            _jsFileInterface.setEnabled(url.startsWith("file://"));
        }
    }

    @Override
    public void onPageFinished(final WebView view, final String url) {
        super.onPageFinished(view, url);
        if (_jsFileInterface != null) {
            _jsFileInterface.setEnabled(url.startsWith("file://"));
        }
    }
}
