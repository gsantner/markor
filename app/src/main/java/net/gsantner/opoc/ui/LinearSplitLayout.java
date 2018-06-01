/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License: Apache 2.0 / Commercial
 *  https://github.com/gsantner/opoc/#licensing
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.opoc.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/**
 * This is a simple layout extended from {@link LinearLayout} that adapt to the device orientation.
 * It may be used for easy splitted children views, orientation independent and without multiple layout files.
 * <p>
 * It is intended to layout two direct children which are splitted by given {@code weight_sum} and {@code layout_weight}
 * The layout adapts to the orientation by swapping horizontal/vertical orientation and swaps height/width.
 */
@SuppressWarnings("unused")
public class LinearSplitLayout extends LinearLayout {

    private void updateLayoutMode(View v, ViewGroup.LayoutParams givenParams) {
        boolean isInPortrait = getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) givenParams;
        setOrientation(isInPortrait ? VERTICAL : HORIZONTAL);

        if (params == null) {
            params = v.getLayoutParams() != null ? ((LinearLayout.LayoutParams) v.getLayoutParams()) : generateDefaultLayoutParams();
        }
        params.width = isInPortrait ? ViewGroup.LayoutParams.MATCH_PARENT : 0;
        params.height = isInPortrait ? 0 : ViewGroup.LayoutParams.MATCH_PARENT;
        v.setLayoutParams(params);
    }

    public LinearSplitLayout(Context context) {
        super(context);
    }

    public LinearSplitLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public LinearSplitLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public LinearSplitLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void addView(View child) {
        if (getChildCount() > 1) {
            throw new IllegalStateException("LinearSplitLayout can host only two direct children");
        }
        updateLayoutMode(child, null);
        super.addView(child);
    }

    @Override
    public void addView(View child, int index) {
        if (getChildCount() > 1) {
            throw new IllegalStateException("LinearSplitLayout can host only two direct children");
        }
        updateLayoutMode(child, null);
        super.addView(child, index);
    }

    @Override
    public void addView(View child, int width, int height) {
        if (getChildCount() > 1) {
            throw new IllegalStateException("LinearSplitLayout can host only two direct children");
        }
        updateLayoutMode(child, null);
        super.addView(child, width, height);
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
        if (getChildCount() > 1) {
            throw new IllegalStateException("LinearSplitLayout can host only two direct children");
        }
        updateLayoutMode(child, child.getLayoutParams());
        super.addView(child, params);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (getChildCount() > 1) {
            throw new IllegalStateException("LinearSplitLayout can host only two direct children");
        }
        updateLayoutMode(child, params);
        super.addView(child, index, params);
    }
}