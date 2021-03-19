package net.gsantner.markor.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import net.gsantner.markor.R;
import net.gsantner.markor.format.general.DatetimeFormatDialog;
import net.gsantner.markor.ui.FilesystemViewerCreator;
import net.gsantner.markor.ui.SearchReplaceDialog;
import net.gsantner.markor.ui.hleditor.TextActions;
import net.gsantner.opoc.ui.FilesystemViewerData;
import net.gsantner.opoc.util.FileUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class BackupRestoreHelper {

    private static final String SAVE_NAME = "markor_settings_backup%s.json";
    private static final String VERSION = "__VERSION__";

    private static final String[] PREF_NAMES = {
            null, // Default pref
            AppSettings.SHARED_PREF_APP,
            DatetimeFormatDialog.DATETIME_SETTINGS,
            TextActions.ACTION_ORDER_PREF_NAME,
            SearchReplaceDialog.SEARCH_REPLACE_SETTINGS
    };

    private static final Pattern[] PREF_EXCLUDE_PATTERNS = {
            Pattern.compile("^pref_key__.*encryption_password.*", Pattern.MULTILINE),
            Pattern.compile(VERSION, Pattern.MULTILINE),
    };

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

    public static String getPrefName(final Context context, final String raw) {
        if (TextUtils.isEmpty(raw)) {
            return context.getPackageName() + "_preferences";
        } else {
            return raw;
        }
    }

    public static boolean includeKey(final String key) {
        for (final Pattern ep : PREF_EXCLUDE_PATTERNS) {
            if (ep.matcher(key).matches()) {
                return false;
            }
        }
        return true;
    }

    public static void createAndSaveBackup(final Context context, final File saveLoc) {
        try {
            final JSONObject json = new JSONObject();
            json.put(VERSION, context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName));
            for (final String _pref : PREF_NAMES) {
                final String pref = getPrefName(context, _pref);
                final SharedPreferences sp = context.getSharedPreferences(pref, Context.MODE_PRIVATE);
                final Map<String, ?> map = sp.getAll();
                final JSONObject prefSon = new JSONObject();
                for (final String key : map.keySet()) {
                    if (includeKey(key)) {
                        final Object value = map.get(key);
                        if (
                            (value instanceof Integer) ||
                            (value instanceof Float) ||
                            (value instanceof String) ||
                            (value instanceof Boolean)
                        ) {
                            prefSon.put(key, value);
                        } else if (value instanceof Set) {
                            final JSONArray lsa = new JSONArray();
                            for (final String s : sp.getStringSet(key, new HashSet<>())) {
                                lsa.put(s);
                            }
                            prefSon.put(key, lsa);
                        } else {
                            Log.w("backup", "Unhandled backup type");
                        }
                    }
                }
                if (prefSon.length() > 0) {
                    json.put(pref, prefSon);
                }
            }
            final FileWriter file = new FileWriter(saveLoc);
            file.write(json.toString(4));
            file.flush();
            file.close();
            Toast.makeText(context, "✔️ " + saveLoc.getName(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            // Attempt to delete file if it exists
            if (saveLoc.exists()) {
                saveLoc.delete();
            }
            Log.e("backup", e.getMessage());
            Toast.makeText(context, R.string.creation_of_backup_failed, Toast.LENGTH_SHORT).show();
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
                    input -> input != null && input.exists() && input.toString().trim().toLowerCase().endsWith(".json")
            );
        }

    }

    public static void loadAndRestoreBackup(final Context context, final File jsonFile) {
        try {
            final JSONObject json = new JSONObject(FileUtils.readTextFileFast(jsonFile));
            final List<SharedPreferences.Editor> editors = new ArrayList<>();
            for (Iterator<String> it = json.keys(); it.hasNext(); ) {
                final String prefName = it.next();
                final SharedPreferences sp = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
                final SharedPreferences.Editor edit = sp.edit();

                final JSONObject prefJson = json.getJSONObject(prefName);
                for (Iterator<String> pit = prefJson.keys(); pit.hasNext(); ) {
                    final String key = pit.next();
                    if (includeKey(key)) {
                        final Object value = prefJson.get(key);
                        if (value instanceof Integer) {
                            edit.putInt(key, (Integer) value);
                        } else if (value instanceof Float) {
                            edit.putFloat(key, (Float) value);
                        } else if (value instanceof String) {
                            edit.putString(key, (String) value);
                        } else if (value instanceof Boolean) {
                            edit.putBoolean(key, (Boolean) value);
                        } else if (value instanceof JSONArray) {
                            final Set<String> ss = new HashSet<>();
                            for (int i = 0; i < ((JSONArray) value).length(); i++) {
                                ss.add(((JSONArray) value).getString(i));
                            }
                            edit.putStringSet(key, ss);
                        } else {
                            Log.w("backup", "Unhandled backup type");
                        }
                    }
                }
                editors.add(edit);
            }
            for (final SharedPreferences.Editor edit : editors) {
                edit.apply();
            }
            System.exit(0);
        } catch (Exception e) {
            Toast.makeText(context, R.string.could_not_restore_from_backup, Toast.LENGTH_SHORT).show();
        }
    }
}
