/*#######################################################
 *
 *   Maintained 2017-2026 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.frontend.textview;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class RecyclerTextEditor extends RecyclerView {
    private final ArrayList<String> _lines = new ArrayList<>();
    private final LinesAdapter _adapter = new LinesAdapter();
    private final ArrayList<Runnable> _textChangedListeners = new ArrayList<>();

    private boolean _trailingNewline;
    private float _textSizeSp = 16f;
    private float _lineSpacingMultiplier = 1f;
    private int _textColor;
    private int _backgroundColor;
    private Typeface _typeface;
    private boolean _wrapEnabled = true;

    public RecyclerTextEditor(@NonNull Context context) {
        this(context, null);
    }

    public RecyclerTextEditor(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setLayoutManager(new LinearLayoutManager(context));
        setAdapter(_adapter);
        setItemAnimator(null);
        _lines.add("");
    }

    public void setText(@Nullable CharSequence text) {
        _lines.clear();
        _trailingNewline = false;
        final String content = text != null ? text.toString() : "";
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
        _adapter.notifyDataSetChanged();
    }

    @NonNull
    public CharSequence getText() {
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
        return sb;
    }

    public int length() {
        int total = _trailingNewline && !_lines.isEmpty() ? 1 : 0;
        for (int i = 0; i < _lines.size(); i++) {
            total += _lines.get(i).length();
            if (i > 0) {
                total += 1;
            }
        }
        return total;
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

    private void notifyTextChanged() {
        for (Runnable listener : _textChangedListeners) {
            if (listener != null) {
                listener.run();
            }
        }
    }

    private final class LinesAdapter extends RecyclerView.Adapter<LineViewHolder> {
        @NonNull
        @Override
        public LineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            final AppCompatEditText edit = new AppCompatEditText(parent.getContext());
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

            _edit.setText(_lines.get(position));
            _edit.setTextSize(TypedValue.COMPLEX_UNIT_SP, _textSizeSp);
            _edit.setTypeface(_typeface);
            _edit.setTextColor(_textColor);
            _edit.setBackgroundColor(_backgroundColor);
            _edit.setLineSpacing(0f, _lineSpacingMultiplier);
            _edit.setHorizontallyScrolling(!_wrapEnabled);

            _watcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    final int pos = getBindingAdapterPosition();
                    if (pos != NO_POSITION) {
                        _lines.set(pos, s != null ? s.toString() : "");
                        notifyTextChanged();
                    }
                }
            };
            _edit.addTextChangedListener(_watcher);
        }
    }
}
