package me.writeily.writeilypro.model;

import android.os.Environment;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.widget.ListView;

import me.writeily.writeilypro.adapter.NotesAdapter;

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

    public void moveFile(String filename, String sourceDir, String destinationDir) {
        Log.d("MOVING FILE", "File: " + filename + " from " + sourceDir + " to " + destinationDir);
        try {
            FileOutputStream fos = new FileOutputStream(Environment.getExternalStorageDirectory() + destinationDir + filename);
            OutputStreamWriter writer = new OutputStreamWriter(fos);

            FileInputStream is = new FileInputStream(Environment.getExternalStorageDirectory() + sourceDir + filename);
            InputStreamReader reader = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(reader);

            String fileContent = "";
            StringBuilder fileContentBuilder = new StringBuilder();

            while ((fileContent = br.readLine()) != null) {
                fileContentBuilder.append(fileContent + "\n");
            }

            writer.write(fileContentBuilder.toString());
            writer.flush();

            writer.close();
            fos.close();

            // Delete old file
            File oldFile = new File(Environment.getExternalStorageDirectory() + sourceDir + filename);
            oldFile.delete();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void moveSelectedNotes(ListView notesListView, NotesAdapter notesAdapter, String source, String destination) {
        SparseBooleanArray checkedIndices = notesListView.getCheckedItemPositions();
        for (int i = 0; i < checkedIndices.size(); i++) {
            if (checkedIndices.valueAt(i)) {
                File note = notesAdapter.getItem(checkedIndices.keyAt(i));
                moveFile(note.getName(), source, destination);
            }
        }
    }

    /**
     * Recursively add all .txt files from the specified directory
     * @param dir the directory to add files from
     */
    public ArrayList<File> addTextFilesFromDirectory(File dir, ArrayList<File> notes) {
        for (File f : dir.listFiles()) {

            Log.d("Adding text file:", f.getAbsolutePath());

            if (f.getName().endsWith(Constants.TXT_EXT)) {
                notes.add(f);
            } else if (f.isDirectory() && !f.getAbsolutePath().contains("archived")) {
                addTextFilesFromDirectory(f, notes);
            }
        }
        return notes;
    }

    /**
     * Recursively add all files from the specified directory
     * @param dir the directory to add files from
     */
    public ArrayList<File> addFilesFromDirectory(File dir, ArrayList<File> notes) {
        for (File f : dir.listFiles()) {
            Log.d("Adding file:", f.getAbsolutePath());
            notes.add(f);
        }
        return notes;
    }
}
