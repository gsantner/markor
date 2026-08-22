package net.gsantner.opoc.frontend.filebrowser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeTrue;

import org.junit.Test;

import java.io.File;
import java.util.Collections;

public class GsFileBrowserListAdapterTest {
    @Test
    public void mappedFolderGoesUpToVirtualRootBeforeWritablePhysicalParent() {
        final File writableParent = new File(System.getProperty("java.io.tmpdir"));
        assumeTrue(writableParent.canWrite());
        final File mappedFolder = new File(writableParent, "markor-private-app-data");

        assertEquals(GsFileBrowserListAdapter.VIRTUAL_STORAGE_ROOT,
                GsFileBrowserListAdapter.findBrowsableParent(
                        mappedFolder, Collections.singleton(mappedFolder), null));
    }

    @Test
    public void regularFolderGoesUpToWritablePhysicalParent() {
        final File writableParent = new File(System.getProperty("java.io.tmpdir"));
        assumeTrue(writableParent.canWrite());
        final File currentFolder = new File(writableParent, "markor-folder");

        assertEquals(writableParent, GsFileBrowserListAdapter.findBrowsableParent(
                currentFolder, Collections.emptySet(), null));
    }

    @Test
    public void regularFolderDoesNotExposeInaccessiblePhysicalParent() {
        final File currentFolder = new File("/markor-inaccessible/child");

        assertNull(GsFileBrowserListAdapter.findBrowsableParent(
                currentFolder, Collections.emptySet(), null));
    }
}
