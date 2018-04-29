/*
 * Copyright (c) 2017-2018 Gregor Santner
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.format.moduleactions;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;

import net.gsantner.markor.R;
import net.gsantner.markor.format.highlighter.HighlightingEditor;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.util.AppSettings;


@SuppressWarnings("WeakerAccess")
public abstract class TextModuleActions {
    protected HighlightingEditor _hlEditor;
    protected Document _document;
    protected Activity _activity;
    protected Context _context;
    protected AppSettings _appSettings;
    private int _textModuleSidePadding;

    public TextModuleActions(Activity activity, Document document) {
        _document = document;
        _activity = activity;
        _context = activity != null ? activity : _hlEditor.getContext();
        _appSettings = new AppSettings(_context);
        _textModuleSidePadding = (int) (_appSettings.getEditorTextmoduleBarItemPadding() * _context.getResources().getDisplayMetrics().density);
    }

    public abstract void appendTextModuleActionsToBar(ViewGroup viewGroup);

    protected void appendTextModuleActionToBar(ViewGroup barLayout, @DrawableRes int iconRes, View.OnClickListener l) {
        ImageView btn = (ImageView) _activity.getLayoutInflater().inflate(R.layout.ui__quick_keyboard_button, null);
        btn.setImageResource(iconRes);
        btn.setOnClickListener(l);
        btn.setPadding(_textModuleSidePadding, btn.getPaddingTop(), _textModuleSidePadding, btn.getPaddingBottom());

        boolean isDarkTheme = AppSettings.get().isDarkThemeEnabled();
        btn.setColorFilter(ContextCompat.getColor(_context,
                isDarkTheme ? android.R.color.white : R.color.grey));
        barLayout.addView(btn);
    }

    protected void setBarVisible(ViewGroup barLayout, boolean visible) {
        if (barLayout.getId() == R.id.document__fragment__edit__textmodule_actions_bar && barLayout.getParent() instanceof HorizontalScrollView) {
            ((HorizontalScrollView) barLayout.getParent())
                    .setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    //
    //
    //
    //
    public HighlightingEditor getHighlightingEditor() {
        return _hlEditor;
    }

    public TextModuleActions setHighlightingEditor(HighlightingEditor hlEditor) {
        _hlEditor = hlEditor;
        return this;
    }

    public Document getDocument() {
        return _document;
    }

    public TextModuleActions setDocument(Document document) {
        _document = document;
        return this;
    }

    public Activity getActivity() {
        return _activity;
    }

    public TextModuleActions setActivity(Activity activity) {
        _activity = activity;
        return this;
    }

    public Context getContext() {
        return _context;
    }

    public TextModuleActions setContext(Context context) {
        _context = context;
        return this;
    }
}
