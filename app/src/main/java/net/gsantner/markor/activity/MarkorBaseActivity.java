package net.gsantner.markor.activity;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import net.gsantner.markor.R;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.markor.util.MarkorContextUtils;
import net.gsantner.opoc.frontend.base.GsActivityBase;
import net.gsantner.opoc.frontend.base.GsFragmentBase;

public abstract class MarkorBaseActivity extends GsActivityBase<AppSettings, MarkorContextUtils> {
    private int _cachedBarBackground = Integer.MIN_VALUE;
    private int _cachedBarContent = Integer.MIN_VALUE;
    private ColorStateList _cachedBottomNavColors;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

    @Override
    protected void onResume() {
        super.onResume();
        applyThemedBars();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        applyThemedBars();
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
        return _appSettings.getAppThemeName().contains("black") ? Color.BLACK : null;
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

    private void applyThemedBars() {
        refreshCachedBarColors();
        final int barBackground = _cachedBarBackground;
        final int actionBarBackground = getActionBarBackgroundColor();
        final int actionBarContent = getActionBarContentColor();

        final Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setBackgroundColor(actionBarBackground);
            toolbar.setTitleTextColor(actionBarContent);
            toolbar.setSubtitleTextColor(actionBarContent);
            toolbar.setPopupTheme(getPopupTheme());
            toolbar.setNavigationIcon(_cu.tintDrawable(toolbar.getNavigationIcon(), actionBarContent));
            toolbar.setOverflowIcon(_cu.tintDrawable(toolbar.getOverflowIcon(), actionBarContent));
            _cu.tintMenuItems(toolbar.getMenu(), true, actionBarContent);
        }

        final BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_bar);
        if (bottomNav != null) {
            bottomNav.setBackgroundColor(barBackground);
            bottomNav.setItemBackground(new ColorDrawable(barBackground));
            bottomNav.setItemTextColor(_cachedBottomNavColors);
            bottomNav.setItemIconTintList(_cachedBottomNavColors);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(actionBarBackground);
            getWindow().setNavigationBarColor(barBackground);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final View decorView = getWindow().getDecorView();
            int flags = decorView.getSystemUiVisibility();
            final boolean lightStatusIcons = _cu.shouldColorOnTopBeLight(actionBarBackground);
            if (!lightStatusIcons) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                final boolean lightNavigationIcons = _cu.shouldColorOnTopBeLight(barBackground);
                if (!lightNavigationIcons) {
                    flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                } else {
                    flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
            }
            decorView.setSystemUiVisibility(flags);
        }
    }

    private int getThemedBarBackgroundColor() {
        if (_appSettings.getAppThemeName().contains("black")) {
            return Color.BLACK;
        }
        return ContextCompat.getColor(this, R.color.bar_background);
    }

    private int getActionBarBackgroundColor() {
        if (_appSettings.getAppThemeName().contains("black")) {
            return Color.BLACK;
        }
        return ContextCompat.getColor(this, R.color.action_bar_background);
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

    private void refreshCachedBarColors() {
        final int background = getThemedBarBackgroundColor();
        final int content = ContextCompat.getColor(this, R.color.bar_content);
        if (background == _cachedBarBackground && content == _cachedBarContent && _cachedBottomNavColors != null) {
            return;
        }
        _cachedBarBackground = background;
        _cachedBarContent = content;
        _cachedBottomNavColors = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{},
                },
                new int[]{
                        ContextCompat.getColor(this, R.color.accent),
                        content,
                }
        );
    }
}
