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
 * Get updates:
 *  https://github.com/gsantner/onePieceOfCode/blob/master/java/AppSettingsBase.java
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

package io.github.gsantner.opoc.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Wrapper for settings based on SharedPreferences
 * with keys in resources
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
    protected final Context _context;

    public AppSettingsBase(final Context context) {
        this(context, SHARED_PREF_APP);
    }

    public AppSettingsBase(final Context context, final String prefAppName) {
        _context = context.getApplicationContext();
        _prefApp = _context.getSharedPreferences(prefAppName, Context.MODE_PRIVATE);
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
    //## Getter & Setter for settings
    //#################################
    public void setString(@StringRes int keyResourceId, String value) {
        setString(_prefApp, keyResourceId, value);
    }

    public void setString(final SharedPreferences pref, @StringRes int keyResourceId, String value) {
        pref.edit().putString(rstr(keyResourceId), value).apply();
    }

    public String getString(@StringRes int keyResourceId, String defaultValue) {
        return getString(_prefApp, keyResourceId, defaultValue);
    }

    public String getString(final SharedPreferences pref, @StringRes int keyResourceId, String defaultValue) {
        return pref.getString(rstr(keyResourceId), defaultValue);
    }

    public String getString(@StringRes int keyResourceId, @StringRes int keyResourceIdDefaultValue) {
        return getString(_prefApp, keyResourceId, keyResourceIdDefaultValue);
    }

    public String getString(final SharedPreferences pref, @StringRes int keyResourceId, @StringRes int keyResourceIdDefaultValue) {
        return pref.getString(rstr(keyResourceId), rstr(keyResourceIdDefaultValue));
    }

    public void setStringArray(@StringRes int keyResourceId, Object[] values) {
        setStringArray(_prefApp, keyResourceId, values);
    }

    public void setStringArray(final SharedPreferences pref, @StringRes int keyResourceId, Object[] values) {
        StringBuilder sb = new StringBuilder();
        for (Object value : values) {
            sb.append(ARRAY_SEPARATOR);
            sb.append(value.toString().replace(ARRAY_SEPARATOR, ARRAY_SEPARATOR_SUBSTITUTE));
        }
        setString(pref, keyResourceId, sb.toString().replaceFirst(ARRAY_SEPARATOR, ""));
    }

    @NonNull
    public String[] getStringArray(@StringRes int keyResourceId) {
        return getStringArray(_prefApp, keyResourceId);
    }

    @NonNull
    public String[] getStringArray(final SharedPreferences pref, @StringRes int keyResourceId) {
        String value = pref.getString(rstr(keyResourceId), ARRAY_SEPARATOR).replace(ARRAY_SEPARATOR_SUBSTITUTE, ARRAY_SEPARATOR);
        if (value.equals(ARRAY_SEPARATOR)) {
            return new String[0];
        }
        return value.split(ARRAY_SEPARATOR);
    }

    public void setStringList(@StringRes int keyResourceId, List<String> values) {
        setStringList(_prefApp, keyResourceId, values);
    }

    public void setStringList(final SharedPreferences pref, @StringRes int keyResourceId, List<String> values) {
        setStringArray(pref, keyResourceId, values.toArray(new String[values.size()]));
    }

    public ArrayList<String> getStringList(@StringRes int keyResourceId) {
        return getStringList(_prefApp, keyResourceId);
    }

    public ArrayList<String> getStringList(final SharedPreferences pref, @StringRes int keyResourceId) {
        return new ArrayList<>(Arrays.asList(getStringArray(pref, keyResourceId)));
    }

    public void setLong(@StringRes int keyResourceId, long value) {
        setLong(_prefApp, keyResourceId, value);
    }

    public void setLong(final SharedPreferences pref, @StringRes int keyResourceId, long value) {
        pref.edit().putLong(rstr(keyResourceId), value).apply();
    }

    public long getLong(@StringRes int keyResourceId, long defaultValue) {
        return getLong(_prefApp, keyResourceId, defaultValue);
    }

    public long getLong(final SharedPreferences pref, @StringRes int keyResourceId, long defaultValue) {
        return pref.getLong(rstr(keyResourceId), defaultValue);
    }

    public void setBool(@StringRes int keyResourceId, boolean value) {
        setBool(_prefApp, keyResourceId, value);
    }

    public void setBool(final SharedPreferences pref, @StringRes int keyResourceId, boolean value) {
        pref.edit().putBoolean(rstr(keyResourceId), value).apply();
    }

    public boolean getBool(@StringRes int keyResourceId, boolean defaultValue) {
        return getBool(_prefApp, keyResourceId, defaultValue);
    }

    public boolean getBool(final SharedPreferences pref, @StringRes int keyResourceId, boolean defaultValue) {
        return pref.getBoolean(rstr(keyResourceId), defaultValue);
    }

    public int getColor(String key, int defaultColor) {
        return getColor(_prefApp, key, defaultColor);
    }

    public int getColor(final SharedPreferences pref, String key, int defaultColor) {
        return pref.getInt(key, defaultColor);
    }

    public int getColor(@StringRes int keyResourceId, int defaultColor) {
        return getColor(_prefApp, keyResourceId, defaultColor);
    }

    public int getColor(final SharedPreferences pref, @StringRes int keyResourceId, int defaultColor) {
        return pref.getInt(rstr(keyResourceId), defaultColor);
    }

    public void setDouble(@StringRes int keyResId, double value) {
        setDouble(_prefApp, keyResId, value);
    }

    public void setDouble(final SharedPreferences pref, @StringRes int keyResId, double value) {
        _prefApp.edit().putLong(rstr(keyResId), Double.doubleToRawLongBits(value)).apply();
    }

    public double getDouble(@StringRes int keyResId, double defaultValue) {
        return getDouble(_prefApp, keyResId, defaultValue);
    }

    public double getDouble(final SharedPreferences pref, @StringRes int keyResId, double defaultValue) {
        return Double.longBitsToDouble(_prefApp.getLong(rstr(keyResId), Double.doubleToLongBits(defaultValue)));
    }

    public int getIntOfStringPref(@StringRes int keyResId, int defaultValue) {
        String strNum = _prefApp.getString(_context.getString(keyResId), Integer.toString(defaultValue));
        return Integer.valueOf(strNum);
    }

    public void setInt(@StringRes int keyResourceId, int value) {
        setInt(_prefApp, keyResourceId, value);
    }

    public void setInt(final SharedPreferences pref, @StringRes int keyResourceId, int value) {
        pref.edit().putInt(rstr(keyResourceId), value).apply();
    }

    public int getInt(@StringRes int keyResourceId, int defaultValue) {
        return getInt(_prefApp, keyResourceId, defaultValue);
    }

    public int getInt(final SharedPreferences pref, @StringRes int keyResourceId, int defaultValue) {
        return pref.getInt(rstr(keyResourceId), defaultValue);
    }

    public void setIntList(@StringRes int keyResId, List<Integer> values) {
        setIntList(_prefApp, keyResId, values);
    }

    public void setIntList(final SharedPreferences pref, @StringRes int keyResId, List<Integer> values) {
        StringBuilder sb = new StringBuilder();
        for (int value : values) {
            sb.append(ARRAY_SEPARATOR);
            sb.append(Integer.toString(value));
        }
        setString(_prefApp, keyResId, sb.toString().replaceFirst(ARRAY_SEPARATOR, ""));
    }

    @NonNull
    public ArrayList<Integer> getIntList(@StringRes int keyResId) {
        return getIntList(_prefApp, keyResId);
    }

    @NonNull
    public ArrayList<Integer> getIntList(final SharedPreferences pref, @StringRes int keyResId) {
        final ArrayList<Integer> ret = new ArrayList<>();
        final String value = getString(_prefApp, keyResId, ARRAY_SEPARATOR);
        if (value.equals(ARRAY_SEPARATOR)) {
            return ret;
        }
        for (String intstr : value.split(ARRAY_SEPARATOR)) {
            ret.add(Integer.parseInt(intstr));
        }
        return ret;
    }
}
