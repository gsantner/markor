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

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.gsantner.markor.App;
import net.gsantner.markor.R;
import net.gsantner.markor.format.TextConverter;
import net.gsantner.markor.format.TextFormat;
import net.gsantner.markor.format.general.CommonTextActions;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.hleditor.HighlightingEditor;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ContextUtils;
import net.gsantner.markor.util.DocumentIO;
import net.gsantner.markor.util.MarkorWebViewClient;
import net.gsantner.markor.util.ShareUtil;
import net.gsantner.opoc.activity.GsFragmentBase;
import net.gsantner.opoc.preference.FontPreferenceCompat;
import net.gsantner.opoc.util.ActivityUtils;
import net.gsantner.opoc.util.TextViewUndoRedo;

import java.io.File;

import butterknife.BindView;
import butterknife.OnTextChanged;
import other.writeily.widget.WrMarkorWidgetProvider;

@SuppressWarnings({"UnusedReturnValue", "RedundantCast"})
public class DocumentEditFragment extends GsFragmentBase implements TextFormat.TextFormatApplier {
    public static final int HISTORY_DELTA = 5000;
    public static final String FRAGMENT_TAG = "DocumentEditFragment";
    private static final String SAVESTATE_DOCUMENT = "DOCUMENT";
    private static final String SAVESTATE_CURSOR_POS = "CURSOR_POS";
    private static final String SAVESTATE_PREVIEW_ON = "SAVESTATE_PREVIEW_ON";

    public static DocumentEditFragment newInstance(Document document) {
        DocumentEditFragment f = new DocumentEditFragment();
        Bundle args = new Bundle();
        args.putSerializable(DocumentIO.EXTRA_DOCUMENT, document);
        f.setArguments(args);
        return f;
    }

    public static DocumentEditFragment newInstance(File path, boolean pathIsFolder, boolean allowRename) {
        DocumentEditFragment f = new DocumentEditFragment();
        Bundle args = new Bundle();
        args.putSerializable(DocumentIO.EXTRA_PATH, path);
        args.putBoolean(DocumentIO.EXTRA_PATH_IS_FOLDER, pathIsFolder);
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

    private Document _document;
    private TextFormat _textFormat;
    private ShareUtil _shareUtil;
    private TextViewUndoRedo _editTextUndoRedoHelper;
    private boolean _isPreviewVisible;
    private MarkorWebViewClient _webViewClient;
    private boolean _nextConvertToPrintMode = false;

    public DocumentEditFragment() {
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.document__fragment__edit;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //applyTextFormat(TextFormat.FORMAT_PLAIN);
        setupAppearancePreferences(view);
        _shareUtil = new ShareUtil(view.getContext());

        AppSettings appSettings = new AppSettings(view.getContext());
        _webViewClient = new MarkorWebViewClient(getActivity());
        _webView.setBackgroundColor(ContextCompat.getColor(view.getContext(), appSettings.isDarkThemeEnabled() ? R.color.dark__background : R.color.light__background));
        _webView.setWebViewClient(_webViewClient);
        WebSettings webSettings = _webView.getSettings();
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setTextZoom((int) (appSettings.getFontSize() / 17f * 100f));
        webSettings.setAppCacheEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setGeolocationEnabled(false);
        webSettings.setJavaScriptEnabled(true);

        if (savedInstanceState != null && savedInstanceState.containsKey(SAVESTATE_DOCUMENT)) {
            _document = (Document) savedInstanceState.getSerializable(SAVESTATE_DOCUMENT);
        }
        _document = loadDocument();
        loadDocumentIntoUi();
        if (savedInstanceState != null && savedInstanceState.containsKey(SAVESTATE_CURSOR_POS)) {
            int cursor = savedInstanceState.getInt(SAVESTATE_CURSOR_POS);
            if (cursor >= 0 && cursor < _hlEditor.length()) {
                _hlEditor.setSelection(cursor);
            }
        }
        _editTextUndoRedoHelper = new TextViewUndoRedo(_hlEditor);

        new ActivityUtils(getActivity()).hideSoftKeyboard();
        _hlEditor.clearFocus();
        _hlEditor.setLineSpacing(0, appSettings.getEditorLineSpacing());


        if (savedInstanceState != null && savedInstanceState.containsKey(SAVESTATE_PREVIEW_ON)) {
            _isPreviewVisible = savedInstanceState.getBoolean(SAVESTATE_PREVIEW_ON, _isPreviewVisible);
        }
        if (_isPreviewVisible) {
            setDocumentViewVisibility(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        checkReloadDisk(false);
        int cursor = _hlEditor.getSelectionStart();
        cursor = Math.max(0, cursor);
        cursor = Math.min(_hlEditor.length(), cursor);
        _hlEditor.setSelection(cursor);

        AppSettings appSettings = new AppSettings(getContext());
        _hlEditor.setGravity(appSettings.isEditorStartEditingInCenter() ? Gravity.CENTER : Gravity.NO_GRAVITY);
        if (_document != null && _document.getFile() != null) {
            if (!_document.getFile().getParentFile().exists()) {
                //noinspection ResultOfMethodCallIgnored
                _document.getFile().getParentFile().mkdirs();
            }
            boolean permok = _shareUtil.canWriteFile(_document.getFile(), false);
            if (!permok && !_document.getFile().isDirectory() && _shareUtil.canWriteFile(_document.getFile(), _document.getFile().isDirectory())) {
                permok = true;
            }
            _textSdWarning.setVisibility(permok ? View.GONE : View.VISIBLE);
        }

        /*if (_savedInstanceState != null && _savedInstanceState.containsKey("undoredopref")) {
            _hlEditor.postDelayed(() -> {
                SharedPreferences sp = getContext().getSharedPreferences("unforedopref", 0);
                _editTextUndoRedoHelper.restorePersistentState(sp, _editTextUndoRedoHelper.undoRedoPrefKeyForFile(_document.getFile()));
            }, 100);
        }*/
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.document__edit__menu, menu);
        ContextUtils cu = ContextUtils.get();
        cu.tintMenuItems(menu, true, Color.WHITE);
        cu.setSubMenuIconsVisiblity(menu, true);

        AppSettings appSettings = new AppSettings(getActivity());
        menu.findItem(R.id.action_undo).setVisible(appSettings.isEditorHistoryEnabled());
        menu.findItem(R.id.action_redo).setVisible(appSettings.isEditorHistoryEnabled());

        boolean isTextEmpty;
        boolean canUndo = _editTextUndoRedoHelper.getCanUndo();
        boolean canRedo = _editTextUndoRedoHelper.getCanRedo();

        // Undo / Redo / Save (keep visible, but deactivated and tinted grey if not executable)
        Drawable drawable;
        drawable = menu.findItem(R.id.action_undo).setEnabled(canUndo).setVisible(!_isPreviewVisible).getIcon();
        drawable.mutate().setAlpha(canUndo ? 255 : 40);
        drawable = menu.findItem(R.id.action_redo).setEnabled(canRedo).setVisible(!_isPreviewVisible).getIcon();
        drawable.mutate().setAlpha(canRedo ? 255 : 40);
        isTextEmpty = !(_document.getContent().isEmpty() || _document.getTitle().isEmpty());
        drawable = menu.findItem(R.id.action_save).setEnabled(isTextEmpty).getIcon();
        drawable.mutate().setAlpha(isTextEmpty ? 255 : 40);

        // Edit / Preview switch
        menu.findItem(R.id.action_edit).setVisible(_isPreviewVisible);
        menu.findItem(R.id.action_preview).setVisible(!_isPreviewVisible);


        menu.findItem(R.id.action_share_pdf).setVisible(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT);
        menu.findItem(R.id.action_share_image).setVisible(true);
    }

    public void loadDocumentIntoUi() {
        int editorpos = _hlEditor.getSelectionStart();
        _hlEditor.setText(_document.getContent());
        editorpos = editorpos > _hlEditor.length() ? _hlEditor.length() - 1 : editorpos;
        _hlEditor.setSelection(editorpos < 0 ? 0 : editorpos);
        Activity activity = getActivity();
        if (activity != null && activity instanceof DocumentActivity) {
            DocumentActivity da = ((DocumentActivity) activity);
            da.setDocumentTitle(_document.getTitle());
            da.setDocument(_document);
        }
        applyTextFormat(_document.getFormat());
        _textFormat.getTextActions().setDocument(_document);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_undo: {
                if (_editTextUndoRedoHelper.getCanUndo()) {
                    _editTextUndoRedoHelper.undo();
                    ((AppCompatActivity) getActivity()).supportInvalidateOptionsMenu();
                }
                return true;
            }
            case R.id.action_redo: {
                if (_editTextUndoRedoHelper.getCanRedo()) {
                    _editTextUndoRedoHelper.redo();
                    ((AppCompatActivity) getActivity()).supportInvalidateOptionsMenu();
                }
                return true;
            }
            case R.id.action_save: {
                saveDocument();
                return true;
            }
            case R.id.action_reload: {
                checkReloadDisk(true);
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
                if (saveDocument()) {
                    _shareUtil.shareText(_document.getContent(), "text/plain");
                }
                return true;
            }
            case R.id.action_share_file: {
                if (saveDocument()) {
                    _shareUtil.shareStream(_document.getFile(), "text/plain");
                }
                return true;
            }
            case R.id.action_share_html:
            case R.id.action_share_html_source: {
                if (saveDocument()) {
                    TextConverter converter = TextFormat.getFormat(_document.getFormat(), getActivity(), _document, _hlEditor).getConverter();
                    converter.setCurrentFileExt(_document.getFileExtension());
                    _shareUtil.shareText(converter.convertMarkup(_document.getContent(), _hlEditor.getContext(), false),
                            "text/" + (item.getItemId() == R.id.action_share_html ? "html" : "plain"));
                }
                return true;
            }
            case R.id.action_share_calendar_event: {
                if (saveDocument()) {
                    if (!_shareUtil.createCalendarAppointment(_document.getTitle(), _document.getContent(), null)) {
                        Toast.makeText(getActivity(), R.string.no_calendar_app_is_installed, Toast.LENGTH_SHORT).show();
                    }
                }
                return true;
            }
            case R.id.action_share_image:
            case R.id.action_share_pdf: {
                if (saveDocument()) {
                    _nextConvertToPrintMode = true;
                    setDocumentViewVisibility(true);
                    Toast.makeText(getActivity(), R.string.please_wait, Toast.LENGTH_LONG).show();
                    _webView.postDelayed(() -> {
                        if (item.getItemId() == R.id.action_share_pdf && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            _shareUtil.printOrCreatePdfFromWebview(_webView, _document);
                        } else if (item.getItemId() == R.id.action_share_image) {
                            _shareUtil.shareImage(net.gsantner.opoc.util.ShareUtil.getBitmapFromWebView(_webView), Bitmap.CompressFormat.JPEG);
                        }
                    }, 6000);
                }

                return true;
            }
            case R.id.action_format_keyvalue:
            case R.id.action_format_todotxt:
            case R.id.action_format_plaintext:
            case R.id.action_format_markdown: {
                if (_document != null) {
                    _document.setFormat(item.getItemId());
                    applyTextFormat(item.getItemId());
                }
                return true;
            }
            case R.id.action_search: {
                setDocumentViewVisibility(false);
                _textFormat.getTextActions().runAction(CommonTextActions.ACTION_SEARCH);
                return true;
            }
            case R.id.action_record_audio: {
                FragmentManager fm = getChildFragmentManager();
                AudioToNoteFragment editNameDialogFragment = AudioToNoteFragment.newInstance(_document.getFile().getPath());
                editNameDialogFragment.show(fm, "fragment_edit_name");

                return true;

            }
        }
        return super.onOptionsItemSelected(item);
    }

    private long _lastChangedThreadStart = 0;

    @OnTextChanged(value = R.id.document__fragment__edit__highlighting_editor, callback = OnTextChanged.Callback.TEXT_CHANGED)
    public void onContentEditValueChanged(CharSequence text) {
        if ((_lastChangedThreadStart + HISTORY_DELTA) < System.currentTimeMillis()) {
            _lastChangedThreadStart = System.currentTimeMillis();
            _hlEditor.postDelayed(() -> {
                _document.setContent(text.toString());
                Activity activity = getActivity();
                if (activity != null && activity instanceof AppCompatActivity) {
                    ((AppCompatActivity) activity).supportInvalidateOptionsMenu();
                }
            }, HISTORY_DELTA);
        }
        Activity activity = getActivity();
        if (activity != null && activity instanceof AppCompatActivity) {
            ((AppCompatActivity) activity).supportInvalidateOptionsMenu();
        }

    }

    @SuppressWarnings({"ConstantConditions", "ResultOfMethodCallIgnored"})
    private Document loadDocument() {
        AppSettings appSettings = new AppSettings(getActivity().getApplicationContext());
        Document document = DocumentIO.loadDocument(getActivity(), getArguments(), _document);
        if (document != null) {
            document.setDoHistory(appSettings.isEditorHistoryEnabled());
        }
        if (document.getHistory().isEmpty()) {
            document.forceAddNextChangeToHistory();
            document.addToHistory();
        }

        return document;
    }

    public void applyTextFormat(int textFormatId) {
        _textActionsBar.removeAllViews();
        _textFormat = TextFormat.getFormat(textFormatId, getActivity(), _document, _hlEditor);
        _hlEditor.setHighlighter(_textFormat.getHighlighter());
        _textFormat.getTextActions()
                .setHighlightingEditor(_hlEditor)
                .appendTextActionsToBar(_textActionsBar);
        if (_textActionsBar.getChildCount() == 0) {
            _textActionsBar.setVisibility(View.GONE);
        } else {
            _textActionsBar.setVisibility(View.VISIBLE);
        }
    }

    private void setupAppearancePreferences(View fragmentView) {
        boolean isInMainActivity = getActivity() instanceof MainActivity;
        AppSettings as = AppSettings.get();
        _hlEditor.setTextSize(TypedValue.COMPLEX_UNIT_SP, as.getFontSize());
        _hlEditor.setTypeface(FontPreferenceCompat.typeface(getContext(), as.getFontFamily(), Typeface.NORMAL));
        _hlEditor.setHorizontallyScrolling(!as.isEditorLineBreakingEnabled() && !isInMainActivity);

        _hlEditor.setBackgroundColor(as.getEditorBackgroundColor());
        _hlEditor.setTextColor(as.getEditorForegroundColor());
        fragmentView.findViewById(R.id.document__fragment__edit__text_actions_bar__scrolling_parent).setBackgroundColor(as.getEditorTextactionBarColor());
    }

    @Override
    public String getFragmentTag() {
        return FRAGMENT_TAG;
    }

    @Override
    public boolean onBackPressed() {
        boolean preview = getActivity().getIntent().getBooleanExtra(DocumentActivity.EXTRA_DO_PREVIEW, false) || AppSettings.get().isPreviewFirst() || _document.getFile().getName().startsWith("index.");
        saveDocument();
        if (_isPreviewVisible && !preview) {
            setDocumentViewVisibility(false);
            return true;
        } else if (!_isPreviewVisible && preview) {
            setDocumentViewVisibility(true);
            return true;
        }
        return false;
    }

    // Save the file
    // Only supports java.io.File. TODO: Android Content
    public boolean saveDocument() {
        boolean ret = false;
        if (isAdded() && _hlEditor != null && _hlEditor.getText() != null) {
            ret = DocumentIO.saveDocument(_document, _hlEditor.getText().toString(), _shareUtil);
            updateLauncherWidgets();

            if (_document != null && _document.getFile() != null) {
                new AppSettings(getContext()).setLastEditPosition(_document.getFile(), _hlEditor.getSelectionStart(), _hlEditor.getTop());
            }
        }
        return ret;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        saveDocument();
        if ((_hlEditor.length() * _document.getHistory().size() * 1.05) < 9200 && false) {
            outState.putSerializable(SAVESTATE_DOCUMENT, _document);
        }
        if (getArguments() != null && _document.getFile() != null) {
            getArguments().putSerializable(DocumentIO.EXTRA_PATH, _document.getFile());
            getArguments().putSerializable(DocumentIO.EXTRA_PATH_IS_FOLDER, false);
        }
        if (_hlEditor != null) {
            outState.putSerializable(SAVESTATE_CURSOR_POS, _hlEditor.getSelectionStart());
        }
        outState.putBoolean(SAVESTATE_PREVIEW_ON, _isPreviewVisible);

        /*SharedPreferences sp = getContext().getSharedPreferences("unforedopref", 0);
        _editTextUndoRedoHelper.storePersistentState(sp.edit(), _editTextUndoRedoHelper.undoRedoPrefKeyForFile(_document.getFile()));
        outState.putString("undoredopref", "put");*/
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        super.onPause();
        saveDocument();
        if (_document != null && _document.getFile() != null) {
            AppSettings appSettings = new AppSettings(getContext());
            appSettings.addRecentDocument(_document.getFile());
        }
    }

    private void updateLauncherWidgets() {
        Context c = App.get().getApplicationContext();
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(c);
        int appWidgetIds[] = appWidgetManager.getAppWidgetIds(new ComponentName(c, WrMarkorWidgetProvider.class));
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_notes_list);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        Activity a = getActivity();
        if (isVisibleToUser && a != null && a instanceof MainActivity) {
            checkReloadDisk(false);
        } else if (!isVisibleToUser && _document != null) {
            saveDocument();
        }
    }

    private void checkReloadDisk(boolean forceReload) {
        Document cmp = DocumentIO.loadDocument(getActivity(), getArguments(), null);
        if (forceReload || (_document != null && cmp != null && cmp.getContent() != null && !cmp.getContent().equals(_document.getContent()))) {
            _editTextUndoRedoHelper.clearHistory();
            _document = cmp;
            loadDocument();
            loadDocumentIntoUi();
        }
    }

    @Override
    public void onFragmentFirstTimeVisible() {
        AppSettings as = new AppSettings(getContext());
        if (_savedInstanceState == null || !_savedInstanceState.containsKey(SAVESTATE_CURSOR_POS) && _hlEditor.length() > 0) {
            int lastPos;
            if (_document != null && _document.getFile() != null && (lastPos = as.getLastEditPositionChar(_document.getFile())) >= 0 && lastPos <= _hlEditor.length()) {
                _hlEditor.requestFocus();
                _hlEditor.setSelection(lastPos);
                _hlEditor.scrollTo(0, as.getLastEditPositionScroll(_document.getFile()));
            } else if (as.isEditorStartOnBotttom()) {
                _hlEditor.requestFocus();
                _hlEditor.setSelection(_hlEditor.length());
            }
        }
    }

    public void setDocumentViewVisibility(boolean show) {
        if (!show) {
            _webViewClient.setRestoreScrollY(_webView.getScrollY());
        }
        if (show) {
            _document.setContent(_hlEditor.getText().toString());
            _textFormat.getConverter().convertMarkupShowInWebView(_document, _webView, _nextConvertToPrintMode);
            new ActivityUtils(getActivity()).hideSoftKeyboard().freeContextRef();
            _hlEditor.clearFocus();
            _hlEditor.postDelayed(() -> new ActivityUtils(getActivity()).hideSoftKeyboard().freeContextRef(), 300);
        }
        _nextConvertToPrintMode = false;
        _webView.setAlpha(0);
        ((FrameLayout) _webView.getParent()).setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            _webView.animate().setDuration(150).alpha(1.0f).setListener(null);
        }
        _isPreviewVisible = show;
        ((AppCompatActivity) getActivity()).supportInvalidateOptionsMenu();
    }

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
}
