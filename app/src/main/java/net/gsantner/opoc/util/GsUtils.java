package net.gsantner.opoc.util;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class GsUtils {

    /**
     * Like Arrays.asList, but supports null input (returns empty list)
     */
    @SafeVarargs
    public static <T> List<T> asList(final T ... items) {
        return items == null ? Collections.emptyList(): Arrays.asList(items);
    }

    public static <T> boolean addAll(Collection<? super T> c, T... elements) {
        return elements != null && Collections.addAll(c, elements);
    }

    public static <T> boolean addAll(Collection<? super T> c, Collection<? extends T> elements) {
        return elements != null && c.addAll(elements);
    }
}
