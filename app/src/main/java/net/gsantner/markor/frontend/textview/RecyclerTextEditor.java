/*#######################################################
 *
 *   Maintained 2017-2026 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.frontend.textview;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.gsantner.opoc.format.GsTextUtils;
import net.gsantner.opoc.wrapper.GsCallback;

import java.util.ArrayList;
import java.util.List;

public class RecyclerTextEditor extends RecyclerView implements MarkorEditor {
    private final ArrayList<String> _lines = new ArrayList<>();
    private final LinesAdapter _adapter = new LinesAdapter();
    private final ArrayList<Runnable> _textChangedListeners = new ArrayList<>();
    private final ArrayList<TextWatcher> _textWatcherListeners = new ArrayList<>();
    private final ArrayList<SyntaxHighlighterBase.SpanGroup> _matches = new ArrayList<>();
    private final RecyclerEditable _editorText = new RecyclerEditable();
    private int _textChangedNumber;
    private final Runnable _textChangedRecorder = TextViewUtils.makeDebounced(1000, () -> _textChangedNumber++);
    private final Runnable _highlightingDebounced = TextViewUtils.makeDebounced(220, this::recomputeHighlighting);

    private boolean _trailingNewline;
    private float _textSizeSp = 16f;
    private float _lineSpacingMultiplier = 1f;
    private int _textColor;
    private int _backgroundColor;
    private Typeface _typeface;
    private boolean _wrapEnabled = true;

    private int _selectionStart;
    private int _selectionEnd;
    private InputFilter _autoFormatFilter;
    private TextWatcher _autoFormatModifier;
    private boolean _autoFormatEnabled;
    private SyntaxHighlighterBase _highlighter;
    private boolean _hlEnabled;
    private SyntaxHighlighterBase.SpanGroup _searchSelection;
    private boolean _saveInstanceState = true;
    private boolean _suppressEditorTextCallback;
    private boolean _suppressSelectionSync;
    private View.OnFocusChangeListener _externalFocusChangeListener;

    public RecyclerTextEditor(@NonNull Context context) {
        this(context, null);
    }

    public RecyclerTextEditor(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setLayoutManager(new LinearLayoutManager(context));
        setAdapter(_adapter);
        setItemAnimator(null);
        _lines.add("");
        syncEditorTextFromLines();
    }

    @Override
    public void setText(@Nullable CharSequence text) {
        parseToLines(text != null ? text.toString() : "");
        syncEditorTextFromLines();
        if (_hlEnabled) {
            _highlightingDebounced.run();
        }
        notifyDataSetChangedPreservingFocus();
        applySelectionToVisibleLineEditor();
    }

    @NonNull
    @Override
    public Editable getText() {
        return _editorText;
    }

    @Override
    public int length() {
        return _editorText.length();
    }

    @Override
    public void setBackgroundColor(int color) {
        super.setBackgroundColor(color);
        _backgroundColor = color;
        _adapter.notifyItemRangeChanged(0, _lines.size());
    }

    public void setEditorTextColor(int color) {
        _textColor = color;
        _adapter.notifyItemRangeChanged(0, _lines.size());
    }

    public void setEditorTextSize(float sizeSp) {
        _textSizeSp = sizeSp;
        _adapter.notifyItemRangeChanged(0, _lines.size());
    }

    public void setEditorTypeface(@Nullable Typeface typeface) {
        _typeface = typeface;
        _adapter.notifyItemRangeChanged(0, _lines.size());
    }

    public void setEditorLineSpacing(float spacingMultiplier) {
        _lineSpacingMultiplier = spacingMultiplier;
        _adapter.notifyItemRangeChanged(0, _lines.size());
    }

    public void setWrapEnabled(boolean enabled) {
        _wrapEnabled = enabled;
        _adapter.notifyItemRangeChanged(0, _lines.size());
    }

    public boolean isWrapEnabled() {
        return _wrapEnabled;
    }

    public void addTextChangedListener(@Nullable Runnable listener) {
        if (listener != null) {
            _textChangedListeners.add(listener);
        }
    }

    @Override
    public void addTextChangedListener(@NonNull TextWatcher watcher) {
        if (!_textWatcherListeners.contains(watcher)) {
            _textWatcherListeners.add(watcher);
        }
    }

    @Override
    public void removeTextChangedListener(@NonNull TextWatcher watcher) {
        _textWatcherListeners.remove(watcher);
    }

    @Override
    public void setSelection(int index) {
        if (indexesValid(index)) {
            _selectionStart = index;
            _selectionEnd = index;
            Selection.setSelection(_editorText, index);
            applySelectionToVisibleLineEditor();
        }
    }

    @Override
    public void setSelection(int start, int stop) {
        if (indexesValid(start, stop)) {
            _selectionStart = start;
            _selectionEnd = stop;
        } else if (indexesValid(start, stop - 1)) {
            _selectionStart = start;
            _selectionEnd = stop - 1;
        } else if (indexesValid(start + 1, stop)) {
            _selectionStart = start + 1;
            _selectionEnd = stop;
        } else {
            return;
        }
        Selection.setSelection(_editorText, _selectionStart, _selectionEnd);
        applySelectionToVisibleLineEditor();
    }

    @Override
    public int getSelectionStart() {
        final int sel = Selection.getSelectionStart(_editorText);
        return sel >= 0 ? sel : _selectionStart;
    }

    @Override
    public int getSelectionEnd() {
        final int sel = Selection.getSelectionEnd(_editorText);
        return sel >= 0 ? sel : _selectionEnd;
    }

    @Override
    public boolean hasSelection() {
        return getSelectionStart() != getSelectionEnd();
    }

    @Override
    public void selectLines() {
        final int[] lineSelection = TextViewUtils.getLineSelection(_editorText);
        setSelection(lineSelection[0], lineSelection[1]);
    }

    @Override
    public void insertOrReplaceTextOnCursor(String newText) {
        final Editable edit = getText();
        if (newText == null) {
            return;
        }

        final int[] sel = TextViewUtils.getSelection(edit);
        final CharSequence selected = TextViewUtils.toString(edit, Math.max(sel[0], 0), Math.max(sel[1], 0));
        final String expanded = newText.replace(INSERT_SELECTION_HERE_TOKEN, selected);

        final int newCursorPos = expanded.indexOf(PLACE_CURSOR_HERE_TOKEN);
        final String finalText = expanded.replace(PLACE_CURSOR_HERE_TOKEN, "");

        final int start = Math.max(sel[0], 0);
        final int end = Math.max(sel[1], start);
        if (newCursorPos >= 0) {
            setSelection(start);
        }

        withAutoFormatDisabled(() -> edit.replace(start, end, finalText));

        if (newCursorPos >= 0) {
            setSelection(start + newCursorPos);
        }
    }

    @Override
    public void simulateKeyPress(int keyEvent_KEYCODE_SOMETHING) {
        final AppCompatEditText focused = findFocusedLineEditor();
        if (focused == null) {
            return;
        }
        focused.dispatchKeyEvent(new KeyEvent(0, 0, KeyEvent.ACTION_DOWN, keyEvent_KEYCODE_SOMETHING, 0));
        focused.dispatchKeyEvent(new KeyEvent(0, 0, KeyEvent.ACTION_UP, keyEvent_KEYCODE_SOMETHING, 0));
    }

    @Override
    public void setAutoFormatters(@Nullable InputFilter inputFilter, @Nullable TextWatcher modifier) {
        final boolean enabled = _autoFormatEnabled;
        if (enabled) {
            setAutoFormatEnabled(false);
        }
        _autoFormatFilter = inputFilter;
        _autoFormatModifier = modifier;
        if (enabled) {
            setAutoFormatEnabled(true);
        }
    }

    @Override
    public boolean getAutoFormatEnabled() {
        return _autoFormatEnabled;
    }

    @Override
    public void setAutoFormatEnabled(boolean enable) {
        if (enable && !_autoFormatEnabled) {
            if (_autoFormatModifier != null && !_textWatcherListeners.contains(_autoFormatModifier)) {
                _textWatcherListeners.add(_autoFormatModifier);
            }
        } else if (!enable && _autoFormatEnabled) {
            _textWatcherListeners.remove(_autoFormatModifier);
        }
        _autoFormatEnabled = enable;
        _adapter.notifyItemRangeChanged(0, _lines.size());
    }

    @Override
    public void withAutoFormatDisabled(@NonNull GsCallback.a0 callback) {
        final boolean enabled = _autoFormatEnabled;
        try {
            _autoFormatEnabled = false;
            callback.callback();
        } finally {
            _autoFormatEnabled = enabled;
        }
    }

    @Override
    public void setHighlighter(@Nullable SyntaxHighlighterBase highlighter) {
        if (_highlighter != null) {
            _highlighter.clearDynamic().clearStatic(true);
        }
        _highlighter = highlighter;
        if (_hlEnabled) {
            initHighlighter();
            _highlightingDebounced.run();
        }
    }

    @Nullable
    @Override
    public SyntaxHighlighterBase getHighlighter() {
        return _highlighter;
    }

    @Override
    public boolean setHighlightingEnabled(boolean enable) {
        final boolean previous = _hlEnabled;
        _hlEnabled = enable;
        if (_hlEnabled) {
            initHighlighter();
            _highlightingDebounced.run();
        } else if (_highlighter != null) {
            _highlighter.clearDynamic().clearStatic(true).clearComputed();
            notifyDataSetChangedPreservingFocus();
        }
        return previous;
    }

    @Override
    public boolean getHighlightingEnabled() {
        return _hlEnabled;
    }

    @Override
    public void recomputeHighlighting() {
        if (!_hlEnabled || _highlighter == null) {
            return;
        }
        _highlighter
                .setSpannable(_editorText)
                .configure(getEditorPaint())
                .clearDynamic()
                .clearStatic(false)
                .recompute()
                .addAdditional(_matches);
        if (_searchSelection != null) {
            _highlighter.addAdditional(_searchSelection);
        }
        _highlighter.applyStatic();
        refreshVisibleLineEditors(false);
    }

    @Override
    public void initHighlighter() {
        if (_highlighter != null) {
            _highlighter.setSpannable(_editorText).configure(getEditorPaint());
        }
    }

    private Paint getEditorPaint() {
        final Paint paint = new Paint();
        paint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, _textSizeSp, getResources().getDisplayMetrics()));
        paint.setTypeface(_typeface);
        return paint;
    }

    @Override
    public void setSearchMatches(@Nullable List<SyntaxHighlighterBase.SpanGroup> spanGroups) {
        _matches.clear();
        if (spanGroups != null) {
            _matches.addAll(spanGroups);
        }
        if (_hlEnabled) {
            _highlightingDebounced.run();
        }
    }

    @Override
    public void removeSearchMatch(@Nullable SyntaxHighlighterBase.SpanGroup spanGroup) {
        _matches.remove(spanGroup);
        if (_hlEnabled) {
            _highlightingDebounced.run();
        }
    }

    @Override
    public void clearSearchMatches() {
        _matches.clear();
        if (_hlEnabled) {
            _highlightingDebounced.run();
        }
    }

    @Override
    public void applyDynamicHighlight() {
        if (_hlEnabled) {
            _highlightingDebounced.run();
        }
    }

    @Override
    public void addSearchSelection(int start, int end, @ColorInt int color) {
        _searchSelection = SyntaxHighlighterBase.createBackgroundHighlight(start, end, color);
        if (_hlEnabled) {
            _highlightingDebounced.run();
        }
    }

    @Override
    public void clearSearchSelection() {
        _searchSelection = null;
        if (_hlEnabled) {
            _highlightingDebounced.run();
        }
    }

    @NonNull
    @Override
    public View getView() {
        return this;
    }

    @Override
    public void setOnFocusChangeListener(@Nullable OnFocusChangeListener listener) {
        _externalFocusChangeListener = listener;
        super.setOnFocusChangeListener(listener);
    }

    @Override
    public int getTextChangedNumber() {
        return _textChangedNumber;
    }

    @Override
    public void setSaveInstanceState(boolean save) {
        _saveInstanceState = save;
    }

    @Override
    public boolean indexesValid(int... indexes) {
        return GsTextUtils.inRange(0, length(), indexes);
    }

    @Override
    public int moveCursorToEndOfLine(int offset) {
        final int[] lineCol = globalOffsetToLineCol(getSelectionEnd());
        final int lineEnd = lineColToGlobalOffset(lineCol[0], _lines.get(lineCol[0]).length());
        final int newPos = clampToLength(lineEnd + offset);
        setSelection(newPos);
        return getSelectionStart();
    }

    @Override
    public int moveCursorToBeginOfLine(int offset) {
        final int[] lineCol = globalOffsetToLineCol(getSelectionEnd());
        final int lineStart = lineColToGlobalOffset(lineCol[0], 0);
        final int newPos = clampToLength(lineStart + offset);
        setSelection(newPos);
        return getSelectionStart();
    }

    private void parseToLines(@NonNull String content) {
        _lines.clear();
        _trailingNewline = false;

        final int len = content.length();
        int start = 0;
        for (int i = 0; i < len; i++) {
            if (content.charAt(i) == '\n') {
                _lines.add(content.substring(start, i));
                start = i + 1;
            }
        }
        if (start < len) {
            _lines.add(content.substring(start));
        } else {
            _trailingNewline = len > 0;
        }
        if (_lines.isEmpty()) {
            _lines.add("");
        }
    }

    @NonNull
    private String buildFullTextFromLines() {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < _lines.size(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(_lines.get(i));
        }
        if (_trailingNewline && !_lines.isEmpty()) {
            sb.append('\n');
        }
        return sb.toString();
    }

    private void syncEditorTextFromLines() {
        _suppressEditorTextCallback = true;
        _editorText.replace(0, _editorText.length(), buildFullTextFromLines());
        _selectionStart = clampToLength(_selectionStart);
        _selectionEnd = clampToLength(_selectionEnd);
        Selection.setSelection(_editorText, _selectionStart, _selectionEnd);
        _suppressEditorTextCallback = false;
    }

    private void syncFromEditorText() {
        parseToLines(_editorText.toString());
        _selectionStart = clampToLength(getSelectionStart());
        _selectionEnd = clampToLength(getSelectionEnd());
        notifyDataSetChangedPreservingFocus();
        applySelectionToVisibleLineEditor();
    }

    private void notifyDataSetChangedPreservingFocus() {
        final boolean hadFocusedLineEditor = findFocusedLineEditor() != null;
        _adapter.notifyDataSetChanged();
        if (hadFocusedLineEditor) {
            post(this::applySelectionToVisibleLineEditor);
        }
    }

    private void onEditorTextMutated() {
        if (_suppressEditorTextCallback) {
            return;
        }
        syncFromEditorText();
        _textChangedRecorder.run();
        notifyTextChanged();
        if (_hlEnabled) {
            _highlightingDebounced.run();
        }
    }

    private int clampToLength(int value) {
        return Math.max(0, Math.min(value, _editorText.length()));
    }

    private int[] globalOffsetToLineCol(int offset) {
        if (_lines.isEmpty()) {
            return new int[]{0, 0};
        }

        int remaining = clampToLength(offset);
        final int lastIndex = _lines.size() - 1;
        for (int i = 0; i < _lines.size(); i++) {
            final int lineLen = _lines.get(i).length();
            if (remaining <= lineLen) {
                return new int[]{i, remaining};
            }
            remaining -= lineLen;

            final boolean hasLineBreakAfter = i < lastIndex || _trailingNewline;
            if (hasLineBreakAfter) {
                if (remaining == 0) {
                    if (i < lastIndex) {
                        return new int[]{i + 1, 0};
                    }
                    return new int[]{i, lineLen};
                }
                remaining -= 1;
            }
        }
        return new int[]{lastIndex, _lines.get(lastIndex).length()};
    }

    private int lineColToGlobalOffset(int lineIndex, int column) {
        if (_lines.isEmpty()) {
            return 0;
        }

        final int safeLine = Math.max(0, Math.min(lineIndex, _lines.size() - 1));
        int offset = 0;
        for (int i = 0; i < safeLine; i++) {
            offset += _lines.get(i).length() + 1;
        }
        final int safeCol = Math.max(0, Math.min(column, _lines.get(safeLine).length()));
        return Math.max(0, Math.min(offset + safeCol, _editorText.length()));
    }

    private int lineStartOffset(int lineIndex) {
        int start = 0;
        for (int i = 0; i < lineIndex; i++) {
            start += _lines.get(i).length() + 1;
        }
        return start;
    }

    private void updateSelectionFromLineEditor(int lineIndex, int localStart, int localEnd) {
        _selectionStart = lineColToGlobalOffset(lineIndex, localStart);
        _selectionEnd = lineColToGlobalOffset(lineIndex, localEnd);
        Selection.setSelection(_editorText, _selectionStart, _selectionEnd);
    }

    private void applySelectionToVisibleLineEditor() {
        if (_lines.isEmpty()) {
            return;
        }

        final int selStart = clampToLength(_selectionStart);
        final int selEnd = clampToLength(_selectionEnd);
        final int[] startLineCol = globalOffsetToLineCol(selStart);
        final int[] endLineCol = globalOffsetToLineCol(selEnd);
        final int targetLine = endLineCol[0];

        final LineViewHolder holder = (LineViewHolder) findViewHolderForAdapterPosition(targetLine);
        if (holder == null) {
            scrollToPosition(targetLine);
            post(this::applySelectionToVisibleLineEditor);
            return;
        }

        final AppCompatEditText edit = holder._edit;
        if (!edit.hasFocus()) {
            edit.requestFocus();
        }

        if (startLineCol[0] == endLineCol[0]) {
            final int localStart = Math.min(startLineCol[1], endLineCol[1]);
            final int localEnd = Math.max(startLineCol[1], endLineCol[1]);
            edit.setSelection(localStart, localEnd);
        } else {
            edit.setSelection(endLineCol[1]);
        }
    }

    @Nullable
    private AppCompatEditText findFocusedLineEditor() {
        final View focused = findFocus();
        if (focused instanceof AppCompatEditText) {
            return (AppCompatEditText) focused;
        }
        return null;
    }

    private void refreshVisibleLineEditors(boolean includeFocused) {
        final LayoutManager lm = getLayoutManager();
        if (!(lm instanceof LinearLayoutManager)) {
            return;
        }

        final LinearLayoutManager llm = (LinearLayoutManager) lm;
        final int first = llm.findFirstVisibleItemPosition();
        final int last = llm.findLastVisibleItemPosition();
        if (first == NO_POSITION || last == NO_POSITION || first > last) {
            return;
        }

        final View focused = findFocus();
        for (int pos = first; pos <= last; pos++) {
            final LineViewHolder holder = (LineViewHolder) findViewHolderForAdapterPosition(pos);
            if (holder == null) {
                continue;
            }
            if (!includeFocused && focused != null && focused == holder._edit) {
                continue;
            }
            holder.rebindDisplayOnly(pos);
        }
    }

    @NonNull
    private CharSequence getLineDisplayText(int position) {
        if (!_hlEnabled || _highlighter == null) {
            return _lines.get(position);
        }
        final int start = lineStartOffset(position);
        final int end = Math.min(start + _lines.get(position).length(), _editorText.length());
        if (start <= end) {
            return _editorText.subSequence(start, end);
        }
        return _lines.get(position);
    }

    private void notifyTextChanged() {
        for (Runnable listener : _textChangedListeners) {
            if (listener != null) {
                listener.run();
            }
        }
    }

    private void dispatchBeforeTextChanged(CharSequence s, int start, int count, int after) {
        for (TextWatcher watcher : new ArrayList<>(_textWatcherListeners)) {
            watcher.beforeTextChanged(s, start, count, after);
        }
    }

    private void dispatchOnTextChanged(CharSequence s, int start, int before, int count) {
        for (TextWatcher watcher : new ArrayList<>(_textWatcherListeners)) {
            watcher.onTextChanged(s, start, before, count);
        }
    }

    private void dispatchAfterTextChanged(Editable s) {
        for (TextWatcher watcher : new ArrayList<>(_textWatcherListeners)) {
            watcher.afterTextChanged(s);
        }
    }

    private final class RecyclerEditable extends SpannableStringBuilder {
        @NonNull
        @Override
        public SpannableStringBuilder replace(int start, int end, CharSequence tb, int tbstart, int tbend) {
            super.replace(start, end, tb, tbstart, tbend);
            onEditorTextMutated();
            return this;
        }
    }

    private final class LinesAdapter extends RecyclerView.Adapter<LineViewHolder> {
        @NonNull
        @Override
        public LineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            final AppCompatEditText edit = new AppCompatEditText(parent.getContext()) {
                @Override
                protected void onSelectionChanged(int selStart, int selEnd) {
                    super.onSelectionChanged(selStart, selEnd);
                    if (_suppressSelectionSync) {
                        return;
                    }
                    final Object tag = getTag();
                    if (tag instanceof Integer) {
                        updateSelectionFromLineEditor((Integer) tag, selStart, selEnd);
                    }
                }
            };
            final RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            edit.setLayoutParams(lp);
            edit.setGravity(Gravity.START | Gravity.TOP);
            edit.setPadding(0, 0, 0, 0);
            edit.setHorizontallyScrolling(!_wrapEnabled);
            edit.setMinHeight(1);
            return new LineViewHolder(edit);
        }

        @Override
        public void onBindViewHolder(@NonNull LineViewHolder holder, int position) {
            holder.bind(position);
        }

        @Override
        public int getItemCount() {
            return _lines.size();
        }
    }

    private final class LineViewHolder extends RecyclerView.ViewHolder {
        private final AppCompatEditText _edit;
        private TextWatcher _watcher;

        private LineViewHolder(@NonNull AppCompatEditText itemView) {
            super(itemView);
            _edit = itemView;
        }

        private void bind(int position) {
            if (_watcher != null) {
                _edit.removeTextChangedListener(_watcher);
            }

            _edit.setTag(position);
            _suppressSelectionSync = true;
            try {
                _edit.setText(getLineDisplayText(position));
            } finally {
                _suppressSelectionSync = false;
            }
            _edit.setTextSize(TypedValue.COMPLEX_UNIT_SP, _textSizeSp);
            _edit.setTypeface(_typeface);
            _edit.setTextColor(_textColor);
            _edit.setBackgroundColor(_backgroundColor);
            _edit.setLineSpacing(0f, _lineSpacingMultiplier);
            _edit.setHorizontallyScrolling(!_wrapEnabled);
            _edit.setFilters(_autoFormatEnabled && _autoFormatFilter != null
                    ? new InputFilter[]{_autoFormatFilter}
                    : new InputFilter[0]);
            _edit.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    updateSelectionFromLineEditor(position, _edit.getSelectionStart(), _edit.getSelectionEnd());
                }
                if (_externalFocusChangeListener != null) {
                    _externalFocusChangeListener.onFocusChange(RecyclerTextEditor.this, hasFocus);
                }
            });

            _watcher = new TextWatcher() {
                int _globalStart;

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    final int pos = getBindingAdapterPosition();
                    if (pos == NO_POSITION) {
                        return;
                    }
                    _globalStart = lineColToGlobalOffset(pos, start);
                    dispatchBeforeTextChanged(_editorText, _globalStart, count, after);
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    final int pos = getBindingAdapterPosition();
                    if (pos == NO_POSITION) {
                        return;
                    }
                    _lines.set(pos, s != null ? s.toString() : "");
                    syncEditorTextFromLines();
                    dispatchOnTextChanged(_editorText, _globalStart, before, count);
                }

                @Override
                public void afterTextChanged(Editable s) {
                    final int pos = getBindingAdapterPosition();
                    if (pos == NO_POSITION) {
                        return;
                    }
                    final boolean hasNewline = s != null && s.toString().contains("\n");
                    _lines.set(pos, s != null ? s.toString() : "");
                    updateSelectionFromLineEditor(pos, _edit.getSelectionStart(), _edit.getSelectionEnd());
                    syncEditorTextFromLines();
                    dispatchAfterTextChanged(_editorText);

                    if (hasNewline) {
                        syncFromEditorText();
                    }

                    _textChangedRecorder.run();
                    notifyTextChanged();
                    if (_hlEnabled) {
                        _highlightingDebounced.run();
                    }
                }
            };
            _edit.addTextChangedListener(_watcher);

            final int[] selectionLineCol = globalOffsetToLineCol(_selectionEnd);
            if (selectionLineCol[0] == position && !_edit.hasFocus()) {
                _edit.setSelection(selectionLineCol[1]);
            }
        }

        private void rebindDisplayOnly(int position) {
            if (_watcher != null) {
                _edit.removeTextChangedListener(_watcher);
            }
            final int selStart = _edit.getSelectionStart();
            final int selEnd = _edit.getSelectionEnd();
            _edit.setTag(position);
            _suppressSelectionSync = true;
            try {
                _edit.setText(getLineDisplayText(position));
            } finally {
                _suppressSelectionSync = false;
            }
            if (_edit.hasFocus()) {
                final int len = _edit.length();
                final int start = Math.max(0, Math.min(selStart, len));
                final int end = Math.max(0, Math.min(selEnd, len));
                _edit.setSelection(Math.min(start, end), Math.max(start, end));
            }
            if (_watcher != null) {
                _edit.addTextChangedListener(_watcher);
            }
        }
    }
}
