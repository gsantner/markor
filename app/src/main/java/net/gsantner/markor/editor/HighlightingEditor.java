/*
 * Copyright (c) 2014 Jeff Martin
 * Copyright (c) 2015 Pedro Lafuente
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.editor;

import android.content.Context;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.AttributeSet;

import net.gsantner.markor.util.AppSettings;

public class HighlightingEditor extends AppCompatEditText {

    private Highlighter highlighter;

    interface OnTextChangedListener {
        void onTextChanged(String text);
    }

    private OnTextChangedListener onTextChangedListener = null;

    private final Handler updateHandler = new Handler();
    private final Runnable updateRunnable =
            new Runnable() {
                @Override
                public void run() {
                    Editable e = getText();

                    if (onTextChangedListener != null)
                        onTextChangedListener.onTextChanged(e.toString());

                    highlightWithoutChange(e);
                }
            };
    private boolean modified = true;

    public HighlightingEditor(Context context) {
        super(context);
        if (AppSettings.get().isHighlightingEnabled()) {
            init();
        }
    }

    public HighlightingEditor(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (AppSettings.get().isHighlightingEnabled()) {
            init();
        }
    }

    public HighlightingEditor(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (AppSettings.get().isHighlightingEnabled()) {
            init();
        }
    }

    private void init() {

        setFilters(new InputFilter[]{new IndentationFilter()});

        final int highlightingDelay = getHighlightingDelayFromPrefs();

        highlighter = new Highlighter(new MyHighlighterColorsNeutral(),
                AppSettings.get().getFontFamily(),
                AppSettings.get().getFontSize());

        addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count) {
                    }

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after) {
                    }

                    @Override
                    public void afterTextChanged(Editable e) {
                        cancelUpdate();

                        if (!modified)
                            return;

                        updateHandler.postDelayed(
                                updateRunnable,
                                highlightingDelay);
                    }
                });
    }

    private void cancelUpdate() {
        updateHandler.removeCallbacks(updateRunnable);
    }

    private void highlightWithoutChange(Editable e) {
        modified = false;
        highlighter.run(e);
        modified = true;
    }

    private String getStringFromStringTable(int preference_key) {
        return this.getContext().getString(preference_key);
    }

    private int getHighlightingDelayFromPrefs() {
        return AppSettings.get().getHighlightingDelay();
    }

    private class IndentationFilter implements InputFilter {
        @Override
        public CharSequence filter(
                CharSequence source,
                int start,
                int end,
                Spanned dest,
                int dstart,
                int dend) {

            if (modified &&
                    end - start == 1 &&
                    start < source.length() &&
                    dstart <= dest.length()) {
                char newChar = source.charAt(start);

                if (newChar == '\n') {
                    return autoIndent(
                            source,
                            dest,
                            dstart,
                            dend);
                }
            }

            return source;
        }

        private CharSequence autoIndent(CharSequence source, Spanned dest, int dstart, int dend) {

            int istart = findLineBreakPosition(dest, dstart);

            // append white space of previous line and new indent
            return source + createIndentForNextLine(dest, dend, istart);
        }

        private int findLineBreakPosition(Spanned dest, int dstart) {
            int istart = dstart - 1;

            for (; istart > -1; --istart) {
                char c = dest.charAt(istart);

                if (c == '\n')
                    break;
            }
            return istart;
        }

        private String createIndentForNextLine(Spanned dest, int dend, int istart) {
            //TODO: Auto-populate the next number for ordered-lists in addition to bullet points
            //TODO: Replace this
            if (istart > -1 && istart < dest.length() - 1) {
                int iend;

                for (iend = ++istart;
                     iend < dest.length() - 1;
                     ++iend) {
                    char c = dest.charAt(iend);

                    if (c != ' ' &&
                            c != '\t') {
                        break;
                    }
                }

                if (iend < dest.length() - 1) {
                    if (dest.charAt(iend + 1) == ' ') {
                        return dest.subSequence(istart, iend) + addBulletPointIfNeeded(dest.charAt(iend));
                    } else {
                        return "";
                    }
                } else {
                    return "";
                }
            } else if (istart > -1) {
                return "";
            } else if (dest.length() > 1) { // You need at least a list marker and a space to trigger auto-list-item
                if (dest.charAt(1) == ' ') {
                    return addBulletPointIfNeeded(dest.charAt(0));
                } else {
                    return "";
                }
            } else {
                return "";
            }
        }

        private String addBulletPointIfNeeded(char character) {
            if (character == '*' || character == '+' || character == '-') {
                return Character.toString(character) + " ";
            } else {
                return "";
            }

        }
    }

}
