/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

import net.gsantner.markor.R;
import net.gsantner.markor.format.TextFormat;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ContextUtils;
import net.gsantner.markor.util.DocumentIO;
import net.gsantner.markor.util.MarkorWebViewClient;
import net.gsantner.opoc.activity.GsFragmentBase;

import java.io.File;

import butterknife.BindView;

public class DocumentPreviewFragment extends GsFragmentBase implements TextFormat.TextFormatApplier {
    public static boolean showEditOnBack = false;
    public static final String FRAGMENT_TAG = "DocumentPreviewFragment";

    public static DocumentPreviewFragment newInstance(Document document) {
        DocumentPreviewFragment f = new DocumentPreviewFragment();
        Bundle args = new Bundle();
        args.putSerializable(DocumentIO.EXTRA_DOCUMENT, document);
        f.setArguments(args);
        return f;
    }

    public static DocumentPreviewFragment newInstance(File path) {
        DocumentPreviewFragment f = new DocumentPreviewFragment();
        Bundle args = new Bundle();
        args.putSerializable(DocumentIO.EXTRA_PATH, path);
        f.setArguments(args);
        return f;
    }

    @BindView(R.id.preview__activity__webview)
    WebView _webView;

    private Document _document;
    private TextFormat _textFormat;

    public DocumentPreviewFragment() {
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.document__fragment__preview;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            WebView.enableSlowWholeDocumentDraw();
        }
        AppSettings appSettings = new AppSettings(view.getContext());
        _webView.setWebViewClient(new MarkorWebViewClient(getActivity()));
        WebSettings webSettings = _webView.getSettings();
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setTextZoom((int) (appSettings.getFontSize() / 17f * 100f));
        webSettings.setAppCacheEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setGeolocationEnabled(false);
        webSettings.setJavaScriptEnabled(true);

        _document = loadDocument();
        applyTextFormat(_document.getFormat()); //showDocument();
    }

    private void showDocument() {
        Activity activity = getActivity();
        if (activity != null && activity instanceof DocumentActivity) {
            DocumentActivity da = ((DocumentActivity) activity);
            da.setDocumentTitle(_document.getTitle());
            da.setDocument(_document);
        }
        _textFormat.getConverter().convertMarkupShowInWebView(_document, _webView);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.document__preview__menu, menu);
        ContextUtils cu = ContextUtils.get();

        cu.tintMenuItems(menu, true, Color.WHITE);
        cu.setSubMenuIconsVisiblity(menu, true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_edit: {
                // Handled by parent
                return false;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private Document loadDocument() {
        return DocumentIO.loadDocument(getActivity(), getArguments(), _document);
    }

    @Override
    public void applyTextFormat(int textFormatId) {
        _textFormat = TextFormat.getFormat(textFormatId, getActivity(), _document);
        showDocument();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        showEditOnBack = false;
    }

    @Override
    public String getFragmentTag() {
        return FRAGMENT_TAG;
    }

    @Override
    public boolean onBackPressed() {
        if (showEditOnBack) {
            showEditOnBack = false;
            Activity activity = getActivity();
            if (activity != null && activity instanceof DocumentActivity) {
                DocumentActivity da = ((DocumentActivity) activity);
                da.showTextEditor(_document, null, false);
            }
            return true;
        }
        return false;
    }

    //
    //
    //

    public WebView getWebview() {
        return _webView;
    }
}
