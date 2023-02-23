/*#######################################################
 *
 *   Maintained 2023 by k3b
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.csv;

import net.gsantner.opoc.format.GsTextUtils;

/**
 * Configuration for CSV file format.
 *
 * Implementation detail for csv support. This file should be not have dependencies to
 * android and to Markor-Architecture.
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
        char csvFieldDelimiterChar = findChar(line, CSV_DELIMITER_CANDIDATES);
        char csvQuoteChar = findChar(line, CSV_QUOTE_CANDIDATES);

        return new CsvConfig(csvFieldDelimiterChar, csvQuoteChar);
    }

    private static char findChar(String line, char... candidates) {
        int pos = GsTextUtils.indexOfAny(line,0, line.length(), candidates);
        return pos == -1 ? candidates[0] : line.charAt(pos);
    }

    public char getFieldDelimiterChar() {
        return fieldDelimiterChar;
    }

    public char getQuoteChar() {
        return quoteChar;
    }

}
