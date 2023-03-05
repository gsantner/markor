package net.gsantner.opoc.util;

import net.gsantner.opoc.wrapper.GsCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

// Class for general utilities
public class GsUtils {

    /**
     * Like Arrays.asList, but supports null input (returns empty list)
     */
    @SafeVarargs
    public static <T> List<T> asList(final T ... items) {
        return items == null ? Collections.emptyList(): Arrays.asList(items);
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
    public static <T> void replaceAll(final List<T> l, final GsCallback.r1<T, T> op) {
        for (int i = 0; i < l.size(); i++) {
            l.set(i, op.callback(l.get(i)));
        }
    }

    public static <I, O> List<O> map(final Collection<? extends I> in, final GsCallback.r1<O, I> op) {
        final List<O> out = new ArrayList<>();
        for (final I val : in) {
            out.add(op.callback(val));
        }
        return out;
    }
}
