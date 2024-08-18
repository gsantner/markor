/*#######################################################
 *
 *   Maintained 2017-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.HorizontalScrollView;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import net.gsantner.markor.ApplicationObject;
import net.gsantner.markor.BuildConfig;
import net.gsantner.markor.R;
import net.gsantner.markor.format.ActionButtonBase;
import net.gsantner.markor.format.FormatRegistry;
import net.gsantner.markor.format.TextConverterBase;
import net.gsantner.markor.frontend.DraggableScrollbarScrollView;
import net.gsantner.markor.frontend.FileInfoDialog;
import net.gsantner.markor.frontend.MarkorDialogFactory;
import net.gsantner.markor.frontend.filebrowser.MarkorFileBrowserFactory;
import net.gsantner.markor.frontend.textview.HighlightingEditor;
import net.gsantner.markor.frontend.textview.TextViewUtils;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.util.MarkorContextUtils;
import net.gsantner.markor.web.MarkorWebViewClient;
import net.gsantner.opoc.frontend.filebrowser.GsFileBrowserOptions;
import net.gsantner.opoc.frontend.settings.GsFontPreferenceCompat;
import net.gsantner.opoc.frontend.textview.TextViewUndoRedo;
import net.gsantner.opoc.util.GsContextUtils;
import net.gsantner.opoc.util.GsCoolExperimentalStuff;
import net.gsantner.opoc.web.GsWebViewChromeClient;
import net.gsantner.opoc.wrapper.GsTextWatcherAdapter;

import java.io.File;

@SuppressWarnings({"UnusedReturnValue"})
@SuppressLint("NonConstantResourceId")
public class DocumentEditAndViewFragment extends MarkorBaseFragment implements FormatRegistry.TextFormatApplier {
    public static final String FRAGMENT_TAG = "DocumentEditAndViewFragment";
    public static final String SAVESTATE_DOCUMENT = "DOCUMENT";
    public static final String START_PREVIEW = "START_PREVIEW";

    public static DocumentEditAndViewFragment newInstance(final @NonNull Document document, final Integer lineNumber, final Boolean preview) {
        DocumentEditAndViewFragment f = new DocumentEditAndViewFragment();
        Bundle args = new Bundle();
        args.putSerializable(Document.EXTRA_DOCUMENT, document);
        if (lineNumber != null) {
            args.putInt(Document.EXTRA_FILE_LINE_NUMBER, lineNumber);
        }
        if (preview != null) {
            args.putBoolean(START_PREVIEW, preview);
        }
        f.setArguments(args);
        return f;
    }

    private HighlightingEditor _hlEditor;
    private WebView _webView;
    private MarkorWebViewClient _webViewClient;
    private ViewGroup _textActionsBar;

    private DraggableScrollbarScrollView _primaryScrollView;
    private HorizontalScrollView _hsView;
    private SearchView _menuSearchViewForViewMode;
    private Document _document;
    private FormatRegistry _format;
    private MarkorContextUtils _cu;
    private TextViewUndoRedo _editTextUndoRedoHelper;
    private MenuItem _saveMenuItem, _undoMenuItem, _redoMenuItem;
    private boolean _isPreviewVisible;
    private boolean _nextConvertToPrintMode = false;


    public DocumentEditAndViewFragment() {
        super();
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle args = getArguments();
        if (savedInstanceState != null && savedInstanceState.containsKey(SAVESTATE_DOCUMENT)) {
            _document = (Document) savedInstanceState.getSerializable(SAVESTATE_DOCUMENT);
        } else if (args != null && args.containsKey(Document.EXTRA_DOCUMENT)) {
            _document = (Document) args.get(Document.EXTRA_DOCUMENT);
        }
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.document__fragment__edit;
    }

    @SuppressLint({"SetJavaScriptEnabled", "WrongConstant", "AddJavascriptInterface", "JavascriptInterface"})
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Activity activity = getActivity();

        _hlEditor = view.findViewById(R.id.document__fragment__edit__highlighting_editor);
        _textActionsBar = view.findViewById(R.id.document__fragment__edit__text_actions_bar);
        _webView = view.findViewById(R.id.document__fragment_view_webview);
        _primaryScrollView = view.findViewById(R.id.document__fragment__edit__content_editor__scrolling_parent);
        _cu = new MarkorContextUtils(activity);

        // Using `if (_document != null)` everywhere is dangerous
        // It may cause reads or writes to _silently fail_
        // Instead we try to create it, and exit if that isn't possible
        if (isStateBad()) {
            Toast.makeText(activity, R.string.error_could_not_open_file, Toast.LENGTH_LONG).show();
            if (activity != null) {
                activity.finish();
            }
            return;
        }

        if (_appSettings.getSetWebViewFulldrawing() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            WebView.enableSlowWholeDocumentDraw();
        }

        _webViewClient = new MarkorWebViewClient(_webView, activity);
        _webView.setWebChromeClient(new GsWebViewChromeClient(_webView, activity, view.findViewById(R.id.document__fragment_fullscreen_overlay)));
        _webView.setWebViewClient(_webViewClient);
        _webView.addJavascriptInterface(this, "Android");
        WebSettings webSettings = _webView.getSettings();
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setTextZoom((int) (_appSettings.getViewFontSize() / 15.7f * 100f));
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webSettings.setDatabaseEnabled(true);
        webSettings.setGeolocationEnabled(false);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(false);
        webSettings.setMediaPlaybackRequiresUserGesture(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && BuildConfig.IS_TEST_BUILD && BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true); // Inspect on computer chromium browser: chrome://inspect/#devices
        }

        // Upon construction, the document format has been determined from extension etc
        // Here we replace it with the last saved format.
        _document.setFormat(_appSettings.getDocumentFormat(_document.getPath(), _document.getFormat()));
        applyTextFormat(_document.getFormat());
        _format.getActions().setDocument(_document);

        if (activity instanceof DocumentActivity) {
            ((DocumentActivity) activity).setDocumentTitle(_document.getTitle());
        }

        // Preview mode set before loadDocument to prevent flicker
        final Bundle args = getArguments();
        final boolean startInPreview = _appSettings.getDocumentPreviewState(_document.getPath());
        if (args != null && savedInstanceState == null) { // Use the launch flag on first launch
            setViewModeVisibility(args.getBoolean(START_PREVIEW, startInPreview), false);
        } else {
            setViewModeVisibility(startInPreview, false);
        }

        _hlEditor.setSaveInstanceState(false); // We will reload from disk
        _document.resetChangeTracking(); // force next reload
        loadDocument();

        // If not set by loadDocument, se the undo-redo helper here
        if (_editTextUndoRedoHelper == null) {
            _editTextUndoRedoHelper = new TextViewUndoRedo(_hlEditor);
        }

        // Configure the editor. Doing so after load helps prevent some errors
        // ---------------------------------------------------------
        _hlEditor.setLineSpacing(0, _appSettings.getEditorLineSpacing());
        _hlEditor.setTextSize(TypedValue.COMPLEX_UNIT_SP, _appSettings.getDocumentFontSize(_document.getPath()));
        _hlEditor.setTypeface(GsFontPreferenceCompat.typeface(getContext(), _appSettings.getFontFamily(), Typeface.NORMAL));
        _hlEditor.setBackgroundColor(_appSettings.getEditorBackgroundColor());
        _hlEditor.setTextColor(_appSettings.getEditorForegroundColor());
        _hlEditor.setGravity(_appSettings.isEditorStartEditingInCenter() ? Gravity.CENTER : Gravity.NO_GRAVITY);
        _hlEditor.setHighlightingEnabled(_appSettings.getDocumentHighlightState(_document.getPath(), _hlEditor.getText()));
        _hlEditor.setLineNumbersEnabled(_appSettings.getDocumentLineNumbersEnabled(_document.getPath()));
        _hlEditor.setAutoFormatEnabled(_appSettings.getDocumentAutoFormatEnabled(_document.getPath()));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Do not need to send contents to accessibility
            _hlEditor.setImportantForAccessibility(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
        }
        _webView.setBackgroundColor(Color.TRANSPARENT);

        // Various settings
        setHorizontalScrollMode(isDisplayedAtMainActivity() || _appSettings.getDocumentWrapState(_document.getPath()));
        updateMenuToggleStates(0);
        // ---------------------------------------------------------

        final Runnable debounced = TextViewUtils.makeDebounced(500, () -> {
            checkTextChangeState();
            updateUndoRedoIconStates();
        });
        _hlEditor.addTextChangedListener(GsTextWatcherAdapter.after(s -> debounced.run()));

        // We set the keyboard to be hidden if it was hidden when we lost focus
        // This works well to preserve keyboard state.
        if (activity != null) {
            final Window window = activity.getWindow();
            final int adjustResize = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
            final int unchanged = WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED | adjustResize;
            final int hidden = WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN | adjustResize;
            final int shown = WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE | adjustResize;

            _hlEditor.getViewTreeObserver().addOnWindowFocusChangeListener(hasFocus -> {
                if (hasFocus) {
                    // Restore old state
                    _hlEditor.postDelayed(() -> window.setSoftInputMode(unchanged), 500);
                } else {
                    final Boolean isOpen = TextViewUtils.isImeOpen(_hlEditor);
                    if (isOpen != null) {
                        window.setSoftInputMode(isOpen ? shown : hidden);
                    }
                }
            });
        }
    }

    @Override
    public void onFragmentFirstTimeVisible() {
        _primaryScrollView.invalidate();
        int startPos = _appSettings.getLastEditPosition(_document.getPath(), _hlEditor.length());

        // First start - overwrite start position if needed
        if (_savedInstanceState == null) {
            final Bundle args = getArguments();
            if (args != null && args.containsKey(Document.EXTRA_FILE_LINE_NUMBER)) {
                final int lno = args.getInt(Document.EXTRA_FILE_LINE_NUMBER);
                if (lno >= 0) {
                    startPos = TextViewUtils.getIndexFromLineOffset(_hlEditor.getText(), lno, 0);
                } else if (lno == Document.EXTRA_FILE_LINE_NUMBER_LAST) {
                    startPos = _hlEditor.length();
                }
            }
        }

        TextViewUtils.setSelectionAndShow(_hlEditor, startPos);
    }

    @Override
    public void onResume() {
        loadDocument();
        _webView.onResume();
        super.onResume();
    }

    @Override
    public void onPause() {
        saveDocument(false);
        _webView.onPause();
        _appSettings.addRecentFile(_document.getFile());
        _appSettings.setDocumentPreviewState(_document.getPath(), _isPreviewVisible);
        _appSettings.setLastEditPosition(_document.getPath(), _hlEditor.getSelectionStart());
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putSerializable(SAVESTATE_DOCUMENT, _document);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.document__edit__menu, menu);
        _cu.tintMenuItems(menu, true, Color.WHITE);
        _cu.setSubMenuIconsVisibility(menu, true);

        final boolean isExperimentalFeaturesEnabled = _appSettings.isExperimentalFeaturesEnabled();
        final boolean isText = !_document.isBinaryFileNoTextLoading();

        menu.findItem(R.id.action_undo).setVisible(isText && _appSettings.isEditorHistoryEnabled());
        menu.findItem(R.id.action_redo).setVisible(isText && _appSettings.isEditorHistoryEnabled());
        menu.findItem(R.id.action_send_debug_log).setVisible(MainActivity.IS_DEBUG_ENABLED && !isDisplayedAtMainActivity() && !_isPreviewVisible);

        // Undo / Redo / Save (keep visible, but deactivated and tinted grey if not executable)
        _undoMenuItem = menu.findItem(R.id.action_undo).setVisible(isText && !_isPreviewVisible);
        _redoMenuItem = menu.findItem(R.id.action_redo).setVisible(isText && !_isPreviewVisible);
        _saveMenuItem = menu.findItem(R.id.action_save).setVisible(isText && !_isPreviewVisible);

        // Edit / Preview switch
        menu.findItem(R.id.action_edit).setVisible(isText && _isPreviewVisible);
        menu.findItem(R.id.action_preview).setVisible(isText && !_isPreviewVisible);
        menu.findItem(R.id.action_search).setVisible(isText && !_isPreviewVisible);
        menu.findItem(R.id.action_search_view).setVisible(isText && _isPreviewVisible);
        menu.findItem(R.id.submenu_format_selection).setVisible(isText && !_isPreviewVisible);
        menu.findItem(R.id.submenu_share).setVisible(isText);
        menu.findItem(R.id.submenu_tools).setVisible(isText);
        menu.findItem(R.id.submenu_per_file_settings).setVisible(isText);

        menu.findItem(R.id.action_share_pdf).setVisible(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT);
        menu.findItem(R.id.action_share_image).setVisible(true);
        menu.findItem(R.id.action_load_epub).setVisible(isExperimentalFeaturesEnabled);

        // SearchView (View Mode)
        _menuSearchViewForViewMode = (SearchView) menu.findItem(R.id.action_search_view).getActionView();
        if (_menuSearchViewForViewMode != null) {
            _menuSearchViewForViewMode.setSubmitButtonEnabled(true);
            _menuSearchViewForViewMode.setQueryHint(getString(R.string.search));
            _menuSearchViewForViewMode.setOnQueryTextFocusChangeListener((v, searchHasFocus) -> {
                if (!searchHasFocus) {
                    _menuSearchViewForViewMode.setQuery("", false);
                    _menuSearchViewForViewMode.setIconified(true);
                }
            });
            _menuSearchViewForViewMode.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String text) {
                    _webView.findNext(true);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String text) {
                    _webView.findAllAsync(text);
                    return true;
                }
            });
        }

        // Set various initial states
        updateMenuToggleStates(_document.getFormat());
        checkTextChangeState();
        updateUndoRedoIconStates();
    }

    @Override
    public boolean onReceiveKeyPress(int keyCode, KeyEvent event) {
        if (event.isCtrlPressed()) {
            if (event.isShiftPressed() && keyCode == KeyEvent.KEYCODE_Z) {
                if (_editTextUndoRedoHelper != null && _editTextUndoRedoHelper.getCanRedo()) {
                    _hlEditor.withAutoFormatDisabled(_editTextUndoRedoHelper::redo);
                    updateUndoRedoIconStates();
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_S) {
                saveDocument(true);
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_Y) {
                if (_editTextUndoRedoHelper != null && _editTextUndoRedoHelper.getCanRedo()) {
                    _hlEditor.withAutoFormatDisabled(_editTextUndoRedoHelper::redo);
                    updateUndoRedoIconStates();
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_Z) {
                if (_editTextUndoRedoHelper != null && _editTextUndoRedoHelper.getCanUndo()) {
                    _hlEditor.withAutoFormatDisabled(_editTextUndoRedoHelper::undo);
                    updateUndoRedoIconStates();
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_SLASH) {
                setViewModeVisibility(!_isPreviewVisible);
                return true;
            }
        }

        return false;
    }

    private void updateUndoRedoIconStates() {
        Drawable d;
        final boolean canUndo = _editTextUndoRedoHelper != null && _editTextUndoRedoHelper.getCanUndo();
        if (_undoMenuItem != null && _undoMenuItem.isEnabled() != canUndo && (d = _undoMenuItem.setEnabled(canUndo).getIcon()) != null) {
            d.mutate().setAlpha(canUndo ? 255 : 40);
        }

        final boolean canRedo = _editTextUndoRedoHelper != null && _editTextUndoRedoHelper.getCanRedo();
        if (_redoMenuItem != null && _redoMenuItem.isEnabled() != canRedo && (d = _redoMenuItem.setEnabled(canRedo).getIcon()) != null) {
            d.mutate().setAlpha(canRedo ? 255 : 40);
        }
    }

    public boolean loadDocument() {
        if (isSdStatusBad() || isStateBad()) {
            errorClipText();
            return false;
        }

        // Only trigger the load process if constructing or file updated or force reload
        if (_document.hasFileChangedSinceLastLoad()) {

            final String content = _document.loadContent(getContext());
            if (content == null) {
                errorClipText();
                return false;
            }

            if (!_document.isContentSame(_hlEditor.getText())) {

                final int[] sel = TextViewUtils.getSelection(_hlEditor);
                sel[0] = Math.min(sel[0], content.length());
                sel[1] = Math.min(sel[1], content.length());

                if (_editTextUndoRedoHelper != null) {
                    _editTextUndoRedoHelper.disconnect();
                    _editTextUndoRedoHelper.clearHistory();
                }

                _hlEditor.withAutoFormatDisabled(() -> _hlEditor.setText(content));

                if (_editTextUndoRedoHelper == null) {
                    _editTextUndoRedoHelper = new TextViewUndoRedo(_hlEditor);
                } else {
                    _editTextUndoRedoHelper.setTextView(_hlEditor);
                }

                _hlEditor.setSelection(sel[0], sel[1]);
                TextViewUtils.showSelection(_hlEditor);
            }
            checkTextChangeState();

            if (_isPreviewVisible) {
                updateViewModeText();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        final Activity activity = getActivity();
        if (activity == null) {
            return true;
        }

        final int itemId = item.getItemId();
        switch (itemId) {
            case R.id.action_undo: {
                if (_editTextUndoRedoHelper != null && _editTextUndoRedoHelper.getCanUndo()) {
                    _hlEditor.withAutoFormatDisabled(_editTextUndoRedoHelper::undo);
                    updateUndoRedoIconStates();
                }
                return true;
            }
            case R.id.action_redo: {
                if (_editTextUndoRedoHelper != null && _editTextUndoRedoHelper.getCanRedo()) {
                    _hlEditor.withAutoFormatDisabled(_editTextUndoRedoHelper::redo);
                    updateUndoRedoIconStates();
                }
                return true;
            }
            case R.id.action_save: {
                saveDocument(true);
                return true;
            }
            case R.id.action_reload: {
                _document.resetChangeTracking(); // Force next load
                if (loadDocument()) {
                    Toast.makeText(activity, "✔", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            case R.id.action_preview: {
                setViewModeVisibility(true);
                return true;
            }
            case R.id.action_edit: {
                setViewModeVisibility(false);
                return true;
            }
            case R.id.action_preview_edit_toggle: {
                setViewModeVisibility(!_isPreviewVisible);
                return true;
            }
            case R.id.action_share_path: {
                _cu.shareText(getActivity(), _document.getFile().getAbsolutePath(), GsContextUtils.MIME_TEXT_PLAIN);
                return true;
            }
            case R.id.action_share_text: {
                if (saveDocument(false)) {
                    _cu.shareText(getActivity(), getTextString(), GsContextUtils.MIME_TEXT_PLAIN);
                }
                return true;
            }
            case R.id.action_share_file: {
                if (saveDocument(false)) {
                    _cu.shareStream(getActivity(), _document.getFile(), GsContextUtils.MIME_TEXT_PLAIN);
                }
                return true;
            }
            case R.id.action_share_html:
            case R.id.action_share_html_source: {
                if (saveDocument(false)) {
                    TextConverterBase converter = FormatRegistry.getFormat(_document.getFormat(), activity, _document).getConverter();
                    _cu.shareText(getActivity(),
                            converter.convertMarkup(getTextString(), getActivity(), false, _hlEditor.getLineNumbersEnabled(), _document.getFile()),
                            "text/" + (item.getItemId() == R.id.action_share_html ? "html" : "plain")
                    );
                }
                return true;
            }
            case R.id.action_share_calendar_event: {
                if (saveDocument(false)) {
                    if (!_cu.createCalendarAppointment(getActivity(), _document.getTitle(), getTextString(), null)) {
                        Toast.makeText(activity, R.string.no_calendar_app_is_installed, Toast.LENGTH_SHORT).show();
                    }
                }
                return true;
            }
            case R.id.action_share_screenshot:
            case R.id.action_share_image:
            case R.id.action_share_pdf: {
                _appSettings.getSetWebViewFulldrawing(true);
                if (saveDocument(false)) {
                    _nextConvertToPrintMode = true;
                    setViewModeVisibility(true);
                    Toast.makeText(activity, R.string.please_wait, Toast.LENGTH_LONG).show();
                    _webView.postDelayed(() -> {
                        if (item.getItemId() == R.id.action_share_pdf && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            _cu.printOrCreatePdfFromWebview(_webView, _document, getTextString().contains("beamer\n"));
                        } else if (item.getItemId() != R.id.action_share_pdf) {
                            Bitmap bmp = _cu.getBitmapFromWebView(_webView, item.getItemId() == R.id.action_share_image);
                            _cu.shareImage(getContext(), bmp, null);
                        }
                    }, 7000);
                }

                return true;
            }
            case R.string.action_format_wikitext:
            case R.string.action_format_keyvalue:
            case R.string.action_format_todotxt:
            case R.string.action_format_csv:
            case R.string.action_format_plaintext:
            case R.string.action_format_asciidoc:
            case R.string.action_format_orgmode:
            case R.string.action_format_markdown: {
                if (itemId != _document.getFormat()) {
                    _document.setFormat(itemId);
                    applyTextFormat(itemId);
                    _appSettings.setDocumentFormat(_document.getPath(), _document.getFormat());
                }
                return true;
            }
            case R.id.action_search: {
                setViewModeVisibility(false);
                _format.getActions().onSearch();
                return true;
            }
            case R.id.action_send_debug_log: {
                final String text = AppSettings.getDebugLog() + "\n\n------------------------\n\n\n\n" + Document.getMaskedContent(getTextString());
                _cu.draftEmail(getActivity(), "Debug Log " + getString(R.string.app_name_real), text, "debug@localhost.lan");
                return true;
            }
            case R.id.action_load_epub: {
                MarkorFileBrowserFactory.showFileDialog(new GsFileBrowserOptions.SelectionListenerAdapter() {
                                                            @Override
                                                            public void onFsViewerSelected(String request, File file, final Integer lineNumber) {
                                                                _hlEditor.setText(GsCoolExperimentalStuff.convertEpubToText(file, getString(R.string.page)));
                                                            }

                                                            @Override
                                                            public void onFsViewerConfig(GsFileBrowserOptions.Options dopt) {
                                                                dopt.titleText = R.string.select;
                                                            }
                                                        }, getParentFragmentManager(), activity,
                        (context, file) -> file != null && file.getAbsolutePath().toLowerCase().endsWith(".epub")
                );
                return true;
            }
            case R.id.action_speed_read: {
                GsCoolExperimentalStuff.showSpeedReadDialog(activity, getTextString());
                return true;
            }
            case R.id.action_wrap_words: {
                final boolean newState = !isWrapped();
                _appSettings.setDocumentWrapState(_document.getPath(), newState);
                setHorizontalScrollMode(newState);
                updateMenuToggleStates(0);
                return true;
            }
            case R.id.action_line_numbers: {
                final boolean newState = !_hlEditor.getLineNumbersEnabled();
                _appSettings.setDocumentLineNumbersEnabled(_document.getPath(), newState);
                _hlEditor.setLineNumbersEnabled(newState);
                updateMenuToggleStates(0);
                return true;
            }
            case R.id.action_enable_highlighting: {
                final boolean newState = !_hlEditor.getHighlightingEnabled();
                _hlEditor.setHighlightingEnabled(newState);
                _appSettings.setDocumentHighlightState(_document.getPath(), newState);
                updateMenuToggleStates(0);
                return true;
            }
            case R.id.action_enable_auto_format: {
                final boolean newState = !_hlEditor.getAutoFormatEnabled();
                _hlEditor.setAutoFormatEnabled(newState);
                _appSettings.setDocumentAutoFormatEnabled(_document.getPath(), newState);
                updateMenuToggleStates(0);
                return true;
            }
            case R.id.action_info: {
                if (saveDocument(false)) { // In order to have the correct info displayed
                    FileInfoDialog.show(_document.getFile(), getParentFragmentManager());
                }
                return true;
            }
            case R.id.action_set_font_size: {
                MarkorDialogFactory.showFontSizeDialog(activity, _appSettings.getDocumentFontSize(_document.getPath()), (newSize) -> {
                    _hlEditor.setTextSize(TypedValue.COMPLEX_UNIT_SP, (float) newSize);
                    _appSettings.setDocumentFontSize(_document.getPath(), newSize);
                });
                return true;
            }
            case R.id.action_show_file_browser: {
                // Delay because I want menu to close before we open the file browser
                _hlEditor.postDelayed(() -> {
                    final Intent intent = new Intent(activity, MainActivity.class).putExtra(Document.EXTRA_FILE, _document.getFile());
                    GsContextUtils.instance.animateToActivity(activity, intent, false, null);
                }, 250);
                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    public void checkTextChangeState() {
        final boolean isTextChanged = !_document.isContentSame(_hlEditor.getText());
        Drawable d;

        if (_saveMenuItem != null && _saveMenuItem.isEnabled() != isTextChanged && (d = _saveMenuItem.setEnabled(isTextChanged).getIcon()) != null) {
            d.mutate().setAlpha(isTextChanged ? 255 : 40);
        }
    }

    public void applyTextFormat(final int textFormatId) {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        _format = FormatRegistry.getFormat(textFormatId, activity, _document);
        _document.setFormat(_format.getFormatId());
        _hlEditor.setHighlighter(_format.getHighlighter());
        _hlEditor.setDynamicHighlightingEnabled(_appSettings.isDynamicHighlightingEnabled());
        _hlEditor.setAutoFormatters(_format.getAutoFormatInputFilter(), _format.getAutoFormatTextWatcher());
        _hlEditor.setAutoFormatEnabled(_appSettings.getDocumentAutoFormatEnabled(_document.getPath()));
        _format.getActions()
                .setUiReferences(activity, _hlEditor, _webView)
                .recreateActionButtons(_textActionsBar, _isPreviewVisible ? ActionButtonBase.ActionItem.DisplayMode.VIEW : ActionButtonBase.ActionItem.DisplayMode.EDIT);
        updateMenuToggleStates(_format.getFormatId());
        showHideActionBar();
    }

    private void showHideActionBar() {
        final Activity activity = getActivity();
        if (activity != null) {
            final View bar = activity.findViewById(R.id.document__fragment__edit__text_actions_bar);
            final View parent = activity.findViewById(R.id.document__fragment__edit__text_actions_bar__scrolling_parent);
            final View editScroll = activity.findViewById(R.id.document__fragment__edit__content_editor__scrolling_parent);
            final View viewScroll = activity.findViewById(R.id.document__fragment_view_webview);

            if (bar != null && parent != null && editScroll != null && viewScroll != null) {
                final boolean hide = _textActionsBar.getChildCount() == 0;
                parent.setVisibility(hide ? View.GONE : View.VISIBLE);
                final int marginBottom = hide ? 0 : (int) getResources().getDimension(R.dimen.textactions_bar_height);
                setMarginBottom(editScroll, marginBottom);
                setMarginBottom(viewScroll, marginBottom);
            }
        }
    }

    private void setMarginBottom(final View view, final int marginBottom) {
        final ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        if (params != null) {
            params.setMargins(params.leftMargin, params.topMargin, params.rightMargin, marginBottom);
            view.setLayoutParams(params);
        }
    }

    private void updateMenuToggleStates(final int selectedFormatActionId) {
        MenuItem mi;
        if ((mi = _fragmentMenu.findItem(R.id.action_wrap_words)) != null) {
            mi.setChecked(isWrapped());
        }
        if ((mi = _fragmentMenu.findItem(R.id.action_enable_highlighting)) != null) {
            mi.setChecked(_hlEditor.getHighlightingEnabled());
        }
        if ((mi = _fragmentMenu.findItem(R.id.action_line_numbers)) != null) {
            mi.setChecked(_hlEditor.getLineNumbersEnabled());
        }
        if ((mi = _fragmentMenu.findItem(R.id.action_enable_auto_format)) != null) {
            mi.setChecked(_hlEditor.getAutoFormatEnabled());
        }

        final SubMenu su;
        if (selectedFormatActionId != 0 && (mi = _fragmentMenu.findItem(R.id.submenu_format_selection)) != null && (su = mi.getSubMenu()) != null) {
            for (int i = 0; i < su.size(); i++) {
                if ((mi = su.getItem(i)).getItemId() == selectedFormatActionId) {
                    mi.setChecked(true);
                    break;
                }
            }
        }
    }

    private boolean isWrapped() {
        return _hsView == null || (_hlEditor.getParent() == _primaryScrollView);
    }

    private void setHorizontalScrollMode(final boolean wrap) {
        final Context context = getContext();
        if (context != null && _hlEditor != null && isWrapped() != wrap) {

            final int[] sel = TextViewUtils.getSelection(_hlEditor);

            final boolean hlEnabled = _hlEditor.getHighlightingEnabled();
            _hlEditor.setHighlightingEnabled(false);

            _primaryScrollView.removeAllViews();
            if (_hsView != null) {
                _hsView.removeAllViews();
            }
            if (!wrap) {
                if (_hsView == null) {
                    _hsView = new HorizontalScrollView(context);
                    _hsView.setFillViewport(true);
                }
                _hsView.addView(_hlEditor);
                _primaryScrollView.addView(_hsView);
            } else {
                _primaryScrollView.addView(_hlEditor);
            }

            _hlEditor.setHighlightingEnabled(hlEnabled);

            // Run after layout() of immediate parent completes
            (wrap ? _primaryScrollView : _hsView).post(() -> TextViewUtils.setSelectionAndShow(_hlEditor, sel));
        }
    }

    @Override
    public String getFragmentTag() {
        return FRAGMENT_TAG;
    }

    public void errorClipText() {
        final String text = getTextString();
        if (!TextUtils.isEmpty(text)) {
            Context context = getContext();
            context = context == null ? ApplicationObject.get().getApplicationContext() : context;
            new MarkorContextUtils(context).setClipboard(getContext(), text);
        }
        // Always show error message
        Toast.makeText(getContext(), R.string.error_could_not_open_file, Toast.LENGTH_LONG).show();
        Log.i(DocumentEditAndViewFragment.class.getName(), "Triggering error text clipping");
    }

    public boolean isSdStatusBad() {
        if (_cu.isUnderStorageAccessFolder(getContext(), _document.getFile(), false) &&
                _cu.getStorageAccessFrameworkTreeUri(getContext()) == null) {
            _cu.showMountSdDialog(getActivity());
            return true;
        }
        return false;
    }

    // Checks document state if things aren't in a good state
    public boolean isStateBad() {
        return (_document == null ||
                _hlEditor == null ||
                _appSettings == null ||
                !_cu.canWriteFile(getContext(), _document.getFile(), false, true));
    }

    // Save the file
    public boolean saveDocument(final boolean forceSaveEmpty) {
        final Activity activity = getActivity();
        if (activity == null || isSdStatusBad() || isStateBad()) {
            errorClipText();
            return false;
        }

        // Document is written iff writeable && content has changed
        final CharSequence text = _hlEditor.getText();
        if (!_document.isContentSame(text)) {
            final int minLength = GsContextUtils.TEXTFILE_OVERWRITE_MIN_TEXT_LENGTH;
            if (!forceSaveEmpty && text != null && text.length() < minLength) {
                final String message = activity.getString(R.string.wont_save_min_length, minLength);
                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
                return true;
            }
            if (_document.saveContent(getActivity(), text, _cu, forceSaveEmpty)) {
                checkTextChangeState();
                return true;
            } else {
                errorClipText();
                return false; // Failure only if saveContent somehow fails
            }
        } else {
            return true; // Report success if text not changed
        }
    }

    private boolean isDisplayedAtMainActivity() {
        return getActivity() instanceof MainActivity;
    }

    public void updateViewModeText() {
        final String text = getTextString();
        _format.getConverter().convertMarkupShowInWebView(_document, text, getActivity(), _webView, _nextConvertToPrintMode, _hlEditor.getLineNumbersEnabled());
    }

    public void setViewModeVisibility(final boolean show) {
        setViewModeVisibility(show, true);
    }

    public void setViewModeVisibility(boolean show, final boolean animate) {
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        show |= _document.isBinaryFileNoTextLoading();
        _format.getActions().recreateActionButtons(_textActionsBar, show ? ActionButtonBase.ActionItem.DisplayMode.VIEW : ActionButtonBase.ActionItem.DisplayMode.EDIT);
        showHideActionBar();
        if (show) {
            updateViewModeText();
            _cu.showSoftKeyboard(activity, false, _hlEditor);
            _hlEditor.clearFocus();
            _hlEditor.postDelayed(() -> _cu.showSoftKeyboard(activity, false, _hlEditor), 300);
            GsContextUtils.fadeInOut(_webView, _primaryScrollView, animate);
        } else {
            _webViewClient.setRestoreScrollY(_webView.getScrollY());
            GsContextUtils.fadeInOut(_primaryScrollView, _webView, animate);
        }

        _nextConvertToPrintMode = false;
        _isPreviewVisible = show;

        ((AppCompatActivity) activity).supportInvalidateOptionsMenu();
    }

    // Callback from view-mode/javascript
    @SuppressWarnings("unused")
    @JavascriptInterface
    public void webViewJavascriptCallback(final String[] jsArgs) {
        final String[] args = (jsArgs == null || jsArgs.length == 0 || jsArgs[0] == null) ? new String[0] : jsArgs;
        final String type = args.length == 0 || TextUtils.isEmpty(args[0]) ? "" : args[0];
        if (type.equalsIgnoreCase("toast") && args.length == 2) {
            Toast.makeText(getActivity(), args[1], Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onToolbarClicked(View v) {
        if (_format != null) {
            _format.getActions().runTitleClick();
        }
    }

    @Override
    protected boolean onToolbarLongClicked(View v) {
        if (isVisible() && isResumed()) {
            _format.getActions().runJumpBottomTopAction(_isPreviewVisible ? ActionButtonBase.ActionItem.DisplayMode.VIEW : ActionButtonBase.ActionItem.DisplayMode.EDIT);
            return true;
        }
        return false;
    }

    @Override
    public void onDestroy() {
        try {
            _webView.loadUrl("about:blank");
            _webView.destroy();
        } catch (Exception ignored) {
        }
        super.onDestroy();
    }

    public Document getDocument() {
        return _document;
    }

    public String getTextString() {
        final CharSequence text = _hlEditor != null ? _hlEditor.getText() : null;
        return text != null ? text.toString() : "";
    }
}
