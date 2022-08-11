/*#######################################################
 *
 *   Maintained by Gregor Santner, 2020-
 *   https://gsantner.net/
 *
 *   License: Apache 2.0 / Commercial
 *  https://github.com/gsantner/opoc/#licensing
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.opoc.activity;

import static android.view.WindowManager.LayoutParams.FLAG_SECURE;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import net.gsantner.opoc.preference.SharedPreferencesPropertyBackend;
import net.gsantner.opoc.util.ActivityUtils;
import net.gsantner.opoc.util.Callback;
import net.gsantner.opoc.util.ContextUtils;

public abstract class GsActivityBase<AS extends SharedPreferencesPropertyBackend> extends AppCompatActivity {

    protected AS _appSettings;
    protected Bundle _savedInstanceState;
    protected ActivityUtils _activityUtils;

    private final Callback.a0 m_setActivityBackgroundColor = () -> new ActivityUtils(GsActivityBase.this).setActivityBackgroundColor(getNewActivityBackgroundColor()).freeContextRef();
    private final Callback.a0 m_setActivityNavigationBarColor = () -> new ActivityUtils(GsActivityBase.this).setActivityNavigationBarBackgroundColor(getNewNavigationBarColor()).freeContextRef();

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
        _activityUtils = new ActivityUtils(this);
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
            final Toolbar t = findViewById(_activityUtils.getResId(ContextUtils.ResType.ID, "toolbar"));
            if (t != null) {
                t.setTitle(title);
            }
        } catch (Exception ignored) {
        }
    }
}
