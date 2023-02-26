/*#######################################################
 *
 *   Maintained 2023 by k3b
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.opoc.format;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

public class GsTextUtilsTest {
    //                      012345 67890 1234567
    String lineWithNL = "Hello \njava\n World";

    //                      0123456789012345
    String lineWithoutNL = "Hello java World";

    @Test
    public void getNeighbourLineEndings_bothFound() {
        int pos = lineWithNL.indexOf("av"); // inside java
        int[] result = GsTextUtils.getNeighbourLineEndings(lineWithNL, pos, pos);
        assertArrayEquals("'java' found", new int[]{6, 11}, result);
    }

    @Test
    public void getNeighbourLineEndings_noEnd() {
        int pos = lineWithNL.indexOf("or"); // inside world
        int[] result = GsTextUtils.getNeighbourLineEndings(lineWithNL, pos, pos);
        assertArrayEquals("'world' found", new int[]{11, 18}, result);
    }

    @Test
    public void getNeighbourLineEndings_noBegin() {
        int pos = lineWithNL.indexOf("el"); // inside Hello
        int[] result = GsTextUtils.getNeighbourLineEndings(lineWithNL, pos, pos);
        assertArrayEquals("'hello' found", new int[]{0, 6}, result);
    }

    @Test
    public void getNeighbourLineEndings_noNl() {
        int pos = lineWithNL.indexOf("av"); // inside java
        int[] result = GsTextUtils.getNeighbourLineEndings(lineWithoutNL, pos, pos);
        assertArrayEquals("whole text returned", new int[]{0, 16}, result);
    }

}