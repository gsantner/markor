/*#######################################################
 *
 *   Maintained 2017-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.frontend.textview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.os.Parcelable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Layout;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.AppCompatEditText;

import net.gsantner.markor.ApplicationObject;
import net.gsantner.markor.R;
import net.gsantner.markor.activity.MainActivity;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.markor.util.TextCasingUtils;
import net.gsantner.opoc.format.GsTextUtils;
import net.gsantner.opoc.wrapper.GsCallback;
import net.gsantner.opoc.wrapper.GsTextWatcherAdapter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private final Rect _oldHlRect;        // Rect highlighting was previously applied to
    private final Rect _hlRect;           // Current rect
    private int _hlShiftThreshold = -1;   // How much to scroll before re-apply highlight
    private InputFilter _autoFormatFilter;
    private TextWatcher _autoFormatModifier;
    private boolean _autoFormatEnabled;
    private boolean _saveInstanceState = true;
    private final ExecutorService executor = new ThreadPoolExecutor(0, 3, 60, TimeUnit.SECONDS, new SynchronousQueue<>());
    private final AtomicBoolean _textUnchangedWhileHighlighting = new AtomicBoolean(true);

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
        _hlRect = new Rect();

        addTextChangedListener(new GsTextWatcherAdapter() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (_hlEnabled && _hl != null) {
                    _textUnchangedWhileHighlighting.set(false);
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
        observer.addOnScrollChangedListener(this::updateHighlighting);
        observer.addOnGlobalLayoutListener(this::updateHighlighting);

        // Fix for Android 12 perf issues - https://github.com/gsantner/markor/discussions/1794
        setEmojiCompatEnabled(false);

        // Custom options
        setupCustomOptions();
    }

    @Override
    public boolean onPreDraw() {
        try {
            return super.onPreDraw();
        } catch (OutOfMemoryError ignored) {
            return false; // return false to cancel current drawing pass/round
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        try {
            super.onDraw(canvas);
        } catch (Exception e) {
            // Hinder drawing from crashing the app
            Log.e(getClass().getName(), "HighlightingEdtior onDraw->super.onDraw crash" + e);
            Toast.makeText(getContext(), e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    // Highlighting
    // ---------------------------------------------------------------------------------------------

    // Batch edit spans (or anything else, really)
    // This triggers a reflow which will bring focus back to the cursor.
    // Therefore it cannot be used for updating the highlighting as one scrolls
    private void batch(final Runnable runnable) {
        try {
            beginBatchEdit();
            runnable.run();
        } finally {
            endBatchEdit();
        }
    }

    private boolean isScrollSignificant() {
        return Math.abs(_oldHlRect.top - _hlRect.top) > _hlShiftThreshold ||
                Math.abs(_hlRect.bottom - _oldHlRect.bottom) > _hlShiftThreshold;
    }

    // The order of tests here is important
    // - we want to run getLocalVisibleRect even if recompute is true
    // - we want to run isScrollSignificant after getLocalVisibleRect
    // - We don't care about the presence of spans or scroll significance if recompute is true
    private boolean runHighlight(final boolean recompute) {
        return _hlEnabled && _hl != null && getLayout() != null &&
                (getLocalVisibleRect(_hlRect) || recompute) &&
                (recompute || _hl.hasSpans()) &&
                (recompute || isScrollSignificant());
    }

    private void updateHighlighting() {
        if (runHighlight(false)) {
            // Do not batch as we do not want to reflow
            _hl.clearDynamic().applyDynamic(hlRegion());
            _oldHlRect.set(_hlRect);
        }
    }

    public void recomputeHighlighting() {
        if (runHighlight(true)) {
            batch(() -> _hl.clearDynamic().clearStatic(false).recompute().applyStatic().applyDynamic(hlRegion()));
        }
    }

    /**
     * Computing the highlighting spans for a lot of text can be slow so we do it async
     * 1. We set a flag to check that the text did not change when we were computing
     * 2. We trigger the computation to a buffer
     * 3. If the text did not change during computation, we apply the highlighting
     */
    private void recomputeHighlightingAsync() {
        if (runHighlight(true)) {
            try {
                executor.execute(this::_recomputeHighlightingWorker);
            } catch (RejectedExecutionException ignored) {
            }
        }
    }

    private synchronized void _recomputeHighlightingWorker() {
        _textUnchangedWhileHighlighting.set(true);
        _hl.compute();
        post(() -> {
            if (_textUnchangedWhileHighlighting.get()) {
                batch(() -> _hl.clearStatic(false).clearDynamic().setComputed().applyStatic().applyDynamic(hlRegion()));
            }
        });
    }

    public void setDynamicHighlightingEnabled(final boolean enable) {
        _isDynamicHighlightingEnabled = enable;
        recomputeHighlighting();
    }

    public boolean isDynamicHighlightingEnabled() {
        return _isDynamicHighlightingEnabled;
    }

    public void setHighlighter(final SyntaxHighlighterBase newHighlighter) {
        if (_hl != null) {
            _hl.clearDynamic().clearStatic(true);
        }

        _hl = newHighlighter;

        if (_hl != null) {
            initHighlighter();
            _hlDebounced = TextViewUtils.makeDebounced(getHandler(), _hl.getHighlightingDelay(), this::recomputeHighlightingAsync);
            recomputeHighlighting();
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
                _hl.clearDynamic().clearStatic(true);
            }
        }
        return prev;
    }

    // Region to highlight
    private int[] hlRegion() {
        if (_isDynamicHighlightingEnabled) {
            final int hlSize = Math.round(HIGHLIGHT_REGION_SIZE * _hlRect.height()) + _hlShiftThreshold;
            final int startY = _hlRect.centerY() - hlSize;
            final int endY = _hlRect.centerY() + hlSize;
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
        return layout == null ? 0 : layout.getLineStart(layout.getLineForVertical(y));
    }

    private int rowEnd(final int y) {
        final Layout layout = getLayout();
        return layout == null ? 0 : layout.getLineEnd(layout.getLineForVertical(y));
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
            recomputeHighlighting();
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
        try {
            // i.e. DeadSystemRuntimeException can happen here
            return super.onTextContextMenuItem(id);
        } catch (Exception ignored) {
            return true;
        }
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

    public void selectLines() {
        final int[] sel = TextViewUtils.getLineSelection(this);
        setSelection(sel[0], sel[1]);
    }

    public void simulateKeyPress(int keyEvent_KEYCODE_SOMETHING) {
        dispatchKeyEvent(new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, keyEvent_KEYCODE_SOMETHING, 0));
        dispatchKeyEvent(new KeyEvent(0, 0, KeyEvent.ACTION_UP, keyEvent_KEYCODE_SOMETHING, 0));
    }

    public void insertOrReplaceTextOnCursor(final String newText) {
        final Editable edit = getText();
        if (edit != null && newText != null) {

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
                postDelayed(() -> TextViewUtils.showSelection(this), 500);
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

    public boolean indexesValid(int... indexes) {
        return GsTextUtils.inRange(0, length(), indexes);
    }

    private void setupCustomOptions() {
        setCustomSelectionActionModeCallback(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                // Add custom items programmatically
                menu.add(0, R.string.option_select_lines, 0, "☰");
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                // Modify menu items here if necessary
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.string.option_select_lines:
                        HighlightingEditor.this.selectLines();
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                // Cleanup if needed
            }
        });
    }
}