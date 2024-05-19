/*#######################################################
 *
 * SPDX-FileCopyrightText: 2017-2024 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2017-2024 by Gregor Santner <gsantner AT mailbox DOT org>
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
#########################################################*/
package net.gsantner.opoc.frontend.settings;

import android.app.Activity;
import android.content.pm.PackageManager;

import net.gsantner.opoc.util.GsContextUtils;

import java.io.File;

@SuppressWarnings({"unused", "WeakerAccess"})
public class GsPermissionChecker {
    protected static final int CODE_PERMISSION_EXTERNAL_STORAGE = GsContextUtils.REQUEST_STORAGE_PERMISSION_M;

    protected Activity _activity;

    public GsPermissionChecker(Activity activity) {
        _activity = activity;
    }

    public boolean doIfExtStoragePermissionGranted() {
        return doIfExtStoragePermissionGranted(null);
    }

    public boolean doIfExtStoragePermissionGranted(final String whyNeeded) {
        if (!GsContextUtils.instance.checkExternalStoragePermission(_activity)) {
            GsContextUtils.instance.requestExternalStoragePermission(_activity, whyNeeded);
            return false;
        }
        return true;
    }

    public boolean checkPermissionResult(final int requestCode, String[] permissions, int[] grantResults) {
        if (grantResults.length > 0) {
            switch (requestCode) {
                case CODE_PERMISSION_EXTERNAL_STORAGE: {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean mkdirIfStoragePermissionGranted(final File dir) {
        return doIfExtStoragePermissionGranted() && (dir.exists() || dir.mkdirs());
    }
}
