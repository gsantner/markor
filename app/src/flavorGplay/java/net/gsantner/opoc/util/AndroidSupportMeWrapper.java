/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *     https://github.com/gsantner/opoc/#licensing
 *
#########################################################*/
package net.gsantner.opoc.util;

import android.app.Activity;
import net.gsantner.opoc.util.ActivityUtils;

public class AndroidSupportMeWrapper extends ActivityUtils {

    public AndroidSupportMeWrapper(Activity activity) {
        super(activity);
    }

    public void mainOnResume() {
    }
}
