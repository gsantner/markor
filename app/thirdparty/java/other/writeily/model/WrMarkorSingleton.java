/*#######################################################
 * Copyright (c) 2014 Jeff Martin
 * Copyright (c) 2015 Pedro Lafuente
 * Copyright (c) 2017-2019 Gregor Santner
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

    private boolean saneMoveOrCopy(final File file, final File dest) {
        /* Rules:
         * 1. Don't move a file to the same location, no point
         * 2. Don't move a folder to itself
         * 3. Don't move a folder into its children
         */

        try {
            return (file != null &&
                    dest != null &&
                    !file.equals(dest) &&
                    // dest is file's child
                    !dest.getCanonicalPath().startsWith(file.getCanonicalPath()));
        } catch (IOException e) {
            return false;
        }
    }

    public boolean moveFile(final File file, final File dest, final Context context) {
        if (saneMoveOrCopy(file, dest) && !dest.exists()) {
            boolean renameSuccess;
            try {
                renameSuccess = file.renameTo(dest);
            } catch (Exception e) {
                renameSuccess = false;
            }
            return (renameSuccess || (copyFile(file, dest) && deleteFile(file, context)));
        }
        return false;
    }

    public boolean copyFile(final File file, final File dest) {
        if (saneMoveOrCopy(file, dest) && !dest.exists()) {
            if (file.isDirectory()) {
                if (dest.mkdir()) {
                    boolean success = true;
                    for (final File dirFile : file.listFiles()) {
                        // Merge not supported, dest here will always be available
                        success &= this.copyFile(dirFile, new File(dest, dirFile.getName()));
                    }
                    return success;
                }
                return false;
            } else {
                FileUtils.copyFile(file, dest);
                return true;
            }
        }
        return false;
    }

    public boolean deleteFile(final File file, final Context context) {
        if (file.isDirectory()) {
            for (final File childFile : file.listFiles()) {
                deleteFile(childFile, context);
            }
        }

        final ShareUtil shareUtil = new ShareUtil(context);
        if (context != null && shareUtil.isUnderStorageAccessFolder(file)) {
            final DocumentFile dof = shareUtil.getDocumentFile(file, file.isDirectory());
            shareUtil.freeContextRef();
            return dof == null ? false : (dof.delete() || !dof.exists());
        } else {
            shareUtil.freeContextRef();
            return file.delete();
        }
    }

    public void deleteSelectedItems(final Collection<File> files, final Context context) {
        for (final File file : files) {
            deleteFile(file, context);
        }
    }

    private enum ConflictResollution {
        KEEP_BOTH,
        OVERWRITE,
        SKIP,
        ASK
    }

    public void moveOrCopySelected(final List<File> files, final File destDir, final Activity activity, final boolean isMove) {
        if (destDir.isDirectory()) {
            boolean allSane = true;
            for (final File f : files) {
                allSane &= saneMoveOrCopy(f, new File(destDir, f.getName()));
            }
            if (allSane) {
                final Stack<File> _files = new Stack<>();
                _files.addAll(files);
                _moveOrCopySelected(_files, destDir, activity, isMove, ConflictResollution.ASK, false);
                return;
            }
        }
        // Toast.makeText(activity, "✗", Toast.LENGTH_SHORT).show();
    }

    private void _moveOrCopySelected(
            final Stack<File> files,
            final File destDir,
            final Activity activity,
            final boolean isMove,
            ConflictResollution resolution,
            boolean preserveResolution
    ) {
        while (!files.empty()) {
            final File file = files.pop();
            final File dest = new File(destDir, file.getName());
            if (dest.exists()) {
                if (resolution == ConflictResollution.KEEP_BOTH) {
                    moveOrCopy(activity, file, findNonConflictingDest(file, destDir), isMove);
                } else if (resolution == ConflictResollution.OVERWRITE) {
                    if (deleteFile(dest, activity)) {
                        moveOrCopy(activity, file, dest, isMove);
                    }
                } else if (resolution == ConflictResollution.ASK) {
                    // Put the file back in
                    files.push(file);
                    SearchOrCustomTextDialogCreator.showCopyMoveConflictDialog(
                            activity, file.getName(), destDir.getName(), files.size() > 1, (name, option) -> {
                                ConflictResollution res = ConflictResollution.ASK;
                                if (option == 0 || option == 3) {
                                    res = ConflictResollution.KEEP_BOTH;
                                } else if (option == 1 || option == 4) {
                                    res = ConflictResollution.OVERWRITE;
                                } else if (option == 2 || option == 5) {
                                    res = ConflictResollution.SKIP;
                                }
                                _moveOrCopySelected(files, destDir, activity, isMove, res, option > 2);
                            });
                    return; // Process will be continued by callback
                }
                resolution = preserveResolution ? resolution : ConflictResollution.ASK;
            } else {
                moveOrCopy(activity, file, dest, isMove);
            }
        }
    }

    private void moveOrCopy(final Context context, final File src, final File dest, final boolean isMove) {
        if (isMove) {
            moveFile(src, dest, context);
        } else {
            copyFile(src, dest);
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
