/*#######################################################
 *
 * SPDX-FileCopyrightText: 2022-2024 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2022-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
#########################################################*/
package net.gsantner.opoc.web;

import android.app.Activity;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;

import net.gsantner.opoc.frontend.base.GsActivityBase;

import java.lang.ref.WeakReference;


/**
 * A {@link WebChromeClient} to support showing videos in fullscreen (and potentially other upcoming features)
 */
@SuppressWarnings("rawtypes")
public class GsWebViewChromeClient extends WebChromeClient {
    public static String userAgentOverride = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/104.0.5112.97 Mobile Safari/537.36";

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
    public GsWebViewChromeClient(WebView webView, Activity activity, FrameLayout fullscreenContainer) {
        m_fullscreenContainer = new WeakReference<>(fullscreenContainer);
        m_activity = new WeakReference<>(activity);
        m_webView = new WeakReference<>(webView);

        final WebSettings webSettings = m_webView.get().getSettings();
        if (!TextUtils.isEmpty(userAgentOverride)) {
            webSettings.setUserAgentString(userAgentOverride);
        }
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
