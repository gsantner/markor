/*#######################################################
 *
 *   Maintained 2018-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.frontend.textview;

import android.text.Editable;
import android.text.InputFilter;
import android.text.Selection;
import android.text.Spanned;

import net.gsantner.opoc.format.GsTextUtils;

import java.util.EmptyStackException;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoTextFormatter implements InputFilter {

    private final FormatPatterns _patterns;

    public AutoTextFormatter(FormatPatterns patterns) {
        _patterns = patterns;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        try {
            if (start < source.length() && dstart <= dest.length() && GsTextUtils.isNewLine(source, start, end)) {
                return autoIndent(source, dest, dstart, dend);
            }
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            e.printStackTrace();
        }

        return source;
    }

    private CharSequence autoIndent(final CharSequence source, final CharSequence dest, final int dstart, final int dend) {

        final OrderedListLine oLine = new OrderedListLine(dest, dstart, _patterns);
        final UnOrderedOrCheckListLine uLine = new UnOrderedOrCheckListLine(dest, dstart, _patterns);
        final String indent;
        if (oLine.indent < 0 || oLine.indent > oLine.line.length()) {
            indent = source.toString();
        } else {
            // Copy indent from previous line
            indent = source + oLine.line.substring(0, oLine.indent);
        }

        final String result;
        if (oLine.isOrderedList && oLine.lineEnd != oLine.groupEnd && dend >= oLine.groupEnd) {
            result = indent + String.format("%s%c ", getNextOrderedValue(oLine.value, false), oLine.delimiter);
        } else if (uLine.isUnorderedOrCheckList && uLine.lineEnd != uLine.groupEnd && dend >= uLine.groupEnd) {
            String itemPrefix = uLine.newItemPrefix;
            result = indent + itemPrefix;
        } else {
            result = indent;
        }

        return result;
    }

    public static class FormatPatterns {
        public final Pattern prefixUnorderedList;
        public final Pattern prefixCheckBoxList;
        public final Pattern prefixOrderedList;
        public final int indentSlack;

        public FormatPatterns(final Pattern prefixUnorderedList,
                              final Pattern prefixCheckBoxList,
                              final Pattern prefixOrderedList,
                              final int indentSlack) {
            this.prefixUnorderedList = prefixUnorderedList;
            this.prefixCheckBoxList = prefixCheckBoxList;
            this.prefixOrderedList = prefixOrderedList;
            this.indentSlack = indentSlack;
        }
    }

    public static class ListLine {
        protected final FormatPatterns patterns;
        protected final CharSequence text;

        public final int lineStart, lineEnd;
        public final String line;
        public final int indent;
        public final boolean isEmpty;
        public final boolean isTopLevel;

        public ListLine(CharSequence text, int position, FormatPatterns patterns) {
            this.text = text;
            this.patterns = patterns;

            lineStart = TextViewUtils.getLineStart(text, position);
            lineEnd = TextViewUtils.getLineEnd(text, position);
            indent = TextViewUtils.getNextNonWhitespace(text, lineStart) - lineStart;
            line = text.subSequence(lineStart, lineEnd).toString();
            isEmpty = (lineEnd - lineStart) == indent;
            isTopLevel = indent <= patterns.indentSlack;
        }

        // Empty lines are children of any line level
        public boolean isParentLevelOf(final ListLine line) {
            return line.isEmpty || (!isEmpty && (line.indent - indent) > patterns.indentSlack);
        }

        public boolean isChildLevelOf(final ListLine line) {
            return isEmpty || (!line.isEmpty && (indent - line.indent) > patterns.indentSlack);
        }

        public boolean isSiblingLevelOf(final ListLine line) {
            return !isParentLevelOf(line) && !isChildLevelOf(line);
        }

        @Override
        public boolean equals(final Object obj) {
            final ListLine other = obj instanceof ListLine ? (ListLine) obj : null;
            return other == this || (other != null && lineStart == other.lineStart && lineEnd == other.lineEnd && line.equals(other.line));
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

        public final boolean isOrderedList;
        public final char delimiter;
        public final int numStart, numEnd;
        public final int groupStart, groupEnd;
        public final String value;

        public OrderedListLine(CharSequence text, int position, FormatPatterns patterns) {
            super(text, position, patterns);

            final Matcher match = patterns.prefixOrderedList.matcher(line);
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
            return isOrderedList && line.isOrderedList && delimiter == line.delimiter && Math.abs(indent - line.indent) <= patterns.indentSlack;
        }

        public OrderedListLine getLevelStart() {
            OrderedListLine listStart = this;

            if (lineStart > patterns.indentSlack) {
                boolean matching;
                OrderedListLine line = this;
                do {
                    int position = line.lineStart - 1;
                    line = new OrderedListLine(text, position, patterns);
                    matching = isMatchingList(line);
                    if (matching) {
                        listStart = line;
                    }
                } while (line.lineStart > patterns.indentSlack && (matching || isParentLevelOf(line)));
            }

            return listStart;
        }

        public OrderedListLine getParent() {
            OrderedListLine line = null;
            if ((lineStart > 0) && (isEmpty || !isTopLevel)) {
                int position = lineStart - 1;
                do {
                    line = new OrderedListLine(text, position, patterns);
                    position = line.lineStart - 1;
                } while (position > 0 && !line.isParentLevelOf(this));
            }
            return line;
        }

        public OrderedListLine getNext() {
            final int nextLineStart = lineEnd + 1;
            if (nextLineStart < text.length()) {
                return new OrderedListLine(text, nextLineStart, patterns);
            }
            return null;
        }

        public OrderedListLine recreate() {
            return new OrderedListLine(text, lineStart, patterns);
        }
    }

    /**
     * Detects "real" unordered lists and checklists (both are treated as unordered lists).
     */
    public static class UnOrderedOrCheckListLine extends ListLine {
        private static final int FULL_ITEM_PREFIX_GROUP = 2;
        private static final int CHECKBOX_PREFIX_LEFT_GROUP = 3;
        private static final int CHECKBOX_PREFIX_RIGHT_GROUP = 4;

        public final boolean isUnorderedOrCheckList;
        public final String newItemPrefix;
        public final int groupStart, groupEnd;

        public UnOrderedOrCheckListLine(CharSequence text, int position, FormatPatterns patterns) {
            super(text, position, patterns);

            final Matcher checklistMatcher = patterns.prefixCheckBoxList.matcher(line);
            final Matcher unorderedListMatcher = patterns.prefixUnorderedList.matcher(line);

            final boolean isChecklist = checklistMatcher.find();
            isUnorderedOrCheckList = isChecklist || unorderedListMatcher.find();  // prefer checklist pattern to avoid spurious unordered list match
            final Matcher matcher = isChecklist ? checklistMatcher : unorderedListMatcher;

            if (isUnorderedOrCheckList) {
                groupStart = lineStart + matcher.start(FULL_ITEM_PREFIX_GROUP);
                groupEnd = lineStart + matcher.end(FULL_ITEM_PREFIX_GROUP);
                final String emptyCheckboxContent = " ";
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
     * @param text     CharSequence
     * @param position Position within current line
     * @return OrderedListLine corresponding to top of current list
     */
    private static OrderedListLine getOrderedListStart(final CharSequence text, int position, final FormatPatterns patterns) {
        position = Math.max(Math.min(position, text.length() - 1), 0);
        OrderedListLine listStart = new OrderedListLine(text, position, patterns);

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
     * This is an unfortunately complex + complicated function. Tweak at your peril and test a *lot* :)
     */
    public static void renumberOrderedList(final Editable edit, final FormatPatterns patterns) {

        final int[] sel = TextViewUtils.getSelection(edit);
        if (!GsTextUtils.inRange(0, edit.length(), sel)) {
            return;
        }

        final TextViewUtils.ChunkedEditable chunked = TextViewUtils.ChunkedEditable.wrap(edit);

        final int[] shifts = new int[]{0, 0};

        // Top of list
        final OrderedListLine firstLine = getOrderedListStart(chunked, sel[0], patterns);
        if (!firstLine.isOrderedList) {
            return;
        }

        // Stack represents each level in the list up from current
        final Stack<OrderedListLine> levels = new Stack<>();
        levels.push(firstLine);

        try {
            // Loop to end of list
            OrderedListLine line = firstLine;
            while (line != null && (firstLine.isParentLevelOf(line) || firstLine.isMatchingList(line))) {

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
                    final String newValue = getNextOrderedValue(peek.value, line.equals(peek));
                    if (!newValue.equals(line.value)) {
                        final int delta = newValue.length() - line.value.length();
                        if (line.numEnd < sel[0]) {
                            shifts[0] += delta;
                        }
                        if (line.numEnd < sel[1]) {
                            shifts[1] += delta;
                        }

                        chunked.replace(line.numStart, line.numEnd, newValue);
                        line = line.recreate(); // Recreate as line has changed
                    }
                    levels.pop();
                    levels.push(line);
                }
                line = line.getNext();
            }

            chunked.applyChanges();

            final int[] newSel = new int[]{sel[0] + shifts[0], sel[1] + shifts[1]};
            if (GsTextUtils.inRange(0, edit.length(), newSel)) {
                Selection.setSelection(edit, newSel[0], newSel[1]);
            }

        } catch (EmptyStackException ex) {
            // Usually means that indents and de-indents did not match up
            ex.printStackTrace();
        }
    }

    private static String getNextOrderedValue(final String currentValue, final boolean restart) {
        final Pattern numberPattern = Pattern.compile("\\d+");
        final Pattern lowercaseLetterPattern = Pattern.compile("[a-z]");
        final Pattern capitalLetterPattern = Pattern.compile("[A-z]");

        if (numberPattern.matcher(currentValue).find()) {
            final int intValue = GsTextUtils.tryParseInt(currentValue, 0);
            return restart ? "1" : Integer.toString(intValue + 1);
        } else {
            final char charValue = currentValue.charAt(0);
            if (lowercaseLetterPattern.matcher(currentValue).find()) {
                return (restart || charValue >= 'z') ? "a" : String.valueOf(charValue + 1);
            } else if (capitalLetterPattern.matcher(currentValue).find()) {
                return (restart || charValue >= 'Z') ? "A" : String.valueOf(charValue + 1);
            }
        }
        return "0";
    }
}
