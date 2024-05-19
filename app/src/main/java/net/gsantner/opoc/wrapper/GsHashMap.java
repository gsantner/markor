/*#######################################################
 *
 * SPDX-FileCopyrightText: 2022-2024 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2018-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
#########################################################*/

package net.gsantner.opoc.wrapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({"unchecked", "UnusedReturnValue", "WeakerAccess", "unused"})
public class GsHashMap<K, V> {
    private final LinkedHashMap<K, V> _data = new LinkedHashMap<>();
    private V _defaultValue = null;

    // new GashMap<Integer,String>.create(5,"hi",6,"bye", ...)
    public GsHashMap<K, V> load(Object... keysAndValues2each) {
        _data.clear();
        add(keysAndValues2each);
        return this;
    }

    public GsHashMap<K, V> add(Object... keysAndValues2each) {
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

    public GsHashMap<K, V> withDefault(V d) {
        _defaultValue = d;
        return this;
    }

    public Set<K> keySet() {
        return _data.keySet();
    }

    public void limitSizeByRemovingOldest(int limit) {
        limit = Math.max(0, limit);
        while (_data.size() > limit) {
            K removal = null;
            for (Map.Entry<K, V> kvEntry : _data.entrySet()) {
                removal = kvEntry.getKey();
                break;
            }
            if (removal != null) {
                _data.remove(removal);
            }
        }
    }
}
