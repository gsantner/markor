/*#######################################################
 *
 *   Maintained 2023 by k3b
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.csv;

import net.gsantner.opoc.format.GsTextUtils;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Implementation detail for {@link CsvSyntaxHighlighter} that has no dependencies to
 * android and to Markor-Architecture.
 */
public class CsvMatcher {
    public static final int INDEX_NOT_FOUND = StringUtils.INDEX_NOT_FOUND;
    private final String csv;

    private char csvDelimiter; // i.e. -;-
    private char csvQoute; // i.e. -"-

    private int startOfCol;
    private boolean endOfRow = true;

    public CsvMatcher(CharSequence csv) {
        this.csv = csv.toString();
        inferCsvConfig();
    }

    /**
     * Modified version of org.apache.commons.lang3.StringUtils#indexOfAny.
     * <p>
     * Same as {@link StringUtils#indexOfAny(CharSequence, char...)
     * where you can specify the search intervall}
     * License of this function is Apache2
     */
        public static int indexOfAny(final CharSequence cs, int csFirst, int csLen, final char... searchChars) {
        if (StringUtils.isEmpty(cs) || ArrayUtils.isEmpty(searchChars)) {
            return StringUtils.INDEX_NOT_FOUND;
        }
        final int csLast = csLen - 1;
        final int searchLen = searchChars.length;
        final int searchLast = searchLen - 1;
        for (int i = csFirst; i < csLen; i++) {
            final char ch = cs.charAt(i);
            for (int j = 0; j < searchLen; j++) {
                if (searchChars[j] == ch) {
                    if (i < csLast && j < searchLast && Character.isHighSurrogate(ch)) {
                        // ch is a supplementary character
                        if (searchChars[j + 1] == cs.charAt(i + 1)) {
                            return i;
                        }
                    } else {
                        return i;
                    }
                }
            }
        }
        return StringUtils.INDEX_NOT_FOUND;
    }

    private void inferCsvConfig() {
        int posDelimiter = indexOfAny(csv, 0, csv.length(), CsvConfig.CSV_DELIMITER_CANDIDATES);
        if (posDelimiter >= 0) {
            csvDelimiter = csv.charAt(posDelimiter);
            this.startOfCol = GsTextUtils.beginOfLine(csv, posDelimiter);

            int posEndOfHeader = GsTextUtils.endOfLine(csv, posDelimiter);

            int posQuote = indexOfAny(csv, startOfCol, posEndOfHeader, CsvConfig.CSV_QUOTE_CANDIDATES);
            this.csvQoute = posQuote >= 0 ? csv.charAt(posQuote) : CsvConfig.CSV_QUOTE_CANDIDATES[0];
        }
    }

    public int nextDelimiterPos(final int startOfColumnContent) {
        int csvLen = csv.length();

        int nextDelimiter = StringUtils.indexOf(csv, csvDelimiter, startOfColumnContent);
        int nextBeginQuote = StringUtils.indexOf(csv, csvQoute, startOfColumnContent);

        if (this.endOfRow) {
            int nextComment = StringUtils.indexOf(csv, '#', startOfColumnContent);
            if (nextComment > INDEX_NOT_FOUND && nextComment < nextDelimiter && nextComment < nextBeginQuote) {
                return GsTextUtils.endOfLine(csv, nextComment); // return comment as uninterpreted comment until end of line
            }
        }

        this.endOfRow = true;

        int nextNl = GsTextUtils.endOfLine(csv, startOfColumnContent);
        if (nextBeginQuote > INDEX_NOT_FOUND && nextBeginQuote < nextDelimiter && nextBeginQuote < nextNl) {
            // column surrounded by qoutes
            int nextEndQuote = nextEndQuote(nextBeginQuote + 1);
            if (nextEndQuote == INDEX_NOT_FOUND) return csvLen;

            nextDelimiter = StringUtils.indexOf(csv, csvDelimiter, nextEndQuote);
            nextNl = GsTextUtils.endOfLine(csv, nextEndQuote);
        }

        if (nextNl > INDEX_NOT_FOUND && nextNl < nextDelimiter) {
            // nl comes before next delimiter
            // this.endOfRow = true;
            return nextNl;
        }
        if (nextDelimiter > INDEX_NOT_FOUND) {
            // more columns exist in row
            this.endOfRow = false;
            return nextDelimiter;
        }

        return csvLen;
    }

    // skip double quotes -""-
    private int nextEndQuote(int start) {
        int csvLen = csv.length();
        int found;
        while (start < csvLen) {
            found = StringUtils.indexOf(csv, csvQoute, start);

            if (found == INDEX_NOT_FOUND) return INDEX_NOT_FOUND;
            if (found + 1 < csvLen && csv.charAt(found + 1) != csvQoute) return found;

            // found double quote -""- : ignore
            start = found + 2;
        }

        return INDEX_NOT_FOUND;
    }

    public int getStartOfCol() {
        return startOfCol;
    }

    public boolean isEndOfRow() {
        return endOfRow;
    }
}
