/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.frontend.settings;

import android.app.Activity;

import net.gsantner.markor.ApplicationObject;
import net.gsantner.markor.R;
import net.gsantner.opoc.frontend.settings.GsPermissionChecker;
import net.gsantner.opoc.util.GsContextUtils;

public class MarkorPermissionChecker extends GsPermissionChecker {

    public MarkorPermissionChecker(Activity activity) {
        super(activity);
    }

    @Override
    public boolean doIfExtStoragePermissionGranted(String... optionalToastMessageForKnowingWhyNeeded) {
        return super.doIfExtStoragePermissionGranted(_activity.getString(R.string.error_need_storage_permission_to_save_documents));
    }

    @Override
    public boolean checkPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        boolean val = super.checkPermissionResult(requestCode, permissions, grantResults);
        if (!val) {
            GsContextUtils.instance.showSnackBar(_activity, R.string.error_need_storage_permission_to_save_documents, true);
        }
        return val;
    }

    public boolean mkdirIfStoragePermissionGranted() {
        return super.mkdirIfStoragePermissionGranted(ApplicationObject.settings().getNotebookDirectory());
    }
}
