package io.github.gsantner.marowni.util;

import android.content.Context;

import io.github.gsantner.marowni.App;
import io.github.gsantner.marowni.R;
import io.github.gsantner.opoc.util.AppSettingsBase;

public class AppSettings extends AppSettingsBase {
    private AppSettings(Context _context) {
        super(_context);
    }

    public static AppSettings get() {
        return new AppSettings(App.get());
    }

    public boolean isDarkTheme() {
        return getString(R.string.pref_theme_key, "").equals(rstr(R.string.theme_dark));
    }

    public int getBackgroundColor() {
        return isDarkTheme() ? rcolor(R.color.dark__background) : rcolor(R.color.light__background);
    }
}
