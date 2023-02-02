package net.gsantner.markor.activity;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.gsantner.markor.ApplicationObject;
import net.gsantner.markor.R;
import net.gsantner.markor.frontend.settings.MarkorPermissionChecker;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.markor.util.MarkorContextUtils;
import net.gsantner.opoc.frontend.base.GsActivityBase;
import net.gsantner.opoc.util.GsContextUtils;
import net.gsantner.opoc.wrapper.GsCallback;

import java.io.File;

public abstract class MarkorBaseActivity extends GsActivityBase<AppSettings, MarkorContextUtils> {

    private final GsCallback.a1<GsCallback.a0> _permCallback = GsContextUtils.instance.createFilePermissionCallback(this);

    public final boolean testFilePermission(final @NonNull File f, final GsCallback.a0 callback) {
        if (!f.canWrite() && !GsContextUtils.instance.checkExternalStoragePermission(this)) {
            _permCallback.callback(callback);
            return false;
        }
        return true;
    }

    public final boolean testFilePermission(final @NonNull File f, final GsCallback.a0 yes, final GsCallback.a0 no) {
        return testFilePermission(f, () -> {
            if (GsContextUtils.instance.checkExternalStoragePermission(this) && yes != null) {
                yes.callback();
            } else if (no != null) {
                no.callback();
            }
        });
    }

    @Override
    protected void onPreCreate(@Nullable Bundle savedInstanceState) {
        super.onPreCreate(savedInstanceState); // _appSettings, _cu gets available
        setTheme(R.style.AppTheme_Unified);
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
