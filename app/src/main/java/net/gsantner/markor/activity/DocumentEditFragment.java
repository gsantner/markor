/*
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import net.gsantner.markor.App;
import net.gsantner.markor.R;
import net.gsantner.markor.editor.HighlightingEditor;
import net.gsantner.markor.model.Constants;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.model.DocumentLoader;
import net.gsantner.markor.ui.BaseFragment;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ContextUtils;
import net.gsantner.markor.widget.MarkorWidgetProvider;
import net.gsantner.opoc.util.ActivityUtils;
import net.gsantner.opoc.util.FileUtils;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnTextChanged;

@SuppressWarnings("UnusedReturnValue")
public class DocumentEditFragment extends BaseFragment {
    public static final int HISTORY_DELTA = 5000;
    public static final String FRAGMENT_TAG = "DocumentEditFragment";
    private static final String SAVESTATE_DOCUMENT = "DOCUMENT";
    public static boolean showPreviewOnBack = false;

    public static DocumentEditFragment newInstance(Document document) {
        DocumentEditFragment f = new DocumentEditFragment();
        Bundle args = new Bundle();
        args.putSerializable(DocumentLoader.EXTRA_DOCUMENT, document);
        f.setArguments(args);
        return f;
    }

    public static DocumentEditFragment newInstance(File path, boolean pathIsFolder, boolean allowRename) {
        DocumentEditFragment f = new DocumentEditFragment();
        Bundle args = new Bundle();
        args.putSerializable(DocumentLoader.EXTRA_PATH, path);
        args.putBoolean(DocumentLoader.EXTRA_PATH_IS_FOLDER, pathIsFolder);
        args.putBoolean(DocumentLoader.EXTRA_ALLOW_RENAME, allowRename);
        f.setArguments(args);
        return f;
    }


    @BindView(R.id.note__activity__note_content_editor)
    HighlightingEditor _contentEditor;

    @BindView(R.id.note__activity__markdownchar_bar)
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
        Context c = getContext();
        setupMarkdownShortcutBar();
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

    @OnTextChanged(value = R.id.note__activity__note_content_editor, callback = OnTextChanged.Callback.TEXT_CHANGED)
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
        Document document = DocumentLoader.loadDocument(getActivity(), getArguments(), _document);
        if (document.getHistory().isEmpty()) {
            document.forceAddNextChangeToHistory();
            document.addToHistory();
        }
        return document;
    }

    private void setupMarkdownShortcutBar() {
        if (AppSettings.get().isShowMarkdownShortcuts() && _markdownShortcutBar.getChildCount() == 0) {
            // Smart Actions
            for (int[] actions : Constants.KEYBOARD_SMART_ACTIONS_ICON) {
                appendMarkdownShortcutToBar(actions[0], new KeyboardSmartActionsListener(Constants.KEYBOARD_SMART_ACTIONS[actions[1]]));
            }

            // Regular actions
            for (int[] actions : Constants.KEYBOARD_REGULAR_ACTIONS_ICONS) {
                appendMarkdownShortcutToBar(actions[0], new KeyboardRegularActionListener(Constants.KEYBOARD_REGULAR_ACTIONS[actions[1]]));
            }

            // Extra actions
            for (int[] actions : Constants.KEYBOARD_EXTRA_ACTIONS_ICONS) {
                appendMarkdownShortcutToBar(actions[0], new KeyboardExtraActionsListener(actions[1]));
            }
        } else if (!AppSettings.get().isShowMarkdownShortcuts()) {
            _view.findViewById(R.id.note__activity__scroll_markdownchar_bar).setVisibility(View.GONE);
        }
    }

    private void setupAppearancePreferences() {
        AppSettings as = AppSettings.get();
        _contentEditor.setTextSize(TypedValue.COMPLEX_UNIT_SP, as.getFontSize());
        _contentEditor.setTypeface(Typeface.create(as.getFontFamily(), Typeface.NORMAL));

        if (as.isDarkThemeEnabled()) {
            _contentEditor.setBackgroundColor(getResources().getColor(R.color.dark_grey));
            _contentEditor.setTextColor(getResources().getColor(android.R.color.white));
            _view.findViewById(R.id.note__activity__scroll_markdownchar_bar).setBackgroundColor(getResources().getColor(R.color.dark_grey));
        } else {
            _contentEditor.setBackgroundColor(getResources().getColor(android.R.color.white));
            _contentEditor.setTextColor(getResources().getColor(R.color.dark_grey));
            _view.findViewById(R.id.note__activity__scroll_markdownchar_bar)
                    .setBackgroundColor(getResources().getColor(R.color.lighter_grey));
        }
    }

    private void appendMarkdownShortcutToBar(int shortcut, View.OnClickListener l) {
        ImageView btn = (ImageView) getLayoutInflater().inflate(R.layout.ui__quick_keyboard_button, (ViewGroup) null);
        btn.setImageResource(shortcut);
        btn.setOnClickListener(l);

        boolean isDarkTheme = AppSettings.get().isDarkThemeEnabled();
        btn.setColorFilter(ContextCompat.getColor(_context,
                isDarkTheme ? android.R.color.white : R.color.grey));
        _markdownShortcutBar.addView(btn);
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
        Object a = getArguments();
        boolean argAllowRename = getArguments() == null || getArguments().getBoolean(DocumentLoader.EXTRA_ALLOW_RENAME, true);
        String filename = DocumentLoader.normalizeTitleForFilename(_document) + _document.getFileExtension();
        _document.setDoHistory(true);
        _document.setFile(new File(_document.getFile().getParentFile(), filename));

        Document documentInitial = _document.getInitialVersion();
        if (argAllowRename) {
            if (!_document.getFile().equals(documentInitial.getFile())) {
                if (documentInitial.getFile().exists()) {
                    FileUtils.renameFile(documentInitial.getFile(), _document.getFile());
                }
            }
        } else {
            _document.setFile(documentInitial.getFile());
        }

        if (!_contentEditor.getText().toString().equals(documentInitial.getContent())) {
            _document.forceAddNextChangeToHistory();
            _document.setContent(_contentEditor.getText().toString());
            ret = FileUtils.writeFile(_document.getFile(), _document.getContent());
        } else {
            ret = true;
        }
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
        Document cmp = DocumentLoader.loadDocument(getActivity(), getArguments(), null);
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

    private class KeyboardRegularActionListener implements View.OnClickListener {
        String _action;

        public KeyboardRegularActionListener(String action) {
            _action = action;
        }

        @Override
        public void onClick(View v) {

            if (_contentEditor.hasSelection()) {
                String text = _contentEditor.getText().toString();
                int selectionStart = _contentEditor.getSelectionStart();
                int selectionEnd = _contentEditor.getSelectionEnd();

                //Check if Selection includes the shortcut characters
                if (text.substring(selectionStart, selectionEnd)
                        .matches("(>|#{1,3}|-|[1-9]\\.)(\\s)?[a-zA-Z0-9\\s]*")) {

                    text = text.substring(selectionStart + _action.length(), selectionEnd);
                    _contentEditor.getText()
                            .replace(selectionStart, selectionEnd, text);

                }
                //Check if Selection is Preceded by shortcut characters
                else if ((selectionStart >= _action.length()) && (text.substring(selectionStart - _action.length(), selectionEnd)
                        .matches("(>|#{1,3}|-|[1-9]\\.)(\\s)?[a-zA-Z0-9\\s]*"))) {

                    text = text.substring(selectionStart, selectionEnd);
                    _contentEditor.getText()
                            .replace(selectionStart - _action.length(), selectionEnd, text);

                }
                //Condition to insert shortcut preceding the selection
                else {
                    _contentEditor.getText().insert(selectionStart, _action);
                }
            } else {
                //Condition for Empty Selection
                _contentEditor.getText().insert(_contentEditor.getSelectionStart(), _action);
            }
        }
    }

    private class KeyboardSmartActionsListener implements View.OnClickListener {
        String _action;

        public KeyboardSmartActionsListener(String action) {
            _action = action;
        }

        @Override
        public void onClick(View v) {
            if (_contentEditor.hasSelection()) {
                String text = _contentEditor.getText().toString();
                int selectionStart = _contentEditor.getSelectionStart();
                int selectionEnd = _contentEditor.getSelectionEnd();

                //Check if Selection includes the shortcut characters
                if ((text.substring(selectionStart, selectionEnd)
                        .matches("(\\*\\*|~~|_|`)[a-zA-Z0-9\\s]*(\\*\\*|~~|_|`)"))) {

                    text = text.substring(selectionStart + _action.length(),
                            selectionEnd - _action.length());
                    _contentEditor.getText()
                            .replace(selectionStart, selectionEnd, text);

                }
                //Check if Selection is Preceded and succeeded by shortcut characters
                else if (((selectionEnd <= (_contentEditor.length() - _action.length())) &&
                        (selectionStart >= _action.length())) &&
                        (text.substring(selectionStart - _action.length(),
                                selectionEnd + _action.length())
                                .matches("(\\*\\*|~~|_|`)[a-zA-Z0-9\\s]*(\\*\\*|~~|_|`)"))) {

                    text = text.substring(selectionStart, selectionEnd);
                    _contentEditor.getText()
                            .replace(selectionStart - _action.length(),
                                    selectionEnd + _action.length(), text);

                }
                //Condition to insert shortcut preceding and succeeding the selection
                else {
                    _contentEditor.getText().insert(selectionStart, _action);
                    _contentEditor.getText().insert(_contentEditor.getSelectionEnd(), _action);
                }
            } else {
                //Condition for Empty Selection
                _contentEditor.getText().insert(_contentEditor.getSelectionStart(), _action)
                        .insert(_contentEditor.getSelectionEnd(), _action);
                _contentEditor.setSelection(_contentEditor.getSelectionStart() - _action.length());
            }
        }

    }

    private class KeyboardExtraActionsListener implements View.OnClickListener {
        int _action;

        public KeyboardExtraActionsListener(int action) {
            _action = action;
        }

        @Override
        public void onClick(View view) {
            getAlertDialog(_action);
        }
    }

    private void getAlertDialog(int action) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final View view = getLayoutInflater().inflate(R.layout.format_dialog, (ViewGroup) null);

        final EditText link_name = view.findViewById(R.id.format_dialog_name);
        final EditText link_url = view.findViewById(R.id.format_dialog_url);
        link_name.setHint(getString(R.string.format_dialog_name_hint));
        link_url.setHint(getString(R.string.format_dialog_url_or_path_hint));

        //Insert Link Action
        if (action == 1) {
            builder.setView(view)
                    .setTitle(getString(R.string.format_link_dialog_title))
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            _contentEditor.getText().insert(_contentEditor.getSelectionStart(),
                                    String.format("[%s](%s)", link_name.getText().toString(),
                                            link_url.getText().toString()));
                        }
                    });
        }
        //Insert Image Action
        else if (action == 2) {
            builder.setView(view)
                    .setTitle(getString(R.string.format_image_dialog_title))
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            _contentEditor.getText().insert(_contentEditor.getSelectionStart(),
                                    String.format("![%s](%s)", link_name.getText().toString(),
                                            link_url.getText().toString()));
                        }
                    });
        }

        builder.show();
    }

    public Document getDocument() {
        return _document;
    }
}
