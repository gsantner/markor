package net.gsantner.markor.activity;

import android.content.Context;

import net.gsantner.markor.util.AppSettings;
import net.gsantner.opoc.activity.GsActivityBase;

public abstract class AppActivityBase extends GsActivityBase<AppSettings> {

    @Override
    public Integer getNewNavigationBarColor() {
        return _activityUtils.parseColor(_appSettings.getNavigationBarColor());
    }

    @Override
    public AppSettings createAppSettingsInstance(Context applicationContext) {
        return new AppSettings(applicationContext);
    }
}
