/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format;

import android.annotation.SuppressLint;
import android.text.Editable;
import android.text.Spanned;

import net.gsantner.opoc.util.StringUtils;

import java.util.EmptyStackException;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoFormatter {

    private final PrefixPatterns _prefixPatterns;
    private final char _indentCharacter;

    private CharSequence _source;
    private int _start;
    private int _end;
    private Spanned _dest;
    private int _dstart;
    private int _dend;

    public AutoFormatter(PrefixPatterns prefixPatterns, char indentCharacter) {
        _prefixPatterns = prefixPatterns;
        _indentCharacter = indentCharacter;
    }

    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {

        initialize(source, start, end, dest, dstart, dend);

        try {
            if (_start < _source.length() && _dstart <= _dest.length() && StringUtils.isNewLine(_source, _start, _end)) {
                return autoIndent();
            }
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            e.printStackTrace();
        }
        return _source;
    }

    private void initialize(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        _source = source;
        _start = start;
        _end = end;
        _dest = dest;
        _dstart = dstart;
        _dend = dend;
    }

    @SuppressLint("DefaultLocale")
    private CharSequence autoIndent() {

        final OrderedListLine oLine = new OrderedListLine(_dest, _dstart, _prefixPatterns);
        final UnOrderedOrCheckListLine uLine = new UnOrderedOrCheckListLine(_dest, _dstart, _prefixPatterns);
        final String indent = _source + StringUtils.repeatChars(_indentCharacter, oLine.indent);

        final String result;
        if (oLine.isOrderedList && oLine.lineEnd != oLine.groupEnd && _dend >= oLine.groupEnd) {
            result = indent + String.format("%s%c ", getNextOrderedValue(oLine.value), oLine.delimiter);
        } else if (uLine.isUnorderedOrCheckList && uLine.lineEnd != uLine.groupEnd && _dend >= uLine.groupEnd) {
            String itemPrefix = uLine.newItemPrefix;
            result = indent + itemPrefix;
        } else {
            result = indent;
        }

        return result;
    }

    public static class PrefixPatterns {
        public final Pattern prefixUnorderedList;
        public final Pattern prefixCheckBoxList;
        public final Pattern prefixOrderedList;

        public PrefixPatterns(Pattern prefixUnorderedList, Pattern prefixCheckBoxList, Pattern prefixOrderedList) {
            this.prefixUnorderedList = prefixUnorderedList;
            this.prefixCheckBoxList = prefixCheckBoxList;
            this.prefixOrderedList = prefixOrderedList;
        }
    }

    public static class ListLine {
        protected static final int INDENT_DELTA = 2;

        protected final CharSequence text;

        public final int lineStart, lineEnd;
        public final String line;
        public final int indent;
        public final boolean isEmpty;
        public final boolean isTopLevel;

        public ListLine(CharSequence text, int position) {

            this.text = text;
            lineStart = StringUtils.getLineStart(text, position);
            lineEnd = StringUtils.getLineEnd(text, position);
            line = text.subSequence(lineStart, lineEnd).toString();
            indent = StringUtils.getNextNonWhitespace(text, lineStart) - lineStart;
            isEmpty = (lineEnd - lineStart) == indent;
            isTopLevel = indent <= INDENT_DELTA;
        }

        // Empty lines are children of any line level
        public boolean isParentLevelOf(final ListLine line) {
            return line.isEmpty || (!isEmpty && (line.indent - indent) > INDENT_DELTA);
        }

        public boolean isChildLevelOf(final ListLine line) {
            return isEmpty || (!line.isEmpty && (indent - line.indent) > INDENT_DELTA);
        }

        public boolean isSiblingLevelOf(final ListLine line) {
            return !isParentLevelOf(line) && !isChildLevelOf(line);
        }
    }

    /**
     * Class to parse a line of text and extract useful information
     */
    public static class OrderedListLine extends ListLine {
        // TODO parametrize?
        private static final int FULL_GROUP = 2;
        private static final int VALUE_GROUP = 3;
        private static final int DELIM_GROUP = 4;

        private final PrefixPatterns prefixPatterns;

        public final boolean isOrderedList;
        public final char delimiter;
        public final int numStart, numEnd;
        public final int groupStart, groupEnd;
        public final String value;

        public OrderedListLine(CharSequence text, int position, PrefixPatterns prefixPatterns) {
            super(text, position);
            this.prefixPatterns = prefixPatterns;

            final Matcher match = prefixPatterns.prefixOrderedList.matcher(line);
            isOrderedList = match.find();
            if (isOrderedList) {
                delimiter = match.group(DELIM_GROUP).charAt(0);
                numStart = match.start(VALUE_GROUP) + lineStart;
                numEnd = match.end(VALUE_GROUP) + lineStart;
                value = match.group(VALUE_GROUP);
                groupStart = lineStart + match.start(FULL_GROUP);
                groupEnd = lineStart + match.end(FULL_GROUP);
            } else {
                groupEnd = groupStart = numStart = numEnd = -1;
                value = "";
                delimiter = 0;
            }
        }

        public boolean isMatchingList(final OrderedListLine line) {
            return isOrderedList && line.isOrderedList && delimiter == line.delimiter && Math.abs(indent - line.indent) <= INDENT_DELTA;
        }

        public OrderedListLine getLevelStart() {
            OrderedListLine listStart = this;

            if (lineStart > INDENT_DELTA) {
                boolean matching;
                OrderedListLine line = this;
                do {
                    int position = line.lineStart - 1;
                    line = new OrderedListLine(text, position, prefixPatterns);
                    matching = isMatchingList(line);
                    if (matching) {
                        listStart = line;
                    }
                } while (line.lineStart > INDENT_DELTA && (matching || isParentLevelOf(line)));
            }

            return listStart;
        }

        public OrderedListLine getParent() {
            OrderedListLine line = null;
            if ((lineStart > 0) && (isEmpty || !isTopLevel)) {
                int position = lineStart - 1;
                do {
                    line = new OrderedListLine(text, position, prefixPatterns);
                    position = line.lineStart - 1;
                } while (position > 0 && !line.isParentLevelOf(this));
            }
            return line;
        }
    }

    /**
     * Detects "real" unordered lists and checklists (both are treated as unordered lists).
     */
    public static class UnOrderedOrCheckListLine extends ListLine {
        private static final int FULL_ITEM_PREFIX_GROUP = 2;
        private static final int CHECKBOX_PREFIX_LEFT_GROUP = 3;
        private static final int CHECKBOX_PREFIX_RIGHT_GROUP = 4;

        private final PrefixPatterns prefixPatterns;

        public final boolean isUnorderedOrCheckList;
        public final String newItemPrefix;
        public final int groupStart, groupEnd;

        public UnOrderedOrCheckListLine(CharSequence text, int position, PrefixPatterns prefixPatterns) {
            super(text, position);
            this.prefixPatterns = prefixPatterns;

            final Matcher checklistMatcher = prefixPatterns.prefixCheckBoxList.matcher(line);
            final Matcher unorderedListMatcher = prefixPatterns.prefixUnorderedList.matcher(line);

            final boolean isChecklist = checklistMatcher.find();
            isUnorderedOrCheckList = isChecklist || unorderedListMatcher.find();  // prefer checklist pattern to avoid spurious unordered list match
            final Matcher matcher = isChecklist ? checklistMatcher : unorderedListMatcher;

            if (isUnorderedOrCheckList) {
                groupStart = lineStart + matcher.start(FULL_ITEM_PREFIX_GROUP);
                groupEnd = lineStart + matcher.end(FULL_ITEM_PREFIX_GROUP);
                String emptyCheckboxContent = " ";
                newItemPrefix = isChecklist ? matcher.group(CHECKBOX_PREFIX_LEFT_GROUP) + emptyCheckboxContent + matcher.group(CHECKBOX_PREFIX_RIGHT_GROUP)
                        : matcher.group(FULL_ITEM_PREFIX_GROUP);
            } else {
                groupStart = groupEnd = -1;
                newItemPrefix = "";
            }
        }
    }


    /**
     * Find the topmost orderd list item which is a parent of the current
     *
     * @param text     Editable
     * @param position Position within current line
     * @return OrderedListLine corresponding to top of current list
     */
    private static OrderedListLine getOrderedListStart(final Editable text, int position, final PrefixPatterns prefixPatterns) {
        position = Math.max(Math.min(position, text.length() - 1), 0);
        OrderedListLine listStart = new OrderedListLine(text, position, prefixPatterns);

        OrderedListLine line = listStart;
        do {
            line = line.getParent();
            if (line != null && line.isOrderedList) {
                listStart = line;
            }
        } while (line != null);

        if (listStart.isOrderedList || listStart.isEmpty) {
            listStart = listStart.getLevelStart();
        }

        return listStart;
    }

    /**
     * This function will first walk up to the top of the current list
     * and then walk down to the end, renumbering ordered list items along the way
     * <p>
     * Sub-lists and other children will be skipped.
     */
    public static void renumberOrderedList(Editable text, int cursorPosition, final PrefixPatterns prefixPatterns) {

        // Top of list
        final OrderedListLine firstLine = getOrderedListStart(text, cursorPosition, prefixPatterns);
        int position = firstLine.lineEnd + 1;

        if (firstLine.isOrderedList && position < text.length()) {
            // Stack represents
            final Stack<OrderedListLine> levels = new Stack<>();
            levels.push(firstLine);

            OrderedListLine line = firstLine;

            try {
                // Loop to end of list
                do {
                    line = new OrderedListLine(text, position, prefixPatterns);

                    if (!(firstLine.isParentLevelOf(line) || firstLine.isMatchingList(line))) {
                        // List is over
                        break;
                    }

                    if (line.isOrderedList) {
                        // Indented. Add level
                        if (line.isChildLevelOf(levels.peek())) {
                            levels.push(line);
                        }
                        // Dedented. Remove appropriate number of levels
                        else if (line.isParentLevelOf(levels.peek())) {
                            while (levels.peek().isChildLevelOf(line)) {
                                levels.pop();
                            }
                        }

                        // Restart if bullet does not match list at this level
                        if (line != levels.peek() && !levels.peek().isMatchingList(line)) {
                            levels.pop();
                            levels.push(line);
                        }
                    }
                    // Non-ordered non-empty line. Pop back to parent level
                    else if (!line.isEmpty) {
                        while (!levels.isEmpty() && !levels.peek().isParentLevelOf(line)) {
                            levels.pop();
                        }
                    }

                    // Update numbering if needed
                    if (line.isOrderedList) {

                        // Restart numbering if list changes
                        final OrderedListLine peek = levels.peek();
                        final String newValue = (line == peek) ? "1" : getNextOrderedValue(peek.value);
                        if (!newValue.equals(line.value)) {
                            text.replace(line.numStart, line.numEnd, newValue);

                            // Re-create line as it has changed
                            line = new OrderedListLine(text, line.lineStart, prefixPatterns);
                        }

                        levels.pop();
                        levels.push(line);
                    }

                    position = line.lineEnd + 1;
                } while (position < text.length() && position > 0);

            } catch (EmptyStackException ignored) {
                // Usually means that indents and de-indents did not match up
                ignored.printStackTrace();
            }
        }
    }

    private static String getNextOrderedValue(String currentValue) {
        final Pattern numberPattern = Pattern.compile("\\d+");
        final Pattern lowercaseLetterPattern = Pattern.compile("[a-z]");
        final Pattern capitalLetterPattern = Pattern.compile("[A-z]");

        if (numberPattern.matcher(currentValue).find()) {
            int intValue = Integer.parseInt(currentValue);
            return Integer.toString(intValue + 1);
        } else {
            char charValue = currentValue.charAt(0);
            if (lowercaseLetterPattern.matcher(currentValue).find()) {
                return charValue < 'z' ? "" + (char) (charValue + 1) : "" + 'a';
            } else if (capitalLetterPattern.matcher(currentValue).find()) {
                return charValue < 'Z' ? "" + (char) (charValue + 1) : "" + 'A';
            }
        }
        return "0";
    }
}
