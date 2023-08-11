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
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.AppCompatEditText;

import net.gsantner.markor.ApplicationObject;
import net.gsantner.markor.activity.MainActivity;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.opoc.wrapper.GsCallback;
import net.gsantner.opoc.wrapper.GsTextWatcherAdapter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("UnusedReturnValue")
public class HighlightingEditor extends AppCompatEditText {

    final static int HIGHLIGHT_SHIFT_LINES = 8;              // Lines to scroll before hl updated
    final static float HIGHLIGHT_REGION_SIZE = 0.75f;        // Minimum extra screens to highlight (should be > 0.5 to cover screen)

    public final static String PLACE_CURSOR_HERE_TOKEN = "%%PLACE_CURSOR_HERE%%";

    private boolean _accessibilityEnabled = true;
    private final boolean _isSpellingRedUnderline;
    private SyntaxHighlighterBase _hl;
    private boolean _isDynamicHighlightingEnabled = true;
    private Runnable _hlDebounced;        // Debounced runnable which recomputes highlighting
    private boolean _hlEnabled;           // Whether highlighting is enabled
    private boolean _nuEnabled;           // Whether show line numbers is enabled
    private final Rect _oldHlRect;        // Rect highlighting was previously applied to
    private final Rect _hlRect;           // Current rect
    private int _hlShiftThreshold = -1;   // How much to scroll before re-apply highlight
    private InputFilter _autoFormatFilter;
    private TextWatcher _autoFormatModifier;
    private boolean _autoFormatEnabled;
    private boolean _saveInstanceState = true;

    // For drawing line numbers
    private final Paint _paint = new Paint();
    private ScrollView _scrollView;
    private int _x;
    private int _maxLineNumber = 1;
    private int _maxLineNumberWidth;
    private int _defaultPaddingLeft;
    private static final int LINE_NUMBERS_PADDING_LEFT = 14;
    private static final int LINE_NUMBERS_PADDING_RIGHT = 10;
    private final int[] _firstVisibleLine = {-1, 0}; // {line index, actual line number}


    public HighlightingEditor(Context context, AttributeSet attrs) {
        super(context, attrs);
        final AppSettings as = ApplicationObject.settings();

        setAutoFormatters(null, null);

        _isSpellingRedUnderline = !as.isDisableSpellingRedUnderline();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            setFallbackLineSpacing(false);
        }

        _hlEnabled = false;
        _nuEnabled = false;
        _oldHlRect = new Rect();
        _hlRect = new Rect();

        addTextChangedListener(new GsTextWatcherAdapter() {
            private final Pattern pattern = Pattern.compile("\n");
            private Matcher matcher;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (after == 0 && count > 0) {
                    CharSequence deleted = s.subSequence(start, start + count);
                    matcher = pattern.matcher(deleted);
                    while (matcher.find()) {
                        _maxLineNumber--;
                    }
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (_hlEnabled && _hl != null) {
                    _hl.fixup(start, before, count);
                }

                if (before == 0 && count > 0) {
                    CharSequence added = s.subSequence(start, start + count);
                    matcher = pattern.matcher(added);
                    while (matcher.find()) {
                        _maxLineNumber++;
                    }
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

        // Fix for android 12 perf issues - https://github.com/gsantner/markor/discussions/1794
        setEmojiCompatEnabled(false);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        _defaultPaddingLeft = getPaddingLeft();
        _paint.setTextAlign(Paint.Align.RIGHT);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        _scrollView = getParent() instanceof ScrollView ? (ScrollView) getParent() : (ScrollView) getParent().getParent();
        _scrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (_firstVisibleLine[0] > -1) {
                _firstVisibleLine[0] = -1;
            }
        });
    }

    @Override
    public boolean onPreDraw() {
        _paint.setTextSize(getTextSize());
        return super.onPreDraw();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // If line numbers can be drawn
        if (_nuEnabled && _maxLineNumber < (AppSettings._isDeviceGoodHardware ? 5000 : 3000)) {
            drawLineNumbers(canvas);
        } else if (getPaddingLeft() != _defaultPaddingLeft) {
            _maxLineNumberWidth = 0;
            // Reset padding without line numbers fence
            setPadding(_defaultPaddingLeft, getPaddingTop(), getPaddingRight(), getPaddingBottom());
        }
    }

    private void drawLineNumbers(Canvas canvas) {
        final Editable text = getText();
        final Layout layout = getLayout();
        final float offsetY = getPaddingTop();
        final int top = _scrollView.getScrollY() - 100; // Top of current visible area
        final int bottom = _scrollView.getScrollY() + _scrollView.getHeight(); // Bottom of current visible area
        final int width = (int) _paint.measureText(String.valueOf(_maxLineNumber));
        if (_maxLineNumberWidth != width) {
            _maxLineNumberWidth = width;
            _x = LINE_NUMBERS_PADDING_LEFT + width;
            setPadding(_x + LINE_NUMBERS_PADDING_RIGHT + 10, getPaddingTop(), getPaddingRight(), getPaddingBottom());
        }

        // Draw the vertical line
        _paint.setColor(Color.LTGRAY);
        canvas.drawLine(_x + LINE_NUMBERS_PADDING_RIGHT, top, _x + LINE_NUMBERS_PADDING_RIGHT, bottom, _paint);

        // Draw line numbers
        _paint.setColor(Color.GRAY);
        canvas.drawText("1", _x, layout.getLineBounds(0, null) + offsetY, _paint);

        if (text == null || text.length() == 0) {
            return;
        }

        final int count = getLineCount();
        int i = 1, number = 1;

        if (_firstVisibleLine[0] > -1) {
            // Set and then iterate from the first visible line
            i = _firstVisibleLine[0];
            number = _firstVisibleLine[1];
        } else {
            // Set the first visible line invalid, it needs to be updated
            _firstVisibleLine[0] = -1;
        }

        for (int y; i < count; i++) {
            if (text.charAt(layout.getLineStart(i) - 1) == '\n') {
                number++;
                y = layout.getLineBounds(i, null);
                if (y > bottom) {
                    break;
                }
                if (y > top) {
                    if (_firstVisibleLine[0] < 0) {
                        // Update the first visible line
                        _firstVisibleLine[0] = i;
                        _firstVisibleLine[1] = number - 1;
                    }
                    canvas.drawText(String.valueOf(number), _x, y + offsetY, _paint);
                }
            }
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
        return _nuEnabled;
    }

    public boolean setLineNumbersEnabled(final boolean enable) {
        final boolean prev = _nuEnabled;

        if (enable != _nuEnabled) {
            _nuEnabled = enable;
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