package net.gsantner.markor.activity;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import net.gsantner.markor.R;
import net.gsantner.markor.util.ActivityUtils;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.opoc.activity.GsActivityBase;

public abstract class MarkorBaseActivity extends GsActivityBase<AppSettings> {

    @Override
    protected void onPreCreate(@Nullable Bundle savedInstanceState) {
        super.onPreCreate(savedInstanceState); // _appSettings, _activityUtils gets available
        setTheme(R.style.AppTheme_Unified);
        _appSettings.applyAppTheme();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setEnterTransition(null);
            getWindow().setExitTransition(null);
        }
        _activityUtils.setAppLanguage(_appSettings.getLanguage()).freeContextRef(); // invalidates actvity references
        _activityUtils.freeContextRef();
        _activityUtils = new ActivityUtils(this);
        if (_appSettings.isHideSystemStatusbar()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    @Override
    public Integer getNewNavigationBarColor() {
        return _activityUtils.parseColor(_appSettings.getNavigationBarColor());
    }

    @Override
    public Integer getNewActivityBackgroundColor() {
        return _appSettings.getAppThemeName().contains("black") ? Color.BLACK : null;
    }

    @Override
    public AppSettings createAppSettingsInstance(Context applicationContext) {
        return new AppSettings(applicationContext);
    }

    @Override
    public Boolean isFlagSecure() {
        return _appSettings.isDisallowScreenshots();
    }
}
