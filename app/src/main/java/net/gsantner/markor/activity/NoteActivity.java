package net.gsantner.markor.activity;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
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
    private String _initalContent = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContextUtils.get().setAppLanguage(AppSettings.get().getLanguage());
        if (AppSettings.get().isEditorStatusBarHidden()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            AndroidBug5497Workaround.assistActivity(this);
        }
        setContentView(R.layout.note__activity);
        ButterKnife.bind(this);
        _appSettings = AppSettings.get();

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
            openFromSendAction(receivingIntent);
        } else if (Intent.ACTION_EDIT.equals(intentAction) && type != null) {
            openFromEditAction(receivingIntent);
        } else if (Intent.ACTION_VIEW.equals(intentAction) && type != null) {
            openFromViewAction(receivingIntent);
        } else {
            _note = (File) getIntent().getSerializableExtra(Constants.NOTE_KEY);
        }

        if (_note != null) {
            _contentEditor.setText(readNote());
            _editNoteTitle.setText(_note.getName().replaceAll("((?i)\\.md$)", ""));
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
        _initalContent = _note.getName() + noteContent;
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

    private void readFileUriFromIntent(Uri fileUri) {
        if (fileUri != null) {
            _note = MarkorSingleton.getInstance().getFileFromUri(fileUri);
            _contentEditor.setText(MarkorSingleton.getInstance().readFileUri(fileUri, this));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.note_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                overridePendingTransition(R.anim.anim_slide_out_right, R.anim.anim_slide_in_right);
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
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.anim_slide_out_right, R.anim.anim_slide_in_right);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        // Set up the font and background activity_preferences
        setupKeyboardBar();
        setupAppearancePreferences();

        IntentFilter ifilter = new IntentFilter();
        ifilter.addAction(Constants.SHARE_BROADCAST_TAG);
        super.onResume();
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
            appendRegularShortcuts();
            if (_appSettings.isSmartShortcutsEnabled()) {
                appendSmartBracketShortcuts();
            } else {
                appendRegularBracketShortcuts();
            }
            for (String shortcut : Constants.KEYBOARD_SHORTCUTS_MORE) {
                appendButton(shortcut, new KeyboardBarListener());
            }
        } else if (!_appSettings.isShowMarkdownShortcuts()) {
            findViewById(R.id.note__activity__scroll_markdownchar_bar).setVisibility(View.GONE);
        }
    }

    private void appendRegularShortcuts() {
        for (String shortcut : Constants.KEYBOARD_SHORTCUTS) {
            appendButton(shortcut, new KeyboardBarListener());
        }
    }

    private void appendRegularBracketShortcuts() {
        for (String shortcut : Constants.KEYBOARD_SHORTCUTS_BRACKETS) {
            appendButton(shortcut, new KeyboardBarListener());
        }

    }

    private void appendSmartBracketShortcuts() {
        for (String shortcut : Constants.KEYBOARD_SMART_SHORTCUTS) {
            appendButton(shortcut, new KeyboardBarSmartShortCutListener());
        }
    }

    private void appendButton(String shortcut, View.OnClickListener l) {
        TextView shortcutButton = (TextView) getLayoutInflater().inflate(R.layout.ui__quick_keyboard_button, null);
        shortcutButton.setText(shortcut);
        shortcutButton.setOnClickListener(l);


        boolean isDarkTheme = _appSettings.isDarkThemeEnabled();
        shortcutButton.setTextColor(ContextCompat.getColor(this,
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

        // .replace is a workaround for Markdown lists requiring two \n characters
        if (_note != null) {
            Uri uriBase = MarkorSingleton.getInstance().getUriFromFile(_note.getParentFile());
            intent.putExtra(Constants.MD_PREVIEW_BASE, uriBase.toString());
        }

        intent.putExtra(Constants.NOTE_KEY, _note);
        intent.putExtra(Constants.MD_PREVIEW_KEY, _contentEditor.getText().toString().replace("\n-", "\n\n-"));

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
        if (_note != null && _initalContent.equals(_note.getName() + _contentEditor.getText().toString())) {
            return;
        }
        try {
            String content = _contentEditor.getText().toString();
            String filename = normalizeFilename(content, _editNoteTitle.getText().toString());
            if (filename == null) return;

            String parent = _targetDirectory != null ? _targetDirectory : _note.getParent();
            File newNote = new File(parent, filename + Constants.MD_EXT);
            FileOutputStream fos = new FileOutputStream(newNote);
            OutputStreamWriter writer = new OutputStreamWriter(fos);

            writer.write(content);
            writer.flush();

            writer.close();
            fos.close();
            // If we have created a new _note due to renaming, delete the old copy
            if (_note != null && !newNote.getName().equals(_note.getName()) && newNote.exists()) {
                _note.delete();
            }
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

    private class KeyboardBarListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            CharSequence shortcut = ((TextView) v).getText();
            _contentEditor.getText().insert(_contentEditor.getSelectionStart(), shortcut);
        }
    }

    private class KeyboardBarSmartShortCutListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            CharSequence shortcut = ((TextView) v).getText();
            if (_contentEditor.hasSelection()) {
                CharSequence selected = _contentEditor.getText().subSequence(_contentEditor.getSelectionStart(),
                        _contentEditor.getSelectionEnd());
                _contentEditor.getText().replace(_contentEditor.getSelectionStart(), _contentEditor.getSelectionEnd(),
                        Character.toString(shortcut.charAt(0)) + selected + shortcut.charAt(1));
            } else {
                _contentEditor.getText().insert(_contentEditor.getSelectionStart(), shortcut);
                _contentEditor.setSelection(_contentEditor.getSelectionStart() - 1);
            }
        }
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
}
