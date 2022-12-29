/*#######################################################
 *
 * SPDX-FileCopyrightText: 2018-2022 Gregor Santner <https://gsantner.net/>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2018-2022 by Gregor Santner <https://gsantner.net/>
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
#########################################################*/
package net.gsantner.markor.frontend.textview;

import android.animation.ObjectAnimator;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Layout;
import android.text.Selection;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.gsantner.opoc.format.GsTextUtils;
import net.gsantner.opoc.util.GsContextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

@SuppressWarnings({"CharsetObjectCanBeUsed", "WeakerAccess", "unused"})
public final class TextViewUtils extends GsTextUtils {

    // Suppress default constructor for noninstantiability
    private TextViewUtils() {
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

    public static int getLastNonWhitespace(final CharSequence s, final int start) {
        for (int i = Math.min(s.length() - 1, start); i >= 0; i--) {
            char c = s.charAt(i);
            if (c != ' ' && c != '\t') {
                return i;
            }
        }
        return -1;
    }

    public static int getNextNonWhitespace(final CharSequence s, final int start) {
        final int length = s.length();
        for (int i = Math.max(0, start); i < length; i++) {
            char c = s.charAt(i);
            if (c != ' ' && c != '\t') {
                return i;
            }
        }
        return -1;
    }

    public static int[] getSelection(final TextView text) {
        return getSelection(text.getText());
    }

    // CharSequence must be an instance of _Spanned_
    public static int[] getSelection(final CharSequence text) {

        final int selectionStart = Selection.getSelectionStart(text);
        final int selectionEnd = Selection.getSelectionEnd(text);

        if (selectionEnd >= selectionStart) {
            return new int[]{selectionStart, selectionEnd};
        } else {
            return new int[]{selectionEnd, selectionStart};
        }
    }

    public static int[] getLineSelection(final CharSequence text, final int[] sel) {
        return sel != null && sel.length >= 2 ? new int[]{getLineStart(text, sel[0]), getLineEnd(text, sel[1])} : null;
    }

    public static int[] getLineSelection(final TextView text) {
        return getLineSelection(text.getText(), getSelection(text));
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
        final int line = countChars(s, 0, p, '\n')[0];
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

    public static boolean isNewLine(CharSequence source, int start, int end) {
        return isValidIndex(source, start, end - 1) && (source.charAt(start) == '\n' || source.charAt(end - 1) == '\n');
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
            final int posn = TextViewUtils.getIndexFromLineOffset(text, positions.get(0), 0);
            setSelectionAndShow(edit, posn);
        } else if (positions.size() > 1) {
            final TreeSet<Integer> pSet = new TreeSet<>(positions);
            final int selStart, selEnd;
            final int minLine = Collections.min(pSet), maxLine = Collections.max(pSet);
            if (maxLine - minLine == pSet.size() - 1) { // Case contiguous indices
                selStart = TextViewUtils.getLineStart(text, TextViewUtils.getIndexFromLineOffset(text, minLine, 0));
                selEnd = TextViewUtils.getIndexFromLineOffset(text, maxLine, 0);
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
                selEnd = TextViewUtils.getIndexFromLineOffset(newText, positions.size() - 1, 0);
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
        final int lineStart = TextViewUtils.getLineStart(text.getText(), _start);

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

    public static void setSelectionAndShow(final EditText edit, final int... sel) {
        if (sel == null || sel.length == 0) {
            return;
        }

        final int start = sel[0];
        final int end = sel.length > 1 ? sel[1] : start;

        if (inRange(0, edit.length(), start, end)) {
            edit.post(() -> {
                if (!edit.hasFocus()) {
                    edit.requestFocus();
                }

                edit.setSelection(start, end);
                edit.post(() -> showSelection(edit, start, end));
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
                interpolated.append(GsContextUtils.instance.formatDateTime((Locale) null, temp.toString(), System.currentTimeMillis()));
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

    /**
     * Find the smallest single diff from source -> dest
     *
     * @param dest      Into which we want to apply the diff
     * @param source    From which we want to apply the diff
     * @param startSkip Don't check the first startSkip chars for sameness
     * @param endSkip   Don't check the first startSkip chars for sameness
     * @return { a, b, c } s.t. setting dest[a:b] = source[a:c] makes dest == source
     */
    public static int[] findDiff(final CharSequence dest, final CharSequence source, final int startSkip, final int endSkip) {
        final int[] diff = findDiff(dest, startSkip, dest.length() - endSkip, source, startSkip, source.length() - endSkip);
        return new int[]{diff[0], diff[1], diff[3]};
    }

    /**
     * Find the smallest single diff from source -> dest
     *
     * @param dest   Into which we want to apply the diff
     * @param ds     Dest start region
     * @param dn     Dest end region
     * @param source From which we want to apply the diff
     * @param ss     Source start region
     * @param sn     Dest end region
     * @return { a, b, c, d } s.t. setting dest[a:b] = source[c:d] will make dest[ds:dn] == source[ss:sn]
     */
    public static int[] findDiff(final CharSequence dest, final int ds, final int dn, final CharSequence source, final int ss, final int sn) {
        final int dl = Math.max(dn - ds, 0), sl = Math.max(sn - ss, 0);
        final int minLength = Math.min(dl, sl);

        int start = 0;
        while (start < minLength && source.charAt(start + ss) == dest.charAt(start + ds)) start++;

        // Handle several special cases
        if (sl == dl && start == sl) { // Case where 2 sequences are same
            return new int[]{dn, dn, sn, sn};
        } else if (sl < dl && start == sl) { // Pure crop
            return new int[]{ds + start, dn, sn, sn};
        } else if (dl < sl && start == dl) { // Pure append
            return new int[]{dn, dn, start + ss, sn};
        }

        int end = 0;
        final int maxEnd = minLength - start;
        while (end < maxEnd && source.charAt(sn - end - 1) == dest.charAt(dn - end - 1)) end++;

        return new int[]{ds + start, dn - end, ss + start, sn - end};
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
        private StringBuilder copy;
        private int _startSkip = 0;
        private int _endSkip = 0;

        public static ChunkedEditable wrap(@NonNull final Editable e) {
            return (e instanceof ChunkedEditable) ? (ChunkedEditable) e : new ChunkedEditable(e);
        }

        private ChunkedEditable(@NonNull final Editable e) {
            original = e;
        }

        // Apply changes from copy to original
        public boolean applyChanges() {
            if (copy == null) {
                return false;
            }

            final int[] diff = TextViewUtils.findDiff(original, copy, _startSkip, Math.max(_endSkip, 0));
            final boolean hasDiff = diff[0] != diff[1] || diff[0] != diff[2];
            if (hasDiff) {
                original.replace(diff[0], diff[1], copy.subSequence(diff[0], diff[2]));
            }
            copy = null; // Reset as we have applied all changed
            return hasDiff;
        }

        // All other functions which edit the editable alias this routine
        @Override
        public Editable replace(int st, int en, CharSequence source, int start, int end) {
            // Replace minimal region, only if actually required - replacing is expensive
            final int[] diff = findDiff((copy != null ? copy : original), st, en, source, start, end);
            if (diff[0] != diff[1] || diff[2] != diff[3]) {
                if (copy == null) {
                    // All operations will now run on copy
                    // SpannableStringBuilder maintains spans etc
                    copy = new StringBuilder(original);
                    _startSkip = _endSkip = copy.length();
                }
                _startSkip = Math.min(_startSkip, diff[0]);
                _endSkip = Math.min(_endSkip, copy.length() - diff[1] - 1);
                copy.replace(diff[0], diff[1], TextViewUtils.toString(source, diff[2], diff[3]));
            }
            return this;
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
        public int length() {
            return (copy != null ? copy : original).length();
        }

        @Override
        public char charAt(int index) {
            return (copy != null ? copy : original).charAt(index);
        }

        @NonNull
        @Override
        public CharSequence subSequence(int start, int end) {
            return (copy != null ? copy : original).subSequence(start, end);
        }

        @Override
        public void getChars(int start, int end, char[] dest, int destoff) {
            TextUtils.getChars(copy != null ? copy : original, start, end, dest, destoff);
        }

        // All spannable things unsupported
        // -------------------------------------------------------------------------------
        @Override
        public void clearSpans() {
            // Do nothing
        }

        @Override
        public void setFilters(InputFilter[] filters) {
            // Do nothing
        }

        @Override
        public InputFilter[] getFilters() {
            return null;
        }

        @Override
        public void setSpan(Object what, int start, int end, int flags) {
            // Do nothing
        }

        @Override
        public void removeSpan(Object what) {
            // Do nothing
        }

        @Override
        public <T> T[] getSpans(int start, int end, Class<T> type) {
            return null;
        }

        @Override
        public int getSpanStart(Object tag) {
            return -1;
        }

        @Override
        public int getSpanEnd(Object tag) {
            return -1;
        }

        @Override
        public int getSpanFlags(Object tag) {
            return 0;
        }

        @Override
        public int nextSpanTransition(int start, int limit, Class type) {
            return -1;
        }
    }

    public static Runnable makeDebounced(final long delayMs, final Runnable callback) {
        return makeDebounced(null, delayMs, callback);
    }

    // Debounce any callback
    public static Runnable makeDebounced(final Handler handler, final long delayMs, final Runnable callback) {
        final Handler _handler = handler == null ? new Handler(Looper.getMainLooper()) : handler;
        final Object sync = new Object();
        return () -> {
            synchronized (sync) {
                _handler.removeCallbacks(callback);
                if (delayMs > 0) {
                    _handler.postDelayed(callback, delayMs);
                } else {
                    _handler.post(callback);
                }
            }
        };
    }

    // Converts region to string with a minimum of work
    public static String toString(final CharSequence source, int start, int end) {
        if (source instanceof String) {
            // Already very fast
            return ((String) source).substring(start, end);
        }

        final char[] buf = new char[end - start];
        TextUtils.getChars(source, start, end, buf, 0);
        return new String(buf);
    }

    // Check if a range is valid
    public static boolean checkRange(final CharSequence seq, final int... indices) {
        return checkRange(seq.length(), indices);
    }

    public static boolean checkRange(final int length, final int... indices) {
        return indices != null && indices.length >= 2 && inRange(0, length, indices) && indices[1] > indices[0];
    }

    public static void blinkView(final View view) {
        if (view != null) {
            final float init = view.getAlpha();
            ObjectAnimator.ofFloat(view, View.ALPHA, init, 0.1f, 1.0f, 0.1f, 1.0f, init).setDuration(1000).start();
        }
    }

    public static boolean isViewVisible(final View view) {
        if (view == null || !view.isShown()) {
            return false;
        }
        final Rect actualPosition = new Rect();
        boolean isGlobalVisible = view.getGlobalVisibleRect(actualPosition);
        final DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        final Rect screen = new Rect(0, 0, metrics.widthPixels, metrics.heightPixels);
        return isGlobalVisible && Rect.intersects(actualPosition, screen);
    }

    // Check if keyboard open. Only available after android 11 :(
    public static Boolean isImeOpen(final View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return view.getRootWindowInsets().isVisible(WindowInsets.Type.ime());
        }
        return null; // Uncertain
    }
}
