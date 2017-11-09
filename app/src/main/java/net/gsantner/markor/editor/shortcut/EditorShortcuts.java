package net.gsantner.markor.editor.shortcut;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;

import net.gsantner.markor.R;
import net.gsantner.markor.editor.HighlightingEditor;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.util.AppSettings;


public abstract class EditorShortcuts {
    protected HighlightingEditor _contentEditor;
    protected Document _document;
    protected Activity _activity;
    protected Context _context;

    public EditorShortcuts(HighlightingEditor contentEditor, Document document, Activity activity) {
        _contentEditor = contentEditor;
        _document = document;
        _activity = activity;
        _context = activity != null ? activity : _contentEditor.getContext();
    }

    public abstract void appendShortcutsToBar(ViewGroup viewGroup);

    protected void appendShortcutToBar(ViewGroup barLayout, @DrawableRes int iconRes, View.OnClickListener l) {
        ImageView btn = (ImageView) _activity.getLayoutInflater().inflate(R.layout.ui__quick_keyboard_button, (ViewGroup) null);
        btn.setImageResource(iconRes);
        btn.setOnClickListener(l);

        boolean isDarkTheme = AppSettings.get().isDarkThemeEnabled();
        btn.setColorFilter(ContextCompat.getColor(_context,
                isDarkTheme ? android.R.color.white : R.color.grey));
        barLayout.addView(btn);
    }

    protected void setBarVisible(ViewGroup barLayout, boolean visible) {
        if (barLayout.getId() == R.id.document__fragment__edit__shortcut_bar && barLayout.getParent() instanceof HorizontalScrollView) {
            ((HorizontalScrollView) barLayout.getParent())
                    .setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    //
    //
    //
    //
    public HighlightingEditor getContentEditor() {
        return _contentEditor;
    }

    public void setContentEditor(HighlightingEditor contentEditor) {
        _contentEditor = contentEditor;
    }

    public Document getDocument() {
        return _document;
    }

    public void setDocument(Document document) {
        _document = document;
    }

    public Activity getActivity() {
        return _activity;
    }

    public void setActivity(Activity activity) {
        _activity = activity;
    }

    public Context getContext() {
        return _context;
    }

    public void setContext(Context context) {
        _context = context;
    }
}
