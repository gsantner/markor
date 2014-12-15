package me.writeily.writeilypro.model;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import me.writeily.writeilypro.R;
import me.writeily.writeilypro.adapter.FilesAdapter;

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

    public void moveFile(File file, String destinationDir) {
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

            // Delete old file
            file.delete();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void moveSelectedNotes(ListView notesListView, FilesAdapter filesAdapter, String destination) {
        SparseBooleanArray checkedIndices = notesListView.getCheckedItemPositions();
        for (int i = 0; i < checkedIndices.size(); i++) {
            if (checkedIndices.valueAt(i)) {
                File file = filesAdapter.getItem(checkedIndices.keyAt(i));
                moveFile(file, destination);
            }
        }
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
