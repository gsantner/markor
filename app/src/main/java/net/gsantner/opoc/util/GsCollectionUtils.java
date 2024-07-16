/*#######################################################
 *
 * SPDX-FileCopyrightText: 2023 Harshad Vedartham <harshad1 AT zoho DOT com>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2023 by Harshad Vedartham <harshad1 AT zoho DOT com>
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
#########################################################*/
package net.gsantner.opoc.util;

import android.util.Pair;

import net.gsantner.opoc.wrapper.GsCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// Class for general utilities
public class GsCollectionUtils {

    /**
     * Like Arrays.asList, but supports null input (returns empty list)
     */
    @SafeVarargs
    public static <T> List<T> asList(final T... items) {
        return items == null ? Collections.emptyList() : Arrays.asList(items);
    }

    @SafeVarargs
    public static <T> boolean addAll(Collection<? super T> c, T... elements) {
        return elements != null && Collections.addAll(c, elements);
    }

    public static <T> boolean addAll(Collection<? super T> c, Collection<? extends T> elements) {
        return elements != null && c.addAll(elements);
    }

    /**
     * Replace each element with result of op. Like foo.replaceAll
     */
    public static <T> void replaceAll(final List<T> l, final GsCallback.r1<T, ? super T> op) {
        for (int i = 0; i < l.size(); i++) {
            l.set(i, op.callback(l.get(i)));
        }
    }


    /**
     * Apply the opertation op on each element of in, and return the result.
     */
    public static <I, O> List<O> map(final Collection<? extends I> in, final GsCallback.r2<O, ? super I, Integer> op) {
        final List<O> out = new ArrayList<>();

        int index = 0;
        for (final I val : in) {
            out.add(op.callback(val, index));
            index++;
        }
        return out;
    }

    // Map without index
    public static <I, O> List<O> map(final Collection<? extends I> in, final GsCallback.r1<O, ? super I> op) {
        return map(in, (v, i) -> op.callback(v));
    }

    public static <T extends Comparable<T>> int listComp(final List<T> a, final List<T> b) {
        final int la = a.size(), lb = b.size();
        for (int i = 0; i < Math.min(la, lb); i++) {
            final int comp = a.get(i).compareTo(b.get(i));
            if (comp != 0) {
                return comp;
            }
        }
        return Integer.compare(la, lb);
    }

    /**
     * Sort a list using a key function.
     * Refer to python's sort - https://docs.python.org/3/howto/sorting.html
     *
     * @param list  List to sort
     * @param keyFn Function to generate a self-comparable key from each list item
     * @param <T>   List type
     * @param <K>   Key type
     */
    public static <T, K> void keySort(
            final List<T> list,
            final GsCallback.r1<K, ? super T> keyFn,
            final Comparator<K> comp
    ) {
        final List<Pair<T, K>> decorated = map(list, (v, i) -> Pair.create(v, keyFn.callback(v)));
        Collections.sort(decorated, (a, b) -> comp.compare(a.second, b.second));

        // TODO - investigate ways of doing this sort in-place
        list.clear();
        list.addAll(map(decorated, (d, i) -> d.first));
    }

    public static <T, K extends Comparable<K>> void keySort(
            final List<T> list,
            final GsCallback.r1<K, ? super T> keyFn
    ) {
        keySort(list, keyFn, Comparable::compareTo);
    }

    /**
     * Return set of elements in a which are not in b
     */
    public static <T> Set<T> setDiff(final Collection<T> a, final Collection<T> b) {
        final Set<T> ret = new LinkedHashSet<>(a);
        if (b != null) {
            ret.removeAll(b);
        }
        return ret;
    }

    /**
     * Check if 2 collections have the same elements
     */
    public static <T> boolean setEquals(Collection<T> a, Collection<T> b) {
        a = a != null ? a : Collections.emptySet();
        b = b != null ? b : Collections.emptySet();

        a = a instanceof Set ? a : new HashSet<>(a);
        b = b instanceof Set ? b : new HashSet<>(b);

        return a.equals(b);
    }

    /**
     * Set union
     */
    public static <T> Set<T> union(final Collection<T> a, final Collection<T> b) {
        final Set<T> ret = new LinkedHashSet<>(a);
        ret.addAll(b);
        return ret;
    }

    /**
     * Set intersection
     */
    public static <T> Set<T> intersection(final Collection<T> a, final Collection<T> b) {
        final Set<T> ret = new LinkedHashSet<>(a);
        ret.retainAll(b);
        return ret;
    }


    public static <T, V> V accumulate(
            final Collection<T> collection,
            final GsCallback.r2<V, ? super T, V> func,
            final V initial
    ) {
        V val = initial;
        for (final T item : collection) {
            val = func.callback(item, val);
        }
        return val;
    }

    public static <T> boolean any(
            final Collection<T> collection,
            final GsCallback.b1<T> predicate
    ) {
        for (final T item : collection) {
            if (predicate.callback(item)) {
                return true;
            }
        }
        return false;
    }

    public static <T> boolean all(
            final Collection<T> collection,
            final GsCallback.b1<T> predicate
    ) {
        for (final T item : collection) {
            if (!predicate.callback(item)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get indices of data where predicate is true. Meaningless for unordered data.
     */
    public static <T> List<Integer> indices(final Collection<T> data, final GsCallback.b1<? super T> predicate) {
        final List<Integer> indices = new ArrayList<>();
        int index = 0;
        for (final T item : data) {
            if (predicate.callback(item)) {
                indices.add(index);
            }
            index++;
        }
        return indices;
    }

    /**
     * Select elements of data where predicate is true
     */
    public static <T> List<T> select(final Collection<T> data, final GsCallback.b1<? super T> predicate) {
        final List<T> sel = new ArrayList<>();
        for (final T item : data) {
            if (predicate.callback(item)) {
                sel.add(item);
            }
        }
        return sel;
    }

    public static <T> T selectFirst(final Collection<T> data, final GsCallback.b1<? super T> predicate) {
        for (final T item : data) {
            if (predicate.callback(item)) {
                return item;
            }
        }
        return null;
    }

    /**
     * Get a list of values (like np.arange())
     *
     * @param ops start, stop and step (all optional)
     * @return List of integers with values
     */
    public static List<Integer> range(final int... ops) {
        int start = 0, end = 0, step = 1;
        if (ops != null) {
            if (ops.length == 1) {
                end = ops[0];
            } else if (ops.length == 2) {
                start = ops[0];
                end = ops[1];
            } else if (ops.length >= 3) {
                start = ops[0];
                end = ops[1];
                step = ops[2];
            }
        }

        final List<Integer> values = new ArrayList<>();
        while (start < end) {
            values.add(start);
            start += step;
        }

        return values;
    }

    public static class Holder<T> {
        private T value;

        public Holder(T value) {
            this.value = value;
        }

        public T get() {
            return value;
        }

        public Holder<T> set(T value) {
            this.value = value;
            return this;
        }

        public T clear() {
            try {
                return value;
            } finally {
                value = null;
            }
        }
    }
}
