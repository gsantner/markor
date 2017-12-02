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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
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
import net.gsantner.markor.format.TextFormat;
import net.gsantner.markor.format.converter.MarkdownTextConverter;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.BaseFragment;
import net.gsantner.markor.util.AndroidBug5497Workaround;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ContextUtils;
import net.gsantner.markor.util.DocumentIO;
import net.gsantner.markor.util.PermissionChecker;
import net.gsantner.markor.util.ShareUtil;
import net.gsantner.opoc.util.ShareUtilBase;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnFocusChange;
import butterknife.OnTextChanged;

@SuppressWarnings("unused")
public class DocumentActivity extends AppCompatActivity {
    public static final String EXTRA_DO_PREVIEW = "EXTRA_DO_PREVIEW";
    public static final String EXTRA_LAUNCHER_SHORTCUT_PATH = "EXTRA_LAUNCHER_SHORTCUT_PATH";

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

    private MarkdownTextConverter _mdRenderer = new MarkdownTextConverter();
    private FragmentManager _fragManager;
    private Document _document;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppSettings as = AppSettings.get();
        new ContextUtils(getBaseContext()).setAppLanguage(as.getLanguage());
        if (as.isEditorStatusBarHidden()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        setContentView(R.layout.document__activity);
        as = new AppSettings(this);
        new ContextUtils(this).setAppLanguage(as.getLanguage());
        ButterKnife.bind(this);
        if (as.isEditorStatusBarHidden()) {
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
        File file = (File) receivingIntent.getSerializableExtra(DocumentIO.EXTRA_PATH);
        boolean fileIsFolder = receivingIntent.getBooleanExtra(DocumentIO.EXTRA_PATH_IS_FOLDER, false);

        if (Intent.ACTION_SEND.equals(intentAction) && type != null) {
            if (type.equals("text/plain")) {
                showShareInto(receivingIntent.getStringExtra(Intent.EXTRA_TEXT));
            } else {
                Uri fileUri = receivingIntent.getParcelableExtra(Intent.EXTRA_STREAM);
                file = new File(fileUri.getPath());
            }
        } else if ((Intent.ACTION_VIEW.equals(intentAction) || Intent.ACTION_EDIT.equals(intentAction)) && type != null) {
            Uri fileUri = receivingIntent.getData();
            if (receivingIntent.getStringExtra(EXTRA_LAUNCHER_SHORTCUT_PATH) != null) {
                fileUri = Uri.fromFile(new File(receivingIntent.getStringExtra(EXTRA_LAUNCHER_SHORTCUT_PATH)));
            }
            file = new File(fileUri.getPath());
            if (fileUri.toString().startsWith("content://")) {
                new AlertDialog.Builder(this)
                        .setMessage("Sorry, but editing texts from content:// URIs is not supported yet. See https://github.com/gsantner/markor/issues/126 . Thanks!")
                        .setNegativeButton("Go to issue", (dialogInterface, i) -> ContextUtils.get().openWebpageInExternalBrowser("https://github.com/gsantner/markor/issues/126"))
                        .setPositiveButton("OK", null)
                        .setOnDismissListener((dialogInterface) -> finish())
                        .create().show();
            }
        }

        if (file != null) {
            if (receivingIntent.getBooleanExtra(EXTRA_DO_PREVIEW, false) || AppSettings.get().isPreviewFirst() && file.exists() && file.isFile()) {
                showPreview(null, file);
            } else {
                showTextEditor(null, file, fileIsFolder);
            }
        }

        _toolbarTitleEdit.setFilters(new InputFilter[]{DocumentIO.INPUT_FILTER_FILESYSTEM_FILENAME});
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
                if (isTaskRoot()) {
                    startActivity(new Intent(this, MainActivity.class));
                }
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
                showTextEditor(_document, null, false);
                return true;
            }
            case R.id.action_add_shortcut_launcher_home: {
                shu.createLauncherDesktopShortcut(_document);
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
                    MarkdownTextConverter converter = new MarkdownTextConverter();
                    shu.shareText(converter.convertMarkup(_document.getContent(), this),
                            "text/" + (item.getItemId() == R.id.action_share_html ? "html" : "plain"));
                }
                return true;
            }
            case R.id.action_share_image: {
                if (saveDocument() && getPreviewWebview() != null) {
                    shu.shareImage(ShareUtilBase.getBitmapFromWebView(getPreviewWebview()));
                }
                return true;
            }
            case R.id.action_share_pdf: {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && saveDocument() && getPreviewWebview() != null) {
                    shu.printOrCreatePdfFromWebview(getPreviewWebview(), _document);
                }
                return true;
            }

            case R.id.action_format_todotxt:
            case R.id.action_format_plaintext:
            case R.id.action_format_markdown: {
                _document.setFormat(item.getItemId());
                BaseFragment frag = getCurrentVisibleFragment();
                if (frag != null && frag instanceof TextFormat.TextFormatApplier) {
                    ((TextFormat.TextFormatApplier) frag).applyTextFormat(item.getItemId());
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

    public BaseFragment showTextEditor(@Nullable Document document, @Nullable File file, boolean fileIsFolder) {
        BaseFragment frag;
        if (document != null) {
            frag = showFragment(DocumentEditFragment.newInstance(document));
        } else {
            frag = showFragment(DocumentEditFragment.newInstance(file, fileIsFolder, true));
        }
        return frag;
    }

    public void showShareInto(String text) {
        // Disable edittext when going to shareinto
        if (_toolbarSwitcher.getNextView() == _toolbarTitleText) {
            _toolbarSwitcher.showNext();
        }
        _toolbarTitleText.setText(R.string.import_);
        showFragment(DocumentShareIntoFragment.newInstance(text));
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

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionChecker.checkPermissionResult(this, requestCode, permissions, grantResults);
    }

    @OnClick(R.id.note__activity__text_note_title)
    public void onToolbarTitleTapped(View view) {
        if (getCurrentVisibleFragment() == getExistingFragment(DocumentEditFragment.FRAGMENT_TAG)) {
            if (!getIntent().getBooleanExtra(EXTRA_DO_PREVIEW, false)) {
                _toolbarSwitcher.showPrevious();
                _toolbarTitleEdit.requestFocus();
            }
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
    @SuppressWarnings("StatementWithEmptyBody")
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


    public BaseFragment showFragment(BaseFragment fragment) {
        BaseFragment currentTop = (BaseFragment) _fragManager.findFragmentById(R.id.document__placeholder_fragment);

        if (currentTop == null || !currentTop.getFragmentTag().equals(fragment.getFragmentTag())) {
            _fragManager.beginTransaction()
                    //.addToBackStack(null)
                    .replace(R.id.document__placeholder_fragment
                            , fragment, fragment.getFragmentTag()).commit();
        } else {
            fragment = currentTop;
        }
        supportInvalidateOptionsMenu();
        return fragment;
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
