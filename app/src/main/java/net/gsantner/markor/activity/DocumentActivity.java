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

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
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
import android.text.Html;
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
import android.widget.Toast;
import android.widget.ViewSwitcher;

import net.gsantner.markor.R;
import net.gsantner.markor.format.TextFormat;
import net.gsantner.markor.format.markdown.MarkdownTextConverter;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.util.ActivityUtils;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.DocumentIO;
import net.gsantner.markor.util.PermissionChecker;
import net.gsantner.opoc.activity.GsFragmentBase;
import net.gsantner.opoc.util.ShareUtil;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnFocusChange;
import butterknife.OnTextChanged;
import other.so.AndroidBug5497Workaround;

@SuppressWarnings("unused")
public class DocumentActivity extends AppCompatActivity {
    public static final String EXTRA_DO_PREVIEW = "EXTRA_DO_PREVIEW";
    public static final String EXTRA_LAUNCHER_SHORTCUT_PATH = "real_file_path_2";

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

    private AppSettings _appSettings;
    private ActivityUtils _contextUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _appSettings = new AppSettings(this);
        _contextUtils = new ActivityUtils(this);
        _contextUtils.setAppLanguage(_appSettings.getLanguage());
        if (_appSettings.isEditorStatusBarHidden()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        setTheme(_appSettings.isDarkThemeEnabled() ? R.style.AppTheme_Dark : R.style.AppTheme_Light);
        setContentView(R.layout.document__activity);
        _contextUtils.setAppLanguage(_appSettings.getLanguage());
        ButterKnife.bind(this);
        if (_appSettings.isEditorStatusBarHidden()) {
            AndroidBug5497Workaround.assistActivity(this);
        }

        Intent receivingIntent = getIntent();
        String intentAction = receivingIntent.getAction();
        String type = receivingIntent.getType();
        File file = (File) receivingIntent.getSerializableExtra(DocumentIO.EXTRA_PATH);

        setSupportActionBar(_toolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(false);
            ab.setDisplayShowTitleEnabled(false);
        }

        _fragManager = getSupportFragmentManager();


        boolean fileIsFolder = receivingIntent.getBooleanExtra(DocumentIO.EXTRA_PATH_IS_FOLDER, false);
        if ((Intent.ACTION_VIEW.equals(intentAction) || Intent.ACTION_EDIT.equals(intentAction)) || Intent.ACTION_SEND.equals(intentAction)) {
            if (Intent.ACTION_SEND.equals(intentAction) && receivingIntent.hasExtra(Intent.EXTRA_TEXT)) {
                showShareInto(receivingIntent.getStringExtra(Intent.EXTRA_TEXT));
            } else {
                file = new ShareUtil(getApplicationContext()).extractFileFromIntent(receivingIntent);
                if (file == null && receivingIntent.getData() != null && receivingIntent.getData().toString().startsWith("content://")) {
                    String msg = getString(R.string.filemanager_doesnot_supply_required_data__appspecific) + "\n\n"
                            + getString(R.string.sync_to_local_folder_notice) + "\n\n"
                            + getString(R.string.sync_to_local_folder_notice_paths, getString(R.string.configure_in_the_apps_settings));

                    new AlertDialog.Builder(this)
                            .setMessage(Html.fromHtml(msg.replace("\n", "<br/>")))
                            .setNegativeButton(R.string.more_info, (dialogInterface, i) -> _contextUtils.openWebpageInExternalBrowser("https://github.com/gsantner/markor/issues/197"))
                            .setPositiveButton(android.R.string.ok, null)
                            .setOnDismissListener((dialogInterface) -> finish())
                            .create().show();
                }
            }
        }

        if (file != null) {
            if (receivingIntent.getBooleanExtra(EXTRA_DO_PREVIEW, false) || _appSettings.isPreviewFirst() && file.exists() && file.isFile()) {
                showPreview(null, file);
            } else {
                showTextEditor(null, file, fileIsFolder);
            }
        }

        _toolbarTitleEdit.setFilters(new InputFilter[]{DocumentIO.INPUT_FILTER_FILESYSTEM_FILENAME});
        _toolbarTitleEdit.setEnabled(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.document__menu, menu);
        String frag = getCurrentVisibleFragment() != null ? getCurrentVisibleFragment().getFragmentTag() : null;
        frag = frag == null ? "" : frag;

        menu.findItem(R.id.action_share_pdf).setVisible(frag.equals(DocumentRepresentationFragment.FRAGMENT_TAG)
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT);
        menu.findItem(R.id.action_share_image).setVisible(frag.equals(DocumentRepresentationFragment.FRAGMENT_TAG));

        _contextUtils.tintMenuItems(menu, true, Color.WHITE);
        _contextUtils.setSubMenuIconsVisiblity(menu, true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        _toolbarTitleEdit.clearFocus();
        net.gsantner.markor.util.ShareUtil shu = new net.gsantner.markor.util.ShareUtil(this);

        switch (item.getItemId()) {
            case R.id.action_preview: {
                if (saveDocument()) {
                    DocumentRepresentationFragment.showEditOnBack = true;
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
            case R.id.action_share_calendar_event: {
                if (saveDocument()) {
                    if (!shu.createCalendarAppointment(_document.getTitle(), _document.getContent(), null)) {
                        Toast.makeText(this, R.string.no_calendar_app_is_installed, Toast.LENGTH_SHORT).show();
                    }
                }
                return true;
            }
            case R.id.action_share_image: {
                if (saveDocument() && getPreviewWebview() != null) {
                    shu.shareImage(ShareUtil.getBitmapFromWebView(getPreviewWebview()), Bitmap.CompressFormat.JPEG);
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
                if (_document != null) {
                    _document.setFormat(item.getItemId());
                    GsFragmentBase frag = getCurrentVisibleFragment();
                    if (frag != null && frag instanceof TextFormat.TextFormatApplier) {
                        ((TextFormat.TextFormatApplier) frag).applyTextFormat(item.getItemId());
                    }
                }
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Determine some results and forward using Local Broadcast
        ShareUtil shu = new ShareUtil(this.getApplicationContext());
        shu.extractResultFromActivityResult(requestCode, resultCode, data);
    }

    public void setDocumentTitle(final String title) {
        _toolbarTitleEdit.setText(title);
        _toolbarTitleText.setText(title);
    }

    public GsFragmentBase showTextEditor(@Nullable Document document, @Nullable File file, boolean fileIsFolder) {
        GsFragmentBase frag;
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
            showFragment(DocumentRepresentationFragment.newInstance(document));
        } else {
            showFragment(DocumentRepresentationFragment.newInstance(file));
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
        new PermissionChecker(this).checkPermissionResult(requestCode, permissions, grantResults);
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
        GsFragmentBase top = getCurrentVisibleFragment();
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


    public GsFragmentBase showFragment(GsFragmentBase fragment) {
        GsFragmentBase currentTop = (GsFragmentBase) _fragManager.findFragmentById(R.id.document__placeholder_fragment);

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


    public synchronized GsFragmentBase getExistingFragment(final String fragmentTag) {
        FragmentManager fmgr = getSupportFragmentManager();
        GsFragmentBase fragment = (GsFragmentBase) fmgr.findFragmentByTag(fragmentTag);
        if (fragment != null) {
            return fragment;
        }
        return null;
    }

    private GsFragmentBase getCurrentVisibleFragment() {
        return (GsFragmentBase) getSupportFragmentManager().findFragmentById(R.id.document__placeholder_fragment);
    }

    public void setDocument(Document document) {
        _document = document;
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
        } else if (getExistingFragment(DocumentRepresentationFragment.FRAGMENT_TAG) != null) {
            ret = _document != null;
        }
        return ret;
    }

    private WebView getPreviewWebview() {
        if (getExistingFragment(DocumentRepresentationFragment.FRAGMENT_TAG) != null) {
            return ((DocumentRepresentationFragment) getExistingFragment(DocumentRepresentationFragment.FRAGMENT_TAG))
                    .getWebview();
        }
        return null;
    }
}
