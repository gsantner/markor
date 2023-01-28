package net.gsantner.markor.activity;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import net.gsantner.markor.R;
import net.gsantner.opoc.util.GsContextUtils;

public class StoragePermissionActivity extends MarkorBaseActivity {

    static final String EXTRA_LAUNCHING_CLASS = "EXTRA_LAUNCHING_CLASS";
    static final String EXTRA_LAUNCHING_INTENT = "EXTRA_LAUNCHING_INTENT";

    public static void request(final Activity activity) {
        if (!GsContextUtils.instance.checkExternalStoragePermission(activity)) {
            final Intent intent = new Intent(activity, StoragePermissionActivity.class);

            // Data used to return
            intent.putExtra(EXTRA_LAUNCHING_CLASS, activity.getClass());
            intent.putExtra(EXTRA_LAUNCHING_INTENT, activity.getIntent());

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
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> GsContextUtils.instance.requestExternalStoragePermission(this, null))
                .show();
        d.setCanceledOnTouchOutside(false);
    }

    // Go back if we have received permissions
    private void processPermissionState() {
        if (_cu.checkExternalStoragePermission(this)) {
            final Intent source = getIntent();
            Intent dest;
            if ((dest = source.getParcelableExtra(EXTRA_LAUNCHING_INTENT)) != null) {
                startActivity(dest);
            }
            startActivity(new Intent(this, (Class<?>) source.getSerializableExtra(EXTRA_LAUNCHING_CLASS)));
        } else {
            finishAffinity();
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
}
