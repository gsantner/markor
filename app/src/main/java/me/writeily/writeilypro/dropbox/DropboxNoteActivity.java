package me.writeily.writeilypro.dropbox;

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
import android.widget.EditText;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import me.writeily.writeilypro.PreviewActivity;
import me.writeily.writeilypro.R;
import me.writeily.writeilypro.model.Constants;

/**
 * Created by jeff on 2014-04-11.
 */
public class DropboxNoteActivity extends ActionBarActivity {

    private Context context;

    private EditText noteTitle;
    private EditText content;

    private String filePath;
    private String fileContent;
    private String fileTitle;

    public DropboxNoteActivity() {
    }

    public DropboxNoteActivity(Context context) {
        this.context = context;
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

        Intent receivingIntent = getIntent();

        filePath = receivingIntent.getStringExtra(Constants.DBX_NOTE_PATH);
        fileContent = receivingIntent.getStringExtra(Constants.DBX_NOTE_CONTENT);

        fileTitle = filePath.substring(filePath.lastIndexOf("/") + 1);

        content.setText(fileContent);
        noteTitle.setText(fileTitle);

        super.onCreate(savedInstanceState);
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
        IntentFilter ifilter = new IntentFilter();
        ifilter.addAction(Constants.SHARE_BROADCAST_TAG);
        super.onResume();
    }

    @Override
    protected void onPause() {
        saveNote();
        super.onPause();
    }

    private void previewNote() {
        saveNote();
        Intent intent = new Intent(this, PreviewActivity.class);

        // .replace is a workaround for Markdown lists requiring two \n characters
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

    private void saveNote() {

    }
}
