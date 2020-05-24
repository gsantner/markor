package net.gsantner.markor.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;

public class DraggableScrollbarWebView extends WebView {

    private boolean _isFastScrolling = false;
    private boolean _fastScrollEnabled = true;
    private boolean _ltr = true;
    private int _thumbHeight;

    public DraggableScrollbarWebView(Context context) {
        super(context);
    }

    public DraggableScrollbarWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DraggableScrollbarWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!_fastScrollEnabled) {
            return super.onInterceptTouchEvent(ev);
        }
        if (_isFastScrolling) {
            return true;
        }
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN &&
                (_ltr && getWidth() - getVerticalScrollbarWidth() < ev.getX()
                        || !_ltr && getVerticalScrollbarWidth() > ev.getX())) {
            computeThumbHeight();
            awakenScrollBars();
            float scrollbarStartPos = (float) computeVerticalScrollOffset() / computeVerticalScrollRange() * (getHeight());
            if (Math.abs(ev.getY() - scrollbarStartPos) < _thumbHeight) {
                _isFastScrolling = true;
            }
            return true;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int action = ev.getActionMasked();
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            _isFastScrolling = false;
        }
        if (_isFastScrolling && action == MotionEvent.ACTION_MOVE) {
            scrollTo(0, (int) (((ev.getY() - _thumbHeight / 2) / (getHeight() - _thumbHeight)) * (computeVerticalScrollRange() - computeVerticalScrollExtent())));
            return true;
        }
        return super.onTouchEvent(ev);
    }

    // Approximate height of thumb
    private void computeThumbHeight() {
        int height = (int) ((float) computeVerticalScrollExtent() * getHeight() / computeVerticalScrollRange());
        int minHeight = getHeight() / 8;
        if (height < minHeight) {
            height = minHeight;
        }
        _thumbHeight = height;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            _ltr = getLayoutDirection() == View.LAYOUT_DIRECTION_LTR;
        }
    }

    public void enableFastScroll() {
        _fastScrollEnabled = true;
    }

    public void disableFastScroll() {
        _fastScrollEnabled = false;
    }

}
