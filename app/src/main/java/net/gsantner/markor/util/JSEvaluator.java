package net.gsantner.markor.util;

import android.annotation.SuppressLint;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;

import net.gsantner.markor.ApplicationObject;
import net.gsantner.opoc.wrapper.GsCallback;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class exists to provide a singleton WebView and some utilities
 * for evaluating JavaScript.
 *
 * The WebView is a singleton as inflating it repeatedly can cause perf issues.
 */
public class JSEvaluator {
    @SuppressLint("StaticFieldLeak")
    private static JSEvaluator instance = null; // Not a problem as we use applicationContext

    public static JSEvaluator getInstance() {
        if (instance == null) {
            instance = new JSEvaluator();
        }
        return instance;
    }

    // ----------------------------------------------

    private final WebView webView;

    private static final Pattern quotedString = Pattern.compile("\"(.*)\"");

    @SuppressLint("SetJavaScriptEnabled")
    private JSEvaluator() {
        webView = new WebView(ApplicationObject.get().getApplicationContext());
        final WebSettings settings = webView.getSettings();
        settings.setDatabaseEnabled(false);
        settings.setGeolocationEnabled(false);
        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
    }

    public JSEvaluator clearState() {
        webView.loadUrl("about:blank");
        return this;
    }

    public void eval(final String code, final GsCallback.a1<String> callback) {
        try {
            webView.evaluateJavascript(code, res -> {
                if (res != null) {
                    final Matcher m = quotedString.matcher(res);
                    if (m.matches()) {
                        callback.callback(m.group(1));
                    } else {
                        callback.callback(res);
                    }
                }
            });
        } catch (final Exception exception) {
            Log.i(JSEvaluator.class.getName(), exception.toString());
        }
    }
}
