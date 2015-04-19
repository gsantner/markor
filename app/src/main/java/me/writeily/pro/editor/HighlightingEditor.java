package me.writeily.pro.editor;

import android.content.Context;
import android.os.Handler;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.EditText;

public class HighlightingEditor extends EditText {

    private Highlighter highlighter;

    interface OnTextChangedListener {
        void onTextChanged(String text);
    }

    public OnTextChangedListener onTextChangedListener = null;
    public int updateDelay = 100;
    public boolean dirty = false;

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
        init();
    }

    public HighlightingEditor(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {

        setHorizontallyScrolling(true);

        setFilters(new InputFilter[]{new IndentationFilter()});

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

                        dirty = true;
                        updateHandler.postDelayed(
                                updateRunnable,
                                updateDelay);
                    }
                });

        highlighter = new Highlighter(new HighlighterColors() {

            private static final int COLOR_HEADER = 0xffef6C00;
            private static final int COLOR_LINK = 0xff1ea3fd;
            private static final int COLOR_LIST = COLOR_HEADER;

            @Override
            public int getHeaderColor() {
                return COLOR_HEADER;
            }

            @Override
            public int getLinkColor() {
                return COLOR_LINK;
            }

            @Override
            public int getListColor() {
                return COLOR_LIST;
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
