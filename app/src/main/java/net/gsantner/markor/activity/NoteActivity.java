/*
 * Copyright (c) 2014 Jeff Martin
 * Copyright (c) 2015 Pedro Lafuente
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.activity;

import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import net.gsantner.markor.R;
import net.gsantner.markor.editor.HighlightingEditor;
import net.gsantner.markor.model.Constants;
import net.gsantner.markor.model.MarkorSingleton;
import net.gsantner.markor.util.AndroidBug5497Workaround;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ContextUtils;
import net.gsantner.markor.widget.MarkorWidgetProvider;
import net.gsantner.opoc.util.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Locale;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;

public class NoteActivity extends AppCompatActivity {

    @BindView(R.id.note__activity__edit_note_title)
    EditText _editNoteTitle;

    @BindView(R.id.note__activity__note_content_editor)
    HighlightingEditor _contentEditor;

    @BindView(R.id.note__activity__header_view_switcher)
    ViewSwitcher _viewSwitcher;

    @BindView(R.id.note__activity__markdownchar_bar)
    ViewGroup _markdownCharBar;

    @BindView(R.id.note__activity__text_note_title)
    TextView _headerNoteTitle;

    @BindView(R.id.toolbar)
    Toolbar _toolbar;

    private File _note;
    private String _targetDirectory;
    private boolean _isPreviewIncoming = false;
    private AppSettings _appSettings;
    private String _initialContent = "";
    private String _initialFileName = "";
    private String _fileExtension = Constants.MD_EXT1_MD;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContextUtils.get().setAppLanguage(AppSettings.get().getLanguage());
        _appSettings = AppSettings.get();
        if (_appSettings.isEditorStatusBarHidden()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        setContentView(R.layout.note__activity);
        ButterKnife.bind(this);
        if (_appSettings.isEditorStatusBarHidden()) {
            AndroidBug5497Workaround.assistActivity(this);
        }
        setSupportActionBar(_toolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setHomeAsUpIndicator(ContextCompat.getDrawable(this, R.drawable.ic_arrow_back_white_24dp));
            ab.setDisplayHomeAsUpEnabled(true);
        }

        Intent receivingIntent = getIntent();
        _targetDirectory = receivingIntent.getStringExtra(Constants.TARGET_DIR);

        String intentAction = receivingIntent.getAction();
        String type = receivingIntent.getType();

        if (Intent.ACTION_SEND.equals(intentAction) && type != null) {
            if (type.equals("text/plain")) {
                openTextShareText(receivingIntent);
            } else {
                openFromSendAction(receivingIntent);
            }
        } else if (Intent.ACTION_EDIT.equals(intentAction) && type != null) {
            openFromEditAction(receivingIntent);
        } else if (Intent.ACTION_VIEW.equals(intentAction) && type != null) {
            openFromViewAction(receivingIntent);
        } else {
            _note = (File) getIntent().getSerializableExtra(Constants.NOTE_KEY);
        }

        if (_note != null) {
            _contentEditor.setText(readNote());
            _editNoteTitle.setText(Constants.MD_EXTENSION.matcher(_note.getName()).replaceAll(""));

            // Extract existing extension
            for (String ext : Constants.EXTENSIONS) {
                if (_note.getName().toLowerCase(Locale.getDefault()).endsWith(ext)) {
                    _fileExtension = ext;
                    break;
                }
            }

        }

        _editNoteTitle.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    //TODO Make no change
                } else {
                    switchHeaderView(hasFocus);
                }
            }
        });

    }

    private String readNote() {
        java.net.URI oldUri = _note.toURI();
        String noteContent = MarkorSingleton.getInstance().readFileUri(Uri.parse(oldUri.toString()), this);
        _initialFileName = _note.getName();
        _initialContent = noteContent;
        return noteContent;
    }

    private void openFromSendAction(Intent receivingIntent) {
        Uri fileUri = receivingIntent.getParcelableExtra(Intent.EXTRA_STREAM);
        readFileUriFromIntent(fileUri);
    }

    private void openFromEditAction(Intent receivingIntent) {
        Uri fileUri = receivingIntent.getData();
        readFileUriFromIntent(fileUri);
    }

    private void openFromViewAction(Intent receivingIntent) {
        Uri fileUri = receivingIntent.getData();
        _note = new File(fileUri.getPath());
        _contentEditor.setText(MarkorSingleton.getInstance().readFileUri(fileUri, this));
    }

    private void openTextShareText(Intent receivingIntent) {
        String stringExtra = receivingIntent.getStringExtra(Intent.EXTRA_TEXT);
        _contentEditor.setText(stringExtra);
    }

    private void readFileUriFromIntent(Uri fileUri) {
        if (fileUri != null) {
            _note = MarkorSingleton.getInstance().getFileFromUri(fileUri);
            _contentEditor.setText(MarkorSingleton.getInstance().readFileUri(fileUri, this));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.note__menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
            case R.id.action_share:
                shareNote();
                return true;
            case R.id.action_preview:
                previewNote();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Set up the font and background activity_preferences
        setupKeyboardBar();
        setupAppearancePreferences();

        IntentFilter ifilter = new IntentFilter();
        ifilter.addAction(Constants.SHARE_BROADCAST_TAG);
    }

    @Override
    protected void onPause() {
        saveNote();

        if (_isPreviewIncoming) {
            finish();
        }

        super.onPause();
    }

    private void setupKeyboardBar() {
        if (_appSettings.isShowMarkdownShortcuts() && _markdownCharBar.getChildCount() == 0) {
            appendSmartActions();
            appendRegularActions();
            appendExtraActions();
        } else if (!_appSettings.isShowMarkdownShortcuts()) {
            findViewById(R.id.note__activity__scroll_markdownchar_bar).setVisibility(View.GONE);
        }
    }

    private void appendRegularActions() {
        for (int[] actions : Constants.KEYBOARD_REGULAR_ACTIONS_ICONS) {
            appendButton(actions[0], new KeyboardRegularActionListener(Constants.KEYBOARD_REGULAR_ACTIONS[actions[1]]));
        }
    }

    private void appendSmartActions() {
        for (int[] actions : Constants.KEYBOARD_SMART_ACTIONS_ICON) {
            appendButton(actions[0], new KeyboardSmartActionsListener(Constants.KEYBOARD_SMART_ACTIONS[actions[1]]));
        }
    }

    private void appendExtraActions() {
        for (int[] actions : Constants.KEYBOARD_EXTRA_ACTIONS_ICONS) {
            appendButton(actions[0], new KeyboardExtraActionsListener(actions[1]));
        }
    }

    private void appendButton(int shortcut, View.OnClickListener l) {
        ImageView shortcutButton = (ImageView) getLayoutInflater().inflate(R.layout.ui__quick_keyboard_button, (ViewGroup)null);
        shortcutButton.setImageResource(shortcut);
        shortcutButton.setOnClickListener(l);

        boolean isDarkTheme = _appSettings.isDarkThemeEnabled();
        shortcutButton.setColorFilter(ContextCompat.getColor(this,
                isDarkTheme ? android.R.color.white : R.color.grey));
        _markdownCharBar.addView(shortcutButton);
    }

    private void setupAppearancePreferences() {
        _contentEditor.setTextSize(TypedValue.COMPLEX_UNIT_SP, _appSettings.getFontSize());
        _contentEditor.setTypeface(Typeface.create(_appSettings.getFontFamily(), Typeface.NORMAL));

        if (_appSettings.isDarkThemeEnabled()) {
            _contentEditor.setBackgroundColor(getResources().getColor(R.color.dark_grey));
            _contentEditor.setTextColor(getResources().getColor(android.R.color.white));
            findViewById(R.id.note__activity__scroll_markdownchar_bar).setBackgroundColor(getResources().getColor(R.color.dark_grey));
        } else {
            _contentEditor.setBackgroundColor(getResources().getColor(android.R.color.white));
            _contentEditor.setTextColor(getResources().getColor(R.color.dark_grey));
            findViewById(R.id.note__activity__scroll_markdownchar_bar)
                    .setBackgroundColor(getResources().getColor(R.color.lighter_grey));
        }
    }

    private void previewNote() {
        saveNote();
        Intent intent = new Intent(this, PreviewActivity.class);

        if (_note != null) {
            Uri uriBase = MarkorSingleton.getInstance().getUriFromFile(_note.getParentFile());
            intent.putExtra(Constants.MD_PREVIEW_BASE, uriBase.toString());
        }

        intent.putExtra(Constants.NOTE_KEY, _note);
        intent.putExtra(Constants.MD_PREVIEW_KEY, _contentEditor.getText().toString());

        _isPreviewIncoming = true;
        startActivity(intent);
    }

    private void shareNote() {
        saveNote();

        String shareContent = _contentEditor.getText().toString();

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareContent);
        shareIntent.setType("text/plain");
        startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.share_string)));
    }

    /**
     * Save the file to its directory
     */
    private void saveNote() {
        String content = _contentEditor.getText().toString();
        String filename = normalizeFilename(content, _editNoteTitle.getText().toString());

        if (filename == null) return;

        // Append extension
        filename += _fileExtension;

        try {

            String parent = _targetDirectory != null ? _targetDirectory : _note.getParent();

            if (_note == null || !_note.exists()) {
                _note = new File(parent, filename);
            } else if (!filename.equals(_initialFileName)) {
                FileUtils.renameFileInSameFolder(_note, filename);
                _note = new File(parent, filename);
            }

            if (!content.equals(_initialContent)) {
                FileOutputStream fos = new FileOutputStream(_note);
                OutputStreamWriter writer = new OutputStreamWriter(fos);

                writer.write(content);
                writer.flush();
                writer.close();
                fos.close();
            }

            _initialContent = content;
            _initialFileName = filename;

            updateWidgets();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateWidgets() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
        int appWidgetIds[] = appWidgetManager.getAppWidgetIds(
                new ComponentName(getApplicationContext(), MarkorWidgetProvider.class));
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_notes_list);
    }

    private String normalizeFilename(String content, String title) {
        String filename = title;
        if (filename.length() == 0) {
            if (content.length() == 0) {
                return null;
            } else {
                if (content.length() < Constants.MAX_TITLE_LENGTH) {
                    filename = content.substring(0, content.length());
                } else {
                    filename = content.substring(0, Constants.MAX_TITLE_LENGTH);
                }
            }
        }
        filename = filename.replaceAll("[\\\\/:\"*?<>|]+", "").trim();

        if (filename.isEmpty()) {
            filename = "Markor - " + String.valueOf(UUID.randomUUID().getMostSignificantBits()).substring(0, 6);
        }
        return filename;
    }

    public void switchHeaderView(Boolean hasFocus) {
        if (!hasFocus) {
            _headerNoteTitle.setText(_editNoteTitle.getText().toString());
            _viewSwitcher.showNext();
        }
    }

    public void titleClicked(View view) {
        _viewSwitcher.showPrevious();
        _editNoteTitle.requestFocus();
    }

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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final View view = getLayoutInflater().inflate(R.layout.format_dialog, (ViewGroup)null);

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
}
