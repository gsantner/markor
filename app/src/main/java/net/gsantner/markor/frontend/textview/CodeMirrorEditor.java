package net.gsantner.markor.frontend.textview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.gsantner.opoc.util.GsContextUtils;
import net.gsantner.opoc.util.GsFileUtils;
import net.gsantner.opoc.wrapper.GsCallback;

import org.apache.commons.text.StringEscapeUtils;

import java.io.File;
import java.io.IOException;

public class CodeMirrorEditor extends WebView {

    private static final String BASE_DIR_URL = "file:///android_asset/cm-editor/";
    private static final String BASE_HTML_PATH = "cm-editor/index.html";
    private boolean initialized;

    private final WebViewClient webViewClient = new WebViewClient() {
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            setVisibility(VISIBLE);
            if (onPreparedListener != null) {
                onPreparedListener.callback();
            }
        }
    };

    private GsCallback.a0 onPreparedListener;

    public void setOnPreparedListener(GsCallback.a0 listener) {
        this.onPreparedListener = listener;
    }

    private class CallbackInterface {
        @JavascriptInterface
        public void focus() {
            CodeMirrorEditor.this.requestFocusFromTouch();
        }

        @JavascriptInterface
        public String readText(String path) {
            return GsFileUtils.readTextFile(new File(path));
        }
    }

    public CodeMirrorEditor(@NonNull Context context) {
        super(context);
        init();
    }

    public CodeMirrorEditor(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CodeMirrorEditor(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        if (!initialized) {
            setFocusable(true);
            setFocusableInTouchMode(true);
            requestFocus(View.FOCUS_DOWN);
            load();
            initialized = true;
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void load() {
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

    public void focus() {
        loadUrl("javascript: editorBridge.focus()");
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

    /**
     * Set text but not reset state.
     * This method is suitable for loading text with file size less than 500 KB.
     * Load large text, please use {@link net.gsantner.markor.frontend.textview.CodeMirrorEditor#loadText(java.lang.String path)}.
     *
     * @param text the text
     */
    public void setText(String text) {
        loadUrl("javascript: editorBridge.setText(\"" + StringEscapeUtils.escapeJava(text) + "\")");
    }

    /**
     * Load text from file path and reset state.
     * This method supports loading large text with file size greater than 1 MB.
     *
     * @param path the text file path
     */
    public void loadText(String path) {
        loadUrl("javascript: editorBridge.loadText(\"" + StringEscapeUtils.escapeJava(path) + "\")");
    }

    /**
     * Set text and reset state.
     * This method is suitable for loading text with file size less than 500 KB.
     * Load large text, please use {@link net.gsantner.markor.frontend.textview.CodeMirrorEditor#loadText(java.lang.String path)}.
     *
     * @param text the text
     */
    public void resetText(String text) {
        loadUrl("javascript: editorBridge.resetText(\"" + StringEscapeUtils.escapeJava(text) + "\")");
    }

    public void undo() {
        loadUrl("javascript: editorBridge.undo()");
    }

    public void redo() {
        loadUrl("javascript: editorBridge.redo()");
    }
}
