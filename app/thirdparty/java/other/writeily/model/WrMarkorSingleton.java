/*#######################################################
 * Copyright (c) 2014 Jeff Martin
 * Copyright (c) 2015 Pedro Lafuente
 * Copyright (c) 2017-2018 Gregor Santner
 *
 * Licensed under the MIT license.
 * You can get a copy of the license text here:
 *   https://opensource.org/licenses/MIT
###########################################################*/
package other.writeily.model;

import net.gsantner.markor.format.markdown.MarkdownTextConverter;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

    public String copyDirectory(File dir, String destinationDir) {
        String dirName = dir.getName();
        File outputFile = new File(destinationDir + File.separator + dirName + File.separator);

        if (!outputFile.exists()) {
            outputFile.mkdir();
        }

        return outputFile.getAbsolutePath();
    }

    public void copyFile(File file, String destinationDir) {
        FileInputStream input = null;
        FileOutputStream output = null;
        try {
            input = new FileInputStream(file);
            output = new FileOutputStream(new File(destinationDir, file.getName()));
            IOUtils.copy(input, output);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(input);
            IOUtils.closeQuietly(output);
        }
    }


    public void moveFile(File file, String destinationDir) {
        /* Rules:
         * 1. Don't move a file to the same location, no point
         * 2. Don't move a folder to itself
         * 3. Don't move a folder into its children
         */

        if (destinationDir != null &&
                !destinationDir.equalsIgnoreCase(file.getParentFile().getAbsolutePath()) &&
                !destinationDir.startsWith(file.getAbsolutePath())) {

            if (file.isDirectory()) {
                String newDestinationDir = copyDirectory(file, destinationDir);

                for (File dirFile : file.listFiles()) {
                    moveFile(dirFile, newDestinationDir);
                }
            } else {
                copyFile(file, destinationDir);
            }

            // Delete the old file after copying it over
            deleteFile(file);

        }
    }

    public boolean deleteFile(File file) {
        if (file.isDirectory()) {
            for (File childFile : file.listFiles()) {
                deleteFile(childFile);
            }
        }

        return file.delete();
    }

    public void deleteSelectedItems(List<File> files) {
        for (File file : files) {
            deleteFile(file);
        }
    }

    public void moveSelectedNotes(List<File> files, String destination) {
        for (File file : files) {
            moveFile(file, destination);
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
                } else if (MarkdownTextConverter.isTextOrMarkdownFile(f)) {
                    addedFiles.add(f);
                }
            }
        }

        // Append addedFiles to files so directories appear first
        files.addAll(addedFiles);
        return files;
    }
}
