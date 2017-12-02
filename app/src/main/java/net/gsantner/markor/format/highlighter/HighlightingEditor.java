/*
 * Copyright (c) 2014 Jeff Martin
 * Copyright (c) 2015 Pedro Lafuente
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.format.highlighter;

import android.content.Context;
import android.os.Handler;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.AttributeSet;

import net.gsantner.markor.util.AppSettings;


public class HighlightingEditor extends AppCompatEditText {

    private Highlighter _highlighter;
    private boolean _doHighlighting = false;

    interface OnTextChangedListener {
        void onTextChanged(String text);
    }

    private OnTextChangedListener _onTextChangedListener = null;

    private final Handler _updateHandler = new Handler();
    private final Runnable _updateRunnable = () -> {
        Editable e = getText();

        if (_onTextChangedListener != null)
            _onTextChangedListener.onTextChanged(e.toString());

        highlightWithoutChange(e);
    };
    private boolean _modified = true;


    public HighlightingEditor(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (AppSettings.get().isHighlightingEnabled()) {
            setHighlighter(Highlighter.getDefaultHighlighter());
            setAutoFormat(_highlighter.getAutoFormatter());
            setHighlightingEnabled(AppSettings.get().isHighlightingEnabled());
        }
        init();
    }

    public void setHighlightingEnabled(boolean enable) {
        _doHighlighting = enable;
    }

    public boolean isDoHighlighting() {
        return _doHighlighting;
    }

    public void setHighlighter(Highlighter newHighlighter) {
        _highlighter = newHighlighter;
        reloadHighlighter();

        // Alpha in animation
        setAlpha(0.3f);
        animate().alpha(1.0f)
                .setDuration(500)
                .setListener(null);
    }

    private void setAutoFormat(InputFilter newAutoFormatter) {
        setFilters(new InputFilter[]{newAutoFormatter});
    }

    private void removeAutoFormat() {
        setFilters(new InputFilter[]{});
    }

    private void enableHighlighterAutoFormat() {
        if (_doHighlighting) {
            setAutoFormat(_highlighter.getAutoFormatter());
        }
    }

    private void init() {
        final int highlightingDelay = _highlighter != null
                ? _highlighter.loadHighlightingDelay(getContext())
                : new AppSettings(getContext()).getMarkdownHighlightingDelay();

        addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable e) {
                cancelUpdate();
                if (!_modified) {
                    return;
                }
                _updateHandler.postDelayed(_updateRunnable, highlightingDelay);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
        });
    }

    private void cancelUpdate() {
        _updateHandler.removeCallbacks(_updateRunnable);
    }

    public void reloadHighlighter() {
        enableHighlighterAutoFormat();
        highlightWithoutChange(getText());
    }

    private void highlightWithoutChange(Editable editable) {
        if (_doHighlighting) {
            _modified = false;
            _highlighter.run(this, editable);
            _modified = true;
        }
    }
}
