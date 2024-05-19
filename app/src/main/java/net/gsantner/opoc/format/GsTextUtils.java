/*#######################################################
 *
 * SPDX-FileCopyrightText: 2019-2024 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2019-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
#########################################################*/
package net.gsantner.opoc.format;

import android.util.Base64;

import net.gsantner.opoc.wrapper.GsCallback;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings({"unused", "SpellCheckingInspection"})
public class GsTextUtils {
    public static String UTF8 = "UTF-8";

    // Regex patterns used for finding resources in tags
    public final static Pattern SELF_CLOSING_TAG = Pattern.compile("<(\\w+)([^>]*?)src='([^']+)'([^>]*?)/>");
    public final static Pattern REGULAR_TAG = Pattern.compile("<(\\w+)([^>]*?)src='([^']+)'([^>]*?)>(.*?)</\\1>");


    /**
     * This is a simple method that tries to extract an URL around a given index.
     * It doesn't do any validation. Separation by whitespace or end. Detects http and https.
     *
     * @param text Text to extract from
     * @param pos  Position to start searching from (backwards)
     * @return Extracted URL or {@code null} if none found
     */
    public static String tryExtractUrlAroundPos(final String text, int pos) {
        pos = Math.min(Math.max(0, pos), text.length() - 1);
        int begin = Math.max(text.lastIndexOf("https://", pos), text.lastIndexOf("http://", pos));
        if (begin >= 0) {
            int end = text.length();
            for (final String check : new String[]{"\n", " ", "\t", "\r", ")", "|"}) {
                if ((pos = text.indexOf(check, begin)) > begin && pos < end) {
                    end = pos;
                }
            }

            if ((end - begin) > 5) {
                return text.substring(begin, end).replaceAll("[\\]=%>}]+$", "");
            }
        }
        return null;
    }


    /**
     * This is a simple method that tries to extract the value of the 'src' attribute
     * for any tag in the text which surrounds pos.
     * It doesn't do any validation. Separation by whitespace or end.
     *
     * @param text Text to extract from
     * @param pos  Position to start searching from (backwards)
     * @return Extracted resource path or {@code null} if none found
     */
    public static String tryExtractResourceAroundPos(final String text, int pos) {

        for (final Pattern pattern : Arrays.asList(SELF_CLOSING_TAG, REGULAR_TAG)) {
            final Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();
                if (pos >= start && pos <= end) {
                    return matcher.group(3);
                }
            }
        }

        return null; // Return null if no enclosing tag with src attribute is found
    }

    /**
     * find '\n' to the right and left of text[pos] .. text[posEnd].
     * If left does not exist 0 (begin of text) is used.
     * if right does not exist text.length() (end of text) is used.
     *
     * @return result[0] is left, result[1] is right.
     */
    public static int[] getNeighbourLineEndings(String text, int pos, int posEnd) {
        final int len = text.length();

        if (pos < len && pos >= 0 && text.charAt(pos) == '\n') {
            pos--;
        }
        pos = Math.min(Math.max(0, pos), len - 1);
        posEnd = Math.min(Math.max(0, posEnd), len - 1);
        if (pos == len) {
            pos--;
        }
        if (pos < 0 || pos > len) {
            return null;
        }
        pos = Math.max(0, text.lastIndexOf("\n", pos));
        posEnd = text.indexOf("\n", posEnd);
        if (posEnd < 0 || posEnd >= len - 1) {
            posEnd = len;
        }
        if (pos == 0 && pos == posEnd && posEnd + 1 <= len) {
            posEnd++;
        }
        if (pos <= len && posEnd <= len && pos <= posEnd) {
            return new int[]{pos, posEnd};
        }
        return null;
    }

    /**
     * returns search for begin of line starting for startPosition down to 0
     */
    public static int beginOfLine(final String text, int startPosition) {
        return getNeighbourLineEndings(text, startPosition, startPosition)[0];
    }

    public static int endOfLine(final String text, int startPosition) {
        return getNeighbourLineEndings(text, startPosition, startPosition)[1];
    }

    public static String removeLinesOfTextAround(String text, int pos, int posEnd) {
        int[] endings = getNeighbourLineEndings(text, pos, posEnd);
        if (endings != null) {
            StringBuffer sb = new StringBuffer();
            sb.append(text.substring(0, pos));
            sb.append(text.substring(posEnd));
            return sb.toString();
        }
        return text;
    }


    // Code snippet 'function huuid' is licensed CC0/Public Domain license. Revision 1, Gregor Santner, 2020
    //
    // Generate a UUID that starts with human readable datetime, use 8-4-4-4-12 UUID grouping
    // While there are zero guarantees, you will get most relevant datetime information out of a huuid
    // Plays nice with 'sort by name' in file managers and other tools
    //
    // Example based on "Mon Jan 2 15:04:05 MST 2006", deviceID dddd, 430 milliseconds, random string ffffffff, timezone MST=UTC+07:00
    // 20060102-1504-0543-070a-ddddffffffff
    //
    // Format detail: Milliseconds -> first two digits(=Centiseconds); UTC+/- -> a/f instead of last minute digit
    public static String newHuuid(String hostid4c) {
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-hhmm-ssSSS'%STRIP1BEFORE%'-'%HOSTID%%RAND%'");
        String rnd8c = String.format("%08x", new Random().nextInt());
        hostid4c = ((hostid4c == null ? "" : hostid4c) + "0000").substring(0, 4).replaceAll("[^A-Fa-f0-9]", "0");
        return sdf.format(new Date())
                .replace("%HOSTID%", hostid4c)
                .replace("%RAND%", rnd8c)
                .replaceAll(".%STRIP1BEFORE%", "").toLowerCase();
    }

    public static String toTitleCase(final String str) {
        final String delimiters = " '-/#.";
        final StringBuilder sb = new StringBuilder();

        boolean nextUppercase = true;
        for (char c : str.toCharArray()) {
            c = (nextUppercase) ? Character.toUpperCase(c) : Character.toLowerCase(c);
            sb.append(c);
            nextUppercase = (delimiters.indexOf(c) >= 0);
        }
        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    public static String toBase64(final String s) {
        try {
            return toBase64(s.getBytes(UTF8));
        } catch (Exception e) {
            return "";
        }
    }

    public static String toBase64(final byte[] bytes) {
        try {
            return Base64.encodeToString(bytes, Base64.DEFAULT);
        } catch (Exception ignored) {
            return "";
        }
    }

    public static byte[] fromBase64(final byte[] bytes) {
        return Base64.decode(bytes, Base64.DEFAULT);
    }

    public static String fromBase64ToString(final String s) {
        try {
            return new String(fromBase64(s.getBytes(UTF8)), UTF8);
        } catch (Exception e) {
            return "";
        }
    }

    public static int tryParseInt(final String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static <T> ArrayList<T> toArrayList(T... array) {
        ArrayList<T> list = new ArrayList<>();
        Collections.addAll(list, array);
        return list;
    }

    // Not null, not empty, not spaces only
    public static boolean isNullOrEmpty(final CharSequence str) {
        return str == null || str.length() == 0 || str.toString().trim().isEmpty();
    }

    /**
     * Convert an int color to a hex string. Optionally including alpha value.
     *
     * @param intColor  The color coded in int
     * @param withAlpha Optional; Set first bool parameter to true to also include alpha value
     */
    public static String colorToHexString(final int intColor, final boolean... withAlpha) {
        boolean a = withAlpha != null && withAlpha.length >= 1 && withAlpha[0];
        return String.format(a ? "#%08X" : "#%06X", (a ? 0xFFFFFFFF : 0xFFFFFF) & intColor);
    }


    /**
     * Convert escape sequences in string to escaped special characters. For example, convert
     * A\tB -> A    B
     * --------------
     * A\nB -> A
     * B
     *
     * @param input Input string
     * @return String with escaped sequences converted
     */
    public static String unescapeString(final String input) {
        final StringBuilder builder = new StringBuilder();
        boolean isEscaped = false;
        for (int i = 0; i < input.length(); i++) {
            char current = input.charAt(i);
            if (isEscaped) {
                if (current == 't') {
                    builder.append('\t');
                } else if (current == 'b') {
                    builder.append('\b');
                } else if (current == 'r') {
                    builder.append('\r');
                } else if (current == 'n') {
                    builder.append('\n');
                } else if (current == 'f') {
                    builder.append('\f');
                } else {
                    // Replace anything else with the literal pattern
                    builder.append('\\');
                    builder.append(current);
                }
                isEscaped = false;
            } else if (current == '\\') {
                isEscaped = true;
            } else {
                builder.append(current);
            }
        }

        // Handle trailing slash
        if (isEscaped) {
            builder.append('\\');
        }
        return builder.toString();
    }

    public static String jsonPrettyPrint(final String input) {
        try {
            if (new JSONTokener(input).nextValue() instanceof JSONObject) {
                return new JSONObject(input).toString(2);
            } else {
                return new JSONArray(input).toString(2);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Count number of instances of 'find' in 'text'
     *
     * @param text Text to search
     * @param find Substring to match
     * @return count
     */
    public static int countSubstrings(final String text, final String find) {
        int index = 0, count = 0;
        while ((index = text.indexOf(find, index)) != -1) {
            index += find.length();
            count++;
        }
        return count;
    }


    /**
     * Pad string on left up to size
     *
     * @param obj  Converted to string
     * @param size Total length after padding
     * @param c    Character to pad with
     * @return Padded string
     */
    public static String padLeft(final Object obj, final int size, final char c) {
        final String text = obj.toString();
        return repeatChars(c, size - text.length()) + text;
    }

    /**
     * Repeat a char count times
     *
     * @param character Char to prepeat
     * @param count     Times to repeat,
     * @return String with repeated chars
     */
    public static String repeatChars(char character, int count) {
        final char[] stringChars = new char[count];
        Arrays.fill(stringChars, character);
        return new String(stringChars);
    }

    public static List<Integer> findChar(final CharSequence text, final char c) {
        return findChar(text, c, 0, text.length());
    }

    public static List<Integer> findChar(final CharSequence text, final char c, final int start, final int end) {
        final List<Integer> posns = new ArrayList<>();
        for (int i = start; i < end; i++) {
            if (text.charAt(i) == c) {
                posns.add(i);
            }
        }
        return posns;
    }

    /**
     * Iterate over lines in a text
     *
     * @param text     Text to iterate over
     * @param callback Callback to call for each line. Return false to stop iteration.
     */
    public static void forEachline(final CharSequence text, GsCallback.b3<Integer, Integer, Integer> callback) {
        final List<Integer> ends = findChar(text, '\n');
        int start = 0, i = 0;
        for (; i < ends.size(); i++) {
            final int end = ends.get(i);
            if (!callback.callback(i, start, end)) {
                break;
            }
            start = end + 1;
        }
        callback.callback(i, start, text.length());
    }


    public static int[] countChars(final CharSequence s, final char... chars) {
        return countChars(s, 0, s.length(), chars);
    }

    /**
     * Count instances of chars between start and end
     *
     * @param s     Sequence to count in
     * @param start start of section to count within
     * @param end   end of section to count within
     * @param chars Array of chars to count
     * @return number of instances of each char in [start, end)
     */
    public static int[] countChars(final CharSequence s, int start, int end, final char... chars) {
        // Faster specialization for the common single case
        if (chars.length == 1) {
            return new int[]{countChar(s, start, end, chars[0])};
        }

        final int[] counts = new int[chars.length];
        start = Math.max(0, start);
        end = Math.min(end, s.length());
        for (int i = start; i < end; i++) {
            final char c = s.charAt(i);
            for (int j = 0; j < chars.length; j++) {
                if (c == chars[j]) {
                    counts[j]++;
                }
            }
        }
        return counts;
    }

    public static int countChar(final CharSequence s, final char c) {
        return countChar(s, 0, s.length(), c);
    }

    /**
     * Count instances of a single char in a charsequence
     */
    public static int countChar(final CharSequence s, int start, int end, final char c) {
        start = Math.max(0, start);
        end = Math.min(end, s.length());
        int count = 0;
        for (int i = start; i < end; i++) {
            if (s.charAt(i) == c) {
                count++;
            }
        }
        return count;
    }

    public static boolean isNewLine(CharSequence source, int start, int end) {
        return isValidIndex(source, start, end - 1) && (source.charAt(start) == '\n' || source.charAt(end - 1) == '\n');
    }

    public static boolean isValidIndex(final CharSequence s, final int... indices) {
        return s != null && inRange(0, s.length() - 1, indices);
    }

    // Checks if all values are in [min, max] _inclusive_
    public static boolean inRange(final int min, final int max, final int... values) {
        for (final int i : values) {
            if (i < min || i > max) {
                return false;
            }
        }
        return true;
    }
}
