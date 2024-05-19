/*#######################################################
 *
 * SPDX-FileCopyrightText: 2017-2024 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2022-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
#########################################################*/
package net.gsantner.opoc.web;

import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings({"unused", "FieldCanBeLocal"})
public class GsWebViewClient extends WebViewClient {
    protected final WeakReference<WebView> m_webView;

    public GsWebViewClient(final WebView webView) {
        m_webView = new WeakReference<>(webView);
    }

    @Override
    public void onPageFinished(final WebView webView, final String url) {
        __onPageFinished_restoreScrollY(webView, url);
        super.onPageFinished(webView, url);
    }

    ////////////////////////////////////////////////////////////////////////////////////
    private final AtomicBoolean m_restoreScrollYEnabled = new AtomicBoolean(false);
    private int m_restoreScrollY = 0;

    /**
     * Activate by {@link GsWebViewClient#setRestoreScrollY(int)}
     *
     * @param webView onPageFinished {@link WebView}
     * @param url     onPageFinished url
     */
    protected void __onPageFinished_restoreScrollY(final WebView webView, final String url) {
        if (m_restoreScrollYEnabled.getAndSet(false)) {
            for (int dt : new int[]{50, 100, 150, 200, 250, 300}) {
                webView.postDelayed(() -> webView.setScrollY(m_restoreScrollY), dt);
            }
        }
    }

    /**
     * Apply vertical scroll position on next page load
     *
     * @param scrollY scroll position from {@link WebView#getScrollY()}
     */
    public void setRestoreScrollY(final int scrollY) {
        m_restoreScrollY = scrollY;
        m_restoreScrollYEnabled.set(scrollY >= 0);
    }

    ////////////////////////////////////////////////////////////////////////////////////
}
