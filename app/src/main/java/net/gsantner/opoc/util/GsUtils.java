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
     * Apply the opertation op on each element of in, and return the result.
     *
     * This version runs on lists and the callback is provided with the index
     */
    public static <I, O> List<O> map(final List<? extends I> in, final GsCallback.r2<O, ? super I, Integer> op) {
        final List<O> out = new ArrayList<>();
        for (int i = 0; i < in.size(); i++) {
            out.add(op.callback(in.get(i), i));
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


    /**
     * Find the smallest single diff from source -> dest
     *
     * This is similar to TextViewUtils.findDiff
     *
     * @param dest   Into which we want to apply the diff
     * @param source From which we want to apply the diff
     * @return { a, b, c } s.t. setting dest[a:b] = source[a:c] will make dest == source
     */
    public static <T> int[] diff(final List<T> dest, final List<T> source) {

        final int dl = dest.size(), sl = source.size();
        final int minLength = Math.min(dl, sl);

        int start = 0;
        while (start < minLength && source.get(start).equals(dest.get(start))) start++;

        // Handle several special cases
        if (sl == dl && start == sl) { // Case where 2 sequences are same
            return new int[]{0, 0, 0, 0};
        } else if (sl < dl && start == sl) { // Pure crop
            return new int[]{start, dl, sl, sl};
        } else if (dl < sl && start == dl) { // Pure append
            return new int[]{dl, dl, start, sl};
        }

        int end = 0;
        final int maxEnd = minLength - start;
        while (end < maxEnd && source.get(sl - end - 1).equals(dest.get(sl - end - 1))) end++;

        return new int[]{start, dl - end, sl - end};
    }
}
