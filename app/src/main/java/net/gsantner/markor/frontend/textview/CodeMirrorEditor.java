package net.gsantner.markor.frontend.textview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.gsantner.opoc.util.GsContextUtils;
import net.gsantner.opoc.util.GsFileUtils;

import org.apache.commons.text.StringEscapeUtils;

import java.io.IOException;

public class CodeMirrorEditor extends WebView {

    private static final String BASE_DIR_URL = "file:///android_asset/cm-editor/dist/";
    private static final String BASE_HTML_PATH = "cm-editor/dist/index.html";

    private final WebViewClient webViewClient = new WebViewClient() {
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            setVisibility(VISIBLE);
            if (onPreparedListener != null) {
                onPreparedListener.onPageFinished();
            }
        }
    };

    public interface OnPreparedListener {
        void onPageFinished();
    }

    private OnPreparedListener onPreparedListener;

    public void setOnPreparedListener(OnPreparedListener onPreparedListener) {
        this.onPreparedListener = onPreparedListener;
    }

    private static class CallbackInterface {
        @JavascriptInterface
        public void callback(String msg) {
            Log.i(CallbackInterface.class.getName(), msg);
        }
    }

    public CodeMirrorEditor(@NonNull Context context) {
        super(context);
    }

    public CodeMirrorEditor(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CodeMirrorEditor(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        load();
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void load() {
        setVisibility(INVISIBLE);
        setWebViewClient(webViewClient);
        addJavascriptInterface(new CallbackInterface(), "callbackInterface");
        getSettings().setJavaScriptEnabled(true);
        getSettings().setAllowFileAccessFromFileURLs(true);

        try {
            String index = GsFileUtils.readText(getContext().getAssets().open(BASE_HTML_PATH));
            if (GsContextUtils.instance.isDarkModeEnabled(getContext())) {
                index = index.replace("content=\"light\"", "content=\"dark\"");
            }
            loadDataWithBaseURL(BASE_DIR_URL, index, "text/html", "utf-8", null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public interface OnTextReadListener {
        void onTextRead(String value);
    }

    public void getText(OnTextReadListener listener) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            evaluateJavascript("javascript: editorBridge.getText()", value ->
                    listener.onTextRead(StringEscapeUtils.unescapeJava(value.substring(1, value.length() - 1)))
            );
        }
    }

    public void setText(String text) {
        loadUrl("javascript: editorBridge.setText(\"" + StringEscapeUtils.escapeJava(text) + "\")");
    }

    public void undo() {
        loadUrl("javascript: editorBridge.undo()");
    }

    public void redo() {
        loadUrl("javascript: editorBridge.redo()");
    }
}
