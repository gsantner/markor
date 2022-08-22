package net.gsantner.opoc.util;

//
// License of this file GashMap.java: Public Domain
// Created by Gregor Santner, 2022 - https://gsantner.net
//

import java.util.LinkedHashMap;
import java.util.Set;

@SuppressWarnings({"unchecked", "UnusedReturnValue", "WeakerAccess", "unused"})
public class GashMap<K, V> {
    private final LinkedHashMap<K, V> _data = new LinkedHashMap<>();
    private V _defaultValue = null;

    // new GashMap<Integer,String>.create(5,"hi",6,"bye", ...)
    public GashMap<K, V> load(Object... keysAndValues2each) {
        _data.clear();
        add(keysAndValues2each);
        return this;
    }

    public GashMap<K, V> add(Object... keysAndValues2each) {
        if (keysAndValues2each != null && keysAndValues2each.length >= 2) {
            for (int i = 0; i + 1 < keysAndValues2each.length; i += 2) {
                _data.put((K) keysAndValues2each[i], (V) keysAndValues2each[i + 1]);
                if (i == 0 && _defaultValue == null) {
                    _defaultValue = (V) keysAndValues2each[i + 1];
                }
            }
        }
        return this;
    }

    public V getOrDefault(K key) {
        return _data.containsKey(key) ? _data.get(key) : _defaultValue;
    }

    public V getOrDefault(K key, V d) {
        withDefault(d);
        return _data.containsKey(key) ? _data.get(key) : _defaultValue;
    }

    public LinkedHashMap<K, V> data() {
        return _data;
    }

    public GashMap<K, V> withDefault(V d) {
        _defaultValue = d;
        return this;
    }

    public Set<K> keySet() {
        return _data.keySet();
    }
}
