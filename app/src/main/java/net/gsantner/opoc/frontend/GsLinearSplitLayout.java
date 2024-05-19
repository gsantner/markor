/*#######################################################
 *
 * SPDX-FileCopyrightText: 2018-2024 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2018-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
#########################################################*/
package net.gsantner.opoc.frontend;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * This is a simple layout extended from {@link LinearLayout} that adapt to the device orientation.
 * It may be used for easy splitted children views, orientation independent and without multiple layout files.
 * <p>
 * It is intended to layout two direct children which are splitted by given {@code weight_sum} and {@code layout_weight}
 * The layout adapts to the orientation by swapping horizontal/vertical orientation and swaps height/width.
 */
@SuppressWarnings("unused")
public class GsLinearSplitLayout extends LinearLayout {

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

    public GsLinearSplitLayout(Context context) {
        super(context);
    }

    public GsLinearSplitLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public GsLinearSplitLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public GsLinearSplitLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
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