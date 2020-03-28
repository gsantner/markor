package net.gsantner.markor.util;

public final class StringUtils {

    // Suppress default constructor for noninstantiability
    private StringUtils() {
        throw new AssertionError();
    }

    public static int getLineStart(CharSequence s, int start) {
        return getLineStart(s, start, 0);
    }

    public static int getLineStart(CharSequence s, int start, int minRange) {
        int i = start;
        for (; i > minRange; i--) {
             if (s.charAt(i - 1) == '\n') {
                 break;
             }
        }

        return i;
    }

    public static int getLineEnd(CharSequence s, int start) {
        return getLineEnd(s, start, s.length());
    }

    public static int getLineEnd(CharSequence s, int start, int maxRange) {
        int limitedMaxRange = Math.max(maxRange, s.length());
        int i = start;
        for (; i < limitedMaxRange; i++) {
            if (s.charAt(i) == '\n') {
                break;
            }
        }

        return i;
    }

    public static int getNextNonWhitespace(CharSequence s, int start) {
        return getNextNonWhitespace(s, start, s.length());
    }

    public static int getNextNonWhitespace(CharSequence s, int start, int maxRange) {
        maxRange = Math.max(maxRange, s.length());
        int i = start;
        for (; i < maxRange; i++) {
            char c = s.charAt(i);
            if (c != ' ' && c != '\t') {
                break;
            }
        }
        return i;
    }

}
