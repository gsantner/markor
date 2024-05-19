/*#######################################################
 *
 * SPDX-FileCopyrightText: 2016-2024 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2018-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
#########################################################*/

/*
 * This is a wrapper for settings based on SharedPreferences
 * with keys in resources. Extend from this class and add
 * getters/setters for the app's settings.
 * Example:
    public boolean isAppFirstStart(boolean doSet) {
        int value = getInt(R.string.pref_key__app_first_start, -1);
        if (doSet) {
            setBool(true);
        }
        return value;
    }

    public boolean isAppCurrentVersionFirstStart(boolean doSet) {
        int value = getInt(R.string.pref_key__app_first_start_current_version, -1);
        if (doSet) {
            setInt(R.string.pref_key__app_first_start_current_version, BuildConfig.VERSION_CODE);
        }
        return value != BuildConfig.VERSION_CODE;
    }
 */

package net.gsantner.opoc.model;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;


/**
 * Wrapper for settings based on SharedPreferences, optionally with keys in resources
 * Default SharedPreference (_prefApp) will be taken if no SP is specified, else the first one
 */
@SuppressWarnings({"WeakerAccess", "unused", "SpellCheckingInspection", "SameParameterValue"})
public class GsSharedPreferencesPropertyBackend implements GsPropertyBackend<String, GsSharedPreferencesPropertyBackend> {
    protected static final String ARRAY_SEPARATOR = "%%%";
    protected static final String ARRAY_SEPARATOR_SUBSTITUTE = "§§§";
    public static final String SHARED_PREF_APP = "app";
    private static String _debugLog = "";

    //
    // Members, Constructors
    //
    protected SharedPreferences _prefApp;
    protected String _prefAppName;
    protected Context _context;

    public GsSharedPreferencesPropertyBackend init(final Context context) {
        return init(context, SHARED_PREF_APP);
    }

    public GsSharedPreferencesPropertyBackend init(final Context context, final String prefAppName) {
        _context = context;
        _prefAppName = !TextUtils.isEmpty(prefAppName) ? prefAppName : (_context.getPackageName() + "_preferences");
        _prefApp = _context.getSharedPreferences(_prefAppName, Context.MODE_PRIVATE);
        return this;
    }

    //
    // Methods
    //
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


    public static void limitListTo(final List<?> list, int maxSize, boolean removeDuplicates) {
        Object o;
        int pos;

        for (int i = 0; removeDuplicates && i < list.size(); i++) {
            o = list.get(i);
            while ((pos = list.lastIndexOf(o)) != i && pos >= 0) {
                list.remove(pos);
            }
        }
        while ((pos = list.size()) > maxSize && pos > 0) {
            list.remove(list.size() - 1);
        }
    }

    //
    // Getter for resources
    //
    public String rstr(@StringRes int stringKeyResourceId) {
        return _context.getString(stringKeyResourceId);
    }

    public int rcolor(@ColorRes int resColorId) {
        return ContextCompat.getColor(_context, resColorId);
    }

    public String[] rstrs(int... keyResourceIds) {
        String[] ret = new String[keyResourceIds.length];
        for (int i = 0; i < keyResourceIds.length; i++) {
            ret[i] = rstr(keyResourceIds[i]);
        }
        return ret;
    }


    //
    // Getter & Setter for String
    //
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
        try {
            return gp(pref).getString(key, defaultValue);
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    public String getString(@StringRes int keyResourceId, String defaultValue, @StringRes int keyResourceIdDefaultValue, final SharedPreferences... pref) {
        return getString(rstr(keyResourceId), rstr(keyResourceIdDefaultValue), pref);
    }

    private void setStringListOne(String key, List<String> values, final SharedPreferences pref) {
        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            sb.append(ARRAY_SEPARATOR);
            sb.append(value.replace(ARRAY_SEPARATOR, ARRAY_SEPARATOR_SUBSTITUTE));
        }
        setString(key, sb.toString().replaceFirst(ARRAY_SEPARATOR, ""), pref);
    }

    private ArrayList<String> getStringListOne(String key, final SharedPreferences pref) {
        ArrayList<String> ret = new ArrayList<>();
        String value = getString(key, ARRAY_SEPARATOR).replace(ARRAY_SEPARATOR_SUBSTITUTE, ARRAY_SEPARATOR);
        if (value.equals(ARRAY_SEPARATOR) || TextUtils.isEmpty(value)) {
            return ret;
        }
        ret.addAll(Arrays.asList(value.split(ARRAY_SEPARATOR)));
        return ret;
    }

    public void setStringArray(@StringRes int keyResourceId, String[] values, final SharedPreferences... pref) {
        setStringArray(rstr(keyResourceId), values, pref);
    }

    public void setStringArray(String key, String[] values, final SharedPreferences... pref) {
        setStringListOne(key, Arrays.asList(values), gp(pref));
    }

    public void setStringList(@StringRes int keyResourceId, List<String> values, final SharedPreferences... pref) {
        setStringArray(rstr(keyResourceId), values.toArray(new String[values.size()]), pref);
    }

    public void setStringList(String key, List<String> values, final SharedPreferences... pref) {
        setStringArray(key, values.toArray(new String[values.size()]), pref);
    }

    @NonNull
    public String[] getStringArray(@StringRes int keyResourceId, final SharedPreferences... pref) {
        return getStringArray(rstr(keyResourceId), pref);
    }

    @NonNull
    public String[] getStringArray(String key, final SharedPreferences... pref) {
        List<String> list = getStringListOne(key, gp(pref));
        return list.toArray(new String[list.size()]);
    }


    public ArrayList<String> getStringList(@StringRes int keyResourceId, final SharedPreferences... pref) {
        return getStringListOne(rstr(keyResourceId), gp(pref));
    }

    public ArrayList<String> getStringList(String key, final SharedPreferences... pref) {
        return getStringListOne(key, gp(pref));
    }

    //
    // Getter & Setter for integer
    //
    public void setInt(@StringRes int keyResourceId, int value, final SharedPreferences... pref) {
        gp(pref).edit().putInt(rstr(keyResourceId), value).apply();
    }

    public void setInt(String key, int value, final SharedPreferences... pref) {
        gp(pref).edit().putInt(key, value).apply();
    }

    public int getInt(@StringRes int keyResourceId, int defaultValue, final SharedPreferences... pref) {
        return getInt(rstr(keyResourceId), defaultValue, pref);
    }

    public int getInt(String key, int defaultValue, final SharedPreferences... pref) {
        try {
            return gp(pref).getInt(key, defaultValue);
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    public int getIntOfStringPref(@StringRes int keyResId, int defaultValue, final SharedPreferences... pref) {
        return getIntOfStringPref(rstr(keyResId), defaultValue, gp(pref));
    }

    public int getIntOfStringPref(String key, int defaultValue, final SharedPreferences... pref) {
        String strNum = getString(key, Integer.toString(defaultValue), gp(pref));
        return Integer.valueOf(strNum);
    }

    private void setIntListOne(String key, List<Integer> values, final SharedPreferences pref) {
        StringBuilder sb = new StringBuilder();
        for (Integer value : values) {
            sb.append(ARRAY_SEPARATOR);
            sb.append(value.toString());
        }
        setString(key, sb.toString().replaceFirst(ARRAY_SEPARATOR, ""), pref);
    }

    private ArrayList<Integer> getIntListOne(String key, final SharedPreferences pref) {
        ArrayList<Integer> ret = new ArrayList<>();
        String value = getString(key, ARRAY_SEPARATOR);
        if (value.equals(ARRAY_SEPARATOR)) {
            return ret;
        }
        for (String s : value.split(ARRAY_SEPARATOR)) {
            ret.add(Integer.parseInt(s));
        }
        return ret;
    }

    public void setIntArray(@StringRes int keyResourceId, Integer[] values, final SharedPreferences... pref) {
        setIntArray(rstr(keyResourceId), values, gp(pref));
    }

    public void setIntArray(String key, Integer[] values, final SharedPreferences... pref) {
        setIntListOne(key, Arrays.asList(values), gp(pref));
    }

    public Integer[] getIntArray(@StringRes int keyResourceId, final SharedPreferences... pref) {
        return getIntArray(rstr(keyResourceId), gp(pref));
    }

    public Integer[] getIntArray(String key, final SharedPreferences... pref) {
        List<Integer> data = getIntListOne(key, gp(pref));
        return data.toArray(new Integer[data.size()]);
    }


    public void setIntList(@StringRes int keyResourceId, List<Integer> values, final SharedPreferences... pref) {
        setIntListOne(rstr(keyResourceId), values, gp(pref));
    }

    public void setIntList(String key, List<Integer> values, final SharedPreferences... pref) {
        setIntListOne(key, values, gp(pref));
    }

    public ArrayList<Integer> getIntList(@StringRes int keyResourceId, final SharedPreferences... pref) {
        return getIntListOne(rstr(keyResourceId), gp(pref));
    }

    public ArrayList<Integer> getIntList(String key, final SharedPreferences... pref) {
        return getIntListOne(key, gp(pref));
    }


    //
    // Getter & Setter for Long
    //
    public void setLong(@StringRes int keyResourceId, long value, final SharedPreferences... pref) {
        gp(pref).edit().putLong(rstr(keyResourceId), value).apply();
    }

    public void setLong(String key, long value, final SharedPreferences... pref) {
        gp(pref).edit().putLong(key, value).apply();
    }

    public long getLong(@StringRes int keyResourceId, long defaultValue, final SharedPreferences... pref) {
        return getLong(rstr(keyResourceId), defaultValue, pref);
    }

    public long getLong(String key, long defaultValue, final SharedPreferences... pref) {
        try {
            return gp(pref).getLong(key, defaultValue);
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    //
    // Getter & Setter for Float
    //
    public void setFloat(@StringRes int keyResourceId, float value, final SharedPreferences... pref) {
        gp(pref).edit().putFloat(rstr(keyResourceId), value).apply();
    }

    public void setFloat(String key, float value, final SharedPreferences... pref) {
        gp(pref).edit().putFloat(key, value).apply();
    }

    public float getFloat(@StringRes int keyResourceId, float defaultValue, final SharedPreferences... pref) {
        return getFloat(rstr(keyResourceId), defaultValue);
    }

    public float getFloat(String key, float defaultValue, final SharedPreferences... pref) {
        try {
            return gp(pref).getFloat(key, defaultValue);
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    //
    // Getter & Setter for Double
    //
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

    //
    // Getter & Setter for boolean
    //
    public void setBool(@StringRes int keyResourceId, boolean value, final SharedPreferences... pref) {
        gp(pref).edit().putBoolean(rstr(keyResourceId), value).apply();
    }

    public void setBool(String key, boolean value, final SharedPreferences... pref) {
        gp(pref).edit().putBoolean(key, value).apply();
    }

    public boolean getBool(@StringRes int keyResourceId, boolean defaultValue, final SharedPreferences... pref) {
        return getBool(rstr(keyResourceId), defaultValue);
    }

    public boolean getBool(String key, boolean defaultValue, final SharedPreferences... pref) {
        try {
            return gp(pref).getBoolean(key, defaultValue);
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    public List<String> getStringSet(@StringRes int keyResourceId, List<String> defaultValue, final SharedPreferences... pref) {
        return getStringSet(rstr(keyResourceId), defaultValue);
    }

    public List<String> getStringSet(String key, List<String> defaultValue, final SharedPreferences... pref) {
        try {
            return new ArrayList<>(gp(pref).getStringSet(key, new HashSet<>(defaultValue)));
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    //
    // Getter & Setter for Color
    //
    public int getColor(String key, @ColorRes int defaultColor, final SharedPreferences... pref) {
        return getInt(key, rcolor(defaultColor));
    }

    public int getColor(@StringRes int keyResourceId, @ColorRes int defaultColor, final SharedPreferences... pref) {
        return getColor(rstr(keyResourceId), defaultColor);
    }

    //
    // PropertyBackend<String> implementations
    //
    @Override
    public String getString(String key, String defaultValue) {
        return getString(key, defaultValue, _prefApp);
    }

    @Override
    public int getInt(String key, int defaultValue) {
        return getInt(key, defaultValue, _prefApp);
    }

    @Override
    public long getLong(String key, long defaultValue) {
        return getLong(key, defaultValue, _prefApp);
    }

    @Override
    public boolean getBool(String key, boolean defaultValue) {
        return getBool(key, defaultValue, _prefApp);
    }

    public List<String> getStringSet(String key, List<String> defaultValue) {
        return getStringSet(key, defaultValue, _prefApp);
    }

    @Override
    public float getFloat(String key, float defaultValue) {
        return getFloat(key, defaultValue, _prefApp);
    }

    @Override
    public double getDouble(String key, double defaultValue) {
        return getDouble(key, defaultValue, _prefApp);
    }

    @Override
    public ArrayList<Integer> getIntList(String key) {
        return getIntList(key, _prefApp);
    }

    @Override
    public ArrayList<String> getStringList(String key) {
        return getStringList(key, _prefApp);
    }

    @Override
    public GsSharedPreferencesPropertyBackend setString(String key, String value) {
        setString(key, value, _prefApp);
        return this;
    }

    @Override
    public GsSharedPreferencesPropertyBackend setInt(String key, int value) {
        setInt(key, value, _prefApp);
        return this;
    }

    @Override
    public GsSharedPreferencesPropertyBackend setLong(String key, long value) {
        setLong(key, value, _prefApp);
        return this;
    }

    @Override
    public GsSharedPreferencesPropertyBackend setBool(String key, boolean value) {
        setBool(key, value, _prefApp);
        return this;
    }

    @Override
    public GsSharedPreferencesPropertyBackend setFloat(String key, float value) {
        setFloat(key, value, _prefApp);
        return this;
    }

    @Override
    public GsSharedPreferencesPropertyBackend setDouble(String key, double value) {
        setDouble(key, value, _prefApp);
        return this;
    }

    @Override
    public GsSharedPreferencesPropertyBackend setIntList(String key, List<Integer> value) {
        setIntListOne(key, value, _prefApp);
        return this;
    }

    @Override
    public GsSharedPreferencesPropertyBackend setStringList(String key, List<String> value) {
        setStringListOne(key, value, _prefApp);
        return this;
    }

    public boolean contains(String key, final SharedPreferences... pref) {
        return gp(pref).contains(key);
    }

    /**
     * Substract current datetime by given amount of days
     */
    public Date getDateOfDaysAgo(int days) {
        Calendar cal = new GregorianCalendar();
        cal.add(Calendar.DATE, -days);
        return cal.getTime();
    }

    /**
     * Substract current datetime by given amount of days and check if the given date passed
     */
    public boolean didDaysPassedSince(Date date, int days) {
        if (date == null || days < 0) {
            return false;
        }
        return date.before(getDateOfDaysAgo(days));
    }

    public boolean afterDaysTrue(String key, int daysSinceLastTime, int firstTime, final SharedPreferences... pref) {
        Date d = new Date(System.currentTimeMillis());
        if (!contains(key)) {
            d = getDateOfDaysAgo(daysSinceLastTime - firstTime);
            setLong(key, d.getTime());
            return firstTime < 1;
        } else {
            d = new Date(getLong(key, d.getTime()));
        }
        boolean trigger = didDaysPassedSince(d, daysSinceLastTime);
        if (trigger) {
            setLong(key, new Date(System.currentTimeMillis()).getTime());
        }
        return trigger;
    }

    public static void clearDebugLog() {
        _debugLog = "";
    }

    public static String getDebugLog() {
        return _debugLog;
    }

    public static synchronized void appendDebugLog(String text) {
        _debugLog += "[" + new Date().toString() + "] " + text + "\n";
    }

    public static boolean ne(final String str) {
        return str != null && !str.trim().isEmpty();
    }

    public static boolean fexists(final String fp) {
        return ne(fp) && (new File(fp)).exists();
    }
}
