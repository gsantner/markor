package net.gsantner.markor.format.zimwiki;

import java.io.File;

public class ZimPageFilePathUtil {

    public static File getFileToRelativeZimLink(String zimLink, File currentWorkingFile) {
        String filename = zimLink.replaceFirst("^\\./", "");
        return new File(getZimPageFolderOrCreate(currentWorkingFile), filename);
    }

    public static File getZimPageFolderOrCreate(File currentWorkingFile) {
        File folderToCurrentZimPage = new File(currentWorkingFile.getParentFile(), currentWorkingFile.getName().replace(".txt", ""));
        folderToCurrentZimPage.mkdir();
        return folderToCurrentZimPage;
    }

    // TODO: methods for translating zim page paths (relative, absolute, ...) to file structures and vice versa
    // getFileToPagePath, getFileToSubPagePath, getFileToAbsolutePagePath, getFileToParentPagePath, getAbsolutePagePathFromFile, ...
}
