/*#######################################################
 *
 *   Maintained 2017-2023 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.frontend.textview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
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
import net.gsantner.markor.model.AppSettings;
import net.gsantner.opoc.wrapper.GsCallback;
import net.gsantner.opoc.wrapper.GsTextWatcherAdapter;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("UnusedReturnValue")
public class HighlightingEditor extends AppCompatEditText {

    final static int HIGHLIGHT_SHIFT_LINES = 8;              // Lines to scroll before hl updated
    final static float HIGHLIGHT_REGION_SIZE = 0.75f;        // Minimum extra screens to highlight (should be > 0.5 to cover screen)

    public final static String PLACE_CURSOR_HERE_TOKEN = "%%PLACE_CURSOR_HERE%%";
    public final static String INSERT_SELECTION_HERE_TOKEN = "%%INSERT_SELECTION_HERE%%";

    private boolean _accessibilityEnabled = true;
    private final boolean _isSpellingRedUnderline;
    private SyntaxHighlighterBase _hl;
    private boolean _isDynamicHighlightingEnabled = true;
    private Runnable _hlDebounced;        // Debounced runnable which recomputes highlighting
    private boolean _hlEnabled;           // Whether highlighting is enabled
    private boolean _numEnabled;          // Whether show line numbers is enabled
    private final Rect _oldHlRect;        // Rect highlighting was previously applied to
    private final Rect _hlRect;           // Current rect
    private int _hlShiftThreshold = -1;   // How much to scroll before re-apply highlight
    private InputFilter _autoFormatFilter;
    private TextWatcher _autoFormatModifier;
    private boolean _autoFormatEnabled;
    private boolean _saveInstanceState = true;
    private final LineNumbersDrawer _lineNumbersDrawer = new LineNumbersDrawer(this);


    public HighlightingEditor(Context context, AttributeSet attrs) {
        super(context, attrs);
        final AppSettings as = ApplicationObject.settings();

        setAutoFormatters(null, null);

        _isSpellingRedUnderline = !as.isDisableSpellingRedUnderline();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            setFallbackLineSpacing(false);
        }

        _hlEnabled = false;
        _numEnabled = false;
        _oldHlRect = new Rect();
        _hlRect = new Rect();

        addTextChangedListener(new GsTextWatcherAdapter() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (_hlEnabled && _hl != null) {
                    _hl.fixup(start, before, count);
                }
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
        observer.addOnScrollChangedListener(() -> updateHighlighting(false));
        observer.addOnGlobalLayoutListener(() -> updateHighlighting(false));

        // Fix for Android 12 perf issues - https://github.com/gsantner/markor/discussions/1794
        setEmojiCompatEnabled(false);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        _lineNumbersDrawer.setDefaultPaddingLeft(getPaddingLeft());
    }

    @Override
    public boolean onPreDraw() {
        _lineNumbersDrawer.setTextSize(getTextSize());
        return super.onPreDraw();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (_numEnabled) {
            _lineNumbersDrawer.draw(canvas);
        } else {
            _lineNumbersDrawer.suspend();
        }
    }

    // Highlighting
    // ---------------------------------------------------------------------------------------------

    private boolean isScrollSignificant() {
        return (_oldHlRect.top - _hlRect.top) > _hlShiftThreshold ||
                (_hlRect.bottom - _oldHlRect.bottom) > _hlShiftThreshold;
    }

    private void updateHighlighting(final boolean recompute) {
        if (_hlEnabled && _hl != null && getLayout() != null) {

            final boolean visible = getLocalVisibleRect(_hlRect);

            // Don't highlight unless shifted sufficiently or a recompute is required
            if (recompute || (visible && _hl.hasSpans() && isScrollSignificant())) {
                _oldHlRect.set(_hlRect);

                final int[] newHlRegion = hlRegion(_hlRect); // Compute this _before_ clear
                _hl.clearDynamic();
                if (recompute) {
                    _hl.clearStatic().recompute().applyStatic();
                }
                _hl.applyDynamic(newHlRegion);
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
            _hl.clearAll();
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
                _hl.clearAll();
            }
        }
        return prev;
    }

    public boolean getLineNumbersEnabled() {
        return _numEnabled;
    }

    public void setLineNumbersEnabled(final boolean enable) {
        if (enable ^ _numEnabled) {
            post(this::invalidate);
        }
        _numEnabled = enable;
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

    @Override
    public boolean bringPointIntoView(int i) {
        return super.bringPointIntoView(i);
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
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (changedView == this && visibility == View.VISIBLE) {
            updateHighlighting(true);
        }
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

    // Run some code with auto formatters and accessibility disabled
    public void withAutoFormatDisabled(final GsCallback.a0 callback) {
        final boolean enabled = getAutoFormatEnabled();
        try {
            _accessibilityEnabled = false;
            if (enabled) {
                setAutoFormatEnabled(false);
            }
            callback.callback();
        } finally {
            setAutoFormatEnabled(enabled);
            _accessibilityEnabled = true;
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

            // TODO - should consider moving any snippet specific logic out of here
            // Fill in any instances of selection
            final int[] sel = TextViewUtils.getSelection(this);
            final CharSequence selected = TextViewUtils.toString(edit, sel[0], sel[1]);
            String expanded = newText.replace(INSERT_SELECTION_HERE_TOKEN, selected);

            // Determine where to place the cursor
            final int newCursorPos = expanded.indexOf(PLACE_CURSOR_HERE_TOKEN);
            final String finalText = expanded.replace(PLACE_CURSOR_HERE_TOKEN, "");

            sel[0] = Math.max(sel[0], 0);

            // Needed to prevent selection of whole of inserted text after replace
            // if we want a cursor position instead
            if (newCursorPos >= 0) {
                setSelection(sel[0]);
            }

            withAutoFormatDisabled(() -> edit.replace(sel[0], sel[1], finalText));

            if (newCursorPos >= 0) {
                setSelection(sel[0] + newCursorPos);
                TextViewUtils.showSelection(this);
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

    static class LineNumbersDrawer {

        private final AppCompatEditText _editor;
        private final Paint _paint = new Paint();

        private int _defaultPaddingLeft;
        private static final int LINE_NUMBERS_PADDING_LEFT = 14;
        private static final int LINE_NUMBERS_PADDING_RIGHT = 10;

        private final Rect _visibleRect = new Rect();
        private final Rect _lastVisibleRect = new Rect();

        private int _startNumber = 1;
        private int _maxNumber = 1; // to gauge gutter width
        private int _maxNumberDigits;
        private int _lastMaxNumberDigits;
        private int _lastLayoutLineCount;
        private float _lastTextSize;
        private int _numberX;
        private int _gutterLineX;
        private final List<Integer> _numberYPositions = new ArrayList<>(); // Y positions of numbers to draw

        // Current visible area
        private int _top;
        private int _bottom;


        public LineNumbersDrawer(AppCompatEditText editor) {
            _editor = editor;
            _editor.getLocalVisibleRect(_lastVisibleRect);
            _paint.setTextAlign(Paint.Align.RIGHT);
        }

        public void setDefaultPaddingLeft(int padding) {
            this._defaultPaddingLeft = padding;
        }

        public void setTextSize(float textSize) {
            if (this._lastTextSize != _paint.getTextSize()) {
                this._lastTextSize = _paint.getTextSize();
            }
            _paint.setTextSize(textSize);
        }

        public int getNumberDigits(int number) {
            if (_lastMaxNumberDigits != _maxNumberDigits) {
                _lastMaxNumberDigits = _maxNumberDigits;
            }

            if (number < 10) {
                _maxNumberDigits = 1;
            } else if (number < 100) {
                _maxNumberDigits = 2;
            } else if (number < 1000) {
                _maxNumberDigits = 3;
            } else if (number < 10000) {
                _maxNumberDigits = 4;
            } else {
                _maxNumberDigits = 5;
            }
            return _maxNumberDigits;
        }

        public boolean isVisibleAreaChanged() {
            if (_visibleRect.top == _lastVisibleRect.top && _visibleRect.bottom == _lastVisibleRect.bottom) {
                return false;
            } else {
                _lastVisibleRect.top = _visibleRect.top;
                _lastVisibleRect.bottom = _visibleRect.bottom;
                return true;
            }
        }

        /**
         * Draw line numbers.
         *
         * @param canvas the canvas on which the line numbers will be drawn.
         */
        public void draw(final Canvas canvas) {
            if (!_editor.getLocalVisibleRect(_visibleRect)) {
                return;
            }

            final CharSequence text = _editor.getText();
            final Layout layout = _editor.getLayout();
            if (text == null || layout == null) {
                return;
            }

            final int offsetY = _editor.getPaddingTop();
            final int layoutLineCount = layout.getLineCount();

            // Only if the visible area or the layout line count changed
            // We make a single pass through the text to determine
            // 1. y positions and line numbers we want to draw
            // 2. max line number (to gauge gutter width)
            if (isVisibleAreaChanged() || layoutLineCount != _lastLayoutLineCount) {
                _lastLayoutLineCount = layoutLineCount;
                _maxNumber = 1;
                _startNumber = 1;
                _numberYPositions.clear();
                _top = _visibleRect.top - _visibleRect.height();
                _bottom = _visibleRect.bottom + _visibleRect.height();

                for (int i = 0; i < layoutLineCount; i++) {
                    final int start = layout.getLineStart(i);
                    if (start == 0 || text.charAt(start - 1) == '\n') {
                        final int y = layout.getLineBaseline(i);
                        if (y < _top) {
                            _startNumber++;
                        } else if (y < _bottom) {
                            _numberYPositions.add(y);
                        }
                        _maxNumber++;
                    }
                }
            }

            // Only if the text size or the max line number of digits changed
            // Update the gutter parameters and set padding left
            if (_paint.getTextSize() != _lastTextSize || getNumberDigits(_maxNumber) != _lastMaxNumberDigits) {
                final int width = Math.round(_paint.measureText(String.valueOf(_maxNumber - 1)));
                _numberX = LINE_NUMBERS_PADDING_LEFT + width;
                _gutterLineX = _numberX + LINE_NUMBERS_PADDING_RIGHT;
                _editor.setPadding(_gutterLineX + 10, _editor.getPaddingTop(), _editor.getPaddingRight(), _editor.getPaddingBottom());
            }

            // Draw the gutter line
            _paint.setColor(Color.LTGRAY);
            canvas.drawLine(_gutterLineX, _top, _gutterLineX, _bottom, _paint);

            // Draw the line numbers
            _paint.setColor(Color.GRAY);
            for (int i = 0; i < _numberYPositions.size(); i++) {
                final String number = String.valueOf(_startNumber + i);
                canvas.drawText(number, _numberX, _numberYPositions.get(i) + offsetY, _paint);
            }
        }

        /**
         * Suspend drawing the line numbers.
         */
        public void suspend() {
            if (_editor.getPaddingLeft() != _defaultPaddingLeft) {
                _maxNumberDigits = 0;
                // Reset padding without line numbers fence
                _editor.setPadding(_defaultPaddingLeft, _editor.getPaddingTop(), _editor.getPaddingRight(), _editor.getPaddingBottom());
            }
        }
    }
}