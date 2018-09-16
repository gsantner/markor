/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License: Apache 2.0 / Commercial
 *  https://github.com/gsantner/opoc/#licensing
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.opoc.preference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("SpellCheckingInspection")
public class MapPropertyBackend<TKEY> implements PropertyBackend<TKEY, MapPropertyBackend> {
    private final Map<TKEY, List<String>> _pStringList = new HashMap<>();
    private final Map<TKEY, List<Integer>> _pIntList = new HashMap<>();
    private final Map<TKEY, Boolean> _pBoolean = new HashMap<>();
    private final Map<TKEY, String> _pString = new HashMap<>();
    private final Map<TKEY, Double> _pDouble = new HashMap<>();
    private final Map<TKEY, Integer> _pInt = new HashMap<>();
    private final Map<TKEY, Float> _pFloat = new HashMap<>();
    private final Map<TKEY, Long> _pLong = new HashMap<>();

    public MapPropertyBackend() {

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
    public MapPropertyBackend setString(TKEY key, String value) {
        _pString.put(key, value);
        return this;
    }

    @Override
    public MapPropertyBackend setInt(TKEY key, int value) {
        _pInt.put(key, value);
        return this;
    }

    @Override
    public MapPropertyBackend setLong(TKEY key, long value) {
        _pLong.put(key, value);
        return this;
    }

    @Override
    public MapPropertyBackend setBool(TKEY key, boolean value) {
        _pBoolean.put(key, value);
        return this;
    }

    @Override
    public MapPropertyBackend setFloat(TKEY key, float value) {
        _pFloat.put(key, value);
        return this;
    }

    @Override
    public MapPropertyBackend setDouble(TKEY key, double value) {
        _pDouble.put(key, value);
        return this;
    }

    @Override
    public MapPropertyBackend setIntList(TKEY key, List<Integer> value) {
        _pIntList.put(key, value);
        return this;
    }

    @Override
    public MapPropertyBackend setStringList(TKEY key, List<String> value) {
        _pStringList.put(key, value);
        return this;
    }
}
