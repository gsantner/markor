/*
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.activity;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.print.PrintJob;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import net.gsantner.markor.R;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.model.DocumentLoader;
import net.gsantner.markor.renderer.MarkDownRenderer;
import net.gsantner.markor.ui.BaseFragment;
import net.gsantner.markor.util.AndroidBug5497Workaround;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ContextUtils;
import net.gsantner.markor.util.ShareUtil;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnFocusChange;
import butterknife.OnTextChanged;

public class DocumentActivity extends AppCompatActivity {
    @BindView(R.id.document__placeholder_fragment)
    FrameLayout _fragPlaceholder;
    @BindView(R.id.toolbar)
    Toolbar _toolbar;
    @BindView(R.id.note__activity__header_view_switcher)
    ViewSwitcher _toolbarSwitcher;
    @BindView(R.id.note__activity__edit_note_title)
    EditText _toolbarTitleEdit;
    @BindView(R.id.note__activity__text_note_title)
    TextView _toolbarTitleText;

    private MarkDownRenderer _mdRenderer = new MarkDownRenderer();
    private FragmentManager _fragManager;
    private Document _document;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContextUtils.get().setAppLanguage(AppSettings.get().getLanguage());
        AppSettings appSettings = AppSettings.get();
        if (appSettings.isEditorStatusBarHidden()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        setContentView(R.layout.document__activity);
        ButterKnife.bind(this);
        if (appSettings.isEditorStatusBarHidden()) {
            AndroidBug5497Workaround.assistActivity(this);
        }

        setSupportActionBar(_toolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setHomeAsUpIndicator(ContextCompat.getDrawable(this, R.drawable.ic_arrow_back_white_24dp));
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setDisplayShowTitleEnabled(false);
        }

        _fragManager = getSupportFragmentManager();


        Intent receivingIntent = getIntent();
        String intentAction = receivingIntent.getAction();
        String type = receivingIntent.getType();
        File file = (File) receivingIntent.getSerializableExtra(DocumentLoader.EXTRA_PATH);
        boolean fileIsFolder = receivingIntent.getBooleanExtra(DocumentLoader.EXTRA_PATH_IS_FOLDER, false);

        if (Intent.ACTION_SEND.equals(intentAction) && type != null) {
            if (type.equals("text/plain")) {
                //TODO openTextShareText(receivingIntent);
                // New Fragment: "New document", select between existing
            } else {
                Uri fileUri = receivingIntent.getParcelableExtra(Intent.EXTRA_STREAM);
                file = new File(fileUri.getPath());
            }
        } else if ((Intent.ACTION_VIEW.equals(intentAction) || Intent.ACTION_EDIT.equals(intentAction)) && type != null) {
            Uri fileUri = receivingIntent.getData();
            file = new File(fileUri.getPath());
        }

        if (file != null) {
            if (AppSettings.get().isPreviewFirst() && file.exists() && file.isFile()) {
                showPreview(null, file);
            } else {
                showEditor(null, file, fileIsFolder);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.document__menu, menu);
        ContextUtils cu = ContextUtils.get();
        String frag = getCurrentVisibleFragment() != null ? getCurrentVisibleFragment().getFragmentTag() : null;
        frag = frag == null ? "" : frag;

        menu.findItem(R.id.action_share_pdf).setVisible(frag.equals(DocumentPreviewFragment.FRAGMENT_TAG)
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT);
        menu.findItem(R.id.action_share_image).setVisible(frag.equals(DocumentPreviewFragment.FRAGMENT_TAG));

        cu.tintMenuItems(menu, true, Color.WHITE);
        cu.setSubMenuIconsVisiblity(menu, true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        _toolbarTitleEdit.clearFocus();
        ShareUtil shu = new ShareUtil(this);

        switch (item.getItemId()) {
            case android.R.id.home: {
                super.onBackPressed();
                return true;
            }
            case R.id.action_preview: {
                if (saveDocument()) {
                    DocumentPreviewFragment.showEditOnBack = true;
                    showPreview(_document, null);
                }
                return true;
            }

            case R.id.action_edit: {
                DocumentEditFragment.showPreviewOnBack = true;
                showEditor(_document, null, false);
                return true;
            }
            case R.id.action_share_text: {
                if (saveDocument()) {
                    shu.shareText(_document.getContent(), "text/plain");
                }
                return true;
            }
            case R.id.action_share_file:
                if (saveDocument()) {
                    shu.shareStream(_document.getFile(), "text/plain");
                }
                return true;
            case R.id.action_share_html:
            case R.id.action_share_html_source: {
                if (saveDocument()) {
                    shu.shareText(_mdRenderer.renderMarkdown(_document.getContent(), this),
                            "text/" + (item.getItemId() == R.id.action_share_html ? "html" : "plain"));
                }
                return true;
            }
            case R.id.action_share_image: {
                if (saveDocument() && getPreviewWebview() != null) {
                    shu.shareImage(ShareUtil.getBitmapFromWebView(getPreviewWebview()));
                }
                return true;
            }
            case R.id.action_share_pdf: {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && saveDocument() && getPreviewWebview() != null) {
                    PrintJob a = shu.printPdfOfWebview(_document, getPreviewWebview());
                }
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    public void setDocumentTitle(final String title) {
        _toolbarTitleEdit.setText(title);
        _toolbarTitleText.setText(title);
    }

    public void showEditor(@Nullable Document document, @Nullable File file, @Nullable boolean fileIsFolder) {
        if (document != null) {
            showFragment(DocumentEditFragment.newInstance(document));
        } else {
            showFragment(DocumentEditFragment.newInstance(file, fileIsFolder));
        }
    }

    public void showPreview(@Nullable Document document, @Nullable File file) {
        // Disable edittext when going to preview
        if (_toolbarSwitcher.getNextView() == _toolbarTitleText) {
            _toolbarSwitcher.showNext();
        }

        if (document != null) {
            showFragment(DocumentPreviewFragment.newInstance(document));
        } else {
            showFragment(DocumentPreviewFragment.newInstance(file));
        }
    }

    @OnFocusChange(R.id.note__activity__edit_note_title)
    public void onToolbarEditTitleFocusChanged(View view, boolean hasFocus) {
        if (!hasFocus) {
            setDocumentTitle(_toolbarTitleEdit.getText().toString());
            _toolbarSwitcher.showNext();
        }
    }

    @OnClick(R.id.note__activity__text_note_title)
    public void onToolbarTitleTapped(View view) {
        if (getCurrentVisibleFragment() != getExistingFragment(DocumentPreviewFragment.FRAGMENT_TAG)) {
            _toolbarSwitcher.showPrevious();
            _toolbarTitleEdit.requestFocus();
        }
    }

    @OnTextChanged(value = R.id.note__activity__edit_note_title, callback = OnTextChanged.Callback.TEXT_CHANGED)
    public void onToolbarTitleEditValueChanged(CharSequence title) {
        // Do not recurse
        if (title.equals(_toolbarTitleText.getText())) {
            return;
        }

        if (getExistingFragment(DocumentEditFragment.FRAGMENT_TAG) != null) {
            ((DocumentEditFragment) getExistingFragment(DocumentEditFragment.FRAGMENT_TAG))
                    .getDocument().setTitle(title.toString());
        }
    }

    @Override
    public void onBackPressed() {
        FragmentManager fragMgr = getSupportFragmentManager();
        BaseFragment top = getCurrentVisibleFragment();
        if (top != null) {
            if (!top.onBackPressed()) {
                if (fragMgr.getBackStackEntryCount() == 1) {
                    // Back action was not handled by fragment, handle in activity
                } else if (fragMgr.getBackStackEntryCount() > 0) {
                    // Back action was to go one fragment back
                    fragMgr.popBackStack();
                    return;
                }
            } else {
                // Was handled by child fragment
                return;
            }
        }

        if (_toolbarTitleEdit.hasFocus()) {
            _toolbarTitleEdit.clearFocus();
            return;
        }

        // Handle in this activity
        finish();
    }


    public void showFragment(BaseFragment fragment) {
        BaseFragment currentTop = (BaseFragment) _fragManager.findFragmentById(R.id.document__placeholder_fragment);

        if (currentTop == null || !currentTop.getFragmentTag().equals(fragment.getFragmentTag())) {
            _fragManager.beginTransaction()
                    //.addToBackStack(null)
                    .replace(R.id.document__placeholder_fragment
                            , fragment, fragment.getFragmentTag()).commit();
        }
        supportInvalidateOptionsMenu();
    }


    public synchronized BaseFragment getExistingFragment(final String fragmentTag) {
        FragmentManager fmgr = getSupportFragmentManager();
        BaseFragment fragment = (BaseFragment) fmgr.findFragmentByTag(fragmentTag);
        if (fragment != null) {
            return fragment;
        }
        return null;
    }

    private BaseFragment getCurrentVisibleFragment() {
        return (BaseFragment) getSupportFragmentManager().findFragmentById(R.id.document__placeholder_fragment);
    }

    public void setDocument(Document document) {
        _document = document;
        _toolbarTitleText.setText(_document.getTitle());
        _toolbarTitleText.setText(_document.getTitle());

        if (!TextUtils.isEmpty(_document.getTitle()) && _toolbarSwitcher.getNextView() == _toolbarTitleText) {
            _toolbarSwitcher.showNext();
        }
    }

    private boolean saveDocument() {
        boolean ret = false;
        if (getExistingFragment(DocumentEditFragment.FRAGMENT_TAG) != null) {
            DocumentEditFragment def = ((DocumentEditFragment) getExistingFragment(DocumentEditFragment.FRAGMENT_TAG));
            ret = def.saveDocument();
            setDocument(def.getDocument()); // Apply title again. Document is modified in edit activity
        } else if (getExistingFragment(DocumentPreviewFragment.FRAGMENT_TAG) != null) {
            ret = _document != null;
        }
        return ret;
    }

    private WebView getPreviewWebview() {
        if (getExistingFragment(DocumentPreviewFragment.FRAGMENT_TAG) != null) {
            return ((DocumentPreviewFragment) getExistingFragment(DocumentPreviewFragment.FRAGMENT_TAG))
                    .getWebview();
        }
        return null;
    }
}
