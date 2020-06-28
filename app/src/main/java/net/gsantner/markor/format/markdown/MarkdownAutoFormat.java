/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.markdown;

import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;

import net.gsantner.opoc.util.StringUtils;

import java.util.regex.Matcher;

public class MarkdownAutoFormat implements InputFilter {
    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        try {
            if (start < source.length()
                    && dstart <= dest.length()
                    && isNewLine(source, start, end)) {

                return autoIndent(source, dest, dstart, dend);
            }
        } catch (IndexOutOfBoundsException | NullPointerException e) {
            e.printStackTrace();
        }
        return source;
    }

    private static Boolean isNewLine(CharSequence source, int start, int end) {
        return ((source.charAt(start) == '\n') || (source.charAt(end - 1) == '\n'));
    }

    private CharSequence autoIndent(CharSequence source, Spanned dest, int dstart, int dend) {
        int iStart = StringUtils.getLineStart(dest, dstart);

        // append white space of previous line and new indent
        return source + createIndentForNextLine(dest, dend, iStart);
    }

    private String createIndentForNextLine(Spanned dest, int dend, int istart) {

        // Determine leading whitespace
        int iEnd = StringUtils.getNextNonWhitespace(dest, istart);

        // Construct whitespace
        String indentString = StringUtils.repeatChars(' ', iEnd - istart);

        String previousLine = dest.toString().substring(iEnd, dend);

        Matcher uMatch = MarkdownHighlighterPattern.LIST_UNORDERED.pattern.matcher(previousLine);
        if (uMatch.find()) {
            String bullet = uMatch.group() + " ";
            boolean emptyList = previousLine.equals(bullet);
            return indentString + (emptyList ? "" : bullet);
        }

        Matcher oMatch = MarkdownHighlighterPattern.LIST_ORDERED.pattern.matcher(previousLine);
        if (oMatch.find()) {
            boolean emptyList = previousLine.equals(oMatch.group(1) + ". ");
            return indentString + (emptyList ? "" : addNumericListItemIfNeeded(oMatch.group(1)));
        }

        return indentString;
    }

    private String addNumericListItemIfNeeded(String itemNumStr) {
        try {
            int nextC = Integer.parseInt(itemNumStr) + 1;
            return nextC + ". ";
        } catch (NumberFormatException e) {
            // This should never ever happen
            return "";
        }
    }


    private static class ListLine {

        protected static final int INDENT_DELTA = 2;

        public final int lineStart, lineEnd;
        public final String line;
        public final int indent;
        public final boolean isEmpty;

        public ListLine(CharSequence text, int position) {

            lineStart = StringUtils.getLineStart(text, position);
            lineEnd = StringUtils.getLineEnd(text, position);
            line = text.subSequence(lineStart, lineEnd).toString();
            indent = StringUtils.getNextNonWhitespace(text, lineStart) - lineStart;
            isEmpty = (lineEnd - lineStart) == indent;
        }

        public boolean isChild(final ListLine line) {
            return line.isEmpty || line.indent > (indent + INDENT_DELTA);
        }

        public boolean isParent(final ListLine line) {
            return !line.isEmpty && line.indent < (indent - INDENT_DELTA);
        }
    }

    /**
     * Class to parse a line of text and extract useful information
     */
    private static class OrderedListLine extends ListLine {
        private static final int VALUE_GROUP = 3;
        private static final int DELIM_GROUP = 4;

        public final boolean isOrderedList;
        public final char delimiter;
        public final int numStart, numEnd;
        public final int value;

        public OrderedListLine(CharSequence text, int position) {
            super(text, position);

            final Matcher match = MarkdownTextActions.PREFIX_ORDERED_LIST.matcher(line);
            isOrderedList = match.find();
            if (isOrderedList) {
                delimiter = match.group(DELIM_GROUP).charAt(0);
                numStart = match.start(VALUE_GROUP) + lineStart;
                numEnd = match.end(VALUE_GROUP) + lineStart;
                value = Integer.parseInt(match.group(VALUE_GROUP));
            } else {
                numStart = numEnd = value = -1;
                delimiter = 0;
            }
        }

        public boolean isMatchingList(final OrderedListLine line) {
            final boolean bothOrderedlists = isOrderedList && line.isOrderedList;
            final boolean sameIndent = Math.abs(indent - line.indent) <= ListLine.INDENT_DELTA;
            final boolean sameDelimiter = delimiter == line.delimiter;
            return bothOrderedlists && sameIndent && sameDelimiter;
        }
    }

    private static class UnOrderedListLine extends ListLine {
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

            final Matcher ucMatch = MarkdownTextActions.PREFIX_UNCHECKED_LIST.matcher(line);
            final Matcher cMatch = MarkdownTextActions.PREFIX_CHECKED_LIST.matcher(line);
            final Matcher uMatch = MarkdownTextActions.PREFIX_UNORDERED_LIST.matcher(line);

            isUnorderedList = uMatch.find(); // Will also catch other unordered list types
            isCheckboxList = ucMatch.find() || cMatch.find();
            isChecked = cMatch.find() && !ucMatch.find();

            if (isChecked) {
                checkChar = cMatch.group(CHECK_GROUP).charAt(0);
            } else {
                checkChar = 0;
            }

            if (isUnorderedList) {
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
     * Walks to the top of the current list at the current level
     * <p>
     * This function will not walk to parent levels!
     *
     * @param searchStart position to start search at
     * @return OrderedLine corresponding to top of the list
     */
    private static OrderedListLine getOrderedListStart(Editable text, final int searchStart) {

        int position = Math.max(Math.min(searchStart, text.length() - 1), 0);

        OrderedListLine line, listStart = null, startLine = null;

        do {
            line = new OrderedListLine(text, position);

            if (startLine == null) {
                startLine = line;
                if (!startLine.isOrderedList) {
                    break;
                }
            }

            if (startLine.isMatchingList(line)) {
                listStart = line;
            }

            position = line.lineStart - 1;

        } while (position > 0 && (startLine.isMatchingList(line) || startLine.isChild(line)));

        return listStart == null ? line : listStart;
    }


    /**
     * This function will first walk up to the top of the current list level
     * and then walk down to the end of the current list level.
     * <p>
     * Sub-lists and other children will be skipped.
     */
    public static void renumberOrderedList(Editable text, int cursorPosition) {

        // Top of list
        final OrderedListLine firstLine = getOrderedListStart(text, cursorPosition);

        if (firstLine.isOrderedList && firstLine.lineEnd < text.length()) {
            int number = firstLine.value;

            int position = firstLine.lineEnd + 1;
            while (position >= 0 && position < text.length()) {

                final OrderedListLine line = new OrderedListLine(text, position);

                if (firstLine.isMatchingList(line)) {
                    number += 1;
                    if (line.value != number) {
                        String newNum = Integer.toString(number);
                        text.replace(line.numStart, line.numEnd, newNum);
                        final int lenDiff = newNum.length() - (line.numEnd - line.numStart);
                        position = line.lineEnd + lenDiff + 1;
                    } else {
                        position = line.lineEnd + 1;
                    }
                } else if (firstLine.isChild(line)) {
                    position = line.lineEnd + 1;
                } else {
                    break;
                }
            }
        }
    }
}
