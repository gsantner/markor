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
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.gsantner.markor.App;
import net.gsantner.markor.R;
import net.gsantner.markor.format.TextFormat;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.ui.hleditor.HighlightingEditor;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ContextUtils;
import net.gsantner.markor.util.DocumentIO;
import net.gsantner.opoc.activity.GsFragmentBase;
import net.gsantner.opoc.util.ActivityUtils;

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
    public static boolean showPreviewOnBack = false;

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
        args.putBoolean(DocumentIO.EXTRA_ALLOW_RENAME, allowRename);
        f.setArguments(args);
        return f;
    }


    @BindView(R.id.document__fragment__edit__highlighting_editor)
    HighlightingEditor _hlEditor;

    @BindView(R.id.document__fragment__edit__textmodule_actions_bar)
    ViewGroup _textModuleActionsBar;

    @BindView(R.id.document__fragment__edit__content_editor__permission_warning)
    TextView _textSdWarning;

    private Document _document;
    private TextFormat _textFormat;

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

        new ActivityUtils(getActivity()).hideSoftKeyboard();
        AppSettings appSettings = new AppSettings(view.getContext());
        _hlEditor.clearFocus();
        _hlEditor.setLineSpacing(0, appSettings.getEditorLineSpacing());
    }

    @Override
    public void onResume() {
        super.onResume();
        checkReloadDisk();
        int cursor = _hlEditor.getSelectionStart();
        _hlEditor.setText(_document.getContent());
        cursor = Math.max(0, cursor);
        cursor = Math.min(_hlEditor.length(), cursor);
        _hlEditor.setSelection(cursor);

        AppSettings appSettings = new AppSettings(getContext());
        _hlEditor.setGravity(appSettings.isEditorStartEditingInCenter() ? Gravity.CENTER : Gravity.NO_GRAVITY);
        if (_document != null && _document.getFile() != null) {
            boolean permok = _document.getFile().canWrite();
            if (!permok && !_document.getFile().isDirectory() && _document.getFile().getParentFile().canWrite()) {
                permok = true;
            }
            _textSdWarning.setVisibility(permok ? View.GONE : View.VISIBLE);
        }
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

        boolean enable;
        Drawable drawable;
        drawable = menu.findItem(R.id.action_undo).setEnabled(_document.canGoToEarlierVersion()).getIcon();
        drawable.mutate().setAlpha(_document.canGoToEarlierVersion() ? 255 : 40);
        drawable = menu.findItem(R.id.action_redo).setEnabled(_document.canGoToNewerVersion()).getIcon();
        drawable.mutate().setAlpha(_document.canGoToNewerVersion() ? 255 : 40);
        enable = !(_document.getContent().isEmpty() || _document.getTitle().isEmpty());
        drawable = menu.findItem(R.id.action_save).setEnabled(enable).getIcon();
        drawable.mutate().setAlpha(enable ? 255 : 40);
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
        _textFormat.getTextModuleActions().setDocument(_document);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_undo: {
                if (_document.canGoToEarlierVersion()) {
                    _document.goToEarlierVersion();
                    loadDocumentIntoUi();
                }
                return true;
            }
            case R.id.action_redo: {
                if (_document.canGoToNewerVersion()) {
                    _document.goToNewerVersion();
                    loadDocumentIntoUi();
                }
                return true;
            }
            case R.id.action_save: {
                saveDocument();
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
        _textModuleActionsBar.removeAllViews();
        _textFormat = TextFormat.getFormat(textFormatId, getActivity(), _document);
        _hlEditor.setHighlighter(_textFormat.getHighlighter());
        _textFormat.getTextModuleActions()
                .setHighlightingEditor(_hlEditor)
                .appendTextModuleActionsToBar(_textModuleActionsBar);
        if (_textModuleActionsBar.getChildCount() == 0) {
            _textModuleActionsBar.setVisibility(View.GONE);
        } else {
            _textModuleActionsBar.setVisibility(View.VISIBLE);
        }
    }

    private void setupAppearancePreferences(View fragmentView) {
        AppSettings as = AppSettings.get();
        _hlEditor.setTextSize(TypedValue.COMPLEX_UNIT_SP, as.getFontSize());
        _hlEditor.setTypeface(Typeface.create(as.getFontFamily(), Typeface.NORMAL));

        _hlEditor.setBackgroundColor(as.getEditorBackgroundColor());
        _hlEditor.setTextColor(as.getEditorForegroundColor());
        fragmentView.findViewById(R.id.document__fragment__edit__textmodule_actions_bar__scrolling_parent).setBackgroundColor(as.getEditorTextactionBarColor());
    }

    @Override
    public String getFragmentTag() {
        return FRAGMENT_TAG;
    }

    @Override
    public boolean onBackPressed() {
        saveDocument();
        if (showPreviewOnBack) {
            showPreviewOnBack = false;
            Activity activity = getActivity();
            if (activity != null && activity instanceof DocumentActivity) {
                DocumentActivity da = ((DocumentActivity) activity);
                da.showPreview(_document, null);
            }
            return true;
        }
        return false;
    }

    // Save the file
    // Only supports java.io.File. TODO: Android Content
    public boolean saveDocument() {
        boolean ret = false;
        if (isAdded() && _hlEditor != null) {
            boolean argAllowRename = getArguments() == null || getArguments().getBoolean(DocumentIO.EXTRA_ALLOW_RENAME, true);
            ret = DocumentIO.saveDocument(_document, argAllowRename, _hlEditor.getText().toString());
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
        if ((_hlEditor.length() * _document.getHistory().size() * 1.05) < 9200) {
            outState.putSerializable(SAVESTATE_DOCUMENT, _document);
        }
        if (getArguments() != null && _document.getFile() != null) {
            getArguments().putSerializable(DocumentIO.EXTRA_PATH, _document.getFile());
            getArguments().putSerializable(DocumentIO.EXTRA_PATH_IS_FOLDER, false);
        }
        if (_hlEditor != null) {
            outState.putSerializable(SAVESTATE_CURSOR_POS, _hlEditor.getSelectionStart());
        }
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        showPreviewOnBack = false;
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
            checkReloadDisk();
        } else if (!isVisibleToUser && _document != null) {
            saveDocument();
        }
    }

    private void checkReloadDisk() {
        Document cmp = DocumentIO.loadDocument(getActivity(), getArguments(), null);
        if (_document != null && cmp != null && cmp.getContent() != null && !cmp.getContent().equals(_document.getContent())) {
            _document = cmp;
            loadDocument();
            loadDocumentIntoUi();
        }
    }

    @Override
    public void onFragmentFirstTimeVisible() {
        AppSettings as = new AppSettings(getContext());
        if (_savedInstanceState == null || !_savedInstanceState.containsKey(SAVESTATE_CURSOR_POS)) {
            //  TODO
            if (_hlEditor.length() > 0) {
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
    }

    //
    //
    //
    //

    public Document getDocument() {
        return _document;
    }
}
