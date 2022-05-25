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

import net.gsantner.markor.R;
import net.gsantner.markor.activity.DocumentActivity;
import net.gsantner.markor.activity.MarkorBaseActivity;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.opoc.util.ActivityUtils;
import net.gsantner.opoc.util.FileUtils;
import net.gsantner.opoc.util.PermissionChecker;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class OpenEditorActivity extends MarkorBaseActivity {
    protected void openEditorForFile(File file) {
        Intent openIntent = new Intent(getApplicationContext(), DocumentActivity.class)
                .setAction(Intent.ACTION_CALL_BUTTON)
                .putExtra(Document.EXTRA_PATH, file);
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
                    FileUtils.writeFile(file, "", new FileUtils.FileInfo().setBom(new AppSettings(getApplicationContext()).getNewFileDialogLastUsedUtf8Bom()));
                }
                openIntent.putExtra(Document.EXTRA_PATH, openIntent.hasExtra(Document.EXTRA_PATH) ? openIntent.getSerializableExtra(Document.EXTRA_PATH) : file);
                new ActivityUtils(this).animateToActivity(openIntent, true, 1).freeContextRef();
            }
        } catch (Exception ignored) {
            finish();
        }
    }
}
