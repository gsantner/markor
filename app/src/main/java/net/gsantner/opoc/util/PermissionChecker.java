/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *     https://github.com/gsantner/opoc/#licensing
 *
#########################################################*/
package net.gsantner.opoc.util;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import java.io.File;

@SuppressWarnings({"unused", "WeakerAccess"})
public class PermissionChecker {
    protected static final int CODE_PERMISSION_EXTERNAL_STORAGE = ShareUtil.REQUEST_STORAGE_PERMISSION_M;

    protected Context _context;

    public PermissionChecker(final Context context) {
        _context = context;
    }

    public boolean doIfExtStoragePermissionGranted(String... optionalToastMessageForKnowingWhyNeeded) {
        ShareUtil shareUtil = new ShareUtil(_context);
        final boolean ret = shareUtil.checkExternalStoragePermission(true, optionalToastMessageForKnowingWhyNeeded);
        shareUtil.freeContextRef();
        return ret;
    }

    public boolean checkPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
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

    public boolean mkdirIfStoragePermissionGranted(File dir) {
        return doIfExtStoragePermissionGranted() && (dir.exists() || dir.mkdirs());
    }
}
