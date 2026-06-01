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
import java.util.ArrayList;
import java.util.List;

public class CodeMirrorEditor extends WebView {

    private static final String BASE_DIR_URL = "file:///android_asset/cm-editor/";
    private static final String BASE_HTML_PATH = "cm-editor/index.html";
    private boolean initialized;
    private boolean pageFinished;
    private final List<Runnable> pageFinishedTasks = new ArrayList<>();

    private final WebViewClient webViewClient = new WebViewClient() {
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            setVisibility(VISIBLE);
            pageFinished = true;
            if (!pageFinishedTasks.isEmpty()) {
                for (Runnable task : pageFinishedTasks) {
                    task.run();
                }
                pageFinishedTasks.clear();
            }
            if (onPreparedListener != null) {
                onPreparedListener.callback();
            }
        }
    };

    private GsCallback.a0 onPreparedListener;
    private OnTextChangedListener onTextChangedListener;

    public void setOnPreparedListener(GsCallback.a0 listener) {
        this.onPreparedListener = listener;
    }

    public void setOnTextChangedListener(OnTextChangedListener listener) {
        this.onTextChangedListener = listener;
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

        @JavascriptInterface
        public void onTextChanged(String newText, int undoDepth, int redoDepth) {
            if (onTextChangedListener != null) {
                // Log.i("AAA", undoDepth + ":" + redoDepth);
                onTextChangedListener.onTextChanged(newText, undoDepth, redoDepth);
            }
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

    public interface OnTextChangedListener {
        void onTextChanged(String newText, int undoDepth, int redoDepth);
    }

    private void execute(final String script) {
        final String url = "javascript:" + script;
        if (pageFinished) {
            loadUrl(url);
        } else {
            pageFinishedTasks.add(() -> loadUrl(url));
        }
    }

    private void execute(final String script, OnTextReadListener listener) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            final String url = "javascript:" + script;
            if (pageFinished) {
                evaluateJavascript(url, value ->
                        listener.onTextRead(StringEscapeUtils.unescapeJava(value.substring(1, value.length() - 1)))
                );
            } else {
                pageFinishedTasks.add(() -> evaluateJavascript(url, value ->
                        listener.onTextRead(StringEscapeUtils.unescapeJava(value))
                ));
            }
        }
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
        execute("editorBridge.setText(\"" + StringEscapeUtils.escapeJava(text) + "\")");
    }

    /**
     * Load text from file path and reset state.
     * This method supports loading large text with file size greater than 1 MB.
     *
     * @param path the text file path
     */
    public void loadText(String path) {
        execute("editorBridge.loadText(\"" + StringEscapeUtils.escapeJava(path) + "\")");
    }

    public void requestMeasure() {
        execute("editorBridge.requestMeasure()");
    }

    /**
     * Set text and reset state.
     * This method is suitable for loading text with file size less than 500 KB.
     * Load large text, please use {@link net.gsantner.markor.frontend.textview.CodeMirrorEditor#loadText(java.lang.String path)}.
     *
     * @param text the text
     */
    public void resetText(String text) {
        execute("editorBridge.resetText(\"" + StringEscapeUtils.escapeJava(text) + "\")");
    }

    public void undo() {
        loadUrl("javascript: editorBridge.undo()");
    }

    public void redo() {
        loadUrl("javascript: editorBridge.redo()");
    }

    public void setLineNumbers(boolean enabled) {
        execute("editorBridge.setLineNumbers(" + enabled + ")");
    }

    public void setLineWrapping(boolean enabled) {
        execute("editorBridge.setLineWrapping(" + enabled + ")");
    }

    public void insert(String text) {
        loadUrl("javascript: editorBridge.insertAtCursor(\"" + StringEscapeUtils.escapeJava(text) + "\")");
    }

    public void moveCursor(int distance) {
        loadUrl("javascript: editorBridge.moveCursor(" + distance + ")");
    }

    public void setCodeLanguage(String language) {
        execute("editorBridge.setCodeLanguage(\"" + language + "\")");
    }

    public void setFontSize(String fontSize) {
        execute("editorBridge.setFontSize(\"" + fontSize + "\")");
    }

    public void getUndoDepth(OnTextReadListener listener) {
        execute("editorBridge.getUndoDepth()", listener);
    }
}
