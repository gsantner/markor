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

    private Highlighter highlighter;
    private boolean doHighlighting = false;

    interface OnTextChangedListener {
        void onTextChanged(String text);
    }

    private OnTextChangedListener onTextChangedListener = null;

    private final Handler updateHandler = new Handler();
    private final Runnable updateRunnable = () -> {
        Editable e = getText();

        if (onTextChangedListener != null)
            onTextChangedListener.onTextChanged(e.toString());

        highlightWithoutChange(e);
    };
    private boolean modified = true;


    public HighlightingEditor(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (AppSettings.get().isHighlightingEnabled()) {
            setHighlighter(Highlighter.getDefaultHighlighter());
            setAutoFormat(highlighter.getAutoFormatter());
            setHighlightingEnabled(AppSettings.get().isHighlightingEnabled());
        }
        init();
    }

    private void setHighlightingEnabled(boolean enable) {
        doHighlighting = enable;
    }

    public boolean isDoHighlighting() {
        return doHighlighting;
    }

    private void setHighlighter(Highlighter newHighlighter) {
        highlighter = newHighlighter;
    }

    private void setAutoFormat(InputFilter newAutoFormatter) {
        setFilters(new InputFilter[]{newAutoFormatter});
    }

    private void removeAutoFormat() {
        setFilters(new InputFilter[]{});
    }

    private void enableHighlighterAutoFormat() {
        if (doHighlighting) {
            setAutoFormat(highlighter.getAutoFormatter());
        }
    }

    private void init() {
        final int highlightingDelay = getHighlightingDelayFromPrefs();

        addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable e) {
                cancelUpdate();
                if (!modified) {
                    return;
                }
                updateHandler.postDelayed(updateRunnable, highlightingDelay);
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
        updateHandler.removeCallbacks(updateRunnable);
    }

    private void highlightWithoutChange(Editable e) {
        if (doHighlighting) {
            modified = false;
            highlighter.run(e);
            modified = true;
        }
    }

    private String rstr(int preference_key) {
        return getContext().getString(preference_key);
    }

    private int getHighlightingDelayFromPrefs() {
        return AppSettings.get().getHighlightingDelay();
    }

}
