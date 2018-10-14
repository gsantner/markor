/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.util;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import net.gsantner.markor.R;
import net.gsantner.markor.activity.DocumentActivity;
import net.gsantner.markor.format.markdown.MarkdownTextConverter;

import java.io.File;
import java.net.URLDecoder;
import java.util.concurrent.ExecutionException;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

public class MarkorWebViewClient extends WebViewClient {

    private final Activity _activity;
    private WebView webView;

    public MarkorWebViewClient(Activity activity,WebView webView) {
        _activity = activity;
        this.webView = webView;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        System.out.println(url);
        if (url.startsWith("file:///android_asset/")) {
            return false;
        } else if (url.startsWith("file://")) {
            ShareUtil su = new ShareUtil(view.getContext());
            File file = new File(URLDecoder.decode(url.replace("file://", "")));
            String mimetype;
            if (MarkdownTextConverter.isTextOrMarkdownFile(file)) {
                Intent newPreview = new Intent(_activity, DocumentActivity.class);
                newPreview.putExtra(DocumentIO.EXTRA_PATH, file);
                newPreview.putExtra(DocumentActivity.EXTRA_DO_PREVIEW, true);
                _activity.startActivity(newPreview);
            } else if ((mimetype = ContextUtils.getMimeType(url)) != null) {
                su.viewFileInOtherApp(file, mimetype);
            } else {
                su.viewFileInOtherApp(file, null);
            }
        } else if (url.startsWith("http")){
            // inflate the layout of the popup window
            LayoutInflater inflater = (LayoutInflater) _activity.getSystemService(LAYOUT_INFLATER_SERVICE);
            View popupView = inflater.inflate(R.layout.popup_window, webView,false);

            // create the popup window
            int width = LinearLayout.LayoutParams.WRAP_CONTENT;
            int height = LinearLayout.LayoutParams.WRAP_CONTENT;
            boolean focusable = true; // lets taps outside the popup also dismiss it
            final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);

            // show the popup window
            // which view you pass in doesn't matter, it is only used for the window tolken
            popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);
            // dismiss the popup window when touched
            popupView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    popupWindow.dismiss();
                    return true;
                }
            });
            ImageView imageview = popupView.findViewById(R.id.imageview_url);
            if (imageview != null){
                new DownloadImageTask((ImageView) imageview)
                        .execute(url);
            } else {
                System.out.println("CI PANA JE TAM NULL");
            }
        } else {
            ContextUtils cu = new ContextUtils(_activity.getApplicationContext());
            ShareUtil su = new ShareUtil(view.getContext());
            try {
                // Use a CustomTabsIntent.Builder to configure CustomTabsIntent.
                // Once ready, call CustomTabsIntent.Builder.build() to create a CustomTabsIntent
                // and launch the desired Url with CustomTabsIntent.launchUrl()
                CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                builder.setToolbarColor(ContextCompat.getColor(_activity, R.color.primary));
                builder.setSecondaryToolbarColor(ContextCompat.getColor(_activity, R.color.primary_dark));
                builder.addDefaultShareMenuItem();
                CustomTabsIntent customTabsIntent = builder.build();
                su.enableChromeCustomTabsForOtherBrowsers(customTabsIntent.intent);
                customTabsIntent.launchUrl(_activity, Uri.parse(url));
            } catch (Exception e) {
                cu.openWebpageInExternalBrowser(url);
            }
        }
        return true;
    }
}
