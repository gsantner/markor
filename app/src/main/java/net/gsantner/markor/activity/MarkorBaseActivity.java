package net.gsantner.markor.activity;

import static android.view.WindowManager.LayoutParams.FLAG_SECURE;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;

import net.gsantner.markor.util.AppSettings;
import net.gsantner.opoc.activity.GsActivityBase;

public abstract class MarkorBaseActivity extends GsActivityBase<AppSettings> {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Protect window from screenshots
        if (_appSettings.isPrivacyModeOn()) {
            getWindow().setFlags(FLAG_SECURE, FLAG_SECURE);
        } else {
            getWindow().clearFlags(FLAG_SECURE);
        }
    }

    @Override
    public Integer getNewNavigationBarColor() {
        return _activityUtils.parseColor(_appSettings.getNavigationBarColor());
    }

    @Override
    public AppSettings createAppSettingsInstance(Context applicationContext) {
        return new AppSettings(applicationContext);
    }
}
