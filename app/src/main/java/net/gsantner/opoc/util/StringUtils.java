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

import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.util.Base64;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

@SuppressWarnings({"CharsetObjectCanBeUsed", "WeakerAccess", "unused"})
public final class StringUtils {

    // Suppress default constructor for noninstantiability
    private StringUtils() {
        throw new AssertionError();
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

    public static int getLineStart(CharSequence s, int start) {
        return getLineStart(s, start, 0);
    }

    public static int getLineStart(CharSequence s, int start, int minRange) {
        int i = start;
        if (isValidIndex(s, start - 1, minRange)) {
            for (; i > minRange; i--) {
                if (s.charAt(i - 1) == '\n') {
                    break;
                }
            }
        }

        return i;
    }

    public static int getLineEnd(CharSequence s, int start) {
        return getLineEnd(s, start, s.length());
    }

    public static int getLineEnd(CharSequence s, int start, int maxRange) {
        int i = start;
        if (isValidIndex(s, start, maxRange - 1)) {
            for (; i < maxRange; i++) {
                if (s.charAt(i) == '\n') {
                    break;
                }
            }
        }

        return i;
    }

    public static int getNextNonWhitespace(CharSequence s, int start) {
        return getNextNonWhitespace(s, start, s.length());
    }

    public static int getNextNonWhitespace(CharSequence s, int start, int maxRange) {
        int i = start;
        if (isValidIndex(s, start, maxRange - 1)) {
            for (; i < maxRange; i++) {
                char c = s.charAt(i);
                if (c != ' ' && c != '\t') {
                    break;
                }
            }
        }
        return i;
    }

    public static boolean isNullOrWhitespace(String str) {
        if (str == null || str.isEmpty()) {
            return true;
        }

        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (!Character.isWhitespace(ch)) {
                return false;
            }
        }

        return true;
    }

    public static int[] getSelection(final TextView text) {

        int selectionStart = text.getSelectionStart();
        int selectionEnd = text.getSelectionEnd();

        if (selectionEnd < selectionStart) {
            selectionEnd = text.getSelectionStart();
            selectionStart = text.getSelectionEnd();
        }

        return new int[]{selectionStart, selectionEnd};
    }

    public static int[] getLineSelection(final TextView text) {
        final int[] sel = getSelection(text);
        final CharSequence s = text.getText();
        return new int[]{
                getLineStart(s, sel[0]),
                getLineEnd(s, sel[1])
        };
    }

    public static String getSelectedLines(final TextView text) {
        final int[] sel = getLineSelection(text);
        return text.getText().subSequence(sel[0], sel[1]).toString();
    }

    public static String repeatChars(char character, int count) {
        char[] stringChars = new char[count];
        Arrays.fill(stringChars, character);
        return new String(stringChars);
    }

    /**
     * Convert a char index to a line index + offset from end of line
     *
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
     *
     * @param s text to parse
     * @param l line index
     * @param e offset from end of line
     * @return index in s
     */
    public static int getIndexFromLineOffset(final CharSequence s, final int l, final int e) {
        int i = 0, count = 0;
        if (s != null) {
            if (l > 0) {
                for (; i < s.length(); i++) {
                    if (s.charAt(i) == '\n') {
                        count++;
                        if (count == l) {
                            break;
                        }
                    }
                }
            }
            if (i < s.length()) {
                final int start = (l == 0) ? 0 : i + 1;
                final int end = getLineEnd(s, start);
                // Prevent selection from moving to previous line
                return end - Math.min(e, end - start);
            }
        }
        return i;
    }

    /**
     * Count instances of char 'c' between start and end
     *
     * @param s     Sequence to count in
     * @param c     Char to count
     * @param start start of section to count within
     * @param end   end of section to count within
     * @return number of instances of c in c between start and end
     */
    public static int countChar(final CharSequence s, final char c, int start, int end) {
        int count = 0;
        if (isValidIndex(s, start, end - 1)) {
            start = Math.max(0, start);
            end = Math.min(end, s.length());
            for (int i = start; i < end; i++) {
                if (s.charAt(i) == c) {
                    count++;
                }
            }
        }
        return count;
    }

    public static boolean isNewLine(CharSequence source, int start, int end) {
        return isValidIndex(source, start, end - 1) && (source.charAt(start) == '\n' || source.charAt(end - 1) == '\n');
    }

    public static <T> ArrayList<T> toArrayList(T[] array) {
        ArrayList<T> list = new ArrayList<>();
        Collections.addAll(list, array);
        return list;
    }

    /**
     * Convert escape sequences in string to escaped special characters
     * <p>
     * For example, convert
     * <p>
     * A\tB -> A    B
     * <p>
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

    public static String toBase64(final String s) {
        try {
            return toBase64(s.getBytes("UTF-8"));
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
            return new String(fromBase64(s.getBytes("UTF-8")), "UTF-8");
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

    /**
     * Select the given indices.
     * Case 1: Only one index -> Put cursor on that line
     * Case 2: Contiguous indices -> Select lines
     * Case 3: Non-contiguous indices -> Move all selected lines to the top and select them
     *
     * @param positions: Line indices to select
     */
    public static void selectLines(final EditText edit, final List<Integer> positions) {
        if (!edit.hasFocus()) {
            edit.requestFocus();
        }
        final CharSequence text = edit.getText();
        if (positions.size() == 1) { // Case 1 index
            final int posn = StringUtils.getIndexFromLineOffset(text, positions.get(0), 0);
            setSelectionAndShow(edit, posn);
        } else if (positions.size() > 1) {
            final TreeSet<Integer> pSet = new TreeSet<>(positions);
            final int selStart, selEnd;
            final int minLine = Collections.min(pSet), maxLine = Collections.max(pSet);
            if (maxLine - minLine == pSet.size() - 1) { // Case contiguous indices
                selStart = StringUtils.getLineStart(text, StringUtils.getIndexFromLineOffset(text, minLine, 0));
                selEnd = StringUtils.getIndexFromLineOffset(text, maxLine, 0);
            } else { // Case non-contiguous indices
                final String[] lines = text.toString().split("\n");
                final List<String> sel = new ArrayList<>(), unsel = new ArrayList<>();
                for (int i = 0; i < lines.length; i++) {
                    (pSet.contains(i) ? sel : unsel).add(lines[i]);
                }
                sel.addAll(unsel);
                final String newText = android.text.TextUtils.join("\n", sel);
                edit.setText(newText);
                selStart = 0;
                selEnd = StringUtils.getIndexFromLineOffset(newText, positions.size() - 1, 0);
            }
            setSelectionAndShow(edit, selStart, selEnd);
        }
    }

    public static void showSelection(final TextView text) {
        showSelection(text, text.getSelectionStart(), text.getSelectionEnd());
    }

    public static void showSelection(final TextView text, final int start, final int end) {

        // Get view info
        // ------------------------------------------------------------
        final Layout layout = text.getLayout();
        if (layout == null) {
            return;
        }

        final int _start = Math.min(start, end);
        final int _end = Math.max(start, end);
        if (start < 0 || end > text.length()) {
            return;
        }
        final int lineStart = StringUtils.getLineStart(text.getText(), _start);

        final Rect viewSize = new Rect();
        if (!text.getLocalVisibleRect(viewSize)) {
            return;
        }

        // Region in Y
        // ------------------------------------------------------------
        final int selStartLine = layout.getLineForOffset(_start);
        final int lineStartLine = layout.getLineForOffset(lineStart);
        final int selStartLineTop = layout.getLineTop(selStartLine);
        final int lineStartLineTop = layout.getLineTop(lineStartLine);

        final Rect region = new Rect();

        if ((selStartLine - lineStartLine) <= 3) {
            // good to see the start of the line if close enough
            region.top = lineStartLineTop;
        } else {
            region.top = selStartLineTop;
        }

        // Push the top to the top
        region.bottom = region.top + viewSize.height();

        // Region in X - as handling RTL, text alignment, and centred text etc is
        // a huge pain (see TextView.bringPointIntoView), we use a very simple solution.
        // ------------------------------------------------------------
        final int startLeft = (int) layout.getPrimaryHorizontal(_start);
        final int halfWidth = viewSize.width() / 2;
        // Push the start to the middle of the screen
        region.left = startLeft - halfWidth;
        region.right = startLeft + halfWidth;

        // Call in post to try to make sure we run after any pending actions
        text.post(() -> text.requestRectangleOnScreen(region));
    }

    public static void setSelectionAndShow(final EditText edit, final int start, final int... end) {
        final int _end = end != null && end.length > 0 ? end[0] : start;
        if (inRange(0, edit.length(), start, _end)) {
            edit.post(() -> {
                if (!edit.hasFocus()) {
                    edit.requestFocus();
                }

                edit.setSelection(start, _end);
                edit.post(() -> showSelection(edit, start, _end));
            });
        }
    }

    // Search for matching pairs of backticks
    // interpolate contents of backtick pair as SimpleDateFormat
    public static String interpolateEscapedDateTime(final String snip) {
        final StringBuilder interpolated = new StringBuilder();
        final StringBuilder temp = new StringBuilder();
        boolean isEscaped = false;
        boolean inDate = false;
        for (int i = 0; i < snip.length(); i++) {
            final char c = snip.charAt(i);
            if (c == '\\' && !isEscaped) {
                isEscaped = true;
            } else if (isEscaped) {
                isEscaped = false;
                temp.append(c);
            } else if (c == '`' && inDate) { // Ending a date region
                inDate = false;
                interpolated.append(ShareUtil.formatDateTime((Locale) null, temp.toString(), System.currentTimeMillis()));
                temp.setLength(0); // clear
            } else if (c == '`') { // Starting a date region
                inDate = true;
                interpolated.append(temp);
                temp.setLength(0); // clear
            } else {
                temp.append(c);
            }
        }
        interpolated.append(inDate ? "`" : ""); // Mismatched backtick, just add it literally
        interpolated.append(temp); // Remaining text
        return interpolated.toString();
    }

    // Find the smallest single difference region { a, b, c }
    // s.t. setting dest[a:b] = source[a:c] makes dest == source
    public static int[] findDiff(final CharSequence dest, final CharSequence source) {

        final int dl = dest.length(), sl = source.length();
        final int minLength = Math.min(dl, sl);

        int start = 0;
        while(start < minLength && source.charAt(start) == dest.charAt(start)) start++;

        // Handle several special cases
        if (sl == dl && start == sl) { // Case where 2 sequences are same
            return new int[] { sl, sl, sl };
        } else if (sl < dl && start == sl) { // Pure crop
            return new int[] { sl, dl, sl };
        } else if (dl < sl && start == dl) { // Pure append
            return new int[] { dl, dl, sl };
        }

        int end = 0;
        final int maxEnd = minLength - start;
        while(end < maxEnd && source.charAt(sl - end - 1) == dest.charAt(dl - end - 1)) end++;

        return new int[] { start, dl - end, sl - end };
    }

    /**
     * Allows convenient chunking of actions on an editable.
     * This works by maintaining a _reference_ to an editable to which all operations are passed.
     * When a _change_ is made, the original reference is copied and all operations are
     * applied to the copy. Finally, `applyChanges()` diffs the original and copy and makes
     * a single chunked change.
     */
    public static class ChunkedEditable implements Editable {

        private final Editable original;
        private Editable copy;

        public static ChunkedEditable wrap(@NonNull final Editable e) {
            return (e instanceof ChunkedEditable) ? (ChunkedEditable) e : new ChunkedEditable(e);
        }

        private ChunkedEditable(@NonNull final Editable e) {
            original = e;
        }

        public boolean hasChanges() {
            return copy != null;
        }

        // Apply changes from copy to original
        public boolean applyChanges() {
            if (!hasChanges()) {
                return false;
            }

            final int[] diff = StringUtils.findDiff(original, copy);
            if (diff[0] != diff[1] || diff[0] != diff[2]) {
                original.replace(diff[0], diff[1], copy.subSequence(diff[0], diff[2]));
                copy = null; // Reset as we have applied all changed
                return true;
            }
            return false;
        }

        private void makeCopyIfNeeded() {
            if (copy == null) {
                // All operations will now run on copy
                // SpannableStringBuilder maintains spans etc
                copy = new SpannableStringBuilder(original);
            }
        }

        private Editable select() {
            return hasChanges() ? copy : original;
        }

        // All other functions which edit the editable alias this routine
        @Override
        public Editable replace(int st, int en, CharSequence source, int start, int end) {
            makeCopyIfNeeded();
            return select().replace(st, en, source, start, end);
        }

        // Convenience functions for replace ^. All these are just aliases
        // -------------------------------------------------------------------------------

        @Override
        public Editable replace(int st, int en, CharSequence text) {
            return replace(st, en, text, 0, text.length());
        }

        @Override
        public Editable insert(int where, CharSequence text, int start, int end) {
            return replace(where, where, text, start, end);
        }

        @Override
        public Editable insert(int where, CharSequence text) {
            return replace(where, where, text, 0, text.length());
        }

        @Override
        public Editable delete(int st, int en) {
            return replace(st, en, "", 0, 0);
        }

        @NonNull
        @Override
        public Editable append(CharSequence text) {
            return replace(length(), length(), text, 0, text.length());
        }

        @NonNull
        @Override
        public Editable append(CharSequence text, int start, int end) {
            return replace(length(), length(), text, start, end);
        }

        @NonNull
        @Override
        public Editable append(char text) {
            return append(String.valueOf(text));
        }

        @Override
        public void clear() {
            replace(0, length(), "", 0, 0);
        }

        // Other functions - all just forwarded to copy or original as needed
        // -------------------------------------------------------------------------------
        @Override
        public void clearSpans() {
            select().clearSpans();
        }

        @Override
        public void setFilters(InputFilter[] filters) {
            select().setFilters(filters);
        }

        @Override
        public InputFilter[] getFilters() {
            return select().getFilters();
        }

        @Override
        public void getChars(int start, int end, char[] dest, int destoff) {
            select().getChars(start, end, dest, destoff);
        }

        @Override
        public void setSpan(Object what, int start, int end, int flags) {
            select().setSpan(what, start, end, flags);
        }

        @Override
        public void removeSpan(Object what) {
            select().removeSpan(what);
        }

        @Override
        public <T> T[] getSpans(int start, int end, Class<T> type) {
            return select().getSpans(start, end, type);
        }

        @Override
        public int getSpanStart(Object tag) {
            return select().getSpanStart(tag);
        }

        @Override
        public int getSpanEnd(Object tag) {
            return select().getSpanEnd(tag);
        }

        @Override
        public int getSpanFlags(Object tag) {
            return select().getSpanFlags(tag);
        }

        @Override
        public int nextSpanTransition(int start, int limit, Class type) {
            return select().nextSpanTransition(start, limit, type);
        }

        @Override
        public int length() {
            return select().length();
        }

        @Override
        public char charAt(int index) {
            return select().charAt(index);
        }

        @NonNull
        @Override
        public CharSequence subSequence(int start, int end) {
            return select().subSequence(start, end);
        }
    }
}
