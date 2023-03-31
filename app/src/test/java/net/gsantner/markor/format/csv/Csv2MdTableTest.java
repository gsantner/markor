/*#######################################################
 *
 *   Maintained 2023 by k3b
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.csv;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class Csv2MdTableTest {

    @Test
    public void toMdTable() {
        String expected = "|col1|col2|col3|\n" +
                "|:---|:---|:---|\n" +
                "|1|2|3|\n" +
                "|1|&nbsp;|&nbsp;|\n" +
                "|1|multi<br/>line|3|\n" +
                "|1|text with \" .,;!!\t:|3|";
        String csv =
                // empty lines are ignored/removed
                "\n\n\n\n" +

                        "# comment lines are ignored/removed\n" +

                        "col1;col2;col3\n" +
                        "1;2;3\n" +

                        // missing colums will be completed
                        "1\n" +

                        // empty lines are ignored/removed
                        "\n\n" +

                        // newline in column will be replaced by html <br/>
                        "1;\"multi\n" +
                        "    line\";3\n" +

                        //  handling special chars
                        "1;\"text with \"\" .,;!|\t:\";3\n";
        String markdown = CsvTextConverter.Csv2MdTable.toMdTable(csv);
        assertEquals(expected, markdown.trim());
    }

    @Test
    public void toMdTableInferSeperator() {
        String expected = "|col1|col2|col3|\n" +
                "|:---|:---|:---|\n" +
                "|1|2|3|";

        // use "," instead of ";" as seperator
        // use -'- instead of -"- as quote char
        String csv = "'col1',col2,col3\n" +
                "1,'2',3\n";
        String markdown = CsvTextConverter.Csv2MdTable.toMdTable(csv);
        assertEquals(expected, markdown.trim());
    }
}