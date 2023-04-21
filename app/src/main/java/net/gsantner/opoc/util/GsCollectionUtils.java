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

    public static <T> void mapInplace(final List<T> in, final GsCallback.r2<? extends T, ? super T, Integer> op) {
        final List<? extends T> res = map(in, op);
        in.clear();
        in.addAll(res);
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
     * @param list    List to sort
     * @param reverse Whether to sort in reverse
     * @param keyFn   Function to generate a self-comparable key from each list item
     * @param <T>     List type
     * @param <K>     Key type
     */
    public static <T, K> void keySort(
            final List<T> list,
            final boolean reverse,
            final GsCallback.r1<K, ? super T> keyFn,
            final Comparator<K> comp
    ) {
        final List<Pair<T, K>> decorated = map(list, (v, i) -> Pair.create(v, keyFn.callback(v)));
        final int mul = reverse ? -1 : 1;
        Collections.sort(decorated, (a, b) -> comp.compare(a.second, b.second) * mul);

        // TODO - investigate ways of doing this sort in-place
        list.clear();
        list.addAll(map(decorated, (d, i) -> d.first));
    }

    public static <T, K extends Comparable<K>> void keySort(
            final List<T> list,
            final boolean reverse,
            final GsCallback.r1<K, ? super T> keyFn
    ) {
        keySort(list, reverse, keyFn, Comparable::compareTo);
    }

    /**
     * Return set of elements in a which are not in b
     */
    public static <T> Set<T> setDiff(final Collection<T> a, final Collection<T> b) {
        final Set<T> ret = new LinkedHashSet<>(a);
        ret.removeAll(b);
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
        final Set<T> ret = new HashSet<>(a);
        ret.addAll(b);
        return ret;
    }

    /**
     * Set intersection
     */
    public static <T> Set<T> intersection(final Collection<T> a, final Collection<T> b) {
        final Set<T> ret = new HashSet<>(a);
        ret.retainAll(b);
        return ret;
    }
}
