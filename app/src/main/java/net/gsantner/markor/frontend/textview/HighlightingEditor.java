/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.frontend.textview;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.os.Parcelable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Layout;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityEvent;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.AppCompatEditText;

import net.gsantner.markor.ApplicationObject;
import net.gsantner.markor.activity.MainActivity;
import net.gsantner.markor.frontend.DraggableScrollbarScrollView;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.opoc.wrapper.GsCallback;
import net.gsantner.opoc.wrapper.GsTextWatcherAdapter;

@SuppressWarnings("UnusedReturnValue")
public class HighlightingEditor extends AppCompatEditText {

    final static int HIGHLIGHT_SHIFT_LINES = 8;              // Lines to scroll before hl updated
    final static float HIGHLIGHT_REGION_SIZE = 0.75f;        // Minimum extra screens to highlight (should be > 0.5 to cover screen)

    public final static String PLACE_CURSOR_HERE_TOKEN = "%%PLACE_CURSOR_HERE%%";

    private int _prevSelStart = -1, _prevSelEnd = -1;
    boolean _inSetSelection = false;
    private boolean _accessibilityEnabled = true;
    private final boolean _isSpellingRedUnderline;
    private SyntaxHighlighterBase _hl;
    private DraggableScrollbarScrollView _scrollView;
    private boolean _isDynamicHighlightingEnabled = true;
    private Runnable _hlDebounced;       // Debounced runnable which recomputes highlighting
    private boolean _hlEnabled;          // Whether highlighting is enabled
    private final Rect _oldHlRect;       // Rect highlighting was previously applied to
    private final Rect _oldVisRect;      // Rect highlighting was previously applied to
    private final Rect _visRect;         // Current rect
    private int _hlShiftThreshold = -1;  // How much to scroll before re-apply highlight
    private InputFilter _autoFormatFilter;
    private TextWatcher _autoFormatModifier;
    private boolean _autoFormatEnabled;
    private boolean _saveInstanceState = true;

    public HighlightingEditor(Context context, AttributeSet attrs) {
        super(context, attrs);
        final AppSettings as = ApplicationObject.settings();

        setAutoFormatters(null, null);

        _isSpellingRedUnderline = !as.isDisableSpellingRedUnderline();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            setFallbackLineSpacing(false);
        }

        _hlEnabled = false;
        _oldHlRect = new Rect();
        _oldVisRect = new Rect();
        _visRect = new Rect();

        addTextChangedListener(new GsTextWatcherAdapter() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (_hlEnabled && _hl != null) {
                    _hl.fixup(start, before, count);
                }
                _bringPointIntoView(getSelectionStart());
            }

            @Override
            public void afterTextChanged(final Editable s) {
                if (_hlEnabled && _hl != null && _hlDebounced != null) {
                    _hlDebounced.run();
                }
            }
        });

        // Listen to and update highlighting
        final ViewTreeObserver observer = getViewTreeObserver();
        observer.addOnScrollChangedListener(this::onRegionChangeListener);
        observer.addOnGlobalLayoutListener(this::onRegionChangeListener);

        // Fix for android 12 perf issues - https://github.com/gsantner/markor/discussions/1794
        setEmojiCompatEnabled(false);
    }

    //
    // ---------------------------------------------------------------------------------------------

    private boolean wasCursorPreviouslyVisible() {
        final int sel = getSelectionStart();
        final Layout layout = getLayout();
        if (_oldVisRect.isEmpty() || layout == null || !indexesValid(sel)) {
            return false;
        }
        final int line = layout.getLineForOffset(sel);
        final int Y = Math.round(0.5f * (layout.getLineTop(line) + layout.getLineBottom(line)));
        final int X = Math.round(layout.getPrimaryHorizontal(sel));
        return _oldVisRect.contains(X, Y);
    }

    private boolean isHeightChangeSignificant() {
        final int ha = _oldVisRect.height(), hb = _visRect.height();
        final float min = Math.min(ha, hb), max = Math.max(ha, hb);
        return max != 0 && ((max - min) / max) > 0.10;
    }

    private void onRegionChangeListener() {
        if (getLocalVisibleRect(_visRect) && !_visRect.equals(_oldVisRect)) {
            updateHighlighting(false);

            // Heuristic for sip shown etc
            if (isHeightChangeSignificant() && wasCursorPreviouslyVisible()) {
                _bringPointIntoView(getSelectionStart());
            }
        }

        _oldVisRect.set(_visRect);
    }

    // Highlighting
    // ---------------------------------------------------------------------------------------------

    private boolean isScrollSignificant() {
        return ( _oldHlRect.top - _visRect.top) > _hlShiftThreshold ||
                (_visRect.bottom -  _oldHlRect.bottom) > _hlShiftThreshold;
    }

    private void updateHighlighting(final boolean recompute) {
        final Layout layout;
        if (_hlEnabled && _hl != null && (layout = getLayout()) != null) {

            // Don't highlight unless shifted sufficiently or a recompute is required
            if (recompute || (_hl.hasSpans() && isScrollSignificant())) {

                // Addition of spans which require reflow can shift text on re-application of spans
                // we compute the resulting shift and scroll the view to compensate in order to make
                // the experience smooth for the user.
                final boolean heightSame =  Math.abs(_visRect.height() -  _oldHlRect.height()) <= 2;
                final int shiftTestLine = heightSame ? layout.getLineForVertical(_visRect.centerY()) : -1;
                final int oldOffset = heightSame ? layout.getLineBaseline(shiftTestLine) : 0;

                final int[] newHlRegion = hlRegion(_visRect); // Compute this _before_ clear
                try {
                    beginBatchEdit();
                    _hl.clear();
                    if (recompute) {
                        _hl.recompute();
                    }
                    _hl.apply(newHlRegion);
                } finally {
                    endBatchEdit();
                }

                if (_scrollView != null && shiftTestLine >= 0) {
                    final int shift = layout.getLineBaseline(shiftTestLine) - oldOffset;
                    _scrollView.slowScrollShift(shift);
                }

                 _oldHlRect.set(_visRect);
            }
        }
    }

    public void setDynamicHighlightingEnabled(final boolean enable) {
        _isDynamicHighlightingEnabled = enable;
    }

    public boolean isDynamicHighlightingEnabled() {
        return _isDynamicHighlightingEnabled;
    }

    public void setHighlighter(final SyntaxHighlighterBase newHighlighter) {
        if (_hl != null) {
            _hl.clear();
        }

        _hl = newHighlighter;

        if (_hl != null) {
            initHighlighter();
            _hlDebounced = TextViewUtils.makeDebounced(getHandler(), _hl.getHighlightingDelay(), () -> updateHighlighting(true));
            _hlDebounced.run();
        } else {
            _hlDebounced = null;
        }
    }

    public void initHighlighter() {
        final Paint paint = getPaint();
        _hlShiftThreshold = Math.round(paint.getTextSize() * HIGHLIGHT_SHIFT_LINES);
        if (_hl != null) {
            _hl.setSpannable(getText()).configure(paint);
        }
    }

    public SyntaxHighlighterBase getHighlighter() {
        return _hl;
    }

    public boolean getHighlightingEnabled() {
        return _hlEnabled;
    }

    public boolean setHighlightingEnabled(final boolean enable) {
        final boolean prev = _hlEnabled;
        if (enable && !_hlEnabled) {
            _hlEnabled = true;
            initHighlighter();
            if (_hlDebounced != null) {
                _hlDebounced.run();
            }
        } else if (!enable && _hlEnabled) {
            _hlEnabled = false;
            if (_hl != null) {
                _hl.clear();
            }
        }
        return prev;
    }

    // Region to highlight
    private int[] hlRegion(final Rect rect) {
        if (_isDynamicHighlightingEnabled) {
            final int hlSize = Math.round(HIGHLIGHT_REGION_SIZE * rect.height()) + _hlShiftThreshold;
            final int startY = rect.centerY() - hlSize;
            final int endY = rect.centerY() + hlSize;
            return new int[]{rowStart(startY), rowEnd(endY)};
        } else {
            return new int[]{0, length()};
        }
    }

    private int rowStart(final int y) {
        final Layout layout = getLayout();
        final int line = layout.getLineForVertical(y);
        return layout.getLineStart(line);
    }

    private int rowEnd(final int y) {
        final Layout layout = getLayout();
        final int line = layout.getLineForVertical(y);
        return layout.getLineEnd(line);
    }

    public void setScrollView(final DraggableScrollbarScrollView view) {
        _scrollView = view;
    }

    // Various overrides
    // ---------------------------------------------------------------------------------------------
    public void setSaveInstanceState(final boolean save) {
        _saveInstanceState = save;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        // Call is always required
        final Parcelable state = super.onSaveInstanceState();
        return _saveInstanceState ? state : null;
    }

    @Override
    public void beginBatchEdit() {
        _accessibilityEnabled = false;
        super.beginBatchEdit();
    }

    @Override
    public void endBatchEdit() {
        super.endBatchEdit();
        _accessibilityEnabled = true;
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (changedView == this && visibility == View.VISIBLE) {
            updateHighlighting(true);
        }
    }

    // We block bringPointIntoView as it is called by the system when not needed
    @Override
    public boolean bringPointIntoView(int cursor) {
        return false;
    }

    private boolean _bringPointIntoView(int cursor) {
        return super.bringPointIntoView(cursor);
    }

    @Override
    public void setTextSize(float size) {
        super.setTextSize(size);
        initHighlighter();
        if (_hlDebounced != null) {
            _hlDebounced.run();
        }
    }

    @Override
    public void setText(final CharSequence text, final BufferType type) {
        super.setText(text, type);
        initHighlighter();
        if (_hlDebounced != null) {
            _hlDebounced.run();
        }
    }

    @Override
    public boolean onTextContextMenuItem(int id) {
        // Copy-paste fix by bad richtext pasting - example text from code at https://plantuml.com/activity-diagram-beta
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && id == android.R.id.paste) {
            id = android.R.id.pasteAsPlainText;
        }
        return super.onTextContextMenuItem(id);
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
        try {
            _inSetSelection = true;
            if (indexesValid(start, stop)) {
                super.setSelection(start, stop);
            } else if (indexesValid(start, stop - 1)) {
                super.setSelection(start, stop - 1);
            } else if (indexesValid(start + 1, stop)) {
                super.setSelection(start + 1, stop);
            }
        } finally {
            _inSetSelection = false;
        }
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);

        if (MainActivity.IS_DEBUG_ENABLED) {
            AppSettings.appendDebugLog("Selection changed: " + selStart + "->" + selEnd);
        }

        if (!_inSetSelection) {
            // Bring appropriate piece into view
            if (indexesValid(_prevSelStart, selStart) && _prevSelStart != selStart) {
                // Start dragged
                _bringPointIntoView(selStart);
            } else if (indexesValid(_prevSelEnd, selEnd) && _prevSelEnd != selEnd) {
                // End dragged
                _bringPointIntoView(selEnd);
            }
        }
        _prevSelStart = selStart;
        _prevSelEnd = selEnd;
    }

    // Auto-format
    // ---------------------------------------------------------------------------------------------

    public void setAutoFormatters(final InputFilter inputFilter, final TextWatcher modifier) {
        final boolean state = getAutoFormatEnabled();
        setAutoFormatEnabled(false); // Remove any existing modifiers if applied
        _autoFormatFilter = inputFilter;
        _autoFormatModifier = modifier;
        setAutoFormatEnabled(state); // Restore state
    }

    public boolean getAutoFormatEnabled() {
        return _autoFormatEnabled;
    }

    public void setAutoFormatEnabled(final boolean enable) {
        if (enable && !_autoFormatEnabled) {
            if (_autoFormatFilter != null) {
                setFilters(new InputFilter[]{_autoFormatFilter});
            }
            if (_autoFormatModifier != null) {
                addTextChangedListener(_autoFormatModifier);
            }
        } else if (!enable && _autoFormatEnabled) {
            setFilters(new InputFilter[]{});
            if (_autoFormatModifier != null) {
                removeTextChangedListener(_autoFormatModifier);
            }
        }
        _autoFormatEnabled = enable;
    }

    // Run some code with accessibility disabled
    public void withAccessibilityDisabled(final GsCallback.a0 callback) {
        try {
            _accessibilityEnabled = false;
            callback.callback();
        } finally {
            _accessibilityEnabled = true;
        }
    }

    // Run some code with auto formatters disabled
    // Also disables accessibility
    public void withAutoFormatDisabled(final GsCallback.a0 callback) {
        if (getAutoFormatEnabled()) {
            try {
                setAutoFormatEnabled(false);
                withAccessibilityDisabled(callback);
            } finally {
                setAutoFormatEnabled(true);
            }
        } else {
            withAccessibilityDisabled(callback);
        }
    }

    // Utility functions for interaction
    // ---------------------------------------------------------------------------------------------

    public void simulateKeyPress(int keyEvent_KEYCODE_SOMETHING) {
        dispatchKeyEvent(new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, keyEvent_KEYCODE_SOMETHING, 0));
        dispatchKeyEvent(new KeyEvent(0, 0, KeyEvent.ACTION_UP, keyEvent_KEYCODE_SOMETHING, 0));
    }

    public void insertOrReplaceTextOnCursor(final String newText) {
        final Editable edit = getText();
        if (edit != null && newText != null) {
            final int newCursorPos = newText.indexOf(PLACE_CURSOR_HERE_TOKEN);
            final String finalText = newText.replace(PLACE_CURSOR_HERE_TOKEN, "");
            final int[] sel = TextViewUtils.getSelection(this);
            sel[0] = Math.max(sel[0], 0);
            withAutoFormatDisabled(() -> edit.replace(sel[0], sel[1], finalText));
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
        final int[] sel = TextViewUtils.getSelection(this);
        final CharSequence text = getText();
        setSelection(
                TextViewUtils.getLineStart(text, sel[0]),
                TextViewUtils.getLineEnd(text, sel[1])
        );
        return sel[0];
    }

    public boolean indexesValid(int... indexes) {
        return TextViewUtils.inRange(0, length(), indexes);
    }
}