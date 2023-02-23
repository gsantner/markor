/*#######################################################
 *
 *   Maintained 2023 by k3b
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.csv;

import static java.lang.Math.max;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.ICSVParser;
import com.opencsv.exceptions.CsvValidationException;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * Simple csv to md-table converter using OpenCsv implementation
 * https://opencsv.sourceforge.net/ Licensed under Appache2
 * <p>
 * This file should not have dependencies to Android or Markor-Architecture.
 */
public class Csv2MdTable implements Closeable {
    public static final int BUFFER_SIZE = 8096;

    private static final String MD_LINE_DELIMITER = "\n";
    private static final String MD_COL_DELIMITER = "|";
    private static final String MD_HEADER_LINE_DELIMITER = MD_COL_DELIMITER + ":---";
    private final CSVReader csvReader;
    private int lineNumber = 0;

    private Csv2MdTable(CsvConfig csvConfig, Reader csvDataReader) {
        ICSVParser parser = new CSVParserBuilder()
                .withSeparator(csvConfig.getFieldDelimiterChar())
                .withQuoteChar(csvConfig.getQuoteChar())
                .build();
        csvReader = new CSVReaderBuilder(csvDataReader)
                .withSkipLines(0)
                .withCSVParser(parser)
                .withKeepCarriageReturn(true)
                .build();
    }

    public static String toMdTable(String csvMarkup) {
        // parser cannot handle empty lines if they are not "\r\n"
        return toMdTable(new StringReader(csvMarkup.replace("\n", "\r\n")));
    }

    public static String toMdTable(Reader csvMarkup) {
        StringBuilder mdMarkup = new StringBuilder();
        try (BufferedReader bufferedReader = new BufferedReader(csvMarkup, BUFFER_SIZE)) {
            CsvConfig csvConfig = inferCsvConfiguration(bufferedReader);
            try (Csv2MdTable toMdTable = new Csv2MdTable(csvConfig, bufferedReader)) {
                String[] headers = toMdTable.readNextCsvColumnLine();

                if (headers != null && headers.length > 0) {
                    addColumnsLine(mdMarkup, headers, headers.length);

                    for (int i = 0; i < headers.length; i++) {
                        mdMarkup.append(MD_HEADER_LINE_DELIMITER);
                    }
                    mdMarkup.append(MD_COL_DELIMITER).append(MD_LINE_DELIMITER);

                    String[] lineColumns;
                    while (null != (lineColumns = toMdTable.readNextCsvColumnLine())) {
                        addColumnsLine(mdMarkup, lineColumns, headers.length);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CsvValidationException e) {
            e.printStackTrace();
        }
        return mdMarkup.toString();
    }

    private static CsvConfig inferCsvConfiguration(BufferedReader bufferedReader) throws IOException {
        // remember where we started.
        bufferedReader.mark(BUFFER_SIZE);
        try {
            String line;
            while (null != (line = bufferedReader.readLine())) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    return CsvConfig.infer(line);
                }
            }
            return CsvConfig.DEFAULT;
        } finally {
            // go back to start of csv
            bufferedReader.reset();
        }
    }

    private static void addColumnsLine(StringBuilder mdMarkup, String[] colums, int headerLength) {
        for (int i = 0; i < max(headerLength, colums.length); i++) {
            addColumnContainingNL(mdMarkup.append(MD_COL_DELIMITER), getCol(colums, i));
        }
        mdMarkup
                .append(MD_COL_DELIMITER)
                .append(MD_LINE_DELIMITER);
    }

    private static String getCol(String[] colums, int i) {
        if (i >= 0 && i < colums.length) return colums[i];
        return "";
    }

    private static void addColumnContainingNL(StringBuilder mdMarkup, String col) {
        // '|' is a reseved symbol and my not be content of a csv-column
        col = col.replace('|', '!');

        String[] lines = col.split("\r?\n");
        if (lines.length > 1) {
            addColumn(mdMarkup, lines[0]);
            for (int i = 1; i < lines.length; i++) {
                addColumn(mdMarkup.append("<br/>"), lines[i]);
            }
        } else {
            addColumn(mdMarkup, col);
        }

    }

    private static void addColumn(StringBuilder mdMarkup, String col) {
        if (col != null) col = col.trim();
        if (col == null || col.isEmpty()) col = "&nbsp;";
        mdMarkup.append(col);
    }

    private String[] readNextCsvColumnLine() throws IOException, CsvValidationException {
        String[] columns;
        do {
            lineNumber++;
            columns = csvReader.readNext();
        } while (columns != null && isComment(columns));
        return columns;
    }

    private boolean isComment(String[] columns) {
        // empty line without content
        if (columns.length == 1 && columns[0].trim().length() == 0) return true;

        // comments start with "#" char
        return columns[0].startsWith("#");
    }

    @Override
    public void close() throws IOException {
        csvReader.close();
    }
}
