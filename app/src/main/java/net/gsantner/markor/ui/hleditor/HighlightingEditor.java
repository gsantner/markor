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
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.os.Parcelable;
import android.support.annotation.RequiresApi;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Layout;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

import net.gsantner.markor.activity.MainActivity;
import net.gsantner.markor.ui.DraggableScrollbarScrollView;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.opoc.android.dummy.TextWatcherDummy;
import net.gsantner.opoc.util.Callback;
import net.gsantner.opoc.util.StringUtils;

@SuppressWarnings("UnusedReturnValue")
public class HighlightingEditor extends AppCompatEditText {

    final static int HIGHLIGHT_SHIFT_LINES = 3;              // Lines to scroll before hl updated
    final static float HIGHLIGHT_REGION_SIZE = 0.75f;        // Minimum extra screens to highlight (should be > 0.5 to cover screen)
    final static long BRING_CURSOR_INTO_VIEW_DELAY_MS = 250; // Block auto-scrolling for time after highlighing (hack)

    public final static String PLACE_CURSOR_HERE_TOKEN = "%%PLACE_CURSOR_HERE%%";

    private long _minPointIntoViewTime = 0;
    private boolean _accessibilityEnabled = true;
    private final boolean _isSpellingRedUnderline;
    private Highlighter _hl;
    private DraggableScrollbarScrollView _scrollView;
    private Runnable _hlDebounced;        // Debounced runnable which recomputes highlighting
    private boolean _hlEnabled;           // Whether highlighting is enabled
    private int _oldHlY = -1;             // Y coordinates highlighting is currently applied to
    private final Rect _localVisibleRect;
    private int _currentMiddleY = 0;      // The current view centre
    private int _height = 0;              // The visible view size
    private int _hlShiftThreshold = 0;    // How much scrolling before reapply dynamic highlighting
    private boolean _isUpdatingHighlighting = false;
    private InputFilter _autoFormatFilter;
    private TextWatcher _autoFormatModifier;
    private boolean _autoFormatEnabled;

    public HighlightingEditor(Context context, AttributeSet attrs) {

        super(context, attrs);
        final AppSettings as = new AppSettings(context);

        setAutoFormatters(null, null);

        _isSpellingRedUnderline = !as.isDisableSpellingRedUnderline();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            setFallbackLineSpacing(false);
        }

        _hlEnabled = false;
        _localVisibleRect = new Rect();
        _hlShiftThreshold = Math.round(getPaint().getTextSize() * HIGHLIGHT_SHIFT_LINES);

        addTextChangedListener(new TextWatcherDummy() {
            @Override
            public void onTextChanged (CharSequence s,int start, int before, int count){
                if (_hlEnabled && _hl != null && isDynamicHl()) {
                    withAccessibilityDisabled(() -> _hl.fixup(start, before, count));
                }
            }

            @Override
            public void afterTextChanged ( final Editable s){
                if (_hlEnabled && _hl != null && _hlDebounced != null) {
                    _hlDebounced.run();
                }
            }
        });

        getViewTreeObserver().addOnScrollChangedListener(this::onScrollChanged);

    }

    // Highlighting
    // ---------------------------------------------------------------------------------------------

    private boolean isRunHighlight() {
        return _hlEnabled && _hl != null && getLayout() != null;
    }

    // Re-apply highlighting on region change
    private void updateDynamicHighlighting() {
        if (!_isUpdatingHighlighting && isRunHighlight() && isDynamicHl() && isShiftSignificant()) {

            _isUpdatingHighlighting = true;
            withAccessibilityDisabled(() -> {
                // Addition of spans which require reflow can shift text on re-application of spans
                // we compute the resulting shift and scroll the view to compensate in order to make
                // the experience smooth for the user
                final int shiftTestIndex = rowStart(_currentMiddleY);
                final float oldOffset = _hl.offset(shiftTestIndex);

                // Hack to prevent scrolling to cursor. Time based blocking _may_ fail
                blockBringNextPointIntoView();
                final int[][] regions = hlRegions();
                _hl.clear().apply(regions[0]).reflow(regions[1]);

                final float newOffset = _hl.offset(shiftTestIndex);
                final int shift = Math.round(newOffset - oldOffset);
                if (Math.abs(shift) > 0 && _scrollView != null) {
                    _scrollView.slowScrollShift(shift);
                }

            });
            _isUpdatingHighlighting = false;
            _oldHlY = _currentMiddleY;
        }
    }

    // Recompute and re-apply highlighting
    public void reHighlight() {
        if (isRunHighlight()) {
            updateVisibleRect(); // Just in case
            withAccessibilityDisabled(() -> {
                final int[][] regions = hlRegions();
                blockBringNextPointIntoView();
                _hl.clear().recompute().apply(regions[0]).reflow(regions[1]);
            });
        }
    }

    public void setScrollView(final DraggableScrollbarScrollView view) {
        _scrollView = view;
    }

    public void setHighlighter(final Highlighter newHighlighter) {
        if (_hl != null) {
            _hl.clear();
        }

        _hl = newHighlighter;

        if (_hl != null) {
            initHighlighter();
            _hlDebounced = StringUtils.makeDebounced(_hl.getHighlightingDelay(), this::reHighlight);
            post(this::reHighlight); // Post ensures that this happens after layout
        }
    }

    public void initHighlighter() {
        if (_hl != null) {
            _hl.setSpannable(getText()).configure(getPaint());
        }
        _oldHlY = -1;
    }

    public Highlighter getHighlighter() {
        return _hl;
    }

    public boolean setHighlightingEnabled(final boolean enable) {
        final boolean prev = _hlEnabled;
        if (enable && !_hlEnabled) {
            _hlEnabled = true;
            initHighlighter();
            reHighlight();
        } else if (!enable && _hlEnabled) {
            _hlEnabled = false;
            if (_hl != null) {
                _hl.clear();
            }
        }
        return prev;
    }

    // Region to highlight and region to reflow
    private int[][] hlRegions() {
        final int[] hlRegion, reflowRegion;
        if (isDynamicHl()) {
            final int[] regionY = hlRegionY();
            hlRegion = new int[] { rowStart(regionY[0]), rowEnd(regionY[1]) };
            // We reflow an expanded region to minimize tearing
            reflowRegion = new int[] { rowStart(regionY[0] - _height), rowEnd(regionY[1] + _height) };
        } else {
            hlRegion = new int[] { 0, length() };
            reflowRegion = null; // No need to reflow
        }
        return new int[][] { hlRegion, reflowRegion };
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

    // Region to highlight in y coordinate
    private int[] hlRegionY() {
        final int offset = Math.round(_height * HIGHLIGHT_REGION_SIZE) + _hlShiftThreshold;
        final int startY = _currentMiddleY - offset;
        final int endY = _localVisibleRect.bottom + offset;
        return new int[] { startY, endY };
    }

    private boolean isShiftSignificant() {
        // 2 lines of text prevents jumping
        return _oldHlY < 0 || Math.abs(_currentMiddleY - _oldHlY) > _hlShiftThreshold;
    }

    private boolean isDynamicHl() {
        return length() > HIGHLIGHT_SMALL_FILE_THRESHOLD;
    }

    private void blockBringNextPointIntoView() {
        _minPointIntoViewTime = System.currentTimeMillis() + BRING_CURSOR_INTO_VIEW_DELAY_MS;
    }

    private boolean bringPointIntoViewAllowed() {
        return System.currentTimeMillis() >= _minPointIntoViewTime;
    }

    // Various overrides
    // ---------------------------------------------------------------------------------------------

    public void onPause() {
        // Nothing here
    }

    public void onResume() {
        updateVisibleRect();
        reHighlight();
    }

    protected void updateVisibleRect() {
        getLocalVisibleRect(_localVisibleRect);
        // We ensure that we use the largest height seen so far
        _height = Math.max(_height, _localVisibleRect.height());
        _currentMiddleY = (_localVisibleRect.top + _localVisibleRect.bottom) / 2;
    }

    protected void onScrollChanged() {
        updateVisibleRect();
        updateDynamicHighlighting();
    }

    @Override
    public Parcelable onSaveInstanceState() {
        // Make sure spans cleared before save
        if (_hl != null && _hlEnabled) {
            _hl.clear();
        }
        return super.onSaveInstanceState();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    // Hack to prevent auto-scroll
    @Override
    public boolean bringPointIntoView(int cursor) {
        if (bringPointIntoViewAllowed()) {
            return super.bringPointIntoView(cursor);
        } else {
            return false;
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateVisibleRect();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateVisibleRect();
        updateDynamicHighlighting();
    }

    @Override
    public void setTextSize(float size) {
        super.setTextSize(size);
        _hlShiftThreshold = Math.round(getPaint().getTextSize() * HIGHLIGHT_SHIFT_LINES);
        initHighlighter();
        reHighlight();
    }

    @Override
    public void setText(final CharSequence text, final BufferType type) {
        super.setText(text, type);
        initHighlighter();
        reHighlight();
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
        } else if (!enable && _autoFormatEnabled){
            setFilters(new InputFilter[]{});
            if (_autoFormatModifier != null) {
                removeTextChangedListener(_autoFormatModifier);
            }
        }
        _autoFormatEnabled = enable;
    }

    // Run some code with accessibility disabled
    public void withAccessibilityDisabled(final Callback.a0 callback) {
        try {
            _accessibilityEnabled = false;
            callback.callback();
        } finally {
            _accessibilityEnabled = true;
        }
    }

    // Run some code with auto formatters disabled
    // Also disables accessibility
    public void withAutoFormatDisabled(final Callback.a0 callback) {
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
            int newCursorPos = newText.indexOf(PLACE_CURSOR_HERE_TOKEN);
            final String finalText = newText.replace(PLACE_CURSOR_HERE_TOKEN, "");
            final int[] sel = StringUtils.getSelection(this);
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
        final int[] sel = StringUtils.getSelection(this);
        final CharSequence text = getText();
        setSelection(
                StringUtils.getLineStart(text, sel[0]),
                StringUtils.getLineEnd(text, sel[1])
        );
        return sel[0];
    }

    public boolean indexesValid(int... indexes) {
        return StringUtils.inRange(0, length(), indexes);
    }
}