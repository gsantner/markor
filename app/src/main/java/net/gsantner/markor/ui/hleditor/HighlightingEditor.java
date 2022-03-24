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
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

import net.gsantner.markor.activity.MainActivity;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.opoc.util.Callback;
import net.gsantner.opoc.util.StringUtils;

import java.io.File;
import java.util.HashSet;
import java.util.Set;


@SuppressWarnings("UnusedReturnValue")
public class HighlightingEditor extends AppCompatEditText {

    public boolean isCurrentLineEmpty() {
        final int posOrig = getSelectionStart();
        final int posLineBegin = moveCursorToBeginOfLine(0);
        final int posEndBegin = moveCursorToEndOfLine(0);
        setSelection(posOrig);
        return posLineBegin == posEndBegin;
    }

    private boolean _modified = true;
    private boolean _hlEnabled = false;
    private boolean _accessibilityEnabled = true;
    private final boolean _isSpellingRedUnderline;
    private Highlighter _hl;
    private final Set<TextWatcher> _activeListeners = new HashSet<>(); /* Tracks currently applied modifiers */
    private InputFilter _autoFormatFilter;
    private TextWatcher _autoFormatModifier;

    public final static String PLACE_CURSOR_HERE_TOKEN = "%%PLACE_CURSOR_HERE%%";
    private final Handler _updateHandler = new Handler();
    private final Runnable _updateRunnable;

    public HighlightingEditor(Context context, AttributeSet attrs) {
        super(context, attrs);
        AppSettings as = new AppSettings(context);
        if (as.isHighlightingEnabled()) {
            setHighlighter(Highlighter.getDefaultHighlighter(this, new Document(new File("/tmp"))));
            setHighlightingEnabled(as.isHighlightingEnabled());
        }

        // Initialize. Null == empty
        setAutoFormatters(null, null);

        _isSpellingRedUnderline = !as.isDisableSpellingRedUnderline();
        _updateRunnable = () -> {
            highlightWithoutChange();
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            setFallbackLineSpacing(false);
        }

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

    public void setHighlighter(final Highlighter newHighlighter) {
        _hl = newHighlighter;
        highlightWithoutChange();

        // Alpha in animation
        setAlpha(0.3f);
        animate().alpha(1.0f)
                .setDuration(500)
                .setListener(null);
    }

    public Highlighter getHighlighter() {
        return _hl;
    }

    public void setAutoFormatters(final InputFilter inputFilter, final TextWatcher modifier) {
        setAutoFormatEnabled(false); // Remove any existing modifiers if applied
        _autoFormatFilter = inputFilter;
        _autoFormatModifier = modifier;
    }

    public boolean getAutoFormatEnabled() {
        final boolean filterApplied = getFilters().length > 0;
        final boolean modifierApplied = _autoFormatModifier != null && _activeListeners.contains(_autoFormatModifier);
        return (filterApplied || modifierApplied);
    }

    public void setAutoFormatEnabled(final boolean enable) {
        if (enable) {
            if (_autoFormatFilter != null) {
                setFilters(new InputFilter[]{_autoFormatFilter});
            }
            if (_autoFormatModifier != null && !_activeListeners.contains(_autoFormatModifier)) {
                addTextChangedListener(_autoFormatModifier);
            }
        } else {
            setFilters(new InputFilter[]{});

            if (_autoFormatModifier != null) {
                removeTextChangedListener(_autoFormatModifier);
            }
        }
    }

    @Override
    public void addTextChangedListener(final TextWatcher listener) {
        _activeListeners.add(listener);
        super.addTextChangedListener(listener);
    }

    @Override
    public void removeTextChangedListener(final TextWatcher listener) {
        super.removeTextChangedListener(listener);
        _activeListeners.remove(listener);
    }

    // Run some code with auto formatters disabled
    public void withAutoFormatDisabled(final Callback.a0 callback) {
        if (getAutoFormatEnabled()) {
            try {
                setAutoFormatEnabled(false);
                callback.callback();
            } finally {
                setAutoFormatEnabled(true);
            }
        } else {
            callback.callback();
        }
    }

    private void cancelUpdate() {
        _updateHandler.removeCallbacks(_updateRunnable);
    }

    // Accessibility code is blocked during rapid update events
    // such as highlighting and some actions.
    // This prevents errors and potential crashes.
    @Override
    public void sendAccessibilityEventUnchecked(AccessibilityEvent event) {
        if (_accessibilityEnabled && length() < 10000) {
            super.sendAccessibilityEventUnchecked(event);
        }
    }

    // Hleditor will report that it is not autofillable under certain circumstances
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int getAutofillType() {
        if (_accessibilityEnabled && length() < 10000) {
            return super.getAutofillType();
        } else {
            return View.AUTOFILL_TYPE_NONE;
        }
    }

    private void highlightWithoutChange() {
        if (_hlEnabled) {
            _modified = false;
            try {
                if (MainActivity.IS_DEBUG_ENABLED) {
                    AppSettings.appendDebugLog("Start highlighting");
                }
                setAccessibilityEnabled(false);
                _hl.run(getText());
            } catch (Exception e) {
                // In no case ever let highlighting crash the editor
                e.printStackTrace();
            } catch (Error e) {
                e.printStackTrace();
            } finally {
                setAccessibilityEnabled(true);
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

    public void insertOrReplaceTextOnCursor(final String newText) {
        final Editable edit = getText();
        if (edit != null && newText != null) {
            int newCursorPos = newText.indexOf(PLACE_CURSOR_HERE_TOKEN);
            final String finalText = newText.replace(PLACE_CURSOR_HERE_TOKEN, "");
            final int[] sel = StringUtils.getSelection(this);
            sel[0] = Math.max(sel[0], 0);
            withAutoFormatDisabled(() -> edit.replace(sel[0], sel[1], finalText) );
            if (newCursorPos >= 0) {
                setSelection(sel[0] + newCursorPos);
            }
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

    // Set selection to fill whole lines
    // Returns original selectionStart
    public int setSelectionExpandWholeLines() {
        final int[] sel = StringUtils.getSelection(this);
        final CharSequence text = getText();
        setSelection(
                StringUtils.getLineStart(text, sel[0]),
                StringUtils.getLineEnd(text, sel[1])
        );
        return sel[0];
    }

    //
    // Simple getter / setter
    //

    public void setHighlightingEnabled(final boolean enable) {
        if (_hlEnabled && !enable) {
            _hlEnabled = false;
            Highlighter.clearSpans(getText());
        } else if (!_hlEnabled && enable && _hl != null) {
            _hlEnabled = true;
            highlightWithoutChange();
        }
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

    public void setAccessibilityEnabled(final boolean enabled) {
        _accessibilityEnabled = enabled;
    }

    public boolean getAccessibilityEnabled() {
        return _accessibilityEnabled;
    }
}
