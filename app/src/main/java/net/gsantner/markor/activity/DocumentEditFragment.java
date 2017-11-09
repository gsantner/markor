/*
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
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
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import net.gsantner.markor.App;
import net.gsantner.markor.R;
import net.gsantner.markor.format.highlighter.HighlightingEditor;
import net.gsantner.markor.format.shortcut.EditorShortcuts;
import net.gsantner.markor.format.shortcut.EditorShortcutsMarkdown;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.util.DocumentIO;
import net.gsantner.markor.ui.BaseFragment;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ContextUtils;
import net.gsantner.markor.widget.MarkorWidgetProvider;
import net.gsantner.opoc.util.ActivityUtils;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnTextChanged;

@SuppressWarnings({"UnusedReturnValue", "RedundantCast"})
public class DocumentEditFragment extends BaseFragment {
    public static final int HISTORY_DELTA = 5000;
    public static final String FRAGMENT_TAG = "DocumentEditFragment";
    private static final String SAVESTATE_DOCUMENT = "DOCUMENT";
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


    @BindView(R.id.document__fragment__edit__content_editor)
    HighlightingEditor _contentEditor;

    @BindView(R.id.document__fragment__edit__shortcut_bar)
    ViewGroup _markdownShortcutBar;

    private View _view;
    private Context _context;
    private Document _document;

    public DocumentEditFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.document__fragment__edit, container, false);
        ButterKnife.bind(this, view);
        _view = view;
        _context = view.getContext();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        setupShortcutBar();
        setupAppearancePreferences();

        if (savedInstanceState != null && savedInstanceState.containsKey(SAVESTATE_DOCUMENT)) {
            _document = (Document) savedInstanceState.getSerializable(SAVESTATE_DOCUMENT);
        }
        _document = loadDocument();
        loadDocumentIntoUi();

        new ActivityUtils(getActivity()).hideSoftKeyboard();
        _contentEditor.clearFocus();
    }

    @Override
    public void onResume() {
        super.onResume();
        checkReloadDisk();
        _contentEditor.setText(_document.getContent());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.document__edit__menu, menu);
        ContextUtils cu = ContextUtils.get();
        cu.tintMenuItems(menu, true, Color.WHITE);
        cu.setSubMenuIconsVisiblity(menu, true);

        Drawable drawable;
        drawable = menu.findItem(R.id.action_undo).setEnabled(_document.canGoToEarlierVersion()).getIcon();
        drawable.mutate().setAlpha(_document.canGoToEarlierVersion() ? 255 : 40);
        drawable = menu.findItem(R.id.action_redo).setEnabled(_document.canGoToNewerVersion()).getIcon();
        drawable.mutate().setAlpha(_document.canGoToNewerVersion() ? 255 : 40);
    }

    public void loadDocumentIntoUi() {
        int editorpos = _contentEditor.getSelectionStart();
        _contentEditor.setText(_document.getContent());
        editorpos = editorpos > _contentEditor.length() ? _contentEditor.length() - 1 : editorpos;
        _contentEditor.setSelection(editorpos < 0 ? 0 : editorpos);
        Activity activity = getActivity();
        if (activity != null && activity instanceof DocumentActivity) {
            DocumentActivity da = ((DocumentActivity) activity);
            da.setDocumentTitle(_document.getTitle());
            da.setDocument(_document);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_preview: {
                // Handled by parent
                return false;
            }
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
        }
        return super.onOptionsItemSelected(item);
    }

    private long _lastChangedThreadStart = 0;

    @OnTextChanged(value = R.id.document__fragment__edit__content_editor, callback = OnTextChanged.Callback.TEXT_CHANGED)
    public void onContentEditValueChanged(CharSequence text) {
        if ((_lastChangedThreadStart + HISTORY_DELTA) < System.currentTimeMillis()) {
            _lastChangedThreadStart = System.currentTimeMillis();
            _contentEditor.postDelayed(new Runnable() {
                public void run() {
                    _document.setContent(text.toString());
                    Activity activity = getActivity();
                    if (activity != null && activity instanceof AppCompatActivity) {
                        ((AppCompatActivity) activity).supportInvalidateOptionsMenu();
                    }
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
        Document document = DocumentIO.loadDocument(getActivity(), getArguments(), _document);
        if (document.getHistory().isEmpty()) {
            document.forceAddNextChangeToHistory();
            document.addToHistory();
        }
        return document;
    }

    private void setupShortcutBar() {
        EditorShortcuts editorActions = new EditorShortcutsMarkdown(_contentEditor, _document, getActivity());
        editorActions.appendShortcutsToBar(_markdownShortcutBar);
    }

    private void setupAppearancePreferences() {
        AppSettings as = AppSettings.get();
        _contentEditor.setTextSize(TypedValue.COMPLEX_UNIT_SP, as.getFontSize());
        _contentEditor.setTypeface(Typeface.create(as.getFontFamily(), Typeface.NORMAL));

        if (as.isDarkThemeEnabled()) {
            _contentEditor.setBackgroundColor(getResources().getColor(R.color.dark_grey));
            _contentEditor.setTextColor(getResources().getColor(android.R.color.white));
            _view.findViewById(R.id.document__fragment__edit__shortcut_bar__scrolling_parent).setBackgroundColor(getResources().getColor(R.color.dark_grey));
        } else {
            _contentEditor.setBackgroundColor(getResources().getColor(android.R.color.white));
            _contentEditor.setTextColor(getResources().getColor(R.color.dark_grey));
            _view.findViewById(R.id.document__fragment__edit__shortcut_bar__scrolling_parent)
                    .setBackgroundColor(getResources().getColor(R.color.lighter_grey));
        }
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
        boolean argAllowRename = getArguments() == null || getArguments().getBoolean(DocumentIO.EXTRA_ALLOW_RENAME, true);
        boolean ret = DocumentIO.saveDocument(_document, argAllowRename, _contentEditor.getText().toString());
        updateLauncherWidgets();
        return ret;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        saveDocument();
        outState.putSerializable(SAVESTATE_DOCUMENT, _document);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        saveDocument();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        showPreviewOnBack = false;
    }

    private void updateLauncherWidgets() {
        Context c = App.get().getApplicationContext();
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(c);
        int appWidgetIds[] = appWidgetManager.getAppWidgetIds(new ComponentName(c, MarkorWidgetProvider.class));
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_notes_list);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        Activity a = getActivity();
        if (isVisibleToUser && a != null && a instanceof MainActivity) {
            checkReloadDisk();
        }
    }

    private void checkReloadDisk() {
        Document cmp = DocumentIO.loadDocument(getActivity(), getArguments(), null);
        if (!cmp.getContent().equals(_document.getContent())) {
            _document = cmp;
            loadDocument();
            loadDocumentIntoUi();
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
