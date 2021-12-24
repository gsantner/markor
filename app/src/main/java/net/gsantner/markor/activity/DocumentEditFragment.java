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
import android.appwidget.AppWidgetManager;
import android.arch.lifecycle.Lifecycle;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Selection;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.HorizontalScrollView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import net.gsantner.markor.App;
import net.gsantner.markor.BuildConfig;
import net.gsantner.markor.R;
import net.gsantner.markor.format.TextConverter;
import net.gsantner.markor.format.TextFormat;
import net.gsantner.markor.format.general.CommonTextActions;
import net.gsantner.markor.format.general.DatetimeFormatDialog;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.AttachImageOrLinkDialog;
import net.gsantner.markor.ui.DraggableScrollbarScrollView;
import net.gsantner.markor.ui.FileInfoDialog;
import net.gsantner.markor.ui.FilesystemViewerCreator;
import net.gsantner.markor.ui.SearchOrCustomTextDialogCreator;
import net.gsantner.markor.ui.hleditor.HighlightingEditor;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ContextUtils;
import net.gsantner.markor.util.MarkorWebViewClient;
import net.gsantner.markor.util.ShareUtil;
import net.gsantner.opoc.activity.GsFragmentBase;
import net.gsantner.opoc.preference.FontPreferenceCompat;
import net.gsantner.opoc.ui.FilesystemViewerData;
import net.gsantner.opoc.util.ActivityUtils;
import net.gsantner.opoc.util.CoolExperimentalStuff;
import net.gsantner.opoc.util.StringUtils;
import net.gsantner.opoc.util.TextViewUndoRedo;

import java.io.File;
import java.util.Arrays;

import butterknife.BindView;
import butterknife.OnTextChanged;
import other.writeily.widget.WrMarkorWidgetProvider;

@SuppressWarnings({"UnusedReturnValue"})
@SuppressLint("NonConstantResourceId")
public class DocumentEditFragment extends GsFragmentBase implements TextFormat.TextFormatApplier {
    public static final int HISTORY_DELTA = 5000;
    public static final String FRAGMENT_TAG = "DocumentEditFragment";
    private static final String SAVESTATE_DOCUMENT = "DOCUMENT";
    private static final String SAVESTATE_CURSOR_POS = "CURSOR_POS";
    private static final String SAVESTATE_PREVIEW_ON = "SAVESTATE_PREVIEW_ON";

    private AppSettings _appSettings;
    private HorizontalScrollView hsView;

    // Wrap text setting and wrap text state are separated as the wrap text state may depend on
    // if the file is in the main activity (quicknote and todotxt). Documents in mainactivity
    // will _always_ open wrapped, but can be explicitly be set to unwrapped through the menu.
    // Toggling the wrap state option will set and save the new value, but the file will always
    // open wrapped in the main activity.
    private boolean wrapTextSetting;
    private boolean wrapText;
    private boolean highlightText;

    public static DocumentEditFragment newInstance(Document document) {
        DocumentEditFragment f = new DocumentEditFragment();
        Bundle args = new Bundle();
        args.putSerializable(Document.EXTRA_DOCUMENT, document);
        f.setArguments(args);
        return f;
    }

    public static DocumentEditFragment newInstance(File path, boolean pathIsFolder, final int lineNumber) {
        DocumentEditFragment f = new DocumentEditFragment();
        Bundle args = new Bundle();
        args.putSerializable(Document.EXTRA_PATH, path);
        args.putBoolean(Document.EXTRA_PATH_IS_FOLDER, pathIsFolder);
        args.putInt(Document.EXTRA_FILE_LINE_NUMBER, lineNumber);
        f.setArguments(args);
        return f;
    }

    @BindView(R.id.document__fragment__edit__highlighting_editor)
    HighlightingEditor _hlEditor;

    @BindView(R.id.document__fragment__edit__text_actions_bar)
    ViewGroup _textActionsBar;

    @BindView(R.id.document__fragment__edit__content_editor__permission_warning)
    TextView _textSdWarning;

    @BindView(R.id.document__fragment_view_webview)
    WebView _webView;

    @BindView(R.id.document__fragment__edit__content_editor__scrolling_parent)
    DraggableScrollbarScrollView _primaryScrollView;

    private SearchView _menuSearchViewForViewMode;
    private Document _document;
    private TextFormat _textFormat;
    private ShareUtil _shareUtil;
    private TextViewUndoRedo _editTextUndoRedoHelper;
    private boolean _isPreviewVisible;
    private MarkorWebViewClient _webViewClient;
    private boolean _nextConvertToPrintMode = false;
    private long _loadModTime = 0;
    private boolean _isTextChanged = false;
    private MenuItem _saveMenuItem, _undoMenuItem, _redoMenuItem;

    public DocumentEditFragment() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.document__fragment__edit;
    }

    @SuppressLint({"SetJavaScriptEnabled", "WrongConstant"})
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Activity activity = getActivity();

        _appSettings = new AppSettings(activity);
        if (_appSettings.getSetWebViewFulldrawing() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            WebView.enableSlowWholeDocumentDraw();
        }

        _shareUtil = new ShareUtil(activity);

        _webViewClient = new MarkorWebViewClient(activity);
        _webView.setBackgroundColor(ContextCompat.getColor(view.getContext(), _appSettings.isDarkThemeEnabled() ? R.color.dark__background : R.color.light__background));
        _webView.setWebViewClient(_webViewClient);
        WebSettings webSettings = _webView.getSettings();
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setTextZoom((int) (_appSettings.getViewFontSize() / 15.7f * 100f));
        webSettings.setAppCacheEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setGeolocationEnabled(false);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && BuildConfig.IS_TEST_BUILD && BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true); // Inspect on computer chromium browser: chrome://inspect/#devices
        }

        int intentLineNumber = -1;
        if (savedInstanceState != null && savedInstanceState.containsKey(SAVESTATE_DOCUMENT)) {
            _document = (Document) savedInstanceState.getSerializable(SAVESTATE_DOCUMENT);
        } else {
            _document = Document.fromArguments(activity, getArguments());
            intentLineNumber = _document.getIntentLineNumber();
        }

        // Upon construction, the document format has been determined from extension etc
        // Here we replace it with the last saved format.
        _document.setFormat(_appSettings.getDocumentFormat(_document.getPath(), _document.getFormat()));
        applyTextFormat(_document.getFormat());
        _textFormat.getTextActions().setDocument(_document);

        loadDocument();

        if (activity instanceof DocumentActivity) {
            ((DocumentActivity) activity).setDocument(_document);
        }

        _editTextUndoRedoHelper = new TextViewUndoRedo(_hlEditor);
        new ActivityUtils(activity).hideSoftKeyboard().freeContextRef();
        _hlEditor.clearFocus();
        _hlEditor.setLineSpacing(0, _appSettings.getEditorLineSpacing());
        setupAppearancePreferences(view);

        if (savedInstanceState != null && savedInstanceState.containsKey(SAVESTATE_PREVIEW_ON)) {
            _isPreviewVisible = savedInstanceState.getBoolean(SAVESTATE_PREVIEW_ON, _isPreviewVisible);
        }

        final Toolbar toolbar = getToolbar();
        if (toolbar != null) {
            toolbar.setOnLongClickListener(_longClickToTopOrBottom);
        }

        // Do not need to send contents to accessibility
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            _hlEditor.setImportantForAccessibility(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
        }

        // Set the correct position after everything else done
        if (!isDisplayedAtMainActivity() && !Arrays.asList(_hlEditor, _webView, _document.getFile()).contains(null)) {
            // Scroll to position
            // If Intent contains line number, jump to it
            // intentLineNumber only created with document reconstructed from intent
            if (intentLineNumber >= 0) {
                _hlEditor.smoothMoveCursorToLine(intentLineNumber);
            }

            // Set cursor if saved cursor state present
            final int pos = savedInstanceState != null ? savedInstanceState.getInt(SAVESTATE_CURSOR_POS, -1) : -1;
            final CharSequence text = _hlEditor.getText();
            if (_hlEditor.indexesValid(pos) && text != null) {
                _hlEditor.smoothMoveCursorToLine(StringUtils.getLineOffsetFromIndex(text, pos)[0]);
                _hlEditor.setSelection(pos);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        loadDocument();

        _hlEditor.setGravity(_appSettings.isEditorStartEditingInCenter() ? Gravity.CENTER : Gravity.NO_GRAVITY);

        if (_document != null) {
            _document.testCreateParent();
            boolean permok = _shareUtil.canWriteFile(_document.getFile(), false);
            if (!permok && !_document.getFile().isDirectory() && _shareUtil.canWriteFile(_document.getFile(), _document.getFile().isDirectory())) {
                permok = true;
            }
            if (_shareUtil.isUnderStorageAccessFolder(_document.getFile()) && _shareUtil.getStorageAccessFrameworkTreeUri() == null) {
                _shareUtil.showMountSdDialog(getActivity());
                return;
            }
            _textSdWarning.setVisibility(permok ? View.GONE : View.VISIBLE);
        }

        if (_document != null && _document.getFile().getAbsolutePath().contains("mordor/1-epub-experiment.md") && getActivity() instanceof DocumentActivity) {
            _hlEditor.setText(CoolExperimentalStuff.convertEpubToText(_document.getFile(), getString(R.string.page)));
        }

        // Set initial wrap state
        initDocState();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.document__edit__menu, menu);
        ContextUtils cu = ContextUtils.get();
        cu.tintMenuItems(menu, true, Color.WHITE);
        cu.setSubMenuIconsVisiblity(menu, true);

        menu.findItem(R.id.action_undo).setVisible(_appSettings.isEditorHistoryEnabled());
        menu.findItem(R.id.action_redo).setVisible(_appSettings.isEditorHistoryEnabled());
        menu.findItem(R.id.action_send_debug_log).setVisible(MainActivity.IS_DEBUG_ENABLED && getActivity() instanceof DocumentActivity && !_isPreviewVisible);

        final boolean isExperimentalFeaturesEnabled = _appSettings.isExperimentalFeaturesEnabled();

        // Undo / Redo / Save (keep visible, but deactivated and tinted grey if not executable)
        _undoMenuItem = menu.findItem(R.id.action_undo).setVisible(!_isPreviewVisible);
        _redoMenuItem = menu.findItem(R.id.action_redo).setVisible(!_isPreviewVisible);
        _saveMenuItem = menu.findItem(R.id.action_save).setVisible(!_isPreviewVisible);

        // Edit / Preview switch
        menu.findItem(R.id.action_edit).setVisible(_isPreviewVisible);
        menu.findItem(R.id.submenu_attach).setVisible(false);
        menu.findItem(R.id.action_preview).setVisible(!_isPreviewVisible);
        menu.findItem(R.id.action_search).setVisible(!_isPreviewVisible);
        menu.findItem(R.id.action_search_view).setVisible(_isPreviewVisible);
        menu.findItem(R.id.submenu_format_selection).setVisible(!_isPreviewVisible);

        menu.findItem(R.id.action_share_pdf).setVisible(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT);
        menu.findItem(R.id.action_share_image).setVisible(true);
        menu.findItem(R.id.action_load_epub).setVisible(isExperimentalFeaturesEnabled);

        // SearchView (View Mode)
        _menuSearchViewForViewMode = (SearchView) menu.findItem(R.id.action_search_view).getActionView();
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

        // Set various initial states
        updateMenuToggleStates(_document.getFormat());
        checkTextChangeState();
        updateUndoRedoIconStates();
    }

    private void updateUndoRedoIconStates() {
        if (_editTextUndoRedoHelper == null) {
            return;
        }

        final boolean canUndo = _editTextUndoRedoHelper.getCanUndo();
        if (_undoMenuItem != null && _undoMenuItem.isEnabled() != canUndo) {
            _undoMenuItem.setEnabled(canUndo).getIcon().mutate().setAlpha(canUndo ? 255 : 40);
        }

        final boolean canRedo = _editTextUndoRedoHelper.getCanRedo();
        if (_redoMenuItem != null && _redoMenuItem.isEnabled() != canRedo) {
            _redoMenuItem.setEnabled(canRedo).getIcon().mutate().setAlpha(canRedo ? 255 : 40);
        }
    }

    public void loadDocument() {
        //Only trigger the load process if constructing or file updated
        final long modTime = _document.lastModified();
        if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED) || modTime > _loadModTime) {

            _loadModTime = modTime;

            final String content = _document.loadContent(getContext());
            if (!_document.isContentSame(_hlEditor.getText())) {

                final int[] sel = StringUtils.getSelection(_hlEditor);
                sel[0] = Math.min(sel[0], content.length());
                sel[1] = Math.min(sel[1], content.length());

                _hlEditor.setText(content);

                _hlEditor.setSelection(sel[0], sel[1]); // hleditor can handle invalid selections
            }

            checkTextChangeState();

            if (_isPreviewVisible) {
                setDocumentViewVisibility(true);
                _webViewClient.setRestoreScrollY(_webView.getScrollY());
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item == null) {
            return true;
        }
        _shareUtil.setContext(getContext());
        final Activity activity = getActivity();
        final int itemId = item.getItemId();
        switch (itemId) {
            case R.id.action_undo: {
                if (_editTextUndoRedoHelper.getCanUndo()) {
                    _hlEditor.disableHighlighterAutoFormat();
                    _editTextUndoRedoHelper.undo();
                    _hlEditor.enableHighlighterAutoFormat();
                    updateUndoRedoIconStates();
                }
                return true;
            }
            case R.id.action_redo: {
                if (_editTextUndoRedoHelper.getCanRedo()) {
                    _hlEditor.disableHighlighterAutoFormat();
                    _editTextUndoRedoHelper.redo();
                    _hlEditor.enableHighlighterAutoFormat();
                    updateUndoRedoIconStates();
                }
                return true;
            }
            case R.id.action_save: {
                saveDocument(true);
                return true;
            }
            case R.id.action_reload: {
                final long oldModTime = _loadModTime;
                loadDocument();
                // Use modtime to show toast if document updated
                if (_loadModTime != oldModTime) {
                    Toast.makeText(activity, "✔", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            case R.id.action_preview: {
                setDocumentViewVisibility(true);
                return true;
            }
            case R.id.action_edit: {
                setDocumentViewVisibility(false);
                return true;
            }
            case R.id.action_preview_edit_toggle: {
                setDocumentViewVisibility(!_isPreviewVisible);
                return true;
            }
            case R.id.action_add_shortcut_launcher_home: {
                _shareUtil.createLauncherDesktopShortcut(_document);
                return true;
            }
            case R.id.action_share_text: {
                if (saveDocument(false)) {
                    _shareUtil.shareText(_hlEditor.getText().toString(), "text/plain");
                }
                return true;
            }
            case R.id.action_share_file: {
                if (saveDocument(false)) {
                    _shareUtil.shareStream(_document.getFile(), "text/plain");
                }
                return true;
            }
            case R.id.action_share_html:
            case R.id.action_share_html_source: {
                if (saveDocument(false)) {
                    TextConverter converter = TextFormat.getFormat(_document.getFormat(), activity, _document, _hlEditor).getConverter();
                    _shareUtil.shareText(converter.convertMarkup(_hlEditor.getText().toString(), _hlEditor.getContext(), false, _document.getFile()),
                            "text/" + (item.getItemId() == R.id.action_share_html ? "html" : "plain"));
                }
                return true;
            }
            case R.id.action_share_calendar_event: {
                if (saveDocument(false)) {
                    if (!_shareUtil.createCalendarAppointment(_document.getTitle(), _hlEditor.getText().toString(), null)) {
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
                    setDocumentViewVisibility(true);
                    Toast.makeText(activity, R.string.please_wait, Toast.LENGTH_LONG).show();
                    _webView.postDelayed(() -> {
                        if (item.getItemId() == R.id.action_share_pdf && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            _shareUtil.printOrCreatePdfFromWebview(_webView, _document, _hlEditor.getText().toString().contains("beamer\n"));
                        } else if (item.getItemId() != R.id.action_share_pdf) {
                            _shareUtil.shareImage(net.gsantner.opoc.util.ShareUtil.getBitmapFromWebView(_webView, item.getItemId() == R.id.action_share_image));
                        }
                    }, 7000);
                }

                return true;
            }
            case R.string.action_format_zimwiki:
            case R.string.action_format_keyvalue:
            case R.string.action_format_todotxt:
            case R.string.action_format_plaintext:
            case R.string.action_format_markdown: {
                if (_document != null) {
                    _document.setFormat(itemId);
                    applyTextFormat(itemId);
                    _appSettings.setDocumentFormat(_document.getPath(), _document.getFormat());
                }
                return true;
            }
            case R.id.action_search: {
                setDocumentViewVisibility(false);
                _textFormat.getTextActions().runAction(CommonTextActions.ACTION_SEARCH);
                return true;
            }
            case R.id.action_send_debug_log: {
                final String text = AppSettings.getDebugLog() + "\n\n------------------------\n\n\n\n" + Document.getMaskedContent(_hlEditor.getText().toString());
                _shareUtil.draftEmail("Debug Log " + getString(R.string.app_name_real), text, "debug@localhost.lan");
                return true;
            }

            case R.id.action_attach_color: {
                new CommonTextActions(activity, _hlEditor).runAction(CommonTextActions.ACTION_COLOR_PICKER);
                return true;
            }
            case R.id.action_attach_date: {
                DatetimeFormatDialog.showDatetimeFormatDialog(activity, _hlEditor);
                return true;
            }
            case R.id.action_attach_audio:
            case R.id.action_attach_file:
            case R.id.action_attach_image:
            case R.id.action_attach_link: {
                int actionId = (itemId == R.id.action_attach_audio ? 4 : (itemId == R.id.action_attach_image ? 2 : 3));
                AttachImageOrLinkDialog.showInsertImageOrLinkDialog(actionId, _document.getFormat(), activity, _hlEditor, _document.getFile());
                return true;
            }

            case R.id.action_load_epub: {
                FilesystemViewerCreator.showFileDialog(new FilesystemViewerData.SelectionListenerAdapter() {
                                                           @Override
                                                           public void onFsViewerSelected(String request, File file, final Integer lineNumber) {
                                                               _hlEditor.setText(CoolExperimentalStuff.convertEpubToText(file, getString(R.string.page)));
                                                           }

                                                           @Override
                                                           public void onFsViewerConfig(FilesystemViewerData.Options dopt) {
                                                               dopt.titleText = R.string.select;
                                                           }
                                                       }, getFragmentManager(), activity,
                        input -> input != null && input.getAbsolutePath().toLowerCase().endsWith(".epub")
                );
                return true;
            }
            case R.id.action_speed_read: {
                CoolExperimentalStuff.showSpeedReadDialog(activity, _hlEditor.getText().toString());
                return true;
            }
            case R.id.action_wrap_words: {
                wrapText = !wrapText;
                wrapTextSetting = wrapText;
                _appSettings.setDocumentWrapState(_document.getPath(), wrapTextSetting);
                setHorizontalScrollMode(wrapText);
                updateMenuToggleStates(0);
                return true;
            }
            case R.id.action_enable_highlighting: {
                highlightText = !highlightText;
                _hlEditor.setHighlightingEnabled(highlightText);
                _appSettings.setDocumentHighlightState(_document.getPath(), highlightText);
                updateMenuToggleStates(0);
                return true;
            }
            case R.id.action_info: {
                if (_document != null) {
                    saveDocument(false); // In order to have the correct info displayed
                    FileInfoDialog.show(_document.getFile(), getFragmentManager());
                }
                return true;
            }
            case R.id.action_set_font_size: {
                SearchOrCustomTextDialogCreator.showFontSizeDialog(activity, _appSettings.getDocumentFontSize(_document.getPath()), (newSize) -> {
                    _hlEditor.setTextSize(TypedValue.COMPLEX_UNIT_SP, (float) newSize);
                    _appSettings.setDocumentFontSize(_document.getPath(), newSize);
                });
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    @OnTextChanged(value = R.id.document__fragment__edit__highlighting_editor, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    public void onContentEditValueChanged(CharSequence text) {
        checkTextChangeState();
        updateUndoRedoIconStates();
    }

    public void checkTextChangeState() {
        _isTextChanged = !_document.isContentSame(_hlEditor.getText());

        if (_saveMenuItem != null && _saveMenuItem.isEnabled() != _isTextChanged) {
            _saveMenuItem.setEnabled(_isTextChanged).getIcon().mutate().setAlpha(_isTextChanged ? 255 : 40);
        }
    }

    public void applyTextFormat(final int textFormatId) {
        _textActionsBar.removeAllViews();
        _textFormat = TextFormat.getFormat(textFormatId, getActivity(), _document, _hlEditor);
        _hlEditor.setHighlighter(_textFormat.getHighlighter());
        _hlEditor.enableHighlighterAutoFormat();
        _textFormat.getTextActions()
                .setHighlightingEditor(_hlEditor)
                .appendTextActionsToBar(_textActionsBar);

        updateMenuToggleStates(textFormatId);
    }

    private void setupAppearancePreferences(View fragmentView) {
        _hlEditor.setTextSize(TypedValue.COMPLEX_UNIT_SP, _appSettings.getDocumentFontSize(_document.getPath()));
        _hlEditor.setTypeface(FontPreferenceCompat.typeface(getContext(), _appSettings.getFontFamily(), Typeface.NORMAL));

        _hlEditor.setBackgroundColor(_appSettings.getEditorBackgroundColor());
        _hlEditor.setTextColor(_appSettings.getEditorForegroundColor());
        fragmentView.findViewById(R.id.document__fragment__edit__text_actions_bar__scrolling_parent).setBackgroundColor(_appSettings.getEditorTextactionBarColor());
    }

    private void initDocState() {
        wrapTextSetting = _appSettings.getDocumentWrapState(_document.getPath());
        wrapText = isDisplayedAtMainActivity() || wrapTextSetting;

        highlightText = _appSettings.getDocumentHighlightState(_document.getPath(), _hlEditor.getText());
        updateMenuToggleStates(0);

        setHorizontalScrollMode(wrapText);
        _hlEditor.setHighlightingEnabled(highlightText);
    }

    private void updateMenuToggleStates(final int selectedFormatActionId) {
        MenuItem mi;
        SubMenu su;
        if ((mi = _fragmentMenu.findItem(R.id.action_wrap_words)) != null) {
            mi.setChecked(wrapText);
        }
        if ((mi = _fragmentMenu.findItem(R.id.action_enable_highlighting)) != null) {
            mi.setChecked(highlightText);
        }

        if (selectedFormatActionId != 0 && (mi = _fragmentMenu.findItem(R.id.submenu_format_selection)) != null && (su = mi.getSubMenu()) != null) {
            for (int i = 0; i < su.size(); i++) {
                if ((mi = su.getItem(i)).getItemId() == selectedFormatActionId) {
                    mi.setChecked(true);
                }
            }
        }
    }

    private void setHorizontalScrollMode(final boolean wrap) {

        final Context context = getContext();
        if (context != null && _hlEditor != null) {
            _primaryScrollView.removeAllViews();
            if (hsView != null) {
                hsView.removeAllViews();
            }
            if (!wrap) {
                _hlEditor.setHorizontallyScrolling(true);
                if (hsView == null) {
                    hsView = new HorizontalScrollView(context);
                    hsView.setFillViewport(true);
                }
                hsView.addView(_hlEditor);
                _primaryScrollView.addView(hsView);
            } else {
                _hlEditor.setHorizontallyScrolling(false);
                _primaryScrollView.addView(_hlEditor);
            }
        }
    }

    @Override
    public String getFragmentTag() {
        return FRAGMENT_TAG;
    }

    // Save the file
    // Only supports java.io.File. TODO: Android Content
    public boolean saveDocument(boolean forceSaveEmpty) {
        // Document is written iff content has changed
        // _isTextChanged implies _document != null && _hlEditor != null && _hlEditor.getText() != null
        if (_isTextChanged && isAdded()) {

            _appSettings.setLastEditPosition(_document.getFile(), _hlEditor.getSelectionStart());

            if (_document.saveContent(getContext(), _hlEditor.getText().toString(), _shareUtil, forceSaveEmpty)) {
                updateLauncherWidgets();
                checkTextChangeState();
                return true;
            }
        }
        return false;
    }

    private boolean isDisplayedAtMainActivity() {
        return getActivity() instanceof MainActivity;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (_hlEditor != null) {
            outState.putSerializable(SAVESTATE_CURSOR_POS, _hlEditor.getSelectionStart());
        }
        outState.putSerializable(SAVESTATE_DOCUMENT, _document);
        outState.putBoolean(SAVESTATE_PREVIEW_ON, _isPreviewVisible);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        saveDocument(false);
        if (_document != null) {
            _appSettings.addRecentDocument(_document.getFile());
            _appSettings.setDocumentPreviewState(_document.getPath(), _isPreviewVisible);
        }
        super.onPause();
    }

    private void updateLauncherWidgets() {
        Context c = App.get().getApplicationContext();
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(c);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(c, WrMarkorWidgetProvider.class));
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_notes_list);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        // This function can be called _outside_ the normal lifecycle!
        // Do nothing if the fragment is not at least created!
        if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.CREATED)) {
            return;
        }

        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && isDisplayedAtMainActivity()) {
            loadDocument();
            _primaryScrollView.postDelayed(() -> _primaryScrollView.fullScroll(View.FOCUS_DOWN), 100);
        } else if (!isVisibleToUser && _document != null) {
            saveDocument(false);
        }

        final Toolbar toolbar = getToolbar();
        if (toolbar != null && isVisibleToUser) {
            toolbar.setOnLongClickListener(_longClickToTopOrBottom);
        }

        if (isVisibleToUser) {
            initDocState();
        }
    }

    @Override
    public void onFragmentFirstTimeVisible() {
        final boolean initPreview = _appSettings.getDocumentPreviewState(_document.getPath());
        if (_savedInstanceState == null || !_savedInstanceState.containsKey(SAVESTATE_CURSOR_POS) && _hlEditor.length() > 0) {
            int lastPos;
            if (_document != null && (lastPos = _appSettings.getLastEditPositionChar(_document.getFile())) >= 0 && lastPos <= _hlEditor.length()) {
                if (!initPreview) {
                    _hlEditor.requestFocus();
                }
                _hlEditor.setSelection(lastPos);
            } else if (_appSettings.isEditorStartOnBotttom()) {
                if (!initPreview) {
                    _hlEditor.requestFocus();
                }
                _hlEditor.setSelection(_hlEditor.length());
            }
        }
    }

    public void setDocumentViewVisibility(boolean show) {
        final Activity activity = getActivity();
        if (!show) {
            _webViewClient.setRestoreScrollY(_webView.getScrollY());
        } else {
            _textFormat.getConverter().convertMarkupShowInWebView(_document, _hlEditor.getText().toString(), _webView, _nextConvertToPrintMode);
            new ActivityUtils(activity).hideSoftKeyboard().freeContextRef();
            _hlEditor.clearFocus();
            _hlEditor.postDelayed(() -> new ActivityUtils(activity).hideSoftKeyboard().freeContextRef(), 300);
        }

        _nextConvertToPrintMode = false;
        _webView.setAlpha(0);
        _webView.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            _webView.animate().setDuration(150).alpha(1.0f).setListener(null);
        }

        _isPreviewVisible = show;
        ((AppCompatActivity) activity).supportInvalidateOptionsMenu();
    }

    final View.OnLongClickListener _longClickToTopOrBottom = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            if (getUserVisibleHint()) {
                if (_isPreviewVisible) {
                    boolean top = _webView.getScrollY() > 100;
                    _webView.scrollTo(0, top ? 0 : _webView.getContentHeight());
                    if (!top) {
                        _webView.scrollBy(0, 1000);
                        _webView.scrollBy(0, 1000);
                    }
                } else {
                    new CommonTextActions(getActivity(), _hlEditor).runAction(CommonTextActions.ACTION_JUMP_BOTTOM_TOP);
                }
                return true;
            }
            return false;
        }
    };

    //
    //
    //
    //

    public Document getDocument() {
        return _document;
    }

    public WebView getWebview() {
        return _webView;
    }

    public DocumentEditFragment setPreviewFlag(boolean preview) {
        _isPreviewVisible = preview;
        return this;
    }

    public void onToolbarTitleClicked(final Toolbar toolbar) {
        if (!_isPreviewVisible && _textFormat != null) {
            _textFormat.getTextActions().runAction(getString(R.string.tmaid_common_toolbar_title_clicked_edit_action));
        }
    }
}
