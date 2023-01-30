package net.gsantner.markor.activity;

import android.content.Context;

import androidx.annotation.Nullable;

import net.gsantner.markor.ApplicationObject;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.markor.util.MarkorContextUtils;
import net.gsantner.opoc.frontend.base.GsFragmentBase;
import net.gsantner.opoc.util.GsContextUtils;
import net.gsantner.opoc.wrapper.GsCallback;

import java.io.File;

public abstract class MarkorBaseFragment extends GsFragmentBase<AppSettings, MarkorContextUtils> {

    @Nullable
    @Override
    public AppSettings createAppSettingsInstance(Context applicationContext) {
        return ApplicationObject.settings();
    }

    @Nullable
    @Override
    public MarkorContextUtils createContextUtilsInstance(Context applicationContext) {
        return new MarkorContextUtils(applicationContext);
    }
}
