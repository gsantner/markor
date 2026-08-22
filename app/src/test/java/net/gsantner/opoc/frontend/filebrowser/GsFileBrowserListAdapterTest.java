package net.gsantner.opoc.frontend.filebrowser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.io.File;
import java.util.Collections;

public class GsFileBrowserListAdapterTest {
    @Test
    public void usesImmediateWritableParent() {
        final File writableParent = file("/writable", null, true);
        final File currentFolder = file("/writable/current", writableParent, true);

        assertEquals(writableParent, GsFileBrowserListAdapter.findBrowsableParent(
                currentFolder, null, GsFileBrowserListAdapter.VIRTUAL_STORAGE_ROOT));
    }

    @Test
    public void walksPastInaccessibleParents() {
        final File writableAncestor = file("/writable", null, true);
        final File inaccessibleParent = file("/writable/inaccessible", writableAncestor, false);
        final File currentFolder = file("/writable/inaccessible/current", inaccessibleParent, true);

        assertEquals(writableAncestor, GsFileBrowserListAdapter.findBrowsableParent(
                currentFolder, null, null));
    }

    @Test
    public void usesVirtualFallbackWhenNoPhysicalParentIsWritable() {
        final File inaccessibleRoot = file("/inaccessible", null, false);
        final File currentFolder = file("/inaccessible/current", inaccessibleRoot, true);

        assertEquals(GsFileBrowserListAdapter.VIRTUAL_STORAGE_ROOT,
                GsFileBrowserListAdapter.findBrowsableParent(
                        currentFolder, null, GsFileBrowserListAdapter.VIRTUAL_STORAGE_ROOT));
    }

    @Test
    public void returnsNullWhenNoParentOrFallbackIsBrowsable() {
        final File inaccessibleRoot = file("/inaccessible", null, false);
        final File currentFolder = file("/inaccessible/current", inaccessibleRoot, true);

        assertNull(GsFileBrowserListAdapter.findBrowsableParent(currentFolder, null, null));
    }

    @Test
    public void inaccessibleStorageDescendantsAreSkipped() {
        final File storage = file("/storage", null, false);
        final File inaccessibleParent = file("/storage/inaccessible", storage, false);
        final File currentFolder = file("/storage/inaccessible/current", inaccessibleParent, true);

        assertEquals(storage, GsFileBrowserListAdapter.findBrowsableParent(
                currentFolder, null, null));
    }

    @Test
    public void mountedStorageDescendantsAreBrowsable() {
        final File mountedStorage = new File("/mounted");
        final File mountedParent = file("/mounted/parent", mountedStorage, false);
        final File currentFolder = file("/mounted/parent/current", mountedParent, true);

        assertEquals(mountedParent, GsFileBrowserListAdapter.findBrowsableParent(
                currentFolder, mountedStorage, null));
    }

    @Test
    public void physicalRootDoesNotUseVirtualFallback() {
        final File physicalRoot = file("/", null, false);

        assertNull(GsFileBrowserListAdapter.findBrowsableParent(
                physicalRoot, null, GsFileBrowserListAdapter.VIRTUAL_STORAGE_ROOT));
    }

    @Test
    public void mappedPhysicalDescendantUsesVirtualParentAsFallback() {
        final File mappedPhysicalFolder = new File("/private/app-data");
        final File currentFolder = new File(mappedPhysicalFolder, "snippets");
        final File virtualFolder = GsFileBrowserListAdapter.VIRTUAL_STORAGE_APP_DATA_PRIVATE;

        assertEquals(GsFileBrowserListAdapter.VIRTUAL_STORAGE_ROOT,
                GsFileBrowserListAdapter.findVirtualParentFallback(currentFolder,
                        Collections.singletonMap(virtualFolder, mappedPhysicalFolder)));
    }

    @Test
    public void mappedPhysicalFolderUsesVirtualParentAsFallback() {
        final File mappedPhysicalFolder = new File("/private/app-data");
        final File virtualFolder = GsFileBrowserListAdapter.VIRTUAL_STORAGE_APP_DATA_PRIVATE;

        assertEquals(GsFileBrowserListAdapter.VIRTUAL_STORAGE_ROOT,
                GsFileBrowserListAdapter.findVirtualParentFallback(mappedPhysicalFolder,
                        Collections.singletonMap(virtualFolder, mappedPhysicalFolder)));
    }

    @Test
    public void unrelatedPhysicalFolderHasNoVirtualFallback() {
        assertNull(GsFileBrowserListAdapter.findVirtualParentFallback(
                new File("/unrelated"), Collections.emptyMap()));
    }

    private static File file(final String path, final File parent, final boolean writable) {
        return new File(path) {
            @Override
            public File getParentFile() {
                return parent;
            }

            @Override
            public boolean canWrite() {
                return writable;
            }
        };
    }
}
