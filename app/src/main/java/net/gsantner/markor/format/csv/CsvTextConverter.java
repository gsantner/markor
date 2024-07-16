/*#######################################################
 *
 *   Maintained 2023 by k3b
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.csv;

import static java.lang.Math.max;

import android.content.Context;

import androidx.annotation.NonNull;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.ICSVParser;

import net.gsantner.markor.format.TextConverterBase;
import net.gsantner.markor.format.markdown.MarkdownTextConverter;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

/**
 * Part of Markor-Architecture implementing Preview/Export for csv.
 * <p>
 * Converts csv to md and let
 * {@link  TextConverterBase#convertMarkup(String, Context, boolean, boolean, File)}
 * do the rest.
 * <p>
 * This way csv columns may contain md expressions like bold text.
 */
@SuppressWarnings("WeakerAccess")
public class CsvTextConverter extends MarkdownTextConverter {

    final List<String> EXT = Arrays.asList(".csv", ".tsv", ".tab", ".psv");

    @Override
    public String convertMarkup(String csvMarkup, Context context, boolean lightMode, boolean lineNum, File file) {
        String mdMarkup = Csv2MdTable.toMdTable(csvMarkup);
        return super.convertMarkup(mdMarkup, context, lightMode, lineNum, file);
    }

    @Override
    protected boolean isFileOutOfThisFormat(final File file, final String name, final String ext) {
        return EXT.contains(ext);
    }

    protected static class Csv2MdTable implements Closeable {
        public static final int BUFFER_SIZE = 8096;

        private static final String MD_LINE_DELIMITER = "\n";
        private static final String MD_COL_DELIMITER = "|";
        private static final String MD_HEADER_LINE_DELIMITER = MD_COL_DELIMITER + ":---";
        private final CSVReader m_csvReader;
        private int m_lineNumber = 0;

        private Csv2MdTable(CsvConfig csvConfig, Reader csvDataReader) {
            ICSVParser parser = new CSVParserBuilder()
                    .withSeparator(csvConfig.getFieldDelimiterChar())
                    .withQuoteChar(csvConfig.getQuoteChar())
                    .build();
            m_csvReader = new CSVReaderBuilder(csvDataReader)
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

                        for (String h : headers) {
                            mdMarkup.append(MD_HEADER_LINE_DELIMITER);
                        }
                        mdMarkup.append(MD_COL_DELIMITER).append(MD_LINE_DELIMITER);

                        String[] lineColumns;
                        while (null != (lineColumns = toMdTable.readNextCsvColumnLine())) {
                            addColumnsLine(mdMarkup, lineColumns, headers.length);
                        }
                    }
                }
            } catch (Exception e) {
                // invalid csv format in editor should not crash the app when trying to display as html
                // openCsv-3.10 may throw IOException or RuntimeExcpetion,
                // openCsv-5.7 may throw IOException or CsvValidationException.
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

        private static void addColumnsLine(StringBuilder mdMarkup, @NonNull String[] columns, int headerLength) {
            for (int i = 0; i < max(headerLength, columns.length); i++) {
                addColumnContainingNL(mdMarkup.append(MD_COL_DELIMITER), getCol(columns, i));
            }
            mdMarkup.append(MD_COL_DELIMITER).append(MD_LINE_DELIMITER);
        }

        @NonNull
        private static String getCol(@NonNull String[] columns, int i) {
            return (i >= 0 && i < columns.length) ? columns[i] : "";
        }

        private static void addColumnContainingNL(StringBuilder mdMarkup, String col) {
            // '|' is a reserved symbol and my not be content of a csv-column
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

        private String[] readNextCsvColumnLine() throws IOException {
            // openCsv-3.10 may throw IOException or RuntimeExcpetion,
            // openCsv-5.7 may throw IOException or CsvValidationException.
            String[] columns;
            do {
                m_lineNumber++;
                columns = m_csvReader.readNext();
            } while (columns != null && isComment(columns));
            return columns;
        }

        private boolean isComment(@NonNull String[] columns) {
            if (columns.length == 0) {
                return true;
            }

            // empty line without content
            if (columns.length == 1 && columns[0].trim().length() == 0) {
                return true;
            }

            // comments start with "#" char
            return columns[0].startsWith("#");
        }

        @Override
        public void close() throws IOException {
            m_csvReader.close();
        }

    }
}
