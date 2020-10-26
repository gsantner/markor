package net.gsantner.markor.format;

import android.annotation.SuppressLint;
import android.text.Editable;
import android.text.Spanned;

import net.gsantner.markor.format.markdown.MarkdownReplacePatternGenerator;
import net.gsantner.opoc.util.StringUtils;

import java.util.EmptyStackException;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoFormatter {

    private Pattern _prefixOrderedList;
    private int _orderedListFullGroup;
    private int _orderedListValueGroup;
    private int _orderedListdelimGroup;

    private Pattern _prefixUnorderedList;


    private Pattern _prefixUncheckedList;

    private Pattern _prefixCheckedList;


    private CharSequence _source;
    private int _start;
    private int _end;
    private Spanned _dest;
    private int _dstart;
    private int _dend;


    public AutoFormatter(Pattern prefixOrderedList, Pattern prefixUnorderedList, Pattern prefixUncheckedList, Pattern prefixCheckedList) {
        _prefixOrderedList = prefixOrderedList;
        _prefixUnorderedList = prefixUnorderedList;
        _prefixUncheckedList = prefixUncheckedList;
        _prefixCheckedList = prefixCheckedList;
    }

    public void setUnorderedListPatternWithGroups() {
        // TODO
    }
    public void setChecklistPatternWithGroups() {
        // TODO
    }
    public void setOrderedListPatternWithGroups(Pattern prefixOrderedList, int fullGroup, int valueGroup, int delimGroup) {
        _prefixOrderedList = prefixOrderedList;
//        _orderedListFullGroup = fullGroup;
//        _orderedListValueGroup = valueGroup;
//        _orderedListdelimGroup = delimGroup;

    }
    public void setIndentDelta() {
        // TODO
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

        final String checkSymbol = "[ ] ";

        final OrderedListLine oLine = new OrderedListLine(_dest, _dstart);
        final UnOrderedListLine uLine = new UnOrderedListLine(_dest, _dstart);
        final String indent = _source + StringUtils.repeatChars(' ', oLine.indent);

        final String result;
        if (oLine.isOrderedList && oLine.lineEnd != oLine.groupEnd && _dend >= oLine.groupEnd) {
            result = indent + String.format("%d%c ", oLine.value + 1, oLine.delimiter);
        } else if (uLine.isUnorderedList && uLine.lineEnd != uLine.groupEnd && _dend >= uLine.groupEnd) {
            final String checkString = uLine.isCheckboxList ? checkSymbol : "";
            // TODO: for zim wiki, no list char is used
            result = indent + String.format("%c %s", uLine.listChar, checkString);
        } else {
            result = indent;
        }

        return result;
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

        public final boolean isOrderedList;
        public final char delimiter;
        public final int numStart, numEnd;
        public final int groupStart, groupEnd;
        public final int value;

        public OrderedListLine(CharSequence text, int position) {
            super(text, position);

            final Matcher match = MarkdownReplacePatternGenerator.PREFIX_ORDERED_LIST.matcher(line);
            isOrderedList = match.find();
            if (isOrderedList) {
                delimiter = match.group(DELIM_GROUP).charAt(0);
                numStart = match.start(VALUE_GROUP) + lineStart;
                numEnd = match.end(VALUE_GROUP) + lineStart;
                value = Integer.parseInt(match.group(VALUE_GROUP));
                groupStart = lineStart + match.start(FULL_GROUP);
                groupEnd = lineStart + match.end(FULL_GROUP);
            } else {
                groupEnd = groupStart = numStart = numEnd = value = -1;
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
                    line = new OrderedListLine(text, position);
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
                    line = new OrderedListLine(text, position);
                    position = line.lineStart - 1;
                } while (position > 0 && !line.isParentLevelOf(this));
            }
            return line;
        }
    }

    public static class UnOrderedListLine extends ListLine {
        private static final int CHECK_GROUP = 4;
        private static final int LIST_LEADER_GROUP = 3;
        private static final int FULL_GROUP = 2;

        public final boolean isUnorderedList;
        public final boolean isCheckboxList;
        public final boolean isChecked;
        public final char checkChar;
        public final char listChar;
        public final int groupStart, groupEnd;

        public UnOrderedListLine(CharSequence text, int position) {
            super(text, position);

            // TODO: parametrize prefix patterns and groups
            final Matcher ucMatch = MarkdownReplacePatternGenerator.PREFIX_UNCHECKED_LIST.matcher(line);
            final Matcher cMatch = MarkdownReplacePatternGenerator.PREFIX_CHECKED_LIST.matcher(line);
            final Matcher uMatch = MarkdownReplacePatternGenerator.PREFIX_UNORDERED_LIST.matcher(line);

            isUnorderedList = uMatch.find(); // Will also detect other unordered list types
            isChecked = cMatch.find();
            isCheckboxList = isChecked || ucMatch.find();
            checkChar = isChecked ? cMatch.group(CHECK_GROUP).charAt(0) : 0;

            if (isUnorderedList) {
                // TODO: zim does not use this for checklists
                listChar = uMatch.group(LIST_LEADER_GROUP).charAt(0);
                final Matcher match = isCheckboxList ? (isChecked ? cMatch : ucMatch) : uMatch;
                groupStart = lineStart + match.start(FULL_GROUP);
                groupEnd = lineStart + match.end(FULL_GROUP);
            } else {
                listChar = 0;
                groupStart = groupEnd = -1;
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
    private static OrderedListLine getOrderedListStart(final Editable text, int position) {
        position = Math.max(Math.min(position, text.length() - 1), 0);
        OrderedListLine listStart = new OrderedListLine(text, position);

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
    public static void renumberOrderedList(Editable text, int cursorPosition) {

        // Top of list
        final OrderedListLine firstLine = getOrderedListStart(text, cursorPosition);
        int position = firstLine.lineEnd + 1;

        if (firstLine.isOrderedList && position < text.length()) {
            // Stack represents
            final Stack<OrderedListLine> levels = new Stack<>();
            levels.push(firstLine);

            OrderedListLine line = firstLine;

            try {
                // Loop to end of list
                do {
                    line = new OrderedListLine(text, position);

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
                        final int newValue = (line == peek) ? 1 : peek.value + 1;
                        if (newValue != line.value) {
                            String number = Integer.toString(newValue);
                            text.replace(line.numStart, line.numEnd, number);

                            // Re-create line as it has changed
                            line = new OrderedListLine(text, line.lineStart);
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
}
