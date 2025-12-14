package net.gsantner.markor.activity;

import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.util.TypedValue;
import android.graphics.drawable.ColorDrawable;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import net.gsantner.markor.R;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.markor.util.MarkorContextUtils;
import net.gsantner.opoc.frontend.base.GsActivityBase;
import net.gsantner.opoc.frontend.base.GsFragmentBase;

public abstract class MarkorBaseActivity extends GsActivityBase<AppSettings, MarkorContextUtils> {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        _appSettings.applyAppTheme();
        applyThemedBars();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setEnterTransition(null);
            getWindow().setExitTransition(null);
        }
        _cu.setAppLanguage(this, _appSettings.getLanguage());
        if (_appSettings.isHideSystemStatusbar()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        _cu.tintMenuItems(menu, true, getActionBarContentColor());
        return super.onPrepareOptionsMenu(menu);
    }

    protected boolean onReceiveKeyPress(GsFragmentBase fragment, int keyCode, KeyEvent event) {
        return fragment.onReceiveKeyPress(keyCode, event);
    }

    @Override
    public Integer getNewNavigationBarColor() {
        return getThemedBarBackgroundColor();
    }

    @Override
    public Integer getNewActivityBackgroundColor() {
        return getThemedBarBackgroundColor();
    }

    @Override
    protected AppSettings createAppSettingsInstance() {
        return new AppSettings(this);
    }

    @Override
    protected MarkorContextUtils createContextUtilsInstance() {
        return new MarkorContextUtils(this);
    }

    @Override
    public Boolean isFlagSecure() {
        return _appSettings.isDisallowScreenshots();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void applyThemedBars() {
        final int barBackground = getThemedBarBackgroundColor();
        final int actionBarBackground = getActionBarBackgroundColor();

        final Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setBackgroundColor(actionBarBackground);
            toolbar.setPopupTheme(getPopupTheme());
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final View decorView = getWindow().getDecorView();
            int flags = decorView.getSystemUiVisibility();
            flags = applyLightBarFlag(flags, actionBarBackground, View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                flags = applyLightBarFlag(flags, barBackground, View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
            }
            decorView.setSystemUiVisibility(flags);
        }
    }

    private int getThemedBarBackgroundColor() {
        return resolveThemeColor(android.R.attr.colorBackground, R.color.bar_background);
    }

    private int getActionBarBackgroundColor() {
        return resolveThemeColor(R.attr.colorPrimary, R.color.action_bar_background);
    }

    private int getActionBarContentColor() {
        return ContextCompat.getColor(this, R.color.action_bar_content);
    }

    private int getPopupTheme() {
        final boolean isBlack = _appSettings.getAppThemeName().contains("black");
        final boolean isDark = _cu.isDarkModeEnabled(this);
        if (isBlack) {
            return R.style.ToolbarPopupOverlayBlack;
        }
        return isDark ? R.style.ToolbarPopupOverlayDark : R.style.ToolbarPopupOverlay;
    }

    private int applyLightBarFlag(int flags, int backgroundColor, int lightFlag) {
        final boolean needsDarkIcons = !_cu.shouldColorOnTopBeLight(backgroundColor);
        if (needsDarkIcons) {
            flags |= lightFlag;
        } else {
            flags &= ~lightFlag;
        }
        return flags;
    }

    private int resolveThemeColor(int attrResId, int fallbackColorResId) {
        final TypedValue typedValue = new TypedValue();
        if (getTheme().resolveAttribute(attrResId, typedValue, true)) {
            return typedValue.data;
        }
        return ContextCompat.getColor(this, fallbackColorResId);
    }
}
