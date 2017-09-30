/*
 * Copyright (c) 2014 Jeff Martin
 * Copyright (c) 2015 Pedro Lafuente
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import net.gsantner.markor.R;
import net.gsantner.markor.model.Constants;
import net.gsantner.markor.renderer.MarkDownRenderer;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ContextUtils;
import net.gsantner.opoc.util.FileUtils;

import java.io.File;
import java.io.FileOutputStream;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PreviewActivity extends AppCompatActivity {
    @BindView(R.id.preview__activity__webview)
    public WebView _webview;

    @BindView(R.id.toolbar)
    public Toolbar _toolbar;

    private String _markdownRaw;
    private String _markdownHtml;
    private File _note;
    private boolean _isEditIncoming = false;
    private MarkDownRenderer _renderer = new MarkDownRenderer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContextUtils.get().setAppLanguage(AppSettings.get().getLanguage());
        if (AppSettings.get().isEditorStatusBarHidden()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        setContentView(R.layout.preview__activity);
        ButterKnife.bind(this);

        setSupportActionBar(_toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        _markdownRaw = getIntent().getStringExtra(Constants.MD_PREVIEW_KEY);
        String baseFolder = getIntent().getStringExtra(Constants.MD_PREVIEW_BASE);
        _note = (File) getIntent().getSerializableExtra(Constants.NOTE_KEY);
        loadNote(_note, _markdownRaw, baseFolder);


        _webview.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("file://")) {
                    File file = new File(url.replace("file://", ""));
                    String mimetype;
                    if (ContextUtils.get().isMaybeMarkdownFile(file)) {
                        _note = file;
                        loadNote(_note, null, null);
                    } else if ((mimetype = ContextUtils.getMimeType(url)) != null) {
                        Intent intent = new Intent();
                        intent.setAction(android.content.Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.fromFile(file), mimetype);
                        startActivity(intent);
                    } else {
                        Uri uri = Uri.parse(_note.getAbsolutePath());
                        startActivity(Intent.createChooser(new Intent(Intent.ACTION_VIEW, uri), getString(R.string.open_with)));
                    }
                } else {
                    ContextUtils.get().openWebpageInExternalBrowser(url);
                }
                return true;
            }
        });
    }

    // This will load text from file if markdown is empty
    private void loadNote(File note, String markdownRaw, String baseFolder) {
        setTitle(getResources().getString(R.string.preview));
        if (note != null && note.exists()) {
            setTitle(_note.getName());
            if (TextUtils.isEmpty(markdownRaw)) {
                markdownRaw = FileUtils.readTextFile(_note);
            }
            if (TextUtils.isEmpty(baseFolder)) {
                baseFolder = _note.getParent();
            }
        }
        if (TextUtils.isEmpty(markdownRaw)) {
            markdownRaw = "";
        }
        if (TextUtils.isEmpty(baseFolder)) {
            baseFolder = AppSettings.get().getSaveDirectory();
        }
        _markdownRaw = markdownRaw;
        _markdownHtml = _renderer.renderMarkdown(_markdownRaw, getApplicationContext());
        _webview.loadDataWithBaseURL(baseFolder, _markdownHtml, "text/html", Constants.UTF_CHARSET, null);

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (_isEditIncoming) {
            finish();
        }
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
            case R.id.action_share_text:
                shareText(_markdownRaw, "text/plain");
                return true;
            case R.id.action_share_file:
                shareStream(Uri.fromFile(_note), "text/plain");
                return true;
            case R.id.action_share_html:
                shareText(_markdownHtml, "text/html");
                return true;
            case R.id.action_share_html_source:
                shareText(_markdownHtml, "text/plain");
                return true;
            case R.id.action_share_image:
                shareImage();
                return true;
            case R.id.action_edit:
                editNote();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void shareText(String text, String type) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT, text);
        shareIntent.setType(type);
        startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.share_string)));
    }

    private void shareStream(Uri uri, String type) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.setType(type);
        startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.share_string)));
    }

    private void shareImage() {
        Bitmap bitmap = getBitmapFromWebView(_webview);
        if (bitmap != null) {
            File image = new File(getExternalCacheDir(), _note.getName() + ".png");
            if (saveBitmap(bitmap, image)) {
                shareStream(Uri.fromFile(image), "image/png");
            }
        }
    }

    private Bitmap getBitmapFromWebView(WebView webView) {
        try {
            float scale = 1.0f / getResources().getDisplayMetrics().density;
            Picture picture = webView.capturePicture();
            Bitmap bitmap = Bitmap.createBitmap((int) (picture.getWidth() * scale), (int) (picture.getHeight() * scale), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.scale(scale, scale);
            picture.draw(canvas);
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean saveBitmap(Bitmap bitmap, File file) {
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (Exception e) {
            }
        }
    }

    private void editNote() {
        _isEditIncoming = true;

        Intent intent = new Intent(this, NoteActivity.class);
        intent.putExtra(Constants.NOTE_KEY, _note);

        startActivity(intent);
    }
}
