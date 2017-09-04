package io.github.gsantner.marowni.util;

import android.content.Context;

import io.github.gsantner.marowni.App;
import io.github.gsantner.opoc.util.AppSettingsBase;

public class AppSettings extends AppSettingsBase {
    private AppSettings(Context _context) {
        super(_context);
    }

    public static AppSettings get() {
        return new AppSettings(App.get());
    }
}
