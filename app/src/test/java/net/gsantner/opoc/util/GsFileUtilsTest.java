/*#######################################################
 *
 *   Maintained 2025 by gsantner
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.opoc.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GsFileUtilsTest {
    @Test
    public void getFilenameExtension() {
        assertEquals("", GsFileUtils.getFilenameExtension(""));
        assertEquals("", GsFileUtils.getFilenameExtension("index"));
        assertEquals(".md", GsFileUtils.getFilenameExtension("hello.md"));
        assertEquals(".md", GsFileUtils.getFilenameExtension("hello.md.jenc"));
        assertEquals(".html", GsFileUtils.getFilenameExtension("hello.html"));
        assertEquals(".html", GsFileUtils.getFilenameExtension("my.cool.website.html"));
    }

    @Test
    public void getNameWithoutExtension() {
        assertEquals("", GsFileUtils.getNameWithoutExtension(""));
        assertEquals("index", GsFileUtils.getNameWithoutExtension("index"));
        assertEquals("hello", GsFileUtils.getNameWithoutExtension("hello.md.jenc"));
        assertEquals("hello", GsFileUtils.getNameWithoutExtension("hello.md"));
        assertEquals("hello", GsFileUtils.getNameWithoutExtension("hello.html"));
        assertEquals("my.cool.website", GsFileUtils.getNameWithoutExtension("my.cool.website.html"));
    }
}