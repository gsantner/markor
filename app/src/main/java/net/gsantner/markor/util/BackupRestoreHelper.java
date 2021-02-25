package net.gsantner.markor.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.annotation.StringRes;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.util.Pair;
import android.widget.Toast;

import net.gsantner.markor.R;
import net.gsantner.markor.ui.FilesystemViewerCreator;
import net.gsantner.opoc.preference.SharedPreferencesPropertyBackend;
import net.gsantner.opoc.ui.FilesystemViewerData;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class BackupRestoreHelper {

    private static final String SAVE_NAME = "markor_settings_backup%s.zip";
    private static final int BUFFER_SIZE = 2048;

    private static final String[] EXCLUDE_PATTERNS = {
            "cache.xml",
            "pirate.xml",
            "WebViewChromiumPrefs.xml",
            "AndroidSupportMeWrapper.LocalSettingsImpl.xml"
    };

    private static final String[] INCLUDE_PATTERNS = {
            ".*\\.xml"
    };

    private static final String[][] excludeStringKeys = {
            {"app", "pref_key__default_encryption_password"}
    };

    private static boolean includeFile(final File f) {
        return includeFile(f.getPath());
    }

    private static boolean includeFile(final String string) {

        boolean excluded = false;
        for (final String pattern : EXCLUDE_PATTERNS) {
            if (matchPattern(string, pattern)) {
                excluded = true;
                break;
            }
        }

        boolean included = false;
        for (final String pattern : INCLUDE_PATTERNS) {
            if (matchPattern(string, pattern)) {
                included = true;
                break;
            }
        }

        return included && !excluded;
    }

    private static boolean matchPattern(final String string, final String pattern) {
        final boolean isRegex = pattern.contains("*");
        return ((isRegex && string.matches(pattern)) || (!isRegex && string.contains(pattern)));
    }

    @SuppressLint("ApplySharedPref")
    private static Map<Integer, String> removeExcludedPreferences(final Context context) {
        final Map<Integer, String> map = new HashMap<>();
        for (int i = 0; i < excludeStringKeys.length; i++){
            final String[] p = excludeStringKeys[i];
            final String file = p[0], key = p[1];
            final SharedPreferences sp = context.getSharedPreferences(file, Context.MODE_PRIVATE);
            final SharedPreferences.Editor se = sp.edit();
            final String value = sp.getString(key, null);
            if (!TextUtils.isEmpty(value)) {
                map.put(i, value);
                se.putString(key, "");
                // Commit writes this change to disk immediately
                se.commit();
            }
        }
        return map;
    }

    private static void restoreExcludedPreferences(final Context context, final Map<Integer, String> excludeMap) {
        for (int i = 0; i < excludeStringKeys.length; i++){
            final String[] p = excludeStringKeys[i];
            final String file = p[0], key = p[1];
            final SharedPreferences sp = context.getSharedPreferences(file, Context.MODE_PRIVATE);
            final SharedPreferences.Editor se = sp.edit();
            try {
                final String value = excludeMap.get(i);
                if (value != null) {
                    se.putString(key, value);
                    se.apply();
                }
            } catch (NullPointerException ignored) {}; // Missing key
        }
    }

    private static File getPrefDir(final Context context) throws PackageManager.NameNotFoundException {
        final PackageManager packageManager = context.getPackageManager();
        final PackageInfo p = packageManager.getPackageInfo(context.getPackageName(), 0);
        return new File(p.applicationInfo.dataDir, "shared_prefs");
    }

    public static void backupConfig(final Context context, final FragmentManager manager) {

        if (context instanceof Activity) {
            final Activity activity = (Activity) context;

            FilesystemViewerCreator.showFolderDialog(
                    new FilesystemViewerData.SelectionListenerAdapter() {
                        @Override
                        public void onFsViewerConfig(FilesystemViewerData.Options dopt) {
                            dopt.rootFolder = new AppSettings(context).getNotebookDirectory();
                            dopt.titleText = R.string.select_folder;
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
            final File[] files = getPrefDir(context).listFiles();

            for (final File f : files) {
                if (includeFile(f)) {
                    addFileToZip(zos, f);
                }
            }

            Toast.makeText(context, "✔️ " + saveLoc.getName(), Toast.LENGTH_SHORT).show();
            zos.flush();
            zos.close();
        } catch (Exception e) {
            // Attempt to delete file if it exists
            if (saveLoc.exists()) {
                saveLoc.delete();
            }
            Toast.makeText(context, R.string.creation_of_backup_zip_file_failed, Toast.LENGTH_SHORT).show();
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
                            dopt.titleText = R.string.select;
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
            final ZipInputStream inZip = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)));
            final byte data[] = new byte[BUFFER_SIZE];
            final File destDir = getPrefDir(context);

            ZipEntry ze;
            while ((ze = inZip.getNextEntry()) != null) {
                final String name = ze.getName();
                if (includeFile(name)) {
                    final File dest = new File(destDir, name);

                    // delete old file first
                    if (dest.exists()) {
                        if (!dest.delete()) {
                            throw new Exception("Could not delete " + dest.getPath());
                        }
                    }

                    final FileOutputStream outStream = new FileOutputStream(dest);
                    int count = 0;
                    while ((count = inZip.read(data)) != -1) {
                        outStream.write(data, 0, count);
                    }

                    outStream.close();
                    inZip.closeEntry();
                }
            }
            System.exit(0);
        } catch (Exception e) {
            Toast.makeText(context, R.string.could_not_restore_from_backup, Toast.LENGTH_SHORT).show();
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
}
