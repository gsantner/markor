package net.gsantner.opoc.util;

import android.util.Pair;

import net.gsantner.opoc.wrapper.GsCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

// Class for general utilities
public class GsUtils {

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
     * Apply the opertation op on each element of in, and return the result
     */
    public static <I, O> List<O> map(final Collection<? extends I> in, final GsCallback.r1<O, ? super I> op) {
        final List<O> out = new ArrayList<>();
        for (final I val : in) {
            out.add(op.callback(val));
        }
        return out;
    }

    /**
     * Sort a list using a key function.
     *
     * Refer to python's sort - https://docs.python.org/3/howto/sorting.html
     *
     * @param list     List to sort
     * @param reverse  Whether to sort in reverse
     * @param keyFn    Function to generate a self-comparable key from each list item
     * @param <T>      List type
     * @param <K>      Key type
     */
    public static <T, K extends Comparable<K>> void keySort(
            final List<T> list,
            final boolean reverse,
            final GsCallback.r1<K, ? super T> keyFn
    ) {
        final List<Pair<T, K>> decorated = map(list, (v) -> Pair.create(v, keyFn.callback(v)));
        Collections.sort(decorated, (a, b) -> reverse ? b.second.compareTo(a.second) : a.second.compareTo(b.second));

        // TODO - investigate ways of doing this sort in-place
        list.clear();
        list.addAll(map(decorated, d -> d.first));
    }
}
