/*#######################################################
 *
 *   Maintained by Gregor Santner, 2018-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.activity;

import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;

public class ActionItem {
    @StringRes public int keyId;
    @DrawableRes public int iconId;
    @StringRes public int stringId;

    public ActionItem(@StringRes int key, @DrawableRes int icon, @StringRes int string) {
        keyId = key;
        iconId = icon;
        stringId = string;
    }

    public ActionItem(int[] data) {
        keyId = data[0];
        iconId = data[1];
        stringId = data[2];
    }
}
