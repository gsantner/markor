package me.writeily.model;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.widget.BaseAdapter;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by jeff on 14-12-11.
 */
public class WriteilySingleton {

    private static WriteilySingleton writeilySingletonInstance = null;
    private static File notesLastDirectory = null;

    private static void WriteilySingleton() {

    }

    public static WriteilySingleton getInstance() {
        if (writeilySingletonInstance == null) {
            writeilySingletonInstance = new WriteilySingleton();
        }

        return writeilySingletonInstance;
    }

    public File getNotesLastDirectory() {
        return notesLastDirectory;
    }

    public void setNotesLastDirectory(File notesLastDirectory) {
        WriteilySingleton.notesLastDirectory = notesLastDirectory;
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

    public void copySelectedNotes(SparseBooleanArray checkedIndices, BaseAdapter notesAdapter, String destination) {
        for (int i = 0; i < checkedIndices.size(); i++) {
            if (checkedIndices.valueAt(i)) {
                File file = (File) notesAdapter.getItem(checkedIndices.keyAt(i));
                copyFile(file, destination);
            }
        }
    }

    public Uri getUriFromFile(File f) {
        Uri u = null;
        if (f != null) {
            u = Uri.parse(f.toURI().toString());
        }
        return u;
    }

    public File getFileFromUri(Uri u) {
        File f = null;
        if (u != null) {
            try {
                f = new File(new java.net.URI(URLEncoder.encode(u.toString(), "UTF-8")));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        return f;
    }

    public String readFileUri(Uri fileUri, Context context) {
        StringBuilder uriContent = new StringBuilder();
        if (fileUri != null) {
            try {
                InputStreamReader reader = new InputStreamReader(context.getContentResolver().openInputStream(fileUri));
                BufferedReader br = new BufferedReader(reader);

                while (br.ready()) {
                    uriContent.append(br.readLine());
                    uriContent.append("\n");
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return uriContent.toString();
    }

    /**
     * Hide the header when getting to the external dir so the app doesn't show too much.
     */
    public boolean isRootDir(File previousDir, File compareDir) {
        return (previousDir == null || previousDir.getPath().equalsIgnoreCase(compareDir.getAbsolutePath()));
    }

    public boolean isDirectoryEmpty(ArrayList<File> files) {
        return (files == null || files.isEmpty());
    }

    /**
     * Recursively add all files from the specified directory
     *
     * @param sourceDir the directory to add files from
     */
    public ArrayList<File> addFilesFromDirectory(File sourceDir, ArrayList<File> files) {
        ArrayList<File> addedFiles = new ArrayList<>();

        List<File> listedData = Arrays.asList(sourceDir.listFiles());
        Collections.sort(listedData, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                return f1.getName().compareToIgnoreCase(f2.getName());
            }
        });

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

    /**
     * Recursively add all directories from the specified directory
     *
     * @param dir the directory to add files from
     */
    public ArrayList<File> addDirectories(File dir, ArrayList<File> files) {
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                Log.d("Adding directory:", f.getAbsolutePath());
                files.add(f);
            }
        }
        return files;
    }
}
