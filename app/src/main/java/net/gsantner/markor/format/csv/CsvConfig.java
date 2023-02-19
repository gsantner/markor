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
    public static final CsvConfig DEFAULT = new CsvConfig(';','"');

    public static CsvConfig infer(String line) {
        char csvFieldDelimiterChar = findChar(line, DEFAULT.getFieldDelimiterChar(), ',', '\t', ':', '|');
        char csvQuoteChar = findChar(line, DEFAULT.getQuoteChar(), '\'');

        return new CsvConfig(csvFieldDelimiterChar, csvQuoteChar);
    }

    public CsvConfig(char fieldDelimiterChar, char quoteChar) {
        this.fieldDelimiterChar = fieldDelimiterChar;
        this.quoteChar = quoteChar;
    }

    public char getFieldDelimiterChar() {
        return fieldDelimiterChar;
    }

    public char getQuoteChar() {
        return quoteChar;
    }

    private static char findChar(String line, char... candidates) {
        for(char c : candidates) {
            if (line.contains("" + c)) {
                return c;
            }
        }
        return candidates[0];
    }

    private final char fieldDelimiterChar;
    private final char quoteChar;

}
