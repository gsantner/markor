package io.github.gsantner.marowni.activity;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.UUID;

import io.github.gsantner.marowni.R;
import io.github.gsantner.marowni.editor.HighlightingEditor;
import io.github.gsantner.marowni.model.Constants;
import io.github.gsantner.marowni.model.MarowniSingleton;
import io.github.gsantner.marowni.util.AppSettings;
import io.github.gsantner.marowni.widget.MarowniWidgetProvider;

public class NoteActivity extends AppCompatActivity {

    public static final String EMPTY_STRING = "";
    private File note;
    private Context context;

    private EditText noteTitle;
    private HighlightingEditor content;
    private ScrollView scrollView;
    private ViewSwitcher viewSwitcher;

    private ViewGroup keyboardBarView;
    private String targetDirectory;
    private boolean isPreviewIncoming = false;

    private AppSettings _appSettings;

    public NoteActivity() {
    }

    public NoteActivity(Context context) {
        this.context = context;
        this.note = null;
    }

    public NoteActivity(Context context, File note) {
        this.context = context;
        this.note = note;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);
        _appSettings = AppSettings.get();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            final Drawable upArrow = getResources().getDrawable(R.drawable.abc_ic_ab_back_material);
            upArrow.setColorFilter(getResources().getColor(android.R.color.white), PorterDuff.Mode.SRC_ATOP);
            getSupportActionBar().setHomeAsUpIndicator(upArrow);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        context = getApplicationContext();
        content = (HighlightingEditor) findViewById(R.id.note_content);
        noteTitle = (EditText) findViewById(R.id.edit_note_title);
        scrollView = (ScrollView) findViewById(R.id.note_scrollview);
        keyboardBarView = (ViewGroup) findViewById(R.id.keyboard_bar);

        noteTitle.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    //TODO Make no change
                } else {
                    HeaderViewSwitcher(hasFocus);
                }
            }
        });

        Intent receivingIntent = getIntent();
        targetDirectory = receivingIntent.getStringExtra(Constants.TARGET_DIR);

        String intentAction = receivingIntent.getAction();
        String type = receivingIntent.getType();

        if (Intent.ACTION_SEND.equals(intentAction) && type != null) {
            openFromSendAction(receivingIntent);
        } else if (Intent.ACTION_EDIT.equals(intentAction) && type != null) {
            openFromEditAction(receivingIntent);
        } else if (Intent.ACTION_VIEW.equals(intentAction) && type != null) {
            openFromViewAction(receivingIntent);
        } else {
            note = (File) getIntent().getSerializableExtra(Constants.NOTE_KEY);
        }

        if (note != null) {
            content.setText(readNote());
            noteTitle.setText(note.getName().replaceAll("((?i)\\.md$)", ""));
        }
    }

    private String readNote() {
        java.net.URI oldUri = note.toURI();
        return MarowniSingleton.getInstance().readFileUri(Uri.parse(oldUri.toString()), this);
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
        note = new File(fileUri.getPath());
        content.setText(MarowniSingleton.getInstance().readFileUri(fileUri, this));
    }

    private void readFileUriFromIntent(Uri fileUri) {
        if (fileUri != null) {
            note = MarowniSingleton.getInstance().getFileFromUri(fileUri);
            content.setText(MarowniSingleton.getInstance().readFileUri(fileUri, this));
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

        if (isPreviewIncoming) {
            finish();
        }

        super.onPause();
    }

    private void setupKeyboardBar() {
        if (_appSettings.isShowMarkdownShortcuts() && keyboardBarView.getChildCount() == 0) {
            appendRegularShortcuts();
            if (_appSettings.isSmartShortcutsEnabled()) {
                appendSmartBracketShortcuts();
            } else {
                appendRegularBracketShortcuts();
            }
        } else if (!_appSettings.isShowMarkdownShortcuts()) {
            findViewById(R.id.keyboard_bar_scroll).setVisibility(View.GONE);
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
        TextView shortcutButton = (TextView) getLayoutInflater().inflate(R.layout.keyboard_shortcut, null);
        shortcutButton.setText(shortcut);
        shortcutButton.setOnClickListener(l);

        if (_appSettings.isDarkThemeEnabled()) {
            shortcutButton.setTextColor(getResources().getColor(android.R.color.white));
        } else {
            shortcutButton.setTextColor(getResources().getColor(R.color.grey));
        }

        keyboardBarView.addView(shortcutButton);
    }

    private void setupAppearancePreferences() {
        content.setTextSize(TypedValue.COMPLEX_UNIT_SP, _appSettings.getFontSize());
        content.setTypeface(Typeface.create(_appSettings.getFontFamily(), Typeface.NORMAL));

        if (_appSettings.isDarkThemeEnabled()) {
            content.setBackgroundColor(getResources().getColor(R.color.dark_grey));
            content.setTextColor(getResources().getColor(android.R.color.white));
            keyboardBarView.setBackgroundColor(getResources().getColor(R.color.grey));
        } else {
            content.setBackgroundColor(getResources().getColor(android.R.color.white));
            content.setTextColor(getResources().getColor(R.color.dark_grey));
            keyboardBarView.setBackgroundColor(getResources().getColor(R.color.lighter_grey));
        }
    }

    private void previewNote() {
        saveNote();
        Intent intent = new Intent(this, PreviewActivity.class);

        // .replace is a workaround for Markdown lists requiring two \n characters
        if (note != null) {
            Uri uriBase = MarowniSingleton.getInstance().getUriFromFile(note.getParentFile());
            intent.putExtra(Constants.MD_PREVIEW_BASE, uriBase.toString());
        }

        intent.putExtra(Constants.NOTE_KEY, note);
        intent.putExtra(Constants.MD_PREVIEW_KEY, content.getText().toString().replace("\n-", "\n\n-"));

        isPreviewIncoming = true;
        startActivity(intent);
    }

    private void shareNote() {
        saveNote();

        String shareContent = content.getText().toString();

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
        try {
            String content = this.content.getText().toString();
            String filename = normalizeFilename(content, noteTitle.getText().toString());
            if (filename == null) return;

            String parent = targetDirectory != null ? targetDirectory : note.getParent();
            File newNote = new File(parent, filename + Constants.MD_EXT);
            FileOutputStream fos = new FileOutputStream(newNote);
            OutputStreamWriter writer = new OutputStreamWriter(fos);

            writer.write(content);
            writer.flush();

            writer.close();
            fos.close();
            // If we have created a new note due to renaming, delete the old copy
            if (note != null && !newNote.getName().equals(note.getName()) && newNote.exists()) {
                note.delete();
            }
            updateWidgets();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateWidgets() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int appWidgetIds[] = appWidgetManager.getAppWidgetIds(
                new ComponentName(context, MarowniWidgetProvider.class));
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
            filename = "Marowni - " + String.valueOf(UUID.randomUUID().getMostSignificantBits()).substring(0, 6);
        }
        return filename;
    }

    private class KeyboardBarListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            CharSequence shortcut = ((TextView) v).getText();
            content.getText().insert(content.getSelectionStart(), shortcut);
        }
    }

    private class KeyboardBarSmartShortCutListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            CharSequence shortcut = ((TextView) v).getText();
            if (content.hasSelection()) {
                CharSequence selected = content.getText().subSequence(content.getSelectionStart(),
                        content.getSelectionEnd());
                content.getText().replace(content.getSelectionStart(), content.getSelectionEnd(),
                        Character.toString(shortcut.charAt(0)) + selected + shortcut.charAt(1));
            } else {
                content.getText().insert(content.getSelectionStart(), shortcut);
                content.setSelection(content.getSelectionStart() - 1);
            }
        }
    }

    public void HeaderViewSwitcher(Boolean hasFocus) {
        viewSwitcher = (ViewSwitcher) findViewById(R.id.HeaderViewSwitcher);
        if (!hasFocus) {
            TextView headerNoteTitle = (TextView) findViewById(R.id.note_title_text);
            headerNoteTitle.setText(noteTitle.getText().toString());
            viewSwitcher.showNext();
        }
    }

    public void titleClicked(View view) {
        viewSwitcher.showPrevious();
        noteTitle.requestFocus();
    }
}
