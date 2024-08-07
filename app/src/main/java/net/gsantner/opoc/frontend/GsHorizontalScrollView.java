package net.gsantner.opoc.frontend;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.HorizontalScrollView;

import androidx.annotation.RequiresApi;

public class GsHorizontalScrollView extends HorizontalScrollView {

    private int currentScrollX; // Current X scroll value in pixels

    public GsHorizontalScrollView(Context context) {
        super(context);
    }

    public GsHorizontalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GsHorizontalScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public GsHorizontalScrollView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (currentScrollX > 0) {
            // Restore previous X scroll value (horizontal position)
            scrollTo(currentScrollX, 0);
        }
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        currentScrollX = scrollX;
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);
    }
}
