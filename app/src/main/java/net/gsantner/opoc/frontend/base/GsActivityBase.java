/*#######################################################
 *
 * SPDX-FileCopyrightText: 2020-2022 Gregor Santner <https://gsantner.net/>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2020-2022 by Gregor Santner <https://gsantner.net/>
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
#########################################################*/
package net.gsantner.opoc.frontend.base;

import static android.view.WindowManager.LayoutParams.FLAG_SECURE;

import android.content.Context;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import net.gsantner.opoc.model.GsSharedPreferencesPropertyBackend;
import net.gsantner.opoc.util.GsActivityUtils;
import net.gsantner.opoc.util.GsContextUtils;
import net.gsantner.opoc.wrapper.GsCallback;

public abstract class GsActivityBase<AS extends GsSharedPreferencesPropertyBackend> extends AppCompatActivity {

    protected AS _appSettings;
    protected Bundle _savedInstanceState;
    protected GsActivityUtils _activityUtils;
    private int m_initialToolbarHeight = 0;

    private final GsCallback.a0 m_setActivityBackgroundColor = () -> new GsActivityUtils(GsActivityBase.this).setActivityBackgroundColor(getNewActivityBackgroundColor()).freeContextRef();
    private final GsCallback.a0 m_setActivityNavigationBarColor = () -> new GsActivityUtils(GsActivityBase.this).setActivityNavigationBarBackgroundColor(getNewNavigationBarColor()).freeContextRef();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        onPreCreate(savedInstanceState);
        super.onCreate(savedInstanceState);

        m_setActivityBackgroundColor.callback();
        m_setActivityNavigationBarColor.callback();

        // Set secure flag / disallow screenshots
        if (isFlagSecure() != null) {
            try {
                if (isFlagSecure()) {
                    getWindow().setFlags(FLAG_SECURE, FLAG_SECURE);
                } else {
                    getWindow().clearFlags(FLAG_SECURE);
                }
            } catch (Exception ignored) {
            }
        }
    }

    protected void onPreCreate(@Nullable Bundle savedInstanceState) {
        _savedInstanceState = savedInstanceState;
        _appSettings = createAppSettingsInstance(this);
        _activityUtils = new GsActivityUtils(this);
    }

    public AS createAppSettingsInstance(Context applicationContext) {
        return null;
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        _savedInstanceState = savedInstanceState;
    }

    @Override
    protected void onResume() {
        super.onResume();
        m_setActivityBackgroundColor.callback();
        m_setActivityNavigationBarColor.callback();
    }

    @ColorInt
    public Integer getNewNavigationBarColor() {
        return null;
    }

    @ColorInt
    public Integer getNewActivityBackgroundColor() {
        return null;
    }

    public Boolean isFlagSecure() {
        return null;
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        try {
            final Toolbar t = findViewById(_activityUtils.getResId(GsContextUtils.ResType.ID, "toolbar"));
            if (t != null) {
                t.setTitle(title);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Set the Activity {@link Toolbar}, or more specific it's parent AppBarLayout to visible/gone
     *
     * @param visible Show toolbar or not
     */
    public void setToolbarVisible(boolean visible) {
        try {
            final int toolbarResId = _activityUtils.getResId(GsContextUtils.ResType.ID, "toolbar");
            LinearLayout appBarLayout = ((LinearLayout) findViewById(toolbarResId).getParent());
            if (!visible && m_initialToolbarHeight == 0) {
                m_initialToolbarHeight = appBarLayout.getMeasuredHeight();
            }
            ViewGroup.LayoutParams lp = appBarLayout.getLayoutParams();
            lp.height = visible ? m_initialToolbarHeight : 0;
            appBarLayout.setLayoutParams(lp);
        } catch (Exception ignored) {
        }
    }
}
