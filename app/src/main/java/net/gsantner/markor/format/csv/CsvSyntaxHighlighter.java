/*#######################################################
 *
 *   Maintained 2023 by k3b
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.csv;

import android.graphics.Color;
import android.util.Log;

import net.gsantner.markor.format.markdown.MarkdownSyntaxHighlighter;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.opoc.format.GsTextUtils;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Part of Markor-Architecture implementing SyntaxHighlighting for csv.
 */
public class CsvSyntaxHighlighter extends MarkdownSyntaxHighlighter {
    // standard green, yellow, cyan is not readable on white background
    // dkgray is not much different from black and not readable with black background
    // blue is difficuilt to read on black background
    private static final int[] COLUMN_COLORS = {
            Color.RED, Color.BLUE, Color.MAGENTA,
            0xff00b04c, // dark green,
            0xffdaa500}; // brown
    private static final String TAG = CsvSyntaxHighlighter.class.getSimpleName();
    private static boolean DEBUG_COLORING = false;

    private static final int INDEX_NOT_FOUND = StringUtils.INDEX_NOT_FOUND;

    private char m_csvDelimiter; // i.e. -;-
    private char m_csvQoute; // i.e. -"-

    private int m_startOfCol;
    private boolean m_isEndOfRow = true;

    public CsvSyntaxHighlighter(AppSettings as) {
        super(as);
    }

    @Override
    protected void generateSpans() {
        super.generateSpans();

        inferCsvConfig();

        createSpanForColumns(COLUMN_COLORS);
    }

    private final void createSpanForColumns(int[] colors) {
        int colNumner = -1;
        int from = m_startOfCol;
        int to;
        while (from < _spannable.length()) {
            if (m_isEndOfRow) colNumner = -1; // -1 == skip coloring
            to = nextDelimiterPos(from);

            createSpanForColumn(from, to, colNumner >= 0 ? colors[colNumner] : Color.BLACK, colNumner);
            colNumner++;
            if (colNumner >= colors.length) {
                colNumner = 0;
            }
            from = to + 1;
        }
    }

    private void createSpanForColumn(int from, int to, int color, int colNumber) {
        if (DEBUG_COLORING) {
            Log.d(TAG, String.format("#%d(%d,%d,%d) = %s", colNumber, from, to, color, _spannable.subSequence(from, to)));
        }
        if (colNumber >= 0 && from > 0 && Math.abs(to - from) >= 0) {
            HighlightSpan span = new HighlightSpan().setForeColor(color);

            if (span != null) {
                addSpanGroup(span, from - 1, to); // -1 : also mark delimiter
            }
        }
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
        int posDelimiter = indexOfAny(_spannable, 0, _spannable.length(), CsvConfig.CSV_DELIMITER_CANDIDATES);
        if (posDelimiter >= 0) {
            m_csvDelimiter = _spannable.charAt(posDelimiter);
            m_startOfCol = GsTextUtils.beginOfLine(_spannable.toString(), posDelimiter);

            int posEndOfHeader = GsTextUtils.endOfLine(_spannable.toString(), posDelimiter);

            int posQuote = indexOfAny(_spannable, m_startOfCol, posEndOfHeader, CsvConfig.CSV_QUOTE_CANDIDATES);
            m_csvQoute = posQuote >= 0 ? _spannable.charAt(posQuote) : CsvConfig.CSV_QUOTE_CANDIDATES[0];
        }
    }

    private int nextDelimiterPos(final int startOfColumnContent) {
        int csvLen = _spannable.length();

        int nextDelimiter = StringUtils.indexOf(_spannable, m_csvDelimiter, startOfColumnContent);
        int nextBeginQuote = StringUtils.indexOf(_spannable, m_csvQoute, startOfColumnContent);

        if (m_isEndOfRow) {
            int nextComment = StringUtils.indexOf(_spannable, '#', startOfColumnContent);
            if (nextComment > INDEX_NOT_FOUND && nextComment < nextDelimiter && nextComment < nextBeginQuote) {
                return GsTextUtils.endOfLine(_spannable.toString(), nextComment); // return comment as uninterpreted comment until end of line
            }
        }

        m_isEndOfRow = true;

        int nextNl = GsTextUtils.endOfLine(_spannable.toString(), startOfColumnContent);
        if (nextBeginQuote > INDEX_NOT_FOUND && nextBeginQuote < nextDelimiter && nextBeginQuote < nextNl) {
            // column surrounded by qoutes
            int nextEndQuote = nextEndQuote(nextBeginQuote + 1);
            if (nextEndQuote == INDEX_NOT_FOUND) {
                return csvLen;
            }

            nextDelimiter = StringUtils.indexOf(_spannable, m_csvDelimiter, nextEndQuote);
            nextNl = GsTextUtils.endOfLine(_spannable.toString(), nextEndQuote);
        }

        if (nextNl > INDEX_NOT_FOUND && nextNl < nextDelimiter) {
            // nl comes before next delimiter
            // m_endOfRow = true;
            return nextNl;
        }
        if (nextDelimiter > INDEX_NOT_FOUND) {
            // more columns exist in row
            m_isEndOfRow = false;
            return nextDelimiter;
        }

        return csvLen;
    }

    // skip double quotes -""-
    private int nextEndQuote(int start) {
        int csvLen = _spannable.length();
        int found;
        while (start < csvLen) {
            found = StringUtils.indexOf(_spannable, m_csvQoute, start);

            if (found == INDEX_NOT_FOUND) {
                return INDEX_NOT_FOUND;
            }
            if (found + 1 < csvLen && _spannable.charAt(found + 1) != m_csvQoute) {
                return found;
            }

            // found double quote -""- : ignore
            start = found + 2;
        }

        return INDEX_NOT_FOUND;
    }

}
