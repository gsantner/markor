/*
 * Copyright (c) 2017-2018 Gregor Santner and Markor contributors
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
import android.view.KeyEvent;

import net.gsantner.markor.util.AppSettings;


public class HighlightingEditor extends AppCompatEditText {
    interface OnTextChangedListener {
        void onTextChanged(String text);
    }

    private boolean _modified = true;
    private boolean _hlEnabled = false;
    private Highlighter _hl;
    private int _hlDelay;

    private OnTextChangedListener _onTextChangedListener = null;
    private final Handler _updateHandler = new Handler();
    private final Runnable _updateRunnable = () -> {
        Editable e = getText();
        if (_onTextChangedListener != null) {
            _onTextChangedListener.onTextChanged(e.toString());
        }
        highlightWithoutChange(e);
    };


    public HighlightingEditor(Context context, AttributeSet attrs) {
        super(context, attrs);
        AppSettings as = new AppSettings(context);
        if (as.isHighlightingEnabled()) {
            setHighlighter(Highlighter.getDefaultHighlighter());
            setAutoFormat(_hl.getAutoFormatter());
            setHighlightingEnabled(as.isHighlightingEnabled());
        }

        _hlDelay = as.getMarkdownHighlightingDelay();
        addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable e) {
                cancelUpdate();
                if (!_modified) {
                    return;
                }
                _updateHandler.postDelayed(_updateRunnable, _hlDelay);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
        });
    }

    public void setHighlighter(Highlighter newHighlighter) {
        _hl = newHighlighter;
        _hlDelay = _hl.getHighlightingDelay(getContext());
        reloadHighlighter();

        // Alpha in animation
        setAlpha(0.3f);
        animate().alpha(1.0f)
                .setDuration(500)
                .setListener(null);
    }

    private void enableHighlighterAutoFormat() {
        if (_hlEnabled) {
            setAutoFormat(_hl.getAutoFormatter());
        }
    }

    private void cancelUpdate() {
        _updateHandler.removeCallbacks(_updateRunnable);
    }

    public void reloadHighlighter() {
        enableHighlighterAutoFormat();
        highlightWithoutChange(getText());
    }

    private void highlightWithoutChange(Editable editable) {
        if (_hlEnabled) {
            _modified = false;
            try {
                _hl.run(this, editable);
            } catch (Exception e) {
                // In no case ever let highlighting crash the editor
                e.printStackTrace();
            }
            _modified = true;
        }
    }

    public void pressKeyOnce(int keyEvent_KEYCODE_SOMETHING) {
        dispatchKeyEvent(new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, keyEvent_KEYCODE_SOMETHING, 0));
        dispatchKeyEvent(new KeyEvent(0, 0, KeyEvent.ACTION_UP, keyEvent_KEYCODE_SOMETHING, 0));
    }

    //
    // Simple getter / setter
    //
    private void setAutoFormat(InputFilter newAutoFormatter) {
        setFilters(new InputFilter[]{newAutoFormatter});
    }

    private void removeAutoFormat() {
        setFilters(new InputFilter[]{});
    }

    public void setHighlightingEnabled(boolean enable) {
        _hlEnabled = enable;
    }

    public boolean isDoHighlighting() {
        return _hlEnabled;
    }
}
