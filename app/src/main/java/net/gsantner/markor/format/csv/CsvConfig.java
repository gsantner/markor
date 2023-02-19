/*#######################################################
 *
 *   Maintained 2023 by k3b
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.csv;

/**
 * Configuration for CSV file format
 */
public class CsvConfig {
    public static final CsvConfig DEFAULT = new CsvConfig(';', '"');
    private final char fieldDelimiterChar;
    private final char quoteChar;

    public CsvConfig(char fieldDelimiterChar, char quoteChar) {
        this.fieldDelimiterChar = fieldDelimiterChar;
        this.quoteChar = quoteChar;
    }

    public static CsvConfig infer(String line) {
        char csvFieldDelimiterChar = findChar(line, DEFAULT.getFieldDelimiterChar(), ',', '\t', ':', '|');
        char csvQuoteChar = findChar(line, DEFAULT.getQuoteChar(), '\'');

        return new CsvConfig(csvFieldDelimiterChar, csvQuoteChar);
    }

    private static char findChar(String line, char... candidates) {
        for (char c : candidates) {
            if (line.contains("" + c)) {
                return c;
            }
        }
        return candidates[0];
    }

    public char getFieldDelimiterChar() {
        return fieldDelimiterChar;
    }

    public char getQuoteChar() {
        return quoteChar;
    }

}
