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
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import net.gsantner.opoc.preference.SharedPreferencesPropertyBackend;
import net.gsantner.opoc.util.ActivityUtils;
import net.gsantner.opoc.util.Callback;

public abstract class GsActivityBase<AS extends SharedPreferencesPropertyBackend> extends AppCompatActivity {

    protected AS _appSettings;
    protected Bundle _savedInstanceState;
    protected ActivityUtils _activityUtils;

    private final Callback.a0 m_setActivityBackgroundColor = () -> new ActivityUtils(GsActivityBase.this).setActivityBackgroundColor(getNewActivityBackgroundColor()).freeContextRef();
    private final Callback.a0 m_setActivityNavigationBarColor = () -> new ActivityUtils(GsActivityBase.this).setActivityNavigationBarBackgroundColor(getNewNavigationBarColor()).freeContextRef();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _savedInstanceState = savedInstanceState;
        _appSettings = createAppSettingsInstance(this);
        _activityUtils = new ActivityUtils(this);

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
}
