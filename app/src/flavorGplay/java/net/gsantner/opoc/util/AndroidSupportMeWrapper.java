/*#######################################################
 *
 * SPDX-FileCopyrightText: 2018-2022 Gregor Santner <https://gsantner.net/>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2018-2022 by Gregor Santner <https://gsantner.net/>
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
#########################################################*/
package net.gsantner.opoc.util;

import android.app.Activity;
import android.content.Context;

@SuppressWarnings("unused")
public class AndroidSupportMeWrapper extends GsContextUtils {

    public AndroidSupportMeWrapper(Activity activity) {
        super();
    }

    public void mainOnResume(Context context) {
    }
}
