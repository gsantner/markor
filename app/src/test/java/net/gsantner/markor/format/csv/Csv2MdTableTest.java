/*#######################################################
 *
 *   Maintained 2023 by k3b
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format.csv;

import static org.junit.Assert.*;

import org.junit.Test;

public class Csv2MdTableTest {

    @Test
    public void toMdTable() {
        String expected = "col1|col2|col3\n" +
                ":---|:---|:---\n" +
                "1|2|3";
        String csv = "col1;col2;col3\n" +
                "1;2;3\n" +
                "1;;\n" +
                "1;\"multi\nline\";3\n" +
                "1;\"text with \"\" .,;:\";3\n";
        String markdown = Csv2MdTable.toMdTable(csv);
        assertEquals(expected, markdown.trim());
    }
}