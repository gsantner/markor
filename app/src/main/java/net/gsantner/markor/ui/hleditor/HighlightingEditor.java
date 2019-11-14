/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.ui.hleditor;

import android.content.Context;
import android.os.Handler;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;

import net.gsantner.markor.activity.MainActivity;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ContextUtils;

import java.io.File;


@SuppressWarnings("UnusedReturnValue")
public class HighlightingEditor extends AppCompatEditText {
    public boolean isCurrentLineEmpty() {
        final int posOrig = getSelectionStart();
        final int posLineBegin = moveCursorToBeginOfLine(0);
        final int posEndBegin = moveCursorToEndOfLine(0);
        setSelection(posOrig);
        return posLineBegin == posEndBegin;
    }

    interface OnTextChangedListener {
        void onTextChanged(String text);
    }

    private final boolean _isDeviceGoodHardware;
    private boolean _modified = true;
    private boolean _hlEnabled = false;
    private boolean _isSpellingRedUnderline;
    private Highlighter _hl;

    private OnTextChangedListener _onTextChangedListener = null;
    public final static String PLACE_CURSOR_HERE_TOKEN = "%%PLACE_CURSOR_HERE%%";
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
            setHighlighter(Highlighter.getDefaultHighlighter(this, new Document(new File("/tmp"))));
            setAutoFormat(_hl.getAutoFormatter());
            setHighlightingEnabled(as.isHighlightingEnabled());
        }

        _isDeviceGoodHardware = new ContextUtils(context).isDeviceGoodHardware();
        _isSpellingRedUnderline = !as.isDisableSpellingRedUnderline();
        addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable e) {
                cancelUpdate();
                if (!_modified) {
                    return;
                }
                if (MainActivity.IS_DEBUG_ENABLED) {
                    AppSettings.appendDebugLog("Changed text (afterTextChanged)");
                }
                if (_hl != null) {
                    int delay = (int) _hl.getHighlightingFactorBasedOnFilesize() * (_hl.isFirstHighlighting() ? 300 : _hl.getHighlightingDelay(getContext()));
                    if (MainActivity.IS_DEBUG_ENABLED) {
                        AppSettings.appendDebugLog("Highlighting run: delay " + delay + "ms, cfactor " + _hl.getHighlightingFactorBasedOnFilesize());
                    }
                    _updateHandler.postDelayed(_updateRunnable, delay);
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (MainActivity.IS_DEBUG_ENABLED) {
                    AppSettings.appendDebugLog("Changed text (onTextChanged)");
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (MainActivity.IS_DEBUG_ENABLED) {
                    AppSettings.appendDebugLog("Changed text (beforeTextChanged)");
                }
            }
        });
    }

    public void setHighlighter(Highlighter newHighlighter) {
        _hl = newHighlighter;
        reloadHighlighter();

        // Alpha in animation
        setAlpha(0.3f);
        animate().alpha(1.0f)
                .setDuration(500)
                .setListener(null);
    }

    private void enableHighlighterAutoFormat() {
        //if (_hlEnabled) {
        setAutoFormat(_hl.getAutoFormatter());
        //}
    }

    private void cancelUpdate() {
        _updateHandler.removeCallbacks(_updateRunnable);
    }

    public void reloadHighlighter() {
        enableHighlighterAutoFormat();
        highlightWithoutChange(getText());
    }

    private void highlightWithoutChange(Editable editable) {
        if (_hlEnabled && editable.length() <= (_isDeviceGoodHardware ? 100000 : 35000)) {
            _modified = false;
            try {
                if (MainActivity.IS_DEBUG_ENABLED) {
                    AppSettings.appendDebugLog("Start highlighting");
                }
                _hl.run(editable);
            } catch (Exception e) {
                // In no case ever let highlighting crash the editor
                e.printStackTrace();
            } catch (Error e) {
                e.printStackTrace();
            }
            if (MainActivity.IS_DEBUG_ENABLED) {
                AppSettings.appendDebugLog(_hl._profiler.resetDebugText());
                AppSettings.appendDebugLog("Finished highlighting");
            }
            _modified = true;
        }
    }

    public void simulateKeyPress(int keyEvent_KEYCODE_SOMETHING) {
        dispatchKeyEvent(new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, keyEvent_KEYCODE_SOMETHING, 0));
        dispatchKeyEvent(new KeyEvent(0, 0, KeyEvent.ACTION_UP, keyEvent_KEYCODE_SOMETHING, 0));
    }

    public void insertOrReplaceTextOnCursor(String newText) {
        newText = (newText == null ? "" : newText);
        int newCursorPos = newText.indexOf(PLACE_CURSOR_HERE_TOKEN);
        newText = newText.replace(PLACE_CURSOR_HERE_TOKEN, "");
        int start = Math.max(getSelectionStart(), 0);
        int end = Math.max(getSelectionEnd(), 0);
        getText().replace(Math.min(start, end), Math.max(start, end), newText, 0, newText.length());
        if (newCursorPos >= 0) {
            setSelection(start + newCursorPos);
        }
    }

    public int getShiftWidth(String text) {
        if (text.contains("sw=2") || text.contains("shiftwidth=2")) {
            return 2;
        } else if (text.contains("sw=8") || text.contains("shiftwidth=8")) {
            return 8;
        } else {
            return 4;
        }
    }

    public int moveCursorToEndOfLine(int offset) {
        simulateKeyPress(KeyEvent.KEYCODE_MOVE_END);
        setSelection(getSelectionStart() + offset);
        return getSelectionStart();
    }

    public int moveCursorToBeginOfLine(int offset) {
        simulateKeyPress(KeyEvent.KEYCODE_MOVE_HOME);
        setSelection(getSelectionStart() + offset);
        return getSelectionStart();
    }

    //
    // Simple getter / setter
    //
    private void setAutoFormat(InputFilter newAutoFormatter) {
        setFilters(new InputFilter[]{newAutoFormatter});
    }

    public void setHighlightingEnabled(boolean enable) {
        _hlEnabled = enable;
    }


    public boolean indexesValid(int... indexes) {
        int len = length();
        for (int index : indexes) {
            if (index < 0 || index > len) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isSuggestionsEnabled() {
        return _isSpellingRedUnderline && super.isSuggestionsEnabled();
    }

    @Override
    public void setSelection(int index) {
        if (indexesValid(index)) {
            super.setSelection(index);
        }
    }

    @Override
    public void setSelection(int start, int stop) {
        if (indexesValid(start, stop)) {
            super.setSelection(start, stop);
        } else if (indexesValid(start, stop - 1)) {
            super.setSelection(start, stop - 1);
        } else if (indexesValid(start + 1, stop)) {
            super.setSelection(start + 1, stop);
        }
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        if (MainActivity.IS_DEBUG_ENABLED) {
            AppSettings.appendDebugLog("Selection changed: " + selStart + "->" + selEnd);
        }

    }
}
