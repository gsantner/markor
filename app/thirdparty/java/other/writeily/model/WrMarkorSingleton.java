/*#######################################################
 * Copyright (c) 2014 Jeff Martin
 * Copyright (c) 2015 Pedro Lafuente
 * Copyright (c) 2017-2024 Gregor Santner
 *
 * Licensed under the MIT license.
 * You can get a copy of the license text here:
 *   https://opensource.org/licenses/MIT
###########################################################*/
package other.writeily.model;

import android.app.Activity;
import android.content.Context;

import androidx.documentfile.provider.DocumentFile;

import net.gsantner.markor.frontend.MarkorDialogFactory;
import net.gsantner.markor.util.MarkorContextUtils;
import net.gsantner.opoc.util.GsFileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

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

    private boolean saneCopy(final File file, final File dest) {
        return file != null && dest != null && file.exists() && !GsFileUtils.isChild(file, dest);
    }

    private boolean saneMove(final File file, final File dest) {
        return saneCopy(file, dest) && !file.equals(dest);
    }

    public boolean moveFile(final File file, final File dest, final Context context) {
        if (saneMove(file, dest) && !dest.exists()) {
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
        if (saneCopy(file, dest) && !dest.exists()) {
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
                GsFileUtils.copyFile(file, dest);
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

        final MarkorContextUtils cu = new MarkorContextUtils(context);
        if (context != null && cu.isUnderStorageAccessFolder(context, file, file.isDirectory())) {
            final DocumentFile dof = cu.getDocumentFile(context, file, file.isDirectory());
            return dof == null ? false : (dof.delete() || !dof.exists());
        } else {
            return file.delete();
        }
    }

    public void deleteSelectedItems(final Collection<File> files, final Context context) {
        for (final File file : files) {
            deleteFile(file, context);
        }
    }

    private enum ConflictResolution {
        KEEP_BOTH, OVERWRITE, SKIP, ASK
    }

    public void moveOrCopySelected(final List<File> files, final File destDir, final Activity activity, final boolean isMove) {
        if (destDir.isDirectory()) {
            boolean allSane = true;
            for (final File file : files) {
                final File dest = new File(destDir, file.getName());
                allSane &= isMove ? saneMove(file, dest) : saneCopy(file, dest);
            }
            if (allSane) {
                final Stack<File> _files = new Stack<>();
                _files.addAll(files);
                _moveOrCopySelected(_files, destDir, activity, isMove, ConflictResolution.ASK, false);
                return;
            }
        }
    }

    private void _moveOrCopySelected(final Stack<File> files, final File destDir, final Activity activity, final boolean isMove, ConflictResolution resolution, boolean preserveResolution) {
        while (!files.empty()) {
            final File file = files.pop();
            final File dest = new File(destDir, file.getName());
            if (dest.exists()) {
                // Special case - duplicate the file with new name if copying to same directory
                if (resolution == ConflictResolution.KEEP_BOTH || (!isMove && file.equals(dest))) {
                    moveOrCopy(activity, file, GsFileUtils.findNonConflictingDest(destDir, file.getName()), isMove);
                } else if (resolution == ConflictResolution.OVERWRITE) {
                    if (deleteFile(dest, activity)) {
                        moveOrCopy(activity, file, dest, isMove);
                    }
                } else if (resolution == ConflictResolution.ASK) {
                    // Put the file back in
                    files.push(file);
                    MarkorDialogFactory.showCopyMoveConflictDialog(activity, file.getName(), destDir.getName(), files.size() > 1, (option) -> {
                        ConflictResolution res = ConflictResolution.ASK;
                        if (option == 0 || option == 3) {
                            res = ConflictResolution.KEEP_BOTH;
                        } else if (option == 1 || option == 4) {
                            res = ConflictResolution.OVERWRITE;
                        } else if (option == 2 || option == 5) {
                            res = ConflictResolution.SKIP;
                        }
                        _moveOrCopySelected(files, destDir, activity, isMove, res, option > 2);
                    });
                    return; // Process will be continued by callback
                }
                resolution = preserveResolution ? resolution : ConflictResolution.ASK;
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
                } else {
                    addedFiles.add(f);
                }
            }
        }

        // Append addedFiles to files so directories appear first
        files.addAll(addedFiles);
        return files;
    }
}
