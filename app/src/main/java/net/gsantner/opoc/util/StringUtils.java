/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.opoc.util;

import android.widget.TextView;

import com.vladsch.flexmark.util.sequence.CharSubSequence;

import java.util.Arrays;

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

        return new int[] {selectionStart, selectionEnd};
    }

    public static String repeatChars(char character, int count) {
        char[] stringChars = new char[count];
        Arrays.fill(stringChars, character);
        return new String(stringChars);
    }

    /**
     * Convert a char index to a line index + offset from end of line
     * @param s text to parse
     * @param p position in text
     * @return int[2] where index 0 is line and index 1 is position from end of line
     */
    public static int[] getLineOffsetFromIndex(final CharSequence s, int p) {
        p = Math.min(Math.max(p, 0), s.length());
        final int line = countChar(s, '\n', 0, p);
        final int offset = getLineEnd(s, p) - p;

        return new int[]{line, offset};
    }

    public static int getIndexFromLineOffset(final CharSequence s, final int[] le) {
        return getIndexFromLineOffset(s, le[0], le[1]);

    }

    /**
     * Convert a line index and offset from end of line to absolute position
     * @param s text to parse
     * @param l line index
     * @param e offset from end of line
     * @return index in s
     */
    public static int getIndexFromLineOffset(final CharSequence s, final int l, final int e) {
        int i = 0, count = 0;
        for (; i < s.length(); i++) {
            if (s.charAt(i) == '\n') {
                count++;
            }
            if (count == l) {
                break;
            }
        }
        if (i < s.length()) {
            return getLineEnd(s, i + 1) - e;
        }
        return i;
    }

    public static int countChar(final CharSequence s, final char c, int start, int end) {
        int count = 0;
        start = Math.max(0, start);
        end = Math.min(end, s.length());
        for (int i = start; i < end; i++) {
            if (s.charAt(i) == c) {
                count++;
            }
        }
        return count;
    }
}
