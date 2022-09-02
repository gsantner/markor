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
import android.content.SharedPreferences;

import androidx.appcompat.app.AlertDialog;

import net.gsantner.markor.R;
import net.gsantner.opoc.model.GsSharedPreferencesPropertyBackend;

public class GsAndroidSupportMeWrapper extends GsContextUtils {
    private LocalSettingsImpl _localSettingsImpl;

    public GsAndroidSupportMeWrapper(final Activity activity) {
        super();
        _localSettingsImpl = new LocalSettingsImpl(activity.getApplicationContext());
    }

    public void openPayPalDonationPage(final Context context) {
        String id = getAppIdUsedAtManifest(context);
        String url = "https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=TVV24QBGMN23C&source=self.gsantner.net%2F" + id;
        openWebpageInExternalBrowser(context, url);
    }


    public void openGeneralDonatePage(final Context context) {
        openWebpageInExternalBrowser(context, context.getString(R.string.app_donate_url));
    }

    public void mainOnResume(final Context context) {
        if (_localSettingsImpl.all14dRequest()) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(context);
            dialog.setTitle(R.string.donate_)
                    .setCancelable(false)
                    .setNegativeButton(R.string.close, (dialogInterface, i) -> dialogInterface.dismiss())
                    .setPositiveButton("PayPal", (dialogInterface, i) -> {
                        openPayPalDonationPage(context);
                        dialogInterface.dismiss();
                    })
                    .setNeutralButton(R.string.donate_, (dialogInterface, i) -> {
                        openGeneralDonatePage(context);
                        dialogInterface.dismiss();
                    })
                    .setMessage(R.string.do_you_like_this_project_want_donate_to_keep_alive);
            dialog.show();
        }
    }

    private static class LocalSettingsImpl extends GsSharedPreferencesPropertyBackend {
        private final SharedPreferences _prefCache;

        public LocalSettingsImpl(Context context) {
            super.init(context, "AndroidSupportMeWrapper.LocalSettingsImpl");
            _prefCache = context.getSharedPreferences("cache", Context.MODE_PRIVATE);
        }

        public boolean all14dRequest() {
            return afterDaysTrue("all14dRequest", 14, 3);
        }
    }
}
