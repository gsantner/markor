package me.writeily.pro;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;

import com.commonsware.cwac.anddown.AndDown;

import java.io.File;

import me.writeily.pro.model.Constants;

/**
 * Created by jeff on 2014-04-13.
 */
public class PreviewActivity extends ActionBarActivity {

    private WebView previewWebView;
    private String markdownRaw;
    private String baseFolder = null;
    private String currentDir;
    private File note;
    private boolean isEditIncoming = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_preview);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        previewWebView = (WebView) findViewById(R.id.preview_webview);
        markdownRaw = getIntent().getStringExtra(Constants.MD_PREVIEW_KEY);
        baseFolder = getIntent().getStringExtra(Constants.MD_PREVIEW_BASE);

        currentDir = getIntent().getStringExtra(Constants.NOTE_SOURCE_DIR);
        note = (File) getIntent().getSerializableExtra(Constants.NOTE_KEY);

        if (note != null) {
            setTitle(note.getName());
        } else {
            setTitle(getResources().getString(R.string.preview));
        }

        renderMarkdown();
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onPause() {
        if (isEditIncoming) {
            finish();
        }

        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.preview_menu, menu);
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
            case R.id.action_edit:
                editNote();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void shareNote() {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT, markdownRaw);
        shareIntent.setType("text/plain");
        startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.share_string)));
    }

    private void editNote() {
        isEditIncoming = true;
        
        Intent intent = new Intent(this, NoteActivity.class);
        intent.putExtra(Constants.NOTE_SOURCE_DIR, currentDir);
        intent.putExtra(Constants.NOTE_KEY, note);

        startActivity(intent);
        overridePendingTransition(R.anim.anim_slide_in_left, R.anim.anim_slide_out_left);
    }

    private void renderMarkdown() {
        String markdownHtml = Constants.MD_HTML_PREFIX;

        String theme = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.pref_theme_key), "");

        if (!theme.equals("")) {
            if (theme.equals(getString(R.string.theme_dark))) {
                markdownHtml += Constants.DARK_MD_HTML_PREFIX;
            } else {
                markdownHtml += Constants.MD_HTML_PREFIX;
            }
        }

        AndDown andDown = new AndDown();
        markdownHtml += andDown.markdownToHtml(markdownRaw, 0, 3) + Constants.MD_HTML_SUFFIX;

        previewWebView.loadDataWithBaseURL(baseFolder, markdownHtml, "text/html", Constants.UTF_CHARSET, null);
    }


}
