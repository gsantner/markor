package net.gsantner.markor.activity;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.v4.app.FragmentManager;
import android.widget.Toast;

import net.gsantner.markor.R;
import net.gsantner.markor.ui.FilesystemViewerCreator;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.opoc.ui.FilesystemViewerData;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class BackupRestoreHelper {

    private static final String SAVE_NAME = "markor_settings_backup%s.zip";
    private static final int BUFFER_SIZE = 2048;

    private static String[] BACKUP_PATHS = {
            "shared_prefs/action_order.xml",
            "shared_prefs/app.xml",
            "shared_prefs/datetime_dialog_settings.xml",
            "shared_prefs/search_replace_dialog_settings.xml",
            "shared_prefs/%PACKAGENAME%_preferences.xml"
    };

    private static List<File> getBackupFiles(final Context context) throws PackageManager.NameNotFoundException {
        final PackageManager packageManager = context.getPackageManager();
        final PackageInfo p = packageManager.getPackageInfo(context.getPackageName(), 0);
        final File dataDir = new File(p.applicationInfo.dataDir);
        final String packageName = p.applicationInfo.packageName;
        final List<File> files = new ArrayList<File>();
        for (final String path : BACKUP_PATHS) {
            files.add(new File(dataDir, path.replace("%PACKAGENAME%", packageName)));
        }

        return files;
    }

    public static void backupConfig(final Context context, final FragmentManager manager) {

        if (context instanceof Activity) {
            final Activity activity = (Activity) context;

            FilesystemViewerCreator.showFolderDialog(
                    new FilesystemViewerData.SelectionListenerAdapter() {
                        @Override
                        public void onFsViewerConfig(FilesystemViewerData.Options dopt) {
                            dopt.rootFolder = new AppSettings(context).getNotebookDirectory();
                            dopt.titleText = R.string.select_backup_file_location;
                        }

                        @Override
                        public void onFsViewerSelected(String request, File dir) {
                            createAndSaveBackup(context, getSaveFile(dir));
                        }
                    }, manager, activity
            );
        }
    }

    public static File getSaveFile(final File folder) {
        File file;
        int index = 0;
        do {
            file = new File(folder, String.format(SAVE_NAME, (index == 0) ? "" : String.format("_%d", index)));
            index++;
        } while (file.exists());
        return file;
    }

    public static void createAndSaveBackup(final Context context, final File saveLoc) {
        try {
            final FileOutputStream fos = new FileOutputStream(saveLoc);
            final BufferedOutputStream bos = new BufferedOutputStream(fos);
            final ZipOutputStream zos = new ZipOutputStream(bos);

            for (final File f : getBackupFiles(context)) {
                if (!f.exists()) {
                   throw new RuntimeException(String.format("%s does not exist", f.getName()));
                }
                addFileToZip(zos, f);
            }
            Toast.makeText(context, context.getString(R.string.toast_backup_success, saveLoc.getName()), Toast.LENGTH_SHORT).show();
            zos.flush();
            zos.close();
        } catch (Exception e) {
            // Attempt to delete file if it exists
            if (saveLoc.exists()) {
                saveLoc.delete();
            }
            Toast.makeText(context, R.string.toast_backup_error, Toast.LENGTH_SHORT).show();
        }
    }

    public static void restoreConfig(final Context context, final FragmentManager manager) {
        if (context instanceof Activity) {
            final Activity activity = (Activity) context;

            FilesystemViewerCreator.showFileDialog(
                    new FilesystemViewerData.SelectionListenerAdapter() {
                        @Override
                        public void onFsViewerConfig(FilesystemViewerData.Options dopt) {
                            dopt.rootFolder = new AppSettings(context).getNotebookDirectory();
                            dopt.titleText = R.string.select_backup_file_location;
                        }

                        @Override
                        public void onFsViewerSelected(String request, File file) {
                            loadAndRestoreBackup(context, file);
                        }
                    }, manager, activity,
                    input -> input != null && input.exists() && input.toString().trim().toLowerCase().endsWith(".zip")
            );
        }

    }
    public static void loadAndRestoreBackup(final Context context, final File zipFile) {
        try {
            for (final File backup : getBackupFiles(context)) {
                extractFileFromZip(zipFile, backup);
            }
            Toast.makeText(context, R.string.toast_restore_success, Toast.LENGTH_SHORT).show();
            System.exit(0);
        } catch (Exception e) {
            Toast.makeText(context, R.string.toast_restore_error, Toast.LENGTH_SHORT).show();
        }
    }

    public static void addFileToZip(final ZipOutputStream outZip, final File file) throws Exception {
        final byte data[] = new byte[BUFFER_SIZE];
        final FileInputStream fi = new FileInputStream(file);
        final BufferedInputStream inputStream = new BufferedInputStream(fi, BUFFER_SIZE);
        final String name = file.getName();
        final ZipEntry entry = new ZipEntry(name);
        outZip.putNextEntry(entry);
        int count;
        while ((count = inputStream.read(data, 0, BUFFER_SIZE)) != -1) {
            outZip.write(data, 0, count);
        }
        inputStream.close();
    }

    public static boolean extractFileFromZip(final File source, final File backup) throws Exception {
        final ZipInputStream inZip = new ZipInputStream(new BufferedInputStream(new FileInputStream(source)));
        final byte data[] = new byte[BUFFER_SIZE];
        boolean found = false;
        final String name = backup.getName();

        ZipEntry ze;
        while ((ze = inZip.getNextEntry()) != null) {
            if (ze.getName().equals(name)) {
                found = true;
                // delete old file first
                if (backup.exists()) {
                    if (!backup.delete()) {
                        throw new Exception("Could not delete " + backup.getPath());
                    }
                }

                FileOutputStream outFile = new FileOutputStream(backup);
                int count = 0;
                while ((count = inZip.read(data)) != -1) {
                    outFile.write(data, 0, count);
                }

                outFile.close();
                inZip.closeEntry();
            }
        }
        return found;
    }
}
