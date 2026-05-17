/*#######################################################
 *
 *   Maintained 2017-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.web;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.webkit.WebView;

import net.gsantner.markor.activity.DocumentActivity;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.markor.util.MarkorContextUtils;
import net.gsantner.opoc.format.GsTextUtils;
import net.gsantner.opoc.web.GsWebViewClient;

import java.io.File;
import java.net.URLDecoder;

public class MarkorWebViewClient extends GsWebViewClient {
    protected final Activity _activity;

    public MarkorWebViewClient(final WebView webView, final Activity activity) {
        super(webView);
        _activity = activity;
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
                final Uri uri = Uri.parse(url);
                final String path = URLDecoder.decode(url.replace("file://", "").replace("+", "%2B"));
                final String line = uri.getQueryParameter("line");
                final Integer lineNumber = line != null ? GsTextUtils.tryParseInt(line, -1) : null;
                final String preview = uri.getQueryParameter("preview");
                final Boolean doPreview = preview != null ? Boolean.parseBoolean(preview) : null;
                File file = new File(path);
                final String filePath = file.getAbsolutePath().replaceFirst("[#?].*$", "");
                for (String str : new String[]{file.getAbsolutePath(), filePath, filePath + ".md", filePath + ".txt"}) {
                    File f = new File(str);
                    if (f.exists()) {
                        file = f;
                        break;
                    }
                }
                DocumentActivity.launch(_activity, file, doPreview, lineNumber);
            } else {
                MarkorContextUtils su = new MarkorContextUtils(_activity);
                AppSettings settings = AppSettings.get(_activity);
                if (!settings.isOpenLinksWithChromeCustomTabs() || (settings.isOpenLinksWithChromeCustomTabs() && !su.openWebpageInChromeCustomTab(context, url))) {
                    su.openWebpageInExternalBrowser(context, url);
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return true;
    }
}
