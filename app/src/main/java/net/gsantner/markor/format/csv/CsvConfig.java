/*#######################################################
 *
 *   Maintained 2023 by k3b
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.csv;

import net.gsantner.markor.util.StringUtils;

/**
 * Configuration for CSV file format
 */
public class CsvConfig {
    public static final CsvConfig DEFAULT = new CsvConfig(';', '"');
    public static final char[] CSV_DELIMITER_CANDIDATES = {DEFAULT.getFieldDelimiterChar(), ',', '\t', ':', '|'};
    public static final char[] CSV_QUOTE_CANDIDATES = {DEFAULT.getQuoteChar(), '\''};
    private final char fieldDelimiterChar;
    private final char quoteChar;

    public CsvConfig(char fieldDelimiterChar, char quoteChar) {
        this.fieldDelimiterChar = fieldDelimiterChar;
        this.quoteChar = quoteChar;
    }

    public static CsvConfig infer(String line) {
        char csvFieldDelimiterChar = findChar(line,0, CSV_DELIMITER_CANDIDATES);
        char csvQuoteChar = findChar(line,0, CSV_QUOTE_CANDIDATES);

        return new CsvConfig(csvFieldDelimiterChar, csvQuoteChar);
    }

    private static char findChar(String line, int fromIndex, char... candidates) {
        int pos = StringUtils.indexOfAny(line,fromIndex, line.length(), candidates);
        return pos == -1 ? candidates[0] : line.charAt(pos);
    }

    public char getFieldDelimiterChar() {
        return fieldDelimiterChar;
    }

    public char getQuoteChar() {
        return quoteChar;
    }

}
