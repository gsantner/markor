package net.gsantner.markor.activity;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import net.gsantner.markor.ApplicationObject;
import net.gsantner.markor.R;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.markor.util.MarkorContextUtils;
import net.gsantner.opoc.frontend.base.GsActivityBase;
import net.gsantner.opoc.frontend.base.GsFragmentBase;

public abstract class MarkorBaseActivity extends GsActivityBase<AppSettings, MarkorContextUtils> {

    @Override
    protected void onPreCreate(@Nullable Bundle savedInstanceState) {
        super.onPreCreate(savedInstanceState); // _appSettings, _cu gets available
        //setTheme(R.style.AppTheme_Unified);

        if (_appSettings.getAppThemeName().contains("default")) {
            setTheme(R.style.AppTheme_Unified);
        }

        if (_appSettings.getAppAccentName().contains("black_white")) {
            setTheme(R.style.BlackWhite);
        }

        if (_appSettings.getAppAccentName().contains("white_black")) {
            setTheme(R.style.WhiteBlack);
        }

        if (_appSettings.getAppAccentName().contains("black_aqua")) {
            setTheme(R.style.BlackAqua);
        }

        if (_appSettings.getAppAccentName().contains("black_green")) {
            setTheme(R.style.BlackGreen);
        }

        if (_appSettings.getAppAccentName().contains("sepia")) {
            setTheme(R.style.Sepia);
        }

        if (_appSettings.getAppAccentName().contains("nord")) {
            setTheme(R.style.Nord);
        }

        _appSettings.applyAppTheme();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setEnterTransition(null);
            getWindow().setExitTransition(null);
        }
        _cu.setAppLanguage(this, _appSettings.getLanguage());
        if (_appSettings.isHideSystemStatusbar()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    protected boolean onReceiveKeyPress(GsFragmentBase fragment, int keyCode, KeyEvent event) {
        return fragment.onReceiveKeyPress(keyCode, event);
    }

    @Override
    public Integer getNewNavigationBarColor() {
        return _cu.parseHexColorString(_appSettings.getNavigationBarColor());
    }

    @Override
    public Integer getNewActivityBackgroundColor() {
        return _appSettings.getAppThemeName().contains("black") ? Color.BLACK : null;
    }

    @Override
    public AppSettings createAppSettingsInstance(Context applicationContext) {
        return ApplicationObject.settings();
    }

    @Override
    public MarkorContextUtils createContextUtilsInstance(Context applicationContext) {
        return new MarkorContextUtils(applicationContext);
    }

    @Override
    public Boolean isFlagSecure() {
        return _appSettings.isDisallowScreenshots();
    }
}
