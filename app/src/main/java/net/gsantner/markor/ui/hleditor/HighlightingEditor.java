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
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

import net.gsantner.markor.activity.MainActivity;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.opoc.android.dummy.TextWatcherDummy;
import net.gsantner.opoc.util.Callback;
import net.gsantner.opoc.util.StringUtils;

@SuppressWarnings("UnusedReturnValue")
public class HighlightingEditor extends AppCompatEditText {

    final static float HIGHLIGHT_REFRESH_SLOP_FACTOR = 10.0f; // Extra above and below to highlight
    final static int HIGHLIGHT_SMALL_FILE_THRESHOLD = 25000; // Dynamic highlighting disabled for smaller files
    final static long BRING_CURSOR_INTO_VIEW_DELAY_MS = 200; // Block auto-scrolling for time after highlighing (hack)

    public final static String PLACE_CURSOR_HERE_TOKEN = "%%PLACE_CURSOR_HERE%%";

    private long _minPointIntoViewTime = 0;
    private boolean _accessibilityEnabled = true;
    private final boolean _isSpellingRedUnderline;
    private Highlighter _hl;
    private Runnable _hlDebounced;  /* Debounced runnable which recomputes highlighting */
    private boolean _hlEnabled; /* Whether highlighting is enabled */
    private int[] _oldHlRegionY; /* Region (indices) highlihging is currently applied to */
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

        addTextChangedListener(new TextWatcherDummy() {
            @Override
            public void onTextChanged (CharSequence s,int start, int before, int count){
                if (_hlEnabled && _hl != null && isDynamicHlEnabled()) {
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

        getViewTreeObserver().addOnScrollChangedListener(this::updateDynamicHighlighting);
    }

    // Highlighting
    // ---------------------------------------------------------------------------------------------

    private boolean isRunHighlight() {
        return _hlEnabled && _hl != null && getLayout() != null;
    }

    // Re-apply highlighting on region change
    private void updateDynamicHighlighting() {
        if (isRunHighlight() && isDynamicHlEnabled()) {
            final int[] newRegionY = hlRegionY();
            if (isShiftSignificant(newRegionY)) {
                final long start = System.nanoTime();
                final int[] hlIndices = hlRegionIndex(newRegionY);
                blockBringNextPointIntoView(); // Hack to prevent scrolling to cursor
                withAccessibilityDisabled(() -> _hl.clear().apply(hlIndices));
                _oldHlRegionY = newRegionY;
                Log.d("Highlighting", "" + (0.000001 * (System.nanoTime() - start)) + " mS");
            }
        }
    }

    // Recompute and re-apply highlighting
    public void highlight() {
        if (isRunHighlight()) {
            blockBringNextPointIntoView();
            withAccessibilityDisabled(() -> _hl.clear().recompute().apply(hlRegion()));
        }
    }

    public void setHighlighter(final Highlighter newHighlighter) {
        if (_hl != null) {
            _hl.clear();
        }

        _hl = newHighlighter;

        if (_hl != null) {

            _hl.setSpannable(getText()).configure(getPaint());

            _hlDebounced = StringUtils.makeDebounced(_hl.getHighlightingDelay(), this::highlight);

            if (_hlEnabled) {
                fadeInHighlight();
            }
        }
    }

    public void fadeInHighlight() {
        post(() -> {
            if (isRunHighlight()) {
                setAlpha(0.3f);
                highlight();
                animate().alpha(1.0f).setDuration(1000).start();
            }
        });
    }

    public Highlighter getHighlighter() {
        return _hl;
    }

    public void setHighlightingEnabled(final boolean enable) {
        if (enable && !_hlEnabled) {
            _hlEnabled = true;
            fadeInHighlight();
        } else if (!enable && _hlEnabled) {
            _hlEnabled = false;
            if (_hl != null) {
                _hl.clear();
            }
        }
    }

    // Region to highlight in text index
    private int[] hlRegion() {
        // If no dynamic highlighitng, highlight whole file
        return isDynamicHlEnabled() ? hlRegionIndex(hlRegionY()) : new int[] { 0, -1 };
    }

    // Region to highlight in text index
    private int[] hlRegionIndex(final int[] regionY) {
        final Layout layout = getLayout();

        final int startL = layout.getLineForVertical(regionY[0]);
        final int endL = layout.getLineForVertical(regionY[1]);

        final int start = layout.getLineStart(startL);
        final int end = layout.getLineEnd(endL);

        return new int[] { start, end };
    }

    private int hlSlop() {
        return Math.round(getPaint().getTextSize() * HIGHLIGHT_REFRESH_SLOP_FACTOR);
    }

    // Region to highlight in y coordinate
    private int[] hlRegionY() {
        final Rect rect = new Rect();
        getLocalVisibleRect(rect);

        final int offset = hlSlop();
        final int startY = rect.top - offset;
        final int endY = rect.bottom + offset;

        return new int[] { startY, endY };
    }

    private boolean isShiftSignificant(final int[] region) {
        // No old hl region or shift > text size
        final int threshold = hlSlop() / 2;
        return _oldHlRegionY == null || Math.max(Math.abs(_oldHlRegionY[0] - region[0]), Math.abs(region[1] - _oldHlRegionY[1])) > threshold;
    }

    private boolean isDynamicHlEnabled() {
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
        if (isRunHighlight()) {
            withAccessibilityDisabled(() -> _hl.clear().apply(hlRegion()));
        }
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
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateDynamicHighlighting();
    }

    @Override
    public void setTextSize(float size) {
        super.setTextSize(size);
        if (_hl != null) {
            _hl.configure(getPaint());
        }
    }

    @Override
    public void setText(final CharSequence text, final BufferType type) {
        super.setText(text, type);
        if (_hl != null) {
            _hl.setSpannable(getText());
            if (_hlEnabled) {
                fadeInHighlight();
            }
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
