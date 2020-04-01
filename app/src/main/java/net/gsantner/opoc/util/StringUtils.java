package net.gsantner.opoc.util;

import android.widget.TextView;

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
        int i = start;
        for (; i < maxRange && i < s.length(); i++) {
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
        int i = start;
        for (; i < maxRange && i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != ' ' && c != '\t') {
                break;
            }
        }
        return i;
    }

    public static int[] getSelection(TextView text) {

        int selectionStart = text.getSelectionStart();
        int selectionEnd = text.getSelectionEnd();

        if (selectionEnd < selectionStart) {
            selectionEnd = text.getSelectionStart();
            selectionStart = text.getSelectionEnd();
        }

        int[] selection = {selectionStart, selectionEnd};
        return selection;
    }

}
