package net.gsantner.markor.util;

import org.apache.commons.lang3.ArrayUtils;

public class StringUtils extends org.apache.commons.lang3.StringUtils {
    // modified version of org.apache.commons.lang3.StringUtils#indexOfAny
    /**
     * Same as {@link org.apache.commons.lang3.StringUtils#indexOfAny(CharSequence, char...)
     * where you can specify the search intervall}
     */
    public static int indexOfAny(final CharSequence cs, int csFirst, int csLen, final char... searchChars) {
        if (isEmpty(cs) || ArrayUtils.isEmpty(searchChars)) {
            return INDEX_NOT_FOUND;
        }
        final int csLast = csLen - 1;
        final int searchLen = searchChars.length;
        final int searchLast = searchLen - 1;
        for (int i = csFirst; i < csLen; i++) {
            final char ch = cs.charAt(i);
            for (int j = 0; j < searchLen; j++) {
                if (searchChars[j] == ch) {
                    if (i < csLast && j < searchLast && Character.isHighSurrogate(ch)) {
                        // ch is a supplementary character
                        if (searchChars[j + 1] == cs.charAt(i + 1)) {
                            return i;
                        }
                    } else {
                        return i;
                    }
                }
            }
        }
        return INDEX_NOT_FOUND;
    }

    /**
     * returns search for begin of line starting for csFirst down to 0
     */
    public static int beginOfLine(final CharSequence cs, int csFirst) {
        if (isEmpty(cs)) {
            return 0;
        }
        for (int i = csFirst; i >= 0; i--) {
            final char ch = cs.charAt(i);
            if ('\n' == ch || '\r' == ch) {
                    return i+1;
            }
        }
        return 0;
    }

    public static int endOfLine(final CharSequence cs, int csFirst) {
        if (isEmpty(cs)) {
            return 0;
        }
        final int csLen = cs.length();

        for (int i = csFirst; i < csLen; i++) {
            final char ch = cs.charAt(i);
            if ('\n' == ch || '\r' == ch) {
                return i;
            }
        }
        return csLen;
    }

}