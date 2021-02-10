/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.activity.openeditor;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;

import net.gsantner.markor.R;
import net.gsantner.markor.activity.DocumentActivity;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.DocumentIO;
import net.gsantner.opoc.util.ActivityUtils;
import net.gsantner.opoc.util.FileUtils;
import net.gsantner.opoc.util.PermissionChecker;

import java.io.File;

public class OpenEditorActivity extends AppCompatActivity {
    protected void openEditorForFile(File file) {
        Intent openIntent = new Intent(getApplicationContext(), DocumentActivity.class)
                .setAction(Intent.ACTION_CALL_BUTTON)
                .putExtra(DocumentIO.EXTRA_PATH, file)
                .putExtra(DocumentIO.EXTRA_PATH_IS_FOLDER, false);
        openActivityAndClose(openIntent, file);
    }

    protected void openActivityAndClose(final Intent openIntent, File file) {
        try {
            PermissionChecker permc = new PermissionChecker(this);
            if (permc.doIfExtStoragePermissionGranted(getString(R.string.error_need_storage_permission_to_save_documents))) {
                file = (file != null ? file : new AppSettings(getApplicationContext()).getNotebookDirectory());
                if (!file.getParentFile().exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    file.getParentFile().mkdirs();
                }
                if (!file.exists() && !file.isDirectory()) {
                    FileUtils.writeFile(file, "");
                }
                openIntent.putExtra(DocumentIO.EXTRA_PATH, openIntent.hasExtra(DocumentIO.EXTRA_PATH) ? openIntent.getSerializableExtra(DocumentIO.EXTRA_PATH) : file);
                openIntent.putExtra(DocumentIO.EXTRA_PATH_IS_FOLDER, openIntent.hasExtra(DocumentIO.EXTRA_PATH_IS_FOLDER) ? openIntent.getSerializableExtra(DocumentIO.EXTRA_PATH_IS_FOLDER) : file.isDirectory());
                ActivityUtils au = new ActivityUtils(this);
                au.animateToActivity(openIntent, true, 1);
            }
        } catch (Exception ignored) {
            finish();
        }
    }
}
