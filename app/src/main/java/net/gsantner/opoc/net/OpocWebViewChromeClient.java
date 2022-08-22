/*#######################################################
 *
 * SPDX-FileCopyrightText: 2022-2022 Gregor Santner <https://gsantner.net/>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
#########################################################*/
package net.gsantner.opoc.net;

import android.app.Activity;
import android.os.Build;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.FrameLayout;

import net.gsantner.opoc.activity.GsActivityBase;

import java.lang.ref.WeakReference;


/**
 * A {@link WebChromeClient} to support showing videos in fullscreen (and potentially other upcoming features)
 */
@SuppressWarnings("rawtypes")
public class OpocWebViewChromeClient extends WebChromeClient {
    private final WeakReference<FrameLayout> m_fullscreenContainer;
    private final WeakReference<Activity> m_activity;
    private final WeakReference<WebView> m_webView;

    private WeakReference<View> m_receivedFullscreenView;
    private int m_previousSystemUiVisibilityFlags;

    /**
     * @param webView             Reference to {@link WebView}
     * @param activity            Reference to the {@link Activity}, host of webview and fullscreenContainer
     * @param fullscreenContainer Reference to a {@link FrameLayout} serving as overlay for fullscreen content - automatically shown/hidden
     */
    public OpocWebViewChromeClient(WebView webView, Activity activity, FrameLayout fullscreenContainer) {
        m_fullscreenContainer = new WeakReference<>(fullscreenContainer);
        m_activity = new WeakReference<>(activity);
        m_webView = new WeakReference<>(webView);
    }

    @Override
    public void onShowCustomView(final View receivedFullscreenView, final CustomViewCallback callback) {
        if (m_receivedFullscreenView != null) {
            callback.onCustomViewHidden();
            return;
        }
        if (m_activity.get() == null || m_webView.get() == null || m_fullscreenContainer.get() == null) {
            return;
        }

        // Add WebView's generated view to layout
        m_receivedFullscreenView = new WeakReference<>(receivedFullscreenView);
        m_webView.get().loadUrl("javascript:(function() { document.body.style.overflowX = 'hidden'; window.scrollTo(0, 0); })();");
        m_fullscreenContainer.get().setVisibility(View.VISIBLE);
        m_fullscreenContainer.get().addView(receivedFullscreenView);

        // Hide/overlay other UI
        View dv = m_activity.get().getWindow().getDecorView();
        if (m_activity.get() instanceof GsActivityBase) {
            ((GsActivityBase) m_activity.get()).setToolbarVisible(false);
        }
        m_previousSystemUiVisibilityFlags = dv.getSystemUiVisibility();
        dv.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            dv.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }

    @Override
    public void onHideCustomView() {
        super.onHideCustomView();
        if (m_activity.get() == null || m_webView.get() == null || m_fullscreenContainer.get() == null || m_receivedFullscreenView == null || m_receivedFullscreenView.get() == null) {
            return;
        }

        // Remove/hide WebView custom added UI
        m_fullscreenContainer.get().removeView(m_receivedFullscreenView.get());
        m_fullscreenContainer.get().setVisibility(View.GONE);
        m_receivedFullscreenView = null;

        // Show UI again
        m_activity.get().getWindow().getDecorView().setSystemUiVisibility(m_previousSystemUiVisibilityFlags);
        if (m_activity.get() instanceof GsActivityBase) {
            ((GsActivityBase) m_activity.get()).setToolbarVisible(true);
        }
    }
}
