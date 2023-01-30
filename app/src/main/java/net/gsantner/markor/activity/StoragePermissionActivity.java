package net.gsantner.markor.activity;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import net.gsantner.markor.ApplicationObject;
import net.gsantner.markor.R;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.util.MarkorContextUtils;
import net.gsantner.opoc.util.GsContextUtils;

import java.io.File;

public class StoragePermissionActivity extends MarkorBaseActivity {

    // Check whether we require permissions and setup appropriate conditions
    public static void fixMainActivityPaths(final Activity activity) {
        final AppSettings as = ApplicationObject.settings();

        final File start = MarkorContextUtils.getValidIntentDir(activity.getIntent(), null);

        if (!GsContextUtils.instance.checkExternalStoragePermission(activity) && (
                !as.getNotebookFile().canWrite() ||
                !as.getTodoFile().canWrite() ||
                !as.getQuickNoteFile().canWrite() ||
                start != null && !start.canWrite())
        ) {

            final Intent intent = new Intent(activity, StoragePermissionActivity.class)
                    .putExtra(Document.EXTRA_PATH, start);
            activity.startActivity(intent);
            activity.finish();
        }
    }

    // When this activity starts, request permissions
    @Override
    public void onActivityFirstTimeVisible() {
        super.onActivityFirstTimeVisible();
        final AlertDialog d = new AlertDialog.Builder(this)
                .setMessage(R.string.error_need_storage_permission_to_save_documents)
                .setCancelable(true)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> GsContextUtils.instance.requestExternalStoragePermission(this, R.string.error_need_storage_permission_to_save_documents))
                .show();
        d.setCanceledOnTouchOutside(false);
    }

    private void processPermissionState() {
        if (!_cu.checkExternalStoragePermission(this)) {
            final Intent intent = getIntent();
            // Fix standard file paths
            if (!_appSettings.getNotebookFile().canWrite()) {
                _appSettings.setNotebookFile(_appSettings.getDefaultNotebookFile());
            }
            if (!_appSettings.getQuickNoteFile().canWrite()) {
                _appSettings.setQuickNoteFile(_appSettings.getDefaultQuickNoteFile());
            }
            if (!_appSettings.getTodoFile().canWrite()) {
                _appSettings.setTodoFile(_appSettings.getDefaultTodoFile());
            }

            // Fix requested dir
            final Intent dest = new Intent(this, MainActivity.class);
            final File extra = (File) intent.getSerializableExtra(Document.EXTRA_PATH);
            if (extra != null) {
                dest.putExtra(Document.EXTRA_PATH, extra.canWrite() ? extra : _appSettings.getNotebookFile());
            }

            // Return to MainActivity
            startActivity(dest);
        }
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        processPermissionState();
    }

    @Override
    @SuppressWarnings("unused")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        processPermissionState();
    }

    // Utilities
    // =========================================================================================

    public static boolean checkRequestPermissions(final Activity activity, final File file) {
        return checkRequestPermissions(activity, file, activity.getString(R.string.permission_needed_to_access, file.getPath()));
    }

    public static boolean checkRequestPermissions(final Activity activity, final File file, final String message) {
        if (file == null || file.canWrite()) return true;

        if (!GsContextUtils.instance.checkExternalStoragePermission(activity)) {
            GsContextUtils.instance.requestExternalStoragePermission(activity, message);
        }

        return false;
    }
}
