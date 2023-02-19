/*#######################################################
 *
 *   Maintained 2023 by k3b
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.csv;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.RFC4180Parser;
import com.opencsv.RFC4180ParserBuilder;
import com.opencsv.exceptions.CsvValidationException;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * Simple csv to md-table converter using OpenCsv implementation
 * https://opencsv.sourceforge.net/ Licensed under Appache2
 *
 * This file should not have dependencies to Android
 */
public class Csv2MdTable implements Closeable {
    public static final char CSV_FIELD_DELIMITER_CHAR = ';';
    public static final String MD_LINE_DELIMITER = "\n";
    public static final String MD_COL_DELIMITER = "|";
    public static final String MD_HEADER_LINE_DELIMITER = ":---";

    private int lineNumber = 0;
    private final CSVReader csvReader;

    private Csv2MdTable(Reader csvDataReader) {
        RFC4180Parser parser = new RFC4180ParserBuilder()
                .withSeparator(CSV_FIELD_DELIMITER_CHAR)
                .build();

        csvReader = new CSVReaderBuilder(csvDataReader)
                .withSkipLines(0)
                .withCSVParser(parser)
                .withKeepCarriageReturn(true)
                .build();
    }

    public static String toMdTable(String csvMarkup) {
        return toMdTable(new StringReader(csvMarkup));
    }

    public static String toMdTable(Reader csvMarkup) {
        StringBuilder mdMarkup = new StringBuilder();
        try (Csv2MdTable toMdTable = new Csv2MdTable(csvMarkup)) {
            String[] headers = toMdTable.readNextCsvColumnLine();

            if (headers != null && headers.length > 0) {
                mdMarkup.append(String.join(MD_COL_DELIMITER, headers)).append(MD_LINE_DELIMITER);
                mdMarkup.append(MD_HEADER_LINE_DELIMITER);
                for (int i = 1; i < headers.length; i++) {
                    mdMarkup.append(MD_COL_DELIMITER +
                            MD_HEADER_LINE_DELIMITER);
                }
                mdMarkup.append(MD_LINE_DELIMITER);

                String[] lineColumns;
                while (null != (lineColumns = toMdTable.readNextCsvColumnLine())) {
                    mdMarkup.append(String.join(MD_COL_DELIMITER, lineColumns)).append(MD_LINE_DELIMITER);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CsvValidationException e) {
            e.printStackTrace();
        }
        return mdMarkup.toString();
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
