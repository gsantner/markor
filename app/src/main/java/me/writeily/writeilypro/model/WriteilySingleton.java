package me.writeily.writeilypro.model;

import android.os.Environment;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.widget.BaseAdapter;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

/**
 * Created by jeff on 14-12-11.
 */
public class WriteilySingleton {

    private static WriteilySingleton writeilySingletonInstnce = null;

    private static void WriteilySingleton() {

    }

    public static WriteilySingleton getInstance() {
        if (writeilySingletonInstnce == null) {
            writeilySingletonInstnce = new WriteilySingleton();
        }

        return writeilySingletonInstnce;
    }

    public void copyFile(File file, String destinationDir) {
        try {
            String filename = file.getName();

            FileOutputStream fos = new FileOutputStream(Environment.getExternalStorageDirectory() + destinationDir + filename);
            OutputStreamWriter writer = new OutputStreamWriter(fos);

            FileInputStream is = new FileInputStream(file);
            InputStreamReader reader = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(reader);

            String fileContent;
            StringBuilder fileContentBuilder = new StringBuilder();

            while ((fileContent = br.readLine()) != null) {
                fileContentBuilder.append(fileContent + "\n");
            }

            writer.write(fileContentBuilder.toString());
            writer.flush();

            writer.close();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void moveFile(File file, String destinationDir) {
        copyFile(file, destinationDir);

        // Delete the old file after copying it over
        file.delete();
    }

    public void moveSelectedNotes(ListView notesListView, BaseAdapter notesAdapter, String destination) {
        SparseBooleanArray checkedIndices = notesListView.getCheckedItemPositions();
        for (int i = 0; i < checkedIndices.size(); i++) {
            if (checkedIndices.valueAt(i)) {
                File file = (File) notesAdapter.getItem(checkedIndices.keyAt(i));
                moveFile(file, destination);
            }
        }
    }

    public void copySelectedNotes(ListView notesListView, BaseAdapter notesAdapter, String destination) {
        SparseBooleanArray checkedIndices = notesListView.getCheckedItemPositions();
        for (int i = 0; i < checkedIndices.size(); i++) {
            if (checkedIndices.valueAt(i)) {
                File file = (File) notesAdapter.getItem(checkedIndices.keyAt(i));
                copyFile(file, destination);
            }
        }
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
     * @param dir the directory to add files from
     */
    public ArrayList<File> addFilesFromDirectory(File dir, ArrayList<File> files) {
        for (File f : dir.listFiles()) {
            Log.d("Adding file:", f.getAbsolutePath());
            files.add(f);
        }
        return files;
    }
}
