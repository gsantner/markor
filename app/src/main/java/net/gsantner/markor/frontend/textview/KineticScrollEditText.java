package net.gsantner.markor.frontend.textview;
import android.content.Context;
import android.text.Layout;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.OverScroller;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.view.GestureDetectorCompat;

public class KineticScrollEditText extends AppCompatEditText {
    private GestureDetectorCompat _gestureDetector = null;
    private OverScroller _scroller = null;
    private boolean _isHorizontallyScrolling = true;

    public KineticScrollEditText(final Context context) {
        super(context);
        initScrolling();
    }

    public KineticScrollEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        initScrolling();
    }

    public KineticScrollEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initScrolling();
    }

    private void initScrolling() {
        final Context context = getContext();
        _scroller = new OverScroller(context);

        _gestureDetector = new GestureDetectorCompat(context, new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (canScrollHorizontally((int) distanceX)) {
                    scrollBy((int) distanceX, 0);
                    return true;
                }
                return false;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (Math.abs(velocityX) > Math.abs(velocityY)) {
                    final Layout layout = getLayout();
                    final int startX = getScrollX();
                    final int startY = getScrollY();
                    final int minX = 0;
                    final int minY = 0;
                    final int maxX = layout.getWidth() - getWidth() + getPaddingStart() + getPaddingEnd();
                    final int maxY = layout.getHeight() - getHeight() + getPaddingTop() + getPaddingBottom();
                    _scroller.fling(startX, startY, (int) -velocityX, (int) -velocityY, minX, maxX, minY, maxY);
                    postInvalidateOnAnimation();
                    return true;
                }
                return false;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                return KineticScrollEditText.this.onTouchEvent(e);
            }
        });
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        return _gestureDetector.onTouchEvent(event);
    }

    @Override
    public void computeScroll() {
        if (_scroller != null && _scroller.computeScrollOffset()) {
            scrollTo(_scroller.getCurrX(), _scroller.getCurrY());
            // Continue to invalidate until the animation is complete.
            postInvalidateOnAnimation();
        }
    }

    public boolean isHorizontallyScrolling() {
        return _isHorizontallyScrolling;
    }

    @Override
    public void setHorizontallyScrolling(boolean whether) {
        _isHorizontallyScrolling = whether;
        super.setHorizontallyScrolling(whether);
    }
}
