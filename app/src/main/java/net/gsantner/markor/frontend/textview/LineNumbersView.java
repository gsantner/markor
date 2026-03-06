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

public class LineNumbersView extends View {
    private EditText editText;
    private LineNumbersDrawer lineNumbersDrawer;
    private boolean lineNumbersEnabled;

    public LineNumbersView(Context context) {
        super(context);
    }

    public LineNumbersView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LineNumbersView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setWidth(0); // Initial width
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (isLineNumbersEnabled() && visibility == View.VISIBLE) {
            refresh();
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (lineNumbersEnabled) {
            lineNumbersDrawer.draw(canvas);
        }
    }

    /**
     * Refresh LineNumbersView.
     */
    public void refresh() {
        invalidate();
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

    public void setWidth(int width) {
        getLayoutParams().width = width;
        requestLayout();
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
            lineNumbersDrawer.cleanup();
        }
        refresh();
    }

    public boolean isLineNumbersEnabled() {
        return lineNumbersEnabled;
    }

    static class LineNumbersDrawer {
        private final EditText editText;
        private final LineNumbersView lineNumbersView;

        private final Paint paint = new Paint();

        private static final int NUMBER_PADDING_LEFT = 18;
        private static final int NUMBER_PADDING_RIGHT = 14;
        private static final int EDITOR_PADDING_LEFT = 8;
        private final int ORIGINAL_PADDING_LEFT;

        private final Rect visibleRect = new Rect();
        private final Rect lineNumbersRect = new Rect();

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
                    lineNumbersView.refresh();
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
                    lineNumbersView.refresh();
                }
            }
        };

        public LineNumbersDrawer(final @NonNull EditText editText, final @NonNull LineNumbersView lineNumbersView) {
            this.editText = editText;
            this.lineNumbersView = lineNumbersView;
            ORIGINAL_PADDING_LEFT = editText.getPaddingLeft();
            paint.setColor(0xFF999999);
            paint.setTextAlign(Paint.Align.RIGHT);
        }

        public EditText getEditText() {
            return editText;
        }

        private boolean isOutOfLineNumbersArea() {
            final int margin = (int) (visibleRect.height() * 0.5f);
            final int top = visibleRect.top - margin;
            final int bottom = visibleRect.bottom + margin;

            if (top < lineNumbersRect.top || bottom > lineNumbersRect.bottom) {
                // Set line numbers area
                // height of line numbers area = (1.5 + 1 + 1.5) * height of visible area
                lineNumbersRect.top = top - visibleRect.height();
                lineNumbersRect.bottom = bottom + visibleRect.height();
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
            lineNumbersView.setWidth(0);
            lineNumbersView.setVisibility(VISIBLE);
            editText.setPadding(EDITOR_PADDING_LEFT, editText.getPaddingTop(), editText.getPaddingRight(), editText.getPaddingBottom());
        }

        /**
         * Draw line numbers.
         *
         * @param canvas The canvas on which the line numbers will be drawn.
         */
        public void draw(final Canvas canvas) {
            if (!editText.getLocalVisibleRect(visibleRect)) {
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
                lineNumbersView.setWidth(fenceX + 1);
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
            canvas.drawLine(fenceX, lineNumbersRect.top, fenceX, lineNumbersRect.bottom, paint);

            // Draw line numbers
            int i = startLine[0];
            int number = startLine[1];
            int y = layout.getLineBaseline(i);
            final int count = layout.getLineCount();
            final int offsetY = editText.getPaddingTop();

            if (y > lineNumbersRect.top) {
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
                    if (y > lineNumbersRect.top) {
                        if (invalid) {
                            invalid = false;
                            startLine[0] = i;
                            startLine[1] = number;
                        }
                        canvas.drawText(String.valueOf(number), numberX, y + offsetY, paint);
                        if (y > lineNumbersRect.bottom) {
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
        public void cleanup() {
            setLineTracking(false);
            setRefreshOnScrollChanged(false);
            maxNumberDigits = 0;
            lineNumbersView.setVisibility(GONE);
            editText.setPadding(ORIGINAL_PADDING_LEFT, editText.getPaddingTop(), editText.getPaddingRight(), editText.getPaddingBottom());
        }
    }
}
