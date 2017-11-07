/*
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.activity;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import net.gsantner.markor.R;
import net.gsantner.markor.model.Constants;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.model.DocumentLoader;
import net.gsantner.markor.renderer.MarkDownRenderer;
import net.gsantner.markor.ui.BaseFragment;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ContextUtils;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DocumentPreviewFragment extends BaseFragment {
    public static boolean showEditOnBack = false;
    public static final String FRAGMENT_TAG = "DocumentPreviewFragment";

    public static DocumentPreviewFragment newInstance(Document document) {
        DocumentPreviewFragment f = new DocumentPreviewFragment();
        Bundle args = new Bundle();
        args.putSerializable(DocumentLoader.EXTRA_DOCUMENT, document);
        f.setArguments(args);
        return f;
    }

    public static DocumentPreviewFragment newInstance(File path) {
        DocumentPreviewFragment f = new DocumentPreviewFragment();
        Bundle args = new Bundle();
        args.putSerializable(DocumentLoader.EXTRA_PATH, path);
        f.setArguments(args);
        return f;
    }

    @BindView(R.id.preview__activity__webview)
    WebView _webView;

    private View _view;
    private Context _context;
    private Document _document;
    private MarkDownRenderer _mdRenderer = new MarkDownRenderer();

    public DocumentPreviewFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.document__fragment__preview, container, false);
        ButterKnife.bind(this, view);
        _view = view;
        _context = view.getContext();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            WebView.enableSlowWholeDocumentDraw();
        }

        _document = loadDocument();
        showDocument();
    }

    private void showDocument() {
        Activity activity = getActivity();
        if (activity != null && activity instanceof DocumentActivity) {
            DocumentActivity da = ((DocumentActivity) activity);
            da.setDocumentTitle(_document.getTitle());
            da.setDocument(_document);
        }
        String html = renderMarkdown();
        _webView.loadDataWithBaseURL(_document.getFile().getParent(), html, "text/html", Constants.UTF_CHARSET, null);
    }

    private String renderMarkdown() {
        String html = _mdRenderer.renderMarkdown(_document.getContent(), _context);

        // Default font is set by css in line 1 of generated html
        html = html.replaceFirst("sans-serif-light", AppSettings.get().getFontFamily());

        return html;
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
        return DocumentLoader.loadDocument(getActivity(), getArguments(), _document);
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
                da.showEditor(_document, null, false);
            }
            return true;
        }
        return false;
    }

    public WebView getWebview() {
        return _webView;
    }
}
