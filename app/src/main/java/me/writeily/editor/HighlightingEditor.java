package me.writeily.editor;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.EditText;

import me.writeily.R;

public class HighlightingEditor extends EditText {

    public static final int DEFAULT_DELAY = 500;
    private Highlighter highlighter;
    private SharedPreferences prefs;

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
        prefs = PreferenceManager.getDefaultSharedPreferences(this.getContext());
        if (prefs.getBoolean(getStringFromStringTable(R.string.pref_highlighting_activated_key), false)) {
            init();
        }
    }

    public HighlightingEditor(Context context, AttributeSet attrs) {
        super(context, attrs);
        prefs = PreferenceManager.getDefaultSharedPreferences(this.getContext());
        if (prefs.getBoolean(getStringFromStringTable(R.string.pref_highlighting_activated_key), false)) {
            init();
        }
    }

    private void init() {

        setFilters(new InputFilter[]{new IndentationFilter()});

        final int highlightingDelay = getHighlightingDelayFromPrefs();

        highlighter = new Highlighter(new MyHighlighterColorsNeutral(),
            prefs.getString(getStringFromStringTable(R.string.pref_font_choice_key), ""),
            prefs.getString(getStringFromStringTable(R.string.pref_font_size_key), ""));

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
        String value = prefs.getString(getStringFromStringTable(R.string.pref_highlighting_delay_key), "");
        return value == null || value.equals("") ? DEFAULT_DELAY : Integer.valueOf(value);
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
                    dstart < dest.length()) {
                char c = source.charAt(start);

                if (c == '\n')
                    return autoIndent(
                            source,
                            dest,
                            dstart,
                            dend);
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
            if (istart > -1) {
                int iend;

                for (iend = ++istart;
                     iend < dend;
                     ++iend) {
                    char c = dest.charAt(iend);

                    if (c != ' ' &&
                            c != '\t') {
                        break;
                    }
                }

                return dest.subSequence(istart, iend) + addBulletPointIfNeeded(dest.charAt(iend));
            } else {
                return "";
            }
        }

        private String addBulletPointIfNeeded(char character) {
            return character == '*' ? Character.toString(character) + " " : "";
        }
    }

}
