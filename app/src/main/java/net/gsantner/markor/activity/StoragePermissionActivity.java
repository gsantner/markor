package net.gsantner.markor.activity;

import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import net.gsantner.markor.R;
import net.gsantner.opoc.util.GsContextUtils;

public class StoragePermissionActivity extends MarkorBaseActivity {

    final static String EXTRA_INTENT = "EXTRA_INTENT";

    // Check whether we require permissions and setup appropriate conditions
    public static void requestPermissions(final MarkorBaseActivity activity) {
        if (!GsContextUtils.instance.checkExternalStoragePermission(activity)) {
            final Intent intent = new Intent(activity, StoragePermissionActivity.class).putExtra(EXTRA_INTENT, activity.getIntent());
            activity.startActivity(intent);
            activity.finish();
        }
    }

    // When this activity starts, request permissions
    @Override
    public void onActivityFirstTimeVisible() {
        super.onActivityFirstTimeVisible();
        askForPermissions();
    }

    private void askForPermissions() {
        final AlertDialog d = new AlertDialog.Builder(this)
                .setMessage(R.string.error_need_storage_permission_to_save_documents)
                .setNegativeButton(R.string.exit, (dialog, which) -> finish())
                .setPositiveButton(android.R.string.yes, (dialog, which) -> GsContextUtils.instance.requestExternalStoragePermission(this))
                .show();
        d.setCanceledOnTouchOutside(false);
    }

    private void processPermissionState() {
        if (_cu.checkExternalStoragePermission(this)) {
            this.startActivity(getIntent().getParcelableExtra(EXTRA_INTENT));
            finish();
        } else  {
            Toast.makeText(this, R.string.permission_not_granted, Toast.LENGTH_SHORT).show();
            askForPermissions();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        processPermissionState();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        processPermissionState();
    }
}
