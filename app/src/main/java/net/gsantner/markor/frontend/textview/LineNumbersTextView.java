package net.gsantner.markor.frontend.textview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;

@SuppressWarnings("UnusedReturnValue")
public class LineNumbersTextView extends AppCompatTextView {
    private EditText editText;
    private LineNumbersDrawer lineNumbersDrawer;
    private boolean lineNumbersEnabled;

    public LineNumbersTextView(Context context) {
        super(context);
    }

    public LineNumbersTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LineNumbersTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (isLineNumbersEnabled() && visibility == View.VISIBLE) {
            refresh();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (lineNumbersEnabled) {
            lineNumbersDrawer.draw(canvas);
        }
    }

    public void refresh() {
        setText(""); // To activate LineNumbersTextView refresh
        if (getWidth() == 0) {
            lineNumbersDrawer.getEditText().postInvalidate();
        }
    }

    public void setup(final @NonNull EditText editText) {
        if (lineNumbersEnabled) {
            setLineNumbersEnabled(false);
        }
        this.editText = editText;
        this.lineNumbersDrawer = null;
    }

    public void setLineNumbersEnabled(final boolean enabled) {
        lineNumbersEnabled = enabled;

        if (enabled) {
            if (lineNumbersDrawer == null) {
                lineNumbersDrawer = new LineNumbersDrawer(editText, this);
            }
            lineNumbersDrawer.prepare();
        } else {
            if (lineNumbersDrawer == null) {
                return;
            }
            lineNumbersDrawer.done();
        }
        refresh();
    }

    public boolean isLineNumbersEnabled() {
        return lineNumbersEnabled;
    }

    static class LineNumbersDrawer {
        private final EditText editText;
        private final LineNumbersTextView textView;

        private final Paint paint = new Paint();

        private static final int NUMBER_PADDING_LEFT = 18;
        private static final int NUMBER_PADDING_RIGHT = 14;
        private static final int EDITOR_PADDING_LEFT = 8;
        private final int ORIGINAL_PADDING_LEFT;

        private final Rect visibleArea = new Rect();
        private final Rect lineNumbersArea = new Rect();

        private int fenceX;
        private int numberX;
        private int maxNumber = 1; // To gauge the width of line numbers fence
        private int maxNumberDigits;
        private int lastMaxNumber;
        private int lastLayoutLineCount;
        private float lastTextSize;

        private final int[] startLine = {0, 1}; // {line index, actual line number}

        private final TextWatcher lineTrackingWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                maxNumber -= countLines(s, start, start + count);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                maxNumber += countLines(s, start, start + count);
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (isLayoutLineCountChanged() || isMaxNumberChanged()) {
                    textView.refresh();
                }
            }
        };

        private final ViewTreeObserver.OnScrollChangedListener onScrollChangedListener = new ViewTreeObserver.OnScrollChangedListener() {
            private long lastTime;

            @Override
            public void onScrollChanged() {
                final long time = System.currentTimeMillis();
                if (time - lastTime > 125) {
                    lastTime = time;
                    textView.refresh();
                }
            }
        };

        public LineNumbersDrawer(final @NonNull EditText editText, final @NonNull LineNumbersTextView textView) {
            this.editText = editText;
            this.textView = textView;
            ORIGINAL_PADDING_LEFT = editText.getPaddingLeft();
            paint.setColor(0xFF999999);
            paint.setTextAlign(Paint.Align.RIGHT);
        }

        public EditText getEditText() {
            return editText;
        }

        private boolean isOutOfLineNumbersArea() {
            final int margin = (int) (visibleArea.height() * 0.5f);
            final int top = visibleArea.top - margin;
            final int bottom = visibleArea.bottom + margin;

            if (top < lineNumbersArea.top || bottom > lineNumbersArea.bottom) {
                // Set line numbers area
                // height of line numbers area = (1.5 + 1 + 1.5) * height of visible area
                lineNumbersArea.top = top - visibleArea.height();
                lineNumbersArea.bottom = bottom + visibleArea.height();
                return true;
            } else {
                return false;
            }
        }

        private boolean isTextSizeChanged() {
            if (editText.getTextSize() == lastTextSize) {
                return false;
            } else {
                lastTextSize = editText.getTextSize();
                paint.setTextSize(lastTextSize);
                return true;
            }
        }

        private boolean isMaxNumberChanged() {
            if (maxNumber == lastMaxNumber) {
                return false;
            } else {
                lastMaxNumber = maxNumber;
                return true;
            }
        }

        private boolean isMaxNumberDigitsChanged() {
            int digits;
            if (maxNumber < 10) {
                digits = 1;
            } else if (maxNumber < 100) {
                digits = 2;
            } else if (maxNumber < 1000) {
                digits = 3;
            } else if (maxNumber < 10000) {
                digits = 4;
            } else {
                digits = 5;
            }

            if (digits == maxNumberDigits) {
                return false;
            }

            maxNumberDigits = digits;
            return true;
        }

        private boolean isLayoutLineCountChanged() {
            final Layout layout = editText.getLayout();
            if (layout == null) {
                return true;
            }

            final int lineCount = layout.getLineCount();
            if (lineCount == lastLayoutLineCount) {
                return false;
            } else {
                lastLayoutLineCount = lineCount;
                return true;
            }
        }

        private int countLines(final CharSequence s, int start, int end) {
            int count = 0;
            for (; start < end; start++) {
                if (s.charAt(start) == '\n') {
                    count++;
                }
            }
            return count;
        }

        private void setLineTracking(boolean enabled) {
            editText.removeTextChangedListener(lineTrackingWatcher);

            if (enabled) {
                maxNumber = 1;
                final CharSequence text = editText.getText();
                if (text != null) {
                    maxNumber += countLines(text, 0, text.length());
                }
                editText.addTextChangedListener(lineTrackingWatcher);
            }
        }

        private void setRefreshOnScrollChanged(final boolean enabled) {
            if (enabled) {
                editText.getViewTreeObserver().addOnScrollChangedListener(onScrollChangedListener);
            } else {
                editText.getViewTreeObserver().removeOnScrollChangedListener(onScrollChangedListener);
            }
        }

        /**
         * Prepare for drawing line numbers.
         */
        public void prepare() {
            setLineTracking(true);
            setRefreshOnScrollChanged(true);
            textView.setVisibility(VISIBLE);
            editText.setPadding(EDITOR_PADDING_LEFT, editText.getPaddingTop(), editText.getPaddingRight(), editText.getPaddingBottom());
        }

        /**
         * Draw line numbers.
         *
         * @param canvas The canvas on which the line numbers will be drawn.
         */
        public void draw(final Canvas canvas) {
            if (!editText.getLocalVisibleRect(visibleArea)) {
                return;
            }

            final CharSequence text = editText.getText();
            final Layout layout = editText.getLayout();
            if (text == null || layout == null) {
                return;
            }

            // If text size or the max line number of digits changed, update related variables
            if (isTextSizeChanged() || isMaxNumberDigitsChanged()) {
                numberX = NUMBER_PADDING_LEFT + (int) paint.measureText(String.valueOf(maxNumber));
                fenceX = numberX + NUMBER_PADDING_RIGHT;
                textView.setWidth(fenceX + 1);
            }

            // If current visible area is out of current line numbers area,
            // will recalculate the start line
            boolean invalid = false;
            if (isOutOfLineNumbersArea()) {
                invalid = true;
                startLine[0] = 0;
                startLine[1] = 1;
            }

            // Draw right border of the fence
            canvas.drawLine(fenceX, lineNumbersArea.top, fenceX, lineNumbersArea.bottom, paint);

            // Draw line numbers
            int i = startLine[0];
            int number = startLine[1];
            int y = layout.getLineBaseline(i);
            final int count = layout.getLineCount();
            final int offsetY = editText.getPaddingTop();

            if (y > lineNumbersArea.top) {
                if (invalid) {
                    invalid = false;
                    startLine[0] = i;
                    startLine[1] = number;
                }
                canvas.drawText(String.valueOf(number), numberX, layout.getLineBaseline(i) + offsetY, paint);
            }
            i++;
            number++;

            for (; i < count; i++) {
                if (text.charAt(layout.getLineStart(i) - 1) == '\n') {
                    y = layout.getLineBaseline(i);
                    if (y > lineNumbersArea.top) {
                        if (invalid) {
                            invalid = false;
                            startLine[0] = i;
                            startLine[1] = number;
                        }
                        canvas.drawText(String.valueOf(number), numberX, y + offsetY, paint);
                        if (y > lineNumbersArea.bottom) {
                            break;
                        }
                    }
                    number++;
                }
            }
        }

        /**
         * Reset some states related line numbers.
         */
        public void done() {
            setLineTracking(false);
            setRefreshOnScrollChanged(false);
            maxNumberDigits = 0;
            textView.setWidth(0);
            textView.setVisibility(GONE);
            editText.setPadding(ORIGINAL_PADDING_LEFT, editText.getPaddingTop(), editText.getPaddingRight(), editText.getPaddingBottom());
        }
    }
}
