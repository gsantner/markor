/*#######################################################
 * Copyright (c) 2014 Jeff Martin
 * Copyright (c) 2015 Pedro Lafuente
 * Copyright (c) 2017-2022 Gregor Santner
 *
 * Licensed under the MIT license.
 * You can get a copy of the license text here:
 *   https://opensource.org/licenses/MIT
###########################################################*/
package other.writeily.model;

import android.app.Activity;
import android.content.Context;
import android.support.v4.provider.DocumentFile;

import net.gsantner.markor.format.TextFormat;
import net.gsantner.markor.ui.SearchOrCustomTextDialogCreator;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.ShareUtil;
import net.gsantner.opoc.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

@SuppressWarnings("all")
public class WrMarkorSingleton {

    private static WrMarkorSingleton markorSingletonInstance = null;
    private static File notesLastDirectory = null;

    public static WrMarkorSingleton getInstance() {
        if (markorSingletonInstance == null) {
            markorSingletonInstance = new WrMarkorSingleton();
        }

        return markorSingletonInstance;
    }

    public File getNotesLastDirectory() {
        return notesLastDirectory;
    }

    public void setNotesLastDirectory(File notesLastDirectory) {
        WrMarkorSingleton.notesLastDirectory = notesLastDirectory;
    }

    // Returns true if b is not a child of a. A file is not a child of itself
    private boolean notChild(final File a, final File b) {
        try {
            return b.equals(a) || !b.getParentFile().getCanonicalPath().startsWith(a.getCanonicalPath());
        } catch (IOException e) {
            return false; // Not sure, return false for safety
        }
    }

    private boolean saneCopy(final File file, final File dest) {
        return file != null && dest != null && file.exists() && notChild(file, dest);
    }

    private boolean saneMove(final File file, final File dest) {
        return saneCopy(file, dest) && !file.equals(dest);
    }

    public boolean moveFile(final File file, final File dest, final Context context, AppSettings appSettings) {
        if (saneMove(file, dest) && !dest.exists()) {
            return (copyFile(file, dest, appSettings) && deleteFile(file, context, appSettings));
        }
        return false;
    }

    public boolean copyFile(final File file, final File dest, AppSettings appSettings) {
        if (saneCopy(file, dest) && !dest.exists()) {
            if (file.isDirectory()) {
                if (dest.mkdir()) {
                    boolean success = true;
                    appSettings.copyFavouriteFile(file, dest);
                    for (final File dirFile : file.listFiles()) {
                        // Merge not supported, dest here will always be available
                        success &= this.copyFile(dirFile, new File(dest, dirFile.getName()), appSettings);
                    }
                    return success;
                }
                return false;
            } else {
                FileUtils.copyFile(file, dest);
                appSettings.copyFavouriteFile(file, dest);
                return true;
            }
        }
        return false;
    }

    public boolean deleteFile(final File file, final Context context, AppSettings appSettings) {
        if (file.isDirectory()) {
            for (final File childFile : file.listFiles()) {
                deleteFile(childFile, context, appSettings);
            }
        }

        final ShareUtil shareUtil = new ShareUtil(context);
        if (context != null && shareUtil.isUnderStorageAccessFolder(file)) {
            final DocumentFile dof = shareUtil.getDocumentFile(file, file.isDirectory());
            shareUtil.freeContextRef();
            boolean successfulDelete = true;
            if(dof == null)
                successfulDelete = false;
            else if(dof.delete()) {
                appSettings.deleteFavouriteFile(file);
            }
            else if(dof.exists()) successfulDelete = true;
            return successfulDelete;
        } else {
            shareUtil.freeContextRef();
            boolean successfulDelete = file.delete();
            if(successfulDelete) {
                appSettings.deleteFavouriteFile(file);
            }
            return successfulDelete;
        }
    }

    public void deleteSelectedItems(final Collection<File> files, final Context context, AppSettings appSettings) {
        for (final File file : files) {
            deleteFile(file, context, appSettings);
        }
    }

    private enum ConflictResolution {
        KEEP_BOTH,
        OVERWRITE,
        SKIP,
        ASK
    }

    public void moveOrCopySelected(final List<File> files, final File destDir, final Activity activity, final boolean isMove, AppSettings appSettings) {
        if (destDir.isDirectory()) {
            boolean allSane = true;
            for (final File file : files) {
                final File dest = new File(destDir, file.getName());
                allSane &= isMove ? saneMove(file, dest) : saneCopy(file, dest);
            }
            if (allSane) {
                final Stack<File> _files = new Stack<>();
                _files.addAll(files);
                _moveOrCopySelected(_files, destDir, activity, isMove, ConflictResolution.ASK, false, appSettings);
                return;
            }
        }
    }

    private void _moveOrCopySelected(
            final Stack<File> files,
            final File destDir,
            final Activity activity,
            final boolean isMove,
            ConflictResolution resolution,
            boolean preserveResolution,
            AppSettings appSettings
    ) {
        while (!files.empty()) {
            final File file = files.pop();
            final File dest = new File(destDir, file.getName());
            if (dest.exists()) {
                // Special case - duplicate the file with new name if copying to same directory
                if (resolution == ConflictResolution.KEEP_BOTH || (!isMove && file.equals(dest))) {
                    moveOrCopy(activity, file, findNonConflictingDest(file, destDir), isMove, appSettings);
                } else if (resolution == ConflictResolution.OVERWRITE) {
                    if (deleteFile(dest, activity, appSettings)) {
                        moveOrCopy(activity, file, dest, isMove, appSettings);
                    }
                } else if (resolution == ConflictResolution.ASK) {
                    // Put the file back in
                    files.push(file);
                    SearchOrCustomTextDialogCreator.showCopyMoveConflictDialog(
                            activity, file.getName(), destDir.getName(), files.size() > 1, (option) -> {
                                ConflictResolution res = ConflictResolution.ASK;
                                if (option == 0 || option == 3) {
                                    res = ConflictResolution.KEEP_BOTH;
                                } else if (option == 1 || option == 4) {
                                    res = ConflictResolution.OVERWRITE;
                                } else if (option == 2 || option == 5) {
                                    res = ConflictResolution.SKIP;
                                }
                                _moveOrCopySelected(files, destDir, activity, isMove, res, option > 2, appSettings);
                            });
                    return; // Process will be continued by callback
                }
                resolution = preserveResolution ? resolution : ConflictResolution.ASK;
            } else {
                moveOrCopy(activity, file, dest, isMove, appSettings);
            }
        }
    }

    private void moveOrCopy(final Context context, final File src, final File dest, final boolean isMove, AppSettings appSettings) {
        if (isMove) {
            moveFile(src, dest, context, appSettings);
        } else {
            copyFile(src, dest, appSettings);
        }
    }

    public File findNonConflictingDest(final File file, final File destDir) {
        File dest = new File(destDir, file.getName());
        final String[] splits = file.getName().split("\\.");
        final String name = splits[0];
        splits[0] = "";
        final String extension = String.join(".", splits);
        int i = 1;
        while (dest.exists()) {
            dest = new File(destDir, String.format("%s_%d%s", name, i, extension));
            i++;
        }
        return dest;
    }

    public boolean isDirectoryEmpty(ArrayList<File> files) {
        return (files == null || files.isEmpty());
    }

    /**
     * Recursively add all files from the specified directory
     *
     * @param sourceDir the directory to add files from
     */
    public ArrayList<File> addMarkdownFilesFromDirectory(File sourceDir, ArrayList<File> files) {
        ArrayList<File> addedFiles = new ArrayList<>();

        List<File> listedData = Arrays.asList(sourceDir.listFiles());
        Collections.sort(listedData, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));

        for (File f : listedData) {
            if (!f.getName().startsWith(".")) {
                if (f.isDirectory()) {
                    files.add(f);
                } else if (TextFormat.isTextFile(f)) {
                    addedFiles.add(f);
                }
            }
        }

        // Append addedFiles to files so directories appear first
        files.addAll(addedFiles);
        return files;
    }
}
