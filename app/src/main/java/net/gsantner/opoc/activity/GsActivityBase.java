package net.gsantner.opoc.activity;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import net.gsantner.opoc.preference.SharedPreferencesPropertyBackend;
import net.gsantner.opoc.util.ActivityUtils;

public abstract class GsActivityBase<AS extends SharedPreferencesPropertyBackend> extends AppCompatActivity {

    protected AS _appSettings;
    protected Bundle _savedInstanceState;
    protected ActivityUtils _activityUtils;

    @ColorInt
    protected Integer _defaultNavigationBarColor = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _savedInstanceState = savedInstanceState;
        _appSettings = createAppSettingsInstance(getApplicationContext());
        _activityUtils = new ActivityUtils(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && _defaultNavigationBarColor == null) {
            _defaultNavigationBarColor = getWindow().getNavigationBarColor();
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

        Integer color = getNewNavigationBarColor();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && color != null) {
            getWindow().setNavigationBarColor(color);
        }
    }

    @ColorInt
    public Integer getNewNavigationBarColor() {
        return null;
    }
}
