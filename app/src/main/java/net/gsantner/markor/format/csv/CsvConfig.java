/*#######################################################
 *
 *   Maintained 2023 by k3b
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.csv;

/**
 * Configuration for CSV file format.
 * <p>
 * Implementation detail for csv support. This file should be not have dependencies to
 * android and to Markor-Architecture.
 */
public class CsvConfig {
    public static final CsvConfig DEFAULT = new CsvConfig(',', '"');
    public static final char[] CSV_DELIMITER_CANDIDATES = {DEFAULT.getFieldDelimiterChar(), ';', '\t', ':', '|'};
    public static final char[] CSV_QUOTE_CANDIDATES = {DEFAULT.getQuoteChar(), '\''};
    private final char m_fieldDelimiterChar;
    private final char m_quoteChar;

    public CsvConfig(char fieldDelimiterChar, char quoteChar) {
        m_fieldDelimiterChar = fieldDelimiterChar;
        m_quoteChar = quoteChar;
    }

    public static CsvConfig infer(String line) {
        char csvFieldDelimiterChar = findChar(line, CSV_DELIMITER_CANDIDATES);
        char csvQuoteChar = findChar(line, CSV_QUOTE_CANDIDATES);

        return new CsvConfig(csvFieldDelimiterChar, csvQuoteChar);
    }

    private static char findChar(String line, char... candidates) {
        int pos = CsvSyntaxHighlighter.indexOfAny(line, 0, line.length(), candidates);
        return pos == -1 ? candidates[0] : line.charAt(pos);
    }

    public char getFieldDelimiterChar() {
        return m_fieldDelimiterChar;
    }

    public char getQuoteChar() {
        return m_quoteChar;
    }

}
