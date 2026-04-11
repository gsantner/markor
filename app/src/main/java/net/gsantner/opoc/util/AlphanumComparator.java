package net.gsantner.opoc.util;

import net.gsantner.opoc.format.GsTextUtils;

import java.util.Comparator;

/**
 * An Alphanumeric (Natural Sort) Comparator. Sorts strings containing numbers in natural numeric
 * order. Null and empty strings are sorted to the end. Numeric chunks with leading zeros are sorted
 * after their non-padded equivalents
 */
public class AlphanumComparator implements Comparator<String> {
    private static boolean isDigit(char ch) {
        // ASCII digits only, intentionally excludes Unicode digit characters
        return ch >= '0' && ch <= '9';
    }

    /**
     * Find the end of a chunk starting at the given index. A chunk is either a sequence of digits
     * or a sequence of non-digits.
     *
     * @param s The string to scan
     * @param index The index to start scanning from
     * @return The index of the first character not belonging to the chunk
     */
    private static int getChunkEnd(String s, int index) {
        int sLength = s.length();
        boolean firstIsDigit = isDigit(s.charAt(index));
        int end = index;
        while (end < sLength && isDigit(s.charAt(end)) == firstIsDigit) {
            end++;
        }
        return end;
    }

    /**
     * Count the number of leading zeros in a numeric chunk, leaving at least one significant digit.
     * For example, "001" has 2 leading zeros, and '0' is the single remaining.
     *
     * @param s The string containing the chunk
     * @param start The start index of the numeric chunk
     * @param end The end index of the numeric chunk
     * @return The number of leading zeros that can be ignored for numeric comparison
     */
    private static int countLeadingZeros(String s, int start, int end) {
        int i = start;
        int limit = end - 1; // Always leave at least one digit (e.g., "000" -> "0")

        while (i < limit && s.charAt(i) == '0') {
            i++;
        }
        return i - start;
    }

    /**
     * Compares two regions of strings for order, optionally ignoring case. Mimics String.compareTo
     * and String.compareToIgnoreCase without allocations.
     *
     * @param s1 First string
     * @param start1 Start index in s1
     * @param end1 End index in s1
     * @param s2 Second string
     * @param start2 Start index in s2
     * @param end2 End index in s2
     * @param ignoreCase Whether to perform case-insensitive comparison
     * @return Negative if s1 region < s2 region, positive if >, zero if equal
     */
    private static int compareRegions(
            String s1, int start1, int end1, String s2, int start2, int end2, boolean ignoreCase) {
        int n1 = end1 - start1;
        int n2 = end2 - start2;
        int minLen = Math.min(n1, n2);

        for (int i = 0; i < minLen; i++) {
            char c1 = s1.charAt(start1 + i);
            char c2 = s2.charAt(start2 + i);

            if (c1 != c2) {
                if (ignoreCase) {
                    c1 = Character.toUpperCase(c1);
                    c2 = Character.toUpperCase(c2);
                    if (c1 != c2) {
                        c1 = Character.toLowerCase(c1);
                        c2 = Character.toLowerCase(c2);
                        if (c1 != c2) {
                            return c1 - c2;
                        }
                    }
                    // If they match after conversion, continue to next character
                } else {
                    return c1 - c2;
                }
            }
        }
        return n1 - n2;
    }

    /**
     * Compare two strings alphanumerically
     *
     * @param s1 The first string
     * @param s2 The second string
     * @return Negative if s1 < s2, positive if s1 > s2, zero if equal
     */
    @Override
    public int compare(String s1, String s2) {
        boolean empty1 = GsTextUtils.isNullOrEmpty(s1);
        boolean empty2 = GsTextUtils.isNullOrEmpty(s2);

        if (empty1 && empty2) return 0;
        if (empty1) return 1; // null/empty goes to the end
        if (empty2) return -1; // s1 stays before null/empty s2

        int index1 = 0;
        int index2 = 0;
        int s1Len = s1.length();
        int s2Len = s2.length();

        while (index1 < s1Len && index2 < s2Len) {
            int end1 = getChunkEnd(s1, index1);
            int end2 = getChunkEnd(s2, index2);

            int result;
            // If both chunks are numeric, compare them based on value
            if (isDigit(s1.charAt(index1)) && isDigit(s2.charAt(index2))) {
                // Ignore leading zeros for the length comparison
                int zeros1 = countLeadingZeros(s1, index1, end1);
                int zeros2 = countLeadingZeros(s2, index2, end2);

                int sigLen1 = (end1 - index1) - zeros1;
                int sigLen2 = (end2 - index2) - zeros2;

                // Longer significant part = larger number
                result = sigLen1 - sigLen2;
                if (result == 0) {
                    // Same length, compare digit by digit
                    for (int i = 0; i < sigLen1; i++) {
                        result = s1.charAt(index1 + zeros1 + i) - s2.charAt(index2 + zeros2 + i);
                        if (result != 0) {
                            return result;
                        }
                    }
                    // Numerically equal: fewer leading zeros sorts first ("1" < "01")
                    result = zeros1 - zeros2;
                }
            } else {
                // Non-numeric: case-insensitive first
                result = compareRegions(s1, index1, end1, s2, index2, end2, true);
                if (result == 0) {
                    // Tiebreak case-sensitively: uppercase sorts before lowercase by ASCII order
                    result = compareRegions(s1, index1, end1, s2, index2, end2, false);
                }
            }

            if (result != 0) {
                return result;
            }

            index1 = end1;
            index2 = end2;
        }

        return Integer.compare(s1Len, s2Len);
    }
}
