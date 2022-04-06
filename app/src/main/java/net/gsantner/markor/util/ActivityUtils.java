/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.util;

import android.app.Activity;

import net.gsantner.markor.activity.LaunchActivity.OpenSpecialQuicknote;
import net.gsantner.markor.activity.LaunchActivity.OpenSpecialShareInto;
import net.gsantner.markor.activity.LaunchActivity.OpenSpecialTodo;

public class ActivityUtils extends net.gsantner.opoc.util.ActivityUtils {

    public ActivityUtils(Activity activity) {
        super(activity);
    }

    public void applySpecialLaunchersVisibility(boolean extraLaunchersEnabled) {
        setLauncherActivityEnabled(OpenSpecialQuicknote.class, extraLaunchersEnabled);
        setLauncherActivityEnabled(OpenSpecialTodo.class, extraLaunchersEnabled);
        setLauncherActivityEnabled(OpenSpecialShareInto.class, extraLaunchersEnabled);
    }
}
