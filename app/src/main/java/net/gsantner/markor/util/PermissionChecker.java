/*
 * Copyright (c) 2017-2018 Gregor Santner
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.util;

import android.app.Activity;

import net.gsantner.markor.R;
import net.gsantner.opoc.util.ActivityUtils;

public class PermissionChecker extends net.gsantner.opoc.util.PermissionChecker {

    public PermissionChecker(Activity activity) {
        super(activity);
    }

    @Override
    public boolean doIfExtStoragePermissionGranted(String... optionalToastMessageForKnowingWhyNeeded) {
        return super.doIfExtStoragePermissionGranted(_activity.getString(R.string.error_storage_permission));
    }

    @Override
    public boolean checkPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        boolean val = super.checkPermissionResult(requestCode, permissions, grantResults);
        if (!val) {
            new ActivityUtils(_activity).showSnackBar(R.string.error_storage_permission, true);
        }
        return val;
    }

    public boolean mkdirIfStoragePermissionGranted() {
        return super.mkdirIfStoragePermissionGranted(AppSettings.get().getNotebookDirectory());
    }
}
