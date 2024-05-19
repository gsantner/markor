/*#######################################################
 *
 * SPDX-FileCopyrightText: 2018-2024 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2018-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
#########################################################*/
package net.gsantner.opoc.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("SpellCheckingInspection")
public class GsMapPropertyBackend<TKEY> implements GsPropertyBackend<TKEY, GsMapPropertyBackend<TKEY>> {
    private final Map<TKEY, List<String>> _pStringList = new HashMap<>();
    private final Map<TKEY, List<Integer>> _pIntList = new HashMap<>();
    private final Map<TKEY, Boolean> _pBoolean = new HashMap<>();
    private final Map<TKEY, String> _pString = new HashMap<>();
    private final Map<TKEY, Double> _pDouble = new HashMap<>();
    private final Map<TKEY, Integer> _pInt = new HashMap<>();
    private final Map<TKEY, Float> _pFloat = new HashMap<>();
    private final Map<TKEY, Long> _pLong = new HashMap<>();

    public GsMapPropertyBackend() {

    }

    @Override
    public String getString(TKEY key, String defaultValue) {
        return _pString.containsKey(key) ? _pString.get(key) : defaultValue;
    }

    @Override
    public int getInt(TKEY key, int defaultValue) {
        return _pInt.containsKey(key) ? _pInt.get(key) : defaultValue;
    }

    @Override
    public long getLong(TKEY key, long defaultValue) {
        return _pLong.containsKey(key) ? _pLong.get(key) : defaultValue;
    }

    @Override
    public boolean getBool(TKEY key, boolean defaultValue) {
        return _pBoolean.containsKey(key) ? _pBoolean.get(key) : defaultValue;
    }

    @Override
    public float getFloat(TKEY key, float defaultValue) {
        return _pFloat.containsKey(key) ? _pFloat.get(key) : defaultValue;
    }

    @Override
    public double getDouble(TKEY key, double defaultValue) {
        return _pDouble.containsKey(key) ? _pDouble.get(key) : defaultValue;
    }

    @Override
    public List<Integer> getIntList(TKEY key) {
        return _pIntList.containsKey(key) ? _pIntList.get(key) : new ArrayList<>();
    }

    @Override
    public List<String> getStringList(TKEY key) {
        return _pStringList.containsKey(key) ? _pStringList.get(key) : new ArrayList<>();
    }

    @Override
    public GsMapPropertyBackend<TKEY> setString(TKEY key, String value) {
        _pString.put(key, value);
        return this;
    }

    @Override
    public GsMapPropertyBackend<TKEY> setInt(TKEY key, int value) {
        _pInt.put(key, value);
        return this;
    }

    @Override
    public GsMapPropertyBackend<TKEY> setLong(TKEY key, long value) {
        _pLong.put(key, value);
        return this;
    }

    @Override
    public GsMapPropertyBackend<TKEY> setBool(TKEY key, boolean value) {
        _pBoolean.put(key, value);
        return this;
    }

    @Override
    public GsMapPropertyBackend<TKEY> setFloat(TKEY key, float value) {
        _pFloat.put(key, value);
        return this;
    }

    @Override
    public GsMapPropertyBackend<TKEY> setDouble(TKEY key, double value) {
        _pDouble.put(key, value);
        return this;
    }

    @Override
    public GsMapPropertyBackend<TKEY> setIntList(TKEY key, List<Integer> value) {
        _pIntList.put(key, value);
        return this;
    }

    @Override
    public GsMapPropertyBackend<TKEY> setStringList(TKEY key, List<String> value) {
        _pStringList.put(key, value);
        return this;
    }
}
