package net.gsantner.markor.format.zimwiki;

import net.gsantner.opoc.util.FileUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class ZimWikiLinkResolverTests {
    private Path tempFolder;
    private Path notebookRoot;


    @Before
    public void before() {
        try {
            tempFolder = Files.createTempDirectory("markorTemp");
            notebookRoot = Files.createDirectory(tempFolder.resolve("notebookRoot"));

            System.out.println("Created: "+ notebookRoot);

            createTestNotebookStructure();

        } catch (IOException e) {
            e.printStackTrace();
            fail("Could not create the test directory");
        }
    }

    /**
     * Creates the following notebook structure:
     *
     * notebookRoot ___ My page ___ Another page ___ Very cool subpage
     *   |                   |___ Yet another page ___ Interesting page
     *   |                                      |___ Strange page
     *   |___ Your page ___ Another page
     *                |___ The coolest page
     */
    private void createTestNotebookStructure() throws IOException {

        Files.createFile(notebookRoot.resolve("notebook.zim"));

        Files.createDirectories(notebookRoot.resolve("My_page/Another_page"));
        Files.createFile(notebookRoot.resolve("My_page.txt"));
        Files.createFile(notebookRoot.resolve("My_page/Another_page.txt"));
        Files.createFile(notebookRoot.resolve("My_page/Another_page/Very_cool_subpage.txt"));

        Files.createDirectories(notebookRoot.resolve("My_page/Yet_another_page"));
        Files.createFile(notebookRoot.resolve("My_page/Yet_another_page.txt"));
        Files.createFile(notebookRoot.resolve("My_page/Yet_another_page/Interesting_page.txt"));
        Files.createFile(notebookRoot.resolve("My_page/Yet_another_page/Strange_page.txt"));

        Files.createDirectories(notebookRoot.resolve("Your_page"));
        Files.createFile(notebookRoot.resolve("Your_page.txt"));
        Files.createFile(notebookRoot.resolve("Your_page/Another_page.txt"));
        Files.createFile(notebookRoot.resolve("Your_page/The_coolest_page.txt"));
    }

    @After
    public void after() {
        FileUtils.deleteRecursive(tempFolder.toFile());
        System.out.println("Deleted.");
    }

    @Test
    public void resolvesSubPageLinkDirectSubpage() {
        ZimWikiLinkResolver resolver = ZimWikiLinkResolver.resolve("[[+Another page]]", notebookRoot.toFile(), notebookRoot.resolve("My_page.txt").toFile());
        String expectedLink = notebookRoot.resolve("My_page/Another_page.txt").toString();
        assertEquals(expectedLink, resolver.getResolvedLink());
        assertNull(resolver.getLinkDescription());
    }

    @Test
    public void resolvesSubPageLinkSubpageWithDescription() {
        ZimWikiLinkResolver resolver = ZimWikiLinkResolver.resolve("[[+Yet another page:Strange page|This link leads to a strange page]]", notebookRoot.toFile(), notebookRoot.resolve("My_page.txt").toFile());
        String expectedLink = notebookRoot.resolve("My_page/Yet_another_page/Strange_page.txt").toString();
        assertEquals(expectedLink, resolver.getResolvedLink());
        assertEquals("This link leads to a strange page", resolver.getLinkDescription());
    }

    @Test
    public void resolvesTopLevelLink() {
        ZimWikiLinkResolver resolver = ZimWikiLinkResolver.resolve("[[:Your page:The coolest page]]", notebookRoot.toFile(), notebookRoot.resolve("My_page/Yet_another_page.txt").toFile());
        String expectedLink = notebookRoot.resolve("Your_page/The_coolest_page.txt").toString();
        assertEquals(expectedLink, resolver.getResolvedLink());
        assertNull(resolver.getLinkDescription());
    }

    @Test
    public void resolvesTopLevelLinkWithDescription() {
        ZimWikiLinkResolver resolver = ZimWikiLinkResolver.resolve("[[:My page:Yet another page:Interesting page|Some description]]", notebookRoot.toFile(), notebookRoot.resolve("My_page.txt").toFile());
        String expectedLink = notebookRoot.resolve("My_page/Yet_another_page/Interesting_page.txt").toString();
        assertEquals(expectedLink, resolver.getResolvedLink());
        assertEquals("Some description", resolver.getLinkDescription());
    }

    // TODO: links which are resolved within root to current page

    @Test
    public void resolvesWebLinkWithDescription() {
        ZimWikiLinkResolver resolver = ZimWikiLinkResolver.resolve("[[http://www.example.com|Example website]]", notebookRoot.toFile(), notebookRoot.resolve("My_page.txt").toFile());
        assertEquals("http://www.example.com", resolver.getResolvedLink());
        assertEquals("Example website", resolver.getLinkDescription());
    }

    // TODO: handle whitespaces before or after path/description?
}
