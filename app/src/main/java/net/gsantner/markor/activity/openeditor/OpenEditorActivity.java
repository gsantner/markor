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
import net.gsantner.opoc.frontend.settings.GsPermissionChecker;
import net.gsantner.opoc.util.GsFileUtils;

import java.io.File;

public class OpenEditorActivity extends MarkorBaseActivity {

    protected void openEditorForFile(final File file, final Integer line) {
        final Intent openIntent = new Intent(getApplicationContext(), DocumentActivity.class)
                .setAction(Intent.ACTION_CALL_BUTTON)
                .putExtra(Document.EXTRA_PATH, file);

        if (line != null) {
            openIntent.putExtra(Document.EXTRA_FILE_LINE_NUMBER, line);
        }

        openActivityAndClose(openIntent, file);
    }

    protected void openActivityAndClose(final Intent openIntent, File file) {
        try {
            GsPermissionChecker permc = new GsPermissionChecker(this);
            if (permc.doIfExtStoragePermissionGranted(getString(R.string.error_need_storage_permission_to_save_documents))) {
                file = (file != null ? file : _appSettings.getNotebookDirectory());
                if (!file.getParentFile().exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    file.getParentFile().mkdirs();
                }
                if (!file.exists() && !file.isDirectory()) {
                    final GsFileUtils.FileInfo info = new GsFileUtils.FileInfo();
                    info.hasBom = _appSettings.getNewFileDialogLastUsedUtf8Bom();
                    GsFileUtils.writeFile(file, "", info);
                }
                openIntent.putExtra(Document.EXTRA_PATH, openIntent.hasExtra(Document.EXTRA_PATH) ? openIntent.getSerializableExtra(Document.EXTRA_PATH) : file);
                _cu.animateToActivity(this, openIntent, true, 1);
            }
        } catch (Exception ignored) {
            finish();
        }
    }
}
