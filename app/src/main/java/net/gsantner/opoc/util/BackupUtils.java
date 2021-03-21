/*#######################################################
 *
 *   Maintained by Gregor Santner, 2021-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.opoc.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import net.gsantner.markor.R;
import net.gsantner.opoc.preference.SharedPreferencesPropertyBackend;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class BackupUtils {
    protected static final String LOG_PREFIX = BackupUtils.class.getSimpleName();
    protected static final String FIELD_BACKUP_METADATA = "__BACKUP_METADATA__";

    private static final Pattern[] PREF_EXCLUDE_PATTERNS = {
            Pattern.compile("^(?!PREF_PREFIX_).*password.*$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE),
            Pattern.compile(FIELD_BACKUP_METADATA, Pattern.MULTILINE),
    };

    public static List<String> getPrefNamesToBackup() {
        ArrayList<String> prefNames = new ArrayList<>();
        prefNames.add(null); // Default pref
        prefNames.add(SharedPreferencesPropertyBackend.SHARED_PREF_APP);
        return prefNames;
    }

    public static File generateBackupFilepath(final Context context, final File targetFolder) {
        final ContextUtils cu = new ContextUtils(context);
        try {
            final String appName = cu.rstr("app_name_real").toLowerCase().replaceAll("\\s", "");
            final String date = ShareUtil.SDF_IMAGES.format(new Date());
            final String filename = String.format("%s-settings-backup-%s.json", appName, date);
            return new File(targetFolder, filename);
        } finally {
            cu.freeContextRef();
        }
    }

    public static String getPrefName(final Context context, final String raw) {
        if (TextUtils.isEmpty(raw)) {
            return context.getPackageName() + "_preferences";
        } else {
            return raw;
        }
    }

    public static boolean isPrefKeyAllowBackup(final String key) {
        for (final Pattern ep : PREF_EXCLUDE_PATTERNS) {
            if (ep.matcher(key).matches()) {
                return false;
            }
        }
        return true;
    }

    public static void makeBackup(final Context context, final List<String> prefNamesToBackup, final File targetJsonFile) {
        final ContextUtils cu = new ContextUtils(context);
        try {
            final JSONObject jsonRoot = new JSONObject();

            // Collect metadata for backup file
            final Date now = new Date();
            final JSONObject jsonMetadata = new JSONObject(new GashMap<String, String>().load(
                    "BACKUP_DATE", String.format("%s ::: %d", now.toString(), now.getTime()),
                    "APPLICATION_ID_MANIFEST", cu.getPackageIdManifest(),
                    "EXPORT_ANDROID_DEVICE_VERSION", ContextUtils.getAndroidVersion(),
                    "ISOURCE", cu.getAppInstallationSource(),
                    "BACKUP_REVISION", "1"
            ).data());
            for (final String field : cu.getBuildConfigFields()) {
                final Object v = cu.getBuildConfigValue(field);
                if (v != null && !v.getClass().isArray()) {
                    jsonMetadata.put(field, cu.getBuildConfigValue(field));
                }
            }

            // Iterate preferences and their values
            for (String prefName : prefNamesToBackup) {
                prefName = getPrefName(context, prefName);
                final SharedPreferences pref = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
                final Map<String, ?> prefKeyValues = pref.getAll();
                final JSONObject jsonFromPref = new JSONObject();
                for (final String prefKey : prefKeyValues.keySet()) {
                    if (isPrefKeyAllowBackup(prefKey)) {
                        final Object prefValue = prefKeyValues.get(prefKey);
                        if ((prefValue instanceof Integer) ||
                                (prefValue instanceof Long) ||
                                (prefValue instanceof Float) ||
                                (prefValue instanceof String) ||
                                (prefValue instanceof Boolean)) {
                            jsonFromPref.put(prefKey, prefValue);
                        } else if (prefValue instanceof Set) {
                            final JSONArray jsonArray = new JSONArray();
                            for (final String s : pref.getStringSet(prefKey, new HashSet<>())) {
                                jsonArray.put(s);
                            }
                            jsonFromPref.put(prefKey, jsonArray);
                        } else {
                            Log.w(LOG_PREFIX, "Unhandled backup type");
                        }
                    }
                }
                if (jsonFromPref.length() > 0) {
                    jsonRoot.put(prefName, jsonFromPref);
                }
            }
            jsonRoot.put(FIELD_BACKUP_METADATA, jsonMetadata);
            final FileWriter file = new FileWriter(targetJsonFile);
            file.write(jsonRoot.toString(2));
            file.flush();
            file.close();
            Toast.makeText(context, "✔️ " + targetJsonFile.getName(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            // Attempt to delete file if it exists
            if (targetJsonFile.exists()) {
                targetJsonFile.delete();
            }
            Log.e(LOG_PREFIX, e.getMessage());
            Toast.makeText(context, R.string.creation_of_backup_failed, Toast.LENGTH_SHORT).show();
        } finally {
            cu.freeContextRef();
        }
    }

    public static void loadBackup(final Context context, final File backupFileContainingJson) {
        try {
            final JSONObject json = new JSONObject(FileUtils.readTextFileFast(backupFileContainingJson));
            final List<SharedPreferences.Editor> editors = new ArrayList<>();
            for (Iterator<String> it = json.keys(); it.hasNext(); ) {
                final String prefName = it.next();
                final SharedPreferences sp = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
                final SharedPreferences.Editor edit = sp.edit();
                final Object _pref = json.get(prefName);
                if (_pref instanceof JSONObject) {
                    final JSONObject prefJson = (JSONObject) _pref;
                    for (Iterator<String> pit = prefJson.keys(); pit.hasNext(); ) {
                        final String key = pit.next();
                        if (isPrefKeyAllowBackup(key)) {
                            final Object value = prefJson.get(key);
                            if (value instanceof Integer) {
                                edit.putInt(key, (Integer) value);
                            } else if (value instanceof Long) {
                                edit.putLong(key, (Long) value);
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
                                Log.w(LOG_PREFIX, "Unhandled backup type");
                            }
                        }
                    }
                    editors.add(edit);
                }
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
