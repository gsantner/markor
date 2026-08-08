/*#######################################################
 *
 *   Maintained 2025 by gsantner
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.opoc.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GsFileUtilsTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static GsFileUtils.SortOrder sortOrder(final String sortByType) {
        final GsFileUtils.SortOrder order = new GsFileUtils.SortOrder();
        order.sortByType = sortByType;
        order.folderFirst = false;
        return order;
    }

    private static void assertSortOrder(
            final GsFileUtils.SortOrder order,
            final List<File> input,
            final File... expected
    ) {
        final List<File> files = new ArrayList<>(input);
        GsFileUtils.sortFiles(files, order);
        assertEquals(Arrays.asList(expected), files);
    }

    private static void setFileLength(final File file, final long length) throws Exception {
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw")) {
            randomAccessFile.setLength(length);
        }
    }

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

    @Test
    public void sortFilesByModificationTimeKeepsNumericFilenameSeparate() throws Exception {
        final File olderNumericName = temporaryFolder.newFile("99.txt");
        final File newerAlphabeticName = temporaryFolder.newFile("a.txt");
        assertTrue(olderNumericName.setLastModified(1_700_000_000_000L));
        assertTrue(newerAlphabeticName.setLastModified(1_700_000_010_000L));
        assertTrue(olderNumericName.lastModified() < newerAlphabeticName.lastModified());

        assertSortOrder(
                sortOrder(GsFileUtils.SORT_BY_MTIME),
                Arrays.asList(newerAlphabeticName, olderNumericName),
                olderNumericName, newerAlphabeticName);
    }

    @Test
    public void sortFilesByModificationTimeFallsBackToNaturalFilenameOrder() throws Exception {
        final File fileTen = temporaryFolder.newFile("10.txt");
        final File fileTwo = temporaryFolder.newFile("2.txt");
        assertTrue(fileTen.setLastModified(1_700_000_000_000L));
        assertTrue(fileTwo.setLastModified(1_700_000_000_000L));
        assertEquals(fileTen.lastModified(), fileTwo.lastModified());

        assertSortOrder(
                sortOrder(GsFileUtils.SORT_BY_MTIME),
                Arrays.asList(fileTen, fileTwo),
                fileTwo, fileTen);
    }

    @Test
    public void sortFilesBySizeKeepsNumericFilenameSeparate() throws Exception {
        final File smallerNumericName = temporaryFolder.newFile("99.txt");
        final File largerAlphabeticName = temporaryFolder.newFile("a.txt");
        setFileLength(smallerNumericName, 1);
        setFileLength(largerAlphabeticName, 2);

        assertSortOrder(
                sortOrder(GsFileUtils.SORT_BY_FILESIZE),
                Arrays.asList(largerAlphabeticName, smallerNumericName),
                smallerNumericName, largerAlphabeticName);
    }

    @Test
    public void sortFilesByMimeTypeFallsBackToNaturalFilenameOrder() throws Exception {
        final File textTen = temporaryFolder.newFile("10.txt");
        final File image = temporaryFolder.newFile("z.jpg");
        final File textTwo = temporaryFolder.newFile("2.txt");

        assertSortOrder(
                sortOrder(GsFileUtils.SORT_BY_MIMETYPE),
                Arrays.asList(textTen, image, textTwo),
                image, textTwo, textTen);
    }

}
