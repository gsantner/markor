package net.gsantner.markor.format.zimwiki;

import android.app.Activity;
import android.app.AlertDialog;

import net.gsantner.markor.R;
import net.gsantner.markor.util.ShareUtil;
import net.gsantner.opoc.util.FileUtils;

import java.io.File;

public class ZimPageFilePathUtil {

    public static void copyFileToZimPageFolder(File fileToBeCopied, File currentWorkingFile, Activity activity) {
        File targetCopy = new File(getZimPageFolderOrCreate(currentWorkingFile), fileToBeCopied.getName());
        new ShareUtil(activity).writeFile(targetCopy, false, (opened, outputStream) -> {
            if (opened) {
                FileUtils.copyFile(fileToBeCopied, outputStream);
                new AlertDialog.Builder(activity)
                        .setTitle(R.string.file_copied)
                        .setMessage(R.string.file_copied_to_zim_page_folder)
                        .setNegativeButton(R.string.close, ((dialogInterface, i) -> dialogInterface.dismiss()))
                        .show();
            } else {
                // if an image has been directly created/edited in the page folder, no further copying necessary
                // files with the same name won't be overwritten
            }
        });
    }

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
