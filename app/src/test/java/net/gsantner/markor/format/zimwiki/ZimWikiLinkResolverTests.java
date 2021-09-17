package net.gsantner.markor.format.zimwiki;

import net.gsantner.opoc.util.FileUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
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
        assertResolvedLinkAndDescription("My_page/Another_page.txt", null,
                "[[+Another page]]", "My_page.txt");
    }

    @Test
    public void resolvesSubPageLinkSubpageWithDescription() {
        assertResolvedLinkAndDescription("My_page/Yet_another_page/Strange_page.txt", "This link leads to a strange page",
                "[[+Yet another page:Strange page|This link leads to a strange page]]", "My_page.txt");
    }

    @Test
    public void resolvesTopLevelLink() {
        assertResolvedLinkAndDescription("Your_page/The_coolest_page.txt", null,
                "[[:Your page:The coolest page]]", "My_page/Yet_another_page.txt");
    }

    @Test
    public void resolvesTopLevelLinkWithDescription() {
        assertResolvedLinkAndDescription("My_page/Yet_another_page/Interesting_page.txt", "Some description",
                "[[:My page:Yet another page:Interesting page|Some description]]", "My_page.txt");
    }

    // TODO: links which are resolved within root to current page

    @Test
    public void resolvesWebLinkWithDescription() {
        ZimWikiLinkResolver resolver = ZimWikiLinkResolver.resolve("[[http://www.example.com|Example website]]", notebookRoot.toFile(), notebookRoot.resolve("My_page.txt").toFile());
        assertEquals("http://www.example.com", resolver.getResolvedLink());
        assertEquals("Example website", resolver.getLinkDescription());
    }

    private void assertResolvedLinkAndDescription(String expectedLinkRelativeToRoot, String expectedDescription, String zimLink, String currentPageRelativeToRoot) {
        ZimWikiLinkResolver resolver = ZimWikiLinkResolver.resolve(zimLink, notebookRoot.toFile(), notebookRoot.resolve(currentPageRelativeToRoot).toFile());
        String expectedLink = expectedLinkRelativeToRoot!=null ? notebookRoot.resolve(expectedLinkRelativeToRoot).toString() : null;
        assertEquals(expectedLink, resolver.getResolvedLink());
        assertEquals(expectedDescription, resolver.getLinkDescription());
    }
}
