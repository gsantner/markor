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
    private final String m_csv;

    private char m_csvDelimiter; // i.e. -;-
    private char m_csvQoute; // i.e. -"-

    private int m_startOfCol;
    private boolean m_endOfRow = true;

    public CsvMatcher(CharSequence csv) {
        m_csv = csv.toString();
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
        int posDelimiter = indexOfAny(m_csv, 0, m_csv.length(), CsvConfig.CSV_DELIMITER_CANDIDATES);
        if (posDelimiter >= 0) {
            m_csvDelimiter = m_csv.charAt(posDelimiter);
            m_startOfCol = GsTextUtils.beginOfLine(m_csv, posDelimiter);

            int posEndOfHeader = GsTextUtils.endOfLine(m_csv, posDelimiter);

            int posQuote = indexOfAny(m_csv, m_startOfCol, posEndOfHeader, CsvConfig.CSV_QUOTE_CANDIDATES);
            m_csvQoute = posQuote >= 0 ? m_csv.charAt(posQuote) : CsvConfig.CSV_QUOTE_CANDIDATES[0];
        }
    }

    public int nextDelimiterPos(final int startOfColumnContent) {
        int csvLen = m_csv.length();

        int nextDelimiter = StringUtils.indexOf(m_csv, m_csvDelimiter, startOfColumnContent);
        int nextBeginQuote = StringUtils.indexOf(m_csv, m_csvQoute, startOfColumnContent);

        if (m_endOfRow) {
            int nextComment = StringUtils.indexOf(m_csv, '#', startOfColumnContent);
            if (nextComment > INDEX_NOT_FOUND && nextComment < nextDelimiter && nextComment < nextBeginQuote) {
                return GsTextUtils.endOfLine(m_csv, nextComment); // return comment as uninterpreted comment until end of line
            }
        }

        m_endOfRow = true;

        int nextNl = GsTextUtils.endOfLine(m_csv, startOfColumnContent);
        if (nextBeginQuote > INDEX_NOT_FOUND && nextBeginQuote < nextDelimiter && nextBeginQuote < nextNl) {
            // column surrounded by qoutes
            int nextEndQuote = nextEndQuote(nextBeginQuote + 1);
            if (nextEndQuote == INDEX_NOT_FOUND) return csvLen;

            nextDelimiter = StringUtils.indexOf(m_csv, m_csvDelimiter, nextEndQuote);
            nextNl = GsTextUtils.endOfLine(m_csv, nextEndQuote);
        }

        if (nextNl > INDEX_NOT_FOUND && nextNl < nextDelimiter) {
            // nl comes before next delimiter
            // m_endOfRow = true;
            return nextNl;
        }
        if (nextDelimiter > INDEX_NOT_FOUND) {
            // more columns exist in row
            m_endOfRow = false;
            return nextDelimiter;
        }

        return csvLen;
    }

    // skip double quotes -""-
    private int nextEndQuote(int start) {
        int csvLen = m_csv.length();
        int found;
        while (start < csvLen) {
            found = StringUtils.indexOf(m_csv, m_csvQoute, start);

            if (found == INDEX_NOT_FOUND) return INDEX_NOT_FOUND;
            if (found + 1 < csvLen && m_csv.charAt(found + 1) != m_csvQoute) return found;

            // found double quote -""- : ignore
            start = found + 2;
        }

        return INDEX_NOT_FOUND;
    }

    public int getStartOfCol() {
        return m_startOfCol;
    }

    public boolean isEndOfRow() {
        return m_endOfRow;
    }
}
