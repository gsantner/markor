/*
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.util;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import net.gsantner.markor.R;
import net.gsantner.opoc.util.ActivityUtils;

import java.io.File;

/**
 * Created by gregor on 11.09.17.
 */

public class PermissionChecker {

    public static boolean doIfPermissionGranted(final Activity activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 314
            );
            return false;
        }
        return true;
    }

    public static boolean checkPermissionResult(final Activity activity, int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 314) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        new ActivityUtils(activity).showSnackBar(R.string.error_storage_permission, true);
        return false;
    }

    public static boolean mkSaveDir(Activity activity) {
        File saveDir = AppSettings.get().getNotebookDirectory();
        if (!saveDir.exists() && !saveDir.mkdirs()) {
            new ActivityUtils(activity).showSnackBar(R.string.error_cannot_create_notebook_dir, false);
            return false;
        }
        return true;
    }
}
