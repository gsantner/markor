package me.writeily.pro;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;

import me.writeily.pro.model.Constants;

/**
 * Created by jeff on 2014-04-11.
 */
public class NoteActivity extends ActionBarActivity {

    private File note;
    private Context context;

    private EditText noteTitle;
    private EditText content;
    private ViewGroup keyboardBarView;
    private String sourceDir;

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
        setContentView(R.layout.activity_note);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        context = getApplicationContext();
        content = (EditText) findViewById(R.id.note_content);
        noteTitle = (EditText) findViewById(R.id.edit_note_title);
        keyboardBarView = (ViewGroup) findViewById(R.id.keyboard_bar);

        Intent receivingIntent = getIntent();
        sourceDir = receivingIntent.getStringExtra(Constants.NOTE_SOURCE_DIR);

        String intentAction = receivingIntent.getAction();
        String type = receivingIntent.getType();

        if (Intent.ACTION_SEND.equals(intentAction) && type != null) {
            openFromSendAction(receivingIntent);
        } else if (Intent.ACTION_EDIT.equals(intentAction) && type != null) {
            openFromEditAction(receivingIntent);
        } else {
            note = (File) getIntent().getSerializableExtra(Constants.NOTE_KEY);
        }

        if (note != null) {
            content.setText(readNote());
            noteTitle.setText(note.getName());
        }

        super.onCreate(savedInstanceState);
    }

    private String readNote() {
        java.net.URI oldUri = note.toURI();
        return readFileUri(Uri.parse(oldUri.toString()));
    }

    private void openFromSendAction(Intent receivingIntent) {
        Uri fileUri = receivingIntent.getParcelableExtra(Intent.EXTRA_STREAM);
        readFileUriFromIntent(fileUri);
    }

    private void openFromEditAction(Intent receivingIntent) {
        Uri fileUri = receivingIntent.getData();
        readFileUriFromIntent(fileUri);
    }

    private Uri getUriFromFile(File f) {
        Uri u = null;
        if (f != null) {
            u = Uri.parse(f.toURI().toString());
        }
        return u;
    }

    private File getFileFromUri(Uri u) {
        File f = null;
        if (u != null) {
            try {
                f = new File(new java.net.URI(URLEncoder.encode(u.toString(), "UTF-8")));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        return f;
    }

    private void readFileUriFromIntent(Uri fileUri) {
        if (fileUri != null) {
            note = getFileFromUri(fileUri);
            content.setText(readFileUri(fileUri));
        }
    }

    private String readFileUri(Uri fileUri) {
        StringBuilder uriContent = new StringBuilder();
        if (fileUri != null) {
            try {
                InputStreamReader reader = new InputStreamReader(getContentResolver().openInputStream(fileUri));
                BufferedReader br = new BufferedReader(reader);

                while (br.ready()) {
                    uriContent.append(br.readLine());
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return uriContent.toString();
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
        super.onPause();
    }

    private void setupKeyboardBar() {
        if (keyboardBarView.getChildCount() == 0) {
            for (String shortcut : Constants.KEYBOARD_SHORTCUTS) {
                Button shortcutButton = new Button(this);
                shortcutButton.setText(shortcut);
                shortcutButton.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT));

                shortcutButton.setTextSize(18);
                shortcutButton.setTypeface(null, Typeface.BOLD);
                shortcutButton.setBackground(getResources().getDrawable(R.drawable.keyboard_shortcut_button));
                shortcutButton.setOnClickListener(new KeyboardBarListener());

                String theme = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.pref_theme_key), "");

                if (theme.equals(getString(R.string.theme_dark))) {
                    shortcutButton.setTextColor(getResources().getColor(android.R.color.white));
                } else {
                    shortcutButton.setTextColor(getResources().getColor(R.color.grey));
                }

                keyboardBarView.addView(shortcutButton);
            }
        }
    }

    private void setupAppearancePreferences() {
        String theme = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.pref_theme_key), "");
        String fontType = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.pref_font_choice_key), "");
        String fontSize = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.pref_font_size_key), "");

        if (!fontSize.equals("")) {
            content.setTextSize(TypedValue.COMPLEX_UNIT_SP, Float.parseFloat(fontSize));
        }

        if (!fontType.equals("")) {
            content.setTypeface(Typeface.create(fontType, Typeface.NORMAL));
        }

        if (theme.equals(getString(R.string.theme_dark))) {
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
            Uri uriBase = getUriFromFile(note.getParentFile());
            intent.putExtra(Constants.MD_PREVIEW_BASE, uriBase.toString());
        }
        intent.putExtra(Constants.MD_PREVIEW_KEY, content.getText().toString().replace("\n-", "\n\n-"));

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
            // Creating a new note
            if (note == null) {
                if (noteTitle == null || noteTitle.getText().length() == 0) {
                    if (content.getText().toString().length() == 0) {
                        // If they didn't write anything at all, don't bother saving the file
                        return;
                    } else {
                        String snippet = "";
                        if (content.getText().toString().length() < Constants.MAX_TITLE_LENGTH) {
                            snippet = content.getText().toString().substring(0, content.getText().toString().length()).replace("[^\\w\\s]+", " ");
                        } else {
                            snippet = content.getText().toString().substring(0, Constants.MAX_TITLE_LENGTH).replace("[^\\w\\s]+", " ");
                        }
                        noteTitle.setText(snippet);
                    }
                }

                note = new File(sourceDir + File.separator + noteTitle.getText().toString());
            }

            // If we have to rename the file, do a delete and create
            if (!noteTitle.getText().toString().equals(note.getName())) {
                note.delete();
                note = new File(sourceDir + File.separator + noteTitle.getText().toString());
            }

            FileOutputStream fos = new FileOutputStream(note);
            OutputStreamWriter  writer = new OutputStreamWriter(fos);

            writer.write(content.getText().toString());
            writer.flush();

            writer.close();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class KeyboardBarListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            CharSequence shortcut = ((Button) v).getText();
            content.getText().insert(content.getSelectionStart(), shortcut);
        }
    }
}
