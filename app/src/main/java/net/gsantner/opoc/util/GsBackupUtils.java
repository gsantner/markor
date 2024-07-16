/*#######################################################
 *
 * SPDX-FileCopyrightText: 2022-2024 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2022-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
#########################################################*/
package net.gsantner.opoc.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import net.gsantner.opoc.model.GsSharedPreferencesPropertyBackend;
import net.gsantner.opoc.wrapper.GsHashMap;

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

/**
 * Utility class for backup of Android {@link SharedPreferences}
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public class GsBackupUtils {
    protected static final String LOG_PREFIX = GsBackupUtils.class.getSimpleName();
    protected static final String FIELD_BACKUP_METADATA = "__BACKUP_METADATA__";

    private static final Pattern[] PREF_EXCLUDE_PATTERNS = {
            Pattern.compile("^(?!PREF_PREFIX_).*password.*$", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE),
            Pattern.compile(FIELD_BACKUP_METADATA, Pattern.MULTILINE),
    };

    /**
     * Get a {@link List} of preference (file)names to be included in backup
     *
     * @return String list of preference names to be included in backup
     */
    public static List<String> getPrefNamesToBackup() {
        ArrayList<String> prefNames = new ArrayList<>();
        prefNames.add(null); // Default pref
        prefNames.add(GsSharedPreferencesPropertyBackend.SHARED_PREF_APP);
        return prefNames;
    }

    /**
     * Generate a timestamped filepath for the backup file
     *
     * @param context      Android {@link Context}
     * @param targetFolder Folder in which the {@link File} should be placed
     * @return The {@link File} that should be created
     */
    public static File generateBackupFilepath(final Context context, final File targetFolder) {
        final GsContextUtils cu = GsContextUtils.instance;
        final String appName = cu.rstr(context, "app_name_real").toLowerCase().replaceAll("\\s", "");
        return new File(targetFolder, GsFileUtils.getFilenameWithTimestamp("BACKUP_" + appName, null, ".json"));
    }

    public static String getPrefName(final Context context, final String raw) {
        return !TextUtils.isEmpty(raw) ? raw : (context.getPackageName() + "_preferences");
    }

    /**
     * Determine if the key is allowed to be included in the backup - see  {@link GsBackupUtils#getPrefNamesToBackup()}
     *
     * @param key Check if this key is allowed
     * @return True if allowed -> backup
     */
    public static boolean isPrefKeyAllowBackup(final String key) {
        for (final Pattern ep : PREF_EXCLUDE_PATTERNS) {
            if (ep.matcher(key).matches()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Make backup of Android {@link SharedPreferences}, combined into a single .json file
     *
     * @param context           Android context
     * @param prefNamesToBackup Names of the {@link SharedPreferences}'s to backup
     * @param targetJsonFile    Target json file to write to, overwritten if already exists
     */
    public static void makeBackup(final Context context, final List<String> prefNamesToBackup, final File targetJsonFile) {
        final GsContextUtils cu = GsContextUtils.instance;
        try {
            final JSONObject jsonRoot = new JSONObject();

            // Collect metadata for backup file
            final Date now = new Date();
            final JSONObject jsonMetadata = new JSONObject(new GsHashMap<String, String>().load(
                    "BACKUP_DATE", String.format("%s ::: %d", now.toString(), now.getTime()),
                    "APPLICATION_ID_MANIFEST", cu.getAppIdUsedAtManifest(context),
                    "EXPORT_ANDROID_DEVICE_VERSION", GsContextUtils.getAndroidVersion(),
                    "ISOURCE", cu.getAppInstallationSource(context),
                    "BACKUP_REVISION", "1"
            ).data());
            for (final String field : cu.getBuildConfigFields(context)) {
                final Object v = cu.getBuildConfigValue(context, field);
                if (v != null && !v.getClass().isArray()) {
                    jsonMetadata.put(field, cu.getBuildConfigValue(context, field));
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
            Toast.makeText(context, cu.rstr(context, "failed_to_create_backup", true), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Load a backup-json file that should have been exported by the same app
     *
     * @param context                  Android context
     * @param backupFileContainingJson A existing & accessible json file
     */
    @SuppressLint("ApplySharedPref")
    public static void loadBackup(final Context context, final File backupFileContainingJson) {
        try {
            final JSONObject json = new JSONObject(GsFileUtils.readTextFileFast(backupFileContainingJson).first);
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
                edit.commit();
            }
            System.exit(0);
        } catch (Exception e) {
            Toast.makeText(context, GsContextUtils.instance.rstr(context, "failed_to_restore_settings_from_backup", true), Toast.LENGTH_SHORT).show();
        }
    }
}
