package net.gsantner.markor.format.csv;

import net.gsantner.markor.util.StringUtils;

public class CsvMatcher {
    public static final int INDEX_NOT_FOUND = StringUtils.INDEX_NOT_FOUND;
    private final CharSequence csv;

    private char csvDelimiter; // i.e. -;-
    private char csvQoute; // i.e. -"-

    private int startOfCol;
    private boolean endOfRow = true;

    public CsvMatcher(CharSequence csv) {
        this.csv = csv;
        inferCsvConfig();
    }

    private void inferCsvConfig() {
        int posDelimiter = StringUtils.indexOfAny(csv, 0, csv.length(), CsvConfig.CSV_DELIMITER_CANDIDATES);
        if (posDelimiter >= 0) {
            csvDelimiter = csv.charAt(posDelimiter);
            this.startOfCol = StringUtils.beginOfLine(csv, posDelimiter);

            int posEndOfHeader = StringUtils.endOfLine(csv, posDelimiter);

            int posQuote = StringUtils.indexOfAny(csv, startOfCol, posEndOfHeader, CsvConfig.CSV_QUOTE_CANDIDATES);
            this.csvQoute = posQuote >= 0 ? csv.charAt(posQuote) : CsvConfig.CSV_QUOTE_CANDIDATES[0];
        }
    }

    public int nextDelimiterPos(final int startOfColumnContent) {
        this.endOfRow = true;
        int csvLen = csv.length();

        int nextDelimiter = StringUtils.indexOf(csv, csvDelimiter, startOfColumnContent);
        int nextBeginQuote = StringUtils.indexOf(csv, csvQoute, startOfColumnContent);
        int pos = startOfColumnContent;
        if (nextBeginQuote > INDEX_NOT_FOUND && nextBeginQuote < nextDelimiter) {
            // column surrounded by qoutes
            int nextEndQuote = nextEndQuote(nextBeginQuote + 1);
            if (nextEndQuote == INDEX_NOT_FOUND) return csvLen;

            nextDelimiter = StringUtils.indexOf(csv, csvDelimiter, nextEndQuote);
            pos = nextEndQuote;
        }
        int nextNl = StringUtils.endOfLine(csv, pos);

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
            found = StringUtils.indexOf(csv, start,csvQoute);

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
