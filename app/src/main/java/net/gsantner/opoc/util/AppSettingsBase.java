/*
 * ------------------------------------------------------------------------------
 * Gregor Santner <gsantner.github.io> wrote this. You can do whatever you want
 * with it. If we meet some day, and you think it is worth it, you can buy me a
 * coke in return. Provided as is without any kind of warranty. Do not blame or
 * sue me if something goes wrong. No attribution required.    - Gregor Santner
 *
 * License: Creative Commons Zero (CC0 1.0)
 *  http://creativecommons.org/publicdomain/zero/1.0/
 * ----------------------------------------------------------------------------
 */

/*
 * This is a wrapper for settings based on SharedPreferences
 * with keys in resources. Extend from this class and add
 * getters/setters for the app's settings.
 * Example:
    public boolean isAppFirstStart(boolean doSet) {
        boolean value = getBool(prefApp, R.string.pref_key__app_first_start, true);
        if (doSet) {
            setBool(prefApp, R.string.pref_key__app_first_start, false);
        }
        return value;
    }

    public boolean isAppCurrentVersionFirstStart(boolean doSet) {
        int value = getInt(prefApp, R.string.pref_key__app_first_start_current_version, -1);
        if (doSet) {
            setInt(prefApp, R.string.pref_key__app_first_start_current_version, BuildConfig.VERSION_CODE);
        }
        return value != BuildConfig.VERSION_CODE && !BuildConfig.IS_TEST_BUILD;
    }

 * Maybe add a singleton for this:
 * Whereas App.get() is returning ApplicationContext
    private AppSettings(Context _context) {
        super(_context);
    }

    public static AppSettings get() {
        return new AppSettings(App.get());
    }
 */

package net.gsantner.opoc.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Wrapper for settings based on SharedPreferences with keys in resources
 * Default SharedPreference (_prefApp) will be taken if no SP is specified, else the first one
 */
@SuppressWarnings({"WeakerAccess", "unused", "SpellCheckingInspection", "SameParameterValue"})
public class AppSettingsBase {
    protected static final String ARRAY_SEPARATOR = "%%%";
    protected static final String ARRAY_SEPARATOR_SUBSTITUTE = "§§§";
    public static final String SHARED_PREF_APP = "app";

    //########################
    //## Members, Constructors
    //########################
    protected final SharedPreferences _prefApp;
    protected final String _prefAppName;
    protected final Context _context;

    public AppSettingsBase(final Context context) {
        this(context, SHARED_PREF_APP);
    }

    public AppSettingsBase(final Context context, final String prefAppName) {
        _context = context.getApplicationContext();
        _prefAppName = TextUtils.isEmpty(prefAppName) ?
                _context.getPackageName() + "_preferences" : prefAppName;
        _prefApp = _context.getSharedPreferences(_prefAppName, Context.MODE_PRIVATE);
    }

    //#####################
    //## Methods
    //#####################
    public Context getContext() {
        return _context;
    }

    public boolean isKeyEqual(String key, int stringKeyResourceId) {
        return key.equals(rstr(stringKeyResourceId));
    }

    public void resetSettings() {
        resetSettings(_prefApp);
    }

    @SuppressLint("ApplySharedPref")
    public void resetSettings(final SharedPreferences pref) {
        pref.edit().clear().commit();
    }

    public boolean isPrefSet(@StringRes int stringKeyResourceId) {
        return isPrefSet(_prefApp, stringKeyResourceId);
    }

    public boolean isPrefSet(final SharedPreferences pref, @StringRes int stringKeyResourceId) {
        return pref.contains(rstr(stringKeyResourceId));
    }

    public void registerPreferenceChangedListener(SharedPreferences.OnSharedPreferenceChangeListener value) {
        registerPreferenceChangedListener(_prefApp, value);
    }

    public void registerPreferenceChangedListener(final SharedPreferences pref, SharedPreferences.OnSharedPreferenceChangeListener value) {
        pref.registerOnSharedPreferenceChangeListener(value);
    }

    public void unregisterPreferenceChangedListener(SharedPreferences.OnSharedPreferenceChangeListener value) {
        unregisterPreferenceChangedListener(_prefApp, value);
    }

    public void unregisterPreferenceChangedListener(final SharedPreferences pref, SharedPreferences.OnSharedPreferenceChangeListener value) {
        pref.unregisterOnSharedPreferenceChangeListener(value);
    }

    public SharedPreferences getDefaultPreferences() {
        return _prefApp;
    }

    public SharedPreferences.Editor getDefaultPreferencesEditor() {
        return _prefApp.edit();
    }

    public String getDefaultPreferencesName() {
        return _prefAppName;
    }


    private SharedPreferences gp(final SharedPreferences... pref) {
        return (pref != null && pref.length > 0 ? pref[0] : _prefApp);
    }

    //#################################
    //## Getter for resources
    //#################################
    public String rstr(@StringRes int stringKeyResourceId) {
        return _context.getString(stringKeyResourceId);
    }

    public int rcolor(@ColorRes int resColorId) {
        return ContextCompat.getColor(_context, resColorId);
    }


    //#################################
    //## Getter & Setter for String
    //#################################
    public void setString(@StringRes int keyResourceId, String value, final SharedPreferences... pref) {
        gp(pref).edit().putString(rstr(keyResourceId), value).apply();
    }

    public void setString(String key, String value, final SharedPreferences... pref) {
        gp(pref).edit().putString(key, value).apply();
    }

    public void setString(@StringRes int keyResourceId, @StringRes int defaultValueResourceId, final SharedPreferences... pref) {
        gp(pref).edit().putString(rstr(keyResourceId), rstr(defaultValueResourceId)).apply();
    }

    public String getString(@StringRes int keyResourceId, String defaultValue, final SharedPreferences... pref) {
        return gp(pref).getString(rstr(keyResourceId), defaultValue);
    }

    public String getString(@StringRes int keyResourceId, @StringRes int defaultValueResourceId, final SharedPreferences... pref) {
        return gp(pref).getString(rstr(keyResourceId), rstr(defaultValueResourceId));
    }

    public String getString(String key, String defaultValue, final SharedPreferences... pref) {
        return gp(pref).getString(key, defaultValue);
    }

    public String getString(@StringRes int keyResourceId, String defaultValue, @StringRes int keyResourceIdDefaultValue, final SharedPreferences... pref) {
        return gp(pref).getString(rstr(keyResourceId), rstr(keyResourceIdDefaultValue));
    }

    public void setStringArray(@StringRes int keyResourceId, Object[] values, final SharedPreferences... pref) {
        setStringArray(rstr(keyResourceId), values, gp(pref));
    }

    public void setStringArray(String key, Object[] values, final SharedPreferences... pref) {
        setStringArray(key, values, gp(pref));
    }

    private void setStringArray(String key, Object[] values, final SharedPreferences pref) {
        StringBuilder sb = new StringBuilder();
        for (Object value : values) {
            sb.append(ARRAY_SEPARATOR);
            sb.append(value.toString().replace(ARRAY_SEPARATOR, ARRAY_SEPARATOR_SUBSTITUTE));
        }
        setString(key, sb.toString().replaceFirst(ARRAY_SEPARATOR, ""), pref);
    }

    @NonNull
    public String[] getStringArray(@StringRes int keyResourceId, final SharedPreferences... pref) {
        return getStringArray(rstr(keyResourceId), gp(pref));
    }

    private String[] getStringArray(String key, final SharedPreferences... pref) {
        String value = gp(pref)
                .getString(key, ARRAY_SEPARATOR)
                .replace(ARRAY_SEPARATOR_SUBSTITUTE, ARRAY_SEPARATOR);
        if (value.equals(ARRAY_SEPARATOR)) {
            return new String[0];
        }
        return value.split(ARRAY_SEPARATOR);
    }

    public void setStringList(@StringRes int keyResourceId, List<String> values, final SharedPreferences... pref) {
        setStringArray(rstr(keyResourceId), values.toArray(new String[values.size()]), pref);
    }

    public void setStringList(String key, List<String> values, final SharedPreferences... pref) {
        setStringArray(key, values.toArray(new String[values.size()]), pref);
    }

    public ArrayList<String> getStringList(@StringRes int keyResourceId, final SharedPreferences... pref) {
        return new ArrayList<>(Arrays.asList(getStringArray(rstr(keyResourceId), gp(pref))));
    }

    public ArrayList<String> getStringList(String key, final SharedPreferences... pref) {
        return new ArrayList<>(Arrays.asList(getStringArray(key, gp(pref))));
    }

    //#################################
    //## Getter & Setter for integer
    //#################################
    public void setInt(@StringRes int keyResourceId, int value, final SharedPreferences... pref) {
        gp(pref).edit().putInt(rstr(keyResourceId), value).apply();
    }

    public void setInt(String key, int value, final SharedPreferences... pref) {
        gp(pref).edit().putInt(key, value).apply();
    }

    public int getInt(@StringRes int keyResourceId, int defaultValue, final SharedPreferences... pref) {
        return gp(pref).getInt(rstr(keyResourceId), defaultValue);
    }

    public int getInt(String key, int defaultValue, final SharedPreferences... pref) {
        return gp(pref).getInt(key, defaultValue);
    }

    public int getIntOfStringPref(@StringRes int keyResId, int defaultValue, final SharedPreferences... pref) {
        return getIntOfStringPref(rstr(keyResId), defaultValue, gp(pref));
    }

    public int getIntOfStringPref(String key, int defaultValue, final SharedPreferences... pref) {
        String strNum = getString(key, Integer.toString(defaultValue), gp(pref));
        return Integer.valueOf(strNum);
    }


    public void setIntArray(@StringRes int keyResourceId, Object[] values, final SharedPreferences... pref) {
        setIntArray(rstr(keyResourceId), values, gp(pref));
    }

    public void setIntArray(String key, Object[] values, final SharedPreferences... pref) {
        setIntArray(key, values, gp(pref));
    }

    private void setIntArray(String key, Object[] values, final SharedPreferences pref) {
        StringBuilder sb = new StringBuilder();
        for (Object value : values) {
            sb.append(ARRAY_SEPARATOR);
            sb.append(value.toString());
        }
        setString(key, sb.toString().replaceFirst(ARRAY_SEPARATOR, ""), pref);
    }

    @NonNull
    public Integer[] getIntArray(@StringRes int keyResourceId, final SharedPreferences... pref) {
        return getIntArray(rstr(keyResourceId), gp(pref));
    }

    private Integer[] getIntArray(String key, final SharedPreferences... pref) {
        String value = gp(pref).getString(key, ARRAY_SEPARATOR);
        if (value.equals(ARRAY_SEPARATOR)) {
            return new Integer[0];
        }
        String[] split = value.split(ARRAY_SEPARATOR);
        Integer[] ret = new Integer[split.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = Integer.parseInt(split[i]);
        }
        return ret;
    }

    public void setIntList(@StringRes int keyResourceId, List<Integer> values, final SharedPreferences... pref) {
        setIntArray(rstr(keyResourceId), values.toArray(new Integer[values.size()]), pref);
    }

    public void setIntList(String key, List<Integer> values, final SharedPreferences... pref) {
        setIntArray(key, values.toArray(new Integer[values.size()]), pref);
    }

    public ArrayList<Integer> getIntList(@StringRes int keyResourceId, final SharedPreferences... pref) {
        return new ArrayList<>(Arrays.asList(getIntArray(rstr(keyResourceId), gp(pref))));
    }

    public ArrayList<Integer> getIntList(String key, final SharedPreferences... pref) {
        return new ArrayList<>(Arrays.asList(getIntArray(key, gp(pref))));
    }


    //#################################
    //## Getter & Setter for Long
    //#################################
    public void setLong(@StringRes int keyResourceId, long value, final SharedPreferences... pref) {
        gp(pref).edit().putLong(rstr(keyResourceId), value).apply();
    }

    public void setLong(String key, long value, final SharedPreferences... pref) {
        gp(pref).edit().putLong(key, value).apply();
    }

    public long getLong(@StringRes int keyResourceId, long defaultValue, final SharedPreferences... pref) {
        return gp(pref).getLong(rstr(keyResourceId), defaultValue);
    }

    public long getLong(String key, long defaultValue, final SharedPreferences... pref) {
        return gp(pref).getLong(key, defaultValue);
    }

    //#################################
    //## Getter & Setter for Float
    //#################################
    public void setFloat(@StringRes int keyResourceId, float value, final SharedPreferences... pref) {
        gp(pref).edit().putFloat(rstr(keyResourceId), value).apply();
    }

    public void setFloat(String key, float value, final SharedPreferences... pref) {
        gp(pref).edit().putFloat(key, value).apply();
    }

    public float getFloat(@StringRes int keyResourceId, float defaultValue, final SharedPreferences... pref) {
        return gp(pref).getFloat(rstr(keyResourceId), defaultValue);
    }

    public float getFloat(String key, float defaultValue, final SharedPreferences... pref) {
        return gp(pref).getFloat(key, defaultValue);
    }

    //#################################
    //## Getter & Setter for Double
    //#################################
    public void setDouble(@StringRes int keyResourceId, double value, final SharedPreferences... pref) {
        setLong(rstr(keyResourceId), Double.doubleToRawLongBits(value));
    }

    public void setDouble(String key, double value, final SharedPreferences... pref) {
        setLong(key, Double.doubleToRawLongBits(value));
    }

    public double getDouble(@StringRes int keyResourceId, double defaultValue, final SharedPreferences... pref) {
        return getDouble(rstr(keyResourceId), defaultValue, gp(pref));
    }

    public double getDouble(String key, double defaultValue, final SharedPreferences... pref) {
        return Double.longBitsToDouble(getLong(key, Double.doubleToRawLongBits(defaultValue), gp(pref)));
    }

    //#################################
    //## Getter & Setter for boolean
    //#################################
    public void setBool(@StringRes int keyResourceId, boolean value, final SharedPreferences... pref) {
        gp(pref).edit().putBoolean(rstr(keyResourceId), value).apply();
    }

    public void setBool(String key, boolean value, final SharedPreferences... pref) {
        gp(pref).edit().putBoolean(key, value).apply();
    }

    public boolean getBool(@StringRes int keyResourceId, boolean defaultValue, final SharedPreferences... pref) {
        return gp(pref).getBoolean(rstr(keyResourceId), defaultValue);
    }

    public boolean getBool(String key, boolean defaultValue, final SharedPreferences... pref) {
        return gp(pref).getBoolean(key, defaultValue);
    }

    //#################################
    //## Getter & Setter for Color
    //#################################
    public int getColor(String key, @ColorRes int defaultColor, final SharedPreferences... pref) {
        return gp(pref).getInt(key, rcolor(defaultColor));
    }

    public int getColor(@StringRes int keyResourceId, @ColorRes int defaultColor, final SharedPreferences... pref) {
        return gp(pref).getInt(rstr(keyResourceId), rcolor(defaultColor));
    }
}
