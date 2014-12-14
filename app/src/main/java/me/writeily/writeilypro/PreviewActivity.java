package me.writeily.writeilypro;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.webkit.WebView;

import com.commonsware.cwac.anddown.AndDown;
import me.writeily.writeilypro.model.Constants;

/**
 * Created by jeff on 2014-04-13.
 */
public class PreviewActivity extends ActionBarActivity {

    private WebView previewWebView;
    private String markdownRaw;

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

        renderMarkdown();
        super.onCreate(savedInstanceState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void renderMarkdown() {
        String markdownHtml = "";

        String theme = PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.pref_theme_key), "");

        if (!theme.equals("")) {
            if (theme.equals(getString(R.string.theme_dark))) {
                markdownHtml += Constants.DARK_MD_HTML_PREFIX;
            } else {
                markdownHtml += Constants.MD_HTML_PREFIX;
            }
        }

        AndDown andDown = new AndDown();
        markdownHtml += andDown.markdownToHtml(markdownRaw) + Constants.MD_HTML_SUFFIX;

        previewWebView.loadDataWithBaseURL(null, markdownHtml, "text/html", Constants.UTF_CHARSET, null);
    }
}
