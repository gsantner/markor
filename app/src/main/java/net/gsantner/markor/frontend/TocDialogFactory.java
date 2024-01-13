package net.gsantner.markor.frontend;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import net.gsantner.markor.R;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;

public class TocDialogFactory {
    private static final ArrayDeque<TocDataHolder> deque = new ArrayDeque<>(); // Cache
    public static final int DEQUE_SIZE = 3;

    public static void showTocDialog(@NotNull final Activity activity, @NotNull final Context context, @NotNull final WebView documentWebView) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return;
        }

        TocDataHolder temp = null;
        for (TocDataHolder tocDataHolder : deque) {
            temp = tocDataHolder;
            if (!documentWebView.equals(temp.documentWebView)) {
                temp = null;
            } else {
                break;
            }
        }

        if (temp == null) {
            resize();
            temp = new TocDataHolder();
            temp.documentWebView = documentWebView;
            deque.add(temp);
        }

        final TocDataHolder holder = temp;
        configureTocWebView(holder, activity, context, documentWebView);
        if (!holder.loaded) {
            holder.loaded = true;
            documentWebView.evaluateJavascript("javascript: generate()", result -> {
                if (result.length() < 3) {
                    Toast.makeText(activity, R.string.no_table_of_contents, Toast.LENGTH_SHORT).show();
                    return;
                }
                result = result.replaceAll("\\\\u003C", "<");
                result = result.replaceAll("\\\\", "");
                result = result.substring(1, result.length() - 1);
                // Load TOC page
                holder.tocWebView.loadDataWithBaseURL(null, result, "text/html;charset=utf-8", "utf-8", null);
            });
        }

        documentWebView.evaluateJavascript("javascript: locate()", result -> {
            if (result.length() < 1) {
                return;
            }
            result = result.replaceAll("\\\\u003C", "<");
            result = result.replaceAll("\\\\", "");
            holder.tocWebView.loadUrl("javascript:highlightById(" + result + ")");
        });

        if (holder.dialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(R.string.table_of_contents);
            if (holder.tocWebView.getParent() != null) {
                ((ViewGroup) holder.tocWebView.getParent()).removeView(holder.tocWebView);
            }
            builder.setView(holder.tocWebView);
            holder.dialog = builder.create();
        }

        holder.dialog.show();
        WindowManager.LayoutParams params = holder.dialog.getWindow().getAttributes();
        final int height = activity.getResources().getDisplayMetrics().heightPixels;
        final int width = activity.getResources().getDisplayMetrics().widthPixels;
        if (width > height) {
            params.width = (int) (width * 0.7);
        } else {
            params.height = (int) (height * 0.75);
        }
        holder.dialog.getWindow().setAttributes(params);
    }

    private static void configureTocWebView(TocDataHolder holder, final Activity activity, final Context context, WebView documentWebView) {
        if (holder.configured) {
            return;
        }

        if (holder.tocWebView == null) {
            holder.tocWebView = new WebView(context);
        }

        holder.tocWebView.getSettings().setJavaScriptEnabled(true);
        holder.tocWebView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void run(String param) {
                activity.runOnUiThread(() -> documentWebView.loadUrl("javascript:document.getElementById('" + param.substring(1) + "').scrollIntoView();"));
            }
        }, "injectedObject");

        holder.configured = true;
    }

    private static void resize() {
        if (deque.size() > DEQUE_SIZE) {
            deque.remove();
            deque.remove();
        }
    }

    static class TocDataHolder {
        public WebView tocWebView;
        public WebView documentWebView;
        public AlertDialog dialog;
        public boolean loaded;
        public boolean configured;
    }
}
